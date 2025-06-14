package com.team11.user.userservice.application.service;

import com.team11.user.userservice.persistence.domain.User;
import com.team11.user.userservice.presentation.dto.request.CreateUserRequest;
import com.team11.user.userservice.presentation.dto.request.UpdateUserRequest;
import com.team11.user.userservice.presentation.dto.response.ReadUserResponse;
import com.team11.user.userservice.persistence.repository.UserRepository;
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

        if(!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("비밀번호가 맞지 않습니다.");
        }

        return ReadUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public ReadUserResponse getByUid(String uid) {

        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        return ReadUserResponse.from(user);
    }

    public void createUser(CreateUserRequest request) {
        userRepository.save(request.toEntity(passwordEncoder));
    }

    public boolean confirmDuplicate(String uid) {
        return userRepository.existsByUid(uid);
    }

    public void updateUser(String uid, UpdateUserRequest request) {

        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        if(request.name() != null && !request.name().equals(user.getName())) {
            user.updateName(request.name());
        }

        if(request.email() != null && !request.email().equals(user.getEmail())) {
            user.updateEmail(request.email());
        }

        if(request.password() != null) {
            user.updatePassword(passwordEncoder.encode(request.password()));
        }
    }
}
