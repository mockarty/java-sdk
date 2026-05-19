package ru.mockarty.protocols.soap;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SoapFaultParseTest {

    private static final String SOAP11_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP12_NS = "http://www.w3.org/2003/05/soap-envelope";

    @Test
    void soap11FaultExtracted() {
        String body = "<soap:Envelope xmlns:soap=\"" + SOAP11_NS + "\">"
            + "<soap:Body><soap:Fault>"
            + "<faultcode>Server</faultcode>"
            + "<faultstring>bad input</faultstring>"
            + "</soap:Fault></soap:Body></soap:Envelope>";
        Map<String, String> fault = SoapClient.extractFault(body.getBytes(StandardCharsets.UTF_8), SOAP11_NS);
        assertNotNull(fault);
        assertEquals("Server", fault.get("code"));
        assertEquals("bad input", fault.get("string"));
    }

    @Test
    void soap12FaultExtracted() {
        String body = "<soap:Envelope xmlns:soap=\"" + SOAP12_NS + "\">"
            + "<soap:Body><soap:Fault>"
            + "<soap:Code><soap:Value>Sender</soap:Value></soap:Code>"
            + "<soap:Reason><soap:Text>oops</soap:Text></soap:Reason>"
            + "</soap:Fault></soap:Body></soap:Envelope>";
        Map<String, String> fault = SoapClient.extractFault(body.getBytes(StandardCharsets.UTF_8), SOAP12_NS);
        assertNotNull(fault);
        assertEquals("Sender", fault.get("code"));
        assertEquals("oops", fault.get("string"));
    }

    @Test
    void okResponseReturnsNullFault() {
        String body = "<soap:Envelope xmlns:soap=\"" + SOAP11_NS + "\">"
            + "<soap:Body><GetUserResponse><id>u-1</id></GetUserResponse></soap:Body></soap:Envelope>";
        assertNull(SoapClient.extractFault(body.getBytes(StandardCharsets.UTF_8), SOAP11_NS));
    }

    @Test
    void emptyBodyReturnsNullFault() {
        assertNull(SoapClient.extractFault(new byte[0], SOAP11_NS));
        assertNull(SoapClient.extractFault(null, SOAP11_NS));
    }

    @Test
    void malformedXmlReturnsNullFault() {
        assertNull(SoapClient.extractFault("not-xml".getBytes(StandardCharsets.UTF_8), SOAP11_NS));
    }

    @Test
    void emptyUrlRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SoapClient(""));
    }

    @Test
    void badVersionRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new SoapClient("http://x", o -> o.version("2.0")));
    }
}
