import React, { forwardRef } from "react";
import "./LuxuryInvoice.css";

// Convert number to Vietnamese words
const numberToVietnameseWords = (num) => {
  if (num === 0) return "Không đồng";

  const ones = ["", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"];
  const tens = ["", "mười", "hai mươi", "ba mươi", "bốn mươi", "năm mươi", "sáu mươi", "bảy mươi", "tám mươi", "chín mươi"];

  const readThreeDigits = (n) => {
    let result = "";
    const hundred = Math.floor(n / 100);
    const ten = Math.floor((n % 100) / 10);
    const one = n % 10;

    if (hundred > 0) {
      result += ones[hundred] + " trăm ";
    }
    if (ten > 1) {
      result += tens[ten] + " ";
      if (one === 1) result += "mốt ";
      else if (one === 5) result += "lăm ";
      else if (one > 0) result += ones[one] + " ";
    } else if (ten === 1) {
      result += "mười ";
      if (one === 1) result += "một ";
      else if (one === 5) result += "lăm ";
      else if (one > 0) result += ones[one] + " ";
    } else if (one > 0) {
      if (hundred > 0) result += "lẻ ";
      result += ones[one] + " ";
    }
    return result.trim();
  };

  const units = ["", "nghìn", "triệu", "tỷ"];
  let result = "";
  let unitIndex = 0;

  while (num > 0) {
    const threeDigits = num % 1000;
    if (threeDigits > 0) {
      const words = readThreeDigits(threeDigits);
      result = words + " " + units[unitIndex] + " " + result;
    }
    num = Math.floor(num / 1000);
    unitIndex++;
  }

  result = result.trim();
  // Capitalize first letter
  result = result.charAt(0).toUpperCase() + result.slice(1) + " đồng";

  // Check if it's exact (chẵn)
  if (num % 1000 === 0) {
    result += " chẵn";
  }

  return result;
};

const LuxuryInvoice = forwardRef(({
  // Company info
  companyName = "CÔNG TY CỔ PHẦN MIRROR FUTURE DIAMOND",
  taxCode = "0318950980",
  companyAddress = "74 Nguyễn Cơ Thạch, Phường An Khánh, Thành phố Hồ Chí Minh",
  companyPhone = "0903657146",
  logoUrl = "/logo-mirror.png",

  // Invoice info
  invoiceCode = "2C25MYY",
  invoiceNumber = "<Chưa cấp số>",
  invoiceDate = new Date(),
  taxAuthCode = "", // Mã CQT
  qrCodeUrl = "",

  // Customer info
  customerCompany = "Khách lẻ không lấy hóa đơn",
  customerTaxCode = "",
  customerAddress = "",
  customerName = "Khách lẻ không lấy hóa đơn",
  customerPhone = "",
  customerIdNumber = "", // CCCD
  paymentMethod = "TM/CK",

  // Items
  items = [],

  // Currency (reserved for future use)
  // eslint-disable-next-line no-unused-vars
  currency = "VND",
}, ref) => {

  // Format date
  const formatDate = (date) => {
    const d = new Date(date);
    return `Ngày ${d.getDate()} tháng ${d.getMonth() + 1} năm ${d.getFullYear()}`;
  };

  // Format currency
  const formatCurrency = (value) => {
    return new Intl.NumberFormat("vi-VN").format(value);
  };

  // Calculate total
  const totalAmount = items.reduce((sum, item) => {
    return sum + (item.unitPrice || 0) * (item.quantity || 0);
  }, 0);

  // Amount in words
  const amountInWords = numberToVietnameseWords(totalAmount);

  return (
    <div className="luxury-invoice" ref={ref}>
      {/* Decorative top border */}
      <div className="invoice-top-border">
        <div className="border-line"></div>
        <div className="border-diamond"></div>
        <div className="border-line"></div>
      </div>

      {/* Header */}
      <header className="invoice-header">
        <div className="company-info">
          <div className="logo-section">
            <img src={logoUrl} alt="Logo" className="company-logo" onError={(e) => {
              e.target.style.display = 'none';
            }} />
            <div className="logo-text">MIRROR</div>
          </div>
          <div className="company-details">
            <h1 className="company-name">{companyName}</h1>
            <p className="company-meta">
              <span className="label">Mã số thuế:</span> {taxCode}
            </p>
            <p className="company-meta">
              <span className="label">Địa chỉ:</span> {companyAddress}
            </p>
            <p className="company-meta">
              <span className="label">Điện thoại:</span> {companyPhone}
            </p>
          </div>
        </div>
      </header>

      {/* Invoice Title */}
      <div className="invoice-title-section">
        <div className="title-left">
          <h2 className="invoice-title">HÓA ĐƠN BÁN HÀNG</h2>
          <p className="invoice-date">{formatDate(invoiceDate)}</p>
          {taxAuthCode && <p className="tax-auth-code">Mã CQT: {taxAuthCode}</p>}
        </div>
        <div className="title-right">
          <div className="invoice-codes">
            <p><span className="label">Ký hiệu:</span> <span className="code-value">{invoiceCode}</span></p>
            <p><span className="label">Số:</span> <span className="code-value">{invoiceNumber}</span></p>
          </div>
          {qrCodeUrl && (
            <div className="qr-code">
              <img src={qrCodeUrl} alt="QR Code" />
            </div>
          )}
        </div>
      </div>

      {/* Decorative divider */}
      <div className="section-divider">
        <div className="divider-line"></div>
        <div className="divider-ornament"></div>
        <div className="divider-line"></div>
      </div>

      {/* Customer Info */}
      <section className="customer-section">
        <div className="customer-grid">
          <div className="customer-row">
            <span className="field-label">Tên đơn vị:</span>
            <span className="field-value">{customerCompany}</span>
          </div>
          <div className="customer-row">
            <span className="field-label">MST/CCCD chủ hộ:</span>
            <span className="field-value">{customerTaxCode || "—"}</span>
          </div>
          <div className="customer-row full-width">
            <span className="field-label">Địa chỉ:</span>
            <span className="field-value">{customerAddress || "—"}</span>
          </div>
          <div className="customer-row">
            <span className="field-label">Họ và tên người mua hàng:</span>
            <span className="field-value">{customerName}</span>
          </div>
          <div className="customer-row half">
            <span className="field-label">Điện thoại:</span>
            <span className="field-value">{customerPhone || "—"}</span>
          </div>
          <div className="customer-row half">
            <span className="field-label">CCCD người mua:</span>
            <span className="field-value">{customerIdNumber || "—"}</span>
          </div>
          <div className="customer-row">
            <span className="field-label">Hình thức thanh toán:</span>
            <span className="field-value payment-method">{paymentMethod}</span>
          </div>
        </div>
      </section>

      {/* Items Table */}
      <section className="items-section">
        <table className="items-table">
          <thead>
            <tr>
              <th className="col-stt">STT</th>
              <th className="col-name">Tên hàng hóa, dịch vụ</th>
              <th className="col-unit">Đơn vị tính</th>
              <th className="col-qty">Số lượng</th>
              <th className="col-price">Đơn giá</th>
              <th className="col-total">Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <tr key={index}>
                <td className="col-stt">{index + 1}</td>
                <td className="col-name">{item.name}</td>
                <td className="col-unit">{item.unit || "cái"}</td>
                <td className="col-qty">{item.quantity}</td>
                <td className="col-price">{item.unitPrice ? formatCurrency(item.unitPrice) : ""}</td>
                <td className="col-total">
                  {item.unitPrice && item.quantity
                    ? formatCurrency(item.unitPrice * item.quantity)
                    : ""}
                </td>
              </tr>
            ))}
            {/* Empty rows for visual consistency */}
            {Array.from({ length: Math.max(0, 6 - items.length) }).map((_, i) => (
              <tr key={`empty-${i}`} className="empty-row">
                <td className="col-stt"></td>
                <td className="col-name"></td>
                <td className="col-unit"></td>
                <td className="col-qty"></td>
                <td className="col-price"></td>
                <td className="col-total"></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {/* Total Section */}
      <section className="total-section">
        <div className="total-row">
          <span className="total-label">Cộng tiền bán hàng hóa, dịch vụ:</span>
          <span className="total-value">{formatCurrency(totalAmount)}</span>
        </div>
        <div className="total-words">
          <span className="words-label">Số tiền viết bằng chữ:</span>
          <span className="words-value">{amountInWords}.</span>
        </div>
      </section>

      {/* Signature Section */}
      <section className="signature-section">
        <div className="signature-box">
          <h4 className="signature-title">Người mua hàng</h4>
          <p className="signature-note">(Chữ ký số (nếu có))</p>
          <div className="signature-space"></div>
        </div>
        <div className="signature-box">
          <h4 className="signature-title">Người bán hàng</h4>
          <p className="signature-note">(Chữ ký điện tử, Chữ ký số)</p>
          <div className="signature-space"></div>
        </div>
      </section>

      {/* Footer */}
      <footer className="invoice-footer">
        <p className="footer-note">Cảm ơn Quý khách đã tin tưởng Mirror Future Diamond</p>
        <div className="footer-brand">
          <span className="brand-text">MIRROR</span>
          <span className="brand-tagline">Future of Diamonds</span>
        </div>
      </footer>

      {/* Decorative bottom border */}
      <div className="invoice-bottom-border">
        <div className="border-line"></div>
        <div className="border-diamond"></div>
        <div className="border-line"></div>
      </div>
    </div>
  );
});

LuxuryInvoice.displayName = "LuxuryInvoice";

export default LuxuryInvoice;
