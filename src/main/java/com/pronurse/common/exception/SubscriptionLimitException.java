package com.pronurse.common.exception;

public class SubscriptionLimitException extends ApplicationException {
    public SubscriptionLimitException(String message) {
        super(message);
    }
}