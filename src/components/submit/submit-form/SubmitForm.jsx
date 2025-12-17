import { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import "./SubmitForm.css";
import "react-phone-input-2/lib/style.css";
import PhoneInput from "react-phone-input-2";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import { r2API, notificationsAPI } from "@services/api";
import { ROUTES } from "@/constants/routes";

const TURNSTILE_SITE_KEY = "0x4AAAAAAB5BOMtJV6dfw_JE";
const TURNSTILE_SCRIPT_URL =
  "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";

const SubmitForm = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    designerName: "",
    contactEmail: "",
    phone: "",
    location: "",
    website: "",
    instagram: "",
    tiktok: "",
    portfolioLink: "",
    categories: [],
    priceRange: "",
    productionCapacity: "",
    readiness: "",
    portfolioFile: null,
    portfolioFileUrl: "",
    heroImages: [],
    heroImagesUrls: [],
    videoLookbook: "",
    collaborationInterests: [],
    brandStory: "",
    agreeToContact: false,
    eventExperienceRating: "",
    brandSentiment: "",
    collaborationLikelihood: "",
    feedbackNotes: "",
  });

  const [isUploading, setIsUploading] = useState(false);
  const [fileErrors, setFileErrors] = useState({});
  const [validationErrors, setValidationErrors] = useState({});
  const [showPrivacyPopup, setShowPrivacyPopup] = useState(false);
  const [shouldPreventRadioChange, setShouldPreventRadioChange] =
    useState(false);
  const [phoneInputHeight, setPhoneInputHeight] = useState("42px");
  const [phoneInputFontSize, setPhoneInputFontSize] = useState("14px");

  // Update phone input height and font size based on screen size
  useEffect(() => {
    const updatePhoneInputStyles = () => {
      const width = window.innerWidth;

      if (width <= 480) {
        setPhoneInputHeight("30px");
        setPhoneInputFontSize("10px");
      } else if (width <= 1024) {
        setPhoneInputHeight("35px");
        setPhoneInputFontSize("13px");
      } else if (width <= 1300) {
        setPhoneInputHeight("35px");
        setPhoneInputFontSize("12px");
      } else {
        setPhoneInputHeight("44px");
        setPhoneInputFontSize("14px");
      }
    };

    updatePhoneInputStyles();
    window.addEventListener("resize", updatePhoneInputStyles);

    return () => {
      window.removeEventListener("resize", updatePhoneInputStyles);
    };
  }, []);

  // Hide search emoji and style search box in phone input dropdown
  useEffect(() => {
    const stylePhoneDropdown = () => {
      const searchEmoji = document.querySelector(
        ".phone-input-container .search-emoji"
      );
      if (searchEmoji) {
        searchEmoji.style.display = "none";
      }

      // Match country-list width with input field width
      const formControl = document.querySelector(
        ".phone-input-container .form-control"
      );
      const countryList = document.querySelector(
        ".phone-input-container .country-list"
      );

      if (formControl && countryList) {
        const inputWidth = formControl.offsetWidth;
        countryList.style.width = `${inputWidth}px`;
      }

      const search = document.querySelector(".phone-input-container .search");
      const searchBox = document.querySelector(
        ".phone-input-container .search-box"
      );

      if (search) {
        search.style.padding = "12px 12px";
        search.style.boxSizing = "border-box";
      }

      if (searchBox && search) {
        // Get search container width and apply to search-box
        const searchWidth = search.offsetWidth;
        const searchPadding = 24; // 12px left + 12px right
        searchBox.style.width = `${searchWidth - searchPadding}px`;
        searchBox.style.padding = "10px 12px";
        searchBox.style.fontSize = "14px";
        searchBox.style.boxSizing = "border-box";
        searchBox.style.marginLeft = "0";
        searchBox.style.border = "none";
        searchBox.style.borderBottom = "1px solid #e0e0e0";
        searchBox.style.borderRadius = "0";
        searchBox.style.background = "rgba(246, 246, 246, 1)";
      }

      // Style dial codes to align right
      const countries = document.querySelectorAll(
        ".phone-input-container .country"
      );
      countries.forEach((country) => {
        const dialCode = country.querySelector(".dial-code");
        if (dialCode) {
          // Ensure parent is flex
          country.style.display = "flex";
          country.style.alignItems = "center";
          country.style.padding = "12px 12px";

          // Push dial code to the right
          dialCode.style.marginLeft = "auto";
          dialCode.style.marginRight = "0";
          dialCode.style.paddingRight = "0";
        }
      });

      // Check if dropdown is closed and remove 'open' class
      const flagDropdown = document.querySelector(
        ".phone-input-container .flag-dropdown"
      );
      const selectedFlag = document.querySelector(
        ".phone-input-container .selected-flag"
      );

      if (flagDropdown && !countryList) {
        // If no country-list, dropdown is closed
        flagDropdown.classList.remove("open");

        // Force blur to remove focus/active states
        if (selectedFlag) {
          selectedFlag.blur();
        }
        if (flagDropdown) {
          flagDropdown.blur();
        }
      }
    };

    // Run after component mounts and when dropdown opens
    const observer = new MutationObserver(stylePhoneDropdown);
    const phoneContainer = document.querySelector(".phone-input-container");

    if (phoneContainer) {
      observer.observe(phoneContainer, { childList: true, subtree: true });
      stylePhoneDropdown(); // Initial styling
    }

    return () => {
      observer.disconnect();
    };
  }, []);

  // Feedback options
  const EXPERIENCE_RATINGS = [
    "Exceptional – I was fully immersed",
    "Great – memorable and inspiring",
    "Good – engaging overall",
    "Fair – room for improvement",
    "Needs work",
  ];

  const BRAND_SENTIMENT_OPTIONS = [
    "Deeply inspired by MIRROR's vision",
    "Curious to continue the conversation",
    "Appreciate the innovation",
    "Still discovering the brand",
  ];

  const COLLABORATION_LIKELIHOOD_OPTIONS = [
    "Ready to explore collaborations soon",
    "Interested – let's explore possibilities",
    "Would like to learn more first",
  ];

  // Disable body scroll when popup is open
  const openPrivacyPopup = () => {
    setShowPrivacyPopup(true);
    document.body.style.overflow = "hidden";
  };

  const closePrivacyPopup = () => {
    setShowPrivacyPopup(false);
    document.body.style.overflow = "unset";
  };
  const [captchaToken, setCaptchaToken] = useState("");

  const turnstileContainerRef = useRef(null);
  const turnstileWidgetIdRef = useRef(null);

  useEffect(() => {
    if (typeof window === "undefined") {
      return () => {};
    }

    const renderWidget = () => {
      if (!window.turnstile || !turnstileContainerRef.current) {
        return;
      }

      if (turnstileWidgetIdRef.current !== null) {
        window.turnstile.reset(turnstileWidgetIdRef.current);
        return;
      }

      turnstileWidgetIdRef.current = window.turnstile.render(
        turnstileContainerRef.current,
        {
          sitekey: TURNSTILE_SITE_KEY,
          callback: (token) => setCaptchaToken(token || ""),
          "expired-callback": () => setCaptchaToken(""),
          "error-callback": () => setCaptchaToken(""),
        }
      );
    };

    const script = document.querySelector(
      `script[src="${TURNSTILE_SCRIPT_URL}"]`
    );

    if (script) {
      if (window.turnstile) {
        renderWidget();
      } else {
        script.addEventListener("load", renderWidget, { once: true });
      }
    } else {
      const newScript = document.createElement("script");
      newScript.src = TURNSTILE_SCRIPT_URL;
      newScript.async = true;
      newScript.defer = true;
      newScript.addEventListener("load", renderWidget, { once: true });
      document.body.appendChild(newScript);
    }

    return () => {
      const existingScript = document.querySelector(
        `script[src="${TURNSTILE_SCRIPT_URL}"]`
      );
      if (existingScript) {
        existingScript.removeEventListener("load", renderWidget);
      }
      if (window.turnstile && turnstileWidgetIdRef.current !== null) {
        window.turnstile.remove(turnstileWidgetIdRef.current);
      }
      turnstileWidgetIdRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (captchaToken && validationErrors.captchaToken) {
      setValidationErrors((prev) => {
        const { captchaToken: _removed, ...rest } = prev;
        return rest;
      });
    }
  }, [captchaToken, validationErrors.captchaToken]);

  // File size limits (in bytes)
  const FILE_SIZE_LIMITS = {
    portfolioFile: 20 * 1024 * 1024, // 20MB
    heroImages: 3 * 1024 * 1024, // 3MB per image
  };

  // File count limits
  const FILE_COUNT_LIMITS = {
    heroImages: 5, // up to 5 images
  };

  // Accepted file types
  const ACCEPTED_FILE_TYPES = {
    portfolioFile: [".pdf"],
    heroImages: [".jpg", ".jpeg", ".png"],
  };

  // Helper function to format file size
  const formatFileSize = (bytes) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  // Helper function to validate file size
  const validateFileSize = (file, maxSize) => {
    return file.size <= maxSize;
  };

  // Helper function to validate file type
  const validateFileType = (file, acceptedTypes) => {
    const fileExtension = "." + file.name.split(".").pop().toLowerCase();
    return acceptedTypes.includes(fileExtension);
  };

  // Remove file from hero images
  const removeHeroImage = (indexToRemove) => {
    const updatedFiles = formData.heroImages.filter(
      (_, index) => index !== indexToRemove
    );
    setFormData((prev) => ({ ...prev, heroImages: updatedFiles }));

    // Clear any file count errors when removing files
    if (fileErrors.heroImages && fileErrors.heroImages.includes("total")) {
      const newErrors = { ...fileErrors };
      delete newErrors.heroImages;
      setFileErrors(newErrors);
    }
  };

  // Handle radio button click to allow unselecting
  const handleRadioClick = (e) => {
    const { name, value } = e.target;

    // If clicking on already selected radio, mark to prevent change and unselect it
    if (formData[name] === value) {
      setShouldPreventRadioChange(true);

      setTimeout(() => {
        setFormData((prev) => ({ ...prev, [name]: "" }));
        setShouldPreventRadioChange(false);

        // Clear validation error
        if (validationErrors[name]) {
          setValidationErrors((prev) => {
            const newErrors = { ...prev };
            delete newErrors[name];
            return newErrors;
          });
        }
      }, 0);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;

    // Skip radio change if we're in the process of unchecking
    if (type === "radio" && shouldPreventRadioChange) {
      return;
    }

    // Clear validation error for this field when user starts typing/selecting
    if (validationErrors[name]) {
      setValidationErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[name];
        return newErrors;
      });
    }

    if (type === "checkbox") {
      if (name === "categories" || name === "collaborationInterests") {
        setFormData((prev) => ({
          ...prev,
          [name]: checked
            ? [...prev[name], value]
            : prev[name].filter((item) => item !== value),
        }));
      } else {
        setFormData((prev) => ({ ...prev, [name]: checked }));
      }
    } else if (type === "file") {
      if (name === "portfolioFile") {
        const file = e.target.files[0];
        const newErrors = { ...fileErrors };

        if (file) {
          // Validate file type
          if (!validateFileType(file, ACCEPTED_FILE_TYPES.portfolioFile)) {
            newErrors.portfolioFile = `Only PDF files are allowed`;
            setFormData((prev) => ({ ...prev, [name]: null }));
          }
          // Validate file size
          else if (!validateFileSize(file, FILE_SIZE_LIMITS.portfolioFile)) {
            newErrors.portfolioFile = `File size (${formatFileSize(
              file.size
            )}) exceeds the 20MB limit`;
            setFormData((prev) => ({ ...prev, [name]: null }));
          } else {
            delete newErrors.portfolioFile;
            setFormData((prev) => ({ ...prev, [name]: file }));
          }
        } else {
          delete newErrors.portfolioFile;
          setFormData((prev) => ({ ...prev, [name]: null }));
        }

        setFileErrors(newErrors);
      } else if (name === "heroImages") {
        const newFiles = Array.from(e.target.files);
        const existingFiles = formData.heroImages || [];
        const newErrors = { ...fileErrors };
        const validFiles = [];
        const invalidFiles = [];
        const invalidTypes = [];

        // Check total file count limit (existing + new)
        if (
          existingFiles.length + newFiles.length >
          FILE_COUNT_LIMITS.heroImages
        ) {
          newErrors.heroImages = `You can only upload up to ${FILE_COUNT_LIMITS.heroImages} images total. You have ${existingFiles.length} existing files and selected ${newFiles.length} new files.`;
          setFileErrors(newErrors);
          return;
        }

        newFiles.forEach((file) => {
          // Validate file type
          if (!validateFileType(file, ACCEPTED_FILE_TYPES.heroImages)) {
            invalidTypes.push(file.name);
          }
          // Validate file size
          else if (!validateFileSize(file, FILE_SIZE_LIMITS.heroImages)) {
            invalidFiles.push(`${file.name} (${formatFileSize(file.size)})`);
          } else {
            validFiles.push(file);
          }
        });

        // Set error messages
        let errorMessage = "";
        if (invalidTypes.length > 0) {
          errorMessage += `Invalid file types: ${invalidTypes.join(
            ", "
          )}. Only JPG, JPEG, PNG files are allowed.`;
        }
        if (invalidFiles.length > 0) {
          if (errorMessage) errorMessage += " ";
          errorMessage += `The following files exceed the 3MB limit: ${invalidFiles.join(
            ", "
          )}`;
        }

        if (errorMessage) {
          newErrors.heroImages = errorMessage;
        } else {
          delete newErrors.heroImages;
        }

        // Combine existing files with new valid files
        const allFiles = [...existingFiles, ...validFiles];
        setFormData((prev) => ({ ...prev, [name]: allFiles }));
        setFileErrors(newErrors);

        // Clear file input to allow selecting same files again if needed
        e.target.value = "";
      }
    } else {
      setFormData((prev) => ({ ...prev, [name]: value }));
    }
  };

  const uploadFiles = async () => {
    const uploadedFiles = {
      portfolioFileUrl: "",
      heroImagesUrls: [],
    };

    try {
      // Upload portfolio file if exists
      if (formData.portfolioFile) {
        const portfolioResponse = await r2API.upload(
          formData.portfolioFile,
          "submissions/portfolios"
        );
        uploadedFiles.portfolioFileUrl = portfolioResponse.data.publicUrl;
      }

      // Upload hero images if exist
      if (formData.heroImages && formData.heroImages.length > 0) {
        const heroImagePromises = formData.heroImages.map((file) =>
          r2API.upload(file, "submissions/hero-images")
        );

        const heroImageResponses = await Promise.all(heroImagePromises);
        uploadedFiles.heroImagesUrls = heroImageResponses.map(
          (response) => response.data.publicUrl
        );
      }

      return uploadedFiles;
    } catch (error) {
      throw new Error("Failed to upload files. Please try again.");
    }
  };

  const submitToGoogleSheets = async (formDataToSubmit) => {
    const scriptURL = import.meta.env.VITE_GOOGLE_SHEETS_SCRIPT_URL;

    if (!scriptURL) {
      throw new Error(
        "We're experiencing technical difficulties. Please try again or contact us at info@mirrorfuturediamond.com"
      );
    }

    try {
      await fetch(scriptURL, {
        method: "POST",
        mode: "no-cors",
        body: JSON.stringify(formDataToSubmit),
        headers: { "Content-Type": "application/json" },
      });

      return { success: true };
    } catch (error) {
      throw new Error(
        "We couldn't process your submission right now. Please try again in a moment, or contact us at info@mirrorfuturediamond.com if the issue persists."
      );
    }
  };

  // Validate required fields with field-specific errors
  const validateRequiredFields = () => {
    const requiredFields = [
      { field: "designerName", label: "Designer / Brand name is required" },
      { field: "contactEmail", label: "Contact email is required" },
      { field: "location", label: "Location is required" },
      // { field: "categories", label: "Please select at least one category" }, // Commented out for Milan
      // {
      //   field: "eventExperienceRating",
      //   label: "Please share how the immersive showroom felt",
      // },
      // {
      //   field: "collaborationLikelihood",
      //   label: "Please share your interest in collaborating",
      // },
      {
        field: "agreeToContact",
        label: "You must agree to the Privacy Policy",
      },
    ];

    const errors = {};

    requiredFields.forEach(({ field, label }) => {
      const value = formData[field];

      if (
        !value ||
        (Array.isArray(value) && value.length === 0) ||
        (typeof value === "string" && value.trim() === "") ||
        (field === "agreeToContact" && value === false)
      ) {
        errors[field] = label;
      }
    });

    return errors;
  };

  // Validate email format
  const validateEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  // Comprehensive validation with field-specific errors
  const validateForm = () => {
    const errors = {};

    // Check required fields
    const requiredFieldErrors = validateRequiredFields();
    Object.assign(errors, requiredFieldErrors);

    // Check email format
    if (formData.contactEmail && !validateEmail(formData.contactEmail)) {
      errors.contactEmail = "Please enter a valid email address";
    }

    return errors;
  };

  // Function to scroll to first error field
  const scrollToFirstError = (errors) => {
    // Define field order from top to bottom
    const fieldOrder = [
      "designerName",
      "contactEmail",
      "location",
      // "categories", // Commented out for Milan
      // "eventExperienceRating", // Made optional
      // "collaborationLikelihood", // Made optional
      "agreeToContact",
      "captchaToken",
    ];

    // Find first field with error
    for (const field of fieldOrder) {
      if (errors[field]) {
        let element;

        if (field === "categories") {
          element = document.querySelector(".submit-form-checkbox-group");
        } else if (field === "agreeToContact") {
          element = document.querySelector('input[name="agreeToContact"]');
        } else if (field === "eventExperienceRating") {
          // Scroll to the radio group container instead of first input
          const radioGroup = document.querySelector(
            'input[name="eventExperienceRating"]'
          );
          element = radioGroup?.closest(".submit-form-group");
        } else if (field === "collaborationLikelihood") {
          // Scroll to the radio group container instead of first input
          const radioGroup = document.querySelector(
            'input[name="collaborationLikelihood"]'
          );
          element = radioGroup?.closest(".submit-form-group");
        } else if (field === "captchaToken") {
          element = document.querySelector("#submit-form-turnstile");
        } else {
          element = document.querySelector(`input[name="${field}"]`);
        }

        if (element) {
          // Get element position
          const elementRect = element.getBoundingClientRect();
          const absoluteElementTop = elementRect.top + window.pageYOffset;
          const navbarHeight = 100; // Approximate navbar height
          const offset = 120; // Extra offset for better visibility

          // Scroll to element with offset for fixed navbar
          window.scrollTo({
            top: absoluteElementTop - navbarHeight - offset,
            behavior: "smooth",
          });

          // Focus the element if it's a focusable input
          if (
            element.focus &&
            element.type !== "checkbox" &&
            element.type !== "radio"
          ) {
            setTimeout(() => element.focus(), 500);
          }
          break;
        }
      }
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Clear previous validation errors
    setValidationErrors({});

    // Comprehensive validation
    const errors = validateForm();

    // Check file errors as well
    if (Object.keys(fileErrors).length > 0) {
      errors.fileErrors = "Please fix file size/type errors before submitting";
    }

    if (!captchaToken) {
      errors.captchaToken = "Please verify you are human";
    }

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      scrollToFirstError(errors);
      return;
    }

    setIsUploading(true);

    try {
      // Upload files and get URLs (if any files selected)
      let uploadedFiles = { portfolioFileUrl: "", heroImagesUrls: [] };

      if (
        formData.portfolioFile ||
        (formData.heroImages && formData.heroImages.length > 0)
      ) {
        uploadedFiles = await uploadFiles();
      }

      // Prepare data for Google Sheets (matching HTML form structure)
      const googleSheetsData = {
        designerName: formData.designerName,
        email: formData.contactEmail,
        phone: formData.phone,
        location: formData.location,
        website: formData.website,
        instagram: formData.instagram,
        tiktok: formData.tiktok,
        portfolioLink: formData.portfolioLink,
        categories: Array.isArray(formData.categories)
          ? formData.categories.join(", ")
          : formData.categories || "",
        priceRange: formData.priceRange || "",
        capacity: formData.productionCapacity || "",
        readiness: formData.readiness || "",
        pdfLink: uploadedFiles.portfolioFileUrl || "",
        imageLinks:
          uploadedFiles.heroImagesUrls.length > 0
            ? uploadedFiles.heroImagesUrls.join(", ")
            : "",
        videoLink: formData.videoLookbook || "",
        collabInterest: Array.isArray(formData.collaborationInterests)
          ? formData.collaborationInterests.join(", ")
          : formData.collaborationInterests || "",
        brandStory: formData.brandStory || "",
        showroomRate: formData.eventExperienceRating || "",
        showcaseRate: formData.brandSentiment || "",
        eventExperienceRating: formData.collaborationLikelihood || "",
        feedbackNote: formData.feedbackNotes || "",
        inspirationNote: "",
      };

      // Submit to Google Sheets
      await submitToGoogleSheets(googleSheetsData);

      // Send email notification to user in background (non-blocking)
      notificationsAPI
        .sendEmail(
          [formData.contactEmail],
          "milan-form",
          {
            name: formData.designerName,
          },
          captchaToken
        )
        .catch((emailError) => {
          // Retry once after 2 seconds (in case of cold start)
          return new Promise((resolve) => setTimeout(resolve, 2000)).then(() =>
            notificationsAPI
              .sendEmail(
                [formData.contactEmail],
                "milan-form",
                {
                  name: formData.designerName,
                },
                captchaToken
              )
              .catch(() => {
                // Email failed - silently ignore as form submission was successful
              })
          );
        });

      // Navigate to success page immediately
      navigate(ROUTES.MILAN_SUBMIT_SUCCESS);
    } catch (error) {
      alert(error.message || "Failed to submit form. Please try again.");
    } finally {
      if (typeof window !== "undefined" && window.turnstile) {
        if (turnstileWidgetIdRef.current !== null) {
          window.turnstile.reset(turnstileWidgetIdRef.current);
        }
      }
      setCaptchaToken("");
      setIsUploading(false);
    }
  };

  return (
    <div className="submit-form-wrapper">
      <section className="submit-form-section">
        <div className="submit-form-container">
          {/* Header Text */}
          <p className="submit-form-header bodytext-3--no-margin">
            LET'S BEGIN THE CONVERSATION
          </p>

          {/* Main Title */}
          <h2 className="submit-form-title heading-2--no-margin">
            Share Your Vision
          </h2>

          {/* Form */}
          <form className="submit-form" onSubmit={handleSubmit}>
            <div className="submit-form-row">
              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Designer / Brand name *
                </label>
                <input
                  type="text"
                  name="designerName"
                  placeholder="Your studio or brand"
                  className="submit-form-input bodytext-6--no-margin"
                  value={formData.designerName}
                  onChange={handleInputChange}
                  required
                />
                {validationErrors.designerName && (
                  <p className="submit-form-error bodytext-6--no-margin">
                    {validationErrors.designerName}
                  </p>
                )}
              </div>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Contact email *
                </label>
                <input
                  type="email"
                  name="contactEmail"
                  placeholder="name@gmail.com"
                  className="submit-form-input bodytext-6--no-margin"
                  value={formData.contactEmail}
                  onChange={handleInputChange}
                  required
                />
                {validationErrors.contactEmail && (
                  <p className="submit-form-error bodytext-6--no-margin">
                    {validationErrors.contactEmail}
                  </p>
                )}
              </div>
            </div>

            <div className="submit-form-row">
              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Phone / WhatsApp
                </label>
                <PhoneInput
                  country={"it"}
                  value={formData.phone}
                  onChange={(phone, country) => {
                    // Add space after country code safely
                    let formattedPhone = phone;
                    if (
                      country.dialCode &&
                      phone.startsWith(country.dialCode) &&
                      phone.length > country.dialCode.length &&
                      !phone.startsWith(country.dialCode + " ")
                    ) {
                      formattedPhone =
                        country.dialCode +
                        " " +
                        phone.slice(country.dialCode.length);
                    }
                    setFormData((prev) => ({
                      ...prev,
                      phone: formattedPhone,
                    }));
                  }}
                  containerClass="phone-input-container"
                  inputClass="submit-form-input bodytext-6--no-margin"
                  buttonClass="phone-input-button"
                  dropdownClass="phone-input-dropdown"
                  enableSearch={true}
                  searchPlaceholder="Search country..."
                  disableCountryCode={false}
                  countryCodeEditable={true}
                  containerStyle={{
                    width: "100%",
                    maxWidth: "100%",
                    border: "none",
                    boxShadow: "none",
                  }}
                  inputStyle={{
                    width: "100%",
                    maxWidth: "100%",
                    border: "none",
                    borderBottom: "1px solid #e0e0e0",
                    borderRadius: "0",
                    boxShadow: "none",
                    height: phoneInputHeight,
                    background: "rgba(246, 246, 246, 1)",
                    fontFamily: '"BT Beau Sans", sans-serif',
                    fontSize: phoneInputFontSize,
                    fontWeight: "400",
                    lineHeight: "1",
                    letterSpacing: "1.3px",
                  }}
                  buttonStyle={{
                    border: "none",
                    borderRadius: "0",
                    background: "transparent",
                    height: phoneInputHeight,
                  }}
                />
              </div>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Location *
                </label>
                <input
                  type="text"
                  name="location"
                  placeholder="City, Country"
                  className="submit-form-input bodytext-6--no-margin"
                  value={formData.location}
                  onChange={handleInputChange}
                  required
                />
                {validationErrors.location && (
                  <p className="submit-form-error bodytext-6--no-margin">
                    {validationErrors.location}
                  </p>
                )}
              </div>
            </div>

            <div className="submit-form-row">
              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Website
                </label>
                <input
                  type="url"
                  name="website"
                  placeholder="https://"
                  className="submit-form-input bodytext-6--no-margin"
                  value={formData.website}
                  onChange={handleInputChange}
                />
              </div>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Instagram
                </label>
                <input
                  type="url"
                  name="instagram"
                  placeholder="https://instagram.com/..."
                  className="submit-form-input bodytext-6--no-margin"
                  value={formData.instagram}
                  onChange={handleInputChange}
                />
              </div>
            </div>

            <div className="submit-form-row">
              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Tiktok
                </label>
                <input
                  type="url"
                  name="tiktok"
                  placeholder="https://tiktok.com/@..."
                  className="submit-form-input bodytext-6--no-margin"
                  value={formData.tiktok}
                  onChange={handleInputChange}
                />
              </div>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Behance / Portfolio link
                </label>
                <input
                  type="url"
                  name="portfolioLink"
                  placeholder="https://"
                  className="submit-form-input bodytext-6--no-margin"
                  value={formData.portfolioLink}
                  onChange={handleInputChange}
                />
              </div>
            </div>

            {/* SENSITIVE FIELDS - Commented out for Milan event (move to second-stage form later) */}
            {/* Categories */}
            {/* <div className="submit-form-group">
              <label className="submit-form-label bodytext-3--no-margin">
                Categories (select all that apply) *
              </label>
              <div className="submit-form-checkbox-group">
                {["Fine", "Demi-fine", "Experimental", "Digital/3D"].map(
                  (category) => (
                    <label
                      key={category}
                      className="submit-form-checkbox-label bodytext-4--no-margin"
                    >
                      <input
                        type="checkbox"
                        name="categories"
                        value={category}
                        checked={formData.categories.includes(category)}
                        onChange={handleInputChange}
                      />
                      {category}
                    </label>
                  )
                )}
              </div>
              {validationErrors.categories && (
                <p className="submit-form-error bodytext-6--no-margin">
                  {validationErrors.categories}
                </p>
              )}
            </div> */}

            {/* <div className="submit-form-row">
              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Typical price range
                </label>
                <select
                  name="priceRange"
                  className="submit-form-select bodytext-6--no-margin"
                  value={formData.priceRange}
                  onChange={handleInputChange}
                >
                  <option value="">Select...</option>
                  <option value="under-100">Under $100</option>
                  <option value="100-500">$100 - $500</option>
                  <option value="500-1000">$500 - $1,000</option>
                  <option value="1000-5000">$1,000 - $5,000</option>
                  <option value="over-5000">Over $5,000</option>
                </select>
              </div>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Production capacity
                </label>
                <select
                  name="productionCapacity"
                  className="submit-form-select bodytext-6--no-margin"
                  value={formData.productionCapacity}
                  onChange={handleInputChange}
                >
                  <option value="">Select...</option>
                  <option value="small">Small (1-10 pieces/month)</option>
                  <option value="medium">Medium (10-50 pieces/month)</option>
                  <option value="large">Large (50+ pieces/month)</option>
                </select>
              </div>
            </div> */}

            {/* Readiness */}
            {/* <div className="submit-form-group">
              <label className="submit-form-label bodytext-3--no-margin">
                Readiness
              </label>
              <div className="submit-form-radio-group">
                {["Made-to-order", "Ready stock", "Both"].map((option) => (
                  <label
                    key={option}
                    className="submit-form-radio-label bodytext-4--no-margin"
                  >
                    <input
                      type="radio"
                      name="readiness"
                      value={option}
                      checked={formData.readiness === option}
                      onChange={handleInputChange}
                    />
                    {option}
                  </label>
                ))}
              </div>
            </div> */}

            <div className="submit-form-row">
              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Portfolio PDF (max 20MB)
                </label>
                <div className="submit-form-file-input-wrapper">
                  <input
                    type="file"
                    id="portfolioFile"
                    name="portfolioFile"
                    accept=".pdf"
                    className="submit-form-file-input"
                    onChange={handleInputChange}
                  />
                  <label
                    htmlFor="portfolioFile"
                    className="submit-form-file-input-placeholder bodytext-6--no-margin"
                  >
                    {formData.portfolioFile
                      ? `${formData.portfolioFile.name} (${formatFileSize(
                          formData.portfolioFile.size
                        )})`
                      : "Upload File"}
                  </label>
                </div>
                {fileErrors.portfolioFile && (
                  <p className="submit-form-error bodytext-6--no-margin">
                    {fileErrors.portfolioFile}
                  </p>
                )}
                <p className="submit-form-note bodytext-6--no-margin">
                  Optional if you provided a link above.
                </p>
              </div>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Hero Images (up to 5 JPG/PNG, max 3MB each)
                </label>
                <div className="submit-form-file-input-wrapper">
                  <input
                    type="file"
                    id="heroImages"
                    name="heroImages"
                    accept=".jpg,.jpeg,.png"
                    multiple
                    className="submit-form-file-input"
                    onChange={handleInputChange}
                  />
                  <label
                    htmlFor="heroImages"
                    className="submit-form-file-input-placeholder bodytext-6--no-margin"
                  >
                    {formData.heroImages && formData.heroImages.length > 0
                      ? `Add more images (${formData.heroImages.length}/5 selected)`
                      : "Upload Images"}
                  </label>
                </div>

                {/* Display selected files */}
                {formData.heroImages && formData.heroImages.length > 0 && (
                  <div className="selected-files-list">
                    {formData.heroImages.map((file, index) => (
                      <div key={index} className="selected-file-item">
                        <span className="file-info bodytext-6--no-margin">
                          {file.name} ({formatFileSize(file.size)})
                        </span>
                        <button
                          type="button"
                          className="remove-file-btn"
                          onClick={() => removeHeroImage(index)}
                          title="Remove file"
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {fileErrors.heroImages && (
                  <p className="submit-form-error bodytext-6--no-margin">
                    {fileErrors.heroImages}
                  </p>
                )}
                <p className="submit-form-note bodytext-6--no-margin">
                  We may feature selected images with credit.
                </p>
              </div>
            </div>

            {/* Video Lookbook */}
            <div className="submit-form-group">
              <label className="submit-form-label bodytext-3--no-margin">
                Video lookbook (Youtube / Vimeo)
              </label>
              <input
                type="url"
                name="videoLookbook"
                placeholder="https://www.youtube.com/..."
                className="submit-form-input bodytext-6--no-margin"
                value={formData.videoLookbook}
                onChange={handleInputChange}
              />
            </div>

            {/* Feedback Section */}
            <div className="submit-form-feedback">
              <h3 className="submit-form-subtitle heading-3--no-margin">
                Your experience at Mirror in Milan
              </h3>
              <p className="submit-form-description bodytext-5--no-margin">
                Help us understand how the immersive showroom resonated with you
                so we can shape meaningful collaborations.
              </p>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  How would you rate the immersive showroom experience?
                </label>
                <div className="submit-form-radio-group submit-form-radio-group--stacked">
                  {EXPERIENCE_RATINGS.map((option) => (
                    <label
                      key={option}
                      className="submit-form-radio-label bodytext-4--no-margin"
                    >
                      <input
                        type="radio"
                        name="eventExperienceRating"
                        value={option}
                        checked={formData.eventExperienceRating === option}
                        onChange={handleInputChange}
                        onClick={handleRadioClick}
                      />
                      {option}
                    </label>
                  ))}
                </div>
                {validationErrors.eventExperienceRating && (
                  <p className="submit-form-error bodytext-6--no-margin">
                    {validationErrors.eventExperienceRating}
                  </p>
                )}
              </div>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  How are you feeling about MIRROR after the showcase?
                </label>
                <div className="submit-form-radio-group submit-form-radio-group--stacked">
                  {BRAND_SENTIMENT_OPTIONS.map((option) => (
                    <label
                      key={option}
                      className="submit-form-radio-label bodytext-4--no-margin"
                    >
                      <input
                        type="radio"
                        name="brandSentiment"
                        value={option}
                        checked={formData.brandSentiment === option}
                        onChange={handleInputChange}
                        onClick={handleRadioClick}
                      />
                      {option}
                    </label>
                  ))}
                </div>
              </div>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  How ready are you to explore a collaboration with MIRROR?
                </label>
                <div className="submit-form-radio-group submit-form-radio-group--stacked">
                  {COLLABORATION_LIKELIHOOD_OPTIONS.map((option) => (
                    <label
                      key={option}
                      className="submit-form-radio-label bodytext-4--no-margin"
                    >
                      <input
                        type="radio"
                        name="collaborationLikelihood"
                        value={option}
                        checked={formData.collaborationLikelihood === option}
                        onChange={handleInputChange}
                        onClick={handleRadioClick}
                      />
                      {option}
                    </label>
                  ))}
                </div>
                {validationErrors.collaborationLikelihood && (
                  <p className="submit-form-error bodytext-6--no-margin">
                    {validationErrors.collaborationLikelihood}
                  </p>
                )}
              </div>

              <div className="submit-form-group">
                <label className="submit-form-label bodytext-3--no-margin">
                  Anything we can refine or co-create next time? (optional)
                </label>
                <textarea
                  name="feedbackNotes"
                  placeholder="Share thoughts about the showcase, the brand, or collaboration ideas."
                  className="submit-form-textarea bodytext-6--no-margin"
                  rows="4"
                  value={formData.feedbackNotes}
                  onChange={handleInputChange}
                />
              </div>
            </div>

            {/* Collaboration Interests */}
            <div className="submit-form-group">
              <label className="submit-form-label bodytext-3--no-margin">
                Collaboration interests (can choose more than one)
              </label>
              <div className="submit-form-checkbox-group">
                {[
                  "Retail pop-ups",
                  "Co-design",
                  "Influencer shoots",
                  "Workshops",
                ].map((interest) => (
                  <label
                    key={interest}
                    className="submit-form-checkbox-label bodytext-4--no-margin"
                  >
                    <input
                      type="checkbox"
                      name="collaborationInterests"
                      value={interest}
                      checked={formData.collaborationInterests.includes(
                        interest
                      )}
                      onChange={handleInputChange}
                    />
                    {interest}
                  </label>
                ))}
              </div>
            </div>

            {/* Brand Story */}
            <div className="submit-form-group">
              <label className="submit-form-label bodytext-3--no-margin">
                If you would like, share a few words about your inspirations
                (optional)
              </label>
              <textarea
                name="brandStory"
                placeholder="What inspires your work? What materials do you love?"
                className="submit-form-textarea submit-form-textarea--white bodytext-6--no-margin"
                rows="4"
                value={formData.brandStory}
                onChange={handleInputChange}
              />
            </div>

            {/* Agreement */}
            <div className="submit-form-group">
              <label className="submit-form-checkbox-label bodytext-4--no-margin">
                <input
                  type="checkbox"
                  name="agreeToContact"
                  checked={formData.agreeToContact}
                  onChange={handleInputChange}
                  required
                />
                <span className="privacy-policy-wrapper">
                  I agree to be contacted regarding my submission and accept the{" "}
                  <span
                    className="privacy-policy-link"
                    onClick={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      openPrivacyPopup();
                    }}
                  >
                    Privacy Policy
                  </span>
                </span>
              </label>
              {validationErrors.agreeToContact && (
                <p className="submit-form-error bodytext-6--no-margin">
                  {validationErrors.agreeToContact}
                </p>
              )}
            </div>

            {/* Captcha */}
            <div className="submit-form-group">
              <div
                id="submit-form-turnstile"
                ref={turnstileContainerRef}
                className="submit-form-turnstile"
              />
              {validationErrors.captchaToken && (
                <p className="submit-form-error bodytext-6--no-margin">
                  {validationErrors.captchaToken}
                </p>
              )}
            </div>

            {/* Submit Button */}
            <div className="submit-form-group form-submit">
              <ShineGlassButton
                theme="light"
                onClick={(e) => {
                  e.preventDefault();
                  handleSubmit(e);
                }}
                disabled={isUploading}
              >
                {isUploading ? "Submitting..." : "Share your vision"}
              </ShineGlassButton>
              <p className="submit-note bodytext-6--no-margin">
                You will receive an email confirmation with your submission
                summary.
              </p>
            </div>
          </form>
        </div>
      </section>

      {/* Privacy Policy Popup - Using Portal to render at document root */}
      {showPrivacyPopup &&
        createPortal(
          <div className="privacy-popup-overlay" onClick={closePrivacyPopup}>
            <div
              className="privacy-popup-content"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="privacy-popup-header">
                <div className="privacy-popup-close">
                  <ShineGlassButton
                    width={40}
                    height={40}
                    theme="light"
                    onClick={closePrivacyPopup}
                  >
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path
                        d="M16.8623 5.06054L11.4355 10.4883L16.8623 15.916L15.8018 16.9766L10.375 11.5488L5.06055 16.8652L4 15.8037L9.31445 10.4873L4 5.17188L5.06055 4.11133L10.375 9.42676L15.8018 4L16.8623 5.06054Z"
                        fill="currentColor"
                      />
                    </svg>
                  </ShineGlassButton>
                </div>
              </div>
              <div className="privacy-popup-inner">
                <h2 className="heading-2--no-margin">Privacy Policy</h2>
                <div className="privacy-popup-body">
                  <h3 className="heading-3--no-margin">
                    1. Information We Collect
                  </h3>
                  <p className="bodytext-3--no-margin">
                    When you submit this form, we collect the following
                    information:
                  </p>
                  <ul>
                    <li className="bodytext-3--no-margin">
                      Contact details (name, email, phone/WhatsApp, location).
                    </li>
                    <li className="bodytext-3--no-margin">
                      Public links you choose to share (website, Instagram,
                      TikTok, Behance/Portfolio, video lookbook).
                    </li>
                    <li className="bodytext-3--no-margin">
                      Uploaded files (PDF portfolios, hero images, and other
                      attachments).
                    </li>
                    <li className="bodytext-3--no-margin">
                      Optional information you provide (inspirations, materials
                      you love).
                    </li>
                  </ul>

                  <h3 className="heading-3--no-margin">
                    2. How We Use Your Information
                  </h3>
                  <ul>
                    <li className="bodytext-3--no-margin">
                      To review your submission and evaluate potential
                      collaborations.
                    </li>
                    <li className="bodytext-3--no-margin">
                      To contact you regarding opportunities, events, or next
                      steps.
                    </li>
                    <li className="bodytext-3--no-margin">
                      To feature selected images or content (with credit) on our
                      communication channels.
                    </li>
                    <li className="bodytext-3--no-margin">
                      For internal analysis and future improvements of our
                      programs.
                    </li>
                  </ul>

                  <h3 className="heading-3--no-margin">
                    3. How We Store Your Information
                  </h3>
                  <ul>
                    <li className="bodytext-3--no-margin">
                      Form inputs are stored securely in our internal Google
                      Workspace.
                    </li>
                    <li className="bodytext-3--no-margin">
                      Uploaded files are stored in Amazon Web Services (AWS)
                      storage.
                    </li>
                    <li className="bodytext-3--no-margin">
                      Access to your data is limited to our internal team and
                      collaborators directly involved in the review process.
                    </li>
                  </ul>

                  <h3 className="heading-3--no-margin">4. Data Retention</h3>
                  <p className="bodytext-3--no-margin">
                    We keep your information for as long as needed for
                    collaboration purposes. You may request deletion of your
                    data at any time by contacting us at{" "}
                    <a href="mailto:info@mirrorfuturediamond.com">
                      info@mirrorfuturediamond.com
                    </a>
                    .
                  </p>

                  <h3 className="heading-3--no-margin">
                    5. Sharing of Information
                  </h3>
                  <p className="bodytext-3--no-margin">
                    We do not sell or rent your personal data. Information may
                    only be shared with trusted partners when directly relevant
                    to a collaboration opportunity.
                  </p>

                  <h3 className="heading-3--no-margin">6. Your Rights</h3>
                  <p className="bodytext-3--no-margin">
                    You have the right to:
                  </p>
                  <ul>
                    <li className="bodytext-3--no-margin">
                      Request a copy of the information we hold about you.
                    </li>
                    <li className="bodytext-3--no-margin">
                      Request corrections or updates to your data.
                    </li>
                    <li className="bodytext-3--no-margin">
                      Request deletion of your data from our systems.
                    </li>
                  </ul>

                  <h3 className="heading-3--no-margin">7. Contact Us</h3>
                  <p className="bodytext-3--no-margin">
                    If you have any questions about this Privacy Policy or how
                    your data is handled, please contact us at:{" "}
                    <a href="mailto:info@mirrorfuturediamond.com">
                      info@mirrorfuturediamond.com
                    </a>
                  </p>
                </div>
              </div>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
};

export default SubmitForm;
