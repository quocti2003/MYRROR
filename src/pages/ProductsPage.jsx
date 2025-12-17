import { useEffect, useState } from "react";
import View360 from "@components/view360/View360";
import SelectOptionSection from "@components/selectOptionSection/SelectOptionSection";
import ParallaxScrolling from "@components/parallaxScrolling/ParallaxScrolling";
import ViewAllProduct from "@components/viewAllProduct/ViewAllProduct";
import ContactUs from "@components/contactUs/ContactUs";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import { useScrollToNextSection } from "@/hooks/useScrollToNextSection";
import { useBottomTheme } from "@/hooks/useBottomTheme";
import "@components/home-page/scrollEffect/ScrollEffect.css";
import "./products.css";

const ProductsPage = () => {
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

  useEffect(() => {
    // Check if we need to scroll to top
    if (sessionStorage.getItem("scrollToTop") === "true") {
      window.scrollTo({ top: 0, behavior: "instant" });
      sessionStorage.removeItem("scrollToTop");
    }
  }, []);

  return (
    <div className="products-page">
      {/* Section 1: View360 Component */}
      {/* <section className="products-section-1"><View360 /></section> */}

      {/* Section 2: SelectOptionSection */}
      <section className="products-section-2">
        <SelectOptionSection />
      </section>
      {/* Section 3 */}
      <ParallaxScrolling />

      {/* Section 4: View All Products */}
      <ViewAllProduct showViewProductButton={true} />

      {/* Section 5: Reach Out */}
      <ContactUs />

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

export default ProductsPage;
