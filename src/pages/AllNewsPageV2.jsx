import React, { useState, useEffect } from "react";
import "@styles/grid-system.css";
import "./AllNewsPageV2.css";
import NewsHero from "@components/news/NewsHero";
import NewsItemV2 from "@components/news/NewsItemV2";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import { useScrollToNextSection } from "@/hooks/useScrollToNextSection";
import { useBottomTheme } from "@/hooks/useBottomTheme";
import "@components/home-page/scrollEffect/ScrollEffect.css";

const AllNewsPageV2 = () => {
  const [visibleItems, setVisibleItems] = useState(999); // Show all items
  const { isArrowVisible, handleArrowClick } = useScrollToNextSection({
    footerSelector: '.footer',
  });
  const { theme: arrowTheme } = useBottomTheme();
  const [isImmersiveCollapsed, setIsImmersiveCollapsed] = useState(false);
  const [showScrollTop, setShowScrollTop] = useState(false);

  // Detect scroll to collapse immersive button and show scroll-to-top
  useEffect(() => {
    const handleScroll = () => {
      const scrollY = window.scrollY;
      setIsImmersiveCollapsed(scrollY > 100);
      setShowScrollTop(scrollY > 500);
    };
    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Sample news data - replace with actual API data
  const newsData = [
    {
      id: 1,
      title: "Digital Jewelry Week Milan 2025",
      image: "/news/mirror-milan1.webp",
      gallery: [
        "/news/mirror-milan2.webp",
        "/news/mirror-milan3.webp",
        "/news/mirror-milan1.webp",
      ],
      date: "10/2025",
      description:
        "At the heart of Milan's Digital Jewelry Week 2025, MIRROR unveiled a glimpse into the future of luxury - where technology and emotion merge through light, sound, and reflection.",
    },
    {
      id: 2,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      image: "/news/mirror-milan2.webp",
      gallery: [
        "/news/mirror-milan2.webp",
        "/news/mirror-milan3.webp",
        "/news/mirror-milan1.webp",
      ],
      date: "10/2025",
      description:
        "Exploring the intersection of business and social responsibility in the modern entrepreneurial landscape.",
    },
    {
      id: 3,
      title: "Sustainable Luxury: The Future of Fine Jewelry",
      image: "/news/mirror-milan3.webp",
      gallery: [
        "/news/mirror-milan2.webp",
        "/news/mirror-milan3.webp",
        "/news/mirror-milan1.webp",
      ],
      date: "09/2025",
      description:
        "How the luxury jewelry industry is embracing sustainability and ethical practices for a better tomorrow.",
    },
    {
      id: 4,
      title: "Innovation in Diamond Cutting Technology",
      image: "/news/mirror-milan1.webp",
      gallery: [
        "/news/mirror-milan2.webp",
        "/news/mirror-milan3.webp",
        "/news/mirror-milan1.webp",
      ],
      date: "09/2025",
      description:
        "Revolutionary techniques in diamond cutting that are transforming the industry and creating unprecedented brilliance.",
    },
    {
      id: 5,
      title: "The Art of Craftsmanship Meets Modern Design",
      image: "/news/mirror-milan2.webp",
      gallery: [
        "/news/mirror-milan2.webp",
        "/news/mirror-milan3.webp",
        "/news/mirror-milan1.webp",
      ],
      date: "08/2025",
      description:
        "A journey through the evolution of jewelry design where traditional craftsmanship harmonizes with contemporary aesthetics.",
    },
    {
      id: 6,
      title: "Global Trends in Luxury Market 2025",
      image: "/news/mirror-milan3.webp",
      gallery: [
        "/news/mirror-milan2.webp",
        "/news/mirror-milan3.webp",
        "/news/mirror-milan1.webp",
      ],
      date: "08/2025",
      description:
        "An in-depth analysis of emerging trends shaping the global luxury market and consumer behavior.",
    },
  ];

  const handleViewMore = () => {
    setVisibleItems((prev) => Math.min(prev + 3, newsData.length));
  };

  return (
    <div className="all-news-page-v2" data-navbar-theme="blend">
      <NewsHero />
      <section className="news-list-v2">
        <div className="news-list-v2-container">
          {newsData.slice(0, visibleItems).map((item) => (
            <NewsItemV2 key={item.id} item={item} />
          ))}
        </div>
      </section>

      {/* Fixed Immersive Button */}
      <div className="fixed-immersive-container">
        <GlassThemeButton
          theme={arrowTheme === "white" ? "dark" : "light"}
          icon="globe"
          isCollapsed={isImmersiveCollapsed}
        >
          Immersive Showroom
        </GlassThemeButton>
      </div>

      {/* Fixed Arrow Button */}
      {isArrowVisible && (
        <div className="fixed-arrow-container">
          <GlassThemeButton
            theme={arrowTheme === "white" ? "dark" : "light"}
            icon="arrow"
            onClick={handleArrowClick}
          />
        </div>
      )}

      {/* Fixed Scroll to Top Button - only show when scroll-down arrow is hidden */}
      <div className={`fixed-scroll-top-container ${showScrollTop ? 'visible' : ''}`}>
        <GlassThemeButton
          theme={arrowTheme === "white" ? "dark" : "light"}
          icon="arrow-up"
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
        />
      </div>
    </div>
  );
};

export default AllNewsPageV2;
