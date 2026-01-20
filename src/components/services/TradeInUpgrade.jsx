import React from "react";
import ShineGlassButton from "@components/common/button/ShineGlassButton";

const TradeInUpgrade = () => {
  return (
    <div className="trade-in-upgrade">
      <div className="intro-section">
        <h3 className="heading-3--no-margin">Trade-In & Upgrade Programs</h3>
        <p className="bodytext-4--no-margin">
          Mirror's "Reclaim & Renew" program allows you to trade in eligible
          pieces in the future toward upgraded designs - a promise of continuous
          evolution in your jewelry journey.
        </p>
        <p className="availability-note">
          <em className="bodytext-6--no-margin">
            Details available in showroom or by inquiry.
          </em>
        </p>
      </div>

      <div className="how-it-works-section">
        <h3 className="heading-3--no-margin">How it works</h3>

        <div className="process-item">
          <ul>
            <li>
              <p className="bodytext-4--no-margin">
                <strong className="bodytext-4--no-margin">
                  Trade-In Value:
                </strong>
              </p>
              <ul>
                <li>
                  <p className="bodytext-4--no-margin">
                    When you're ready for a new design, bring in your eligible
                    Mirror jewelry. We will assess its condition and provide a
                    trade-in value, which can be applied as credit toward your
                    next purchase.
                  </p>
                </li>
              </ul>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                <strong className="bodytext-4--no-margin">
                  Eligibility Criteria
                </strong>
              </p>
              <ul>
                <li>
                  <p className="bodytext-4--no-margin">
                    Must be a piece originally purchased from Mirror
                  </p>
                </li>
                <li>
                  <p className="bodytext-4--no-margin">
                    Jewelry must pass a quality and authenticity inspection by
                    our specialists
                  </p>
                </li>
                <li>
                  <p className="bodytext-4--no-margin">
                    Trade-in is only applicable toward the purchase of a new
                    piece of greater or equal value
                  </p>
                </li>
              </ul>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                <strong className="bodytext-4--no-margin">
                  Upgrade Opportunities
                </strong>
              </p>
              <ul>
                <li>
                  <p className="bodytext-4--no-margin">
                    Choose from our latest collections or create a custom piece.
                    The program allows you to evolve your style while retaining
                    the value of your original piece.
                  </p>
                </li>
              </ul>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                <strong className="bodytext-4--no-margin">
                  Sustainable Luxury
                </strong>
              </p>
              <ul>
                <li>
                  <p className="bodytext-4--no-margin">
                    Reclaim & Renew is also a commitment to sustainability -
                    giving your existing jewelry a second life and reducing
                    waste while enhancing your collection.
                  </p>
                </li>
              </ul>
            </li>
          </ul>
        </div>

        <div className="note-section">
          <p className="note-text">
            <em className="bodytext-6--no-margin">
              Full program details, eligibility assessments, and upgrade
              consultations are available at our showroom or by direct inquiry.
              Appointments are recommended.
            </em>
          </p>
        </div>
      </div>

      <div className="cta-section">
        <ShineGlassButton className="services-cta" theme="light">
          Book an Appointment
        </ShineGlassButton>
      </div>
    </div>
  );
};

export default TradeInUpgrade;
