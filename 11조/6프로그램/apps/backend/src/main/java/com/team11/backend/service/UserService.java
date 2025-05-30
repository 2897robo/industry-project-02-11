package com.team11.backend.service;

import com.team11.backend.domain.user.User;
import com.team11.backend.repository.user.UserRepository;
import com.team11.backend.service.dto.ReadUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public ReadUserResponse getByUidAndPassword(String uid, String password) {

        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        if(!passwordEncoder.matches(user.getPasswordHash(), password)) {
            throw new RuntimeException("비밀번호가 맞지 않습니다.");
        }

        return ReadUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public ReadUserResponse getByUid(String uid) {

        User user =userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        return ReadUserResponse.from(user);
    }
}
