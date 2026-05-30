// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SOAP facet. Mirrors {@code sdk/go-sdk/tester/soap.go} and the Python
 * port. Uses javax.xml + javax.xml.xpath (stdlib JDK 11+) — no Jackson
 * XML or extra deps. Accepts Go-style {@code //*[local-name()='X']}
 * probes directly because that's a valid XPath 1.0 expression.
 */
public final class SOAPFacet {

    private final Tester t;
    private final String endpoint;
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    SOAPFacet(Tester t, String endpoint) {
        this.t = t;
        this.endpoint = endpoint;
    }

    public SOAPStep call(String action, String body) {
        t.flushPending();
        Map<String, String> v = t.snapshotVars();
        SOAPStep s = new SOAPStep(t,
                Interpolate.apply(endpoint, v),
                Interpolate.apply(action, v),
                wrapEnvelope(Interpolate.apply(body, v)));
        t.setPending(s);
        return s;
    }

    private static String wrapEnvelope(String body) {
        String trimmed = body.trim();
        if (trimmed.startsWith("<?xml") || trimmed.contains(":Envelope")) {
            return body;
        }
        return "<?xml version=\"1.0\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>" + body + "</soap:Body>"
                + "</soap:Envelope>";
    }

    public static final class SOAPStep implements Committable {
        private final Tester t;
        private final String endpoint;
        private final String action;
        private final String body;
        private final Map<String, String> headers = new HashMap<>();
        private HttpResponse<byte[]> resp;
        private Document doc;
        private boolean sent;
        private boolean committed;
        private boolean abortChain;
        private Instant startedAt = Instant.EPOCH;
        private Instant endedAt = Instant.EPOCH;
        private final List<String> failures = new ArrayList<>();

        SOAPStep(Tester t, String endpoint, String action, String body) {
            this.t = t; this.endpoint = endpoint; this.action = action; this.body = body;
        }

        public SOAPStep header(String k, String v) {
            if (sent) { return fail("header() after send"); }
            headers.put(k, Interpolate.apply(v, t.snapshotVars()));
            return this;
        }

        public SOAPStep expectStatus(int code) {
            if (!ensureSent()) { return this; }
            if (resp != null && resp.statusCode() != code) {
                fail("expectStatus: want " + code + ", got " + resp.statusCode());
            }
            return this;
        }

        public SOAPStep expectXPath(String xpathExpr, Object want) {
            if (!ensureSent()) { return this; }
            String got = evalXPath(xpathExpr);
            if (got == null) {
                return fail("expectXPath " + xpathExpr + ": no match");
            }
            String wantStr = String.valueOf(want);
            if (!got.equals(wantStr)) {
                fail("expectXPath " + xpathExpr + ": want \"" + wantStr + "\", got \"" + got + "\"");
            }
            return this;
        }

        public SOAPStep expectXPathContains(String xpathExpr, String sub) {
            if (!ensureSent()) { return this; }
            String got = evalXPath(xpathExpr);
            if (got == null) {
                return fail("expectXPathContains " + xpathExpr + ": no match");
            }
            if (!got.contains(sub)) {
                fail("expectXPathContains " + xpathExpr + ": \"" + sub + "\" not found in \"" + got + "\"");
            }
            return this;
        }

        public SOAPStep expectNoFault() {
            if (!ensureSent()) { return this; }
            if (doc == null) { return this; }
            Element f = findLocal("Fault");
            if (f != null) {
                String code = textOfLocal(f, "faultcode");
                String msg = textOfLocal(f, "faultstring");
                fail("expectNoFault: " + (code == null ? "" : code) + " — " + (msg == null ? "" : msg));
            }
            return this;
        }

        public SOAPStep expectFault(String faultCode) {
            if (!ensureSent()) { return this; }
            if (doc == null) { return fail("expectFault: no response"); }
            Element f = findLocal("Fault");
            if (f == null) { return fail("expectFault: no <Fault> in response"); }
            if (faultCode == null || faultCode.isEmpty()) { return this; }
            String code = textOfLocal(f, "faultcode");
            if (code == null || !code.contains(faultCode)) {
                fail("expectFault: want code \"" + faultCode + "\", got \"" + (code == null ? "" : code) + "\"");
            }
            return this;
        }

        public SOAPStep extract(String xpathExpr, String name) {
            if (!ensureSent()) { return this; }
            String got = evalXPath(xpathExpr);
            if (got == null) { return fail("extract " + xpathExpr + ": no match"); }
            t.setVar(name, got);
            return this;
        }

        public byte[] responseBody() {
            ensureSent();
            return resp != null ? resp.body() : new byte[0];
        }

        public Tester done() {
            commit();
            t.clearPending(this);
            return t;
        }

        private SOAPStep fail(String msg) { failures.add(msg); return this; }

        private String evalXPath(String expr) {
            if (doc == null) { return null; }
            try {
                XPath xp = XPATH_FACTORY.newXPath();
                XPathExpression e = xp.compile(expr);
                Object out = e.evaluate(doc, XPathConstants.STRING);
                String s = out == null ? "" : String.valueOf(out).trim();
                if (s.isEmpty()) {
                    // STRING returns "" when the node doesn't exist; check
                    // NODE explicitly to differentiate empty-text from missing.
                    Object nodeOut = e.evaluate(doc, XPathConstants.NODE);
                    if (nodeOut == null) { return null; }
                }
                return s;
            } catch (XPathExpressionException ex) {
                return null;
            }
        }

        private Element findLocal(String localName) {
            if (doc == null) { return null; }
            NodeList list = doc.getElementsByTagNameNS("*", localName);
            return list.getLength() == 0 ? null : (Element) list.item(0);
        }

        private static String textOfLocal(Element parent, String localName) {
            NodeList list = parent.getElementsByTagNameNS("*", localName);
            if (list.getLength() == 0) { return null; }
            Node n = list.item(0);
            return n.getTextContent() == null ? null : n.getTextContent().trim();
        }

        private boolean ensureSent() {
            if (sent) { return !abortChain; }
            sent = true;
            if (t.shouldAbort()) {
                abortChain = true;
                fail("skipped: fail-fast triggered by earlier step");
                return false;
            }

            String url = endpoint;
            if (!url.startsWith("http://") && !url.startsWith("https://") && !t.baseUrl().isEmpty()) {
                if (!url.startsWith("/")) { url = "/" + url; }
                url = t.baseUrl() + url;
            }

            HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url));
            b.header("Content-Type", "text/xml; charset=utf-8");
            b.header("SOAPAction", action);
            for (Map.Entry<String, String> e : t.defaultHeaders().entrySet()) {
                if (!headers.containsKey(e.getKey())) {
                    b.header(e.getKey(), e.getValue());
                }
            }
            for (Map.Entry<String, String> e : headers.entrySet()) {
                b.header(e.getKey(), e.getValue());
            }
            b.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

            try {
                startedAt = Instant.now();
                resp = t.http2().send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
                endedAt = Instant.now();
            } catch (Exception e) {
                endedAt = Instant.now();
                fail("soap: " + e.getMessage());
                abortChain = true;
                return false;
            }
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                // Harden against XXE — disable external entities.
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                doc = db.parse(new ByteArrayInputStream(resp.body()));
            } catch (ParserConfigurationException | java.io.IOException
                     | org.xml.sax.SAXException e) {
                fail("soap: parse XML: " + e.getMessage());
                return true;
            }
            return true;
        }

        @Override
        public void commit() {
            if (committed) { return; }
            committed = true;
            if (!sent) { ensureSent(); }
            StepRecord rec = new StepRecord();
            rec.protocol = "soap";
            rec.method = "POST";
            rec.name = "soap " + action;
            rec.url = endpoint;
            rec.statusOrCode = resp == null ? 0 : resp.statusCode();
            rec.startedAt = startedAt;
            rec.endedAt = endedAt;
            rec.failures.addAll(failures);
            t.recordStep(rec);
        }
    }
}
