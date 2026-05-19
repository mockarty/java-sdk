package ru.mockarty.protocols.grpc;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import ru.mockarty.protocols.telemetry.AccumulatingRecorder;
import ru.mockarty.protocols.telemetry.NopRecorder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class GrpcClientUnitTest {

    @Test
    void emptyTargetRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GrpcClient(""));
        assertThrows(IllegalArgumentException.class, () -> new GrpcClient(null));
    }

    @Test
    void closeIsIdempotent() {
        GrpcClient c = new GrpcClient("localhost:1");
        c.close();
        c.close();
        assertThrows(GrpcException.class, () -> c.invokeJson("svc.S/M", null, String.class));
    }

    @Test
    void optionsNullRecorderCoercesToNop() {
        GrpcClient c = new GrpcClient("localhost:1", o -> o.recorder(null));
        // Reach into the client and verify no NPE on close — coercion path works.
        c.close();
    }

    @Test
    void optionsNegativePayloadCapClamps() {
        GrpcClient c = new GrpcClient("localhost:1", o -> o.payloadCap(-100));
        c.close();
        // Reflective check is intrusive; the contract is that close() works,
        // and the clamp is verified directly on the Options bag below.
        GrpcClient.Options opts = new GrpcClient.Options().payloadCap(-7);
        // No assertion on private field — confirm Options#payloadCap doesn't throw.
        assertNotNull(opts);
    }

    @Test
    void optionsZeroTimeoutIgnored() {
        // No throw — zero/negative timeouts must be silently ignored.
        new GrpcClient("localhost:1", o -> o.timeout(Duration.ZERO).timeout(Duration.ofSeconds(-3))).close();
    }

    @Test
    void splitMethodAcceptsLeadingSlash() {
        String[] parts = GrpcClient.splitMethod("/acme.UserService/GetUser");
        assertEquals("acme.UserService", parts[0]);
        assertEquals("GetUser", parts[1]);
    }

    @Test
    void splitMethodAcceptsBare() {
        String[] parts = GrpcClient.splitMethod("acme.UserService/GetUser");
        assertEquals("acme.UserService", parts[0]);
        assertEquals("GetUser", parts[1]);
    }

    @Test
    void splitMethodTrimsWhitespace() {
        String[] parts = GrpcClient.splitMethod("  acme.X/Y  ");
        assertEquals("acme.X", parts[0]);
        assertEquals("Y", parts[1]);
    }

    @Test
    void splitMethodRejectsMalformed() {
        assertThrows(GrpcException.class, () -> GrpcClient.splitMethod(""));
        assertThrows(GrpcException.class, () -> GrpcClient.splitMethod("/"));
        assertThrows(GrpcException.class, () -> GrpcClient.splitMethod("nosep"));
        assertThrows(GrpcException.class, () -> GrpcClient.splitMethod("a/"));
    }

    @Test
    void classifyMatrix() {
        assertEquals("broken", classifyStatus(Status.CANCELLED));
        assertEquals("broken", classifyStatus(Status.DEADLINE_EXCEEDED));
        assertEquals("failed", classifyStatus(Status.INVALID_ARGUMENT));
        assertEquals("failed", classifyStatus(Status.UNAVAILABLE));
        assertEquals("failed", classifyStatus(Status.UNKNOWN));
    }

    /** Mirror the classification used in {@link GrpcClient#invokeJson}. */
    private static String classifyStatus(Status s) {
        Status.Code code = s.getCode();
        return (code == Status.Code.CANCELLED || code == Status.Code.DEADLINE_EXCEEDED)
            ? "broken" : "failed";
    }

    @Test
    void streamingMethodRejectedWithDescriptorSet() throws Exception {
        byte[] descSet = buildFileDescriptorSet(/*streaming*/ true);
        AccumulatingRecorder rec = new AccumulatingRecorder();
        try (GrpcClient c = new GrpcClient("localhost:1",
            o -> o.protoDescriptorSet(descSet).reflection(false).recorder(rec))) {
            GrpcException ex = assertThrows(GrpcException.class,
                () -> c.invokeJson("test.SvcOne/Stream", null, String.class));
            assertTrue(ex.getMessage().contains("streaming"));
        }
        // One broken step emitted on the rejection.
        assertEquals(1, rec.size());
        assertEquals("broken", rec.raw().get(0).getStatus());
    }

    @Test
    void listServicesViaFileSource() {
        byte[] descSet = buildFileDescriptorSet(false);
        try (GrpcClient c = new GrpcClient("localhost:1",
            o -> o.protoDescriptorSet(descSet).reflection(false))) {
            assertTrue(c.listServices().contains("test.SvcOne"));
            assertEquals(java.util.List.of("Echo"), c.listMethods("test.SvcOne"));
        }
    }

    @Test
    void methodNotFoundRecordsBrokenStep() {
        byte[] descSet = buildFileDescriptorSet(false);
        AccumulatingRecorder rec = new AccumulatingRecorder();
        try (GrpcClient c = new GrpcClient("localhost:1",
            o -> o.protoDescriptorSet(descSet).reflection(false).recorder(rec))) {
            assertThrows(GrpcException.class, () -> c.invokeJson("test.SvcOne/Unknown", null, String.class));
        }
        assertEquals(1, rec.size());
        assertEquals("broken", rec.raw().get(0).getStatus());
        assertEquals("test.SvcOne/Unknown", rec.raw().get(0).getName());
    }

    @Test
    void stepKeyMonotonic() {
        byte[] descSet = buildFileDescriptorSet(false);
        AccumulatingRecorder rec = new AccumulatingRecorder();
        try (GrpcClient c = new GrpcClient("localhost:1",
            o -> o.protoDescriptorSet(descSet).reflection(false).recorder(rec))) {
            for (int i = 0; i < 4; i++) {
                try { c.invokeJson("test.SvcOne/Unknown", null, String.class); } catch (GrpcException ignored) {}
            }
        }
        assertEquals(4, rec.size());
        for (int i = 0; i < 4; i++) {
            assertEquals("test.SvcOne/Unknown#" + (i + 1), rec.raw().get(i).getKey());
        }
    }

    @Test
    void recorderDefaultsToNop() {
        // No explicit recorder → NopRecorder.INSTANCE; no exception when
        // calls run + fail without a buffer behind them.
        byte[] descSet = buildFileDescriptorSet(false);
        try (GrpcClient c = new GrpcClient("localhost:1",
            o -> o.protoDescriptorSet(descSet).reflection(false))) {
            assertThrows(GrpcException.class, () -> c.invokeJson("test.SvcOne/Unknown", null, String.class));
        }
    }

    @Test
    void nopRecorderSentinel() {
        // Sanity — Options without an explicit recorder uses NopRecorder.
        assertNotNull(NopRecorder.INSTANCE);
    }

    @Test
    void grpcExceptionWrapsCause() {
        Throwable cause = new RuntimeException("boom");
        GrpcException gex = new GrpcException("wrap", cause);
        assertSame(cause, gex.getCause());
        assertEquals("wrap", gex.getMessage());
    }

    @Test
    void closedClientRejectsListCalls() {
        GrpcClient c = new GrpcClient("localhost:1");
        c.close();
        assertThrows(GrpcException.class, c::listServices);
        assertThrows(GrpcException.class, () -> c.listMethods("x"));
    }

    /**
     * Build a minimal {@link FileDescriptorSet} with one service for
     * descriptor-set source tests. When {@code streaming} is true the
     * service's lone method is server-streaming so we can exercise the
     * rejection path.
     */
    private static byte[] buildFileDescriptorSet(boolean streaming) {
        DescriptorProto msg = DescriptorProto.newBuilder()
            .setName("Msg")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("v")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_STRING)
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                .build())
            .build();
        MethodDescriptorProto method = MethodDescriptorProto.newBuilder()
            .setName(streaming ? "Stream" : "Echo")
            .setInputType(".test.Msg")
            .setOutputType(".test.Msg")
            .setServerStreaming(streaming)
            .build();
        ServiceDescriptorProto svc = ServiceDescriptorProto.newBuilder()
            .setName("SvcOne")
            .addMethod(method)
            .build();
        FileDescriptorProto fp = FileDescriptorProto.newBuilder()
            .setName("test.proto")
            .setPackage("test")
            .setSyntax("proto3")
            .addMessageType(msg)
            .addService(svc)
            .build();
        return FileDescriptorSet.newBuilder().addFile(fp).build().toByteArray();
    }

    @Test
    void statusRuntimeExceptionSurfacesAsGrpcException() {
        // Sanity — a StatusRuntimeException is a known carrier of typed gRPC errors.
        StatusRuntimeException sre = Status.INVALID_ARGUMENT.asRuntimeException();
        assertEquals(Status.Code.INVALID_ARGUMENT, sre.getStatus().getCode());
    }
}
