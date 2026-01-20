import React, { useEffect, useState } from "react";
import Collection from "@components/collections/Collections";
import ViewAllProduct from "@components/viewAllProduct/ViewAllProduct";
import ContactUs from "@components/contactUs/ContactUs";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import { useScrollToNextSection } from "@/hooks/useScrollToNextSection";
import { useBottomTheme } from "@/hooks/useBottomTheme";
import "@components/home-page/scrollEffect/ScrollEffect.css"; // Import CSS for arrow styling

const CollectionPage = () => {
  const { isArrowVisible, handleArrowClick } = useScrollToNextSection({
    footerSelector: '[data-section="contact-us"], .footer', // Check both ContactUs section and global footer
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

  useEffect(() => {
    // Scroll to top on mount
    window.scrollTo(0, 0);
  }, []);

  return (
    <div className="collection-page-wrapper">
      {/* Section 1-3: Collection component with internal sections */}
      <div data-section="collection">
        <Collection />
      </div>

      {/* Section 4: View All Products */}
      <div data-section="view-all-product" data-navbar-theme="black">
        <ViewAllProduct showViewProductButton={true} />
      </div>

      {/* Section 5: Contact Us */}
      <div data-section="contact-us">
        <ContactUs />
      </div>

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

export default CollectionPage;
