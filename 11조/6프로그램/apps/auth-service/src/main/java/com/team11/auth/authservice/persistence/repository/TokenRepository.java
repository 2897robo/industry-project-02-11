package com.team11.auth.authservice.persistence.repository;

import com.team11.auth.authservice.persistence.domain.Token;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TokenRepository extends CrudRepository<Token, String> {
    Optional<Token> findByAccessToken(String accessToken);
}
