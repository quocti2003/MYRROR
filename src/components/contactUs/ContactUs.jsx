import { useState } from "react";
import "./ContactUs.css";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import UnderlineButtonOpposite from "@components/common/button/UnderlineButtonOpposite";
import { MediaImage } from "@components/common/media";
import BookingModalV3 from "@components/booking/BookingModalV3";

const ContactUs = () => {
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);

  const handleBookAppointmentClick = () => {
    setIsBookingModalOpen(true);
  };

  const handleLiveChatClick = () => {
    // TODO: Open live chat widget
    console.log("Open live chat");
  };

  const handlePhoneCallClick = () => {
    window.location.href = "tel:+442077589780";
  };

  return (
    <section className="contact-us-hero">
      <MediaImage
        src="contactUs/ReachOut.svg"
        alt="Contact Us Background"
        className="contact-us-hero-background"
      />
      <div className="contact-us-gradient-box"></div>
      <div className="contact-us-hero-content">
        <h4 className="bodytext-4--no-margin">NEED HELP?</h4>
        <h1 className="heading-1--no-margin">REACH OUT</h1>
        <p className="bodytext-4--no-margin">We would love to hear from you.</p>
        <p className="bodytext-4--no-margin">
          Our client care experts are always here to help.
        </p>
        <div className="contact-us-button-wrapper">
          <ShineGlassButton
            theme="footer"
            onClick={handleBookAppointmentClick}
            className="contact-us-button"
          >
            Book an Appointment
          </ShineGlassButton>
        </div>
        <div className="contact-us-actions">
          <UnderlineButtonOpposite
            onClick={handleLiveChatClick}
            textClassName="bodytext-6--no-margin"
          >
            Start a live chat
          </UnderlineButtonOpposite>
          <span className="contact-us-separator bodytext-6--no-margin">or</span>
          <UnderlineButtonOpposite
            onClick={handlePhoneCallClick}
            textClassName="bodytext-6--no-margin"
          >
            Make a phone call
          </UnderlineButtonOpposite>
        </div>
      </div>

      {/* Booking Modal */}
      <BookingModalV3
        isOpen={isBookingModalOpen}
        onClose={() => setIsBookingModalOpen(false)}
      />
    </section>
  );
};

export default ContactUs;
