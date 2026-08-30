package br.com.vpsconsulting.b2b_order_service.exception;

public class EventSerializationException extends RuntimeException {
    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}