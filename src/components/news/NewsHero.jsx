import React from "react";
import "./NewsHero.css";

const NewsHero = () => {
  return (
    <section className="news-hero">
      <div className="news-hero-container">
        {/* <p className="news-hero-explore bodytext-4--no-margin">EXPLORE</p> */}
        <h1 className="news-hero-title heading-1--no-margin">NEWS</h1>
        <p className="news-hero-subtitle bodytext-4--no-margin">
          The latest designs, campaigns and news from MIRROR.
        </p>
      </div>
    </section>
  );
};

export default NewsHero;
