// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Message-pact DSL — consumer-side declaration of async messaging
 * contracts (Kafka events, AMQP messages, SNS notifications, NATS
 * subjects, etc.).
 *
 * <p>Mirrors the V4 {@code Asynchronous/Messages} interaction type
 * from the pact-foundation spec, with V3 legacy fallback. Files
 * produced here verify against any pact-compatible message verifier
 * (pact-go, pact-jvm, pact-python, our own
 * {@link ru.mockarty.pact.verifier.Verifier}).</p>
 *
 * <pre>{@code
 * MessagePact mp = new MessagePact("OrderConsumer", "OrderEvents")
 *     .given("user 42 exists")
 *     .expectsToReceive("an order-created event")
 *     .withMetadata(Map.of("topic", "orders"))
 *     .withContent(Map.of("orderId", 42, "status", "open"));
 *
 * // Consumer-side: verify our real handler can decode the example.
 * mp.verify((bytes, meta) -> orderHandler.handle(bytes));
 * mp.writeFile(Path.of("./pacts"));
 * }</pre>
 */
public final class MessagePact {

    public static final String MESSAGE_INTERACTION_TYPE = "Asynchronous/Messages";

    private static final ObjectMapper MAPPER =
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Pattern UNSAFE_NAME = Pattern.compile("[^A-Za-z0-9_.-]+");

    /** Consumer message handler — raises if it cannot process the bytes. */
    @FunctionalInterface
    public interface MessageHandler {
        void handle(byte[] content, Map<String, String> metadata) throws Exception;
    }

    private final String consumer;
    private final String provider;
    private final String spec;
    private final List<Message> messages = new ArrayList<>();
    private Message cursor;

    public MessagePact(String consumer, String provider) {
        this(consumer, provider, "4.0");
    }

    public MessagePact(String consumer, String provider, String spec) {
        if (consumer == null || consumer.isBlank()) {
            throw new IllegalArgumentException("consumer name is required");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider name is required");
        }
        this.consumer = consumer;
        this.provider = provider;
        this.spec = spec == null || spec.isBlank() ? "4.0" : spec;
    }

    // ------------------------------------------------------------------
    // fluent DSL
    // ------------------------------------------------------------------

    public MessagePact given(String state) {
        return given(state, Map.of());
    }

    public MessagePact given(String state, Map<String, Object> params) {
        Message m = new Message();
        m.states.add(makeState(state, params));
        messages.add(m);
        cursor = m;
        return this;
    }

    /**
     * Append an additional providerState to the CURRENT message
     * (V4 supports multi-state messages). Must be chained after
     * {@code given(...)}; raises {@link IllegalStateException} when
     * called before any state.
     */
    public MessagePact andGiven(String state) {
        return andGiven(state, Map.of());
    }

    public MessagePact andGiven(String state, Map<String, Object> params) {
        requireCursor().states.add(makeState(state, params));
        return this;
    }

    private static Map<String, Object> makeState(String state, Map<String, Object> params) {
        Map<String, Object> st = new LinkedHashMap<>();
        st.put("name", state);
        if (params != null && !params.isEmpty()) st.put("params", params);
        return st;
    }

    public MessagePact expectsToReceive(String description) {
        requireCursor().description = description;
        return this;
    }

    public MessagePact withMetadata(Map<String, String> meta) {
        if (meta != null) {
            requireCursor().metadata.putAll(meta);
        }
        return this;
    }

    public MessagePact withContent(Object body) {
        Message c = requireCursor();
        c.content = body;
        if (c.contentType.isBlank()) c.contentType = "application/json";
        return this;
    }

    public MessagePact withContentType(String ct) {
        requireCursor().contentType = ct;
        return this;
    }

    private Message requireCursor() {
        if (cursor == null) {
            throw new IllegalStateException(
                "call .given(...) first to start a message before "
                    + ".expectsToReceive() / .withContent() / etc.");
        }
        return cursor;
    }

    // ------------------------------------------------------------------
    // output
    // ------------------------------------------------------------------

    public byte[] toJson() {
        ObjectNode doc = MAPPER.createObjectNode();
        doc.putObject("consumer").put("name", consumer);
        doc.putObject("provider").put("name", provider);
        ObjectNode meta = doc.putObject("metadata");
        meta.putObject("pactSpecification").put("version", spec);
        meta.putObject("mockarty").put("role", "messageConsumer");
        if (spec.startsWith("4")) {
            ArrayNode arr = doc.putArray("interactions");
            for (Message m : messages) arr.add(serialiseV4(m));
        } else {
            ArrayNode arr = doc.putArray("messages");
            for (Message m : messages) arr.add(serialiseV3(m));
        }
        try {
            return MAPPER.writeValueAsBytes(doc);
        } catch (IOException e) {
            throw new IllegalStateException("toJson: " + e.getMessage(), e);
        }
    }

    public Path writeFile(Path dir) throws IOException {
        Files.createDirectories(dir);
        String name = sanitise(consumer.toLowerCase()) + "-"
            + sanitise(provider.toLowerCase()) + ".json";
        Path out = dir.resolve(name);
        Files.write(out, toJson());
        return out;
    }

    public void verify(MessageHandler handler) throws Exception {
        if (handler == null) throw new IllegalArgumentException("handler is required");
        for (Message m : messages) {
            byte[] body = encodeBody(resolveMatchers(m.content), m.contentType);
            try {
                handler.handle(body, Map.copyOf(m.metadata));
            } catch (Exception e) {
                throw new RuntimeException(
                    "consumer rejected \"" + m.description + "\": " + e.getMessage(), e);
            }
        }
    }

    /** Snapshot of the configured messages (used by the verifier). */
    public List<Message> messages() {
        return List.copyOf(messages);
    }

    public String consumer() { return consumer; }
    public String provider() { return provider; }
    public String spec() { return spec; }

    // ------------------------------------------------------------------
    // serialisation
    // ------------------------------------------------------------------

    private static ObjectNode serialiseV4(Message m) {
        ObjectNode ix = MAPPER.createObjectNode();
        ix.put("type", MESSAGE_INTERACTION_TYPE);
        ix.put("description", m.description);
        if (!m.states.isEmpty()) {
            ArrayNode arr = ix.putArray("providerStates");
            for (Map<String, Object> s : m.states) arr.addPOJO(s);
        }
        ObjectNode contents = ix.putObject("contents");
        contents.put("contentType", m.contentType.isBlank() ? "application/json" : m.contentType);
        contents.putPOJO("content", resolveMatchers(m.content));
        if (!m.metadata.isEmpty()) ix.putPOJO("metadata", m.metadata);
        return ix;
    }

    private static ObjectNode serialiseV3(Message m) {
        ObjectNode mr = MAPPER.createObjectNode();
        mr.put("description", m.description);
        if (m.states.size() == 1) {
            mr.put("providerState", String.valueOf(m.states.get(0).get("name")));
        } else if (m.states.size() > 1) {
            ArrayNode arr = mr.putArray("providerStates");
            for (Map<String, Object> s : m.states) {
                arr.addObject().put("name", String.valueOf(s.get("name")));
            }
        }
        mr.putPOJO("contents", resolveMatchers(m.content));
        if (!m.metadata.isEmpty()) mr.putPOJO("metaData", m.metadata);
        return mr;
    }

    @SuppressWarnings("unchecked")
    static Object resolveMatchers(Object v) {
        // Minimal: matchers in this Java DSL are plain JSON values
        // (no Like()/Regex() wrappers in the Java surface yet — those
        // exist in the consumer DSL in ru.mockarty.pact.Matchers and
        // can be added later by walking and stripping). For now this
        // is identity: the user supplies plain values.
        if (v instanceof Map<?, ?> m) {
            Map<Object, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) out.put(e.getKey(), resolveMatchers(e.getValue()));
            return out;
        }
        if (v instanceof List<?> ls) {
            List<Object> out = new ArrayList<>(ls.size());
            for (Object o : ls) out.add(resolveMatchers(o));
            return out;
        }
        return v;
    }

    private static byte[] encodeBody(Object body, String contentType) {
        if (body == null) return new byte[0];
        if (body instanceof byte[] b) return b;
        if (body instanceof String s) return s.getBytes(StandardCharsets.UTF_8);
        try {
            return MAPPER.writeValueAsBytes(body);
        } catch (IOException e) {
            return ("{\"encodeError\":\"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String sanitise(String s) {
        String safe = UNSAFE_NAME.matcher(s).replaceAll("_");
        return safe.isBlank() ? "pact" : safe;
    }

    /** Plain bean — used by the verifier. */
    public static final class Message {
        public String description = "";
        public String contentType = "";
        public Object content;
        public Map<String, String> metadata = new LinkedHashMap<>();
        public List<Map<String, Object>> states = new ArrayList<>();
    }
}
