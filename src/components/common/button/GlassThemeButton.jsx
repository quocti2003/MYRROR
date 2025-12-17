import { useLocation } from "react-router-dom";
import { ROUTES } from "@/constants/routes";
import { useImmersiveModal } from "@/contexts/ImmersiveModalContext";
import "./GlassThemeButton.css";

// Arrow down icon
const ArrowDownIcon = () => (
  <svg
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M19 9L12 16L5 9"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

// Arrow up icon
const ArrowUpIcon = () => (
  <svg
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M5 15L12 8L19 15"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

// Immersive icon (from immersiver.svg)
const ImmersiveIcon = () => (
  <svg
    width="22"
    height="17"
    viewBox="0 0 22 17"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M10.6094 5.31463C11.7863 5.31463 12.7403 4.35489 12.7403 3.17099C12.7403 1.98709 11.7863 1.02734 10.6094 1.02734C9.43256 1.02734 8.47852 1.98709 8.47852 3.17099C8.47852 4.35489 9.43256 5.31463 10.6094 5.31463Z"
      stroke="currentColor"
      strokeWidth="0.7"
      strokeMiterlimit="10"
    />
    <path
      d="M19.4091 12.3959C20.3376 12.791 20.8703 13.2503 20.8703 13.7403C20.8703 15.1796 16.2767 16.3494 10.6084 16.3494C4.9402 16.3494 0.349609 15.1796 0.349609 13.7403C0.349609 13.1983 1.00106 12.693 2.11827 12.2734"
      stroke="currentColor"
      strokeWidth="0.7"
      strokeMiterlimit="10"
    />
    <path
      d="M19.7813 13.9779L20.0644 13.8646L19.5103 12.4621L20.9411 12.0088L20.8498 11.7148L19.1055 12.2722L19.7813 13.9779Z"
      fill="currentColor"
    />
    <path
      d="M17.7099 12.6719C17.7099 13.5232 14.5318 14.2153 10.6079 14.2153C6.68396 14.2153 3.50586 13.5232 3.50586 12.6719"
      stroke="currentColor"
      strokeWidth="0.7"
      strokeMiterlimit="10"
    />
    <path
      d="M6.59568 2.89553C4.72961 2.61686 3.50586 2.15138 3.50586 1.62159C3.50586 1.2388 4.15122 0.886629 5.21972 0.617143C5.62155 0.516085 6.08426 0.424215 6.59568 0.347656"
      stroke="currentColor"
      strokeWidth="0.7"
      strokeMiterlimit="10"
    />
    <path
      d="M14.623 0.347656C15.1345 0.424215 15.5972 0.516085 15.999 0.617143C17.0675 0.886629 17.7129 1.2388 17.7129 1.62159C17.7129 2.15138 16.4891 2.61686 14.623 2.89553"
      stroke="currentColor"
      strokeWidth="0.7"
      strokeMiterlimit="10"
    />
    <path
      d="M3.50586 12.6726V3.61719"
      stroke="currentColor"
      strokeWidth="0.7"
      strokeMiterlimit="10"
    />
    <path
      d="M17.7109 12.6726V3.61719"
      stroke="currentColor"
      strokeWidth="0.7"
      strokeMiterlimit="10"
    />
    <path
      d="M14.6229 12.6731V10.5417C14.6229 8.29085 12.8085 6.46875 10.5741 6.46875C8.33667 6.46875 6.52539 8.29391 6.52539 10.5417V12.6731"
      stroke="currentColor"
      strokeWidth="0.7"
      strokeMiterlimit="10"
    />
  </svg>
);

export default function GlassThemeButton({
  children,
  onClick,
  className = "",
  type = "button",
  theme = "light", // "light" | "dark" | "spec_light" | "spec_dark"
  icon = null, // "arrow" | "arrow-up" | "globe" | React element | null
  isCollapsed = false, // For expandable buttons (like immersive)
}) {
  const location = useLocation();
  const { openModal } = useImmersiveModal();

  const renderIcon = () => {
    if (!icon) return null;
    if (icon === "arrow") return <ArrowDownIcon />;
    if (icon === "arrow-up") return <ArrowUpIcon />;
    if (icon === "globe") return <ImmersiveIcon />;
    return icon;
  };

  const isImmersiveShowroomPage = location.pathname === ROUTES.IMMERSIVE_SHOWROOM;

  // Don't render globe button on Immersive Showroom page
  if (icon === "globe" && isImmersiveShowroomPage) {
    return null;
  }

  // Handle click - for globe icon, open immersive modal
  const handleClick = (e) => {
    if (icon === "globe") {
      openModal();
    } else if (onClick) {
      onClick(e);
    }
  };

  const isIconOnly = icon && !children;
  const isExpandable = icon && children && isCollapsed !== undefined;

  // Determine button variant class
  let variantClass = '';
  if (isIconOnly) {
    variantClass = `glass-theme-button--icon-only glass-theme-button--icon-${icon}`;
  } else if (icon && children) {
    variantClass = isCollapsed
      ? 'glass-theme-button--expandable glass-theme-button--collapsed'
      : 'glass-theme-button--expandable glass-theme-button--expanded';
  }

  return (
    <button
      type={type}
      className={`glass-theme-button glass-theme-button--${theme} ${variantClass} ${className}`}
      onClick={handleClick}
    >
      {children && <span className="glass-theme-btn-text">{children}</span>}
      {icon && <span className="glass-theme-btn-icon">{renderIcon()}</span>}
    </button>
  );
}
