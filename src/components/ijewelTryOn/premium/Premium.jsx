import { useEffect, useRef, useState, useCallback } from 'react';
import { useDeviceCamera } from '../ijewel_useDeviceCamera';
import styles from './premium.module.css';
import MirrorLogo from '@/assets/images/Mirror_Logo_Text_Pink.svg';

const MODELS = [
  { id: 'dY4BIhDDQNmCVTRrEpV2QQ', name: 'Twin', basename: 'drive' },
  { id: 'MKyTIlEyRbi89oT6bH76yA', name: 'Pear', basename: 'drive' },
  { id: 'R4Yyjh0QQlmEtazcWf7IGA', name: 'New', basename: 'drive' },
  { id: 'N1w9lJ3FQfOWsrC7jeeYfA', name: 'Oval', basename: 'drive' },
  { id: 'DfRULQ-OSk6TjbYAcB9zkA', name: 'Fistion', basename: 'drive' },
  { id: 'FWV7-qA6QEG_Ju8pjSItuA', name: 'Triology', basename: 'drive' },
  { id: 'QAauSV24QiuM5CxA_1797w', name: 'Myfav', basename: 'drive' },
  { id: 'RUsrBi-vQey2vExitZOYig', name: 'Demo', basename: 'drive' },
  { id: 'VdiuGY0xSDOOBoxoHU2y-A', name: 'Lumex91Cadillac', basename: 'drive'},
  { id: 'P936xDENR7-yCCiZSMeLTQ', name: 'Lumex91Leaves', basename: 'drive'}, 
  { id: 'RW9pPBQFRmqk88tnI2sMKw', name: 'LuckyRing', basename: 'drive'}, 
  // { id: 'YS4Zch2mShSnA-LABIS5wQ', name: 'Flower', basename: 'drive' },
  // { id: 'czl3wmsyTDWrV420qcKOew', name: 'Heart', basename: 'drive' },
];

const getModelFromURL = () => {
  const params = new URLSearchParams(window.location.search);
  const modelName = params.get('model');
  if (modelName) {
    const found = MODELS.find(m => m.name.toLowerCase() === modelName.toLowerCase());
    if (found) return found.id;
  }
  return MODELS[0].id;
};

// Rotation config - finger index: 0=thumb, 1=index, 2=middle, 3=ring, 4=pinky
const ROTATION_CONFIG = {
  backCamera: {
    right: {
      3: { y: 5, z: 0 },
      4: { y: 10, z: 0 },
      1: { y: 42, z: 0 },
      2: { y: 42, z: 0 }
    },
    left: {
      1: { y: 15, z: 0 }
    }
  },
  frontCamera: {
    right: {
      1: { y: 50, z: 350 },
      2: { y: 60, z: 350 },
    },
  }
};

const Premium = () => {
  const containerRef = useRef(null);
  const viewerAppRef = useRef(null);
  const arPluginRef = useRef(null);
  const isInitializedRef = useRef(false);
  const rafIdRef = useRef(null);

  // Camera state ref (no re-renders)
  const isBackCameraRef = useRef(false);
  const isManualRotationRef = useRef(false);

  const {
    isMobile,
    updateCamera,
    toggleCamera
  } = useDeviceCamera();

  const [currentModelId, setCurrentModelId] = useState(() => getModelFromURL());
  const [inAR, setInAR] = useState(false);
  const [fileConfig, setFileConfig] = useState(null);
  const [currentFinger, setCurrentFinger] = useState(3);
  const [isLoading, setIsLoading] = useState(false);
  const [capturedImage, setCapturedImage] = useState(null);

  // Read hand detection directly from SDK (no state, no re-renders)
  const getDetectedHand = useCallback(() => {
    const handedness = arPluginRef.current?.handDetector?.lastResult?.handedness;
    if (handedness?.[0]?.[0]) {
      const handData = handedness[0][0];
      const label = handData.categoryName || handData.label || handData.displayName;
      return label === 'Left' ? 0 : 1;
    }
    return -1;
  }, []);

  // Apply rotation config based on hand/finger/camera
  // fingerOverride: dùng khi muốn áp dụng ngay cho finger mới (tránh giật)
  const applyRotationConfig = useCallback((fingerOverride) => {
    if (isManualRotationRef.current) return;

    const arPlugin = arPluginRef.current;
    if (!arPlugin?.modelRotation) return;

    const currentHand = getDetectedHand();
    // Default to 'right' if no hand detected
    const handType = currentHand === 0 ? 'left' : 'right';

    const finger = fingerOverride ?? arPlugin.finger;
    const isBackCamera = isBackCameraRef.current;
    const cameraConfig = isBackCamera ? ROTATION_CONFIG.backCamera : ROTATION_CONFIG.frontCamera;
    const fingerConfig = cameraConfig?.[handType]?.[finger];

    if (fingerConfig) {
      arPlugin.modelRotation.y = (fingerConfig.y * Math.PI) / 180;
      arPlugin.modelRotation.z = (fingerConfig.z * Math.PI) / 180;
    } else {
      // Không có config cho ngón này → reset về 0
      arPlugin.modelRotation.y = 0;
      arPlugin.modelRotation.z = 0;
    }
  }, [getDetectedHand]);

  // Set finger VÀ áp dụng rotation config NGAY LẬP TỨC (tránh giật)
  const setFingerWithRotation = useCallback((newFinger) => {
    const arPlugin = arPluginRef.current;
    if (!arPlugin) return;

    // Áp dụng rotation config TRƯỚC khi set finger
    applyRotationConfig(newFinger);
    // Sau đó mới set finger cho SDK
    arPlugin.finger = newFinger;
    setCurrentFinger(newFinger);
  }, [applyRotationConfig]);

  // High performance rAF loop - runs every frame
  const runARLoop = useCallback(() => {
    const arPlugin = arPluginRef.current;
    const viewerApp = viewerAppRef.current;

    // Sync modelRoot.visible with arPlugin.visible
    // arPlugin.visible is automatically true when hand detected, false when no hand
    const modelRoot = viewerApp?.scene?.modelRoot;
    if (modelRoot && arPlugin) {
      modelRoot.visible = arPlugin.visible;
    }

    applyRotationConfig();
    rafIdRef.current = requestAnimationFrame(runARLoop);
  }, [applyRotationConfig]);

  // Start/stop loop based on AR state
  useEffect(() => {
    if (inAR) {
      rafIdRef.current = requestAnimationFrame(runARLoop);
    }
    return () => {
      if (rafIdRef.current) {
        cancelAnimationFrame(rafIdRef.current);
        rafIdRef.current = null;
      }
    };
  }, [inAR, runARLoop]);

  // Initialize viewer
  useEffect(() => {
    const initViewer = async () => {
      if (isInitializedRef.current || !containerRef.current) return;

      const currentModel = MODELS.find(m => m.id === currentModelId);
      if (!currentModel) return;

      isInitializedRef.current = true;

      const handleFileData = (event) => {
        const fileData = event.detail.iJewelFileData?.config;
        if (fileData) setFileConfig(JSON.parse(fileData));
      };

      window.addEventListener('ijewel-file-data', handleFileData);

      // Calculate footer height matching CSS: max(13vh, 9vw)
      const calculateFooterHeight = () => {
        const vh = window.innerHeight / 100;
        const vw = window.innerWidth / 100;
        return Math.max(13 * vh, 9 * vw);
      };

      // Configurator sits right above footer
      const calculateConfiguratorOffset = () => {
        const footerHeight = calculateFooterHeight();
        return footerHeight;
      };

      await window.ijewelViewer.loadModelById(currentModel.id, currentModel.basename, containerRef.current, {
        showUiButtons: false,
        hideTryOn: false,
        showConfigurator: true,
        configuratorBottomOffsetPx: calculateConfiguratorOffset(),
        hideNameNumbers: true
      });

      const handleViewerReady = (event) => {
        viewerAppRef.current = event.detail.viewer;
      };

      window.addEventListener('ijewel-viewer-ready', handleViewerReady);

      return () => {
        window.removeEventListener('ijewel-file-data', handleFileData);
        window.removeEventListener('ijewel-viewer-ready', handleViewerReady);
        if (viewerAppRef.current) {
          viewerAppRef.current.dispose?.();
          viewerAppRef.current = null;
        }
        isInitializedRef.current = false;
      };
    };

    initViewer();
  }, [currentModelId]);

  // ============================================================================
  // AR CONTROLS
  // ============================================================================

  const startAR = async () => {
    if (!window.ij_vto || !viewerAppRef.current) {
      console.error('❌ Resources not ready');
      return;
    }

    if (arPluginRef.current) {
      console.warn('AR already running');
      return;
    }

    setIsLoading(true);

    try {
      const viewerApp = viewerAppRef.current;
      const arPlugin = await viewerApp.addPlugin(window.ij_vto.RingTryonPlugin);
      arPluginRef.current = arPlugin;

      arPlugin.modelScaleFactor = 0.5;
      arPlugin.occluderScaleFactor = 1.0;

      if (fileConfig?.tryonConfig) {
        fileConfig.tryonConfig.type = 'RingTryonPlugin';
        arPlugin.fromJSON(fileConfig?.tryonConfig);
      }

      // Ẩn model trước khi AR start (sẽ hiện lại khi detect được tay via rAF loop)
      const modelRoot = viewerApp?.scene?.modelRoot;
      if (modelRoot) {
        modelRoot.visible = false;
      }

      await arPlugin.start();

      // Set finger after AR started - dùng setFingerWithRotation để tránh giật
      setFingerWithRotation(3);

      if (isMobile) {
        await arPlugin.flipCamera();
        isBackCameraRef.current = true;
        updateCamera(0);
      } else {
        isBackCameraRef.current = false;
      }

      setInAR(true);
    } catch (error) {
      console.error('AR error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const stopAR = async () => {
    // Kill loop first
    if (rafIdRef.current) {
      cancelAnimationFrame(rafIdRef.current);
      rafIdRef.current = null;
    }

    const arPlugin = arPluginRef.current;
    if (arPlugin) {
      try {
        await arPlugin.stop();
      } catch (e) {
        console.warn(e);
      }
      arPluginRef.current = null;
    }

    // Hiện lại model khi thoát AR
    const modelRoot = viewerAppRef.current?.scene?.modelRoot;
    if (modelRoot) {
      modelRoot.visible = true;
    }

    setInAR(false);
  };

  // ============================================================================
  // CAMERA CONTROLS
  // ============================================================================

  const handleFlipCamera = async () => {
    const arPlugin = arPluginRef.current;
    if (!arPlugin || !inAR) return;

    try {
      await arPlugin.flipCamera();
      isBackCameraRef.current = !isBackCameraRef.current;
      toggleCamera();
    } catch (error) {
      console.error('Flip camera error:', error);
    }
  };

  // ============================================================================
  // RING CONTROLS
  // ============================================================================

  const handleSwitchFinger = () => {
    const arPlugin = arPluginRef.current;
    if (!arPlugin || !inAR) return;

    const newFinger = (arPlugin.finger + 1) % 5;
    setFingerWithRotation(newFinger);
  };

  // Capture photo - composite video (camera) + canvas (3D model)
  const handleCapture = () => {
    const viewerApp = viewerAppRef.current;
    const arPlugin = arPluginRef.current;

    // Try to get canvas from viewerApp
    let canvas = viewerApp?.canvas || viewerApp?.renderer?.domElement;

    // If still not found, check arPlugin
    if (!canvas && arPlugin) {
      for (const key in arPlugin) {
        const val = arPlugin[key];
        if (val instanceof HTMLCanvasElement) {
          canvas = val;
        }
      }
    }

    if (!canvas) {
      console.error('Canvas not found in viewerApp or arPlugin');
      return;
    }

    try {
      // Create composite canvas with logo
      const compositeCanvas = document.createElement('canvas');
      const ctx = compositeCanvas.getContext('2d');

      compositeCanvas.width = canvas.width;
      compositeCanvas.height = canvas.height;

      // Flip horizontally for back camera (to fix flipped image)
      const shouldFlip = isBackCameraRef.current;
      if (shouldFlip) {
        ctx.scale(-1, 1);
        ctx.translate(-canvas.width, 0);
      }

      // Draw the main canvas (AR view)
      ctx.drawImage(canvas, 0, 0);

      // Reset transform for logo drawing
      if (shouldFlip) {
        ctx.setTransform(1, 0, 0, 1, 0, 0);
      }

      // Load and draw logo
      const logo = new Image();
      logo.onload = () => {
        // Logo size: 2% of canvas AREA
        const canvasArea = compositeCanvas.width * compositeCanvas.height;
        const logoArea = canvasArea * 0.02;
        const aspectRatio = logo.naturalWidth / logo.naturalHeight;
        const logoWidth = Math.sqrt(logoArea * aspectRatio);
        const logoHeight = logoWidth / aspectRatio;

        // Position: bottom right with padding (3% from edges)
        const padding = compositeCanvas.width * 0.03;
        const x = compositeCanvas.width - logoWidth - padding;
        const y = compositeCanvas.height - logoHeight - padding;

        ctx.drawImage(logo, x, y, logoWidth, logoHeight);

        const imageData = compositeCanvas.toDataURL('image/png');
        setCapturedImage(imageData);
      };

      logo.onerror = () => {
        // If logo fails to load, save without logo
        const imageData = compositeCanvas.toDataURL('image/png');
        setCapturedImage(imageData);
      };

      logo.src = MirrorLogo;
    } catch (error) {
      console.error('Capture error:', error);
    }
  };

  // Close captured image preview
  const handleCloseCapture = () => {
    setCapturedImage(null);
  };

  // Download captured image then close preview
  const handleDownload = () => {
    if (!capturedImage) return;

    // Format: DD-MM-YYYY theo múi giờ Việt Nam (UTC+7)
    const vietnamDate = new Intl.DateTimeFormat('vi-VN', {
      timeZone: 'Asia/Ho_Chi_Minh',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(new Date()).replace(/\//g, '-');

    const link = document.createElement('a');
    link.download = `mirror-tryon-${vietnamDate}.png`;
    link.href = capturedImage;
    link.click();

    // Auto close preview after download
    setCapturedImage(null);
  };

  return (
    <div className={styles.container}>
      <div ref={containerRef} className={styles.viewerContainer} />

      {/* SVG Filter for Liquid Glass Effect */}
      <svg className={styles.svgFilter} aria-hidden="true">
        <filter id="liquidGlass" primitiveUnits="objectBoundingBox">
          <feGaussianBlur in="SourceGraphic" stdDeviation="0.02" result="blur" />
          <feDisplacementMap in="blur" in2="blur" scale="0.3" xChannelSelector="R" yChannelSelector="G" />
        </filter>
      </svg>

      {/* Footer with controls */}
      <div className={styles.footer}>
        {inAR ? (
          <div className={styles.footerControls}>
            {/* Exit button */}
            <button className={styles.circleButton} onClick={stopAR} aria-label="Exit AR">
              <svg className={styles.buttonIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>

            {/* Switch Finger */}
            <button className={styles.circleButton} onClick={handleSwitchFinger} aria-label="Switch Finger">
              <svg className={styles.buttonIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M18 11V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2v1" />
                <path d="M14 10V4a2 2 0 0 0-2-2a2 2 0 0 0-2 2v6" />
                <path d="M10 10.5V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2v8" />
                <path d="M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15" />
              </svg>
            </button>

            {/* Camera flip */}
            <button className={styles.circleButton} onClick={handleFlipCamera} aria-label="Flip Camera">
              <svg className={styles.buttonIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M11 19H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h5" />
                <path d="M13 5h7a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-5" />
                <circle cx="12" cy="12" r="3" />
                <path d="m18 22-3-3 3-3" />
                <path d="m6 2 3 3-3 3" />
              </svg>
            </button>

            {/* Capture button */}
            <button className={styles.circleButton} onClick={handleCapture} aria-label="Capture Photo">
              <svg className={styles.buttonIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z" />
                <circle cx="12" cy="13" r="3" />
              </svg>
            </button>
          </div>
        ) : (
          <div className={styles.footerControlsCenter}>
            <button className={styles.pillButton} onClick={startAR} disabled={isLoading}>
              {isLoading ? 'Loading...' : 'Start AR'}
            </button>
          </div>
        )}
      </div>

      {/* Captured Image Preview Overlay */}
      {capturedImage && (
        <div className={styles.captureOverlay}>
          <img src={capturedImage} alt="Captured" className={styles.capturedImage} />
          <div className={styles.captureControls}>
            {/* Close button */}
            <button className={styles.circleButton} onClick={handleCloseCapture} aria-label="Close">
              <svg className={styles.buttonIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>

            {/* Download button */}
            <button className={styles.circleButton} onClick={handleDownload} aria-label="Download">
              <svg className={styles.buttonIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Premium;
