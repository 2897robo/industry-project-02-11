package com.team11.user.userservice.commons.exception;


import com.team11.user.userservice.commons.exception.payload.ErrorStatus;

public class TokenException extends ApplicationException {

    public TokenException(ErrorStatus errorStatus) {
        super(errorStatus);
    }
}