package com.team11.auth.authservice.commons.exception;

import com.team11.auth.authservice.commons.exception.payload.ErrorStatus;
import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {
    private final ErrorStatus errorStatus;

    public ApplicationException(ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
    }
}
