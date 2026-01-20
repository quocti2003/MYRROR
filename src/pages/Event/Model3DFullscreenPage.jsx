/**
 * Model3DFullscreenPage - Fullscreen 3D model viewer
 * 100% viewport width and height
 */
import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import EventBackButton from '@/components/event/EventBackButton';

const Model3DFullscreenPage = () => {
  const navigate = useNavigate();
  const modelId = 'WqEaoYpKTUGTtoKyEw_5XQ';

  const handleGoBack = () => {
    navigate(ROUTES.EVENT_THANKYOU);
  };

  const iframeUrl = useMemo(() => {
    const baseUrl = 'https://drive.ijewel3d.com/drive/files';
    const params = new URLSearchParams({
      slug: modelId,
      isAutoplay: 'true',
      isRemoveHologram: 'true',
      isRemoveLogo: 'true',
      isRemoveLogoLink: 'true',
      isConfigurator: 'false',
      isEnabledZoom: 'true',
      isShare: 'false',
      isResetView: 'false',
      isRotateCamera: 'true',
      isPlayCameraViews: 'false',
      isPlayAnimations: 'false',
      isFullScreen: 'false'
    });

    return `${baseUrl}/${modelId}/embedded?${params.toString()}`;
  }, [modelId]);

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      width: '100vw',
      height: '100vh',
      backgroundColor: '#000',
    }}>
      {/* Back Button */}
      <EventBackButton onClick={handleGoBack} theme="event_dark" />

      <iframe
        title="iJewel 3D Viewer"
        src={iframeUrl}
        style={{
          width: '100%',
          height: '100%',
          border: 'none',
          display: 'block'
        }}
        frameBorder="0"
        allow="camera; autoplay; fullscreen; xr-spatial-tracking; web-share"
        allowFullScreen
        mozallowfullscreen="true"
        webkitallowfullscreen="true"
      />
    </div>
  );
};

export default Model3DFullscreenPage;
