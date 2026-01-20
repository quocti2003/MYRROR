import React from "react";
import "./SizingGuide.css";

const SizingGuide = () => {
  // Ring size conversion data
  const ringSizeData = [
    { circumference: "1.74", us: "3" },
    { circumference: "1.79", us: "3.5" },
    { circumference: "1.84", us: "4" },
    { circumference: "1.89", us: "4.5" },
    { circumference: "1.94", us: "5" },
    { circumference: "1.99", us: "5.5" },
    { circumference: "2.04", us: "6" },
    { circumference: "2.09", us: "6.5" },
    { circumference: "2.14", us: "7" },
    { circumference: "2.19", us: "7.5" },
    { circumference: "2.24", us: "8" },
    { circumference: "2.29", us: "8.5" },
    { circumference: "2.34", us: "9" },
    { circumference: "2.39", us: "9.5" },
    { circumference: "2.44", us: "10" },
  ];

  return (
    <div className="sizing-guide">
      <section className="section ring-size-section">
        <h3 className="heading-3--no-margin">1. Ring size</h3>
        <p className="bodytext-4--no-margin">
          You can easily measure your ring size at home using just a string,
          thread, or ribbon and a ruler. Here's how:
        </p>

        <ol className="step-list">
          <li>
            <p className="bodytext-4--no-margin">
              Gently wrap the string around the base of your finger, close to
              your knuckle (where a ring would sit comfortably).
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              Mark the exact point where the string overlaps.
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              Lay the string flat next to a ruler and measure the length in
              millimetres (mm). This number is your finger's circumference.
            </p>
          </li>
        </ol>

        <div className="tip-section">
          <p className="tip-text bodytext-6--no-margin">
            Tip: For the most accurate results, measure your finger at the end
            of the day when it's at its largest, and avoid measuring when your
            hands are cold.
          </p>
        </div>

        <div className="size-chart-table">
          <h4 className="chart-title bodytext-2--no-margin">Ring Size Conversion Chart</h4>
          <table className="ring-size-table">
            <thead>
              <tr>
                <th className="bodytext-4--no-margin">Inside Circumference (in)</th>
                <th className="bodytext-4--no-margin">United States</th>
              </tr>
            </thead>
            <tbody>
              {ringSizeData.map((row, index) => (
                <tr key={index}>
                  <td className="bodytext-4--no-margin">{row.circumference}</td>
                  <td className="bodytext-4--no-margin">{row.us}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="section necklace-section">
        <h3 className="heading-3--no-margin">2. Necklace</h3>
        <p className="bodytext-4--no-margin">
          Choose the perfect necklace length for your style:
        </p>
        <ul>
          <li>
            <p className="bodytext-4--no-margin">
              14-16 inches: Choker length, sits close to the neck
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              18 inches: Princess length, sits at the collarbone
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              20-24 inches: Matinee length, sits between collarbone and bust
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              28-36 inches: Opera length, sits at the bust or below
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              Over 36 inches: Rope length, can be doubled or tripled
            </p>
          </li>
        </ul>
      </section>

      <section className="section bracelet-section">
        <h3 className="heading-3--no-margin">3. Bracelet</h3>
        <p className="bodytext-4--no-margin">To measure your bracelet size:</p>
        <ul>
          <li>
            <p className="bodytext-4--no-margin">
              Measure your wrist with a flexible tape measure
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              Add 0.5-1 inch for a comfortable fit
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              Standard sizes range from 6.5 to 8 inches
            </p>
          </li>
          <li>
            <p className="bodytext-4--no-margin">
              Consider the bracelet style - bangles need more room
            </p>
          </li>
        </ul>
      </section>
    </div>
  );
};

export default SizingGuide;
