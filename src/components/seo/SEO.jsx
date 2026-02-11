import { Helmet } from "react-helmet-async";

const DEFAULT_TITLE = "MIRROR FUTURE DIAMOND";
const DEFAULT_DESCRIPTION =
  "Awakening Luxury through Your Senses. Premium lab-grown diamond jewelry with innovative AR try-on experience.";
const DEFAULT_IMAGE = "https://www.mirrorfuturediamond.com/favicon-512.png";
const SITE_URL = "https://www.mirrorfuturediamond.com";

export default function SEO({
  title,
  description = DEFAULT_DESCRIPTION,
  image = DEFAULT_IMAGE,
  url,
  type = "website",
  noindex = false,
}) {
  const fullTitle = title ? `${DEFAULT_TITLE} | ${title}` : DEFAULT_TITLE;
  const canonicalUrl = url ? `${SITE_URL}${url}` : SITE_URL;

  return (
    <Helmet>
      <title>{fullTitle}</title>
      <meta name="description" content={description} />
      <link rel="canonical" href={canonicalUrl} />
      {noindex && <meta name="robots" content="noindex, nofollow" />}

      {/* Open Graph */}
      <meta property="og:type" content={type} />
      <meta property="og:title" content={fullTitle} />
      <meta property="og:description" content={description} />
      <meta property="og:image" content={image} />
      <meta property="og:url" content={canonicalUrl} />
      <meta property="og:site_name" content={DEFAULT_TITLE} />

      {/* Twitter Card */}
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={fullTitle} />
      <meta name="twitter:description" content={description} />
      <meta name="twitter:image" content={image} />
    </Helmet>
  );
}
