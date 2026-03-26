package com.mirror.product.entity.user;

/**
 * Types of OTP for different purposes
 */
public enum OtpType {
    /**
     * OTP for password reset flow
     */
    FORGOT_PASSWORD,

    /**
     * OTP for two-factor authentication (future use)
     */
    TWO_FACTOR_AUTH,

    /**
     * OTP for email change verification (future use)
     */
    EMAIL_CHANGE,

    /**
     * OTP for phone number verification (future use)
     */
    PHONE_VERIFICATION
}
