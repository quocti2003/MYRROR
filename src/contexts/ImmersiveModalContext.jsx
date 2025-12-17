import { createContext, useContext, useState, useCallback } from "react";
import "./ImmersiveModal.css";

const IMMERSIVE_IFRAME_URL =
  "https://cdn.lov3d.io/spaces/1759690637958-2v6j8tn5fpq/MirrorNEW/index.htm";

// Context
const ImmersiveModalContext = createContext(null);

// Hook to use the context
export const useImmersiveModal = () => {
  const context = useContext(ImmersiveModalContext);
  if (!context) {
    throw new Error(
      "useImmersiveModal must be used within an ImmersiveModalProvider"
    );
  }
  return context;
};

// Provider component
export const ImmersiveModalProvider = ({ children }) => {
  const [isOpen, setIsOpen] = useState(false);

  const openModal = useCallback(() => {
    setIsOpen(true);
    // Prevent body scroll when modal is open
    document.body.style.overflow = "hidden";
  }, []);

  const closeModal = useCallback(() => {
    setIsOpen(false);
    // Restore body scroll
    document.body.style.overflow = "";
  }, []);

  return (
    <ImmersiveModalContext.Provider value={{ isOpen, openModal, closeModal }}>
      {children}

      {/* Global Immersive Modal */}
      {isOpen && (
        <div className="immersive-modal-overlay">
          <button
            className="immersive-modal-close"
            onClick={closeModal}
            aria-label="Close immersive showroom"
          >
            <svg
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M18 6L6 18M6 6L18 18"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>
          <iframe
            src={IMMERSIVE_IFRAME_URL}
            className="immersive-modal-iframe"
            title="Immersive Showroom - 3D Experience"
            allow="fullscreen; xr-spatial-tracking"
            sandbox="allow-scripts allow-same-origin allow-presentation allow-forms"
          />
        </div>
      )}
    </ImmersiveModalContext.Provider>
  );
};

export default ImmersiveModalContext;
