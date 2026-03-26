package com.mirror.product.repository.user;

import com.mirror.product.entity.user.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    @Query("SELECT t FROM EmailVerificationToken t WHERE t.user.id = :userId AND t.verifiedAt IS NULL")
    Optional<EmailVerificationToken> findPendingTokenByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId AND t.verifiedAt IS NULL")
    void invalidateAllPendingTokensByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now AND t.verifiedAt IS NULL")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    boolean existsByUserIdAndVerifiedAtIsNull(Long userId);
}
