package ru.mockarty.protocols.grpc;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.util.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import ru.mockarty.protocols.telemetry.NopRecorder;
import ru.mockarty.protocols.telemetry.Step;
import ru.mockarty.protocols.telemetry.StepRecorder;
import ru.mockarty.protocols.telemetry.Telemetry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Dynamic gRPC test client. Resolves a method's protobuf descriptor at
 * call time (via pre-compiled {@code FileDescriptorSet} blob and/or
 * server reflection), JSON-encodes the request via
 * {@link JsonFormat}, fires a unary RPC, and decodes the response back
 * into a user-supplied Java type via Jackson.
 *
 * <p>Streaming methods (server / client / bidi) are explicitly rejected
 * — the v1 client is unary-only, same as the Go and Python SDKs.
 *
 * <p>Step capture goes through the shared {@link StepRecorder} contract
 * so a single recorder can buffer steps across every protocol client
 * a test wires (SOAP, GraphQL, SSE, WebSocket, gRPC, Kafka, RabbitMQ).
 */
public final class GrpcClient implements AutoCloseable {

    private final ManagedChannel channel;
    private final DescriptorSource source;
    private final StepRecorder recorder;
    private final int payloadCap;
    private final Duration timeout;
    private final Map<String, String> defaultMetadata;
    private final AtomicLong counter = new AtomicLong(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ObjectMapper json = new ObjectMapper();

    public GrpcClient(String target) {
        this(target, opts -> {});
    }

    public GrpcClient(String target, Consumer<Options> configure) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("mockarty grpc: empty dial target");
        }
        Options opts = new Options();
        configure.accept(opts);
        ChannelCredentials creds = opts.credentials != null
            ? opts.credentials
            : InsecureChannelCredentials.create();
        this.channel = Grpc.newChannelBuilder(target, creds).build();
        this.recorder = opts.recorder == null ? NopRecorder.INSTANCE : opts.recorder;
        this.payloadCap = Math.max(0, opts.payloadCap);
        this.timeout = opts.timeout;
        this.defaultMetadata = opts.metadata == null
            ? Map.of()
            : Map.copyOf(opts.metadata);

        DescriptorSource.FileSource file = null;
        if (opts.protoDescriptorSet != null && opts.protoDescriptorSet.length > 0) {
            file = new DescriptorSource.FileSource(opts.protoDescriptorSet);
        }
        DescriptorSource.ReflectionSource refl = null;
        if (opts.reflection) {
            long timeoutNanos = (this.timeout == null || this.timeout.isZero() || this.timeout.isNegative())
                ? TimeUnit.SECONDS.toNanos(10)
                : this.timeout.toNanos();
            refl = new DescriptorSource.ReflectionSource(channel, timeoutNanos);
        }
        this.source = new DescriptorSource.CombinedSource(file, refl);
    }

    /** Bare full method name (e.g. {@code acme.UserService/GetUser});
     *  request/response are JSON-shaped (Map/POJO/JsonNode/byte[]/String). */
    public <T> T invokeJson(String fullMethod, Object request, Class<T> respClass) {
        if (closed.get()) {
            throw new GrpcException("client is closed");
        }
        long seq = counter.incrementAndGet();
        String stepName = stripLeadingSlash(fullMethod);
        Instant started = Instant.now();

        MethodDescriptor md;
        try {
            md = source.findMethod(fullMethod);
        } catch (GrpcException gex) {
            recordStep(stepName, seq, started, Instant.now(), "broken", gex, Map.of());
            throw gex;
        }
        if (md == null) {
            GrpcException gex = new GrpcException("method not found: " + fullMethod);
            recordStep(stepName, seq, started, Instant.now(), "broken", gex, Map.of());
            throw gex;
        }
        if (md.isClientStreaming() || md.isServerStreaming()) {
            GrpcException gex = new GrpcException(fullMethod
                + " is a streaming method — server/bidi streaming not supported in v1");
            recordStep(stepName, seq, started, Instant.now(), "broken", gex, Map.of());
            throw gex;
        }

        String requestJson;
        DynamicMessage reqMessage;
        try {
            requestJson = toJsonString(request);
            DynamicMessage.Builder b = DynamicMessage.newBuilder(md.getInputType());
            if (!requestJson.isEmpty()) {
                JsonFormat.parser().ignoringUnknownFields().merge(requestJson, b);
            }
            reqMessage = b.build();
        } catch (Exception e) {
            GrpcException gex = new GrpcException(
                "bind JSON to proto " + md.getInputType().getFullName() + ": " + e.getMessage(), e);
            recordStep(stepName, seq, started, Instant.now(), "failed", gex,
                Map.of("request", Telemetry.capPreview(safe(request), payloadCap)));
            throw gex;
        }

        io.grpc.MethodDescriptor<DynamicMessage, DynamicMessage> grpcMd = buildGrpcMethodDescriptor(md);
        Channel callChannel = withDefaultMetadata(channel, defaultMetadata);
        CallOptions callOpts = CallOptions.DEFAULT;
        if (timeout != null && !timeout.isZero() && !timeout.isNegative()) {
            callOpts = callOpts.withDeadlineAfter(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        DynamicMessage respMessage;
        try {
            respMessage = ClientCalls.blockingUnaryCall(callChannel, grpcMd, callOpts, reqMessage);
        } catch (StatusRuntimeException sre) {
            Status.Code code = sre.getStatus().getCode();
            String status = (code == Status.Code.CANCELLED || code == Status.Code.DEADLINE_EXCEEDED)
                ? "broken"
                : "failed";
            Map<String, String> params = new LinkedHashMap<>();
            params.put("request", Telemetry.capPreview(requestJson, payloadCap));
            recordStep(stepName, seq, started, Instant.now(), status, sre, params);
            throw new GrpcException("rpc " + fullMethod + ": " + sre.getStatus(), sre);
        } catch (RuntimeException re) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("request", Telemetry.capPreview(requestJson, payloadCap));
            recordStep(stepName, seq, started, Instant.now(), "broken", re, params);
            throw new GrpcException("rpc " + fullMethod + ": " + re.getMessage(), re);
        }

        String responseJson;
        try {
            responseJson = JsonFormat.printer()
                .omittingInsignificantWhitespace()
                .preservingProtoFieldNames()
                .print(respMessage);
        } catch (Exception e) {
            GrpcException gex = new GrpcException("marshal response: " + e.getMessage(), e);
            Map<String, String> params = new LinkedHashMap<>();
            params.put("request", Telemetry.capPreview(requestJson, payloadCap));
            recordStep(stepName, seq, started, Instant.now(), "broken", gex, params);
            throw gex;
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("request", Telemetry.capPreview(requestJson, payloadCap));
        params.put("response", Telemetry.capPreview(responseJson, payloadCap));
        recordStep(stepName, seq, started, Instant.now(), "passed", null, params);

        if (respClass == null || respClass == Void.class || respClass == void.class) {
            return null;
        }
        try {
            if (respClass == String.class) {
                return respClass.cast(responseJson);
            }
            if (respClass == byte[].class) {
                return respClass.cast(responseJson.getBytes(StandardCharsets.UTF_8));
            }
            return json.readValue(responseJson, respClass);
        } catch (Exception e) {
            throw new GrpcException("decode response into " + respClass.getSimpleName()
                + ": " + e.getMessage(), e);
        }
    }

    public List<String> listServices() {
        if (closed.get()) {
            throw new GrpcException("client is closed");
        }
        return source.listServices();
    }

    public List<String> listMethods(String service) {
        if (closed.get()) {
            throw new GrpcException("client is closed");
        }
        return source.listMethods(service);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        source.close();
        if (channel != null) {
            channel.shutdownNow();
            try {
                channel.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------------

    private void recordStep(String name, long seq, Instant start, Instant end,
                            String status, Throwable err, Map<String, String> params) {
        Step.Builder b = Step.builder()
            .key(Telemetry.newStepKey(name, seq))
            .name(name)
            .status(status)
            .startedAt(start)
            .finishedAt(end)
            .durationMs(Math.max(0, end.toEpochMilli() - start.toEpochMilli()))
            .parameters(params);
        if (err != null) {
            b.message(err.getMessage() == null ? err.getClass().getSimpleName() : err.getMessage());
        }
        recorder.record(b.build());
    }

    private String toJsonString(Object value) throws Exception {
        if (value == null) return "";
        if (value instanceof byte[]) return new String((byte[]) value, StandardCharsets.UTF_8);
        if (value instanceof CharSequence) return value.toString();
        if (value instanceof JsonNode) return value.toString();
        return json.writeValueAsString(value);
    }

    private static String safe(Object v) {
        try {
            return v == null ? "" : v.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static String[] splitMethod(String s) {
        if (s == null) {
            throw new GrpcException("empty method");
        }
        String trimmed = s.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isEmpty()) {
            throw new GrpcException("empty method");
        }
        int idx = trimmed.lastIndexOf('/');
        if (idx <= 0 || idx == trimmed.length() - 1) {
            throw new GrpcException("malformed method \"" + s
                + "\" (expected \"package.Service/Method\")");
        }
        return new String[] { trimmed.substring(0, idx), trimmed.substring(idx + 1) };
    }

    static String stripLeadingSlash(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.startsWith("/") ? t.substring(1) : t;
    }

    private static Channel withDefaultMetadata(Channel base, Map<String, String> md) {
        if (md == null || md.isEmpty()) {
            return base;
        }
        return io.grpc.ClientInterceptors.intercept(base, new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                io.grpc.MethodDescriptor<ReqT, RespT> method, CallOptions opts, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, opts)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        for (Map.Entry<String, String> e : md.entrySet()) {
                            headers.put(
                                Metadata.Key.of(e.getKey(), Metadata.ASCII_STRING_MARSHALLER),
                                e.getValue());
                        }
                        super.start(responseListener, headers);
                    }
                };
            }
        });
    }

    /**
     * Build a grpc-java {@link io.grpc.MethodDescriptor} for a unary
     * call with {@link DynamicMessage} payloads. The marshallers go via
     * the raw proto wire format — JSON conversion happens at the
     * Mockarty-SDK layer.
     */
    static io.grpc.MethodDescriptor<DynamicMessage, DynamicMessage> buildGrpcMethodDescriptor(
        MethodDescriptor md
    ) {
        Marshaller<DynamicMessage> reqM = new Marshaller<DynamicMessage>() {
            @Override
            public InputStream stream(DynamicMessage value) {
                return new ByteArrayInputStream(value.toByteArray());
            }
            @Override
            public DynamicMessage parse(InputStream stream) {
                try {
                    return DynamicMessage.parseFrom(md.getInputType(), stream);
                } catch (Exception e) {
                    throw new GrpcException("decode request bytes: " + e.getMessage(), e);
                }
            }
        };
        Marshaller<DynamicMessage> respM = new Marshaller<DynamicMessage>() {
            @Override
            public InputStream stream(DynamicMessage value) {
                return new ByteArrayInputStream(value.toByteArray());
            }
            @Override
            public DynamicMessage parse(InputStream stream) {
                try {
                    return DynamicMessage.parseFrom(md.getOutputType(), stream);
                } catch (Exception e) {
                    throw new GrpcException("decode response bytes: " + e.getMessage(), e);
                }
            }
        };
        String fullName = md.getService().getFullName() + "/" + md.getName();
        return io.grpc.MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(fullName)
            .setRequestMarshaller(reqM)
            .setResponseMarshaller(respM)
            .build();
    }

    /** Fluent options bag for {@link GrpcClient}. */
    public static final class Options {
        private StepRecorder recorder = NopRecorder.INSTANCE;
        private ChannelCredentials credentials;
        private Map<String, String> metadata;
        private byte[] protoDescriptorSet;
        private boolean reflection = true;
        private Duration timeout = Duration.ofSeconds(30);
        private int payloadCap = 1024;

        public Options recorder(StepRecorder r) {
            this.recorder = r == null ? NopRecorder.INSTANCE : r;
            return this;
        }

        /** Pin TLS credentials. Default is insecure (plaintext) —
         *  suitable for in-cluster Mockarty mocks and local dev. */
        public Options tls(ChannelCredentials creds) {
            this.credentials = creds;
            return this;
        }

        /** Static metadata applied to every RPC (Authorization,
         *  x-tenant-id, …). Per-call metadata is not exposed in v1. */
        public Options metadata(Map<String, String> md) {
            if (md == null) return this;
            if (this.metadata == null) this.metadata = new LinkedHashMap<>();
            this.metadata.putAll(md);
            return this;
        }

        /**
         * Pre-compiled {@code FileDescriptorSet} blob (the output of
         * {@code protoc --descriptor_set_out=path.pb --include_imports schema.proto}).
         * Pure-Java {@code .proto} parsing isn't readily available, so
         * users compile descriptors once at build time and ship the
         * resulting bytes alongside the test sources.
         */
        public Options protoDescriptorSet(byte[] bytes) {
            this.protoDescriptorSet = bytes;
            return this;
        }

        /** Toggle server reflection lookup. Default {@code true}. */
        public Options reflection(boolean enabled) {
            this.reflection = enabled;
            return this;
        }

        public Options timeout(Duration d) {
            if (d != null && !d.isZero() && !d.isNegative()) {
                this.timeout = d;
            }
            return this;
        }

        public Options payloadCap(int n) {
            this.payloadCap = Math.max(0, n);
            return this;
        }
    }

    // Test seam — package-private so unit tests can build a stub
    // descriptor without spinning a server.
    DescriptorSource source() { return source; }
}
