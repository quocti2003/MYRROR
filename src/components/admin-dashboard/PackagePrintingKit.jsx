import { useState, useEffect, useRef } from "react";
import { productsAPI, certificatesAPI, handleAPIError } from "@services/api";
import "./PackagePrintingKit.css";

const PackagePrintingKit = () => {
  const [products, setProducts] = useState([]);
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [activeTemplate, setActiveTemplate] = useState("certificate");
  const [issueDate, setIssueDate] = useState(
    new Date().toISOString().split("T")[0]
  );
  const [certNumber, setCertNumber] = useState("");
  const certificateRef = useRef(null);
  const thankYouCardRef = useRef(null);

  // Thank You Card state
  const [recipientName, setRecipientName] = useState("Valued Customer");
  const [greetingText, setGreetingText] = useState(
    "Thank you for choosing Mirror Diamond. Your trust means everything to us."
  );
  const [closingText, setClosingText] = useState(
    "We hope this piece brings you joy for years to come."
  );

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [productsRes, certificatesRes] = await Promise.all([
        productsAPI.getAllIncludingInactive(),
        certificatesAPI.getAll().catch(() => ({ data: [] })),
      ]);
      setProducts(productsRes.data || []);
      setCertificates(certificatesRes.data || []);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to load data");
      setError(errorInfo.message);
    } finally {
      setLoading(false);
    }
  };

  const filteredProducts = products.filter((product) => {
    const name = (product.name || "").toLowerCase();
    const sku = (product.skuCode || "").toLowerCase();
    const search = searchTerm.toLowerCase();
    return name.includes(search) || sku.includes(search);
  });

  const formatMetalType = (type) => {
    const types = {
      GOLD: "Gold",
      WHITE_GOLD: "White Gold",
      ROSE_GOLD: "Rose Gold",
      PLATINUM: "Platinum",
      SILVER: "Silver",
    };
    return types[type] || type || "-";
  };

  const formatDimensions = (dimensions) => {
    if (!dimensions) return "-";
    if (typeof dimensions === "string") return dimensions;
    if (typeof dimensions === "object") {
      const { width, height, depth } = dimensions;
      const parts = [];
      if (width) parts.push(`W: ${width}`);
      if (height) parts.push(`H: ${height}`);
      if (depth) parts.push(`D: ${depth}`);
      return parts.length > 0 ? parts.join(" × ") : "-";
    }
    return "-";
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "-";
    const date = new Date(dateStr);
    return date.toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  };

  const generateCertNumber = () => {
    const date = new Date();
    const year = date.getFullYear().toString().slice(-2);
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const random = Math.random().toString(36).substring(2, 6).toUpperCase();
    return `CERT-${year}${month}-${random}`;
  };

  const handleSelectProduct = (product) => {
    setSelectedProduct(product);
    setCertNumber(generateCertNumber());
  };

  const handlePrint = () => {
    const printContent =
      activeTemplate === "certificate"
        ? certificateRef.current
        : thankYouCardRef.current;

    if (!printContent) return;

    const printWindow = window.open("", "_blank");
    printWindow.document.write(`
      <html>
      <head>
        <title>Print - ${activeTemplate === "certificate" ? "MIRROR Certificate" : "Thank You Card"}</title>
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          body {
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            font-family: 'Georgia', 'Times New Roman', serif;
          }
          @media print {
            body { margin: 0; }
          }
          ${document.querySelector(".ppk-certificate")?.outerHTML ? getCertificateStyles() : ""}
          ${document.querySelector(".ppk-thank-you-card")?.outerHTML ? getThankYouCardStyles() : ""}
        </style>
      </head>
      <body>
        ${printContent.outerHTML}
      </body>
      </html>
    `);
    printWindow.document.close();
    setTimeout(() => printWindow.print(), 500);
  };

  const getCertificateStyles = () => `
    .ppk-certificate {
      width: 800px;
      min-height: 600px;
      background: linear-gradient(135deg, #fefefe 0%, #f8f8f8 100%);
      position: relative;
      padding: 50px;
    }
    .ppk-certificate-border {
      position: absolute;
      top: 15px;
      left: 15px;
      right: 15px;
      bottom: 15px;
      border: 2px solid #d4af37;
    }
    .ppk-certificate-border-inner {
      position: absolute;
      top: 20px;
      left: 20px;
      right: 20px;
      bottom: 20px;
      border: 1px solid #e8d5a3;
    }
    .ppk-certificate-header { text-align: center; margin-bottom: 30px; }
    .ppk-certificate-logo {
      font-family: 'Times New Roman', serif;
      font-size: 42px;
      font-weight: 400;
      letter-spacing: 8px;
      color: #1a1a1a;
      margin-bottom: 5px;
    }
    .ppk-certificate-tagline {
      font-size: 10px;
      letter-spacing: 3px;
      color: #888;
      text-transform: uppercase;
    }
    .ppk-certificate-title { text-align: center; margin: 25px 0; }
    .ppk-certificate-title h1 {
      font-size: 28px;
      font-weight: 400;
      color: #d4af37;
      letter-spacing: 4px;
      text-transform: uppercase;
    }
    .ppk-certificate-body { display: flex; gap: 40px; margin-top: 30px; }
    .ppk-certificate-image { width: 250px; flex-shrink: 0; }
    .ppk-certificate-image img {
      width: 100%;
      height: 250px;
      object-fit: cover;
      border: 1px solid #ddd;
    }
    .ppk-certificate-details { flex: 1; }
    .ppk-detail-row {
      display: flex;
      border-bottom: 1px solid #eee;
      padding: 10px 0;
    }
    .ppk-detail-label {
      width: 140px;
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: #888;
      font-weight: 600;
    }
    .ppk-detail-value { flex: 1; font-size: 14px; color: #333; }
    .ppk-certificate-footer {
      margin-top: 40px;
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      min-height: 80px;
    }
    .ppk-certificate-info { flex: 1; display: flex; align-items: center; gap: 15px; }
    .ppk-certificate-qr { width: 80px; height: 80px; }
    .ppk-certificate-qr img { width: 100%; height: 100%; }
    .ppk-certificate-codes { display: flex; flex-direction: column; gap: 8px; }
    .ppk-certificate-code { display: flex; flex-direction: column; gap: 2px; }
    .ppk-cert-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: #888; font-weight: 600; }
    .ppk-cert-value { font-size: 14px; color: #333; font-weight: 500; }
    .ppk-certificate-signature { text-align: right; margin-left: auto; flex-shrink: 0; }
    .ppk-signature-line { width: 180px; border-top: 1px solid #333; margin-bottom: 5px; }
    .ppk-signature-text { font-size: 11px; color: #666; }
    .ppk-certificate-date { text-align: center; font-size: 11px; color: #888; margin-top: 20px; }
    .ppk-certificate-id {
      font-family: monospace;
      font-size: 10px;
      color: #bbb;
      text-align: center;
      margin-top: 15px;
    }
  `;

  const getThankYouCardStyles = () => `
    .ppk-thank-you-card {
      position: relative;
      display: inline-block;
    }
    .ppk-ty-bg-image {
      display: block;
      width: 800px;
      height: auto;
    }
    .ppk-ty-content {
      position: absolute;
      top: 45%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
      width: 65%;
      padding: 30px 25px;
    }
    .ppk-ty-greeting { font-size: 18px; color: #444; line-height: 1.8; margin-bottom: 15px; }
    .ppk-ty-recipient {
      font-size: 28px;
      font-weight: 700;
      color: #d4af37;
      font-style: italic;
      min-height: 40px;
      border-bottom: 2px solid #d4af37;
      display: inline-block;
      padding: 0 20px 5px;
      margin: 10px 0 20px;
    }
    .ppk-ty-closing { font-size: 16px; color: #555; line-height: 1.6; }
    .ppk-ty-divider { width: 60px; height: 2px; background: #d4af37; margin: 25px auto; }
    .ppk-ty-care { font-size: 14px; font-weight: 600; color: #333; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 15px; }
    .ppk-ty-qr { width: 120px; height: 120px; margin: 0 auto; }
    .ppk-ty-qr img { width: 100%; height: 100%; }
  `;

  const getProductImage = () => {
    if (!selectedProduct) return null;
    return (
      selectedProduct.imageUrl ||
      (selectedProduct.imageUrls && selectedProduct.imageUrls[0])
    );
  };

  // Get certificate details from product's certificates
  const getCertificateDetails = () => {
    if (!selectedProduct) return null;

    // Check if product has certificateCodes and look up in certificates array
    if (selectedProduct.certificateCodes && selectedProduct.certificateCodes.length > 0) {
      const certDetails = [];
      for (const code of selectedProduct.certificateCodes) {
        const cert = certificates.find(c => c.certificateCode === code);
        if (cert) {
          certDetails.push({
            code: cert.certificateCode,
            type: cert.certificateType || cert.type || "Certificate",
            url: cert.certificateUrl || null,
          });
        } else {
          // If certificate not found in API, just show the code
          certDetails.push({
            code: code,
            type: "Certificate",
            url: null,
          });
        }
      }
      return certDetails.length > 0 ? certDetails : null;
    }

    return null;
  };

  const certificateDetails = getCertificateDetails();
  const certificateUrl = certificateDetails?.find(c => c.url)?.url || null;

  // QR for thank you card (static)
  const thankYouQrUrl =
    "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" +
    encodeURIComponent(
      `https://www.mirrorfuturediamond.com/f6a7b8c9-d0e1-2f3a-4b5c-6d7e8f9a0b1c`
    );

  // QR for certificate (from product's certificate URL)
  const certificateQrUrl = certificateUrl
    ? "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" +
      encodeURIComponent(certificateUrl)
    : null;

  return (
    <div className="ppk-container">
      {/* Left Panel - Controls */}
      <div className="ppk-controls">
        <div className="ppk-controls-header">
          <h3>Package Printing Kit</h3>
          <p>Generate certificates and thank you cards</p>
        </div>

        {error && <div className="ppk-error">{error}</div>}

        {/* Template Selection */}
        <div className="ppk-section">
          <label className="ppk-label">Template</label>
          <div className="ppk-template-tabs">
            <button
              className={`ppk-template-tab ${activeTemplate === "certificate" ? "active" : ""}`}
              onClick={() => setActiveTemplate("certificate")}
            >
              Certificate
            </button>
            <button
              className={`ppk-template-tab ${activeTemplate === "thankyou" ? "active" : ""}`}
              onClick={() => setActiveTemplate("thankyou")}
            >
              Thank You Card
            </button>
          </div>
        </div>

        {/* Certificate Options - Product Search & Selection */}
        {activeTemplate === "certificate" && (
          <>
            {/* Product Search */}
            <div className="ppk-section">
              <label className="ppk-label">Search Product</label>
              <input
                type="text"
                className="ppk-input"
                placeholder="Search by name or SKU..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>

            {/* Product List */}
            <div className="ppk-section">
              <label className="ppk-label">Select Product</label>
              <div className="ppk-product-list">
                {loading ? (
                  <div className="ppk-loading">Loading products...</div>
                ) : filteredProducts.length === 0 ? (
                  <div className="ppk-empty">No products found</div>
                ) : (
                  filteredProducts.slice(0, 50).map((product) => (
                    <div
                      key={product.id}
                      className={`ppk-product-item ${selectedProduct?.id === product.id ? "selected" : ""}`}
                      onClick={() => handleSelectProduct(product)}
                    >
                      <div className="ppk-product-name">
                        {product.name || "Unnamed Product"}
                      </div>
                      <div className="ppk-product-sku">
                        {product.skuCode || "N/A"}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Certificate Number & Date */}
            <div className="ppk-section">
              <label className="ppk-label">Certificate Number</label>
              <input
                type="text"
                className="ppk-input"
                placeholder="Auto-generated"
                value={certNumber}
                onChange={(e) => setCertNumber(e.target.value)}
              />
            </div>
            <div className="ppk-section">
              <label className="ppk-label">Issue Date</label>
              <input
                type="date"
                className="ppk-input"
                value={issueDate}
                onChange={(e) => setIssueDate(e.target.value)}
              />
            </div>
          </>
        )}

        {/* Thank You Card Options */}
        {activeTemplate === "thankyou" && (
          <>
            <div className="ppk-section">
              <label className="ppk-label">Recipient Name</label>
              <input
                type="text"
                className="ppk-input"
                value={recipientName}
                onChange={(e) => setRecipientName(e.target.value)}
              />
            </div>
            <div className="ppk-section">
              <label className="ppk-label">Greeting Message</label>
              <textarea
                className="ppk-textarea"
                value={greetingText}
                onChange={(e) => setGreetingText(e.target.value)}
              />
            </div>
            <div className="ppk-section">
              <label className="ppk-label">Closing Message</label>
              <textarea
                className="ppk-textarea"
                value={closingText}
                onChange={(e) => setClosingText(e.target.value)}
              />
            </div>
          </>
        )}

        {/* Action Buttons */}
        <div className="ppk-actions">
          <button className="ppk-btn ppk-btn-primary" onClick={handlePrint}>
            Print {activeTemplate === "certificate" ? "Certificate" : "Card"}
          </button>
        </div>
      </div>

      {/* Right Panel - Preview */}
      <div className="ppk-preview">
        <div className="ppk-preview-header">
          <h4>Preview</h4>
        </div>
        <div className="ppk-preview-content">
          {activeTemplate === "certificate" ? (
            <div className="ppk-certificate" ref={certificateRef}>
              <div className="ppk-certificate-border"></div>
              <div className="ppk-certificate-border-inner"></div>

              <div className="ppk-certificate-header">
                <div className="ppk-certificate-logo">MIRROR</div>
                <div className="ppk-certificate-tagline">
                  Created by Science, Crafted for Eternity
                </div>
              </div>

              <div className="ppk-certificate-title">
                <h1>Certificate of Authenticity</h1>
              </div>

              {!selectedProduct ? (
                <div className="ppk-no-product">
                  <h3>No Product Selected</h3>
                  <p>Please select a product from the list</p>
                </div>
              ) : (
                <>
                  <div className="ppk-certificate-body">
                    <div className="ppk-certificate-image">
                      {getProductImage() ? (
                        <img src={getProductImage()} alt={selectedProduct.name} />
                      ) : (
                        <div className="ppk-image-placeholder">No Image</div>
                      )}
                    </div>

                    <div className="ppk-certificate-details">
                      <div className="ppk-detail-row">
                        <div className="ppk-detail-label">Product Name</div>
                        <div className="ppk-detail-value">
                          {selectedProduct.name || "-"}
                        </div>
                      </div>
                      <div className="ppk-detail-row">
                        <div className="ppk-detail-label">SKU</div>
                        <div className="ppk-detail-value">
                          {selectedProduct.skuCode || "-"}
                        </div>
                      </div>
                      <div className="ppk-detail-row">
                        <div className="ppk-detail-label">Metal Type</div>
                        <div className="ppk-detail-value">
                          {formatMetalType(selectedProduct.metalType)}
                        </div>
                      </div>
                      <div className="ppk-detail-row">
                        <div className="ppk-detail-label">Metal Purity</div>
                        <div className="ppk-detail-value">
                          {selectedProduct.metalPurity || "-"}
                        </div>
                      </div>
                      <div className="ppk-detail-row">
                        <div className="ppk-detail-label">Stone Type</div>
                        <div className="ppk-detail-value">
                          {selectedProduct.stoneType || "-"}
                        </div>
                      </div>
                      <div className="ppk-detail-row">
                        <div className="ppk-detail-label">Weight</div>
                        <div className="ppk-detail-value">
                          {selectedProduct.weightGrams
                            ? `${selectedProduct.weightGrams}g`
                            : "-"}
                        </div>
                      </div>
                      <div className="ppk-detail-row">
                        <div className="ppk-detail-label">Dimensions</div>
                        <div className="ppk-detail-value">
                          {formatDimensions(selectedProduct.dimensions)}
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="ppk-certificate-footer">
                    {certificateDetails && certificateDetails.length > 0 && (
                      <div className="ppk-certificate-info">
                        {certificateQrUrl && (
                          <div className="ppk-certificate-qr">
                            <img src={certificateQrUrl} alt="QR Code" />
                          </div>
                        )}
                        <div className="ppk-certificate-codes">
                          {certificateDetails.map((cert, index) => (
                            <div className="ppk-certificate-code" key={index}>
                              <div className="ppk-cert-label">{cert.type}</div>
                              <div className="ppk-cert-value">{cert.code}</div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                    <div className="ppk-certificate-signature">
                      <div className="ppk-signature-line"></div>
                      <div className="ppk-signature-text">
                        Authorized Signature
                      </div>
                    </div>
                  </div>

                  <div className="ppk-certificate-date">
                    Issue Date: {formatDate(issueDate)}
                  </div>
                  <div className="ppk-certificate-id">
                    {certNumber || generateCertNumber()}
                  </div>
                </>
              )}
            </div>
          ) : (
            <div className="ppk-thank-you-card" ref={thankYouCardRef}>
              <img
                src="/ty_card/thank_you_card_design -01.png"
                alt=""
                className="ppk-ty-bg-image"
              />
              <div className="ppk-ty-content">
                <div className="ppk-ty-greeting">{greetingText}</div>
                <div className="ppk-ty-recipient">{recipientName}</div>
                <div className="ppk-ty-closing">{closingText}</div>
                <div className="ppk-ty-divider"></div>
                <div className="ppk-ty-care">Care Instruction Guide</div>
                <div className="ppk-ty-qr">
                  <img src={thankYouQrUrl} alt="QR Code" />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default PackagePrintingKit;
