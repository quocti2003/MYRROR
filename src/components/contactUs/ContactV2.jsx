import React, { useState } from "react";
import UnderlineButton from "@components/common/button/UnderlineButton";
import BookingModalV3 from "@components/booking/BookingModalV3";
import "./ContactV2.css";

// Import icons
import chatIcon from "@assets/images/button/chat.png";
import callIcon from "@assets/images/button/call.png";
import mailIcon from "@assets/images/button/mail.png";
import bookingIcon from "@assets/images/button/appartment.png";

const ContactV2 = () => {
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);

  const handleBookAppointment = () => {
    setIsBookingModalOpen(true);
  };

  return (
    <section className="contact-v2">
      <div className="contact-v2-header" data-navbar-theme="white">
        <h2 className="contact-v2-title heading-1--no-margin">CONTACT US</h2>
        <p className="contact-v2-subtitle bodytext-4--no-margin">
          Have a question or concern? Our team is standing by to assist
        </p>
      </div>

      <div className="contact-v2-cards" data-navbar-theme="black">
        <div className="contact-v2-card">
          <div className="contact-v2-card-icon-wrapper">
            <img src={chatIcon} alt="Chat" className="contact-v2-card-icon" />
          </div>
          <div className="contact-v2-card-content">
            <h3 className="contact-v2-card-title bodytext-1--no-margin">
              LIVE CHAT
            </h3>
            <p className="contact-v2-card-details bodytext-4--no-margin">
              Available Mon - Fri 09:00-21:00
            </p>
            <UnderlineButton
              className="contact-v2-card-link"
              textClassName="bodytext-6--no-margin"
            >
              Chat now
            </UnderlineButton>
          </div>
          <span className="contact-v2-card-arrow">›</span>
        </div>

        <div className="contact-v2-card">
          <div className="contact-v2-card-icon-wrapper">
            <img src={callIcon} alt="Call" className="contact-v2-card-icon" />
          </div>
          <div className="contact-v2-card-content">
            <h3 className="contact-v2-card-title bodytext-1--no-margin">
              PHONE CALL
            </h3>
            <p className="contact-v2-card-details bodytext-4--no-margin">
              TEL: +44 (0) 207 758 9780
              <br />
              Available Mon - Fri 09:00-19:00
            </p>
            <a href="tel:+442077589780">
              <UnderlineButton
                className="contact-v2-card-link"
                textClassName="bodytext-6--no-margin"
              >
                Call now
              </UnderlineButton>
            </a>
          </div>
          <span className="contact-v2-card-arrow">›</span>
        </div>

        <div className="contact-v2-card">
          <div className="contact-v2-card-icon-wrapper">
            <img src={mailIcon} alt="Email" className="contact-v2-card-icon" />
          </div>
          <div className="contact-v2-card-content">
            <h3 className="contact-v2-card-title bodytext-1--no-margin">
              EMAIL
            </h3>
            <p className="contact-v2-card-details bodytext-4--no-margin">
              support@mirrorfuturediamond.com
              <br />
              Available: 24/7
            </p>
            <a href="mailto:support@mirrorfuturediamond.com">
              <UnderlineButton
                className="contact-v2-card-link"
                textClassName="bodytext-6--no-margin"
              >
                Send email
              </UnderlineButton>
            </a>
          </div>
          <span className="contact-v2-card-arrow">›</span>
        </div>

        <div
          className="contact-v2-card clickable"
          onClick={handleBookAppointment}
        >
          <div className="contact-v2-card-icon-wrapper">
            <img
              src={bookingIcon}
              alt="Booking"
              className="contact-v2-card-icon"
            />
          </div>
          <div className="contact-v2-card-content">
            <h3 className="contact-v2-card-title bodytext-1--no-margin">
              APPOINTMENT
            </h3>
            <p className="contact-v2-card-details bodytext-4--no-margin">
              Book an in-store appointments <br />
              Available: Mon – Fri 09:00-19:00
            </p>
            <UnderlineButton
              className="contact-v2-card-link"
              textClassName="bodytext-6--no-margin"
            >
              Book an appointment
            </UnderlineButton>
          </div>
          <span className="contact-v2-card-arrow">›</span>
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

export default ContactV2;
