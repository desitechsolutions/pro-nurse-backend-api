package com.pronurse.notification.repository;

import com.pronurse.notification.entity.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    
    List<FCMToken> findByUserIdAndIsActiveTrue(Long userId);
    
    Optional<FCMToken> findByDeviceToken(String deviceToken);
    
    @Modifying
    @Query("UPDATE FCMToken f SET f.isActive = false WHERE f.user.id = :userId AND f.deviceToken != :currentToken")
    void deactivateOtherTokens(@Param("userId") Long userId, @Param("currentToken") String currentToken);
    
    @Modifying
    @Query("DELETE FROM FCMToken f WHERE f.user.id = :userId AND f.isActive = false")
    void deleteInactiveTokensByUserId(@Param("userId") Long userId);
}


