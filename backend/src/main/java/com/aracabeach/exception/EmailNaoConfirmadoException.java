package com.aracabeach.exception;

/** Login de uma conta do portal cujo e-mail ainda nao foi confirmado (HTTP 403). */
public class EmailNaoConfirmadoException extends RuntimeException {
    public EmailNaoConfirmadoException(String message) {
        super(message);
    }
}
