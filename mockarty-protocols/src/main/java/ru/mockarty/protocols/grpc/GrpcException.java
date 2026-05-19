package ru.mockarty.protocols.grpc;

/**
 * Unchecked failure raised by {@link GrpcClient}. Wraps both transport
 * errors (dial / TLS / DNS) and dynamic-dispatch failures
 * (method-not-found, JSON↔proto shape mismatch, gRPC status errors).
 *
 * <p>Use {@link #getCause()} to introspect the underlying
 * {@code io.grpc.StatusRuntimeException} or {@code IOException} when
 * the test needs to discriminate by gRPC status code.
 */
public class GrpcException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GrpcException(String message) {
        super(message);
    }

    public GrpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
