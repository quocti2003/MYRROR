import React from "react";
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/constants/routes";
import { useImmersiveModal } from "@/contexts/ImmersiveModalContext";
import "./Section6ProductCTA.css";

const Section6ProductCTA = ({ product }) => {
  const navigate = useNavigate();
  const { openModal } = useImmersiveModal();

  const handleContact = () => {
    navigate(ROUTES.CONTACT);
  };

  const handleBookAppointment = () => {
    navigate(ROUTES.CONTACT);
  };

  const handleImmersiveShowroom = () => {
    openModal();
  };

  return (
    <div className="section6-product-cta" data-navbar-theme="white">
      <div className="product-cta-container">
        {/* Main CTA */}
        <div className="cta-main">
          <h2 className="cta-title heading-1--no-margin">
            Interested in This Piece?
          </h2>
          <p className="cta-subtitle bodytext-3--no-margin">
            Connect with our experts to learn more, schedule a viewing, or create a custom piece inspired by this design.
          </p>

          <div className="cta-buttons">
            <button
              className="cta-button primary"
              onClick={handleBookAppointment}
            >
              <span className="button-icon">📅</span>
              <span className="button-text">Book an Appointment</span>
            </button>

            <button
              className="cta-button secondary"
              onClick={handleContact}
            >
              <span className="button-icon">💬</span>
              <span className="button-text">Contact Us</span>
            </button>
          </div>
        </div>

        {/* Secondary Actions */}
        <div className="cta-secondary">
          <div className="secondary-card">
            <div className="card-icon">🏛️</div>
            <h3 className="card-title bodytext-3--no-margin">
              Visit Our Showroom
            </h3>
            <p className="card-description bodytext-4--no-margin">
              Experience our collections in person at our flagship location
            </p>
            <button
              className="card-button"
              onClick={handleImmersiveShowroom}
            >
              Explore Showroom
            </button>
          </div>

          <div className="secondary-card">
            <div className="card-icon">✨</div>
            <h3 className="card-title bodytext-3--no-margin">
              Custom Design
            </h3>
            <p className="card-description bodytext-4--no-margin">
              Work with our designers to create a unique piece tailored to you
            </p>
            <button
              className="card-button"
              onClick={handleContact}
            >
              Start Custom Project
            </button>
          </div>

          <div className="secondary-card">
            <div className="card-icon">📞</div>
            <h3 className="card-title bodytext-3--no-margin">
              Speak to an Expert
            </h3>
            <p className="card-description bodytext-4--no-margin">
              Our specialists are available to answer your questions
            </p>
            <button
              className="card-button"
              onClick={handleContact}
            >
              Get in Touch
            </button>
          </div>
        </div>

        {/* Product Reference */}
        {product && (
          <div className="cta-reference">
            <p className="reference-text bodytext-4--no-margin">
              Reference: <strong>{product.skuCode}</strong> - {product.name}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Section6ProductCTA;
