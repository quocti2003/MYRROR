import { useEffect, useRef, useState, useCallback } from 'react';
import { useDeviceCamera } from '../ijewel_useDeviceCamera';
import styles from './premium_dev.module.css';

const MODELS = [
  { id: 'dY4BIhDDQNmCVTRrEpV2QQ', name: 'Twin', basename: 'drive' }, // chuan
  { id: 'MKyTIlEyRbi89oT6bH76yA', name: 'Pear', basename: 'drive' }, // chuan
  { id: 'R4Yyjh0QQlmEtazcWf7IGA', name: 'New', basename: 'drive' }, // chuan, chi co sai luc viewer thoi 
  { id: 'N1w9lJ3FQfOWsrC7jeeYfA', name: 'Oval', basename: 'drive' }, // chuan
  { id: 'DfRULQ-OSk6TjbYAcB9zkA', name: 'Fistion', basename: 'drive' }, // chuan
  { id: 'FWV7-qA6QEG_Ju8pjSItuA', name: 'Triology', basename: 'drive' }, // chuan
  { id: 'QAauSV24QiuM5CxA_1797w', name: 'Myfav', basename: 'drive' }, // chuan
  { id: 'VdiuGY0xSDOOBoxoHU2y-A', name: 'Lumex91Cadillac', basename: 'drive' }, // chuan
  // ==============================================================================================================
  // đang sửa

  // ==============================================================================================================
  // chịu chúa cứu, hữu duyên cứu được thì cứu
  { id: 'YS4Zch2mShSnA-LABIS5wQ', name: 'Flower', basename: 'drive' }, // scale quá lớn cái heart có cái lỗi giống cái flower
  { id: 'czl3wmsyTDWrV420qcKOew', name: 'Heart', basename: 'drive' }, // scale quá lớn, sai như hình gửi trong zalo, sai luôn có lúc viewer nha
  { id: 'RUsrBi-vQey2vExitZOYig', name: 'Demo', basename: 'drive' },

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

  // FPS counter refs
  const fpsRef = useRef(0);
  const frameCountRef = useRef(0);
  const lastFpsTimeRef = useRef(performance.now());
  const [displayFps, setDisplayFps] = useState(0);

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
  const [displayCamera, setDisplayCamera] = useState('Trước');
  const [displayHand, setDisplayHand] = useState('Chưa detect');

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

    // FPS calculation + update debug info (mỗi giây 1 lần để không lag)
    frameCountRef.current++;
    const now = performance.now();
    const elapsed = now - lastFpsTimeRef.current;
    if (elapsed >= 1000) {
      fpsRef.current = Math.round((frameCountRef.current * 1000) / elapsed);
      setDisplayFps(fpsRef.current);
      frameCountRef.current = 0;
      lastFpsTimeRef.current = now;

      // Update hand display
      const hand = getDetectedHand();
      if (hand === 0) setDisplayHand('Trái');
      else if (hand === 1) setDisplayHand('Phải');
      else setDisplayHand('Chưa detect');
    }

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

      await window.ijewelViewer.loadModelById(currentModel.id, currentModel.basename, containerRef.current, {
        showUiButtons: false,
        hideTryOn: false
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
        setDisplayCamera('Sau');
        updateCamera(0);
      } else {
        isBackCameraRef.current = false;
        setDisplayCamera('Trước');
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
      setDisplayCamera(isBackCameraRef.current ? 'Sau' : 'Trước');
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

  // FPS color: green >= 50, yellow >= 30, red < 30
  const getFpsColor = (fps) => {
    if (fps >= 50) return '#00ff00';  // Xanh lá - Tốt
    if (fps >= 30) return '#ffff00';  // Vàng - Trung bình
    return '#ff0000';                  // Đỏ - Lag
  };

  // Finger names
  const FINGER_NAMES = ['Cái', 'Trỏ', 'Giữa', 'Áp út', 'Út'];

  return (
    <div className={styles.container}>
      <div ref={containerRef} className={styles.viewerContainer} />

      {/* Debug Info - chỉ hiện khi AR đang chạy */}
      {inAR && (
        <div className={styles.debugPanel}>
          <div className={styles.debugRow}>
            <span className={styles.debugLabel}>FPS:</span>
            <span style={{ color: getFpsColor(displayFps) }}>{displayFps}</span>
          </div>
          <div className={styles.debugRow}>
            <span className={styles.debugLabel}>Cam:</span>
            <span>{displayCamera}</span>
          </div>
          <div className={styles.debugRow}>
            <span className={styles.debugLabel}>Tay:</span>
            <span>{displayHand}</span>
          </div>
          <div className={styles.debugRow}>
            <span className={styles.debugLabel}>Ngón:</span>
            <span>{FINGER_NAMES[currentFinger]}</span>
          </div>
        </div>
      )}

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
            <button className={styles.pillButton} onClick={handleSwitchFinger}>
              Switch Finger
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
          </div>
        ) : (
          <div className={styles.footerControlsCenter}>
            <button className={styles.pillButton} onClick={startAR} disabled={isLoading}>
              {isLoading ? 'Loading...' : 'Start AR'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default Premium;