package com.example.auth_service.repository;

import com.example.auth_service.entity.SubscriptionPlan;
import com.example.auth_service.entity.UserRole;
import com.example.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByIdAndActiveTrue(Long id);

    List<User> findAllByRole(UserRole role);

    List<User> findBySubscriptionPlan(SubscriptionPlan subscriptionPlan);

    List<User> findByActive(boolean active);
}
