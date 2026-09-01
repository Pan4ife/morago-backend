package org.morago.exception;

public class InvalidCallStateException extends RuntimeException {
    public InvalidCallStateException(String message) {
        super(message);
    }
}