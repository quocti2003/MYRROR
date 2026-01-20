import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { MediaImage } from "@components/common/media";
import { getProductDetailRoute } from "@/constants/routes";
import Ellipse245 from "@assets/images/icons/Ellipse 245.png";
import Ellipse246 from "@assets/images/icons/Ellipse 246.png";
import Ellipse247 from "@assets/images/icons/Ellipse 247.png";
import "./ProductCarouselItem.css";

const ProductCarouselItem = ({ images = [], label, className = "", productId = null }) => {
  const navigate = useNavigate();
  const [currentIndex, setCurrentIndex] = useState(0);
  const [prevIndex, setPrevIndex] = useState(null);
  const [isHovered, setIsHovered] = useState(false);
  const [slideDirection, setSlideDirection] = useState(null); // "next" or "prev" or null
  const [isAnimating, setIsAnimating] = useState(false);

  // Ensure we have at least one image
  const imageList = images.length > 0 ? images : ["collections/collectionDetail/collectionDetail_more.webp"];
  const totalImages = imageList.length;

  const handleNext = (e) => {
    e.stopPropagation();
    if (isAnimating) return;

    setPrevIndex(currentIndex);
    setSlideDirection("next");
    setIsAnimating(true);
    setCurrentIndex((prev) => (prev + 1) % totalImages);

    setTimeout(() => {
      setIsAnimating(false);
      setPrevIndex(null);
    }, 300);
  };

  const handlePrev = (e) => {
    e.stopPropagation();
    if (isAnimating) return;

    setPrevIndex(currentIndex);
    setSlideDirection("prev");
    setIsAnimating(true);
    setCurrentIndex((prev) => (prev - 1 + totalImages) % totalImages);

    setTimeout(() => {
      setIsAnimating(false);
      setPrevIndex(null);
    }, 300);
  };

  const handleMouseLeave = () => {
    setIsHovered(false);
    setCurrentIndex(0);
    setPrevIndex(null);
    setSlideDirection(null);
    setIsAnimating(false);
  };

  const handleProductClick = () => {
    if (productId) {
      navigate(getProductDetailRoute(productId));
    }
  };

  const getSlideClass = (index) => {
    if (index === currentIndex) {
      if (isAnimating && slideDirection) {
        return `active slide-in-${slideDirection}`;
      }
      return "active";
    }
    if (index === prevIndex && isAnimating && slideDirection) {
      return `slide-out-${slideDirection}`;
    }
    return "";
  };

  return (
    <div
      className={`product-carousel-item ${className} ${productId ? "clickable" : ""}`}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={handleMouseLeave}
      onClick={handleProductClick}
      role={productId ? "button" : undefined}
      tabIndex={productId ? 0 : undefined}
    >
      <div className="product-carousel-images">
        {imageList.map((src, index) => (
          <div
            key={index}
            className={`product-carousel-slide ${getSlideClass(index)}`}
          >
            <MediaImage src={src} alt={`${label || "Product"} ${index + 1}`} />
          </div>
        ))}
      </div>

      {label && (
        <span className="product-carousel-label heading-3--no-margin">
          {label}
        </span>
      )}

      {/* Progress bar - only show on hover and if multiple images */}
      {totalImages > 1 && (
        <div className={`product-carousel-progress ${isHovered ? "visible" : ""}`}>
          {imageList.map((_, index) => (
            <div
              key={index}
              className={`product-carousel-progress-segment ${index === currentIndex ? "active" : ""}`}
              style={{ width: `${100 / totalImages}%` }}
            />
          ))}
        </div>
      )}

      {/* Arrow buttons - only show on hover and if multiple images */}
      {totalImages > 1 && (
        <>
          <button
            className={`product-carousel-arrow product-carousel-arrow-back ${isHovered ? "visible" : ""}`}
            onClick={handlePrev}
            aria-label="Previous image"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M13 16L7 10L13 4.00001" stroke="black" strokeOpacity="0.5" strokeWidth="1.5" strokeLinecap="square" strokeLinejoin="round"/>
            </svg>
          </button>
          <button
            className={`product-carousel-arrow product-carousel-arrow-next ${isHovered ? "visible" : ""}`}
            onClick={handleNext}
            aria-label="Next image"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M7 16L13 10L7 4.00001" stroke="black" strokeOpacity="0.5" strokeWidth="1.5" strokeLinecap="square" strokeLinejoin="round"/>
            </svg>
          </button>
        </>
      )}

      {/* Thumbnail circles - bottom right corner */}
      {totalImages > 1 && (
        <div className={`product-carousel-thumbnails ${isHovered ? "visible" : ""}`}>
          <img src={Ellipse245} alt="" className="product-carousel-thumbnail" />
          <img src={Ellipse246} alt="" className="product-carousel-thumbnail" />
          <img src={Ellipse247} alt="" className="product-carousel-thumbnail" />
        </div>
      )}
    </div>
  );
};

export default ProductCarouselItem;
