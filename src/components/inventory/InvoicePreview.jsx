import React, { useRef, useCallback } from "react";
import { Printer, ArrowLeft } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import LuxuryInvoice from "./LuxuryInvoice";
import "./InvoicePreview.css";

const InvoicePreview = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const invoiceRef = useRef(null);

  // Get order data from navigation state (passed from CreateOrder page)
  const orderData = location.state?.orderData;

  // Default company info
  const companyInfo = {
    companyName: "CÔNG TY CỔ PHẦN MIRROR FUTURE DIAMOND",
    taxCode: "0318950980",
    companyAddress: "74 Nguyễn Cơ Thạch, Phường An Khánh, Thành phố Hồ Chí Minh",
    companyPhone: "0903657146",
    logoUrl: "/logo-mirror.png",
  };

  // Build invoice data from order
  const invoiceData = orderData ? {
    ...companyInfo,
    invoiceCode: orderData.invoiceCode || "2C25MYY",
    invoiceNumber: orderData.invoiceNumber || "<Chưa cấp số>",
    invoiceDate: orderData.invoiceDate || new Date(),
    taxAuthCode: orderData.taxAuthCode || "",
    qrCodeUrl: orderData.qrCodeUrl || "",

    customerCompany: orderData.customerCompany || "Khách lẻ không lấy hóa đơn",
    customerTaxCode: orderData.customerTaxCode || "",
    customerAddress: orderData.customerAddress || "",
    customerName: orderData.customerName || "Khách lẻ không lấy hóa đơn",
    customerPhone: orderData.customerPhone || "",
    customerIdNumber: orderData.customerIdNumber || "",
    paymentMethod: orderData.paymentMethod || "TM/CK",

    // Items from scanned products
    items: (orderData.items || []).map(item => ({
      name: item.name,
      unit: "cái",
      quantity: item.quantity,
      unitPrice: item.price,
    })),
  } : null;

  // Native print handler
  const handlePrint = useCallback(() => {
    const printContent = invoiceRef.current;
    if (!printContent) return;

    const printWindow = window.open("", "_blank", "width=800,height=600");
    if (!printWindow) {
      alert("Vui lòng cho phép popup để in hóa đơn");
      return;
    }

    const stylesheets = Array.from(document.styleSheets)
      .map((sheet) => {
        try {
          return Array.from(sheet.cssRules || [])
            .map((rule) => rule.cssText)
            .join("\n");
        } catch {
          if (sheet.href) {
            return `@import url("${sheet.href}");`;
          }
          return "";
        }
      })
      .join("\n");

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Invoice_${invoiceData?.invoiceCode || "preview"}</title>
          <link rel="preconnect" href="https://fonts.googleapis.com">
          <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
          <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;500;600;700&family=Cormorant+Garamond:ital,wght@0,400;0,500;0,600;1,400&family=Montserrat:wght@300;400;500;600&display=swap" rel="stylesheet">
          <style>
            ${stylesheets}
            @media print {
              body { margin: 0; padding: 0; }
              .luxury-invoice { box-shadow: none !important; margin: 0 !important; }
            }
            body {
              margin: 0;
              padding: 20px;
              background: #f5f5f5;
              display: flex;
              justify-content: center;
            }
          </style>
        </head>
        <body>
          ${printContent.outerHTML}
          <script>
            document.fonts.ready.then(() => {
              setTimeout(() => {
                window.print();
                window.close();
              }, 500);
            });
          </script>
        </body>
      </html>
    `);

    printWindow.document.close();
  }, [invoiceData?.invoiceCode]);

  // If no order data, show message
  if (!invoiceData) {
    return (
      <div className="invoice-preview-page">
        <div className="preview-toolbar">
          <button className="toolbar-btn back-btn" onClick={() => navigate(-1)}>
            <ArrowLeft size={20} />
            Quay lại
          </button>
          <div className="toolbar-title">
            <h1>Hóa đơn Luxury</h1>
          </div>
          <div className="toolbar-actions"></div>
        </div>

        <div className="no-order-message">
          <h2>Chưa có đơn hàng</h2>
          <p>Vui lòng tạo đơn hàng trước khi xem hóa đơn.</p>
          <button
            className="go-create-order-btn"
            onClick={() => navigate("/inventory/create-order")}
          >
            Đi tới Tạo đơn hàng
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="invoice-preview-page">
      {/* Toolbar */}
      <div className="preview-toolbar">
        <button className="toolbar-btn back-btn" onClick={() => navigate(-1)}>
          <ArrowLeft size={20} />
          Quay lại
        </button>
        <div className="toolbar-title">
          <h1>Hóa đơn Luxury</h1>
          {invoiceData.invoiceCode && (
            <span className="invoice-code">#{invoiceData.invoiceCode}</span>
          )}
        </div>
        <div className="toolbar-actions">
          <button className="toolbar-btn print-btn" onClick={handlePrint}>
            <Printer size={20} />
            In hóa đơn
          </button>
        </div>
      </div>

      {/* Invoice Preview Container */}
      <div className="preview-container">
        <div className="preview-paper">
          <LuxuryInvoice ref={invoiceRef} {...invoiceData} />
        </div>
      </div>

      {/* Footer info */}
      <div className="preview-footer">
        <p>Nhấn "In hóa đơn" để in hoặc lưu dưới dạng PDF</p>
      </div>
    </div>
  );
};

export default InvoicePreview;
