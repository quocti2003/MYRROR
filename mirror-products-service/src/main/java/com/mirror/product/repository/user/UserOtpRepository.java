package com.mirror.product.repository.user;

import com.mirror.product.entity.user.OtpType;
import com.mirror.product.entity.user.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserOtpRepository extends JpaRepository<UserOtp, Long> {

    /**
     * Find pending OTP by email and type
     */
    Optional<UserOtp> findByEmailAndTypeAndVerifiedAtIsNull(String email, OtpType type);

    /**
     * Find pending OTP by user ID and type
     */
    Optional<UserOtp> findByUserIdAndTypeAndVerifiedAtIsNull(Long userId, OtpType type);

    /**
     * Invalidate all pending OTPs for a user by type
     */
    @Modifying
    @Query("DELETE FROM UserOtp o WHERE o.user.id = :userId AND o.type = :type AND o.verifiedAt IS NULL")
    void invalidateAllPendingOtpsByUserIdAndType(@Param("userId") Long userId, @Param("type") OtpType type);

    /**
     * Delete all expired OTPs (for cleanup job)
     * Calculates expiration as: created_at + expiration_seconds
     */
    @Modifying
    @Query("DELETE FROM UserOtp o WHERE FUNCTION('TIMESTAMPADD', SECOND, o.expirationSeconds, o.createdAt) < :now AND o.verifiedAt IS NULL")
    int deleteExpiredOtps(@Param("now") LocalDateTime now);

    /**
     * Count OTPs created by email and type after a certain time (for rate limiting)
     */
    int countByEmailAndTypeAndCreatedAtAfter(String email, OtpType type, LocalDateTime since);

    /**
     * Count pending (unverified and unexpired) OTPs created by email and type after a certain time
     * This is used for rate limiting to only count active OTP requests
     */
    @Query("SELECT COUNT(o) FROM UserOtp o WHERE o.email = :email AND o.type = :type " +
           "AND o.createdAt > :since AND o.verifiedAt IS NULL " +
           "AND FUNCTION('TIMESTAMPADD', SECOND, o.expirationSeconds, o.createdAt) > :now")
    int countPendingOtpsByEmailAndTypeAndCreatedAtAfter(
            @Param("email") String email,
            @Param("type") OtpType type,
            @Param("since") LocalDateTime since,
            @Param("now") LocalDateTime now);

    /**
     * Check if user has pending OTP of a certain type
     */
    boolean existsByUserIdAndTypeAndVerifiedAtIsNull(Long userId, OtpType type);
}
