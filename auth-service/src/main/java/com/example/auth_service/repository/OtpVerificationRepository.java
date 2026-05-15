package com.example.auth_service.repository;

import com.example.auth_service.entity.OtpType;
import com.example.auth_service.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc(String email, OtpType type);

    List<OtpVerification> findAllByEmailIgnoreCaseAndTypeAndUsedFalse(String email, OtpType type);

    long deleteByUsedTrueAndCreatedAtBefore(Instant cutoff);

    long deleteByExpiryTimeBefore(Instant now);
}
