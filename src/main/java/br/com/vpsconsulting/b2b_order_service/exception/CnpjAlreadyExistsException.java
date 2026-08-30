package br.com.vpsconsulting.b2b_order_service.exception;

public class CnpjAlreadyExistsException extends RuntimeException {
    public CnpjAlreadyExistsException(String message) {
        super(message);
    }
}