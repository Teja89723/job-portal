package com.jobportal.repository;

import com.jobportal.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findTopByEmailOrderByIdDesc(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerificationToken e WHERE e.email = :email")
    void deleteByEmail(String email);
}