package com.team11.user.userservice.infrastructure.fallback;

import com.team11.user.userservice.commons.exception.ApplicationException;
import com.team11.user.userservice.commons.exception.payload.ErrorStatus;
import com.team11.user.userservice.infrastructure.adapter.AuthAdapter;

import java.time.LocalDateTime;

public class AuthFallBack implements AuthAdapter {

    @Override
    public String loginByRefreshToken() {
        throw new ApplicationException(
                ErrorStatus.toErrorStatus("인증 서버 오류", 500, LocalDateTime.now())
        );
    }
}
