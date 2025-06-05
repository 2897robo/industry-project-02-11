package com.team11.auth.authservice.infrastructure.fallback;

import com.team11.auth.authservice.infrastructure.dto.ReadUserResponse;
import com.team11.auth.authservice.infrastructure.adapter.UserAdapter;

import java.time.LocalDateTime;

public class UserFallBack implements UserAdapter {

    @Override
    public ReadUserResponse findByUidAndPassword(String uid, String password) {
        return new ReadUserResponse(0L ,"test","test", LocalDateTime.now());
    }

    @Override
    public ReadUserResponse findByUid() {
        return new ReadUserResponse(0L ,"test","test", LocalDateTime.now());
    }
}
