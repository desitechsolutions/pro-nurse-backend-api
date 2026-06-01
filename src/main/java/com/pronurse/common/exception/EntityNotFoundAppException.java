package com.pronurse.common.exception;

public class EntityNotFoundAppException extends ApplicationException {
    public EntityNotFoundAppException(String entity, Object id) {
        super(entity + " not found with ID: " + id);
    }
}
