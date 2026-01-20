import { useState, useEffect, useRef, useCallback } from "react";
import { collectionsAPI } from "@services/api";
import { MediaImage } from "@components/common/media";
import ProductCarouselItem from "./ProductCarouselItem";
import "./Section4CollectionDetail.css";

const Section4CollectionDetail = ({
  collectionId,
  showViewProductButton = false,
}) => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const largeItemRef = useRef(null);

  // Parallax effect for large item
  useEffect(() => {
    const handleScroll = () => {
      if (!largeItemRef.current) return;

      const windowHeight = window.innerHeight;
      const rect = largeItemRef.current.getBoundingClientRect();
      const scrollDistance = windowHeight - rect.top;
      const totalDistance = windowHeight + rect.height;
      const scrollRatio = Math.max(0, Math.min(1, scrollDistance / totalDistance));

      // Image moves DOWN slowly (0% → 30%)
      const imgParallax = scrollRatio * 30;

      largeItemRef.current.style.setProperty("--parallax-img-offset", `${imgParallax}%`);
    };

    window.addEventListener("scroll", handleScroll);
    handleScroll();

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Each product has multiple images for carousel
  const productCards = Array(12)
    .fill(null)
    .map(() => ({
      label: "Lumina",
      images: [
        "products/allGems/Product_card_2.webp",
        "products/allGems/model_4.webp",
        "products/allGems/toietmoi-product.webp",
      ],
    }));
  const modelImage = "products/allGems/flower.webp";

  // Helper function to convert slug back to name for API lookup
  const slugToName = (slug) => {
    return slug
      .split("-")
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(" ");
  };

  // Fetch products from collection
  useEffect(() => {
    // Add abort controller to prevent duplicate requests
    const abortController = new AbortController();

    if (!collectionId) {
      setLoading(false);
      setProducts(getDefaultProducts());
      return;
    }

    const fetchCollectionProducts = async () => {
      try {
        setLoading(true);

        // First, get all collections to find the one matching our slug
        const collectionsResponse = await collectionsAPI.getAll();
        const collections =
          collectionsResponse.data.data || collectionsResponse.data || [];

        // Convert slug back to name and find matching collection
        const collectionName = slugToName(collectionId);
        // Try multiple matching strategies
        const matchingCollection = collections.find((col) => {
          const matches =
            col.slug === collectionId || // Direct slug match
            col.name === collectionId || // Direct name match (for case where name IS the slug)
            col.name.toLowerCase() === collectionId.toLowerCase() || // Case-insensitive match
            col.name.toLowerCase() === collectionName.toLowerCase() ||
            col.name.toLowerCase() ===
              collectionId.replace(/-/g, " ").toLowerCase() ||
            col.name.toLowerCase().replace(/\s+/g, "-") === collectionId; // Name to slug

          return matches;
        });

        if (matchingCollection) {
          // Try different API endpoints
          let collectionProducts = [];

          try {
            // Try getWithProducts first (gets collection with its products)
            const withProductsResponse = await collectionsAPI.getWithProducts(
              matchingCollection.id
            );
            // The response structure is: data.products where each item has a 'product' object
            const collectionData = withProductsResponse.data;
            // Check different possible structures
            if (
              collectionData?.products &&
              Array.isArray(collectionData.products)
            ) {
              if (collectionData.products.length > 0) {
                // Check if each item has a 'product' property
                if (collectionData.products[0].product) {
                  collectionProducts = collectionData.products
                    .map((cp) => cp.product)
                    .filter((p) => p);
                } else {
                  collectionProducts = collectionData.products;
                }
              }
            } else if (Array.isArray(collectionData)) {
              // Maybe the response is directly an array
              collectionProducts = collectionData;
            } else {
              // Fallback structure if different
              collectionProducts = withProductsResponse.data?.products || [];
            }
          } catch (err1) {
            try {
              // Fallback to getProductsInCollection
              const productsResponse =
                await collectionsAPI.getProductsInCollection(
                  matchingCollection.id
                );
              collectionProducts =
                productsResponse.data?.data || productsResponse.data || [];
            } catch (err2) {}
          }

          // Check if request was aborted
          if (abortController.signal.aborted) {
            return;
          }

          // Check if we have products
          if (!collectionProducts || collectionProducts.length === 0) {
            setProducts(getDefaultProducts());
            return;
          }

          // Map products to the format needed for display
          const formattedProducts = collectionProducts.map((product) => {
            // Use imageUrl as primary field (matching admin dashboard)
            const imageUrl =
              product.imageUrl ||
              product.image ||
              (product.images && product.images.length > 0
                ? product.images[0]
                : null) ||
              product.mainImage ||
              product.primaryImage ||
              product.thumbnail ||
              product.featuredImage ||
              "collections/collectionDetail/collectionDetail_more.webp";

            return {
              id: product.id,
              name:
                product.name ||
                product.title ||
                product.productName ||
                "Product",
              image: imageUrl,
            };
          });

          setProducts(formattedProducts);
        } else {
          setProducts(getDefaultProducts());
        }

        setError(null);
      } catch (err) {
        // Check if error is due to abort
        if (err.name === "AbortError") {
          return;
        }
        setError("Failed to load products");
        // Use default products as fallback
        setProducts(getDefaultProducts());
      } finally {
        // Only set loading to false if not aborted
        if (!abortController.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchCollectionProducts();

    // Cleanup function to abort request if component unmounts or effect reruns
    return () => {
      abortController.abort();
    };
  }, [collectionId]);

  // Default products for fallback
  const getDefaultProducts = () => {
    return Array(8)
      .fill(null)
      .map((_, index) => ({
        id: index + 1,
        name: "Lumina",
        image: "collections/collectionDetail/collectionDetail_more.webp",
      }));
  };

  return (
    <div className="section4-collection-page" data-navbar-theme="black">
      <div className="section4-collection-header">
        <p className="section4-collection-explore bodytext-4--no-margin">
          EXPLORE
        </p>
        <h1 className="section4-collection-title heading-1--no-margin">
          OUR GEMS
        </h1>
        <p className="section4-collection-subtitle bodytext-4--no-margin">
          Mirror’s curation of visionary designs - where each piece embodies the essence of future luxury. From bold signatures to refined silhouettes, these are the diamonds reimagined for a new era.
        </p>
      </div>

      <div className="section4-collection-grid-container">
        {/* First row - 4 items */}
        <div className="section4-collection-row section4-collection-row-4">
          {productCards.slice(0, 4).map((product, index) => (
            <ProductCarouselItem
              key={`row1-${index}`}
              images={product.images}
              label={product.label}
            />
          ))}
        </div>

        {/* Second row - 3 columns (2 columns with 2x2 grid + 1 large) */}
        <div className="section4-collection-row section4-collection-row-3-special">
          <div className="section4-collection-grid-2x2">
            {productCards.slice(4, 8).map((product, index) => (
              <ProductCarouselItem
                key={`row2-${index}`}
                images={product.images}
                label={product.label}
              />
            ))}
          </div>
          <div ref={largeItemRef} className="section4-collection-item section4-collection-item-large">
            <MediaImage src={modelImage} alt="Model showcase" />
          </div>
        </div>

        {/* Third row - 4 items */}
        <div className="section4-collection-row section4-collection-row-4">
          {productCards.slice(8, 12).map((product, index) => (
            <ProductCarouselItem
              key={`row3-${index}`}
              images={product.images}
              label={product.label}
            />
          ))}
        </div>
      </div>
    </div>
  );
};

export default Section4CollectionDetail;
