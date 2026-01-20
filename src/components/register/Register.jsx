import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import api from "@services/api";
import "./Register.css";
import EyeIconSvg from "@assets/images/icons/EyeIcon.svg";
import EyeSlashIconSvg from "@assets/images/icons/EyeSlashIcon.svg";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import { ROUTES } from "@/constants/routes";

const Register = () => {
  const navigate = useNavigate();
  const titles = ["Ms", "Mrs", "Mr"];

  const [formData, setFormData] = useState({
    title: "Ms",
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [registrationSuccess, setRegistrationSuccess] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState("");

  // Resend email states
  const [isResending, setIsResending] = useState(false);
  const [resendMessage, setResendMessage] = useState("");
  const [resendCountdown, setResendCountdown] = useState(0);
  const countdownIntervalRef = useRef(null);

  const validatePassword = (password) => {
    // Validate password according to backend PasswordValidationService.java rules
    const errors = [];

    // Length check (8-128 characters)
    if (password.length < 8 || password.length > 128) {
      errors.push("Password must be between 8 and 128 characters");
    }

    // Must contain at least 1 uppercase letter
    if (!/[A-Z]/.test(password)) {
      errors.push("Password must contain at least 1 uppercase letter");
    }

    // Must contain at least 1 lowercase letter
    if (!/[a-z]/.test(password)) {
      errors.push("Password must contain at least 1 lowercase letter");
    }

    // Must contain at least 1 digit
    if (!/\d/.test(password)) {
      errors.push("Password must contain at least 1 digit");
    }

    // Must contain at least 1 special character
    if (!/[!@#$%^&*(),.?":{}|<>_\-+=\[\]\\\/;'`~]/.test(password)) {
      errors.push("Password must contain at least 1 special character");
    }

    // No whitespace allowed
    if (/\s/.test(password)) {
      errors.push("Password cannot contain whitespace");
    }

    // No more than 4 consecutive repeated characters
    if (/(.)\1{3,}/.test(password)) {
      errors.push(
        "Password cannot have more than 4 consecutive repeated characters"
      );
    }

    // Check for common passwords
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

    // Check for illegal sequences (5+ consecutive characters)
    // Alphabetical sequences
    if (
      /abcde|bcdef|cdefg|defgh|efghi|fghij|ghijk|hijkl|ijklm|jklmn|klmno|lmnop|mnopq|nopqr|opqrs|pqrst|qrstu|rstuv|stuvw|tuvwx|uvwxy|vwxyz/i.test(
        password
      )
    ) {
      errors.push(
        "Password cannot contain alphabetical sequences of 5 or more characters"
      );
    }

    // Numerical sequences
    if (/01234|12345|23456|34567|45678|56789/.test(password)) {
      errors.push(
        "Password cannot contain numerical sequences of 5 or more characters"
      );
    }

    // QWERTY sequences
    if (/qwert|werty|asdfg|sdfgh|zxcvb|xcvbn/i.test(password)) {
      errors.push(
        "Password cannot contain keyboard sequences of 5 or more characters"
      );
    }

    return errors;
  };

  const validateForm = () => {
    const newErrors = {};
    const { firstName, lastName, username, email, password, confirmPassword } =
      formData;

    if (!firstName.trim()) newErrors.firstName = "First Name is required";
    if (!lastName.trim()) newErrors.lastName = "Last Name is required";

    // Username validation (3-50 characters)
    if (!username.trim()) {
      newErrors.username = "Username is required";
    } else if (username.trim().length < 3 || username.trim().length > 50) {
      newErrors.username = "Username must be between 3 and 50 characters";
    }

    if (!email.trim()) {
      newErrors.email = "Email Address is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = "Email address is invalid";
    }

    if (!password) {
      newErrors.password = "Password is required";
    } else {
      const passwordErrors = validatePassword(password);
      if (passwordErrors.length > 0) {
        newErrors.password = passwordErrors[0]; // Show first error
      }
    }

    if (!confirmPassword) {
      newErrors.confirmPassword = "Please confirm your password";
    } else if (password && password !== confirmPassword) {
      newErrors.confirmPassword = "Passwords do not match";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

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

  // Handle resend verification email
  const handleResendEmail = async () => {
    if (resendCountdown > 0 || isResending) return;

    setIsResending(true);
    setResendMessage("");

    try {
      await api.post("/api/v1/auth/resend-verification-email", {
        email: registeredEmail,
      });

      setResendMessage(
        "Verification email sent successfully! Please check your inbox."
      );
      setResendCountdown(60); // 60 seconds cooldown
    } catch (error) {
      let errorMessage = "Failed to resend email. Please try again later.";

      if (
        error.response &&
        error.response.data &&
        error.response.data.message
      ) {
        errorMessage = error.response.data.message;
      }

      setResendMessage(errorMessage);
    } finally {
      setIsResending(false);
    }
  };

  // --- BƯỚC 3: CẬP NHẬT HOÀN CHỈNH HÀM SUBMIT ---
  const handleSubmit = async (e) => {
    e.preventDefault();
    // Thực hiện validation phía client trước
    if (!validateForm()) {
      // Scroll to top to show errors
      window.scrollTo({ top: 0, behavior: 'smooth' });
      setErrors(prev => ({
        ...prev,
        form: "Please fix the errors above before submitting."
      }));
      return;
    }

    setIsLoading(true);
    setErrors({}); // Xóa lỗi cũ trước khi gửi

    // Tạo payload để gửi đi, chỉ chứa các trường backend cần
    const payload = {
      title: formData.title,
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      username: formData.username.trim(),
      email: formData.email.trim(),
      password: formData.password,
      // phoneNumber là optional, có thể thêm vào nếu form có
    };

    try {
      await api.post("/api/v1/auth/register", payload);

      // Lưu email và hiển thị thông báo kiểm tra email
      setRegisteredEmail(formData.email);
      setRegistrationSuccess(true);
    } catch (error) {
      // Xử lý khi thất bại
      let errorMessage = "Registration failed. Please try again.";

      // Lấy thông báo lỗi cụ thể từ response của backend
      if (
        error.response &&
        error.response.data &&
        error.response.data.message
      ) {
        errorMessage = error.response.data.message;

        // Hiển thị lỗi ngay tại trường input tương ứng
        if (errorMessage.toLowerCase().includes("username")) {
          setErrors({ username: errorMessage });
        } else if (errorMessage.toLowerCase().includes("email")) {
          setErrors({ email: errorMessage });
        } else if (errorMessage.toLowerCase().includes("password")) {
          setErrors({ password: errorMessage });
        } else {
          // Lỗi chung không thuộc trường nào
          setErrors({ form: errorMessage });
        }
      } else {
        // Lỗi mạng hoặc lỗi không xác định
        console.error("Registration failed:", error);
        setErrors({
          form: "Cannot connect to the server. Please try again later.",
        });
      }
    } finally {
      setIsLoading(false); // Luôn dừng loading sau khi hoàn tất
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;

    // Nếu là firstName hoặc lastName, chỉ cho phép chữ cái không dấu và khoảng trắng
    let processedValue = value;
    if (name === 'firstName' || name === 'lastName') {
      processedValue = value.replace(/[^a-zA-Z\s]/g, '');
    }

    setFormData((prev) => ({ ...prev, [name]: processedValue }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const renderPasswordToggle = (isVisible, toggleVisibility) => (
    <button
      type="button"
      className="password-toggle"
      onClick={toggleVisibility}
      aria-label={isVisible ? "Hide password" : "Show password"}
    >
      <img
        src={isVisible ? EyeSlashIconSvg : EyeIconSvg}
        alt={isVisible ? "Hide password" : "Show password"}
        width="20"
        height="20"
      />
    </button>
  );

  // Nếu đăng ký thành công, hiển thị thông báo kiểm tra email
  if (registrationSuccess) {
    return (
      <div className="register-container" data-navbar-theme="black">
        <div className="registration-success-wrapper">
          <div className="registration-success">
            <h1 className="heading-1--no-margin register-title success-title">
              REGISTRATION<br />SUCCESSFUL!
            </h1>
            <p className="bodytext-4--no-margin success-message">
              Thank you for registering.
            </p>
            <p className="bodytext-4--no-margin verification-instruction">
              We've sent a verification email to <strong>{registeredEmail}</strong>. Please check your email and click on the verification link to activate your account.
            </p>
            <p className="bodytext-4--no-margin expiry-notice">
              The link will expire in 24 hours.
            </p>

            {/* Resend email message */}
            {resendMessage && (
              <p
                className={`bodytext-4--no-margin resend-message ${
                  resendMessage.includes("success") ? "success" : "error"
                }`}
              >
                {resendMessage}
              </p>
            )}

            <div className="success-actions">
              <ShineGlassButton
                theme="light"
                onClick={() => navigate(ROUTES.AUTH_LOGIN)}
                className="back-to-login-button"
              >
                Back to Sign in
              </ShineGlassButton>
            </div>

            {/* Resend email section */}
            <div className="resend-email-section">
              <p className="bodytext-3--no-margin email-note">
                Didn't receive the email? <button
                  className="resend-email-button bodytext-3--no-margin"
                  onClick={handleResendEmail}
                  disabled={resendCountdown > 0 || isResending}
                >
                  {isResending
                    ? "Sending..."
                    : resendCountdown > 0
                    ? `Resend Verification Email (${resendCountdown}s)`
                    : "Resend Verification Email"}
                </button>
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="register-container" data-navbar-theme="black">
      <div className="register-form-wrapper">
        <h1 className="heading-1--no-margin register-title">CREATE ACCOUNT</h1>
        <form className="register-form" onSubmit={handleSubmit} noValidate>
          {/* Hiển thị lỗi chung của form */}
          {errors.form && (
            <p className="input-error form-error bodytext-4--no-margin">
              {errors.form}
            </p>
          )}

          <div className="title-group">
            <label className="bodytext-3--no-margin">Title*</label>
            <div className="title-options">
              {titles.map((title) => (
                <span
                  key={title}
                  className={`title-option ${
                    formData.title === title ? "active" : ""
                  }`}
                  onClick={() => setFormData((prev) => ({ ...prev, title }))}
                >
                  {title}
                </span>
              ))}
            </div>
          </div>

          <div className="form-field-container">
            <div className="input-group">
              <input
                type="text"
                id="firstName"
                name="firstName"
                value={formData.firstName}
                onChange={handleInputChange}
                placeholder=" "
                required
              />
              <label htmlFor="firstName" className="bodytext-3--no-margin">
                First Name*
              </label>
            </div>
            {errors.firstName && (
              <p className="input-error bodytext-3--no-margin">
                {errors.firstName}
              </p>
            )}
          </div>

          <div className="form-field-container">
            <div className="input-group">
              <input
                type="text"
                id="lastName"
                name="lastName"
                value={formData.lastName}
                onChange={handleInputChange}
                placeholder=" "
                required
              />
              <label htmlFor="lastName" className="bodytext-3--no-margin">
                Last name*
              </label>
            </div>
            {errors.lastName && (
              <p className="input-error bodytext-3--no-margin">
                {errors.lastName}
              </p>
            )}
          </div>

          <div className="form-field-container">
            <div className="input-group">
              <input
                type="text"
                id="username"
                name="username"
                value={formData.username}
                onChange={handleInputChange}
                placeholder=" "
                required
              />
              <label htmlFor="username" className="bodytext-3--no-margin">
                Username*
              </label>
            </div>
            {errors.username && (
              <p className="input-error bodytext-3--no-margin">
                {errors.username}
              </p>
            )}
          </div>

          <div className="form-field-container">
            <div className="input-group">
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                placeholder=" "
                required
              />
              <label htmlFor="email" className="bodytext-3--no-margin">
                Email Address*
              </label>
            </div>
            {errors.email && (
              <p className="input-error bodytext-3--no-margin">
                {errors.email}
              </p>
            )}
          </div>

          <div className="form-field-container">
            <div className="input-group password-group">
              <input
                type={showPassword ? "text" : "password"}
                id="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                placeholder=" "
                required
              />
              <label htmlFor="password" className="bodytext-3--no-margin">
                Password*
              </label>
              {renderPasswordToggle(showPassword, () =>
                setShowPassword(!showPassword)
              )}
            </div>
            {errors.password && (
              <p className="input-error bodytext-3--no-margin">
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
                value={formData.confirmPassword}
                onChange={handleInputChange}
                placeholder=" "
                required
              />
              <label
                htmlFor="confirmPassword"
                className="bodytext-3--no-margin"
              >
                Confirm Password*
              </label>
              {renderPasswordToggle(showConfirmPassword, () =>
                setShowConfirmPassword(!showConfirmPassword)
              )}
            </div>
            {errors.confirmPassword && (
              <p className="input-error bodytext-3--no-margin">
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
          <p className="privacy-info bodytext-4--no-margin">
            {" "}
            For further information about how <br /> we use your personal
            information, please see our <strong>Privacy Policy</strong>{" "}
          </p>
          <div className="create-account-button-wrapper">
            <ShineGlassButton
              theme="light"
              type="submit"
              className="create-account-button"
              disabled={isLoading}
            >
              {isLoading ? "Creating Account..." : "Create account"}
            </ShineGlassButton>
          </div>
          <p className="bodytext-3--no-margin login-prompt">
            Already a member?{" "}
            <a
              onClick={() => navigate(ROUTES.AUTH_LOGIN)}
              className="bodytext-3--no-margin return-login-link"
            >
              Sign in now
            </a>
          </p>
        </form>
      </div>
    </div>
  );
};

export default Register;
