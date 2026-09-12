package ru.mockarty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.mockarty.exception.MockartyException;

import java.time.Duration;

/** Child-process probe used to inspect TRACE output with a fresh SLF4J binding. */
public final class ConnectionCredentialLogProbe {
    public static final String SECRET = "trace-leak-password-6be79d";

    public static void main(String[] args) {
        ObjectNode descriptor = new ObjectMapper().createObjectNode();
        descriptor.put("id", "gitlab-prod");
        descriptor.put("kind", "gitlab");
        descriptor.putObject("configuredBindings").put("password", SECRET);
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:1").namespace("team")
                .timeout(Duration.ofMillis(50)).maxRetries(0).build()) {
            client.connections().create(descriptor);
        } catch (MockartyException expected) {
            // Serialization and its TRACE logging happen before this expected transport failure.
        }
    }

    private ConnectionCredentialLogProbe() {}
}
