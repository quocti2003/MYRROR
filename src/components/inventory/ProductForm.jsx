import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { inventoryProductsAPI } from "@/services/inventoryApi";
import { ROUTES, getInventoryProductDetailRoute } from "@/constants/routes";
import {
  Save,
  X,
  Package,
  Copy,
  CheckCircle,
  AlertCircle,
  ArrowLeft,
  Info,
} from "lucide-react";
import "./ProductForm.css";

const METAL_TYPES = ["Gold", "Silver", "Platinum", "White Gold", "Rose Gold"];
const METAL_PURITIES = ["10K", "14K", "18K", "22K", "24K", "925", "950"];
const STONE_TYPES = ["Diamond", "Ruby", "Sapphire", "Emerald", "Pearl", "None"];
const STATUS_OPTIONS = [
  { value: "available", label: "Con hang" },
  { value: "hold", label: "Dang giu" },
  { value: "warranty", label: "Bao hanh" },
  { value: "sold", label: "Da ban" },
];
const CURRENCY_OPTIONS = ["VND", "USD"];

const initialFormData = {
  name: "",
  sku: "",
  description: "",
  price: "",
  currency: "VND",
  metalType: "",
  metalPurity: "",
  stoneType: "",
  weightGrams: "",
  dimensions: "",
  status: "available",
  imageUrl: "",
};

const ProductForm = ({ isEdit = false }) => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [formData, setFormData] = useState(initialFormData);
  const [originalData, setOriginalData] = useState(null);
  const [originalSku, setOriginalSku] = useState(""); // Lưu SKU gốc để gọi API update
  const [loading, setLoading] = useState(false);
  const [fetchLoading, setFetchLoading] = useState(isEdit);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (isEdit && id) {
      fetchProduct();
    }
  }, [isEdit, id]);

  const fetchProduct = async () => {
    try {
      setFetchLoading(true);
      const response = await inventoryProductsAPI.getById(id);
      const product = response.data?.data || response.data;

      if (product) {
        const data = {
          name: product.name || "",
          sku: product.sku || "",
          description: product.description || "",
          price: product.price?.toString() || "",
          currency: product.currency || "VND",
          metalType: product.metalType || "",
          metalPurity: product.metalPurity || "",
          stoneType: product.stoneType || "",
          weightGrams: product.weightGrams?.toString() || "",
          dimensions: product.dimensions || "",
          status: product.status || "available",
          imageUrl: product.imageUrl || "",
        };
        setFormData(data);
        setOriginalData(data);
        setOriginalSku(product.sku); // Lưu SKU gốc
      }
    } catch (err) {
      console.error("Error fetching product:", err);
      setError("Khong the tai thong tin san pham");
    } finally {
      setFetchLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError("");
  };

  const validateForm = () => {
    if (!formData.name.trim()) {
      setError("Vui long nhap ten san pham");
      return false;
    }
    if (!formData.sku.trim()) {
      setError("Vui long nhap ma SKU");
      return false;
    }
    if (!formData.price || parseFloat(formData.price) <= 0) {
      setError("Vui long nhap gia hop le");
      return false;
    }
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) return;

    try {
      setLoading(true);
      setError("");

      // Chuẩn bị data theo format backend yêu cầu (camelCase)
      const submitData = {
        name: formData.name,
        description: formData.description || null,
        price: parseFloat(formData.price),
        currency: formData.currency,
        metalType: formData.metalType || null,
        metalPurity: formData.metalPurity || null,
        stoneType: formData.stoneType || null,
        weightGrams: formData.weightGrams ? parseFloat(formData.weightGrams) : null,
        dimensions: formData.dimensions || null,
        status: formData.status,
        imageUrl: formData.imageUrl || null,
      };

      if (isEdit) {
        // Gọi API update theo SKU
        await inventoryProductsAPI.updateBySku(originalSku, submitData);
        setSuccess("Cap nhat san pham thanh cong!");
        setOriginalData(formData);
      } else {
        // API tạo mới đang bị disable do foreign key constraint
        setError("Chuc nang them san pham tam thoi bi vo hieu hoa. Vui long them san pham qua backend.");
        return;
      }
    } catch (err) {
      console.error("Submit error:", err);
      const errorMessage = err.response?.data?.message || err.response?.data?.error;
      setError(errorMessage || "Co loi xay ra khi luu san pham");
    } finally {
      setLoading(false);
    }
  };

  const handleCopyForPrint = () => {
    const printData = {
      sku: formData.sku,
      name: formData.name,
      price: formData.price,
      currency: formData.currency,
    };
    navigator.clipboard.writeText(JSON.stringify(printData, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const hasChanges = () => {
    if (!originalData) return false;
    return JSON.stringify(formData) !== JSON.stringify(originalData);
  };

  if (fetchLoading) {
    return (
      <div className="product-form-page">
        <div className="form-loading">Dang tai...</div>
      </div>
    );
  }

  return (
    <div className="product-form-page">
      <div className="form-header">
        <button
          className="back-btn"
          onClick={() => navigate(-1)}
        >
          <ArrowLeft size={20} />
          Quay lai
        </button>
        <h1>{isEdit ? "Sua san pham" : "Them san pham moi"}</h1>
      </div>

      {/* Thông báo cho mode Add */}
      {!isEdit && (
        <div className="form-info">
          <Info size={20} />
          <span>
            Chuc nang them san pham tam thoi bi vo hieu hoa do rang buoc co so du lieu.
            Vui long them san pham truc tiep qua backend hoac database.
          </span>
        </div>
      )}

      {error && (
        <div className="form-alert error">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="form-alert success">
          <CheckCircle size={20} />
          <span>{success}</span>
        </div>
      )}

      <div className="form-container">
        <form onSubmit={handleSubmit} className="product-form">
          {/* Basic Info */}
          <section className="form-section">
            <h2>Thong tin co ban</h2>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="name">Ten san pham *</label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleChange}
                  placeholder="Nhap ten san pham"
                  disabled={!isEdit}
                />
              </div>

              <div className="form-group">
                <label htmlFor="sku">Ma SKU *</label>
                <input
                  type="text"
                  id="sku"
                  name="sku"
                  value={formData.sku}
                  onChange={handleChange}
                  placeholder="Nhap ma SKU"
                  disabled={true} // SKU không được sửa
                />
                {isEdit && (
                  <small className="field-hint">SKU khong the thay doi</small>
                )}
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="description">Mo ta</label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Mo ta san pham"
                rows={3}
                disabled={!isEdit}
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="price">Gia *</label>
                <input
                  type="number"
                  id="price"
                  name="price"
                  value={formData.price}
                  onChange={handleChange}
                  placeholder="0"
                  min="0"
                  step="1000"
                  disabled={!isEdit}
                />
              </div>

              <div className="form-group">
                <label htmlFor="currency">Don vi tien</label>
                <select
                  id="currency"
                  name="currency"
                  value={formData.currency}
                  onChange={handleChange}
                  disabled={!isEdit}
                >
                  {CURRENCY_OPTIONS.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="status">Trang thai</label>
                <select
                  id="status"
                  name="status"
                  value={formData.status}
                  onChange={handleChange}
                  disabled={!isEdit}
                >
                  {STATUS_OPTIONS.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </section>

          {/* Material Info */}
          <section className="form-section">
            <h2>Thong tin chat lieu</h2>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="metalType">Loai kim loai</label>
                <select
                  id="metalType"
                  name="metalType"
                  value={formData.metalType}
                  onChange={handleChange}
                  disabled={!isEdit}
                >
                  <option value="">Chon loai kim loai</option>
                  {METAL_TYPES.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="metalPurity">Do tinh khiet</label>
                <select
                  id="metalPurity"
                  name="metalPurity"
                  value={formData.metalPurity}
                  onChange={handleChange}
                  disabled={!isEdit}
                >
                  <option value="">Chon do tinh khiet</option>
                  {METAL_PURITIES.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="stoneType">Loai da</label>
                <select
                  id="stoneType"
                  name="stoneType"
                  value={formData.stoneType}
                  onChange={handleChange}
                  disabled={!isEdit}
                >
                  <option value="">Chon loai da</option>
                  {STONE_TYPES.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="weightGrams">Trong luong (gram)</label>
                <input
                  type="number"
                  id="weightGrams"
                  name="weightGrams"
                  value={formData.weightGrams}
                  onChange={handleChange}
                  placeholder="0.00"
                  min="0"
                  step="0.01"
                  disabled={!isEdit}
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="dimensions">Kich thuoc</label>
              <input
                type="text"
                id="dimensions"
                name="dimensions"
                value={formData.dimensions}
                onChange={handleChange}
                placeholder="VD: 10mm x 15mm"
                disabled={!isEdit}
              />
            </div>
          </section>

          {/* Image */}
          <section className="form-section">
            <h2>Hinh anh</h2>

            <div className="form-group">
              <label htmlFor="imageUrl">URL hinh anh</label>
              <input
                type="url"
                id="imageUrl"
                name="imageUrl"
                value={formData.imageUrl}
                onChange={handleChange}
                placeholder="https://..."
                disabled={!isEdit}
              />
            </div>

            {formData.imageUrl && (
              <div className="image-preview">
                <img src={formData.imageUrl} alt="Preview" />
              </div>
            )}
          </section>

          {/* Form Actions */}
          <div className="form-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => navigate(-1)}
            >
              <X size={20} />
              Huy
            </button>

            {formData.sku && (
              <button
                type="button"
                className="btn-secondary"
                onClick={handleCopyForPrint}
              >
                {copied ? <CheckCircle size={20} /> : <Copy size={20} />}
                {copied ? "Da copy!" : "Copy de in"}
              </button>
            )}

            {isEdit && (
              <button
                type="submit"
                className="btn-primary"
                disabled={loading || !hasChanges()}
              >
                <Save size={20} />
                {loading ? "Dang luu..." : "Cap nhat"}
              </button>
            )}
          </div>
        </form>

        {/* QR Preview Sidebar */}
        <aside className="form-sidebar">
          <div className="qr-preview-card">
            <h3>Preview Label</h3>
            <div className="qr-preview-content">
              {formData.sku ? (
                <>
                  <div className="qr-code-placeholder">
                    <Package size={64} />
                    <span>QR: {formData.sku}</span>
                  </div>
                  <div className="label-preview">
                    <p className="label-name">{formData.name || "Ten san pham"}</p>
                    <p className="label-sku">{formData.sku}</p>
                    <p className="label-price">
                      {formData.price
                        ? new Intl.NumberFormat("vi-VN", {
                            style: "currency",
                            currency: formData.currency || "VND",
                          }).format(formData.price)
                        : "0 VND"}
                    </p>
                  </div>
                </>
              ) : (
                <p className="no-preview">Nhap ma SKU de xem preview</p>
              )}
            </div>
          </div>

          {/* Changes comparison for edit mode */}
          {isEdit && originalData && hasChanges() && (
            <div className="changes-card">
              <h3>Thay doi</h3>
              <div className="changes-list">
                {Object.keys(formData).map((key) => {
                  if (formData[key] !== originalData[key]) {
                    return (
                      <div key={key} className="change-item">
                        <span className="change-field">{key}:</span>
                        <span className="change-old">{originalData[key] || "(trong)"}</span>
                        <span className="change-arrow">→</span>
                        <span className="change-new">{formData[key] || "(trong)"}</span>
                      </div>
                    );
                  }
                  return null;
                })}
              </div>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
};

export default ProductForm;
