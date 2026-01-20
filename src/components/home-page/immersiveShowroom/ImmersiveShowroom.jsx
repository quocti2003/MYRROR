import { useState } from "react";
import { MediaImage } from "@components/common/media";
import "./ImmersiveShowroom.css";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import { useImmersiveModal } from "@/contexts/ImmersiveModalContext";

const ImmersiveShowroom = () => {
  const [iframeLoaded, setIframeLoaded] = useState(false);
  const [iframeError, setIframeError] = useState(false);
  const { openModal } = useImmersiveModal();

  const handleExploreClick = () => {
    openModal();
  };

  const handleIframeLoad = () => {
    setIframeLoaded(true);
    setIframeError(false);
  };

  const handleIframeError = () => {
    setIframeLoaded(false);
    setIframeError(true);
  };

  return (
    <>
      <div className="immersive-showroom">
        {/* Fallback background image - always rendered, CSS controls visibility */}
        <MediaImage
          src="immersiveShowroom/immersiveShowroomBackground.webp"
          alt="Immersive Showroom Background"
          className={`showroom-background-fallback ${
            !iframeLoaded || iframeError ? "visible" : ""
          }`}
        />

        {/* Iframe - hidden on mobile via CSS */}
        {!iframeError && (
          <iframe
            src="https://cdn.lov3d.io/spaces/1759690637958-2v6j8tn5fpq/MirrorNEW/index.htm"
            className={`showroom-background ${iframeLoaded ? "loaded" : ""}`}
            title="3D viewer for Mirror Jewelry"
            allow="fullscreen; xr-spatial-tracking"
            sandbox="allow-scripts allow-same-origin allow-presentation allow-forms"
            loading="lazy"
            onLoad={handleIframeLoad}
            onError={handleIframeError}
          />
        )}

        {/* Gradient overlays */}
        <div className="showroom-gradient-top"></div>
        <div className="showroom-gradient-bottom"></div>

        <div className="showroom-content">
          <h4 className="bodytext-4--no-margin showroom-subtitle">
            WHERE TECHNOLOGY MEETS EMOTIONS
          </h4>
          <h1 className="heading-1--no-margin showroom-title">
            IMMERSIVE SHOWROOM
          </h1>
          <p className="bodytext-4--no-margin showroom-description">
            Step into Mirror's physical universe - a sensorial space where
            light, sound, and storytelling converge. Here, lab-grown brilliance
            comes alive through cinematic displays, tactile explorations and
            AR/VR encounters that let you feel the future of luxury before you
            wear it.
          </p>
          <ShineGlassButton
            theme="footer"
            className="explore-button"
            onClick={handleExploreClick}
          >
            Explore
          </ShineGlassButton>
        </div>
      </div>
    </>
  );
};

export default ImmersiveShowroom;
