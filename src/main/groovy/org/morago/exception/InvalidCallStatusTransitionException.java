package org.morago.exception;

public class InvalidCallStatusTransitionException extends RuntimeException {
    public InvalidCallStatusTransitionException(String message) {
        super(message);
    }
}
