import { getImageUrl } from "@utils/cloudflareMediaUtil";
import PropTypes from "prop-types";

/**
 * MediaImage Component
 * Automatically uses Cloudflare CDN for images with fallback to public folder
 *
 * @example
 * <MediaImage src="home-page/banner.jpg" alt="Banner" />
 * // Automatically converts to: https://cloudflare-url/home-page/banner.jpg
 */
const MediaImage = ({ src, alt = "", className = "", loading = "eager", ...props }) => {
  // Generate Cloudflare URL or fallback to public folder
  const imageUrl = getImageUrl(src);

  return (
    <img
      src={imageUrl}
      alt={alt}
      className={className}
      loading={loading}
      decoding="async"
      {...props}
    />
  );
};

MediaImage.propTypes = {
  src: PropTypes.string.isRequired,
  alt: PropTypes.string,
  className: PropTypes.string,
  loading: PropTypes.oneOf(["eager", "lazy"]),
};

export default MediaImage;
