package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.SpecExportRequest;
import ru.mockarty.model.SpecExportResponse;
import ru.mockarty.model.SpecExportWarning;

import java.util.List;

/**
 * Builds an API specification FROM the mocks — the reverse of the generator
 * examples, which turn a document into mocks.
 *
 * <p>A contour built by hand, or grown from recorded traffic, can be handed to a
 * partner, a client generator or the contract registry this way. Two things are
 * worth reading closely in the output:
 *
 * <ul>
 *   <li>{@code warnings} names the mocks the document could NOT express — a
 *       response built by a script, a oneOf rotation, a proxy, a template file.
 *       They are reported rather than turned into examples the product never
 *       returns, and their operations stay in the document with a default
 *       response.</li>
 *   <li>{@code stats} accounts for every mock considered, so a small document can
 *       be told apart from a filter that matched nothing.</li>
 * </ul>
 */
public final class SpecExportExample {
    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create()) {
            // OpenAPI: HTTP mocks become paths and operations.
            SpecExportResponse openapi = client.generator().exportSpec(
                    new SpecExportRequest("openapi")
                            .title("Checkout contour")
                            .namespace(System.getenv("MOCKARTY_NAMESPACE"))
                            .tags(List.of("checkout")));
            report(openapi);

            // AsyncAPI: Kafka/RabbitMQ/NATS/socket/SMTP mocks become channels.
            SpecExportResponse asyncapi = client.generator().exportSpec(
                    new SpecExportRequest("asyncapi")
                            .namespace(System.getenv("MOCKARTY_NAMESPACE")));
            report(asyncapi);
        }
    }

    private static void report(SpecExportResponse response) {
        System.out.printf("%s: %d mock(s) exported into %d path(s)/channel(s), %d skipped%n",
                response.getFormat(),
                response.getStats().getExported(),
                response.getStats().getPaths() != null
                        ? response.getStats().getPaths()
                        : response.getStats().getChannels(),
                response.getStats().getSkipped());
        if (response.getWarnings() != null) {
            for (SpecExportWarning warning : response.getWarnings()) {
                System.out.printf("  ! %s (%s): %s%n",
                        warning.getMockId(), warning.getRoute(), warning.getReason());
            }
        }
    }
}
