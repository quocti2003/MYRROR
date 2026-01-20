import React from "react";
import ShineGlassButton from "@components/common/button/ShineGlassButton";

const ProductCareRepair = () => {
  return (
    <div className="product-care-repair">
      <div className="intro-section">
        <p className="bodytext-4--no-margin">
          Extend the life and brilliance of your jewelry with Mirror's
          professional care and repair services, tailored to keep each piece as
          radiant as the day you received it.
        </p>
      </div>

      <div className="service-section">
        <h3 className="heading-3--no-margin">
          1. Complimentary Lifetime Cleaning & Inspection
        </h3>
        <p className="bodytext-4--no-margin">
          To ensure your jewelry always looks its best, Mirror proudly offers:
        </p>

        <div className="sd-service-item">
          <ul>
            <li>
              <p className="bodytext-4--no-margin">
                <strong className="bodytext-4--no-margin">
                  Unlimited Lifetime Cleaning
                </strong>
              </p>
              <ul>
                <li>
                  <p className="bodytext-4--no-margin">
                    <strong className="bodytext-4--no-margin">
                      Shining Service:
                    </strong>{" "}
                    Restores the original luster and removes light surface
                    scratches through a complete process including diagnosis,
                    buffing, cleaning, technical and aesthetic checks.
                  </p>
                </li>
              </ul>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                <strong className="bodytext-4--no-margin">
                  Annual Jewelry Inspections
                </strong>
              </p>
              <ul>
                <li>
                  <p className="bodytext-4--no-margin">
                    Complimentary yearly check-ups to ensure gemstone security
                    and maintain brilliance.
                  </p>
                </li>
              </ul>
            </li>
          </ul>
        </div>

        <div className="note-section">
          <p className="note-text">
            <em className="bodytext-6--no-margin">
              Note: These services are complimentary and available in Vietnam
              only.
            </em>
          </p>
        </div>
      </div>

      <div className="service-section">
        <h3 className="heading-3--no-margin">
          2. Professional Repair Services
        </h3>
        <p className="bodytext-4--no-margin">
          If your piece requires attention beyond our standard warranty, Mirror
          offers expert repair services to restore its original condition.
        </p>

        <div className="sd-service-item">
          <ul>
            <li>
              <p className="bodytext-4--no-margin">
                <strong className="bodytext-4--no-margin">
                  Transparent Pricing:
                </strong>{" "}
                All repair costs are quoted after inspection, with no hidden
                fees.
              </p>
            </li>
            <li>
              <p className="bodytext-4--no-margin">
                <strong className="bodytext-4--no-margin">
                  Repair Services Include:
                </strong>
              </p>
              <ul>
                <li>
                  <p className="bodytext-4--no-margin">
                    <strong className="bodytext-4--no-margin">
                      Component Repair or Replacement:
                    </strong>{" "}
                    Replacement of damaged or missing elements (charged
                    separately from service fee).
                  </p>
                </li>
                <li>
                  <p className="bodytext-4--no-margin">
                    <strong className="bodytext-4--no-margin">
                      Reshaping:
                    </strong>{" "}
                    Restores your piece to its original form if distorted.
                  </p>
                </li>
                <li>
                  <p className="bodytext-4--no-margin">
                    <strong className="bodytext-4--no-margin">
                      Clasp or Link Repair:
                    </strong>{" "}
                    Essential to prevent loss from worn or damaged clasps or
                    links.
                  </p>
                </li>
                <li>
                  <p className="bodytext-4--no-margin">
                    <strong className="bodytext-4--no-margin">
                      Polishing:
                    </strong>{" "}
                    Gently restores shine and removes superficial abrasions
                    (performed per jeweler's recommendation to preserve shape).
                  </p>
                </li>
                <li>
                  <p className="bodytext-4--no-margin">
                    <strong className="bodytext-4--no-margin">
                      Stone or Pearl Replacement:
                    </strong>{" "}
                    Damaged stones or pearls can be replaced upon request.
                    Replacement stones/pearls are billed separately.
                  </p>
                </li>
              </ul>
            </li>
          </ul>
        </div>

        <div className="note-section">
          <p className="note-text">
            <em className="bodytext-6--no-margin">
              Note: Repair timelines may vary based on the complexity of the
              piece.
            </em>
          </p>
        </div>
      </div>

      <div className="service-section last-section">
        <h3 className="heading-3--no-margin">3. Jewelry Care Tips</h3>
        <p className="bodytext-4--no-margin">
          Maintain your jewelry's brilliance and longevity with these expert
          tips:
        </p>

        <ul>
          <li>
            <p className="bodytext-4--no-margin">
              Remove jewelry before swimming, showering, exercising, or applying
              lotions and perfumes.
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              Store each piece in its original Mirror box or a soft pouch to
              avoid scratches.
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              Avoid contact with harsh chemicals, ultrasonic cleaners, or
              abrasive surfaces.
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              Clean gently using warm water and a soft brush.
            </p>
          </li>
        </ul>
      </div>

      <div className="cta-section">
        <ShineGlassButton className="services-cta" theme="light">
          Book a Service
        </ShineGlassButton>
      </div>
    </div>
  );
};

export default ProductCareRepair;
