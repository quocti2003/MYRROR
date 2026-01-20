import React, { useState, useMemo } from "react";
import "./Section1ProductHero.css";

const Section1ProductHero = ({ product, onOrderNow }) => {
  // Get all valid images, filtering out example.com placeholders
  const getValidImages = () => {
    const allImages = [
      ...(product?.imageUrls || []),
      ...(product?.imageUrl && !product.imageUrls?.includes(product.imageUrl) ? [product.imageUrl] : [])
    ];
    return allImages.filter(url => url && !url.includes('example.com'));
  };

  const validImages = getValidImages();
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [is3DViewActive, setIs3DViewActive] = useState(false);
  const selectedImage = validImages[selectedImageIndex] || null;

  // Check if product has 3D model
  const hasModel3d = product?.model3dId && product.model3dId.trim().length > 0;

  // Build iJewel 3D viewer iframe URL
  const iframeUrl = useMemo(() => {
    if (!hasModel3d) return null;
    const baseUrl = 'https://drive.ijewel3d.com/drive/files';
    const params = new URLSearchParams({
      slug: product.model3dId,
      isAutoplay: 'true',
      isRemoveHologram: 'true',
      isDisplayThumbnail: 'false',
      isRemoveBackground: 'true',
      backgroundType: 'transparent',
      isLoadHighQuality: 'true',
      isLoadMediumQuality: 'true',
      isDisplayLogo: 'false',
      isDisplayShadow: 'true',
      isDisplayFloatingBtn: 'false',
      isDisplayAnnotation: 'false',
    });
    return `${baseUrl}/${product.model3dId}/embedded?${params.toString()}`;
  }, [hasModel3d, product?.model3dId]);

  // Handle thumbnail click - deactivate 3D view when selecting an image
  const handleImageSelect = (index) => {
    setSelectedImageIndex(index);
    setIs3DViewActive(false);
  };

  // Handle 3D view thumbnail click
  const handle3DViewSelect = () => {
    setIs3DViewActive(true);
  };

  // Format price
  const formatPrice = (price) => {
    if (!price) return "Contact for price";
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(price);
  };

  return (
    <div className="section1-product-hero" data-navbar-theme="black">
      <div className="product-hero-container">
        {/* Left: Product Image Gallery */}
        <div className="product-hero-gallery">
          {/* Main Image or 3D View */}
          <div className="gallery-main-image-wrapper">
            {is3DViewActive && iframeUrl ? (
              <div className="gallery-3d-viewer">
                <iframe
                  src={iframeUrl}
                  title="3D Product View"
                  frameBorder="0"
                  allowFullScreen
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                />
              </div>
            ) : selectedImage ? (
              <img
                src={selectedImage}
                alt={product?.name || "Product"}
                className="gallery-main-image"
              />
            ) : (
              <div className="gallery-image-placeholder">
                <span>No Image Available</span>
              </div>
            )}
          </div>

          {/* Thumbnail Gallery */}
          {(validImages.length > 1 || hasModel3d) && (
            <div className="gallery-thumbnails">
              {validImages.map((image, index) => (
                <div
                  key={index}
                  className={`gallery-thumbnail ${index === selectedImageIndex && !is3DViewActive ? 'active' : ''}`}
                  onClick={() => handleImageSelect(index)}
                >
                  <img src={image} alt={`${product?.name} - ${index + 1}`} />
                </div>
              ))}
              {/* 3D View Thumbnail */}
              {hasModel3d && (
                <div
                  className={`gallery-thumbnail gallery-thumbnail-3d ${is3DViewActive ? 'active' : ''}`}
                  onClick={handle3DViewSelect}
                >
                  <div className="thumbnail-3d-content">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M12 2L2 7l10 5 10-5-10-5z" />
                      <path d="M2 17l10 5 10-5" />
                      <path d="M2 12l10 5 10-5" />
                    </svg>
                    <span>3D</span>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right: Product Information */}
        <div className="product-hero-info">
          <div className="product-category bodytext-4--no-margin">
            {product?.categoryName || "Jewelry"}
          </div>

          <h1 className="product-name heading-1--no-margin">
            {product?.name || "Product Name"}
          </h1>

          <div className="product-sku bodytext-4--no-margin">
            SKU: {product?.skuCode || "N/A"}
          </div>

          <div className="product-price-section">
            <div className="product-price heading-2--no-margin">
              {formatPrice(product?.salePrice)}
            </div>
            {product?.basePrice && product?.salePrice && product.salePrice < product.basePrice && (
              <div className="product-base-price bodytext-4--no-margin">
                Was: {formatPrice(product.basePrice)}
              </div>
            )}
          </div>

          {/* Quick Info */}
          <div className="product-quick-info">
            {product?.goldWeight && (
              <div className="quick-info-item">
                <span className="info-label">Gold Weight</span>
                <span className="info-value">{product.goldWeight}g</span>
              </div>
            )}
            {product?.diamondWeight && (
              <div className="quick-info-item">
                <span className="info-label">Diamond Weight</span>
                <span className="info-value">{product.diamondWeight} ct</span>
              </div>
            )}
            {product?.gemstoneType && (
              <div className="quick-info-item">
                <span className="info-label">Gemstone</span>
                <span className="info-value">{product.gemstoneType}</span>
              </div>
            )}
          </div>

          {/* Availability Status */}
          <div className="product-status">
            <span className={`status-badge ${product?.status?.toLowerCase()}`}>
              {product?.status || "Available"}
            </span>
          </div>

          {/* Action Buttons */}
          <div className="product-actions">
            <div className="action-buttons-row">
              <button
                className={`wishlist-button ${isWishlisted ? 'active' : ''}`}
                onClick={() => setIsWishlisted(!isWishlisted)}
                aria-label="Add to wishlist"
              >
                <span className="wishlist-icon">{isWishlisted ? '❤️' : '🤍'}</span>
              </button>
              <button className="book-appointment-button">
                Book An Appointment
              </button>
            </div>
            <button className="order-now-button" onClick={onOrderNow}>
              Order Now
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Section1ProductHero;
