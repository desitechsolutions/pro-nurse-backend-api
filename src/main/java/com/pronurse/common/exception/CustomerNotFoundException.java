package com.pronurse.common.exception;

public class CustomerNotFoundException extends ApplicationException{
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
