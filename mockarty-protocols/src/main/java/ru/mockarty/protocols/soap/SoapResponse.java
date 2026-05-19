package ru.mockarty.protocols.soap;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/** Parsed SOAP response. */
public final class SoapResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final byte[] body;
    private final Map<String, String> fault;

    SoapResponse(int statusCode, Map<String, String> headers, byte[] body, Map<String, String> fault) {
        this.statusCode = statusCode;
        this.headers = Map.copyOf(headers);
        this.body = body == null ? new byte[0] : body;
        this.fault = fault == null ? null : Map.copyOf(fault);
    }

    public int getStatusCode() { return statusCode; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }
    public Map<String, String> getFault() { return fault; }

    public String getText() { return new String(body, StandardCharsets.UTF_8); }

    /** Parse the body as a DOM Document — useful for XPath-style
     *  descents in test assertions. Returns empty when the body is
     *  empty or unparseable. */
    public Optional<Document> getDocument() {
        if (body.length == 0) return Optional.empty();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return Optional.of(dbf.newDocumentBuilder().parse(new ByteArrayInputStream(body)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
