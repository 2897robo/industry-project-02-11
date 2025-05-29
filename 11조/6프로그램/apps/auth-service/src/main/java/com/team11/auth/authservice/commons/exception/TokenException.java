package com.team11.auth.authservice.commons.exception;

import com.team11.auth.authservice.commons.exception.payload.ErrorStatus;

public class TokenException extends ApplicationException {

    public TokenException(ErrorStatus errorStatus) {
        super(errorStatus);
    }
}