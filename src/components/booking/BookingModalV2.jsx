import { useState, useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import "./BookingModalV2.css";
import { locationsAPI, appointmentsAPI, handleAPIError } from "@services/api";
import UnderlineButton from "@components/common/button/UnderlineButton";
import UnderlineButtonOpposite from "@components/common/button/UnderlineButtonOpposite";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import { getImageUrl } from "@/utils/cloudflareMediaUtil";

// Default left panel image from Cloudflare CDN
const DEFAULT_LEFT_IMAGE = getImageUrl(
  "10_Book an appointment/bông hoa pts copy.webp"
);

// Preload all booking images immediately when module loads (before modal opens)
if (typeof window !== "undefined") {
  const preloadDefault = new Image();
  preloadDefault.src = DEFAULT_LEFT_IMAGE;
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

// Vietnamese diacritics removal - extracted to utility
const removeDiacritics = (str) => {
  const vietnameseMap = {
    à: "a",
    á: "a",
    ạ: "a",
    ả: "a",
    ã: "a",
    â: "a",
    ầ: "a",
    ấ: "a",
    ậ: "a",
    ẩ: "a",
    ẫ: "a",
    ă: "a",
    ằ: "a",
    ắ: "a",
    ặ: "a",
    ẳ: "a",
    ẵ: "a",
    è: "e",
    é: "e",
    ẹ: "e",
    ẻ: "e",
    ẽ: "e",
    ê: "e",
    ề: "e",
    ế: "e",
    ệ: "e",
    ể: "e",
    ễ: "e",
    ì: "i",
    í: "i",
    ị: "i",
    ỉ: "i",
    ĩ: "i",
    ò: "o",
    ó: "o",
    ọ: "o",
    ỏ: "o",
    õ: "o",
    ô: "o",
    ồ: "o",
    ố: "o",
    ộ: "o",
    ổ: "o",
    ỗ: "o",
    ơ: "o",
    ờ: "o",
    ớ: "o",
    ợ: "o",
    ở: "o",
    ỡ: "o",
    ù: "u",
    ú: "u",
    ụ: "u",
    ủ: "u",
    ũ: "u",
    ư: "u",
    ừ: "u",
    ứ: "u",
    ự: "u",
    ử: "u",
    ữ: "u",
    ỳ: "y",
    ý: "y",
    ỵ: "y",
    ỷ: "y",
    ỹ: "y",
    đ: "d",
    À: "A",
    Á: "A",
    Ạ: "A",
    Ả: "A",
    Ã: "A",
    Â: "A",
    Ầ: "A",
    Ấ: "A",
    Ậ: "A",
    Ẩ: "A",
    Ẫ: "A",
    Ă: "A",
    Ằ: "A",
    Ắ: "A",
    Ặ: "A",
    Ẳ: "A",
    Ẵ: "A",
    È: "E",
    É: "E",
    Ẹ: "E",
    Ẻ: "E",
    Ẽ: "E",
    Ê: "E",
    Ề: "E",
    Ế: "E",
    Ệ: "E",
    Ể: "E",
    Ễ: "E",
    Ì: "I",
    Í: "I",
    Ị: "I",
    Ỉ: "I",
    Ĩ: "I",
    Ò: "O",
    Ó: "O",
    Ọ: "O",
    Ỏ: "O",
    Õ: "O",
    Ô: "O",
    Ồ: "O",
    Ố: "O",
    Ộ: "O",
    Ổ: "O",
    Ỗ: "O",
    Ơ: "O",
    Ờ: "O",
    Ớ: "O",
    Ợ: "O",
    Ở: "O",
    Ỡ: "O",
    Ù: "U",
    Ú: "U",
    Ụ: "U",
    Ủ: "U",
    Ũ: "U",
    Ư: "U",
    Ừ: "U",
    Ứ: "U",
    Ự: "U",
    Ử: "U",
    Ữ: "U",
    Ỳ: "Y",
    Ý: "Y",
    Ỵ: "Y",
    Ỷ: "Y",
    Ỹ: "Y",
    Đ: "D",
  };
  return str
    .split("")
    .map((char) => vietnameseMap[char] || char)
    .join("");
};

const BookingModalV2 = ({ isOpen, onClose, initialStep = 1 }) => {
  const [currentStep, setCurrentStep] = useState(initialStep);

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

  // Fetch locations data when modal opens
  useEffect(() => {
    if (isOpen) {
      fetchLocationsData();
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
    if (currentStep < 7) {
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
      1: "Book an Appointment",
      2: "Select a venue",
      3: "Choose a service",
      4: "Choose a date",
      5: "Fill out the form",
      6: "Review your booking",
    };
    return titles[step] || "";
  };

  const getProgressPercent = () => {
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

  // Step 4: Special layout - Choose Date (left) | Choose Time (right)
  if (currentStep === 4) {
    return createPortal(
      <div className="bkv2-modal-overlay">
        <div className="bkv2-modal-container bkv2-datetime-layout">
          {/* Left Panel - Choose Date */}
          <div className="bkv2-datetime-panel bkv2-datetime-panel-left">
            <h1 className="bkv2-datetime-title heading-3--no-margin">
              Choose a date
            </h1>
            <div className="bkv2-datetime-content">
              <DatePicker
                selected={selectedDate}
                onChange={(date) => setSelectedDate(date)}
                inline
                minDate={new Date()}
                calendarClassName="bkv2-calendar bkv2-calendar-large"
                filterDate={isDateAvailable}
                renderCustomHeader={({
                  date,
                  decreaseMonth,
                  increaseMonth,
                  prevMonthButtonDisabled,
                  nextMonthButtonDisabled,
                }) => (
                  <div className="bkv2-calendar-header">
                    <button
                      type="button"
                      onClick={decreaseMonth}
                      disabled={prevMonthButtonDisabled}
                      className="bkv2-calendar-nav"
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
                    <div className="bkv2-calendar-month bodytext-4--no-margin">
                      {date.toLocaleString("en-US", {
                        month: "long",
                        year: "numeric",
                      })}
                    </div>
                    <button
                      type="button"
                      onClick={increaseMonth}
                      disabled={nextMonthButtonDisabled}
                      className="bkv2-calendar-nav"
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
                    {day.substring(0, 3).toLowerCase()}
                  </span>
                )}
                dayClassName={() => "bodytext-6--no-margin"}
              />
            </div>
          </div>

          {/* Right Panel - Choose Time */}
          <div className="bkv2-datetime-panel bkv2-datetime-panel-right">
            <div className="bkv2-header">
              <button className="bkv2-back-btn" onClick={handleBack}>
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
              <div className="bkv2-progress-bar">
                <div
                  className="bkv2-progress-fill"
                  style={{ width: `${getProgressPercent()}%` }}
                />
              </div>
              <button className="bkv2-close-btn" onClick={onClose}>
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

            <h1 className="bkv2-step-title heading-3--no-margin">
              Choose a time
            </h1>

            <div className="bkv2-content">
              <div className="bkv2-time-only">
                {!selectedDate ? (
                  <div className="bkv2-time-placeholder">
                    <p className="bodytext-4--no-margin">
                      Please select a date first
                    </p>
                  </div>
                ) : (
                  <>
                    <div className="bkv2-time-header">
                      <p className="bodytext-4--no-margin">
                        {selectedDate.toLocaleDateString("en-US", {
                          weekday: "long",
                          month: "short",
                          day: "numeric",
                          year: "numeric",
                        })}
                      </p>
                      <p className="bkv2-timezone bodytext-4--no-margin">
                        Indochine: GMT+7
                      </p>
                    </div>

                    {loadingSlots ? (
                      <div className="bkv2-loading">
                        Loading available times...
                      </div>
                    ) : (
                      <div className="bkv2-time-grid">
                        {TIME_SLOTS.map((time) => {
                          const booked = bookedSlots.includes(time);
                          return (
                            <button
                              key={time}
                              className={`bkv2-time-slot bodytext-4--no-margin ${
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
                  </>
                )}
              </div>
            </div>

            <div className="bkv2-footer">
              <GlassThemeButton
                theme="light"
                className="bkv2-continue-btn"
                onClick={handleContinue}
                disabled={!canContinue() || isSubmitting}
              >
                Continue
              </GlassThemeButton>
            </div>
          </div>
        </div>
      </div>,
      document.body
    );
  }

  // Step 7: Confirmation - Full width with gradient background
  if (currentStep === 7) {
    return createPortal(
      <div className="bkv2-modal-overlay">
        <div className="bkv2-modal-container bkv2-confirmation-layout">
          <div className="bkv2-confirmation-full">
            <button className="bkv2-confirmation-close" onClick={onClose}>
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
            <div className="bkv2-confirmation-content">
              <h1 className="heading-2--no-margin">
                Your Appointment is Confirmed
              </h1>
              <p className="bodytext-6--no-margin bkv2-confirmation-subtitle">
                Thank you for choosing to experience Mirror.
              </p>
              <p className="bodytext-6--no-margin bkv2-confirmation-text">
                Your visit is now in motion - you'll receive an email reminder
                with all details soon.
              </p>
              <div className="bkv2-confirmation-actions">
                <GlassThemeButton
                  theme="dark"
                  className="bkv2-confirmation-btn"
                  onClick={() => {
                    onClose();
                    window.location.href = "/";
                  }}
                >
                  Back to homepage
                </GlassThemeButton>
                <GlassThemeButton
                  theme="spec_dark"
                  className="bkv2-confirmation-btn"
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
    <div className="bkv2-modal-overlay">
      <div className="bkv2-modal-container">
        {/* Left Panel - Image or Map */}
        <div className="bkv2-left-panel">
          {currentStep === 2 ? (
            <div className="bkv2-map-container">
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
                className="bkv2-map-link bodytext-6--no-margin"
              >
                View larger map
              </a>
              <iframe
                ref={mapIframeRef}
                src={getMapUrl()}
                width="100%"
                height="100%"
                className={`bkv2-map-iframe ${mapLoading ? "loading" : ""}`}
                style={{ border: 0 }}
                allowFullScreen=""
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
                title="Store Locations Map"
                onLoad={() => setMapLoading(false)}
              />
            </div>
          ) : (
            <img
              key={
                currentStep === 3
                  ? selectedService?.id || "default"
                  : currentStep
              }
              src={
                currentStep === 3
                  ? selectedService?.image || DEFAULT_LEFT_IMAGE
                  : DEFAULT_LEFT_IMAGE
              }
              alt="Mirror Diamond"
              className="bkv2-left-image"
            />
          )}
        </div>

        {/* Right Panel - Content */}
        <div className="bkv2-right-panel">
          {currentStep < 7 && (
            <>
              {/* Header with back button, progress bar, and close button */}
              <div className="bkv2-header">
                {currentStep > 1 && (
                  <button className="bkv2-back-btn" onClick={handleBack}>
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
                  <div className="bkv2-progress-bar">
                    <div
                      className="bkv2-progress-fill"
                      style={{ width: `${getProgressPercent()}%` }}
                    />
                  </div>
                )}

                <button className="bkv2-close-btn" onClick={onClose}>
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
              <h1 className="bkv2-step-title heading-3--no-margin">
                {getStepTitle(currentStep)}
              </h1>

              {/* Step Content */}
              <div className="bkv2-content" key={currentStep}>
                {currentStep === 1 && <StepIntro />}
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
                    bookedSlots={bookedSlots}
                    loadingSlots={loadingSlots}
                    unavailableDates={unavailableDates}
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

              {/* Footer with Continue button */}
              <div className="bkv2-footer">
                {submissionError && currentStep === 6 && (
                  <div className="bkv2-error-message">{submissionError}</div>
                )}
                <GlassThemeButton
                  theme="light"
                  className="bkv2-continue-btn"
                  onClick={handleContinue}
                  disabled={!canContinue() || isSubmitting}
                >
                  {isSubmitting
                    ? "Submitting..."
                    : currentStep === 6
                    ? "Booking"
                    : "Continue"}
                </GlassThemeButton>
              </div>
            </>
          )}

          {/* Confirmation Page */}
          {currentStep === 7 && (
            <StepConfirmation
              onClose={onClose}
              bookingReference={bookingReference}
            />
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
    <div className="bkv2-intro">
      <div className="bkv2-intro-header">
        <span className="bodytext-6--no-margin">
          Immerse in the Mirror Experience
        </span>
        <span className="bkv2-intro-divider">|</span>
        <span className="bkv2-intro-duration">
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
      </div>

      <div className="bkv2-intro-section">
        <h4 className="bodytext-4--no-margin">What to Expect</h4>
        <p className="bodytext-6--no-margin">
          Step into an intimate, sensory-crafted space where your story becomes
          design. During your visit, our stylists will curate:
        </p>
        <ul>
          <li className="bodytext-6--no-margin">
            A selection of Mirror's signature pieces for you to try on
          </li>
          <li className="bodytext-6--no-margin">
            Style guidance to help you discover the piece that reflects you most
          </li>
        </ul>
        <p className="bodytext-6--no-margin">
          Each creation is made-to-order, crafted with intention and precision.
        </p>
        <p className="bodytext-6--no-margin">
          If you have any preferences - specific designs, collections, or
          diamond cuts, simply let us know beforehand.
        </p>
      </div>

      <div className="bkv2-intro-section">
        <h4 className="bodytext-4--no-margin">Before Your Visit</h4>
        <p className="bodytext-6--no-margin">
          To preserve a calm and personal experience, we welcome:
        </p>
        <ul>
          <li className="bodytext-6--no-margin">
            Up to 2 guests per appointment
          </li>
          <li className="bodytext-6--no-margin">
            Polite pets - dogs are welcome in our boutique
          </li>
          <li className="bodytext-6--no-margin">
            Punctual arrival: while we'll accommodate when possible, extensions
            may not be available during peak hours
          </li>
        </ul>
        <p className="bodytext-6--no-margin">
          Your time at Mirror is designed to feel unhurried, attentive, and
          beautifully reflective - a moment just for you.
        </p>
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
      if (!event.target.closest(".bkv2-city-filter")) {
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
    <div className="bkv2-venue">
      <div className="bkv2-filter-section">
        <label className="bodytext-6--no-margin">
          Filter by City/Province:
        </label>
        <div className="bkv2-city-filter">
          <div
            className={`bkv2-dropdown-selected ${
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
            <div className="bkv2-dropdown-options">
              {cities.map((city) => (
                <div
                  key={city}
                  className={`bkv2-dropdown-option bodytext-6--no-margin ${
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
        <div className="bkv2-loading">Loading venues...</div>
      ) : filteredLocations.length === 0 ? (
        <div className="bkv2-empty">No venues available</div>
      ) : (
        <div className="bkv2-venue-list">
          {filteredLocations.map((venue) => (
            <div
              key={venue.id}
              className={`bkv2-venue-item ${
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
    <div className="bkv2-service">
      <p className="bkv2-service-subtitle bodytext-4--no-margin">
        Tailored guidance for every step of your Mirror journey.
      </p>

      <div className="bkv2-service-list">
        {SERVICES.map((service) => (
          <div
            key={service.id}
            className={`bkv2-service-item ${
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
                className="bkv2-service-input"
              />
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

// Step 4: Choose Date & Time (Combined)
const StepDateTime = ({
  selectedDate,
  setSelectedDate,
  selectedTime,
  setSelectedTime,
  bookedSlots,
  loadingSlots,
  unavailableDates = [],
}) => {
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
  const renderCustomHeader = ({
    date,
    decreaseMonth,
    increaseMonth,
    prevMonthButtonDisabled,
    nextMonthButtonDisabled,
  }) => (
    <div className="bkv2-calendar-header">
      <button
        type="button"
        onClick={decreaseMonth}
        disabled={prevMonthButtonDisabled}
        className="bkv2-calendar-nav"
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
      <div className="bkv2-calendar-month bodytext-4--no-margin">
        {date.toLocaleString("en-US", { month: "long", year: "numeric" })}
      </div>
      <button
        type="button"
        onClick={increaseMonth}
        disabled={nextMonthButtonDisabled}
        className="bkv2-calendar-nav"
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
  );

  const formatDate = (date) => {
    if (!date) return "Select a date";
    return date.toLocaleDateString("en-US", {
      weekday: "long",
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  const isSlotBooked = (time) => bookedSlots.includes(time);

  return (
    <div className="bkv2-datetime">
      <div className="bkv2-datetime-left">
        <DatePicker
          selected={selectedDate}
          onChange={(date) => {
            setSelectedDate(date);
            setSelectedTime(null); // Reset time when date changes
          }}
          inline
          minDate={new Date()}
          calendarClassName="bkv2-calendar"
          filterDate={isDateAvailable}
          renderCustomHeader={renderCustomHeader}
          formatWeekDay={(day) => (
            <span className="bodytext-6--no-margin">
              {day.substring(0, 3).toLowerCase()}
            </span>
          )}
          dayClassName={(date) => {
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const checkDate = new Date(date);
            checkDate.setHours(0, 0, 0, 0);
            const classes = ["bodytext-4--no-margin"];
            if (checkDate.getTime() === today.getTime()) {
              classes.push("today");
            }
            return classes.join(" ");
          }}
        />
      </div>

      <div className="bkv2-datetime-right">
        <div className="bkv2-time-header">
          <p className="bodytext-4--no-margin">{formatDate(selectedDate)}</p>
          <p className="bkv2-timezone bodytext-4--no-margin">
            Indochine: GMT+7
          </p>
        </div>

        {!selectedDate ? (
          <div className="bkv2-time-placeholder">
            Please select a date first
          </div>
        ) : loadingSlots ? (
          <div className="bkv2-loading">Loading available times...</div>
        ) : (
          <div className="bkv2-time-grid">
            {TIME_SLOTS.map((time) => {
              const booked = isSlotBooked(time);
              return (
                <button
                  key={time}
                  className={`bkv2-time-slot bodytext-4--no-margin ${
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
    <div className="bkv2-form">
      <div className="bkv2-form-title-group">
        <label className="bodytext-6--no-margin">Title *</label>
        <div className="bkv2-form-title-options">
          {titles.map((title) => (
            <UnderlineButton
              key={title}
              onClick={() => handleTitleSelect(title)}
              className={`bkv2-form-title-option ${
                formData.title === title ? "active" : ""
              }`}
            >
              {title}
            </UnderlineButton>
          ))}
        </div>
      </div>

      <div className="bkv2-form-field">
        <label className="bodytext-6--no-margin">First name *</label>
        <input
          type="text"
          placeholder="Your first name"
          value={formData.firstName}
          onChange={(e) => handleChange("firstName", e.target.value)}
          className="bkv2-form-input bodytext-4--no-margin"
        />
      </div>

      <div className="bkv2-form-field">
        <label className="bodytext-6--no-margin">Last name *</label>
        <input
          type="text"
          placeholder="Nguyen"
          value={formData.lastName}
          onChange={(e) => handleChange("lastName", e.target.value)}
          className="bkv2-form-input bodytext-4--no-margin"
        />
      </div>

      <div className="bkv2-form-field">
        <label className="bodytext-6--no-margin">Email</label>
        <input
          type="email"
          placeholder="Your email"
          value={formData.email}
          onChange={(e) => handleChange("email", e.target.value)}
          className="bkv2-form-input bodytext-4--no-margin"
        />
      </div>

      <div className="bkv2-form-field">
        <label className="bodytext-6--no-margin">Phone / WhatsApp *</label>
        <input
          type="tel"
          placeholder="(+84) ..."
          value={formData.phone}
          onChange={(e) => handleChange("phone", e.target.value)}
          className="bkv2-form-input bodytext-4--no-margin"
        />
      </div>

      <div className="bkv2-form-field">
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
          className="bkv2-form-textarea bodytext-4--no-margin"
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
    <div className="bkv2-review">
      <div className="bkv2-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Service</h4>
          <p className="bodytext-4--no-margin">
            In-store appointment: {selectedService?.title}
          </p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(3)}
          className="bkv2-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv2-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Venue</h4>
          <p className="bodytext-4--no-margin">{selectedVenue?.name}</p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(2)}
          className="bkv2-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv2-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Date</h4>
          <p className="bodytext-4--no-margin">{formatDate(selectedDate)}</p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(4)}
          className="bkv2-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv2-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Time</h4>
          <p className="bodytext-4--no-margin">{selectedTime}, GMT +7</p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(4)}
          className="bkv2-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv2-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Customer name</h4>
          <p className="bodytext-4--no-margin">
            {formData.title}. {formData.firstName} {formData.lastName}
          </p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(5)}
          className="bkv2-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv2-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Email</h4>
          <p className="bodytext-4--no-margin">
            {formData.email || "Not provided"}
          </p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(5)}
          className="bkv2-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv2-review-item">
        <div>
          <h4 className="bodytext-6--no-margin">Phone / WhatsApp</h4>
          <p className="bodytext-4--no-margin">{formData.phone}</p>
        </div>
        <UnderlineButtonOpposite
          onClick={() => onEdit(5)}
          className="bkv2-review-edit"
        >
          Edit
        </UnderlineButtonOpposite>
      </div>

      <div className="bkv2-privacy">
        <h4 className="bodytext-4--no-margin">Booking and privacy policy</h4>
        <p className="bodytext-6Confirm Booking--no-margin">
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
        </p>
      </div>

      <div className="bkv2-checkboxes">
        <label className="bkv2-checkbox-item">
          <input
            type="checkbox"
            checked={acceptTerms}
            onChange={(e) => setAcceptTerms(e.target.checked)}
          />
          <span className="bodytext-4--no-margin">
            I agree with the policy and accept the{" "}
            <a href="#" className="bkv2-link">
              terms & conditions
            </a>
          </span>
        </label>

        <label className="bkv2-checkbox-item">
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

// Step 7: Confirmation
const StepConfirmation = ({ onClose, bookingReference }) => {
  return (
    <div className="bkv2-confirmation">
      <h1 className="heading-2--no-margin">Your Appointment is Confirmed</h1>
      <p className="bodytext-4--no-margin bkv2-confirmation-subtitle">
        Thank you for choosing to experience Mirror.
      </p>
      <p className="bodytext-4--no-margin bkv2-confirmation-text">
        Your visit is now in motion - you'll receive an email reminder with all
        details soon.
      </p>

      <div className="bkv2-confirmation-actions">
        <ShineGlassButton
          theme="light"
          className="bkv2-confirmation-btn"
          onClick={() => {
            onClose();
            window.location.href = "/";
          }}
        >
          Back to homepage
        </ShineGlassButton>
        <ShineGlassButton
          className="bkv2-confirmation-btn bkv2-confirmation-btn-dark"
          onClick={onClose}
        >
          Continue scrolling
        </ShineGlassButton>
      </div>
    </div>
  );
};

export default BookingModalV2;
