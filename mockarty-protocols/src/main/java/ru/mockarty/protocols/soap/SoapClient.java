package ru.mockarty.protocols.soap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.mockarty.protocols.telemetry.NopRecorder;
import ru.mockarty.protocols.telemetry.Step;
import ru.mockarty.protocols.telemetry.StepRecorder;
import ru.mockarty.protocols.telemetry.Telemetry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sync SOAP 1.1 / 1.2 client built on {@link HttpClient}. Uses JDK's
 * built-in DOM parser for response/fault extraction — no lxml.
 *
 * <p>Wraps the user-supplied body XML in
 * {@code <soap:Envelope><soap:Body>…</soap:Body></soap:Envelope>}.
 * The client does NOT introspect WSDL — pass the operation element
 * with its own namespaces directly.
 */
public final class SoapClient implements AutoCloseable {

    private static final String SOAP11_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP12_NS = "http://www.w3.org/2003/05/soap-envelope";

    private final URI url;
    private final String soapAction;
    private final String version;
    private final StepRecorder recorder;
    private final int payloadCap;
    private final Duration timeout;
    private final HttpClient http;
    private final AtomicLong counter = new AtomicLong(0);

    public SoapClient(String url) {
        this(url, opts -> {});
    }

    public SoapClient(String url, java.util.function.Consumer<Options> configure) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("mockarty soap: empty url");
        }
        Options opts = new Options();
        configure.accept(opts);
        if (!"1.1".equals(opts.version) && !"1.2".equals(opts.version)) {
            throw new IllegalArgumentException("mockarty soap: version must be '1.1' or '1.2'");
        }
        this.url = URI.create(url);
        this.soapAction = opts.soapAction;
        this.version = opts.version;
        this.recorder = opts.recorder == null ? NopRecorder.INSTANCE : opts.recorder;
        this.payloadCap = Math.max(0, opts.payloadCap);
        this.timeout = opts.timeout;
        this.http = opts.client == null
            ? HttpClient.newBuilder().connectTimeout(timeout).build()
            : opts.client;
    }

    /** Send one SOAP call. */
    public SoapResponse call(String operation, String bodyXml) {
        return call(operation, bodyXml, null, null);
    }

    public SoapResponse call(String operation, String bodyXml, String soapAction, Map<String, String> extraHeaders) {
        String ns = "1.1".equals(version) ? SOAP11_NS : SOAP12_NS;
        String envelope = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<soap:Envelope xmlns:soap=\"" + ns + "\">"
            + "<soap:Body>" + bodyXml + "</soap:Body>"
            + "</soap:Envelope>";
        byte[] envBytes = envelope.getBytes(StandardCharsets.UTF_8);
        String action = soapAction != null ? soapAction : this.soapAction;
        String contentType = "1.1".equals(version)
            ? "text/xml; charset=utf-8"
            : "application/soap+xml; charset=utf-8";

        HttpRequest.Builder rb = HttpRequest.newBuilder(url)
            .header("Content-Type", contentType)
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofByteArray(envBytes));
        if (action != null && !action.isEmpty()) {
            rb.header("SOAPAction", "\"" + action + "\"");
        }
        if (extraHeaders != null) {
            extraHeaders.forEach(rb::header);
        }

        String stepName = "soap:" + operation;
        Instant started = Instant.now();
        HttpResponse<byte[]> resp;
        try {
            resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            recordStep(stepName, started, Instant.now(), "broken", ex,
                Map.of("operation", operation));
            throw new SoapException("transport: " + ex.getMessage(), ex);
        }
        Instant finished = Instant.now();
        byte[] body = resp.body();
        Map<String, String> fault = extractFault(body, ns);

        String status = "passed";
        String message = "";
        if (resp.statusCode() >= 400) {
            status = "failed";
            message = "HTTP " + resp.statusCode();
        }
        if (fault != null) {
            status = "failed";
            message = fault.getOrDefault("string", fault.getOrDefault("code", "soap fault"));
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("operation", operation);
        params.put("http_status", String.valueOf(resp.statusCode()));
        params.put("request", Telemetry.capPreview(envelope, payloadCap));
        params.put("response", Telemetry.capPreview(new String(body, StandardCharsets.UTF_8), payloadCap));
        if (fault != null) {
            fault.forEach((k, v) -> params.put("fault_" + k, v));
        }
        recordStep(stepName, started, finished, status,
            "passed".equals(status) ? null : new RuntimeException(message), params);

        Map<String, String> headers = new HashMap<>();
        resp.headers().map().forEach((k, vs) -> headers.put(k.toLowerCase(), vs.isEmpty() ? "" : vs.get(0)));
        return new SoapResponse(resp.statusCode(), headers, body, fault);
    }

    private void recordStep(String name, Instant started, Instant finished,
                            String status, Throwable err, Map<String, String> params) {
        Step.Builder b = Step.builder()
            .key(Telemetry.newStepKey(name, counter.incrementAndGet()))
            .name(name)
            .status(status)
            .startedAt(started)
            .finishedAt(finished)
            .durationMs(Math.max(0, finished.toEpochMilli() - started.toEpochMilli()))
            .parameters(params);
        if (err != null) {
            b.message(err.getMessage() == null ? err.getClass().getSimpleName() : err.getMessage());
        }
        recorder.record(b.build());
    }

    @Override
    public void close() { /* HttpClient is GC-managed in JDK 11+. */ }

    static Map<String, String> extractFault(byte[] body, String ns) {
        if (body == null || body.length == 0) return null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            // Disable XXE for safety even in test code.
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(body));
            Element fault = firstByLocalName(doc.getDocumentElement(), "Fault");
            if (fault == null) return null;
            Map<String, String> out = new LinkedHashMap<>();
            if (SOAP11_NS.equals(ns)) {
                Optional.ofNullable(firstChildText(fault, "faultcode")).ifPresent(v -> out.put("code", v));
                Optional.ofNullable(firstChildText(fault, "faultstring")).ifPresent(v -> out.put("string", v));
            } else {
                Element code = firstByLocalName(fault, "Code");
                Element reason = firstByLocalName(fault, "Reason");
                if (code != null) {
                    Element value = firstByLocalName(code, "Value");
                    if (value != null && value.getTextContent() != null) out.put("code", value.getTextContent());
                }
                if (reason != null) {
                    Element text = firstByLocalName(reason, "Text");
                    if (text != null && text.getTextContent() != null) out.put("string", text.getTextContent());
                }
            }
            return out.isEmpty() ? Map.of("code", "fault") : out;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Element firstByLocalName(Element parent, String localName) {
        if (parent == null) return null;
        NodeList all = parent.getElementsByTagNameNS("*", localName);
        return all.getLength() == 0 ? null : (Element) all.item(0);
    }

    private static String firstChildText(Element parent, String name) {
        if (parent == null) return null;
        NodeList kids = parent.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && name.equals(n.getNodeName())) {
                return n.getTextContent();
            }
        }
        return null;
    }

    /** Fluent options bag for {@link SoapClient}. */
    public static final class Options {
        private String soapAction = "";
        private String version = "1.1";
        private StepRecorder recorder = NopRecorder.INSTANCE;
        private int payloadCap = 1024;
        private Duration timeout = Duration.ofSeconds(30);
        private HttpClient client;

        public Options soapAction(String a) { this.soapAction = a == null ? "" : a; return this; }
        public Options version(String v) { this.version = v; return this; }
        public Options recorder(StepRecorder r) { this.recorder = r == null ? NopRecorder.INSTANCE : r; return this; }
        public Options payloadCap(int n) { this.payloadCap = Math.max(0, n); return this; }
        public Options timeout(Duration d) { if (d != null && !d.isZero() && !d.isNegative()) this.timeout = d; return this; }
        public Options client(HttpClient c) { this.client = c; return this; }
    }
}
