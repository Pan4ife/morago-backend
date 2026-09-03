package org.morago.exception;

public class CallNotFoundException extends RuntimeException {
    public CallNotFoundException(String message) {
        super(message);
    }
}