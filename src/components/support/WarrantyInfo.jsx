import React from "react";
import "./WarrantyInfo.css";

const WarrantyInfo = () => {
  return (
    <div className="warranty-info">
      <div className="header-section">
        <h3 className="heading-3--no-margin">
          Created by Science. Crafted for Eternity.
        </h3>
        <p className="bodytext-4--no-margin">
          Each Mirror piece is meticulously crafted with rare precision,
          intention, and limited availability. To preserve the integrity and
          exclusivity of our jewelry, we do not accept returns under any
          circumstances. However, exchanges may be accommodated under specific
          conditions as outlined below.
        </p>
      </div>

      <section className="section limited-warranty-section">
        <h3 className="heading-3--no-margin">
          1. Limited Warranty - 12 Months
        </h3>
        <div className="warranty-overview">
          <p className="bodytext-4--no-margin">
            All jewelry purchased from Mirror Future Diamond is covered by a{" "}
            <span className="bodytext-4 warranty-highlight">12-month limited</span> warranty, which
            includes:
          </p>

          <div className="warranty-covered">
            <ul>
              <li>
                <p className="bodytext-4--no-margin">
                  Manufacturing defects in materials or craftsmanship
                </p>
              </li>
              <li>
                <p className="bodytext-4--no-margin">
                  Structural integrity of the setting
                </p>
              </li>
              <li>
                <p className="bodytext-4--no-margin">
                  Clasp or chain malfunction (necklaces or bracelets)
                </p>
              </li>
              <li>
                <p className="bodytext-4--no-margin">
                  Stone loosening due to workmanship issues (not customer
                  damage)
                </p>
              </li>
            </ul>
          </div>
        </div>

        <div className="warranty-exclusions">
          <h4>
            <span className="bodytext-4">What's Not Covered:</span>
          </h4>
          <p className="bodytext-4--no-margin">
            This warranty does not apply to:
          </p>
          <ul>
            <li>
              <p className="bodytext-4--no-margin">
                Damage caused by improper use, impact, or accidents
              </p>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                Normal wear-and-tear, including surface scratches or abrasions
              </p>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                Discoloration from exposure to chemicals, cosmetics, or
                environmental conditions
              </p>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                Theft, loss, or misplacement
              </p>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                Any alteration, resizing, or repair performed by a third party
                not authorized by Mirror
              </p>
            </li>
          </ul>
        </div>

        <div className="warranty-note">
          <p className="warranty-note-text bodytext-6--no-margin">
            Note: To maintain warranty coverage, all inspections, repairs, and
            modifications must be performed exclusively by Mirror or its
            authorized partners.
          </p>
        </div>
      </section>

      <section className="section warranty-activation-section">
        <h3 className="heading-3--no-margin">
          2. Warranty Activation & Conditions
        </h3>
        <div className="activation-details">
          <ul>
            <li>
              <p className="bodytext-4--no-margin">
                Warranty is automatically registered upon purchase through
                Mirror's internal system.
              </p>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                For online purchases, please retain your digital receipt and
                certification documents.
              </p>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                Proof of purchase is required for all warranty or after-care
                claims.
              </p>
            </li>
          </ul>
        </div>
      </section>
    </div>
  );
};

export default WarrantyInfo;
