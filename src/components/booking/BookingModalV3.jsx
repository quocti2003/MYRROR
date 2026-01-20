import { useState, useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import "./BookingModalV3.css";
import { locationsAPI, appointmentsAPI, handleAPIError } from "@services/api";
import UnderlineButton from "@components/common/button/UnderlineButton";
import UnderlineButtonOpposite from "@components/common/button/UnderlineButtonOpposite";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import { getImageUrl } from "@/utils/cloudflareMediaUtil";

// Left panel images for each step from Cloudflare CDN
const STEP_IMAGES = {
  default: getImageUrl("10_Book an appointment/Book an Appointment.webp"),
  landing: getImageUrl("10_Book an appointment/Book an Appointment.webp"),
  intro: getImageUrl("10_Book an appointment/Book an Appointment.webp"),
  venue: getImageUrl("10_Book an appointment/Book an Appointment.webp"),
  service: getImageUrl("10_Book an appointment/Discover Our Collections & New Creations.webp"),
  dateTime: getImageUrl("10_Book an appointment/Choose time.webp"),
  form: getImageUrl("10_Book an appointment/Fill out the form.webp"),
  review: getImageUrl("10_Book an appointment/Review your booking.webp"),
};

// Preload all booking images immediately when module loads (before modal opens)
if (typeof window !== "undefined") {
  Object.values(STEP_IMAGES).forEach((src) => {
    const img = new Image();
    img.src = src;
  });
}

// Default map URL for Ho Chi Minh City
const DEFAULT_MAP_URL =
  "https://www.google.com/maps?q=Mirror+Diamond+Ho+Chi+Minh+City&hl=en&z=13&output=embed";

// Services data - extracted to avoid duplication
const SERVICES = [
  {
    id: 1,
    title: "Discover Our Collections & New Creations",
    description:
      "Explore Mirror's signature pieces and latest releases. See, feel, and experience every piece in an intimate, sensory-crafted space.",
    image: getImageUrl(
      "10_Book an appointment/Discover Our Collections & New Creations.webp"
    ),
  },
  {
    id: 2,
    title: "Personalization & Custom Design",
    description:
      "Shape your own story.\n\nLearn about Mirror's customization options: select diamond shapes, metal tones, engravings, or co-create a piece.",
    image: getImageUrl(
      "10_Book an appointment/Personalization & Custom Design.webp"
    ),
  },
  {
    id: 3,
    title: "After-Sales Care & Exchanges",
    description:
      "Your relationship with Mirror continues beyond the moment of purchase. Book an appointment for resizing, cleaning, polishing, warranty support, or exchange guidance.",
    image: getImageUrl(
      "10_Book an appointment/After-Sales Care & Exchanges.webp"
    ),
  },
  {
    id: 4,
    title: "Other Dedicated Services",
    description:
      "For everything beyond the usual. Please indicate the purpose of your visit so our team can prepare thoughtfully.",
    hasInput: true,
    image: getImageUrl("10_Book an appointment/Other Dedicated Services.webp"),
  },
];

// Preload all service images when module loads
if (typeof window !== "undefined") {
  SERVICES.forEach((service) => {
    if (service.image) {
      const img = new Image();
      img.src = service.image;
    }
  });
}

// Time slots
const TIME_SLOTS = [
  "09:00 AM",
  "10:00 AM",
  "11:00 AM",
  "12:00 PM",
  "01:00 PM",
  "02:00 PM",
  "03:00 PM",
  "04:00 PM",
  "05:00 PM",
  "06:00 PM",
];

// Vietnamese diacritics removal - simplified version
const removeDiacritics = (str) => {
  return str
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D");
};

// Get ordinal suffix for a number (1st, 2nd, 3rd, 4th, etc.)
const getOrdinalSuffix = (n) => {
  const s = ["th", "st", "nd", "rd"];
  const v = n % 100;
  return s[(v - 20) % 10] || s[v] || s[0];
};

const BookingModalV3 = ({ isOpen, onClose, initialStep = 1 }) => {
  const [currentStep, setCurrentStep] = useState(initialStep);
  const totalSteps = 7; // Intro(1) + Venue(2) + Service(3) + DateTime(4) + Form(5) + Review(6) + Confirmation(7)

  // API data state
  const [locations, setLocations] = useState([]);
  const [cities, setCities] = useState(["All Cities"]);
  const [loading, setLoading] = useState(true);

  // Step-specific state
  const [selectedVenue, setSelectedVenue] = useState(null);
  const [selectedService, setSelectedService] = useState(null);
  const [otherServiceText, setOtherServiceText] = useState("");
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedTime, setSelectedTime] = useState(null);
  const [formData, setFormData] = useState({
    title: "Mr",
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    preferences: "",
  });
  const [acceptTerms, setAcceptTerms] = useState(false);
  const [acceptPrivacy, setAcceptPrivacy] = useState(false);

  // Booked slots state
  const [bookedSlots, setBookedSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);

  // Unavailable dates state (fully blocked dates)
  const [unavailableDates, setUnavailableDates] = useState([]);
  const [loadingUnavailableDates, setLoadingUnavailableDates] = useState(false);

  // Map loading state
  const [mapLoading, setMapLoading] = useState(false);
  const mapIframeRef = useRef(null);

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submissionError, setSubmissionError] = useState(null);
  const [bookingReference, setBookingReference] = useState(null);

  // Fetch locations data when modal opens and set defaults
  useEffect(() => {
    if (isOpen) {
      fetchLocationsData();
      // Set default service to first option
      if (SERVICES.length > 0) {
        setSelectedService(SERVICES[0]);
      }
    }
  }, [isOpen]);

  // Note: Images are preloaded at module level (above) for faster display

  const fetchLocationsData = async () => {
    setLoading(true);
    try {
      const [locationsResponse, filtersResponse] = await Promise.all([
        locationsAPI.getAll(),
        locationsAPI.getFilterOptions(),
      ]);

      const locationsData = locationsResponse.data || [];
      const filtersData = filtersResponse.data || {};

      // Filter all types except POD
      const nonPodLocations = locationsData.filter(
        (location) => location.type !== "POD"
      );

      setLocations(nonPodLocations);
      setCities(["All Cities", ...(filtersData.cities || [])]);

      // Set default venue to first option
      if (nonPodLocations.length > 0) {
        setSelectedVenue(nonPodLocations[0]);
      }
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to load locations");
      console.error("Error fetching locations:", errorInfo);
      setLocations([]);
      setCities(["All Cities"]);
    } finally {
      setLoading(false);
    }
  };

  // Handle map transition when venue changes
  useEffect(() => {
    if (selectedVenue && mapIframeRef.current) {
      setMapLoading(true);
    }
  }, [selectedVenue]);

  // Fetch booked slots when date or venue changes
  useEffect(() => {
    const fetchBookedSlots = async () => {
      if (!selectedDate || !selectedVenue) {
        setBookedSlots([]);
        return;
      }

      setLoadingSlots(true);
      try {
        // Format date as YYYY-MM-DD for the API (using local date)
        const year = selectedDate.getFullYear();
        const month = String(selectedDate.getMonth() + 1).padStart(2, "0");
        const day = String(selectedDate.getDate()).padStart(2, "0");
        const dateStr = `${year}-${month}-${day}`;

        const response = await appointmentsAPI.getBookedSlots(
          dateStr,
          selectedVenue.id
        );

        // Convert LocalTime strings (e.g., "09:00:00") to display format (e.g., "09:00 AM")
        const booked = (response.data?.bookedSlots || []).map((timeStr) => {
          const [hours, minutes] = timeStr.split(":");
          const hour = parseInt(hours, 10);
          const ampm = hour >= 12 ? "PM" : "AM";
          const displayHour = hour % 12 || 12;
          return `${displayHour
            .toString()
            .padStart(2, "0")}:${minutes} ${ampm}`;
        });

        setBookedSlots(booked);
      } catch (err) {
        console.error("Error fetching booked slots:", err);
        setBookedSlots([]);
      } finally {
        setLoadingSlots(false);
      }
    };

    fetchBookedSlots();
  }, [selectedDate, selectedVenue]);

  // Fetch unavailable dates when venue is selected
  useEffect(() => {
    const fetchUnavailableDates = async () => {
      if (!selectedVenue) {
        setUnavailableDates([]);
        return;
      }

      setLoadingUnavailableDates(true);
      try {
        // Get unavailable dates for the next 90 days
        const today = new Date();
        const endDate = new Date();
        endDate.setDate(endDate.getDate() + 90);

        const formatDate = (d) => {
          const year = d.getFullYear();
          const month = String(d.getMonth() + 1).padStart(2, "0");
          const day = String(d.getDate()).padStart(2, "0");
          return `${year}-${month}-${day}`;
        };

        const response = await appointmentsAPI.getUnavailableDates(
          formatDate(today),
          formatDate(endDate),
          selectedVenue.id
        );

        // Convert date strings to Date objects for comparison
        const dates = (response.data?.unavailableDates || []).map(
          (dateStr) => new Date(dateStr + "T00:00:00")
        );
        setUnavailableDates(dates);
      } catch (err) {
        console.error("Error fetching unavailable dates:", err);
        setUnavailableDates([]);
      } finally {
        setLoadingUnavailableDates(false);
      }
    };

    fetchUnavailableDates();
  }, [selectedVenue]);

  // Disable scroll when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }

    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isOpen]);

  // Reset state when modal closes
  useEffect(() => {
    if (!isOpen) {
      setTimeout(() => {
        setCurrentStep(1);
        setSelectedVenue(null);
        setSelectedService(null);
        setOtherServiceText("");
        setSelectedDate(null);
        setSelectedTime(null);
        setFormData({
          title: "Mr",
          firstName: "",
          lastName: "",
          email: "",
          phone: "",
          preferences: "",
        });
        setAcceptTerms(false);
        setAcceptPrivacy(false);
        setBookedSlots([]);
        setLoadingSlots(false);
        setUnavailableDates([]);
        setLoadingUnavailableDates(false);
        setIsSubmitting(false);
        setSubmissionError(null);
        setBookingReference(null);
      }, 300);
    }
  }, [isOpen]);

  const handleNext = () => {
    if (currentStep < totalSteps) {
      setCurrentStep(currentStep + 1);
    }
  };

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleEdit = (step) => {
    setCurrentStep(step);
  };

  const canContinue = () => {
    switch (currentStep) {
      case 1: // Intro
        return true;
      case 2: // Select Venue
        return !!selectedVenue;
      case 3: // Choose Service
        return !!selectedService;
      case 4: // Choose Date & Time
        return !!selectedDate && !!selectedTime;
      case 5: // Fill Form
        return !!(formData.firstName && formData.lastName && formData.phone);
      case 6: // Review
        return acceptTerms && acceptPrivacy;
      default:
        return false;
    }
  };

  // Convert display time (e.g., "09:00 AM") to 24-hour format (e.g., "09:00:00")
  const convertTo24HourFormat = (timeStr) => {
    const [time, period] = timeStr.split(" ");
    let [hours, minutes] = time.split(":");
    hours = parseInt(hours, 10);

    if (period === "PM" && hours !== 12) {
      hours += 12;
    } else if (period === "AM" && hours === 12) {
      hours = 0;
    }

    return `${hours.toString().padStart(2, "0")}:${minutes}:00`;
  };

  // Submit booking to backend
  const submitBooking = async () => {
    setIsSubmitting(true);
    setSubmissionError(null);

    try {
      // Format date as YYYY-MM-DD (using local date)
      const year = selectedDate.getFullYear();
      const month = String(selectedDate.getMonth() + 1).padStart(2, "0");
      const day = String(selectedDate.getDate()).padStart(2, "0");
      const dateStr = `${year}-${month}-${day}`;

      const appointmentData = {
        customerTitle: formData.title,
        customerFirstName: formData.firstName,
        customerLastName: formData.lastName,
        customerEmail: formData.email || "",
        customerPhone: formData.phone,
        venueId: selectedVenue.id,
        service: selectedService?.title || "",
        appointmentDate: dateStr,
        appointmentTime: convertTo24HourFormat(selectedTime),
        language: "en",
        preferences: formData.preferences || "",
      };

      const response = await appointmentsAPI.create(appointmentData);
      setBookingReference(response.data?.id || "APT000000");
      setCurrentStep(7); // Go to confirmation
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to submit booking");
      setSubmissionError(errorInfo.message);
      console.error("Booking submission error:", errorInfo);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleContinue = () => {
    if (!canContinue()) return;

    if (currentStep === 6) {
      submitBooking();
    } else {
      handleNext();
    }
  };

  const getStepTitle = (step) => {
    const titles = {
      1: "Book an Appointment", // Intro
      2: "Select a venue",
      3: "Choose a service",
      4: "Choose time",
      5: "Fill out the form",
      6: "Review your booking",
    };
    return titles[step] || "";
  };

  const getProgressPercent = () => {
    // Progress bar starts from step 1 (intro), reaches 100% at step 6 (review)
    // Steps 1-6 = 6 steps for progress (0% to 100%)
    return ((currentStep - 1) / 5) * 100;
  };

  // Get map URL for selected venue
  const getMapUrl = () => {
    if (selectedVenue) {
      const lat = selectedVenue.coordinates?.lat || selectedVenue.latitude;
      const lng = selectedVenue.coordinates?.lng || selectedVenue.longitude;
      if (lat && lng) {
        return `https://www.google.com/maps?q=${lat},${lng}&hl=en&z=16&output=embed`;
      }
    }
    return DEFAULT_MAP_URL;
  };

  if (!isOpen) return null;

  // Filter function to disable unavailable dates
  const isDateAvailable = (date) => {
    const checkDate = new Date(date);
    checkDate.setHours(0, 0, 0, 0);

    return !unavailableDates.some((unavailableDate) => {
      const unavailable = new Date(unavailableDate);
      unavailable.setHours(0, 0, 0, 0);
      return checkDate.getTime() === unavailable.getTime();
    });
  };

  // Step 7: Confirmation - Full width with gradient background
  if (currentStep === 7) {
    return createPortal(
      <div className="bkv3-modal-overlay">
        <div className="bkv3-modal-container bkv3-confirmation-layout">
          <div className="bkv3-confirmation-full">
            <button className="bkv3-confirmation-close" onClick={onClose}>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="20"
                height="20"
                viewBox="0 0 20 20"
                fill="none"
              >
                <path
                  d="M16.8623 5.06055L11.4355 10.4883L16.8623 15.916L15.8018 16.9766L10.375 11.5488L5.06055 16.8652L4 15.8037L9.31445 10.4873L4 5.17188L5.06055 4.11133L10.375 9.42676L15.8018 4L16.8623 5.06055Z"
                  fill="white"
                  fillOpacity="0.7"
                />
              </svg>
            </button>
            <div className="bkv3-confirmation-content">
              <h1 className="heading-2--no-margin">
                Your Appointment is Confirmed
              </h1>
              <p className="bodytext-6--no-margin bkv3-confirmation-subtitle">
                Thank you for choosing to experience Mirror.
              </p>
              <p className="bodytext-6--no-margin bkv3-confirmation-text">
                Your visit is now in motion - you'll receive an email reminder
                with all details soon.
              </p>
              <div className="bkv3-confirmation-actions">
                <GlassThemeButton
                  theme="dark"
                  className="bkv3-confirmation-btn"
                  onClick={() => {
                    onClose();
                    window.location.href = "/";
                  }}
                >
                  Back to homepage
                </GlassThemeButton>
                <GlassThemeButton
                  theme="spec_dark"
                  className="bkv3-confirmation-btn"
                  onClick={onClose}
                >
                  Continue scrolling
                </GlassThemeButton>
              </div>
            </div>
          </div>
        </div>
      </div>,
      document.body
    );
  }

  return createPortal(
    <div className="bkv3-modal-overlay">
      {/* Hidden preload container - forces browser to load all images immediately */}
      <div style={{ position: 'absolute', width: 0, height: 0, overflow: 'hidden', opacity: 0 }}>
        {Object.values(STEP_IMAGES).map((src, index) => (
          <img key={index} src={src} alt="" />
        ))}
      </div>

      <div className="bkv3-modal-container">
        {/* Left Panel - Image or Map */}
        <div className="bkv3-left-panel">
          {currentStep === 2 ? (
            <div className="bkv3-map-container">
              <a
                href={
                  selectedVenue
                    ? `https://www.google.com/maps?q=${
                        selectedVenue.coordinates?.lat || selectedVenue.latitude
                      },${
                        selectedVenue.coordinates?.lng ||
                        selectedVenue.longitude
                      }`
                    : "https://www.google.com/maps?q=Mirror+Diamond+Ho+Chi+Minh+City"
                }
                target="_blank"
                rel="noopener noreferrer"
                className="bkv3-map-link bodytext-6--no-margin"
              >
                View larger map
              </a>
              <iframe
                ref={mapIframeRef}
                src={getMapUrl()}
                width="100%"
                height="100%"
                className={`bkv3-map-iframe ${mapLoading ? "loading" : ""}`}
                style={{ border: 0 }}
                allowFullScreen=""
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
                title="Store Locations Map"
                onLoad={() => setMapLoading(false)}
              />
            </div>
          ) : (
            <>
              <img
                key={currentStep}
                src={
                  currentStep === 1
                    ? STEP_IMAGES.intro
                    : currentStep === 3
                    ? STEP_IMAGES.service
                    : currentStep === 4
                    ? STEP_IMAGES.dateTime
                    : currentStep === 5
                    ? STEP_IMAGES.form
                    : currentStep === 6
                    ? STEP_IMAGES.review
                    : STEP_IMAGES.default
                }
                alt="Mirror Diamond"
                className="bkv3-left-image"
              />
              {/* Date/Time overlay for step 4 */}
              {currentStep === 4 && selectedDate && (
                <div className="bkv3-date-overlay">
                  <span className="bkv3-date-overlay-day">
                    {selectedDate.getDate()}
                  </span>
                  <span className="bkv3-date-overlay-weekday bodytext-6--no-margin">
                    {selectedDate.toLocaleDateString("en-US", { weekday: "long" }).toUpperCase()}
                  </span>
                  <span className="bkv3-date-overlay-full bodytext-6--no-margin">
                    {selectedDate.getDate()}<sup className="bkv3-date-overlay-suffix-small">{getOrdinalSuffix(selectedDate.getDate())}</sup>, {selectedDate.toLocaleDateString("en-US", { month: "long" })} {selectedDate.getFullYear()}
                    {selectedTime && `, ${selectedTime}`}
                  </span>
                </div>
              )}
            </>
          )}
        </div>

        {/* Right Panel - Content */}
        <div className="bkv3-right-panel">
          {currentStep < 7 && (
            <>
              {/* Step 1: Intro/Landing-style layout */}
              {currentStep === 1 ? (
                <div className="bkv3-landing">
                  <div className="bkv3-landing-header">
                    <div className="bkv3-landing-info">
                      <span className="bkv3-landing-duration">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                          <circle
                            cx="12"
                            cy="12"
                            r="10"
                            stroke="currentColor"
                            strokeWidth="1.5"
                          />
                          <path
                            d="M12 6v6l4 2"
                            stroke="currentColor"
                            strokeWidth="1.5"
                            strokeLinecap="round"
                          />
                        </svg>
                        <span className="bodytext-6--no-margin">45 minutes</span>
                      </span>
                      <span className="bkv3-landing-tagline bodytext-6--no-margin">
                        Immerse in Mirrorverse
                      </span>
                    </div>
                    <button className="bkv3-close-btn" onClick={onClose}>
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="20"
                        height="20"
                        viewBox="0 0 20 20"
                        fill="none"
                      >
                        <path
                          d="M16.8623 5.06055L11.4355 10.4883L16.8623 15.916L15.8018 16.9766L10.375 11.5488L5.06055 16.8652L4 15.8037L9.31445 10.4873L4 5.17188L5.06055 4.11133L10.375 9.42676L15.8018 4L16.8623 5.06055Z"
                          fill="black"
                          fillOpacity="0.5"
                        />
                      </svg>
                    </button>
                  </div>
                  <div className="bkv3-landing-content">
                    <StepIntro />
                  </div>
                  <div className="bkv3-landing-footer">
                    <ShineGlassButton
                      theme="light"
                      className="bkv3-landing-arrow-btn"
                      onClick={handleNext}
                    >
                      <span className="bkv3-landing-btn-text bodytext-6--no-margin">Next</span>
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="16"
                        height="16"
                        viewBox="0 0 16 16"
                        fill="none"
                      >
                        <path
                          d="M2.30078 8.3999L14.3008 8.3999M14.3008 8.3999L9.15792 13.3999M14.3008 8.3999L9.15792 3.3999"
                          stroke="currentColor"
                          strokeLinecap="square"
                        />
                      </svg>
                    </ShineGlassButton>
                  </div>
                </div>
              ) : (
                <>
                  {/* Header with back button, progress bar, and close button */}
                  <div className="bkv3-header">
                    {currentStep > 1 && (
                      <button className="bkv3-back-btn" onClick={handleBack}>
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          width="20"
                          height="20"
                          viewBox="0 0 20 20"
                          fill="none"
                        >
                          <path
                            d="M13 4L7.00001 10L13 16"
                            stroke="black"
                            strokeOpacity="0.5"
                            strokeWidth="1.5"
                            strokeLinecap="square"
                            strokeLinejoin="round"
                          />
                        </svg>
                      </button>
                    )}

                    {currentStep > 1 && (
                      <div className="bkv3-progress-bar">
                        <div
                          className="bkv3-progress-fill"
                          style={{ width: `${getProgressPercent()}%` }}
                        />
                      </div>
                    )}

                    <button className="bkv3-close-btn" onClick={onClose}>
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="20"
                        height="20"
                        viewBox="0 0 20 20"
                        fill="none"
                      >
                        <path
                          d="M16.8623 5.06055L11.4355 10.4883L16.8623 15.916L15.8018 16.9766L10.375 11.5488L5.06055 16.8652L4 15.8037L9.31445 10.4873L4 5.17188L5.06055 4.11133L10.375 9.42676L15.8018 4L16.8623 5.06055Z"
                          fill="black"
                          fillOpacity="0.5"
                        />
                      </svg>
                    </button>
                  </div>

                  {/* Step Title */}
                  <h1 className="bkv3-step-title heading-3--no-margin">
                    {getStepTitle(currentStep)}
                  </h1>

                  {/* Step Content */}
                  <div className="bkv3-content" key={currentStep}>
                    {currentStep === 2 && (
                      <StepVenue
                        locations={locations}
                        cities={cities}
                        loading={loading}
                        selectedVenue={selectedVenue}
                        setSelectedVenue={setSelectedVenue}
                      />
                    )}
                    {currentStep === 3 && (
                      <StepService
                        selectedService={selectedService}
                        setSelectedService={setSelectedService}
                        otherServiceText={otherServiceText}
                        setOtherServiceText={setOtherServiceText}
                      />
                    )}
                    {currentStep === 4 && (
                      <StepDateTime
                        selectedDate={selectedDate}
                        setSelectedDate={setSelectedDate}
                        selectedTime={selectedTime}
                        setSelectedTime={setSelectedTime}
                        isDateAvailable={isDateAvailable}
                        loadingUnavailableDates={loadingUnavailableDates}
                        bookedSlots={bookedSlots}
                        loadingSlots={loadingSlots}
                      />
                    )}
                    {currentStep === 5 && (
                      <StepForm formData={formData} setFormData={setFormData} />
                    )}
                    {currentStep === 6 && (
                      <StepReview
                        selectedVenue={selectedVenue}
                        selectedService={selectedService}
                        selectedDate={selectedDate}
                        selectedTime={selectedTime}
                        formData={formData}
                        acceptTerms={acceptTerms}
                        setAcceptTerms={setAcceptTerms}
                        acceptPrivacy={acceptPrivacy}
                        setAcceptPrivacy={setAcceptPrivacy}
                        onEdit={handleEdit}
                      />
                    )}
                  </div>

                  {/* Footer with Next/Booking button */}
                  <div className="bkv3-landing-footer">
                    {submissionError && currentStep === 6 && (
                      <div className="bkv3-error-message">{submissionError}</div>
                    )}
                    {currentStep === 6 ? (
                      <ShineGlassButton
                        theme="light"
                        className="bkv3-booking-btn"
                        onClick={handleContinue}
                        disabled={!canContinue() || isSubmitting}
                      >
                        {isSubmitting ? "Booking..." : "Booking"}
                      </ShineGlassButton>
                    ) : (
                      <ShineGlassButton
                        theme="light"
                        className="bkv3-landing-arrow-btn"
                        onClick={handleContinue}
                        disabled={!canContinue() || isSubmitting}
                      >
                        <span className="bkv3-landing-btn-text bodytext-6--no-margin">Next</span>
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          width="16"
                          height="16"
                          viewBox="0 0 16 16"
                          fill="none"
                        >
                          <path
                            d="M2.30078 8.3999L14.3008 8.3999M14.3008 8.3999L9.15792 13.3999M14.3008 8.3999L9.15792 3.3999"
                            stroke="currentColor"
                            strokeLinecap="square"
                          />
                        </svg>
                      </ShineGlassButton>
                    )}
                  </div>
                </>
              )}
            </>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
};

// Step 1: Introduction
const StepIntro = () => {
  return (
    <div className="bkv3-intro">
      <h1 className="bkv3-intro-title heading-2--no-margin">
        Book an<br />experience
      </h1>

      <div className="bkv3-intro-sections">
        <div className="bkv3-intro-section">
          <h4 className="bkv3-intro-section-title bodytext-4--no-margin">WHAT TO EXPECT</h4>
          <p className="bkv3-intro-section-text bodytext-6--no-margin">
            During your visit, our stylists will curate a selections of Mirror's signature pieces, or style guidance to help you discover the perfect piece
          </p>
        </div>

        <div className="bkv3-intro-section">
          <h4 className="bkv3-intro-section-title bodytext-4--no-margin">BEFORE YOUR VISIT</h4>
          <p className="bkv3-intro-section-text bodytext-6--no-margin">
            To preserve a calm and personal experience, we welcome up to 2 guests per appointment, polite pets - dogs are welcome in our boutique
          </p>
        </div>
      </div>
    </div>
  );
};

// Step 2: Select Venue
const StepVenue = ({
  locations,
  cities,
  loading,
  selectedVenue,
  setSelectedVenue,
}) => {
  const [filterCity, setFilterCity] = useState("All Cities");
  const [isCityDropdownOpen, setIsCityDropdownOpen] = useState(false);

  const filteredLocations = locations.filter((location) => {
    if (filterCity === "All Cities") return true;
    return location.city === filterCity;
  });

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!event.target.closest(".bkv3-city-filter")) {
        setIsCityDropdownOpen(false);
      }
    };

    if (isCityDropdownOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isCityDropdownOpen]);

  return (
    <div className="bkv3-venue">
      <div className="bkv3-filter-section">
        <label className="bodytext-6--no-margin">
          Filter by City/Province:
        </label>
        <div className="bkv3-city-filter">
          <div
            className={`bkv3-dropdown-selected ${
              isCityDropdownOpen ? "open" : ""
            }`}
            onClick={() => setIsCityDropdownOpen(!isCityDropdownOpen)}
          >
            <span
              className={`bodytext-6--no-margin ${
                filterCity === "All Cities" ? "default" : ""
              }`}
            >
              {filterCity}
            </span>
          </div>
          {isCityDropdownOpen && (
            <div className="bkv3-dropdown-options">
              {cities.map((city) => (
                <div
                  key={city}
                  className={`bkv3-dropdown-option bodytext-6--no-margin ${
                    filterCity === city ? "selected" : ""
                  }`}
                  onClick={() => {
                    setFilterCity(city);
                    setIsCityDropdownOpen(false);
                  }}
                >
                  {city}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {loading ? (
        <div className="bkv3-loading">Loading venues...</div>
      ) : filteredLocations.length === 0 ? (
        <div className="bkv3-empty">No venues available</div>
      ) : (
        <div className="bkv3-venue-list">
          {filteredLocations.map((venue) => (
            <div
              key={venue.id}
              className={`bkv3-venue-item ${
                selectedVenue?.id === venue.id ? "selected" : ""
              }`}
              onClick={() => setSelectedVenue(venue)}
            >
              <h3 className="bodytext-4--no-margin">{venue.name}</h3>
              <p className="bodytext-6--no-margin">
                {venue.address}, {venue.ward}, {venue.city}, {venue.country}
              </p>
              <p className="bodytext-6--no-margin">{venue.phone}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// Step 3: Choose Service
const StepService = ({
  selectedService,
  setSelectedService,
  otherServiceText,
  setOtherServiceText,
}) => {
  return (
    <div className="bkv3-service">
      <p className="bkv3-service-subtitle bodytext-4--no-margin">
        Tailored guidance for every step of your Mirror journey.
      </p>

      <div className="bkv3-service-list">
        {SERVICES.map((service) => (
          <div
            key={service.id}
            className={`bkv3-service-item ${
              selectedService?.id === service.id ? "selected" : ""
            }`}
            onClick={() => setSelectedService(service)}
          >
            <h3 className="bodytext-4--no-margin">{service.title}</h3>
            <p
              className="bodytext-6--no-margin"
              style={{ whiteSpace: "pre-line" }}
            >
              {service.description}
            </p>
            {service.hasInput && selectedService?.id === service.id && (
              <input
                type="text"
                placeholder="Type here"
                value={otherServiceText}
                onChange={(e) => setOtherServiceText(e.target.value)}
                onClick={(e) => e.stopPropagation()}
                className="bkv3-service-input bodytext-4--no-margin"
              />
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

// Step 4: Choose Date & Time
const StepDateTime = ({
  selectedDate,
  setSelectedDate,
  selectedTime,
  setSelectedTime,
  isDateAvailable,
  loadingUnavailableDates,
  bookedSlots,
  loadingSlots
}) => {
  return (
    <div className="bkv3-datetime">
      {/* Calendar Section */}
      <div className="bkv3-datetime-calendar">
        {loadingUnavailableDates ? (
          <div className="bkv3-loading">Loading available dates...</div>
        ) : (
          <DatePicker
            selected={selectedDate}
            onChange={(date) => setSelectedDate(date)}
            inline
            minDate={new Date()}
            calendarClassName="bkv3-calendar bkv3-calendar-large"
            filterDate={isDateAvailable}
            renderCustomHeader={({
              date,
              decreaseMonth,
              increaseMonth,
              prevMonthButtonDisabled,
              nextMonthButtonDisabled,
            }) => (
              <div className="bkv3-calendar-header">
                <button
                  type="button"
                  onClick={decreaseMonth}
                  disabled={prevMonthButtonDisabled}
                  className="bkv3-calendar-nav"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 20 20"
                    fill="none"
                  >
                    <path
                      d="M13 4L7.00001 10L13 16"
                      stroke="black"
                      strokeOpacity="0.5"
                      strokeWidth="1.5"
                      strokeLinecap="square"
                      strokeLinejoin="round"
                    />
                  </svg>
                </button>
                <div className="bkv3-calendar-month bodytext-4--no-margin">
                  {date.toLocaleString("en-US", {
                    month: "long",
                    year: "numeric",
                  })}
                </div>
                <button
                  type="button"
                  onClick={increaseMonth}
                  disabled={nextMonthButtonDisabled}
                  className="bkv3-calendar-nav"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 20 20"
                    fill="none"
                    style={{ transform: "rotate(180deg)" }}
                  >
                    <path
                      d="M13 4L7.00001 10L13 16"
                      stroke="black"
                      strokeOpacity="0.5"
                      strokeWidth="1.5"
                      strokeLinecap="square"
                      strokeLinejoin="round"
                    />
                  </svg>
                </button>
              </div>
            )}
            formatWeekDay={(day) => (
              <span className="bodytext-6--no-margin">
                {day.substring(0, 1).toUpperCase()}
              </span>
            )}
            dayClassName={() => "bodytext-6--no-margin"}
          />
        )}
      </div>

      {/* Time Section - Only shows after date is selected */}
      {selectedDate && (
        <div className="bkv3-datetime-time">
          <div className="bkv3-time-header">
            <p className="bodytext-4--no-margin">
              {selectedDate.toLocaleDateString("en-US", {
                weekday: "long",
                month: "short",
                day: "numeric",
                year: "numeric",
              })}
            </p>
            <p className="bkv3-timezone bodytext-4--no-margin">
              Indochine: GMT+7
            </p>
          </div>

          {loadingSlots ? (
            <div className="bkv3-loading">Loading available times...</div>
          ) : (
            <div className="bkv3-time-grid">
              {TIME_SLOTS.map((time) => {
                const booked = bookedSlots.includes(time);
                return (
                  <button
                    key={time}
                    className={`bkv3-time-slot bodytext-4--no-margin ${
                      selectedTime === time ? "selected" : ""
                    } ${booked ? "booked" : ""}`}
                    onClick={() => !booked && setSelectedTime(time)}
                    disabled={booked}
                  >
                    <span>{time}</span>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// Step 5: Fill Form
const StepForm = ({ formData, setFormData }) => {
  const titles = ["Mr", "Mrs", "Ms"];
  const textareaRef = useRef(null);

  const handleChange = (field, value) => {
    const valueWithoutDiacritics = removeDiacritics(value);
    setFormData((prev) => ({ ...prev, [field]: valueWithoutDiacritics }));
  };

  const handleTitleSelect = (title) => {
    setFormData((prev) => ({ ...prev, title }));
  };

  const autoResizeTextarea = (textarea) => {
    if (!textarea) return;
    textarea.style.height = "auto";
    textarea.style.height = textarea.scrollHeight + "px";
  };

  useEffect(() => {
    if (textareaRef.current) {
      autoResizeTextarea(textareaRef.current);
    }
  }, [formData.preferences]);

  return (
    <div className="bkv3-form">
      <div className="bkv3-form-title-group">
        <label className="bodytext-6--no-margin">Title *</label>
        <div className="bkv3-form-title-options">
          {titles.map((title) => (
            <UnderlineButton
              key={title}
              onClick={() => handleTitleSelect(title)}
              className={`bkv3-form-title-option ${
                formData.title === title ? "active" : ""
              }`}
            >
              {title}
            </UnderlineButton>
          ))}
        </div>
      </div>

      <div className="bkv3-form-field">
        <label className="bodytext-6--no-margin">First name *</label>
        <input
          type="text"
          placeholder="Your first name"
          value={formData.firstName}
          onChange={(e) => handleChange("firstName", e.target.value)}
          className="bkv3-form-input bodytext-4--no-margin"
        />
      </div>

      <div className="bkv3-form-field">
        <label className="bodytext-6--no-margin">Last name *</label>
        <input
          type="text"
          placeholder="Nguyen"
          value={formData.lastName}
          onChange={(e) => handleChange("lastName", e.target.value)}
          className="bkv3-form-input bodytext-4--no-margin"
        />
      </div>

      <div className="bkv3-form-field">
        <label className="bodytext-6--no-margin">Email</label>
        <input
          type="email"
          placeholder="Your email"
          value={formData.email}
          onChange={(e) => handleChange("email", e.target.value)}
          className="bkv3-form-input bodytext-4--no-margin"
        />
      </div>

      <div className="bkv3-form-field">
        <label className="bodytext-6--no-margin">Phone / WhatsApp *</label>
        <input
          type="tel"
          placeholder="(+84) ..."
          value={formData.phone}
          onChange={(e) => handleChange("phone", e.target.value)}
          className="bkv3-form-input bodytext-4--no-margin"
        />
      </div>

      <div className="bkv3-form-field">
        <label className="bodytext-6--no-margin">
          Please share any specific preferences you have regarding styles, stone
          shapes, and carat weights, or anything else you feel is important.
        </label>
        <textarea
          ref={textareaRef}
          placeholder="Type here"
          value={formData.preferences}
          onChange={(e) => {
            handleChange("preferences", e.target.value);
            autoResizeTextarea(e.target);
          }}
          rows={1}
          className="bkv3-form-textarea bodytext-4--no-margin"
        />
      </div>
    </div>
  );
};

// Step 6: Review Booking
const StepReview = ({
  selectedVenue,
  selectedService,
  selectedDate,
  selectedTime,
  formData,
  acceptTerms,
  setAcceptTerms,
  acceptPrivacy,
  setAcceptPrivacy,
  onEdit,
}) => {
  const formatDate = (date) => {
    if (!date) return "Not selected";
    return date.toLocaleDateString("en-US", {
      weekday: "long",
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  return (
    <div className="bkv3-review">
      <div className="bkv3-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Service</h4>
          <p className="bodytext-4--no-margin">
            In-store appointment: {selectedService?.title}
          </p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(3)}
          className="bkv3-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv3-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Venue</h4>
          <p className="bodytext-4--no-margin">{selectedVenue?.name}</p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(2)}
          className="bkv3-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv3-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Date</h4>
          <p className="bodytext-4--no-margin">{formatDate(selectedDate)}</p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(4)}
          className="bkv3-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv3-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Time</h4>
          <p className="bodytext-4--no-margin">{selectedTime}, GMT +7</p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(4)}
          className="bkv3-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv3-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Customer name</h4>
          <p className="bodytext-4--no-margin">
            {formData.title}. {formData.firstName} {formData.lastName}
          </p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(5)}
          className="bkv3-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv3-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Email</h4>
          <p className="bodytext-4--no-margin">
            {formData.email || "Not provided"}
          </p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(5)}
          className="bkv3-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv3-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Phone / WhatsApp</h4>
          <p className="bodytext-4--no-margin">{formData.phone}</p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(5)}
          className="bkv3-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv3-privacy">
        <h4 className="bodytext-4--no-margin">Booking and privacy policy</h4>
        {/* <p className="bodytext-6--no-margin">
          By ticking the boxes below, you confirm that you have read the Privacy
          Statement accessible from the link below, and consent to the
          processing of your personal data by Christian Dior Couture for the
          purposes described in this Statement and in order to respond to your
          request. Please note that your data may be transferred to the European
          Union where Christian Dior Couture has its Headquarters. In accordance
          with applicable laws and regulations, you have the right to access,
          correct, delete any data that may concern you and to ask us not to
          send you personalized communications about our products and services.
          These rights can be exercised by contacting us from our Contact
          section in our Privacy Statement.
        </p> */}
      </div>

      <div className="bkv3-checkboxes">
        <label className="bkv3-checkbox-item">
          <input
            type="checkbox"
            checked={acceptTerms}
            onChange={(e) => setAcceptTerms(e.target.checked)}
          />
          <span className="bodytext-4--no-margin">
            I agree with the policy and accept the{" "}
            <a href="#" className="bkv3-link">
              terms & conditions
            </a>
          </span>
        </label>

        <label className="bkv3-checkbox-item">
          <input
            type="checkbox"
            checked={acceptPrivacy}
            onChange={(e) => setAcceptPrivacy(e.target.checked)}
          />
          <span className="bodytext-4--no-margin">
            I agree with the Privacy Policy
          </span>
        </label>
      </div>
    </div>
  );
};

export default BookingModalV3;
