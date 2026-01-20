import React from "react";
import "./Section1.css";

const Section1 = () => {
  return (
    <section className="services-section1">
      <div className="section1-background-overlay"></div>
      <div className="section1-container">
        <div className="section1-content">
          <h1 className="section1-title heading-1--no-margin">SERVICES</h1>
          <p className="section1-description bodytext-4--no-margin">
            To honour the personal story behind every Mirror creation, we are committed to accompanying each piece through time.
          </p>
        </div>
      </div>
    </section>
  );
};

export default Section1;
