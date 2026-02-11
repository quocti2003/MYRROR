/**
 * ProductFinderResultPage - Shows recommended product based on quiz answers
 * Layout: Large product image left, info right
 * Now supports 3 selections: diamond + band + sidestone
 */
import { useEffect, useState, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import { formatPrice } from '@/utils/formatPrice';
import GlassThemeButton from '@/components/common/button/GlassThemeButton';
import { productFinderAPI } from '@/services/api';
import { getProductFinderConfigByKey } from '@/components/productsV2/shapeConfig';

import './ProductFinderResultPage.css';

const ProductFinderResultPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const viewerContainerRef = useRef(null);
  const viewerRef = useRef(null);
  const isViewerInitializedRef = useRef(false);
  const colorChangeHandlerRef = useRef(null);
  const colorsRef = useRef({ main: null, side: null, band: null });

  // Get selections from location state
  const diamond = location.state?.diamond;
  const band = location.state?.band;
  const sidestone = location.state?.sidestone;
  const savedSelections = location.state?.selections || {};
  const savedPrices = location.state?.prices || {};
  const savedEstimatedTotal = location.state?.estimatedTotal || 0;
  const modelKey = location.state?.modelKey; // e.g., "single_round_baguette"
  const passedModelId = location.state?.modelId; // modelId from shapeConfig

  useEffect(() => {
    // Require all 3 selections
    if (!diamond || !band || !sidestone) {
      navigate(ROUTES.PRODUCT_FINDER_CHOOSE_SHAPE, { replace: true });
      return;
    }

    const abortController = new AbortController();

    const fetchRecommendation = async () => {
      try {
        // Try to get recommendation from API
        const response = await productFinderAPI.getRecommendation(
          { diamond, band, sidestone },
          { signal: abortController.signal }
        );
        if (!abortController.signal.aborted) {
          // Use modelId from location state if API doesn't provide one
          const apiProduct = response.data;
          setProduct({
            ...apiProduct,
            modelId: apiProduct.modelId || passedModelId,
          });
        }
      } catch (err) {
        if (abortController.signal.aborted) return;

        // If API fails (e.g., not implemented yet), use local data
        if (err.response?.status === 401) {
          navigate(ROUTES.AUTH_LOGIN, { state: { from: location } });
          return;
        }

        // Fallback: build product from local config
        const config = modelKey ? getProductFinderConfigByKey(modelKey) : null;
        if (config || passedModelId) {
          setProduct({
            name: config?.name || `${band} ${diamond} ${sidestone}`.replace(/-/g, ' '),
            description: `${diamond.charAt(0).toUpperCase() + diamond.slice(1)} diamond with ${band} band and ${sidestone} side stones.`,
            estimatedTotal: savedEstimatedTotal,
            estimatedTotalFormatted: formatPrice(savedEstimatedTotal),
            modelId: passedModelId || config?.modelId,
            images: [],
          });
          setError(null);
        } else {
          const message = err.response?.data?.message || 'Đã có lỗi xảy ra';
          setError(message);
        }
      } finally {
        if (!abortController.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchRecommendation();

    return () => {
      abortController.abort();
    };
  }, [diamond, band, sidestone, navigate, passedModelId, modelKey, savedEstimatedTotal]);

  // iJewel SDK: load 3D model with configurator
  useEffect(() => {
    if (!product?.modelId || !viewerContainerRef.current) return;
    if (!window.ijewelViewer?.loadModelById) {
      console.error('iJewel SDK not loaded');
      return;
    }
    if (isViewerInitializedRef.current) return;
    isViewerInitializedRef.current = true;

    let cancelled = false;

    const handleViewerReady = (event) => {
      if (cancelled) return;

      const viewer = event.detail.viewer;
      viewerRef.current = viewer;

      setTimeout(() => {
        if (cancelled) return;

        const configurator = viewer.plugins?.MaterialConfiguratorPlugin;
        if (!configurator?.variations) return;

        const materialToTab = {};
        configurator.variations.forEach(v => {
          materialToTab[v.uuid] = v.title;
        });
        console.log('Material → Tab mapping:', materialToTab);

        const getMaterials = () => {
          const materials = [];
          viewer.traverseSceneObjects((obj) => {
            if (obj.material) {
              materials.push({
                name: obj.material.name,
                color: obj.material.color?.getHexString?.() || 'N/A',
              });
            }
          });
          return materials;
        };

        const materials = getMaterials();
        const lastTabColors = {};
        Object.keys(materialToTab).forEach(uuid => {
          const mat = materials.find(m => m.name === uuid);
          if (mat) {
            console.log(`[${materialToTab[uuid]}] color: #${mat.color}`);
            lastTabColors[materialToTab[uuid]] = mat.color;
            colorsRef.current[materialToTab[uuid]] = '#' + mat.color;
          }
        });

        const handleColorChange = () => {
          setTimeout(() => {
            const currentMaterials = getMaterials();
            Object.keys(materialToTab).forEach(uuid => {
              const tabName = materialToTab[uuid];
              const mat = currentMaterials.find(m => m.name === uuid);
              if (mat && mat.color !== lastTabColors[tabName]) {
                console.log(`[${tabName}] color changed: #${mat.color}`);
                lastTabColors[tabName] = mat.color;
                colorsRef.current[tabName] = '#' + mat.color;
              }
            });
          }, 200);
        };
        colorChangeHandlerRef.current = handleColorChange;
        document.addEventListener('click', handleColorChange);
      }, 4000);
    };

    window.addEventListener('ijewel-viewer-ready', handleViewerReady);

    const initViewer = async () => {
      try {
        await window.ijewelViewer.loadModelById(
          product.modelId,
          'drive',
          viewerContainerRef.current,
          {
            showUiButtons: false,
            showConfigurator: true,
            hideNameNumbers: true
          }
        );
      } catch (err) {
        console.error('iJewel loadModelById failed:', err);
      }
    };

    initViewer();

    return () => {
      cancelled = true;
      window.removeEventListener('ijewel-viewer-ready', handleViewerReady);
      if (colorChangeHandlerRef.current) {
        document.removeEventListener('click', colorChangeHandlerRef.current);
        colorChangeHandlerRef.current = null;
      }
      if (viewerRef.current?.dispose) {
        viewerRef.current.dispose();
        viewerRef.current = null;
      }
    };
  }, [product]);

  const handleBookAppointment = () => {
    navigate(ROUTES.CONTACT);
  };

  const handlePreOrder = async () => {
    if (saving) return;
    setSaving(true);
    setSaveError(null);
    try {
      await productFinderAPI.saveSelection({
        diamond,
        band,
        sidestone,
        modelKey,
        mainColor: colorsRef.current.main,
        sideColor: colorsRef.current.side,
        bandColor: colorsRef.current.band,
      });
      navigate(ROUTES.CONTACT);
    } catch (err) {
      console.error('Save selection failed:', err);
      // If 401, redirect to login
      if (err.response?.status === 401) {
        navigate(`${ROUTES.AUTH}/login`, { state: { from: location } });
        return;
      }
      setSaveError('Lưu không thành công. Vui lòng thử lại.');
      setSaving(false);
    }
  };

  const handleWishlist = () => {
    setIsWishlisted(!isWishlisted);
  };

  const handleRetake = () => {
    navigate(ROUTES.PRODUCT_FINDER_CHOOSE_SHAPE, {
      state: {
        selections: savedSelections,
        prices: savedPrices,
        estimatedTotal: savedEstimatedTotal
      }
    });
  };

  if (loading) {
    return (
      <div className="product-finder-result" data-navbar-theme="black">
        <div className="product-finder-result__loading">
          <div className="product-finder-result__loading-spinner" />
          <p className="bodytext-5--no-margin">Đang tìm sản phẩm phù hợp...</p>
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="product-finder-result" data-navbar-theme="black">
        <div className="product-finder-result__loading">
          <p className="bodytext-5--no-margin">{error || 'Không tìm thấy sản phẩm'}</p>
          <GlassThemeButton theme="event_dark" onClick={handleRetake}>
            Thử lại
          </GlassThemeButton>
        </div>
      </div>
    );
  }

  return (
    <div className="product-finder-result" data-navbar-theme="black">
      <div className="product-finder-result__container">
        {/* Product Image - Left */}
        <div className="product-finder-result__image-section">
          {product.modelId ? (
            <div
              ref={viewerContainerRef}
              className="product-finder-result__viewer"
            />
          ) : product.images && product.images.length > 0 ? (
            product.images.map((img, idx) => (
              <img
                key={idx}
                src={getMediaUrl(img)}
                alt={`${product.name} - ${idx + 1}`}
                className="product-finder-result__image"
              />
            ))
          ) : (
            <div className="product-finder-result__no-image">
              <img
                src={`/product-finder/a1_${band}_${diamond}_${sidestone}.png`}
                alt={product.name}
                className="product-finder-result__image"
                onError={(e) => {
                  e.target.style.display = 'none';
                  e.target.parentElement.innerHTML = '<p>No image available</p>';
                }}
              />
            </div>
          )}
        </div>

        {/* Product Info - Right */}
        <div className="product-finder-result__info-section">
          <div className="product-finder-result__info-content">
            <h1 className="product-finder-result__name">
              {product.name}
            </h1>

            <p className="product-finder-result__description bodytext-5--no-margin">
              {product.description}
            </p>

            {/* Selection Summary */}
            <div className="product-finder-result__selections">
              <div className="product-finder-result__selection-item">
                <span className="product-finder-result__selection-label">Main Stone:</span>
                <span className="product-finder-result__selection-value">{diamond}</span>
              </div>
              <div className="product-finder-result__selection-item">
                <span className="product-finder-result__selection-label">Band:</span>
                <span className="product-finder-result__selection-value">{band}</span>
              </div>
              <div className="product-finder-result__selection-item">
                <span className="product-finder-result__selection-label">Side Stones:</span>
                <span className="product-finder-result__selection-value">{sidestone}</span>
              </div>
            </div>

            <p className="product-finder-result__price">
              {product.estimatedTotalFormatted}
            </p>

            {/* Action Buttons */}
            <div className="product-finder-result__actions">
              <GlassThemeButton
                theme="event_dark"
                onClick={handleBookAppointment}
                className="product-finder-result__btn-appointment"
              >
                Book An Appointment
              </GlassThemeButton>
              <button
                className={`product-finder-result__btn-wishlist ${isWishlisted ? 'product-finder-result__btn-wishlist--active' : ''}`}
                onClick={handleWishlist}
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill={isWishlisted ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
              </button>
            </div>

            <GlassThemeButton
              theme="event_spec"
              onClick={handlePreOrder}
              disabled={saving}
              className="product-finder-result__btn-preorder"
            >
              {saving ? 'Saving...' : 'Pre-order now'}
            </GlassThemeButton>

            {saveError && (
              <p className="product-finder-result__save-error bodytext-6--no-margin">
                {saveError}
              </p>
            )}

            {/* Links */}
            <div className="product-finder-result__links">
              <button type="button" className="product-finder-result__link" onClick={() => {}}>
                Complimentary shipping & returns
              </button>
              <button type="button" className="product-finder-result__link" onClick={() => navigate(ROUTES.CONTACT)}>
                Contact us
              </button>
            </div>

            {/* Change Selection */}
            <button
              className="product-finder-result__retake"
              onClick={handleRetake}
            >
              ← Thay đổi lựa chọn
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductFinderResultPage;
