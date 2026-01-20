// src/pages/Profile/Profile.js

import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import api from "@services/api"; // Use centralized API client
import "./Profile.css";
import "@styles/typography.css";
import "@styles/grid-system.css";
import PhoneInput from "react-phone-input-2";
import "react-phone-input-2/lib/style.css";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import ChangePassword from "./ChangePassword";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import { useAuth } from "@/context/AuthContext";
import Orders from "./Orders";
import Services from "./Services";
import AddressBook from "./AddressBook";
import Wishlist from "./Wishlist";
import Toast from "@components/common/toast/Toast";

const Profile = () => {
  const navigate = useNavigate();
  const location = useLocation();
  // Bây giờ chỉ cần 'logout' từ context, không cần 'handleApiError' nữa
  const { logout } = useAuth();
  const navTabsRef = useRef(null);

  // ... (Các state và hằng số không thay đổi)
  const titles = ["Ms", "Mrs", "Mr"];
  const navItems = [
    "My Passport",
    "Orders",
    "Services",
    "Address Book",
    "Wishlist",
  ];

  // Danh sách các quốc gia
  const countries = [
    "Afghanistan",
    "Albania",
    "Algeria",
    "Argentina",
    "Armenia",
    "Australia",
    "Austria",
    "Azerbaijan",
    "Bangladesh",
    "Belgium",
    "Brazil",
    "Bulgaria",
    "Cambodia",
    "Canada",
    "Chile",
    "China",
    "Colombia",
    "Croatia",
    "Czech Republic",
    "Denmark",
    "Egypt",
    "Estonia",
    "Finland",
    "France",
    "Georgia",
    "Germany",
    "Greece",
    "Hungary",
    "Iceland",
    "India",
    "Indonesia",
    "Iran",
    "Iraq",
    "Ireland",
    "Israel",
    "Italy",
    "Japan",
    "Jordan",
    "Kazakhstan",
    "Kenya",
    "South Korea",
    "Kuwait",
    "Latvia",
    "Lebanon",
    "Lithuania",
    "Luxembourg",
    "Malaysia",
    "Mexico",
    "Morocco",
    "Netherlands",
    "New Zealand",
    "Norway",
    "Pakistan",
    "Philippines",
    "Poland",
    "Portugal",
    "Romania",
    "Russia",
    "Saudi Arabia",
    "Singapore",
    "Slovakia",
    "Slovenia",
    "South Africa",
    "Spain",
    "Sri Lanka",
    "Sweden",
    "Switzerland",
    "Thailand",
    "Turkey",
    "Ukraine",
    "United Arab Emirates",
    "United Kingdom",
    "United States",
    "Vietnam",
  ];

  const [formData, setFormData] = useState({
    title: "",
    firstName: "",
    lastName: "",
    email: "",
    dateOfBirth: "",
    phoneNumber: "",
    nationality: "",
  });
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [activeNavItem, setActiveNavItem] = useState(
    location.state?.activeTab || "My Passport"
  );
  const [showChangePassword, setShowChangePassword] = useState(false);
  const [isDatePickerOpen, setIsDatePickerOpen] = useState(false);
  const datePickerRef = useRef(null);
  const [toast, setToast] = useState({
    show: false,
    message: "",
    type: "success",
  });

  // ... (Các hàm validate, handleInputChange, handlePhoneChange, etc. không thay đổi)
  const validateForm = () => {
    const newErrors = {};
    const { firstName, lastName, email, phoneNumber, dateOfBirth } = formData;

    if (!firstName.trim()) newErrors.firstName = "First Name is required";
    else if (firstName.trim().length < 2)
      newErrors.firstName = "First Name must be at least 2 characters";
    else if (!/^[a-zA-ZÀ-ỹ\s]+$/.test(firstName.trim()))
      newErrors.firstName = "First Name contains invalid characters";

    if (!lastName.trim()) newErrors.lastName = "Last Name is required";
    else if (lastName.trim().length < 2)
      newErrors.lastName = "Last Name must be at least 2 characters";
    else if (!/^[a-zA-ZÀ-ỹ\s]+$/.test(lastName.trim()))
      newErrors.lastName = "Last Name contains invalid characters";

    if (!email.trim()) newErrors.email = "Email Address is required";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      newErrors.email = "Email address is invalid";

    if (phoneNumber && phoneNumber.length < 10)
      newErrors.phoneNumber = "Phone number is too short";

    if (dateOfBirth) {
      const birthDate = new Date(dateOfBirth);
      const today = new Date();
      const age = today.getFullYear() - birthDate.getFullYear();
      if (age < 18 || age > 120)
        newErrors.dateOfBirth = "Please enter a valid date of birth";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: null }));
  };

  const handlePhoneChange = (phone) => {
    setFormData((prev) => ({ ...prev, phoneNumber: phone }));
    if (errors.phoneNumber)
      setErrors((prev) => ({ ...prev, phoneNumber: null }));
  };

  const handleTitleSelect = (title) => {
    setFormData((prev) => ({ ...prev, title }));
  };

  const handleNationalityChange = (e) => {
    const { value } = e.target;
    setFormData((prev) => ({ ...prev, nationality: value }));
    if (errors.nationality)
      setErrors((prev) => ({ ...prev, nationality: null }));
  };

  const handleNavClick = (navItem) => {
    setActiveNavItem(navItem);
    // Update location.state so navbar can read the active tab
    navigate(location.pathname, {
      state: { activeTab: navItem },
      replace: true,
    });
  };

  const handleLogout = () => {
    logout(); // Gọi thẳng hàm logout từ context
  };

  const handleScrollRight = () => {
    if (navTabsRef.current) {
      const scrollAmount = navTabsRef.current.clientWidth * 0.7; // Scroll 70% của width
      navTabsRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
    }
  };

  // useEffect to handle activeTab from navigation state
  useEffect(() => {
    if (location.state?.activeTab) {
      setActiveNavItem(location.state.activeTab);
    }
  }, [location.state]);

  // useEffect để lấy thông tin user - ĐƠN GIẢN HƠN RẤT NHIỀU
  useEffect(() => {
    const fetchUserProfile = async () => {
      setIsLoading(true);
      try {
        // Use local API through gateway
        const response = await api.get("/api/v1/users/me");
        let userData = response.data;
        if (userData.dateOfBirth) {
          // Convert string to Date object for DatePicker
          userData.dateOfBirth = new Date(userData.dateOfBirth);
        }
        setFormData((prev) => ({ ...prev, ...userData }));
      } catch (error) {
        // Lỗi ở đây có nghĩa là request đã thất bại ngay cả sau khi thử lại
        console.error("Failed to fetch user profile:", error);
        setErrors({
          form: "Could not load your profile. Please log in again.",
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsLoading(true);
    setErrors({});

    // Chuyển đổi date format từ Date object sang MM/dd/yyyy
    let formattedDateOfBirth = null;
    if (formData.dateOfBirth) {
      const date = formData.dateOfBirth;
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      const year = date.getFullYear();
      formattedDateOfBirth = `${month}/${day}/${year}`;
    }

    const payload = {
      title: formData.title,
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      email: formData.email.trim(),
      dateOfBirth: formattedDateOfBirth, // <-- Sử dụng format đã chuyển đổi
      phoneNumber: formData.phoneNumber,
      nationality: formData.nationality,
    };

    try {
      await api.put("/api/v1/users/me", payload);
      setToast({
        show: true,
        message: "Your changes have been saved successfully!",
        type: "success",
      });
    } catch (error) {
      console.error("Failed to save profile:", error);
      const errorMessage =
        error.response?.data?.message || "Save failed. Please try again.";
      setErrors({ form: errorMessage });
      setToast({
        show: true,
        message: `Failed to save profile: ${errorMessage}`,
        type: "error",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleDateChange = (date) => {
    setFormData((prev) => ({ ...prev, dateOfBirth: date }));
    if (errors.dateOfBirth) {
      setErrors((prev) => ({ ...prev, dateOfBirth: null }));
    }
  };

  const handleDatePickerClick = () => {
    setIsDatePickerOpen((prev) => !prev);
  };

  // Get ordinal suffix
  const getOrdinalSuffix = (day) => {
    if (day > 3 && day < 21) return "th";
    switch (day % 10) {
      case 1:
        return "st";
      case 2:
        return "nd";
      case 3:
        return "rd";
      default:
        return "th";
    }
  };

  // Custom input component for DatePicker with ordinal suffix
  const CustomDateInput = React.forwardRef(({ value, onClick }, ref) => {
    const displayValue = formData.dateOfBirth
      ? (() => {
          const day = formData.dateOfBirth.getDate();
          const month = formData.dateOfBirth.toLocaleDateString("en-US", {
            month: "long",
          });
          const year = formData.dateOfBirth.getFullYear();
          const suffix = getOrdinalSuffix(day);

          return (
            <>
              {month} {day}
              <sup style={{ fontSize: "0.7em", verticalAlign: "super" }}>
                {suffix}
              </sup>
              , {year}
            </>
          );
        })()
      : "Select...";

    return (
      <div
        ref={ref}
        onClick={onClick}
        className="profile-form-input profile-datepicker-input bodytext-4--no-margin custom-datepicker-input"
        style={{
          cursor: "pointer",
          backgroundImage:
            "url(\"data:image/svg+xml,%3Csvg width='12' height='8' viewBox='0 0 12 8' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1L6 6L11 1' stroke='%23797979' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E\")",
          backgroundRepeat: "no-repeat",
          backgroundPosition: "right center",
          backgroundSize: "12px 8px",
          paddingRight: "24px",
        }}
      >
        {displayValue}
      </div>
    );
  });

  CustomDateInput.displayName = "CustomDateInput";

  // Phần JSX return không có gì thay đổi
  if (isLoading && !formData.firstName) {
    // Chỉ hiển thị loading nếu chưa có dữ liệu
    return <div className="profile-loading">Loading Profile...</div>;
  }

  return (
    <div className="profile-container">
      <div className="profile-form-wrapper">
        {/* Profile Header */}
        <div
          className="profile-header grid-container"
          data-navbar-theme="white"
        >
          <div className="profile-info col-4">
            <h1 className="heading-1--no-margin profile-name">
              {formData.firstName}
              <br />
              {formData.lastName}
            </h1>
            <div className="profile-logout" onClick={handleLogout}>
              Logout
            </div>
          </div>
          <div className="profile-center col-4">
            <div className="profile-logo"></div>
          </div>
          <div className="profile-tier col-4">
            <p className="tier-level bodytext-1--no-margin">Tier 3</p>
            <p className="tier-name bodytext-1--no-margin">hewhewe</p>
          </div>
        </div>

        {/* Header Navigation */}
        <div className="profile-nav-header" data-navbar-theme="black">
          <nav className="profile-nav" ref={navTabsRef}>
            {navItems.map((item) => (
              <button
                key={item}
                className={`profile-nav-item ${
                  activeNavItem === item ? "active" : ""
                }`}
                onClick={() => handleNavClick(item)}
              >
                <span className="bodytext-6--no-margin">{item}</span>
              </button>
            ))}
          </nav>
          <button
            className="profile-nav-scroll-arrow"
            onClick={handleScrollRight}
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path
                d="M6 12L10 8L6 4"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>
        </div>

        {/* Form Content */}
        <div className="profile-form-content" data-navbar-theme="black">
          {activeNavItem === "My Passport" && (
            <form className="profile-form" onSubmit={handleSubmit} noValidate>
              {/* Title */}
              <div className="profile-title-group">
                <label className="bodytext-6--no-margin">Title*</label>
                <div className="profile-title-options">
                  {titles.map((title) => (
                    <span
                      key={title}
                      onClick={() => handleTitleSelect(title)}
                      className={`profile-title-option bodytext-6--no-margin ${
                        formData.title?.toLowerCase() === title.toLowerCase()
                          ? "active"
                          : ""
                      }`}
                    >
                      {title}
                    </span>
                  ))}
                </div>
              </div>

              {/* Form Fields */}
              <div className="profile-field-container">
                <label className="bodytext-6--no-margin">First Name*</label>
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName || ""}
                  onChange={handleInputChange}
                  className="profile-form-input bodytext-4--no-margin"
                  required
                />
                {errors.firstName && (
                  <p className="profile-input-error bodytext-4--no-margin">
                    {errors.firstName}
                  </p>
                )}
              </div>
              <div className="profile-field-container">
                <label className="bodytext-6--no-margin">Last name*</label>
                <input
                  type="text"
                  name="lastName"
                  value={formData.lastName || ""}
                  onChange={handleInputChange}
                  className="profile-form-input bodytext-4--no-margin"
                  required
                />
                {errors.lastName && (
                  <p className="profile-input-error bodytext-4--no-margin">
                    {errors.lastName}
                  </p>
                )}
              </div>
              <div className="profile-field-container">
                <label className="bodytext-6--no-margin">Email Address*</label>
                <input
                  type="email"
                  name="email"
                  value={formData.email || ""}
                  onChange={handleInputChange}
                  className="profile-form-input bodytext-4--no-margin"
                  required
                />
                {errors.email && (
                  <p className="profile-input-error bodytext-4--no-margin">
                    {errors.email}
                  </p>
                )}
              </div>
              <div className="profile-field-container">
                <label className="bodytext-6--no-margin">Date of Birth</label>
                <DatePicker
                  ref={datePickerRef}
                  selected={formData.dateOfBirth}
                  onChange={handleDateChange}
                  onInputClick={handleDatePickerClick}
                  open={isDatePickerOpen}
                  onClickOutside={() => setIsDatePickerOpen(false)}
                  onSelect={() => setIsDatePickerOpen(false)}
                  customInput={<CustomDateInput />}
                  wrapperClassName="profile-datepicker-wrapper"
                  calendarClassName="profile-datepicker-calendar"
                  showYearDropdown
                  scrollableYearDropdown
                  yearDropdownItemNumber={100}
                  maxDate={new Date()}
                />
                {errors.dateOfBirth && (
                  <p className="profile-input-error bodytext-4--no-margin">
                    {errors.dateOfBirth}
                  </p>
                )}
              </div>

              <div className="profile-phone-input-container">
                <label className="bodytext-3--no-margin">Phone Number</label>
                <PhoneInput
                  country={"vn"}
                  value={formData.phoneNumber}
                  onChange={handlePhoneChange}
                  inputClass="profile-phone-input-field bodytext-4--no-margin"
                  containerClass="profile-phone-input-control"
                  buttonClass="profile-phone-dropdown-button"
                />
                {errors.phoneNumber && (
                  <p className="profile-input-error bodytext-4--no-margin">
                    {errors.phoneNumber}
                  </p>
                )}
              </div>

              <div className="profile-field-container">
                <label className="bodytext-6--no-margin">Nationality</label>
                <select
                  name="nationality"
                  value={formData.nationality || ""}
                  onChange={handleNationalityChange}
                  className={`profile-form-input profile-nationality-dropdown bodytext-4--no-margin ${!formData.nationality ? 'placeholder-state' : ''}`}
                  style={{ color: formData.nationality ? '#333' : 'rgba(121, 121, 121, 1)' }}
                >
                  <option value="">Select a country</option>
                  {countries.map((country) => (
                    <option key={country} value={country}>
                      {country}
                    </option>
                  ))}
                </select>
              </div>

              {/* Action Buttons */}
              {errors.form && (
                <div className="profile-field-container">
                  <p className="profile-input-error bodytext-4--no-margin">
                    {errors.form}
                  </p>
                </div>
              )}

              <div className="profile-action-buttons-container">
                <ShineGlassButton
                  theme="light"
                  onClick={() => setShowChangePassword(true)}
                  className="profile-change-password-button"
                >
                  Change Password
                </ShineGlassButton>
                <ShineGlassButton
                  theme="light"
                  type="submit"
                  className="profile-save-button"
                >
                  {isLoading ? "Saving..." : "Save Profile"}
                </ShineGlassButton>
              </div>
            </form>
          )}

          {activeNavItem === "Orders" && <Orders />}
          {activeNavItem === "Services" && <Services />}
          {activeNavItem === "Address Book" && <AddressBook />}
          {activeNavItem === "Wishlist" && <Wishlist />}
        </div>
      </div>
      {showChangePassword && (
        <ChangePassword onClose={() => setShowChangePassword(false)} />
      )}

      {toast.show && (
        <Toast
          message={toast.message}
          type={toast.type}
          duration={10000}
          onClose={() =>
            setToast({ show: false, message: "", type: "success" })
          }
        />
      )}
    </div>
  );
};

export default Profile;
