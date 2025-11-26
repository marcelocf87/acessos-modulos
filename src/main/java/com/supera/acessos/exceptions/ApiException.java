package com.supera.acessos.exceptions;

public class ApiException extends RuntimeException {
    public ApiException(String mensagem) {
        super(mensagem);
    }
}