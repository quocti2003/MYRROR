// Thêm 'useState', 'useEffect' từ React
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";
import CollectionHeroSection from "./CollectionHeroSection";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import UnderlineButton from "@components/common/button/UnderlineButton";
import ArrowButton from "@components/common/button/ArrowButton";
import { collectionsAPI } from "@services/api";
import { getCollectionDetailRoute } from "@/constants/routes";
import MediaImage from "@components/common/media/MediaImage";
import "./Collections.css";

const products = [
  {
    id: 1,
    title: "AURORA",
    image: "/collections/pendant.webp",
    description:
      "More than a ring, AURORA is a \ncelebration of light, geometry, and the \nfuture you're building together.",
  },
  {
    id: 2,
    title: "SOLARIS",
    image: "/collections/ring1.webp",
    description:
      "A testament to the sun's eternal brilliance, \ncaptured in a timeless design that radiates \nwarmth and elegance.",
  },
  {
    id: 3,
    title: "LUNA",
    image: "/collections/earings.webp",
    description:
      "Capturing the serene glow of the moonlight, \nLUNA reflects a story of mystique and \nprofound beauty.",
  },
];

function Collection() {
  const navigate = useNavigate();

  const [currentIndex, setCurrentIndex] = useState(0);
  const [featuredCollection, setFeaturedCollection] = useState(null);
  const [otherCollections, setOtherCollections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [slideDirection, setSlideDirection] = useState("right");

  // Fetch collections data from API
  useEffect(() => {
    const fetchCollections = async () => {
      try {
        setLoading(true);
        // Get all collections
        const response = await collectionsAPI.getAll();
        const collections = response.data.data || response.data;

        // Find "Treasure of the Orient" collection for featured section
        const treasureCollection = collections.find(
          (col) =>
            col.name.toLowerCase() === "treasure of the orient".toLowerCase()
        );

        if (treasureCollection) {
          setFeaturedCollection(treasureCollection);
          // Set other collections (excluding the featured one)
          setOtherCollections(
            collections.filter((col) => col.id !== treasureCollection.id)
          );
        } else {
          // If not found, use the first collection as featured
          setFeaturedCollection(collections[0]);
          setOtherCollections(collections.slice(1));
        }
        setError(null);
      } catch (err) {
        console.error("Error fetching collections:", err);
        setError("Failed to load collection data");
        // Set default values for fallback
        setFeaturedCollection({
          id: "treasure-of-the-orient",
          name: "Treasure of the Orient",
          description:
            "Step into a world where ancient splendor meets modern elegance.",
        });
        // Set default other collections
        setOtherCollections([
          { id: "whispers-of-kyoto", name: "Whispers of Kyoto" },
          { id: "oceans-embrace", name: "Ocean's Embrace" },
          { id: "nile-reverie", name: "Nile Reverie" },
          { id: "byzantine-bloom", name: "Byzantine Bloom" },
          { id: "sands-of-samarkand", name: "Sands of Samarkand" },
          { id: "echoes-of-eternity", name: "Echoes of Eternity" },
          { id: "the-alchemists-touch", name: "The Alchemist's Touch" },
          { id: "lunar-veil", name: "Lunar Veil" },
        ]);
      } finally {
        setLoading(false);
      }
    };

    fetchCollections();
  }, []);

  // Auto-rotate images every 3 seconds - reset khi user click arrow
  useEffect(() => {
    const interval = setInterval(() => {
      setSlideDirection("right");
      setCurrentIndex((prevIndex) => (prevIndex + 1) % products.length);
    }, 3000);

    return () => clearInterval(interval);
  }, [currentIndex]); // Thêm currentIndex vào dependency để reset interval khi index thay đổi

  // MỚI: Hàm handleNext được đơn giản hóa tối đa
  const handleNext = () => {
    setSlideDirection("right");
    setCurrentIndex((prevIndex) => (prevIndex + 1) % products.length);
    // Interval sẽ tự động reset nhờ useEffect dependency [currentIndex]
  };

  // MỚI: Hàm handlePrevious được đơn giản hóa tối đa
  const handlePrevious = () => {
    setSlideDirection("left");
    setCurrentIndex(
      (prevIndex) => (prevIndex - 1 + products.length) % products.length
    );
    // Interval sẽ tự động reset nhờ useEffect dependency [currentIndex]
  };

  const currentProduct = products[currentIndex];

  // Helper function to convert collection name to URL slug
  const nameToSlug = (name) => {
    return name
      .toLowerCase()
      .replace(/['']/g, "") // Remove apostrophes
      .replace(/[^a-z0-9]+/g, "-") // Replace non-alphanumeric with hyphens
      .replace(/^-+|-+$/g, ""); // Remove leading/trailing hyphens
  };

  const handleExploreCollection = async () => {
    if (featuredCollection && featuredCollection.name) {
      const slug = nameToSlug(featuredCollection.name);
      await optimizedTransitionUtils.transitionToRoute(
        navigate,
        getCollectionDetailRoute(slug)
      );
    }
  };

  const handleNavigateToCollection = async (collectionName) => {
    if (collectionName) {
      const slug = nameToSlug(collectionName);
      await optimizedTransitionUtils.transitionToRoute(
        navigate,
        getCollectionDetailRoute(slug)
      );
    }
  };

  return (
    <div className="collection-page">
      {/* --- SECTION 1 --- */}
      <div data-section="collection-hero">
        <CollectionHeroSection />
      </div>

      {/* --- SECTION 2 --- */}
      <div className="section-2" data-section="collection-featured">
        <div className="collection-hero-content">
          <div className="collection-hero-subtitle bodytext-4--no-margin">
            THE NEW COLLECTION
          </div>
          <div className="collection-hero-title">
            <div className="collection-title-text heading-2--no-margin">
              {loading
                ? "Loading..."
                : featuredCollection?.title || "TREASURE OF THE ORIENT"}
            </div>
          </div>
          <div className="collection-hero-description bodytext-4--no-margin">
            {loading
              ? "Loading collection details..."
              : featuredCollection?.description || (
                  <>
                    Step into a world where ancient splendor meets modern
                    elegance. The
                    <br />
                    <strong>
                      {featuredCollection?.name || "TREASURE OF THE ORIENT"}
                    </strong>{" "}
                    collection draws inspiration from the rich cultural
                    <br />
                    heritage, vibrant artistry, and timeless mystique of the
                    East.
                  </>
                )}
          </div>
          <ShineGlassButton
            theme="footer"
            onClick={handleExploreCollection}
            className="collection-hero-explore-button"
          >
            Explore this collection
          </ShineGlassButton>
        </div>

        <div className="collection-content-panel full-width">
          <div className="product-slider">
            <div className="slider-main-row">
              <ArrowButton
                direction="left"
                className="slider-arrow"
                onClick={handlePrevious}
                ariaLabel="Previous Product"
              />

              {/* MỚI: Áp dụng class động dựa trên slideDirection */}
              <div
                className={`product-image-container slide-effect-${slideDirection}`}
                key={`${currentProduct.id}-${slideDirection}`}
              >
                <MediaImage
                  src={currentProduct.image}
                  alt={currentProduct.title}
                  className="product-image"
                />
              </div>

              <ArrowButton
                direction="right"
                className="slider-arrow"
                onClick={handleNext}
                ariaLabel="Next Product"
              />
            </div>

            {/* Product info with separate animation for title and static button */}
            <div className="product-info">
              <h3
                className={`heading-3--no-margin product-title slide-effect-${slideDirection}`}
                key={`${currentProduct.id}-title-${slideDirection}`}
              >
                {currentProduct.title}
              </h3>
              <ShineGlassButton
                className="shop-now-button"
                theme="footer"
              >
                Shop now
              </ShineGlassButton>
            </div>
          </div>
        </div>
      </div>

      {/* --- SECTION 3 --- */}
      <div className="section-3" data-section="collection-other" data-navbar-theme="black">
        <div className="other-collections-content">
          <div className="other-collections-subtitle bodytext-4--no-margin">
            OTHER COLLECTIONS
          </div>
          <div className="collection-names">
            <div className="collection-name heading-1--no-margin">
              Whispers of Kyoto
            </div>
            <div className="collection-name heading-1--no-margin">
              Ocean's Embrace
            </div>
            <div className="collection-name heading-1--no-margin">
              Nile Reverie
            </div>
            <div className="collection-name heading-1--no-margin">
              Byzantine Bloom
            </div>
            <div className="collection-name heading-1--no-margin">
              Sands of Samarkand
            </div>
            <div className="collection-name heading-1--no-margin">
              Echoes of Eternity
            </div>
            <div className="collection-name heading-1--no-margin">
              The Alchemist's Touch
            </div>
            <div className="collection-name heading-1--no-margin">
              Lunar Veil
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Collection;
