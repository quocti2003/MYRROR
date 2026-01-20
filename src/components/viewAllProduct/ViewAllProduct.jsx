import { useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import MediaImage from "@components/common/media/MediaImage";
import "./ViewAllProduct.css";
import { ROUTES } from "@/constants/routes";

const ViewAllProduct = ({ showViewProductButton = false }) => {
  const navigate = useNavigate();
  const sectionRef = useRef(null);
  const containerRef = useRef(null);
  const scrollContainerRef = useRef(null);
  const wrapperRef = useRef(null);

  const products = [
    { id: 1, name: "Lumina", image: "products/product_3_fix.webp" },
    { id: 2, name: "Lumina", image: "products/product_3_fix.webp" },
    { id: 3, name: "Lumina", image: "products/product_3_fix.webp" },
    { id: 4, name: "Lumina", image: "products/product_3_fix.webp" },
    { id: 5, name: "Lumina", image: "products/product_3_fix.webp" },
    { id: 6, name: "Lumina", image: "products/product_3_fix.webp" },
    { id: 7, name: "Lumina", image: "products/product_3_fix.webp" },
    { id: 8, name: "Lumina", image: "products/product_3_fix.webp" },
  ];

  useEffect(() => {
    const wrapper = wrapperRef.current;
    if (!wrapper) return;

    let isDragging = false;
    let startX = 0;
    let scrollLeft = 0;
    let targetScrollLeft = wrapper.scrollLeft;
    let currentScrollLeft = wrapper.scrollLeft;
    let animationId = null;
    let isHoveringLocal = false;

    // Smooth scroll animation
    const smoothScroll = () => {
      const diff = targetScrollLeft - currentScrollLeft;
      const delta = diff * 0.15; // Easing factor (lower = smoother but slower)

      if (Math.abs(diff) > 0.5) {
        currentScrollLeft += delta;
        wrapper.scrollLeft = currentScrollLeft;
        animationId = requestAnimationFrame(smoothScroll);
      } else {
        currentScrollLeft = targetScrollLeft;
        wrapper.scrollLeft = targetScrollLeft;
        animationId = null;
      }
    };

    // Handle wheel event to scroll horizontally when hovering
    const handleWheel = (e) => {
      if (isHoveringLocal) {
        // Always prevent default to avoid simultaneous horizontal and vertical scroll
        e.preventDefault();

        // Calculate max scroll position
        const maxScroll = wrapper.scrollWidth - wrapper.clientWidth;
        const currentScroll = wrapper.scrollLeft;

        // Determine scroll direction with threshold to avoid diagonal scrolling
        const absDeltaX = Math.abs(e.deltaX);
        const absDeltaY = Math.abs(e.deltaY);
        const threshold = 10; // Threshold to determine primary scroll direction

        // Determine if this is primarily a horizontal or vertical scroll
        const isHorizontalScroll = absDeltaX > absDeltaY + threshold;
        const isVerticalScroll = absDeltaY > absDeltaX + threshold;

        // If it's clearly vertical scroll, check boundaries
        if (isVerticalScroll || (!isHorizontalScroll && e.deltaY !== 0)) {
          // This is vertical scroll (or ambiguous but has deltaY)
          const isScrollingUp = e.deltaY < 0;
          const isScrollingDown = e.deltaY > 0;
          const isAtStart = currentScroll <= 1;
          const isAtEnd = currentScroll >= maxScroll - 1;

          // At boundaries, allow page scroll by manually triggering it
          if ((isAtStart && isScrollingUp) || (isAtEnd && isScrollingDown)) {
            // Manually scroll the page
            window.scrollBy(0, e.deltaY);
            return;
          }

          // In the middle, convert vertical to horizontal
          const scrollDelta = e.deltaY * 1.5;
          targetScrollLeft += scrollDelta;
          targetScrollLeft = Math.max(0, Math.min(targetScrollLeft, maxScroll));

          if (!animationId) {
            currentScrollLeft = wrapper.scrollLeft;
            animationId = requestAnimationFrame(smoothScroll);
          }
          return;
        }

        // For horizontal scroll (or when deltaX is dominant)
        if (isHorizontalScroll) {
          const scrollDelta = e.deltaX;
          targetScrollLeft += scrollDelta;
          targetScrollLeft = Math.max(0, Math.min(targetScrollLeft, maxScroll));

          if (!animationId) {
            currentScrollLeft = wrapper.scrollLeft;
            animationId = requestAnimationFrame(smoothScroll);
          }
          return;
        }

        // If we reach here, it's an ambiguous or very small movement
        // Do nothing to avoid unintended scrolling
      }
    };

    // Drag to scroll functionality
    const handleMouseDown = (e) => {
      // Cancel smooth scroll animation when dragging
      if (animationId) {
        cancelAnimationFrame(animationId);
        animationId = null;
      }

      isDragging = true;
      wrapper.classList.add("dragging");
      wrapper.style.cursor = "grabbing";
      startX = e.pageX - wrapper.offsetLeft;
      scrollLeft = wrapper.scrollLeft;
    };

    const handleMouseMove = (e) => {
      if (!isDragging) return;
      e.preventDefault();
      const x = e.pageX - wrapper.offsetLeft;
      const walk = (x - startX) * 2; // Multiply by 2 for faster scroll
      const newScrollLeft = scrollLeft - walk;
      wrapper.scrollLeft = newScrollLeft;
      // Update tracking variables
      currentScrollLeft = newScrollLeft;
      targetScrollLeft = newScrollLeft;
    };

    const handleMouseUp = () => {
      isDragging = false;
      wrapper.classList.remove("dragging");
      wrapper.style.cursor = "grab";
    };

    const handleMouseLeave = () => {
      isHoveringLocal = false;
      if (isDragging) {
        isDragging = false;
        wrapper.classList.remove("dragging");
        wrapper.style.cursor = "grab";
      }
    };

    const handleMouseEnter = () => {
      isHoveringLocal = true;
    };

    // Add event listeners
    wrapper.addEventListener("wheel", handleWheel, { passive: false });
    wrapper.addEventListener("mousedown", handleMouseDown);
    wrapper.addEventListener("mousemove", handleMouseMove);
    wrapper.addEventListener("mouseup", handleMouseUp);
    wrapper.addEventListener("mouseleave", handleMouseLeave);
    wrapper.addEventListener("mouseenter", handleMouseEnter);

    return () => {
      // Cancel animation frame on cleanup
      if (animationId) {
        cancelAnimationFrame(animationId);
      }
      wrapper.removeEventListener("wheel", handleWheel);
      wrapper.removeEventListener("mousedown", handleMouseDown);
      wrapper.removeEventListener("mousemove", handleMouseMove);
      wrapper.removeEventListener("mouseup", handleMouseUp);
      wrapper.removeEventListener("mouseleave", handleMouseLeave);
      wrapper.removeEventListener("mouseenter", handleMouseEnter);
    };
  }, []);

  const handleViewAllProducts = async () => {
    await optimizedTransitionUtils.transitionToRoute(navigate, ROUTES.ALL_GEMS);
  };

  return (
    <>
      <section className="collection-section-4-gsap" ref={sectionRef}>
        <div className="same-collection-container" ref={containerRef}>
          <div className="same-collection-header">
            <h2 className="heading-1 same-collection-title">MORE GEMS</h2>
            <p className="bodytext-4 same-collection-description">
              Mirror’s curation of visionary designs - where each piece embodies the essence of future luxury.
            </p>
          </div>

          <div
            className="horizontal-scroll-wrapper"
            ref={wrapperRef}
          >
            <div className="same-collection-grid-gsap" ref={scrollContainerRef}>
              {products.map((product) => (
                <div key={product.id} className="product-card">
                  <MediaImage
                    src={product.image}
                    alt={`${product.name} Ring`}
                    className="product-image"
                    draggable={false}
                  />
                  <span className="product-label heading-3--no-margin">
                    {product.name}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {showViewProductButton && (
        <div className="view-product-button-container">
          <ShineGlassButton theme="light" onClick={handleViewAllProducts}>
            View all products
          </ShineGlassButton>
        </div>
      )}
    </>
  );
};

export default ViewAllProduct;
