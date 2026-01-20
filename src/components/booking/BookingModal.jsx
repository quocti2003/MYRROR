import { useState, useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import "./BookingModal.css";
import { locationsAPI, appointmentsAPI, handleAPIError } from "@services/api";
import UnderlineButton from "@components/common/button/UnderlineButton";
import ShineGlassButton from "@components/common/button/ShineGlassButton";

const BookingModal = ({ isOpen, onClose, initialStep = 1 }) => {
  const [currentStep, setCurrentStep] = useState(initialStep);
  const [nextStep, setNextStep] = useState(null);
  const [isAnimating, setIsAnimating] = useState(false);
  const [noTransition, setNoTransition] = useState(false);
  const [hasNavigatedBack, setHasNavigatedBack] = useState(false);

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

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submissionError, setSubmissionError] = useState(null);
  const [bookingReference, setBookingReference] = useState(null);

  const [bookingData, setBookingData] = useState({
    venue: null,
    service: null,
    date: null,
    time: null,
    formData: {
      title: "",
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      preferences: "",
    },
  });

  // Fetch locations data when modal opens
  useEffect(() => {
    if (isOpen) {
      fetchLocationsData();
    }
  }, [isOpen]);

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

  // Fetch booked slots when date or venue changes
  useEffect(() => {
    const fetchBookedSlots = async () => {
      if (!selectedDate || !selectedVenue) {
        setBookedSlots([]);
        return;
      }

      setLoadingSlots(true);
      try {
        // Format date as YYYY-MM-DD for the API (use local date to avoid timezone issues)
        const year = selectedDate.getFullYear();
        const month = String(selectedDate.getMonth() + 1).padStart(2, '0');
        const day = String(selectedDate.getDate()).padStart(2, '0');
        const dateStr = `${year}-${month}-${day}`;
        console.log('[BookingModal] Fetching slots for:', { dateStr, venueId: selectedVenue.id });
        const response = await appointmentsAPI.getBookedSlots(dateStr, selectedVenue.id);
        console.log('[BookingModal] API response:', response.data);

        // Convert LocalTime strings (e.g., "09:00:00") to display format (e.g., "09:00 AM")
        const booked = (response.data?.bookedSlots || []).map(timeStr => {
          const [hours, minutes] = timeStr.split(':');
          const hour = parseInt(hours, 10);
          const ampm = hour >= 12 ? 'PM' : 'AM';
          const displayHour = hour % 12 || 12;
          return `${displayHour.toString().padStart(2, '0')}:${minutes} ${ampm}`;
        });

        console.log('[BookingModal] Converted booked slots:', booked);
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
          const month = String(d.getMonth() + 1).padStart(2, '0');
          const day = String(d.getDate()).padStart(2, '0');
          return `${year}-${month}-${day}`;
        };

        console.log('[BookingModal] Fetching unavailable dates for venue:', selectedVenue.id);
        const response = await appointmentsAPI.getUnavailableDates(
          formatDate(today),
          formatDate(endDate),
          selectedVenue.id
        );

        // Convert date strings to Date objects for comparison
        const dates = (response.data?.unavailableDates || []).map(dateStr => new Date(dateStr + 'T00:00:00'));
        console.log('[BookingModal] Unavailable dates:', dates);
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

    // Cleanup function to restore scroll when component unmounts
    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isOpen]);

  // Reset state when modal closes
  useEffect(() => {
    if (!isOpen) {
      setTimeout(() => {
        setCurrentStep(1);
        setNextStep(null);
        setIsAnimating(false);
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
        setBookingData({
          venue: null,
          service: null,
          date: null,
          time: null,
          formData: {
            title: "",
            firstName: "",
            lastName: "",
            email: "",
            phone: "",
            preferences: "",
          },
        });
        setAcceptTerms(false);
        setAcceptPrivacy(false);
        setHasNavigatedBack(false);
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

  const handleNext = (data) => {
    // Update booking data
    setBookingData((prev) => ({ ...prev, ...data }));

    // Reset hasNavigatedBack when moving to step 5 or later
    if (currentStep + 1 >= 5) {
      setHasNavigatedBack(false);
    }

    // If going to confirmation page (step 7), skip carousel sliding animation
    if (currentStep + 1 === 7) {
      setCurrentStep(7);
      return;
    }

    // Set next step first (render next slide off-screen)
    setNextStep(currentStep + 1);

    // Wait for browser to render the next slide, then trigger animation
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        setIsAnimating(true);
      });
    });

    // After slide animation completes
    setTimeout(() => {
      // Enable no-transition mode
      setNoTransition(true);

      // Update content and snap back to position 0 at the same time
      requestAnimationFrame(() => {
        setCurrentStep(currentStep + 1);
        setNextStep(null);
        setIsAnimating(false);

        // Re-enable transitions after snap completes
        requestAnimationFrame(() => {
          setNoTransition(false);
        });
      });
    }, 500);
  };

  const handleBack = () => {
    setHasNavigatedBack(true);
    setCurrentStep((prev) => prev - 1);
  };

  const handleEdit = (step) => {
    setHasNavigatedBack(true);
    setCurrentStep(step);
  };

  const canContinue = () => {
    switch (currentStep) {
      case 1: // Select Venue
        return !!selectedVenue;
      case 2: // Choose Service
        return !!selectedService;
      case 3: // Choose Date
        return !!selectedDate;
      case 4: // Choose Time
        return !!selectedTime;
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
    const [time, period] = timeStr.split(' ');
    let [hours, minutes] = time.split(':');
    hours = parseInt(hours, 10);

    if (period === 'PM' && hours !== 12) {
      hours += 12;
    } else if (period === 'AM' && hours === 12) {
      hours = 0;
    }

    return `${hours.toString().padStart(2, '0')}:${minutes}:00`;
  };

  // Submit booking to backend
  const submitBooking = async () => {
    setIsSubmitting(true);
    setSubmissionError(null);

    try {
      // Prepare appointment data (use local date to avoid timezone issues)
      const year = selectedDate.getFullYear();
      const month = String(selectedDate.getMonth() + 1).padStart(2, '0');
      const day = String(selectedDate.getDate()).padStart(2, '0');
      const appointmentData = {
        customerTitle: formData.title,
        customerFirstName: formData.firstName,
        customerLastName: formData.lastName,
        customerEmail: formData.email || '',
        customerPhone: formData.phone,
        venueId: selectedVenue.id,
        service: selectedService?.title || '',
        appointmentDate: `${year}-${month}-${day}`, // YYYY-MM-DD (local date)
        appointmentTime: convertTo24HourFormat(selectedTime), // HH:mm:ss
        language: 'en',
        preferences: formData.preferences || '',
      };

      const response = await appointmentsAPI.create(appointmentData);

      // Store booking reference
      setBookingReference(response.data?.id || 'APT000000');

      // Move to confirmation step
      handleNext({});
    } catch (err) {
      const errorInfo = handleAPIError(err, 'Failed to submit booking');
      setSubmissionError(errorInfo.message);
      console.error('Booking submission error:', errorInfo);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleContinue = () => {
    if (!canContinue()) return;

    let data = {};

    switch (currentStep) {
      case 1: // Select Venue
        data = { venue: selectedVenue };
        break;
      case 2: // Choose Service
        data = { service: selectedService, otherServiceText };
        break;
      case 3: // Choose Date
        data = { date: selectedDate };
        break;
      case 4: // Choose Time
        data = { time: selectedTime };
        break;
      case 5: // Fill Form
        data = { formData };
        break;
      case 6: // Review - Submit to backend
        submitBooking();
        return; // Don't call handleNext here, submitBooking will do it on success
      default:
        break;
    }

    handleNext(data);
  };

  if (!isOpen) return null;

  return createPortal(
    <div className="booking-modal-overlay">
      <div className="booking-modal-container">
        {/* Close button - fixed position, outside carousel */}
        {currentStep < 7 && (
          <button className="booking-modal-close" onClick={onClose}>
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
        )}

        <div className="booking-modal-content">
          <div
            className={`booking-carousel ${isAnimating ? "sliding" : ""} ${
              noTransition ? "no-transition" : ""
            } ${currentStep === 7 ? "show-confirmation" : ""}`}
          >
            {/* Left panel with its own tab */}
            <div className="booking-panel booking-panel-left">
              {currentStep === 1 ? (
                <>
                  <div className="booking-panel-tab-row">
                    <div className="booking-panel-tab bodytext-4--no-margin">
                      {getStepName(0)}
                    </div>
                  </div>
                  <div className="booking-panel-content">
                    <StepInfo currentStep={1} bookingData={bookingData} />
                  </div>
                </>
              ) : currentStep < 7 ? (
                <>
                  <div className="booking-panel-tab-row">
                    {currentStep > 1 && (
                      <button className="booking-back-btn" onClick={handleBack}>
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
                    <div className="booking-panel-tab bodytext-4--no-margin">
                      {getStepName(currentStep - 1)}
                    </div>
                  </div>
                  <div className="booking-panel-content">
                    {renderPreviousStepReadonly(
                      currentStep - 1,
                      bookingData,
                      locations,
                      cities,
                      loading,
                      {
                        selectedVenue,
                        setSelectedVenue,
                        selectedService,
                        setSelectedService,
                        otherServiceText,
                        setOtherServiceText,
                        selectedDate,
                        setSelectedDate,
                        selectedTime,
                        setSelectedTime,
                        formData,
                        setFormData,
                        unavailableDates,
                      },
                      handleNext
                    )}
                  </div>
                </>
              ) : (
                <div className="booking-panel-content">
                  {renderPreviousStepReadonly(
                    currentStep - 1,
                    bookingData,
                    locations,
                    cities,
                    loading,
                    {
                      selectedVenue,
                      setSelectedVenue,
                      selectedService,
                      setSelectedService,
                      otherServiceText,
                      setOtherServiceText,
                      selectedDate,
                      setSelectedDate,
                      selectedTime,
                      setSelectedTime,
                      formData,
                      setFormData,
                      unavailableDates,
                    },
                    handleNext
                  )}
                </div>
              )}
            </div>

            {/* Right panel with its own tab */}
            <div className="booking-panel booking-panel-right">
              {currentStep < 7 && (
                <div className="booking-panel-tab-row">
                  <div className="booking-panel-tab bodytext-4--no-margin">
                    {getStepName(currentStep)}
                  </div>
                </div>
              )}
              <div className="booking-panel-content">
                {renderStep(
                  currentStep,
                  handleNext,
                  handleBack,
                  handleEdit,
                  bookingData,
                  onClose,
                  {
                    selectedVenue,
                    setSelectedVenue,
                    selectedService,
                    setSelectedService,
                    otherServiceText,
                    setOtherServiceText,
                    selectedDate,
                    setSelectedDate,
                    selectedTime,
                    setSelectedTime,
                    formData,
                    setFormData,
                    locations,
                    cities,
                    loading,
                    acceptTerms,
                    setAcceptTerms,
                    acceptPrivacy,
                    setAcceptPrivacy,
                    bookedSlots,
                    loadingSlots,
                    unavailableDates,
                  }
                )}
              </div>
              {currentStep < 7 && (
                <div
                  className={`booking-panel-footer ${
                    isAnimating ? "animating" : ""
                  }`}
                >
                  {submissionError && currentStep === 6 && (
                    <div className="booking-error-message">
                      {submissionError}
                    </div>
                  )}
                  <ShineGlassButton
                    theme="light"
                    className="booking-continue-btn"
                    onClick={handleContinue}
                    disabled={!canContinue() || isSubmitting}
                  >
                    {isSubmitting ? "Submitting..." : currentStep === 6 ? "Confirm Booking" : "Continue"}
                  </ShineGlassButton>
                </div>
              )}
            </div>

            {/* Next panel with its own tab */}
            {nextStep && (
              <div className="booking-panel booking-panel-next">
                {nextStep < 7 && (
                  <div className="booking-panel-tab-row">
                    <div className="booking-panel-tab bodytext-4--no-margin">
                      {getStepName(nextStep)}
                    </div>
                  </div>
                )}
                <div className="booking-panel-content">
                  {renderStep(
                    nextStep,
                    handleNext,
                    handleBack,
                    handleEdit,
                    bookingData,
                    onClose,
                    {
                      selectedVenue,
                      setSelectedVenue,
                      selectedService,
                      setSelectedService,
                      otherServiceText,
                      setOtherServiceText,
                      selectedDate,
                      setSelectedDate,
                      selectedTime,
                      setSelectedTime,
                      formData,
                      setFormData,
                      locations,
                      cities,
                      loading,
                      acceptTerms,
                      setAcceptTerms,
                      acceptPrivacy,
                      setAcceptPrivacy,
                      bookedSlots,
                      loadingSlots,
                      unavailableDates,
                    }
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Confirmation Page - outside carousel */}
          <div className={`booking-panel-confirmation ${currentStep === 7 ? "show" : ""}`}>
            <ConfirmationPage onClose={onClose} bookingReference={bookingReference} />
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
};

// Helper function to get step name
const getStepName = (step) => {
  const stepNames = {
    0: "Book an Appointment", // Left panel at step 1 (StepInfo)
    1: "Select a venue",
    2: "Choose a service",
    3: "Choose a date",
    4: "Choose a time",
    5: "Fill out the form",
    6: "Review your booking",
  };

  return stepNames[step] || "";
};

// Helper function to render step content
const renderStep = (
  step,
  onNext,
  onBack,
  onEdit,
  bookingData,
  onClose,
  stateProps = {}
) => {
  const {
    selectedVenue,
    setSelectedVenue,
    selectedService,
    setSelectedService,
    otherServiceText,
    setOtherServiceText,
    selectedDate,
    setSelectedDate,
    selectedTime,
    setSelectedTime,
    formData,
    setFormData,
    locations,
    cities,
    loading,
  } = stateProps;

  switch (step) {
    case 1:
      return (
        <SelectVenue
          onNext={onNext}
          selectedVenue={selectedVenue}
          setSelectedVenue={setSelectedVenue}
          locations={locations}
          cities={cities}
          loading={loading}
        />
      );
    case 2:
      return (
        <ChooseService
          onNext={onNext}
          onBack={onBack}
          selectedService={selectedService}
          setSelectedService={setSelectedService}
          otherServiceText={otherServiceText}
          setOtherServiceText={setOtherServiceText}
        />
      );
    case 3:
      return (
        <ChooseDate
          onNext={onNext}
          onBack={onBack}
          selectedDate={selectedDate}
          setSelectedDate={setSelectedDate}
          unavailableDates={stateProps.unavailableDates}
        />
      );
    case 4:
      return (
        <ChooseTime
          onNext={onNext}
          onBack={onBack}
          selectedTime={selectedTime}
          setSelectedTime={setSelectedTime}
          selectedDate={selectedDate}
          bookedSlots={stateProps.bookedSlots}
          loadingSlots={stateProps.loadingSlots}
        />
      );
    case 5:
      return (
        <FillForm
          onNext={onNext}
          onBack={onBack}
          formData={formData}
          setFormData={setFormData}
        />
      );
    case 6:
      return (
        <ReviewBooking
          onNext={onNext}
          onBack={onBack}
          bookingData={bookingData}
          onEdit={onEdit}
          acceptTerms={stateProps.acceptTerms}
          setAcceptTerms={stateProps.setAcceptTerms}
          acceptPrivacy={stateProps.acceptPrivacy}
          setAcceptPrivacy={stateProps.setAcceptPrivacy}
        />
      );
    default:
      return null;
  }
};

// Helper function to render previous step in readonly mode (for left panel)
const renderPreviousStepReadonly = (
  step,
  bookingData,
  locations,
  cities,
  loading,
  stateProps,
  handleNext
) => {
  const {
    selectedVenue,
    setSelectedVenue,
    selectedService,
    setSelectedService,
    otherServiceText,
    setOtherServiceText,
    selectedDate,
    setSelectedDate,
    selectedTime,
    setSelectedTime,
    formData,
    setFormData,
    unavailableDates,
  } = stateProps;

  switch (step) {
    case 1:
      return (
        <SelectVenueReadonly
          selectedVenue={selectedVenue}
          setSelectedVenue={setSelectedVenue}
          locations={locations}
          cities={cities}
          loading={loading}
        />
      );
    case 2:
      return (
        <ChooseServiceReadonly
          selectedService={selectedService}
          setSelectedService={setSelectedService}
          otherServiceText={otherServiceText}
          setOtherServiceText={setOtherServiceText}
        />
      );
    case 3:
      return (
        <ChooseDateReadonly
          selectedDate={selectedDate}
          setSelectedDate={setSelectedDate}
          unavailableDates={unavailableDates}
        />
      );
    case 4:
      return (
        <ChooseTimeReadonly
          selectedTime={selectedTime}
          setSelectedTime={setSelectedTime}
          selectedDate={selectedDate}
        />
      );
    case 5:
      return <FillFormReadonly formData={formData} setFormData={setFormData} />;
    default:
      return null;
  }
};

// Step Information Component
const StepInfo = ({ currentStep, bookingData }) => {
  const stepTitles = {
    1: "Book an Appointment",
    2: "Select a venue",
    3: "Choose a service",
    4: "Choose a date",
    5: "Choose a time",
    6: "Fill out the form",
  };

  const stepDescriptions = {
    1: "Immerse in the Mirror Experience",
    2: "Choose a service",
    3: "Choose a date",
    4: "Choose a time",
    5: "Fill out the form",
    6: "Review your booking",
  };

  return (
    <div className="booking-step-info">
      {/* <h2 className="booking-step-info-title">{stepTitles[currentStep]}</h2> */}
      <h3
        className="booking-step-info-subtitle bodytext-4--no-margin"
        style={{
          marginBottom: currentStep === 1 ? "36px" : "0",
          color: "rgba(121, 121, 121, 1)",
        }}
      >
        {stepDescriptions[currentStep]}
      </h3>

      {currentStep === 1 && (
        <div className="booking-step-info-details">
          <div className="booking-step-info-detail-item">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <circle
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="2"
              />
              <path
                d="M12 6v6l4 2"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
            </svg>
            <span className="bodytext-4--no-margin">45 minutes</span>
          </div>

          <h4 className="bodytext-1--no-margin">What to Expect</h4>
          <p
            className="bodytext-4--no-margin"
            style={{ color: "rgba(121, 121, 121, 1)" }}
          >
            Step into an intimate, sensory-crafted space where your story
            becomes design. During your visit, our stylists will curate:
          </p>
          <ul style={{ color: "rgba(121, 121, 121, 1)" }}>
            <li className="bodytext-4--no-margin">
              A selection of Mirror's signature pieces for you to try on
            </li>
            <li className="bodytext-4--no-margin">
              Style guidance to help you discover the piece that reflects you
              most
            </li>
          </ul>
          <p
            className="bodytext-4--no-margin"
            style={{ color: "rgba(121, 121, 121, 1)" }}
          >
            Each creation is made-to-order, crafted with intention and
            precision.
          </p>
          <p
            className="bodytext-4--no-margin"
            style={{ color: "rgba(121, 121, 121, 1)" }}
          >
            If you have any preferences - specific designs, collections, or
            diamond cuts, simply let us know beforehand.
          </p>

          <h4 className="bodytext-1--no-margin">Before Your Visit</h4>
          <p
            className="bodytext-4--no-margin"
            style={{ color: "rgba(121, 121, 121, 1)" }}
          >
            To preserve a calm and personal experience, we welcome:
          </p>
          <ul style={{ color: "rgba(121, 121, 121, 1)" }}>
            <li className="bodytext-4--no-margin">
              Up to 2 guests per appointment
            </li>
            <li className="bodytext-4--no-margin">
              Polite pets - dogs are welcome in our boutique
            </li>
            <li className="bodytext-4--no-margin">
              Punctual arrival: while we'll accommodate when possible,
              extensions may not be available during peak hours
            </li>
          </ul>
          <p
            className="bodytext-4--no-margin"
            style={{ color: "rgba(121, 121, 121, 1)" }}
          >
            Your time at Mirror is designed to feel unhurried, attentive, and
            beautifully reflective - a moment just for you.
          </p>
        </div>
      )}
    </div>
  );
};

// Step 1: Select Venue
const SelectVenue = ({
  onNext,
  selectedVenue,
  setSelectedVenue,
  locations,
  cities,
  loading,
}) => {
  const [filterCity, setFilterCity] = useState("All Cities");
  const [isCityDropdownOpen, setIsCityDropdownOpen] = useState(false);

  // Filter locations by selected city
  const filteredLocations = locations.filter((location) => {
    if (filterCity === "All Cities") return true;
    return location.city === filterCity;
  });

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!event.target.closest(".city-filter")) {
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

  const handleContinue = () => {
    if (selectedVenue) {
      onNext({ venue: selectedVenue });
    }
  };

  return (
    <div className="booking-step">
      <h2 className="booking-step-title">Select a venue</h2>

      <div className="filter-section">
        <label className="filter-label bodytext-4--no-margin">
          Filter by City/Province:
        </label>
        <div className="city-filter">
          <div
            className={`dropdown-selected ${isCityDropdownOpen ? "open" : ""}`}
            onClick={() => setIsCityDropdownOpen(!isCityDropdownOpen)}
          >
            <span
              className={`selected-text bodytext-4--no-margin ${
                filterCity === "All Cities" ? "default" : ""
              }`}
            >
              {filterCity}
            </span>
          </div>
          {isCityDropdownOpen && (
            <div className="dropdown-options">
              {cities.map((city) => (
                <div
                  key={city}
                  className={`dropdown-option bodytext-4--no-margin ${
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
        <div className="loading-message">Loading venues...</div>
      ) : filteredLocations.length === 0 ? (
        <div className="no-venues-message">No venues available</div>
      ) : (
        <div className="venue-list">
          {filteredLocations.map((venue) => (
            <div
              key={venue.id}
              className={`venue-item ${
                selectedVenue?.id === venue.id ? "selected" : ""
              }`}
              onClick={() => {
                setSelectedVenue(venue);
                onNext({ venue });
              }}
            >
              <h3 className="bodytext-1--no-margin">{venue.name}</h3>
              <p className="bodytext-4--no-margin">
                {venue.address}, {venue.ward}, {venue.city}, {venue.country}
              </p>
              <p className="bodytext-4--no-margin">{venue.phone}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// Readonly version of SelectVenue (for left panel display)
const SelectVenueReadonly = ({
  selectedVenue,
  setSelectedVenue,
  locations,
  cities,
  loading,
}) => {
  const [filterCity, setFilterCity] = useState("All Cities");
  const [isCityDropdownOpen, setIsCityDropdownOpen] = useState(false);

  // Filter locations by selected city
  const filteredLocations = locations.filter((location) => {
    if (filterCity === "All Cities") return true;
    return location.city === filterCity;
  });

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!event.target.closest(".city-filter")) {
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
    <div className="booking-step readonly">
      <h2 className="booking-step-title">Select a venue</h2>

      <div className="filter-section">
        <label className="filter-label bodytext-4--no-margin">
          Filter by City/Province:
        </label>
        <div className="city-filter">
          <div
            className={`dropdown-selected ${isCityDropdownOpen ? "open" : ""}`}
            onClick={() => setIsCityDropdownOpen(!isCityDropdownOpen)}
          >
            <span
              className={`selected-text bodytext-4--no-margin ${
                filterCity === "All Cities" ? "default" : ""
              }`}
            >
              {filterCity}
            </span>
          </div>
          {isCityDropdownOpen && (
            <div className="dropdown-options">
              {cities.map((city) => (
                <div
                  key={city}
                  className={`dropdown-option bodytext-4--no-margin ${
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
        <div className="loading-message">Loading venues...</div>
      ) : filteredLocations.length === 0 ? (
        <div className="no-venues-message">No venues available</div>
      ) : (
        <div className="venue-list">
          {filteredLocations.map((venue) => (
            <div
              key={venue.id}
              className={`venue-item ${
                selectedVenue?.id === venue.id ? "selected" : ""
              }`}
              onClick={() => {
                setSelectedVenue(venue);
              }}
            >
              <h3 className="bodytext-1--no-margin">{venue.name}</h3>
              <p className="bodytext-4--no-margin">
                {venue.address}, {venue.ward}, {venue.city}, {venue.country}
              </p>
              <p className="bodytext-4--no-margin">{venue.phone}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// Step 2: Choose Service
const ChooseService = ({
  onNext,
  onBack,
  selectedService,
  setSelectedService,
  otherServiceText,
  setOtherServiceText,
}) => {
  const services = [
    {
      id: 1,
      title: "Discover Our Collections & New Creations",
      description:
        "Explore Mirror's signature pieces and latest releases. See, feel, and experience every piece in an intimate, sensory-crafted space.",
    },
    {
      id: 2,
      title: "Personalization & Custom Design",
      description:
        "Shape your own story.\n\nLearn about Mirror's customization options: select diamond shapes, metal tones, engravings, or co-create a piece.",
    },
    {
      id: 3,
      title: "After-Sales Care & Exchanges",
      description:
        "Your relationship with Mirror continues beyond the moment of purchase. Book an appointment for resizing, cleaning, polishing, warranty support, or exchange guidance.",
    },
    {
      id: 4,
      title: "Other Dedicated Services",
      description:
        "For everything beyond the usual. Please indicate the purpose of your visit so our team can prepare thoughtfully.",
      hasInput: true,
    },
  ];

  const handleContinue = () => {
    if (selectedService) {
      onNext({ service: selectedService, otherServiceText });
    }
  };

  return (
    <div className="booking-step">
      <h2 className="booking-step-title">Choose a service</h2>
      <p className="booking-step-subtitle bodytext-4--no-margin">
        Tailored guidance for every step of your Mirror journey.
      </p>

      <div className="service-list">
        {services.map((service) => (
          <div
            key={service.id}
            className={`service-item ${
              selectedService?.id === service.id ? "selected" : ""
            }`}
            onClick={() => {
              setSelectedService(service);
              // Auto-advance for services without input field
              if (!service.hasInput) {
                onNext({ service, otherServiceText });
              }
            }}
          >
            <h3 className="bodytext-1--no-margin">{service.title}</h3>
            <p
              className="bodytext-4--no-margin"
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
              />
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

// Readonly version of ChooseService
const ChooseServiceReadonly = ({
  selectedService,
  setSelectedService,
  otherServiceText,
  setOtherServiceText,
}) => {
  const services = [
    {
      id: 1,
      title: "Discover Our Collections & New Creations",
      description:
        "Explore Mirror's signature pieces and latest releases. See, feel, and experience every piece in an intimate, sensory-crafted space.",
    },
    {
      id: 2,
      title: "Personalization & Custom Design",
      description:
        "Shape your own story.\n\nLearn about Mirror's customization options: select diamond shapes, metal tones, engravings, or co-create a piece.",
    },
    {
      id: 3,
      title: "After-Sales Care & Exchanges",
      description:
        "Your relationship with Mirror continues beyond the moment of purchase. Book an appointment for resizing, cleaning, polishing, warranty support, or exchange guidance.",
    },
    {
      id: 4,
      title: "Other Dedicated Services",
      description:
        "For everything beyond the usual. Please indicate the purpose of your visit so our team can prepare thoughtfully.",
      hasInput: true,
    },
  ];

  return (
    <div className="booking-step readonly">
      <h2 className="booking-step-title">Choose a service</h2>
      <p className="booking-step-subtitle bodytext-4--no-margin">
        Tailored guidance for every step of your Mirror journey.
      </p>

      <div className="service-list">
        {services.map((service) => (
          <div
            key={service.id}
            className={`service-item ${
              selectedService?.id === service.id ? "selected" : ""
            }`}
            onClick={() => {
              setSelectedService(service);
            }}
          >
            <h3 className="bodytext-1--no-margin">{service.title}</h3>
            <p
              className="bodytext-4--no-margin"
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
              />
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

// Step 3: Choose Date
const ChooseDate = ({ onNext, onBack, selectedDate, setSelectedDate, unavailableDates = [] }) => {
  const handleContinue = () => {
    if (selectedDate) {
      onNext({ date: selectedDate });
    }
  };

  // Filter function to disable unavailable dates
  const isDateAvailable = (date) => {
    const checkDate = new Date(date);
    checkDate.setHours(0, 0, 0, 0);

    return !unavailableDates.some(unavailableDate => {
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
    <div className="custom-calendar-header">
      <button
        type="button"
        onClick={decreaseMonth}
        disabled={prevMonthButtonDisabled}
        className="custom-nav-btn custom-nav-prev"
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
      <div className="custom-month-year bodytext-3--no-margin">
        {date.toLocaleString("en-US", { month: "long", year: "numeric" })}
      </div>
      <button
        type="button"
        onClick={increaseMonth}
        disabled={nextMonthButtonDisabled}
        className="custom-nav-btn custom-nav-next"
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

  return (
    <div className="booking-step">
      <h2 className="booking-step-title">Choose a date</h2>

      <div className="calendar-container">
        <DatePicker
          selected={selectedDate}
          onChange={(date) => {
            setSelectedDate(date);
            onNext({ date });
          }}
          inline
          minDate={new Date()}
          calendarClassName="booking-calendar"
          renderCustomHeader={renderCustomHeader}
          filterDate={isDateAvailable}
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
    </div>
  );
};

// Step 4: Choose Time
const ChooseTime = ({
  onNext,
  onBack,
  selectedTime,
  setSelectedTime,
  selectedDate,
  bookedSlots = [],
  loadingSlots = false,
}) => {
  console.log('[ChooseTime] Props received:', { bookedSlots, loadingSlots, selectedDate });

  const timeSlots = [
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

  const handleContinue = () => {
    if (selectedTime) {
      onNext({ time: selectedTime });
    }
  };

  const formatDate = (date) => {
    if (!date) return "Select a date";
    return date.toLocaleDateString("en-US", {
      weekday: "long",
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  const isSlotBooked = (time) => {
    return bookedSlots.includes(time);
  };

  return (
    <div className="booking-step">
      <h2 className="booking-step-title">Choose a time</h2>
      <div className="booking-time-header">
        <p className="booking-step-subtitle bodytext-4--no-margin">
          {formatDate(selectedDate)}
        </p>
        <p className="booking-timezone bodytext-4--no-margin">
          Indochine: GMT+7
        </p>
      </div>

      {loadingSlots ? (
        <div className="loading-message">Loading available times...</div>
      ) : (
        <div className="time-grid">
          {timeSlots.map((time) => {
            const booked = isSlotBooked(time);
            return (
              <button
                key={time}
                className={`time-slot bodytext-1--no-margin ${
                  selectedTime === time ? "selected" : ""
                } ${booked ? "booked" : ""}`}
                onClick={() => {
                  if (!booked) {
                    setSelectedTime(time);
                    onNext({ time });
                  }
                }}
                disabled={booked}
              >
                <span>{time}</span>
                {booked && <span className="booked-label">Booked</span>}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

// Step 5: Fill Form
const FillForm = ({ onNext, onBack, formData, setFormData }) => {
  const titles = ["Mr", "Mrs", "Ms"];
  const textareaRef = useRef(null);

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

  const handleChange = (field, value) => {
    const valueWithoutDiacritics = removeDiacritics(value);
    setFormData((prev) => ({ ...prev, [field]: valueWithoutDiacritics }));
  };

  const handleTitleSelect = (title) => {
    setFormData((prev) => ({ ...prev, title }));
  };

  const handleContinue = () => {
    onNext({ formData });
  };

  const handleTextareaChange = (e) => {
    handleChange("preferences", e.target.value);
    autoResizeTextarea(e.target);
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
    <div className="booking-step">
      <h2 className="booking-step-title">Fill out the form</h2>

      <div className="form-container">
        <div className="form-title-group">
          <label className="bodytext-6--no-margin">Title*</label>
          <div className="form-title-options">
            {titles.map((title) => (
              <span
                key={title}
                onClick={() => handleTitleSelect(title)}
                className={`form-title-option bodytext-6--no-margin ${
                  formData.title === title ? "active" : ""
                }`}
              >
                {title}
              </span>
            ))}
          </div>
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">First Name*</label>
          <input
            type="text"
            placeholder="Your first name"
            value={formData.firstName}
            onChange={(e) => handleChange("firstName", e.target.value)}
            className="booking-form-input bodytext-4--no-margin"
          />
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">Last Name*</label>
          <input
            type="text"
            placeholder="Nguyen"
            value={formData.lastName}
            onChange={(e) => handleChange("lastName", e.target.value)}
            className="booking-form-input bodytext-4--no-margin"
          />
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">Email Address</label>
          <input
            type="email"
            placeholder="Your email"
            value={formData.email}
            onChange={(e) => handleChange("email", e.target.value)}
            className="booking-form-input bodytext-4--no-margin"
          />
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">Phone / WhatsApp*</label>
          <input
            type="tel"
            placeholder="(+84) ..."
            value={formData.phone}
            onChange={(e) => handleChange("phone", e.target.value)}
            className="booking-form-input bodytext-4--no-margin"
          />
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">
            Please share any specific preferences you have regarding styles,
            stone shapes, and carat weights, or anything else you feel is
            important.
          </label>
          <textarea
            ref={textareaRef}
            placeholder="Type here"
            value={formData.preferences}
            onChange={handleTextareaChange}
            rows={1}
            className="booking-form-textarea bodytext-4--no-margin"
          />
        </div>
      </div>
    </div>
  );
};

// Readonly version of ChooseDate
const ChooseDateReadonly = ({ selectedDate, setSelectedDate, unavailableDates = [] }) => {
  // Filter function to disable unavailable dates
  const isDateAvailable = (date) => {
    const checkDate = new Date(date);
    checkDate.setHours(0, 0, 0, 0);

    return !unavailableDates.some(unavailableDate => {
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
    <div className="custom-calendar-header">
      <button
        type="button"
        onClick={decreaseMonth}
        disabled={prevMonthButtonDisabled}
        className="custom-nav-btn custom-nav-prev"
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
      <div className="custom-month-year bodytext-3--no-margin">
        {date.toLocaleString("en-US", { month: "long", year: "numeric" })}
      </div>
      <button
        type="button"
        onClick={increaseMonth}
        disabled={nextMonthButtonDisabled}
        className="custom-nav-btn custom-nav-next"
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

  return (
    <div className="booking-step readonly">
      <h2 className="booking-step-title">Choose a date</h2>

      <div className="calendar-container">
        <DatePicker
          selected={selectedDate}
          onChange={(date) => {
            setSelectedDate(date);
          }}
          inline
          minDate={new Date()}
          calendarClassName="booking-calendar"
          renderCustomHeader={renderCustomHeader}
          filterDate={isDateAvailable}
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
    </div>
  );
};

// Readonly version of ChooseTime
const ChooseTimeReadonly = ({
  selectedTime,
  setSelectedTime,
  selectedDate,
}) => {
  const timeSlots = [
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

  const formatDate = (date) => {
    if (!date) return "Select a date";
    return date.toLocaleDateString("en-US", {
      weekday: "long",
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  return (
    <div className="booking-step readonly">
      <h2 className="booking-step-title">Choose a time</h2>
      <div className="booking-time-header">
        <p className="booking-step-subtitle bodytext-4--no-margin">
          {formatDate(selectedDate)}
        </p>
        <p className="booking-timezone bodytext-4--no-margin">
          Indochine: GMT+7
        </p>
      </div>

      <div className="time-grid">
        {timeSlots.map((time) => (
          <button
            key={time}
            className={`time-slot bodytext-1--no-margin ${
              selectedTime === time ? "selected" : ""
            }`}
            onClick={() => {
              setSelectedTime(time);
            }}
          >
            <span>{time}</span>
          </button>
        ))}
      </div>
    </div>
  );
};

// Readonly version of FillForm
const FillFormReadonly = ({ formData, setFormData }) => {
  const titles = ["Mr", "Mrs", "Ms"];
  const textareaRef = useRef(null);

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

  const handleChange = (field, value) => {
    const valueWithoutDiacritics = removeDiacritics(value);
    setFormData((prev) => ({ ...prev, [field]: valueWithoutDiacritics }));
  };

  const handleTitleSelect = (title) => {
    setFormData((prev) => ({ ...prev, title }));
  };

  const handleTextareaChange = (e) => {
    handleChange("preferences", e.target.value);
    autoResizeTextarea(e.target);
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
    <div className="booking-step readonly">
      <h2 className="booking-step-title">Fill out the form</h2>

      <div className="form-container">
        <div className="form-title-group">
          <label className="bodytext-6--no-margin">Title*</label>
          <div className="form-title-options">
            {titles.map((title) => (
              <span
                key={title}
                onClick={() => handleTitleSelect(title)}
                className={`form-title-option bodytext-6--no-margin ${
                  formData?.title === title ? "active" : ""
                }`}
              >
                {title}
              </span>
            ))}
          </div>
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">First Name*</label>
          <input
            type="text"
            placeholder="Your first name"
            value={formData?.firstName || ""}
            onChange={(e) => handleChange("firstName", e.target.value)}
            className="booking-form-input bodytext-4--no-margin"
          />
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">Last Name*</label>
          <input
            type="text"
            placeholder="Nguyen"
            value={formData?.lastName || ""}
            onChange={(e) => handleChange("lastName", e.target.value)}
            className="booking-form-input bodytext-4--no-margin"
          />
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">Email Address</label>
          <input
            type="email"
            placeholder="Your email"
            value={formData?.email || ""}
            onChange={(e) => handleChange("email", e.target.value)}
            className="booking-form-input bodytext-4--no-margin"
          />
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">Phone / WhatsApp*</label>
          <input
            type="tel"
            placeholder="(+84) ..."
            value={formData?.phone || ""}
            onChange={(e) => handleChange("phone", e.target.value)}
            className="booking-form-input bodytext-4--no-margin"
          />
        </div>

        <div className="booking-form-field">
          <label className="bodytext-6--no-margin">
            Please share any specific preferences you have regarding styles,
            stone shapes, and carat weights, or anything else you feel is
            important.
          </label>
          <textarea
            ref={textareaRef}
            placeholder="Type here"
            value={formData?.preferences || ""}
            onChange={handleTextareaChange}
            rows={1}
            className="booking-form-textarea bodytext-4--no-margin"
          />
        </div>
      </div>
    </div>
  );
};

// Step 6: Review Booking
const ReviewBooking = ({
  onNext,
  onBack,
  bookingData,
  onEdit,
  acceptTerms,
  setAcceptTerms,
  acceptPrivacy,
  setAcceptPrivacy,
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
    <div className="booking-step review-step">
      <h2 className="booking-step-title">Review your booking</h2>

      <div className="review-container">
        <div className="review-item">
          <div>
            <h4 className="bodytext-6--no-margin">Service</h4>
            <p className="bodytext-4--no-margin">
              In-store appointment: {bookingData.service?.title}
            </p>
          </div>
          <UnderlineButton
            onClick={() => onEdit(2)}
            className="review-edit-btn"
          >
            Edit
          </UnderlineButton>
        </div>

        <div className="review-item">
          <div>
            <h4 className="bodytext-6--no-margin">Where</h4>
            <p className="bodytext-4--no-margin">{bookingData.venue?.name}</p>
          </div>
          <UnderlineButton
            onClick={() => onEdit(1)}
            className="review-edit-btn"
          >
            Edit
          </UnderlineButton>
        </div>

        <div className="review-item">
          <div>
            <h4 className="bodytext-6--no-margin">When</h4>
            <p className="bodytext-4--no-margin">{formatDate(bookingData.date)}</p>
          </div>
          <UnderlineButton
            onClick={() => onEdit(3)}
            className="review-edit-btn"
          >
            Edit
          </UnderlineButton>
        </div>

        <div className="review-item">
          <div>
            <h4 className="bodytext-6--no-margin">Time</h4>
            <p className="bodytext-4--no-margin">{bookingData.time}, GMT +7</p>
          </div>
          <UnderlineButton
            onClick={() => onEdit(4)}
            className="review-edit-btn"
          >
            Edit
          </UnderlineButton>
        </div>

        <div className="review-item">
          <div>
            <h4 className="bodytext-6--no-margin">Customer name</h4>
            <p className="bodytext-4--no-margin">
              {bookingData.formData?.title}. {bookingData.formData?.firstName}{" "}
              {bookingData.formData?.lastName}
            </p>
          </div>
          <UnderlineButton
            onClick={() => onEdit(5)}
            className="review-edit-btn"
          >
            Edit
          </UnderlineButton>
        </div>

        <div className="review-item">
          <div>
            <h4 className="bodytext-6--no-margin">Email</h4>
            <p className="bodytext-4--no-margin">
              {bookingData.formData?.email}
            </p>
          </div>
          <UnderlineButton
            onClick={() => onEdit(5)}
            className="review-edit-btn"
          >
            Edit
          </UnderlineButton>
        </div>

        <div className="review-item">
          <div>
            <h4 className="bodytext-6--no-margin">Phone / WhatsApp</h4>
            <p className="bodytext-4--no-margin">
              {bookingData.formData?.phone}
            </p>
          </div>
          <UnderlineButton
            onClick={() => onEdit(5)}
            className="review-edit-btn"
          >
            Edit
          </UnderlineButton>
        </div>

        <div className="privacy-policy">
          <h4 className="bodytext-1--no-margin">Booking and privacy policy</h4>
          <p className="bodytext-4--no-margin">
            By ticking the boxes below, you confirm that you have read the
            Privacy Statement accessible from the link below, and consent to the
            processing of your personal data by Christian Dior Couture for the
            purposes described in this Statement and in order to respond to your
            request. Please note that your data may be transferred to the
            European Union where Christian Dior Couture has its Headquarters. In
            accordance with applicable laws and regulations, you have the right
            to access, correct, delete any data that may concern you and to ask
            us not to send you personalized communications about our products
            and services. These rights can be exercised by contacting us from
            our Contact section in our Privacy Statement.
          </p>
        </div>

        <div className="review-checkboxes">
          <label className="review-checkbox-item">
            <input
              type="checkbox"
              checked={acceptTerms}
              onChange={(e) => setAcceptTerms(e.target.checked)}
            />
            <span className="bodytext-4--no-margin">
              I agree with the policy and accept the{" "}
              <a href="#" className="review-link">
                terms & conditions
              </a>
            </span>
          </label>

          <label className="review-checkbox-item">
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

        <div className="review-confirmation-text">
          <p className="bodytext-4--no-margin">
            You'll receive an email confirmation
          </p>
        </div>
      </div>
    </div>
  );
};

// Step 7: Confirmation Page
const ConfirmationPage = ({ onClose, bookingReference }) => {
  const handleHomepage = () => {
    onClose();
    window.location.href = "/";
  };

  const handleContinueScrolling = () => {
    onClose();
  };

  return (
    <div className="confirmation-page">
      <h1 className="heading-2--no-margin">Your Appointment is Confirmed</h1>
      <p className="bodytext-4--no-margin confirmation-subtitle">
        Thank you for choosing to experience Mirror.
      </p>
      <p className="bodytext-4--no-margin confirmation-text">
        Your visit is now in motion - you'll receive an email reminder with all
        details soon.
      </p>

      <div className="confirmation-actions">
        <ShineGlassButton
          theme="light"
          className="confirmation-btn-back"
          onClick={handleHomepage}
        >
          Back to homepage
        </ShineGlassButton>
        <ShineGlassButton
          className="confirmation-btn-continue"
          onClick={handleContinueScrolling}
        >
          Continue scrolling
        </ShineGlassButton>
      </div>
    </div>
  );
};

export default BookingModal;
