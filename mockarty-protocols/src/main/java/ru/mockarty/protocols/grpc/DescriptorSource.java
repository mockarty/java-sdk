package ru.mockarty.protocols.grpc;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Narrow interface the {@link GrpcClient} uses to resolve a method's
 * {@link MethodDescriptor}, list services and list methods. Two
 * concrete sources ship in the SDK:
 *
 * <ul>
 *   <li>{@link FileSource} — pre-compiled {@code FileDescriptorSet}
 *       (output of {@code protoc --descriptor_set_out=...}).</li>
 *   <li>{@link ReflectionSource} — talks {@code grpc.reflection.v1alpha}
 *       to the server.</li>
 * </ul>
 *
 * <p>{@link CombinedSource} probes the file source first (cheaper,
 * deterministic) then falls back to reflection — same precedence as
 * the Go SDK.
 */
interface DescriptorSource extends AutoCloseable {

    /**
     * Look up a unary method by full method name (e.g.
     * {@code "acme.UserService/GetUser"}). Returns {@code null} when no
     * descriptor source knows the method — callers translate that into
     * the canonical {@link GrpcException}.
     */
    MethodDescriptor findMethod(String fullMethod) throws GrpcException;

    /** Fully-qualified service names. */
    List<String> listServices() throws GrpcException;

    /** Method names of a service (bare names, NOT fully qualified). */
    List<String> listMethods(String service) throws GrpcException;

    @Override
    void close();

    // ------------------------------------------------------------------
    // FileSource
    // ------------------------------------------------------------------

    /**
     * Descriptor source backed by a pre-compiled {@code FileDescriptorSet}
     * blob. Users typically generate one via
     * {@code protoc --descriptor_set_out=out.pb --include_imports schema.proto}
     * and pass the bytes to {@link GrpcClient.Options#protoDescriptorSet(byte[])}.
     */
    final class FileSource implements DescriptorSource {

        private final List<FileDescriptor> files;

        FileSource(byte[] descriptorSet) {
            if (descriptorSet == null || descriptorSet.length == 0) {
                throw new GrpcException("empty FileDescriptorSet");
            }
            try {
                FileDescriptorSet set = FileDescriptorSet.parseFrom(descriptorSet);
                Map<String, FileDescriptorProto> byName = new HashMap<>(set.getFileCount());
                for (FileDescriptorProto fp : set.getFileList()) {
                    byName.put(fp.getName(), fp);
                }
                Map<String, FileDescriptor> built = new LinkedHashMap<>();
                for (FileDescriptorProto fp : set.getFileList()) {
                    buildRecursive(fp, byName, built);
                }
                this.files = new ArrayList<>(built.values());
            } catch (Exception e) {
                throw new GrpcException("parse FileDescriptorSet: " + e.getMessage(), e);
            }
        }

        private static FileDescriptor buildRecursive(
            FileDescriptorProto fp,
            Map<String, FileDescriptorProto> byName,
            Map<String, FileDescriptor> built
        ) throws DescriptorValidationException {
            FileDescriptor existing = built.get(fp.getName());
            if (existing != null) {
                return existing;
            }
            FileDescriptor[] deps = new FileDescriptor[fp.getDependencyCount()];
            for (int i = 0; i < fp.getDependencyCount(); i++) {
                String depName = fp.getDependency(i);
                FileDescriptorProto depProto = byName.get(depName);
                if (depProto == null) {
                    throw new GrpcException("missing transitive proto dependency: " + depName
                        + " (rebuild your descriptor set with --include_imports)");
                }
                deps[i] = buildRecursive(depProto, byName, built);
            }
            FileDescriptor fd = FileDescriptor.buildFrom(fp, deps);
            built.put(fp.getName(), fd);
            return fd;
        }

        List<FileDescriptor> files() {
            return files;
        }

        @Override
        public MethodDescriptor findMethod(String fullMethod) {
            String[] parts = GrpcClient.splitMethod(fullMethod);
            for (FileDescriptor f : files) {
                ServiceDescriptor sd = f.findServiceByName(localName(parts[0]));
                if (sd != null && fullyQualifiedMatches(sd, parts[0])) {
                    MethodDescriptor md = sd.findMethodByName(parts[1]);
                    if (md != null) {
                        return md;
                    }
                }
            }
            return null;
        }

        @Override
        public List<String> listServices() {
            List<String> out = new ArrayList<>();
            for (FileDescriptor f : files) {
                for (ServiceDescriptor sd : f.getServices()) {
                    out.add(sd.getFullName());
                }
            }
            return out;
        }

        @Override
        public List<String> listMethods(String service) {
            ServiceDescriptor sd = findService(service);
            if (sd == null) {
                return null;
            }
            List<String> out = new ArrayList<>();
            for (MethodDescriptor md : sd.getMethods()) {
                out.add(md.getName());
            }
            return out;
        }

        ServiceDescriptor findService(String fullyQualified) {
            String simple = localName(fullyQualified);
            for (FileDescriptor f : files) {
                ServiceDescriptor sd = f.findServiceByName(simple);
                if (sd != null && fullyQualifiedMatches(sd, fullyQualified)) {
                    return sd;
                }
            }
            return null;
        }

        private static String localName(String fullyQualified) {
            int dot = fullyQualified.lastIndexOf('.');
            return dot < 0 ? fullyQualified : fullyQualified.substring(dot + 1);
        }

        private static boolean fullyQualifiedMatches(ServiceDescriptor sd, String expected) {
            return sd.getFullName().equals(expected);
        }

        @Override
        public void close() {
            // Descriptors are immutable + GC-managed.
        }
    }

    // ------------------------------------------------------------------
    // ReflectionSource
    // ------------------------------------------------------------------

    /**
     * Descriptor source backed by gRPC server-reflection ({@code v1alpha}).
     * Each lookup opens a fresh bidi stream against the same channel —
     * reflection responses are small and the per-call overhead is
     * negligible for test code.
     *
     * <p>The reflection wire returns the proto descriptors as raw
     * {@code FileDescriptorProto} bytes; we build {@link FileDescriptor}
     * objects on the fly and cache them per file name so subsequent
     * lookups don't re-fetch the same .proto.
     */
    final class ReflectionSource implements DescriptorSource {

        private final Channel channel;
        private final long timeoutNanos;
        // Cache: proto file name → built FileDescriptor.
        private final Map<String, FileDescriptor> cache = new HashMap<>();
        private final Object cacheLock = new Object();

        ReflectionSource(Channel channel, long timeoutNanos) {
            this.channel = channel;
            this.timeoutNanos = timeoutNanos;
        }

        @Override
        public MethodDescriptor findMethod(String fullMethod) {
            String[] parts = GrpcClient.splitMethod(fullMethod);
            ServiceDescriptor sd = resolveService(parts[0]);
            if (sd == null) {
                return null;
            }
            return sd.findMethodByName(parts[1]);
        }

        @Override
        public List<String> listServices() {
            ServerReflectionRequest req = ServerReflectionRequest.newBuilder()
                .setListServices("")
                .build();
            ServerReflectionResponse resp = roundTrip(req);
            if (resp == null || !resp.hasListServicesResponse()) {
                return new ArrayList<>();
            }
            List<String> out = new ArrayList<>(resp.getListServicesResponse().getServiceCount());
            for (int i = 0; i < resp.getListServicesResponse().getServiceCount(); i++) {
                out.add(resp.getListServicesResponse().getService(i).getName());
            }
            return out;
        }

        @Override
        public List<String> listMethods(String service) {
            ServiceDescriptor sd = resolveService(service);
            if (sd == null) {
                return null;
            }
            List<String> out = new ArrayList<>();
            for (MethodDescriptor md : sd.getMethods()) {
                out.add(md.getName());
            }
            return out;
        }

        private ServiceDescriptor resolveService(String fullyQualified) {
            ServerReflectionRequest req = ServerReflectionRequest.newBuilder()
                .setFileContainingSymbol(fullyQualified)
                .build();
            ServerReflectionResponse resp = roundTrip(req);
            if (resp == null || !resp.hasFileDescriptorResponse()) {
                return null;
            }
            // Build every returned proto (and its transitive deps if needed).
            Map<String, FileDescriptorProto> pending = new LinkedHashMap<>();
            for (int i = 0; i < resp.getFileDescriptorResponse().getFileDescriptorProtoCount(); i++) {
                try {
                    FileDescriptorProto fp = FileDescriptorProto.parseFrom(
                        resp.getFileDescriptorResponse().getFileDescriptorProto(i));
                    pending.put(fp.getName(), fp);
                } catch (Exception e) {
                    throw new GrpcException("decode FileDescriptorProto from reflection: "
                        + e.getMessage(), e);
                }
            }
            // First pass: build any file whose deps we already have, then loop
            // until everything builds (deps come back in the same response).
            synchronized (cacheLock) {
                for (FileDescriptorProto fp : pending.values()) {
                    buildAndCache(fp, pending);
                }
                for (FileDescriptor fd : cache.values()) {
                    for (ServiceDescriptor sd : fd.getServices()) {
                        if (sd.getFullName().equals(fullyQualified)) {
                            return sd;
                        }
                    }
                }
            }
            return null;
        }

        private FileDescriptor buildAndCache(
            FileDescriptorProto fp,
            Map<String, FileDescriptorProto> pending
        ) {
            FileDescriptor cached = cache.get(fp.getName());
            if (cached != null) {
                return cached;
            }
            FileDescriptor[] deps = new FileDescriptor[fp.getDependencyCount()];
            for (int i = 0; i < fp.getDependencyCount(); i++) {
                String name = fp.getDependency(i);
                FileDescriptor depCached = cache.get(name);
                if (depCached != null) {
                    deps[i] = depCached;
                    continue;
                }
                FileDescriptorProto depProto = pending.get(name);
                if (depProto == null) {
                    // Fetch the missing dep over reflection on demand.
                    ServerReflectionRequest req = ServerReflectionRequest.newBuilder()
                        .setFileByFilename(name)
                        .build();
                    ServerReflectionResponse resp = roundTrip(req);
                    if (resp == null || !resp.hasFileDescriptorResponse()) {
                        throw new GrpcException(
                            "reflection: cannot resolve transitive dep " + name);
                    }
                    for (int j = 0; j < resp.getFileDescriptorResponse().getFileDescriptorProtoCount(); j++) {
                        try {
                            FileDescriptorProto fetched = FileDescriptorProto.parseFrom(
                                resp.getFileDescriptorResponse().getFileDescriptorProto(j));
                            pending.put(fetched.getName(), fetched);
                        } catch (Exception e) {
                            throw new GrpcException("decode dep " + name + ": "
                                + e.getMessage(), e);
                        }
                    }
                    depProto = pending.get(name);
                    if (depProto == null) {
                        throw new GrpcException(
                            "reflection: dep " + name + " not in response");
                    }
                }
                deps[i] = buildAndCache(depProto, pending);
            }
            try {
                FileDescriptor fd = FileDescriptor.buildFrom(fp, deps);
                cache.put(fp.getName(), fd);
                return fd;
            } catch (DescriptorValidationException e) {
                throw new GrpcException("build descriptor " + fp.getName() + ": "
                    + e.getMessage(), e);
            }
        }

        private ServerReflectionResponse roundTrip(ServerReflectionRequest req) {
            ServerReflectionGrpc.ServerReflectionStub stub =
                ServerReflectionGrpc.newStub(channel);
            LinkedBlockingQueue<ServerReflectionResponse> queue = new LinkedBlockingQueue<>();
            AtomicReference<Throwable> err = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            StreamObserver<ServerReflectionResponse> respObs = new StreamObserver<>() {
                @Override
                public void onNext(ServerReflectionResponse v) {
                    queue.offer(v);
                }
                @Override
                public void onError(Throwable t) {
                    err.set(t);
                    done.countDown();
                }
                @Override
                public void onCompleted() {
                    done.countDown();
                }
            };
            StreamObserver<ServerReflectionRequest> reqObs = stub.serverReflectionInfo(respObs);
            reqObs.onNext(req);
            reqObs.onCompleted();
            try {
                ServerReflectionResponse first = queue.poll(timeoutNanos, TimeUnit.NANOSECONDS);
                if (first != null) {
                    return first;
                }
                if (!done.await(timeoutNanos, TimeUnit.NANOSECONDS)) {
                    throw new GrpcException("reflection: timed out");
                }
                Throwable t = err.get();
                if (t instanceof StatusRuntimeException) {
                    throw new GrpcException("reflection: " + t.getMessage(), t);
                }
                return null;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new GrpcException("reflection: interrupted", ie);
            }
        }

        // Test-only descriptor injector. Lets unit tests seed the cache
        // without going through a live reflection stream. NOT part of the
        // public surface.
        void seedCache(FileDescriptor fd) {
            synchronized (cacheLock) {
                cache.put(fd.getName(), fd);
            }
        }

        @Override
        public void close() {
            // The channel lifetime belongs to GrpcClient. Nothing to release
            // here beyond letting the descriptor cache age out.
            synchronized (cacheLock) {
                cache.clear();
            }
        }

    }

    // ------------------------------------------------------------------
    // CombinedSource
    // ------------------------------------------------------------------

    /**
     * Probes the file source first (cheaper + deterministic) then falls
     * back to reflection — same precedence as the Go SDK. Either branch
     * may be {@code null}; the surviving one carries every lookup.
     */
    final class CombinedSource implements DescriptorSource {

        private final FileSource file;
        private final ReflectionSource refl;

        CombinedSource(FileSource file, ReflectionSource refl) {
            this.file = file;
            this.refl = refl;
        }

        FileSource file() { return file; }
        ReflectionSource refl() { return refl; }

        @Override
        public MethodDescriptor findMethod(String fullMethod) {
            if (file != null) {
                MethodDescriptor md = file.findMethod(fullMethod);
                if (md != null) {
                    return md;
                }
            }
            if (refl != null) {
                MethodDescriptor md = refl.findMethod(fullMethod);
                if (md != null) {
                    return md;
                }
            }
            return null;
        }

        @Override
        public List<String> listServices() {
            List<String> out = new ArrayList<>();
            HashMap<String, Boolean> seen = new HashMap<>();
            if (file != null) {
                for (String s : file.listServices()) {
                    if (seen.put(s, true) == null) {
                        out.add(s);
                    }
                }
            }
            if (refl != null) {
                for (String s : refl.listServices()) {
                    if (seen.put(s, true) == null) {
                        out.add(s);
                    }
                }
            }
            return out;
        }

        @Override
        public List<String> listMethods(String service) {
            if (file != null) {
                List<String> out = file.listMethods(service);
                if (out != null) {
                    return out;
                }
            }
            if (refl != null) {
                List<String> out = refl.listMethods(service);
                if (out != null) {
                    return out;
                }
            }
            return null;
        }

        @Override
        public void close() {
            if (file != null) file.close();
            if (refl != null) refl.close();
        }
    }
}
