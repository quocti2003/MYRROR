import React, { useEffect, useRef, useState } from "react";
import { productsAPI, handleAPIError } from "@services/api";
import { MediaImage } from "@components/common/media";
import ProductCarouselItem from "@components/collections/ProductCarouselItem";
import "./AllGems.css";

const AllGems = () => {
  const largeItem1Ref = useRef(null);
  const largeItem2Ref = useRef(null);

  // Products state
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Layout option: 1 = 2 columns, 2 = 4 columns (current layout)
  const [layoutOption, setLayoutOption] = useState(2);

  // Fetch published products from API
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        const response = await productsAPI.getPublished();
        setProducts(response.data || []);
      } catch (err) {
        const errorInfo = handleAPIError(err, "Failed to load products");
        setError(errorInfo.message);
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  // Parallax effect for large items
  useEffect(() => {
    const handleScroll = () => {
      const windowHeight = window.innerHeight;

      [largeItem1Ref, largeItem2Ref].forEach((ref) => {
        if (!ref.current) return;

        const rect = ref.current.getBoundingClientRect();
        const scrollDistance = windowHeight - rect.top;
        const totalDistance = windowHeight + rect.height;
        const scrollRatio = Math.max(0, Math.min(1, scrollDistance / totalDistance));

        // Image moves DOWN slowly (0% → 30%)
        const imgParallax = scrollRatio * 30;

        ref.current.style.setProperty("--parallax-img-offset", `${imgParallax}%`);
      });
    };

    window.addEventListener("scroll", handleScroll);
    handleScroll();

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Static fallback data (used when API fails or returns empty)
  const staticGemImages = [
    "products/allGems/Product_card_2.webp",
    "products/allGems/model_4.webp",
    "products/allGems/toietmoi-product.webp",
  ];
  const staticFallbackProducts = Array(28).fill(null).map((_, index) => ({
    id: null,
    name: "Lumina",
    images: staticGemImages,
  }));

  // Helper function to get product images array
  const getProductImages = (product) => {
    const images = [];
    if (product.imageUrl) {
      images.push(product.imageUrl);
    }
    if (Array.isArray(product.imageUrls)) {
      product.imageUrls.forEach((url) => {
        if (url && !images.includes(url)) {
          images.push(url);
        }
      });
    }
    // Fallback if no images
    if (images.length === 0) {
      images.push("products/allGems/Product_card_2.webp");
    }
    return images;
  };

  // Prepare product cards data - use API data or fallback to static
  const apiProductCards = products.map((product) => ({
    id: product.id,
    name: product.name || "Unnamed Product",
    images: getProductImages(product),
  }));

  // Use API data if available, otherwise use static fallback
  const productCards = (apiProductCards.length > 0) ? apiProductCards : staticFallbackProducts;

  // Fallback images for showcase
  const modelImage1 = "products/allGems/earrings-heart.webp";
  const modelImage2 = "products/allGems/flower.webp";

  // Render product item helper
  const renderProductItem = (product, index) => (
    <ProductCarouselItem
      key={product.id || `product-${index}`}
      images={product.images}
      label={product.name}
      className="gem-item"
      productId={product.id}
    />
  );

  // Loading state - show static fallback while loading
  if (loading) {
    return (
      <div className="all-gems-page">
        <div className="gems-header" data-navbar-theme="white">
          <p className="gems-explore bodytext-4--no-margin">EXPLORE</p>
          <h1 className="gems-title heading-1--no-margin">OUR GEMS</h1>
        </div>
        <div className="gems-loading">
          <div className="gems-loading-spinner"></div>
          <p>Loading products...</p>
        </div>
      </div>
    );
  }

  // Calculate rows based on available products (uses fallback if API failed or empty)
  const row1 = productCards.slice(0, 4);
  const row2 = productCards.slice(4, 8);
  const row3 = productCards.slice(8, 12);
  const row4 = productCards.slice(12, 16);
  const row5 = productCards.slice(16, 20);
  const row6 = productCards.slice(20, 24);

  return (
    <div className="all-gems-page">
      <div
        className="gems-header"
        data-navbar-theme="white"
        data-navbar-theme-mobile="black"
        data-navbar-theme-tablet="black"
      >
        <p className="gems-explore bodytext-4--no-margin">EXPLORE</p>
        <h1 className="gems-title heading-1--no-margin">OUR GEMS</h1>
        {/* <p className="gems-subtitle bodytext-4--no-margin">
          Mirror invites you to step into the era of personalized luxury. Each
          piece is a reflection of your unique style and a signpost to endless
          possibilities. Every gem has a story, and that story awaits your
          personal touch to shine.
        </p> */}

        {/* Layout toggle */}
        <div className="gems-layout-toggle">
          <span className="gems-layout-label bodytext-6--no-margin">View</span>
          <button
            className={`gems-layout-btn bodytext-6--no-margin ${layoutOption === 1 ? "active" : ""}`}
            onClick={() => setLayoutOption(1)}
            aria-label="2 columns layout"
          >
            1
          </button>
          <button
            className={`gems-layout-btn bodytext-6--no-margin ${layoutOption === 2 ? "active" : ""}`}
            onClick={() => setLayoutOption(2)}
            aria-label="4 columns layout"
          >
            2
          </button>
          <button
            className={`gems-layout-btn bodytext-6--no-margin ${layoutOption === 3 ? "active" : ""}`}
            onClick={() => setLayoutOption(3)}
            aria-label="6 columns layout"
          >
            3
          </button>
        </div>
      </div>

      <div className={`gems-grid-container ${layoutOption === 1 ? "layout-2-cols" : layoutOption === 3 ? "layout-6-cols" : ""}`} data-navbar-theme="black">
        {layoutOption === 1 ? (
          /* Layout 1: 2 columns per row with special rows */
          <>
            {/* Row 1 - 1 product + 1 large image (right) */}
            <div className="gems-row gems-row-2">
              {productCards.slice(0, 1).map((product, index) => renderProductItem(product, index))}
              <div ref={largeItem1Ref} className="gem-item gem-item-large">
                <MediaImage src={modelImage1} alt="Model showcase 1" />
              </div>
            </div>
            {/* Row 2-4: 6 products */}
            <div className="gems-row gems-row-2">
              {productCards.slice(1, 7).map((product, index) => renderProductItem(product, index + 1))}
            </div>
            {/* Row 5 - 1 large image (left) + 1 product */}
            <div className="gems-row gems-row-2">
              <div ref={largeItem2Ref} className="gem-item gem-item-large">
                <MediaImage src={modelImage2} alt="Model showcase 2" />
              </div>
              {productCards.slice(7, 8).map((product, index) => renderProductItem(product, index + 7))}
            </div>
            {/* Rest of products */}
            <div className="gems-row gems-row-2">
              {productCards.slice(8).map((product, index) => renderProductItem(product, index + 8))}
            </div>
          </>
        ) : layoutOption === 3 ? (
          /* Layout 3: 6 columns with large images */
          <>
            {/* Row 1: 6 products */}
            <div className="gems-row gems-row-6">
              {productCards.slice(0, 6).map((product, index) => renderProductItem(product, index))}
            </div>
            {/* Row 2-3: Large image 1 (2x2) at start + 8 products */}
            <div className="gems-row gems-row-6-special">
              <div ref={largeItem1Ref} className="gem-item gem-item-large gem-item-2x2">
                <MediaImage src={modelImage1} alt="Model showcase 1" />
              </div>
              {productCards.slice(6, 14).map((product, index) => renderProductItem(product, index + 6))}
            </div>
            {/* Row 4-5: 8 products + Large image 2 (2x2) at end */}
            <div className="gems-row gems-row-6-special">
              {productCards.slice(14, 22).map((product, index) => renderProductItem(product, index + 14))}
              <div ref={largeItem2Ref} className="gem-item gem-item-large gem-item-2x2 gem-item-2x2-end">
                <MediaImage src={modelImage2} alt="Model showcase 2" />
              </div>
            </div>
            {/* Rest of products */}
            <div className="gems-row gems-row-6">
              {productCards.slice(22).map((product, index) => renderProductItem(product, index + 22))}
            </div>
          </>
        ) : (
          /* Layout 2: Original 4 columns layout */
          <>
            {/* First row - 4 items */}
            {row1.length > 0 && (
              <div className="gems-row gems-row-4">
                {row1.map((product, index) => renderProductItem(product, index))}
              </div>
            )}

            {/* Second row - 3 columns (2 columns with 2x2 grid + 1 large) */}
            {row2.length > 0 && (
              <div className="gems-row gems-row-3-special">
                <div className="gem-grid-2x2">
                  {row2.map((product, index) => renderProductItem(product, index + 4))}
                </div>
                <div ref={largeItem1Ref} className="gem-item gem-item-large">
                  <MediaImage src={modelImage1} alt="Model showcase 1" />
                </div>
              </div>
            )}

            {/* Third row - 4 items */}
            {row3.length > 0 && (
              <div className="gems-row gems-row-4">
                {row3.map((product, index) => renderProductItem(product, index + 8))}
              </div>
            )}

            {/* Fourth row - 3 columns (1 large + 2 columns with 2x2 grid) */}
            {row4.length > 0 && (
              <div className="gems-row gems-row-3-reverse-special">
                <div ref={largeItem2Ref} className="gem-item gem-item-large">
                  <MediaImage src={modelImage2} alt="Model showcase 2" />
                </div>
                <div className="gem-grid-2x2">
                  {row4.map((product, index) => renderProductItem(product, index + 12))}
                </div>
              </div>
            )}

            {/* Fifth row - 4 items */}
            {row5.length > 0 && (
              <div className="gems-row gems-row-4">
                {row5.map((product, index) => renderProductItem(product, index + 16))}
              </div>
            )}

            {/* Sixth row - 4 items */}
            {row6.length > 0 && (
              <div className="gems-row gems-row-4">
                {row6.map((product, index) => renderProductItem(product, index + 20))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default AllGems;
