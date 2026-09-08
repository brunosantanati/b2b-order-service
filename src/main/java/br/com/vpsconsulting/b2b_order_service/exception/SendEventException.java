package br.com.vpsconsulting.b2b_order_service.exception;

public class SendEventException extends RuntimeException {
    public SendEventException(String message, Throwable cause) {
        super(message, cause);
    }
}