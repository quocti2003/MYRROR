import { useLocation } from "react-router-dom";
import { ROUTES } from "@/constants/routes";
import { useImmersiveModal } from "@/contexts/ImmersiveModalContext";
import "./ImmersiveButton.css";

/**
 * ImmersiveButton component - Globe icon button for immersive experience
 * Supports expanded state with text and collapsed state with icon only
 * Opens fullscreen immersive iframe modal when clicked
 *
 * @param {string} className - Additional CSS classes
 * @param {string} theme - Theme variant: "white" | "black" | "blend" (default: "blend")
 * @param {boolean} isCollapsed - Whether button is collapsed (icon only) or expanded (with text)
 */
const ImmersiveButton = ({
  className = "",
  theme = "blend",
  isCollapsed = false,
  ...props
}) => {
  const location = useLocation();
  const { openModal } = useImmersiveModal();

  // Don't render on Immersive Showroom page
  if (location.pathname === ROUTES.IMMERSIVE_SHOWROOM) {
    return null;
  }

  const handleClick = () => {
    openModal();
  };

  return (
    <button
      className={`immersive-globe-btn immersive-globe-btn--${theme} ${
        isCollapsed
          ? "immersive-globe-btn--collapsed"
          : "immersive-globe-btn--expanded"
      } ${className}`}
      aria-label="Immersive Showroom"
      onClick={handleClick}
      {...props}
    >
      <svg
        className="immersive-globe-btn__icon"
        width="18"
        height="18"
        viewBox="0 0 18 18"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <circle cx="9" cy="9" r="8.5" stroke="currentColor" />
        <path
          d="M9 0.5C9.27165 0.5 9.59018 0.645122 9.93945 1.03906C10.2882 1.4324 10.6249 2.03008 10.918 2.81152C11.5027 4.37091 11.875 6.5588 11.875 9C11.875 11.4412 11.5027 13.6291 10.918 15.1885C10.6249 15.9699 10.2882 16.5676 9.93945 16.9609C9.59018 17.3549 9.27165 17.5 9 17.5C8.72835 17.5 8.40982 17.3549 8.06055 16.9609C7.71182 16.5676 7.37509 15.9699 7.08203 15.1885C6.49726 13.6291 6.125 11.4412 6.125 9C6.125 6.5588 6.49726 4.37091 7.08203 2.81152C7.37509 2.03008 7.71182 1.4324 8.06055 1.03906C8.40982 0.645122 8.72835 0.5 9 0.5Z"
          stroke="currentColor"
        />
        <path
          d="M0.5625 9H17.4375"
          stroke="currentColor"
          strokeLinecap="round"
        />
      </svg>
      <span className="immersive-globe-btn__text bodytext-6--no-margin">
        Immersive Showroom
      </span>
    </button>
  );
};

export default ImmersiveButton;
