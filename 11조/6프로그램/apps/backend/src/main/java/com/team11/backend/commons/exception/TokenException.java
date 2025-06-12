package com.team11.backend.commons.exception;

import com.team11.backend.commons.exception.payload.ErrorStatus;

public class TokenException extends ApplicationException {

    public TokenException(ErrorStatus errorStatus) {
        super(errorStatus);
    }
}