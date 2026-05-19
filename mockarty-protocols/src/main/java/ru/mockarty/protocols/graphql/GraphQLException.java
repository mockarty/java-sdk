package ru.mockarty.protocols.graphql;

/**
 * Unchecked exception thrown by {@link GraphQLClient} when the
 * transport fails or the response is unparseable. GraphQL-level
 * errors (non-empty {@code errors[]} on a 200) are NOT thrown by
 * default — they surface on
 * {@link GraphQLResponse#getErrors()} so the caller can choose to
 * assert / ignore. To propagate them as exceptions, check
 * {@code response.isOk()} after the call.
 */
public final class GraphQLException extends RuntimeException {

    public GraphQLException(String message) {
        super(message);
    }

    public GraphQLException(String message, Throwable cause) {
        super(message, cause);
    }
}
