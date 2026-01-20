import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import api from "@services/api";
import "./ForgotPassword.css";
import EyeIconSvg from "@assets/images/icons/EyeIcon.svg";
import EyeSlashIconSvg from "@assets/images/icons/EyeSlashIcon.svg";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import { ROUTES } from "@/constants/routes";

const ForgotPassword = () => {
  const navigate = useNavigate();

  // Step state: 1 = Email, 2 = OTP, 3 = New Password
  const [step, setStep] = useState(1);

  // Form data
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState(["", "", "", "", "", ""]);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // OTP input refs
  const otpInputRefs = useRef([]);

  // Password visibility states
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // UI states
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState("");

  // Reset token from step 2
  const [resetToken, setResetToken] = useState("");

  // Resend OTP states
  const [isResending, setIsResending] = useState(false);
  const [resendCountdown, setResendCountdown] = useState(0);
  const countdownIntervalRef = useRef(null);

  // Countdown timer for resend button
  useEffect(() => {
    if (resendCountdown > 0) {
      countdownIntervalRef.current = setInterval(() => {
        setResendCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(countdownIntervalRef.current);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => {
        if (countdownIntervalRef.current) {
          clearInterval(countdownIntervalRef.current);
        }
      };
    }
  }, [resendCountdown]);

  // Validate email
  const validateEmail = (email) => {
    if (!email.trim()) {
      return "Email is required";
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return "Email address is invalid";
    }
    return null;
  };

  // Validate OTP
  const validateOtp = (otp) => {
    const otpString = Array.isArray(otp) ? otp.join("") : otp;
    if (!otpString.trim()) {
      return "OTP is required";
    }
    if (!/^[0-9]{6}$/.test(otpString)) {
      return "OTP must be 6 digits";
    }
    return null;
  };

  // Handle OTP input change
  const handleOtpChange = (index, value) => {
    // Only allow digits
    if (value && !/^\d$/.test(value)) return;

    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);

    // Auto-focus to next input
    if (value && index < 5) {
      otpInputRefs.current[index + 1]?.focus();
    }
  };

  // Handle OTP input keydown
  const handleOtpKeyDown = (index, e) => {
    // Handle backspace
    if (e.key === "Backspace" && !otp[index] && index > 0) {
      otpInputRefs.current[index - 1]?.focus();
    }
  };

  // Handle OTP paste
  const handleOtpPaste = (e) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData("text").slice(0, 6);
    const digits = pastedData.match(/\d/g);

    if (digits) {
      const newOtp = [...otp];
      digits.forEach((digit, i) => {
        if (i < 6) newOtp[i] = digit;
      });
      setOtp(newOtp);

      // Focus the next empty input or last input
      const nextEmptyIndex = newOtp.findIndex(val => !val);
      const focusIndex = nextEmptyIndex === -1 ? 5 : nextEmptyIndex;
      otpInputRefs.current[focusIndex]?.focus();
    }
  };

  // Validate password (same as Register)
  const validatePassword = (password) => {
    const errors = [];

    if (password.length < 8 || password.length > 128) {
      errors.push("Password must be between 8 and 128 characters");
    }
    if (!/[A-Z]/.test(password)) {
      errors.push("Password must contain at least 1 uppercase letter");
    }
    if (!/[a-z]/.test(password)) {
      errors.push("Password must contain at least 1 lowercase letter");
    }
    if (!/\d/.test(password)) {
      errors.push("Password must contain at least 1 digit");
    }
    if (!/[!@#$%^&*(),.?":{}|<>_\-+=\[\]\\\/;'`~]/.test(password)) {
      errors.push("Password must contain at least 1 special character");
    }
    if (/\s/.test(password)) {
      errors.push("Password cannot contain whitespace");
    }
    if (/(.)\1{3,}/.test(password)) {
      errors.push(
        "Password cannot have more than 4 consecutive repeated characters"
      );
    }

    const commonPasswords = [
      "password",
      "123456",
      "password123",
      "admin",
      "qwerty",
      "letmein",
      "welcome",
      "monkey",
      "1234567890",
      "abc123",
    ];
    const lowerPassword = password.toLowerCase();
    if (commonPasswords.some((common) => lowerPassword.includes(common))) {
      errors.push("Password is too common and easily guessable");
    }

    if (
      /abcde|bcdef|cdefg|defgh|efghi|fghij|ghijk|hijkl|ijklm|jklmn|klmno|lmnop|mnopq|nopqr|opqrs|pqrst|qrstu|rstuv|stuvw|tuvwx|uvwxy|vwxyz/i.test(
        password
      )
    ) {
      errors.push(
        "Password cannot contain alphabetical sequences of 5 or more characters"
      );
    }
    if (/01234|12345|23456|34567|45678|56789/.test(password)) {
      errors.push(
        "Password cannot contain numerical sequences of 5 or more characters"
      );
    }
    if (/qwert|werty|asdfg|sdfgh|zxcvb|xcvbn/i.test(password)) {
      errors.push(
        "Password cannot contain keyboard sequences of 5 or more characters"
      );
    }

    return errors;
  };

  // Step 1: Request OTP
  const handleRequestOtp = async (e) => {
    e.preventDefault();
    setErrors({});
    setMessage("");

    const emailError = validateEmail(email);
    if (emailError) {
      setErrors({ email: emailError });
      return;
    }

    setIsLoading(true);

    try {
      await api.post("/api/v1/auth/forgot-password", { email });
      // Don't show message, just proceed to step 2
      setStep(2);
      setResendCountdown(60); // Start countdown
    } catch (error) {
      let errorMessage = "Failed to send OTP. Please try again.";
      if (
        error.response &&
        error.response.data &&
        error.response.data.message
      ) {
        errorMessage = error.response.data.message;
      }
      setErrors({ form: errorMessage });
    } finally {
      setIsLoading(false);
    }
  };

  // Step 2: Verify OTP
  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setErrors({});
    setMessage("");

    const otpError = validateOtp(otp);
    if (otpError) {
      setErrors({ otp: otpError });
      return;
    }

    setIsLoading(true);

    const otpString = otp.join("");

    try {
      const response = await api.post("/api/v1/auth/verify-otp", { email, otp: otpString });
      setResetToken(response.data.resetToken);
      setMessage(response.data.message || "OTP verified successfully");
      setStep(3);
    } catch (error) {
      let errorMessage = "Invalid or expired OTP. Please try again.";
      if (
        error.response &&
        error.response.data &&
        error.response.data.message
      ) {
        errorMessage = error.response.data.message;
      }
      setErrors({ otp: errorMessage });
    } finally {
      setIsLoading(false);
    }
  };

  // Step 3: Reset Password
  const handleResetPassword = async (e) => {
    e.preventDefault();
    setErrors({});
    setMessage("");

    const passwordErrors = validatePassword(newPassword);
    if (passwordErrors.length > 0) {
      setErrors({ password: passwordErrors[0] });
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrors({ confirmPassword: "Passwords do not match" });
      return;
    }

    setIsLoading(true);

    try {
      const response = await api.post("/api/v1/auth/reset-password", {
        resetToken,
        newPassword,
      });
      setMessage(response.data.message || "Password reset successfully!");

      // Redirect to login after 2 seconds
      setTimeout(() => {
        navigate(ROUTES.AUTH_LOGIN);
      }, 2000);
    } catch (error) {
      let errorMessage = "Failed to reset password. Please try again.";
      if (
        error.response &&
        error.response.data &&
        error.response.data.message
      ) {
        errorMessage = error.response.data.message;
      }
      setErrors({ form: errorMessage });
    } finally {
      setIsLoading(false);
    }
  };

  // Resend OTP
  const handleResendOtp = async () => {
    if (resendCountdown > 0 || isResending) return;

    setIsResending(true);
    setMessage("");
    setErrors({});

    try {
      await api.post("/api/v1/auth/resend-password-reset-otp", {
        email,
      });
      // Don't show message, just reset countdown
      setResendCountdown(60);
    } catch (error) {
      let errorMessage = "Failed to resend OTP. Please try again.";
      if (
        error.response &&
        error.response.data &&
        error.response.data.message
      ) {
        errorMessage = error.response.data.message;
      }
      setErrors({ form: errorMessage });
    } finally {
      setIsResending(false);
    }
  };

  return (
    <div className="forgot-password-container" data-navbar-theme="black">
      <div className="forgot-password-wrapper">
        <h1 className="heading-1--no-margin forgot-password-title">
          RESET PASSWORD
        </h1>

        {/* General message */}
        {message && (
          <p
            className={`bodytext-4--no-margin message-box ${
              message.includes("success") ? "success" : "info"
            }`}
          >
            {message}
          </p>
        )}

        {/* General error */}
        {errors.form && (
          <p className="bodytext-4--no-margin message-box error">
            {errors.form}
          </p>
        )}

        {/* Step 1: Enter Email */}
        {step === 1 && (
          <form onSubmit={handleRequestOtp} className="forgot-password-form">
            <p className="bodytext-4--no-margin step-instruction">
              Enter your email address and we'll send you an OTP to reset your
              password.
            </p>

            <div className="form-field-container">
              <div className="input-group">
                <input
                  type="email"
                  id="email"
                  name="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder=" "
                  required
                />
                <label htmlFor="email" className="bodytext-3--no-margin">
                  Email Address*
                </label>
              </div>
              {errors.email && (
                <p className="input-error bodytext-4--no-margin">
                  {errors.email}
                </p>
              )}
            </div>

            <div className="button-wrapper">
              <ShineGlassButton
                theme="light"
                type="submit"
                disabled={isLoading}
                className="submit-button"
              >
                {isLoading ? "Sending..." : "Send OTP"}
              </ShineGlassButton>
            </div>

            <p className="bodytext-3--no-margin back-link-wrapper">
              <a
                onClick={() => navigate(ROUTES.AUTH_LOGIN)}
                className="bodytext-4--no-margin back-link"
              >
                Back to Login
              </a>
            </p>
          </form>
        )}

        {/* Step 2: Enter OTP */}
        {step === 2 && (
          <form onSubmit={handleVerifyOtp} className="forgot-password-form">
            <p className="bodytext-3--no-margin step-instruction">
              We've sent a 6-digit OTP to <strong>{email}</strong>.
              <br />
              Please enter it below.
            </p>

            <div className="form-field-container">
              <div className="otp-input-container">
                {otp.map((digit, index) => (
                  <input
                    key={index}
                    ref={(el) => (otpInputRefs.current[index] = el)}
                    type="text"
                    inputMode="numeric"
                    maxLength="1"
                    value={digit}
                    onChange={(e) => handleOtpChange(index, e.target.value)}
                    onKeyDown={(e) => handleOtpKeyDown(index, e)}
                    onPaste={index === 0 ? handleOtpPaste : undefined}
                    className="otp-input"
                    autoFocus={index === 0}
                  />
                ))}
              </div>
              {errors.otp && (
                <p className="input-error bodytext-4--no-margin">
                  {errors.otp}
                </p>
              )}
            </div>

            <div className="button-wrapper">
              <ShineGlassButton
                theme="light"
                type="submit"
                disabled={isLoading}
                className="submit-button"
              >
                {isLoading ? "Verifying..." : "Verify OTP"}
              </ShineGlassButton>
            </div>

            {/* Resend OTP */}
            <div className="resend-section">
              <p className="bodytext-3--no-margin">Didn't receive the OTP?</p>
              <p className="bodytext-3--no-margin">Please check your spam folder or verify your email address.</p>
              <p className="bodytext-4--no-margin">
                <button
                  type="button"
                  className="resend-button bodytext-4--no-margin"
                  onClick={handleResendOtp}
                  disabled={resendCountdown > 0 || isResending}
                >
                  {isResending
                    ? "Sending..."
                    : resendCountdown > 0
                    ? `Resend OTP (${resendCountdown}s)`
                    : "Resend OTP"}
                </button> or <a
                  onClick={() => {
                    setStep(1);
                    setOtp(["", "", "", "", "", ""]);
                    setErrors({});
                  }}
                  className="bodytext-4--no-margin back-link"
                >
                  Change email
                </a>
              </p>
            </div>

            <p className="bodytext-3--no-margin back-link-wrapper">
              <a
                onClick={() => navigate(ROUTES.AUTH_LOGIN)}
                className="bodytext-3--no-margin back-link"
              >
                Back to login
              </a>
            </p>
          </form>
        )}

        {/* Step 3: Enter New Password */}
        {step === 3 && (
          <form onSubmit={handleResetPassword} className="forgot-password-form">
            <p className="bodytext-3--no-margin step-instruction">
              Enter your new password below.
            </p>

            <div className="form-field-container">
              <div className="input-group password-group">
                <input
                  type={showNewPassword ? "text" : "password"}
                  id="newPassword"
                  name="newPassword"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder=" "
                  required
                />
                <label htmlFor="newPassword" className="bodytext-3--no-margin">
                  New Password*
                </label>
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  aria-label={
                    showNewPassword ? "Hide password" : "Show password"
                  }
                >
                  <img
                    src={showNewPassword ? EyeSlashIconSvg : EyeIconSvg}
                    alt={showNewPassword ? "Hide password" : "Show password"}
                    width="20"
                    height="20"
                  />
                </button>
              </div>
              {errors.password && (
                <p className="input-error bodytext-4--no-margin">
                  {errors.password}
                </p>
              )}
            </div>

            <div className="form-field-container">
              <div className="input-group password-group">
                <input
                  type={showConfirmPassword ? "text" : "password"}
                  id="confirmPassword"
                  name="confirmPassword"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder=" "
                  required
                />
                <label
                  htmlFor="confirmPassword"
                  className="bodytext-3--no-margin"
                >
                  Confirm Password*
                </label>
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  aria-label={
                    showConfirmPassword ? "Hide password" : "Show password"
                  }
                >
                  <img
                    src={showConfirmPassword ? EyeSlashIconSvg : EyeIconSvg}
                    alt={
                      showConfirmPassword ? "Hide password" : "Show password"
                    }
                    width="20"
                    height="20"
                  />
                </button>
              </div>
              {errors.confirmPassword && (
                <p className="input-error bodytext-4--no-margin">
                  {errors.confirmPassword}
                </p>
              )}
            </div>

            <div className="password-requirements">
              <p className="bodytext-5--no-margin">Password requirements</p>
              <ul>
                <li className="bodytext-5--no-margin">
                  No repetition of more than two characters
                </li>
                <li className="bodytext-5--no-margin">
                  One lowercase character
                </li>
                <li className="bodytext-5--no-margin">One number</li>
                <li className="bodytext-5--no-margin">
                  One uppercase character
                </li>
                <li className="bodytext-5--no-margin">
                  At least 1 special character(s)
                </li>
                <li className="bodytext-5--no-margin">10 characters minimum</li>
                <li className="bodytext-5--no-margin">
                  Allowed special character(s) from {`!"#$£€%&()*+,-./:<=>?@[]^_{}~\`¨`}
                </li>
              </ul>
            </div>

            <div className="button-wrapper">
              <ShineGlassButton
                theme="light"
                type="submit"
                disabled={isLoading}
                className="submit-button"
                style={{ width: '300px' }}
              >
                {isLoading ? "Resetting..." : "Reset Password"}
              </ShineGlassButton>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default ForgotPassword;
