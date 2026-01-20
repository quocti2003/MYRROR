import React, { useState } from "react";
import "./Section4ProductMedia.css";

const Section4ProductMedia = ({ product }) => {
  const [activeTab, setActiveTab] = useState("photos");

  // Prepare media data - filter out example.com placeholders
  const filterValidUrls = (urls) => {
    if (!urls) return [];
    return Array.isArray(urls)
      ? urls.filter(url => url && !url.includes('example.com'))
      : [];
  };

  // Combine imageUrls and imageUrl, removing duplicates
  const allImageUrls = [...new Set([
    ...(product?.imageUrls || []),
    ...(product?.imageUrl ? [product.imageUrl] : [])
  ].map(url => url?.trim()).filter(Boolean))];
  const photos = filterValidUrls(allImageUrls);

  // Combine videoUrls and videoUrl, removing duplicates
  const allVideoUrls = [...new Set([
    ...(product?.videoUrls || []),
    ...(product?.videoUrl ? [product.videoUrl] : [])
  ].map(url => url?.trim()).filter(Boolean))];
  const videos = filterValidUrls(allVideoUrls);

  const model3D = product?.model3dUrl && !product.model3dUrl.includes('example.com') ? product.model3dUrl : null;
  const hasTryOn = product?.arTryOnEnabled || false;

  // Determine available tabs
  const tabs = [];
  if (photos.length > 0) tabs.push({ id: "photos", label: "Photos", icon: "📸" });
  if (videos.length > 0) tabs.push({ id: "videos", label: "Videos", icon: "🎥" });
  if (model3D) tabs.push({ id: "360", label: "360° View", icon: "🔄" });
  if (hasTryOn) tabs.push({ id: "tryon", label: "Try On", icon: "👁️" });

  // If no media available, don't render
  if (tabs.length === 0) {
    return null;
  }

  // Set initial active tab to first available
  if (!tabs.find(t => t.id === activeTab) && tabs.length > 0) {
    setActiveTab(tabs[0].id);
  }

  return (
    <div className="section4-product-media" data-navbar-theme="black">
      <div className="product-media-container">
        {/* Header */}
        <div className="media-header">
          <h2 className="media-title heading-2--no-margin">
            Experience The Product
          </h2>
          <p className="media-subtitle bodytext-4--no-margin">
            Explore through photos, videos, and augmented reality
          </p>
        </div>

        {/* Tab Navigation */}
        <div className="media-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={`media-tab ${activeTab === tab.id ? "active" : ""}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <span className="tab-icon">{tab.icon}</span>
              <span className="tab-label">{tab.label}</span>
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="media-content">
          {/* Photos Tab */}
          {activeTab === "photos" && photos.length > 0 && (
            <div className="media-panel photos-panel">
              <div className="photos-grid">
                {photos.map((photo, idx) => (
                  <div key={idx} className="photo-item">
                    <img src={photo} alt={`${product?.name} - ${idx + 1}`} />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Videos Tab */}
          {activeTab === "videos" && videos.length > 0 && (
            <div className="media-panel videos-panel">
              <div className="videos-grid">
                {videos.map((video, idx) => (
                  <div key={idx} className="video-item">
                    <video
                      controls
                      src={video}
                      className="product-video"
                    >
                      Your browser does not support the video tag.
                    </video>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 360 View Tab */}
          {activeTab === "360" && model3D && (
            <div className="media-panel ar-view-panel">
              <div className="ar-viewer-placeholder">
                <div className="placeholder-icon">🔄</div>
                <h3>360° 3D Model Viewer</h3>
                <p>Interactive 3D model viewer will be integrated here</p>
                <p className="placeholder-note">Model URL: {model3D}</p>
                <button className="placeholder-button">
                  Load 3D Model
                </button>
              </div>
            </div>
          )}

          {/* AR Try-On Tab */}
          {activeTab === "tryon" && hasTryOn && (
            <div className="media-panel ar-tryon-panel">
              <div className="ar-tryon-placeholder">
                <div className="placeholder-icon">👁️</div>
                <h3>AR Virtual Try-On</h3>
                <p>See how this piece looks on you using your camera</p>
                <button className="placeholder-button primary">
                  Launch Try-On Experience
                </button>
                <p className="placeholder-note">
                  Requires camera access on mobile or desktop
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Media Info */}
        <div className="media-info">
          <p className="info-text bodytext-4--no-margin">
            {activeTab === "photos" && `${photos.length} ${photos.length === 1 ? 'photo' : 'photos'} available`}
            {activeTab === "videos" && `${videos.length} ${videos.length === 1 ? 'video' : 'videos'} available`}
            {activeTab === "360" && "Rotate and zoom the 3D model"}
            {activeTab === "tryon" && "Compatible with iOS and Android devices"}
          </p>
        </div>
      </div>
    </div>
  );
};

export default Section4ProductMedia;
