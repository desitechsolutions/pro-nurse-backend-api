package com.pronurse.auth.repository;

import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Core lookup used during OTP verification handshake and token authentication queries.
     */
    Optional<User> findByMobile(String mobile);

    /**
     * Guard metric to quickly block duplicate signups using pre-existing phone profiles.
     */
    boolean existsByMobile(String mobile);

    /**
     * Guard metric used during onboarding validation checks when post-login profile fields are submitted.
     */
    boolean existsByEmail(String email);

    /**
     * Secondary lookup if you need to fetch users via email context at any stage.
     */
    Optional<User> findByEmail(String email);

    /**
     * Used mainly for system administration queries or matching specific demographic scopes.
     */
    List<User> findByRole(Role role);
}