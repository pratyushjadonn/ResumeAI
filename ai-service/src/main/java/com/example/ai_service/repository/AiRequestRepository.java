package com.example.ai_service.repository;

import com.example.ai_service.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface AiRequestRepository extends JpaRepository<AiRequest, Long> {

    long countByUserIdAndRequestTypeAndCreatedAtBetween(Long userId,
                                                        String requestType,
                                                        Instant from,
                                                        Instant to);

    long countByUserIdAndRequestTypeInAndCreatedAtBetween(Long userId,
                                                          Collection<String> requestTypes,
                                                          Instant from,
                                                          Instant to);

    List<AiRequest> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
}
