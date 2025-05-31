package com.team11.backend.commons.advice;

import com.team11.backend.commons.exception.ApplicationException;
import com.team11.backend.commons.exception.payload.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalRestControllerAdvice {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorStatus> handleException(ApplicationException e) {

        ErrorStatus errorStatus = e.getErrorStatus();

        return new ResponseEntity<>(errorStatus, errorStatus.toHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorStatus> handleException(Exception e) {

        log.info("{}", e.getMessage());

        ErrorStatus errorStatus = ErrorStatus.toErrorStatus("알 수 없는 문제가 발생하였습니다.", 500, LocalDateTime.now());

        return new ResponseEntity<>(errorStatus, errorStatus.toHttpStatus());
    }
}

