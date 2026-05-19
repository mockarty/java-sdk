package ru.mockarty.protocols.soap;

/** Unchecked exception for SOAP transport / parse failures. */
public final class SoapException extends RuntimeException {
    public SoapException(String message) { super(message); }
    public SoapException(String message, Throwable cause) { super(message, cause); }
}
