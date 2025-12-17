import { useState, useEffect } from "react";
import {
  productsAPI,
  categoriesAPI,
  vendorsAPI,
  r2API,
  certificatesAPI,
  handleAPIError,
} from "@services/api";

const CERTIFICATE_TYPES = [
  { value: "IGI", label: "IGI - International Gemological Institute" },
];

const ProductsManager = () => {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [vendors, setVendors] = useState([]);
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [uploading, setUploading] = useState(false);
  const [imageFiles, setImageFiles] = useState([]); // Array of {file, preview, uploaded, url}
  const [existingImages, setExistingImages] = useState([]); // Existing image URLs from server

  // Certificate modal state
  const [isCertificateModalOpen, setIsCertificateModalOpen] = useState(false);
  const [certificateFormData, setCertificateFormData] = useState({
    certificateCode: "",
    certificateType: "IGI",
    certificateUrl: "",
  });
  const [certificateError, setCertificateError] = useState(null);

  const [formData, setFormData] = useState({
    name: "",
    description: "",
    categoryId: "",
    vendorId: "",
    sku: "",
    price: "",
    currency: "VND",
    metalType: "GOLD",
    metalPurity: "",
    stoneType: "",
    weightGrams: "",
    dimensions: "",
    imageUrl: "",
    imageUrls: "",
    tags: "",
    status: "ACTIVE",
    featured: false,
    stockQuantity: "",
    minStockLevel: "",
    certificateCodes: [],
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [productsRes, categoriesRes, vendorsRes, certificatesRes] = await Promise.all([
        productsAPI.getAllIncludingInactive(),
        categoriesAPI.getAll(),
        vendorsAPI.getAll().catch(() => ({ data: [] })), // Fallback if vendors API fails
        certificatesAPI.getAll().catch(() => ({ data: [] })), // Fallback if certificates API fails
      ]);

      setProducts(productsRes.data || []);
      setCategories(categoriesRes.data || []);
      setVendors(vendorsRes.data || []);
      setCertificates(certificatesRes.data || []);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to load data");
      setError(errorInfo.message);
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFormData({
      name: "",
      description: "",
      categoryId: "",
      vendorId: "",
      sku: "",
      price: "",
      currency: "VND",
      metalType: "GOLD",
      metalPurity: "",
      stoneType: "",
      weightGrams: "",
      dimensions: "",
      imageUrl: "",
      imageUrls: "",
      tags: "",
      status: "ACTIVE",
      featured: false,
      stockQuantity: "",
      minStockLevel: "",
      certificateCodes: [],
    });
    setEditingProduct(null);
    // Cleanup preview URLs to prevent memory leak
    imageFiles.forEach(img => {
      if (img.preview) {
        URL.revokeObjectURL(img.preview);
      }
    });
    setImageFiles([]);
    setExistingImages([]);
  };

  // Multiple file upload functions
  const handleMultipleFileSelect = (event) => {
    const files = Array.from(event.target.files);
    if (files.length === 0) return;

    const validFiles = [];
    for (const file of files) {
      // Check file type
      if (!file.type.startsWith("image/")) {
        setError(`${file.name} is not an image file`);
        continue;
      }
      // Check file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        setError(`${file.name} is too large (max 5MB)`);
        continue;
      }

      // Create preview URL
      const preview = URL.createObjectURL(file);
      validFiles.push({
        file,
        preview,
        uploaded: false,
        url: null
      });
    }

    if (validFiles.length > 0) {
      setImageFiles(prev => [...prev, ...validFiles]);
      setError(null);
    }
  };

  const uploadAllImages = async () => {
    const uploadPromises = imageFiles
      .filter(img => !img.uploaded && img.file)
      .map(async (img) => {
        try {
          const response = await r2API.upload(img.file, "products");
          const publicUrl = response.data.publicUrl;
          if (publicUrl) {
            img.uploaded = true;
            img.url = publicUrl;
            return publicUrl;
          }
          throw new Error("No public URL returned");
        } catch (err) {
          console.error(`Failed to upload ${img.file.name}:`, err);
          throw err;
        }
      });

    const uploadedUrls = await Promise.all(uploadPromises);
    return uploadedUrls;
  };

  const removeImage = (index, isExisting = false) => {
    if (isExisting) {
      setExistingImages(prev => prev.filter((_, i) => i !== index));
    } else {
      setImageFiles(prev => {
        const removed = prev[index];
        if (removed.preview) {
          URL.revokeObjectURL(removed.preview);
        }
        return prev.filter((_, i) => i !== index);
      });
    }
  };

  const moveImage = (index, direction, isExisting = false) => {
    if (isExisting) {
      setExistingImages(prev => {
        const newArr = [...prev];
        const newIndex = direction === 'up' ? index - 1 : index + 1;
        if (newIndex < 0 || newIndex >= newArr.length) return prev;
        [newArr[index], newArr[newIndex]] = [newArr[newIndex], newArr[index]];
        return newArr;
      });
    } else {
      setImageFiles(prev => {
        const newArr = [...prev];
        const newIndex = direction === 'up' ? index - 1 : index + 1;
        if (newIndex < 0 || newIndex >= newArr.length) return prev;
        [newArr[index], newArr[newIndex]] = [newArr[newIndex], newArr[index]];
        return newArr;
      });
    }
  };

  const handleAdd = () => {
    resetForm();
    setIsModalOpen(true);
  };

  const handleEdit = (product) => {
    setFormData({
      name: product.name || "",
      description: product.description || "",
      categoryId: product.categoryId || "",
      vendorId: product.vendor?.id || "",
      sku: product.skuCode || "",
      price: product.price?.toString() || "",
      currency: product.currency || "VND",
      metalType: product.metalType || "GOLD",
      metalPurity: product.metalPurity || "",
      stoneType: product.stoneType || "",
      weightGrams: product.weightGrams?.toString() || "",
      dimensions:
        typeof product.dimensions === "object"
          ? Object.entries(product.dimensions)
              .map(([key, value]) => `${key}: ${value}`)
              .join(", ")
          : product.dimensions || "",
      imageUrl: product.imageUrl || "",
      imageUrls: Array.isArray(product.imageUrls)
        ? product.imageUrls.join(", ")
        : "",
      tags: Array.isArray(product.tags) ? product.tags.join(", ") : "",
      status: product.status || "ACTIVE",
      featured: product.featured || false,
      stockQuantity: product.stockQuantity?.toString() || "",
      minStockLevel: product.minStockLevel?.toString() || "",
      certificateCodes: product.certificateCodes || (product.certificateCode ? [product.certificateCode] : []),
    });

    // Populate existing images
    const existingUrls = [];
    if (product.imageUrl && !product.imageUrl.includes('example.com')) {
      existingUrls.push(product.imageUrl);
    }
    if (Array.isArray(product.imageUrls)) {
      product.imageUrls.forEach(url => {
        if (url && !url.includes('example.com') && !existingUrls.includes(url)) {
          existingUrls.push(url);
        }
      });
    }
    setExistingImages(existingUrls);

    setEditingProduct(product);
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setUploading(true);

    try {
      // Upload all new images first
      let newUploadedUrls = [];
      if (imageFiles.length > 0) {
        newUploadedUrls = await uploadAllImages();
      }

      // Combine existing images and newly uploaded images
      const allImageUrls = [
        ...existingImages,
        ...newUploadedUrls.filter(url => url)
      ];

      // First image becomes imageUrl, rest go to imageUrls
      const imageUrl = allImageUrls[0] || "";
      const imageUrls = allImageUrls.length > 0 ? allImageUrls : [];

      const submitData = {
        ...formData,
        skuId: formData.sku, // Map 'sku' form field to 'skuId' backend field
        imageUrl: imageUrl,
        imageUrls: imageUrls,
        vendorId: formData.vendorId || null,
        price: parseFloat(formData.price) || 0,
        weightGrams: formData.weightGrams
          ? parseFloat(formData.weightGrams)
          : null,
        stockQuantity: parseInt(formData.stockQuantity) || 0,
        minStockLevel: parseInt(formData.minStockLevel) || 1,
        tags: formData.tags
          ? formData.tags
              .split(",")
              .map((tag) => tag.trim())
              .filter((tag) => tag)
          : [],
        dimensions: formData.dimensions
          ? (() => {
              // Parse text to JSON object
              // "Diameter: 17mm, Band width: 2.5mm" -> {diameter: "17mm", bandWidth: "2.5mm"}
              const dimensionsObj = {};
              const pairs = formData.dimensions.split(",");
              pairs.forEach((pair) => {
                const [key, value] = pair.split(":").map((s) => s.trim());
                if (key && value) {
                  // Convert key to camelCase
                  const camelKey = key
                    .toLowerCase()
                    .replace(/\s+(.)/g, (_, letter) => letter.toUpperCase());
                  dimensionsObj[camelKey] = value;
                }
              });
              return JSON.stringify(dimensionsObj);
            })()
          : null,
      };

      // Remove the 'sku' field since we're using 'skuId'
      delete submitData.sku;

      if (editingProduct) {
        await productsAPI.update(editingProduct.id, submitData);
      } else {
        await productsAPI.create(submitData);
      }

      await fetchData();
      setIsModalOpen(false);
      resetForm();
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to save product");
      setError(errorInfo.message);
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this product?")) {
      return;
    }

    try {
      await productsAPI.delete(id);
      await fetchData();
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to delete product");
      setError(errorInfo.message);
    }
  };

  const toggleFeatured = async (product) => {
    try {
      await productsAPI.toggleFeatured(product.id);
      await fetchData();
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to update featured status");
      setError(errorInfo.message);
    }
  };

  const filteredProducts = products.filter((product) => {
    const matchesSearch =
      product.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (product.skuCode || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory =
      selectedCategory === "all" || product.categoryId === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const formatPrice = (price, currency) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: currency || "VND",
    }).format(price || 0);
  };

  if (loading) {
    return <div className="admin-empty-state">Loading products...</div>;
  }

  return (
    <div className="products-manager">
      {error && (
        <div className="admin-error-state" style={{ marginBottom: "1.5rem" }}>
          {error}
        </div>
      )}

      {/* Header Controls */}
      <div className="admin-card" style={{ padding: "1.5rem", marginBottom: "1.5rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem", flexWrap: "wrap" }}>
          <div style={{ display: "flex", gap: "1rem", flex: 1, minWidth: "300px" }}>
            <input
              type="text"
              placeholder="Search products..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="admin-input"
              style={{ flex: 1 }}
            />
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="admin-select"
              style={{ width: "200px" }}
            >
              <option value="all">All Categories</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {typeof category.name === 'string'
                    ? category.name
                    : (typeof category.categoryName === 'string'
                        ? category.categoryName
                        : "Unnamed Category")}
                </option>
              ))}
            </select>
          </div>
          <button onClick={handleAdd} className="admin-button admin-button-primary">
            Add Product
          </button>
        </div>
      </div>

      {/* Products Table */}
      <div className="admin-card">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>SKU</th>
              <th>Category</th>
              <th>Vendor</th>
              <th>Price</th>
              <th>Stock</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.map((product) => (
              <tr key={product.id}>
                <td>
                  <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
                    {product.imageUrl && (
                      <img
                        src={product.imageUrl}
                        alt={product.name}
                        style={{ width: "40px", height: "40px", borderRadius: "4px", objectFit: "cover" }}
                      />
                    )}
                    <div>
                      <div style={{ fontWeight: "500", color: "#0f172a" }}>{product.name}</div>
                      {product.featured && (
                        <span style={{ fontSize: "0.75rem", color: "#64748b", fontWeight: "400" }}>
                          Featured
                        </span>
                      )}
                    </div>
                  </div>
                </td>
                <td>
                  <code
                    style={{
                      background: "#f1f5f9",
                      padding: "2px 8px",
                      borderRadius: "4px",
                    }}
                  >
                    {product.skuCode || "N/A"}
                  </code>
                </td>
                <td>
                  {product.categoryName || "No Category"}
                </td>
                <td>
                  {product.vendor ? (
                    <div>
                      <div style={{ fontWeight: "500", color: "#0066cc" }}>
                        {typeof product.vendor === 'string'
                          ? product.vendor
                          : (product.vendor.name || product.vendor.vendorName || "Unknown Vendor")}
                      </div>
                      {typeof product.vendor === 'object' && product.vendor.code && (
                        <div style={{ fontSize: "12px", color: "#666" }}>
                          {typeof product.vendor.code === 'string' ? product.vendor.code : String(product.vendor.code || '')}
                          {product.vendor.country && ` • ${typeof product.vendor.country === 'string' ? product.vendor.country : String(product.vendor.country || '')}`}
                        </div>
                      )}
                    </div>
                  ) : (
                    <span style={{ color: "#94a3b8", fontStyle: "italic" }}>
                      No Vendor
                    </span>
                  )}
                </td>
                <td>{formatPrice(product.price, product.currency)}</td>
                <td>
                  <span
                    style={{
                      color:
                        product.stockQuantity <= product.minStockLevel
                          ? "#dc2626"
                          : "#059669",
                      fontWeight: "500",
                    }}
                  >
                    {product.stockQuantity || 0}
                  </span>
                  {product.stockQuantity <= product.minStockLevel && (
                    <span
                      style={{
                        fontSize: "0.75rem",
                        color: "#dc2626",
                        marginLeft: "4px",
                      }}
                    >
                      Low
                    </span>
                  )}
                </td>
                <td>
                  <span
                    className="status-pill"
                    style={{
                      backgroundColor:
                        product.status === "ACTIVE" ? "#ecfdf5" : "#fef2f2",
                      color:
                        product.status === "ACTIVE" ? "#059669" : "#dc2626",
                      borderColor:
                        product.status === "ACTIVE" ? "#059669" : "#dc2626",
                    }}
                  >
                    {product.status || "ACTIVE"}
                  </span>
                </td>
                <td>
                  <div style={{ display: "flex", gap: "0.5rem" }}>
                    <button
                      onClick={() => handleEdit(product)}
                      className="admin-button admin-button-outline"
                      style={{ padding: "0.375rem 0.75rem", fontSize: "0.8125rem" }}
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(product.id)}
                      className="admin-button admin-button-danger"
                      style={{ padding: "0.375rem 0.75rem", fontSize: "0.8125rem" }}
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {filteredProducts.length === 0 && (
          <div className="admin-empty-state">
            No products found.{" "}
            {searchTerm || selectedCategory !== "all"
              ? "Try adjusting your filters."
              : "Add your first product to get started."}
          </div>
        )}
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div
          className="admin-modal-overlay"
          onClick={() => setIsModalOpen(false)}
        >
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ padding: "2rem" }}>
              <h2
                style={{
                  margin: "0 0 1.5rem 0",
                  fontSize: "24px",
                  fontWeight: "600",
                }}
              >
                {editingProduct ? "Edit Product" : "Add New Product"}
              </h2>

              <form onSubmit={handleSubmit}>
                <div style={{ display: "grid", gap: "1rem" }}>
                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "1rem",
                    }}
                  >
                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        Name *
                      </label>
                      <input
                        type="text"
                        value={formData.name}
                        onChange={(e) =>
                          setFormData({ ...formData, name: e.target.value })
                        }
                        className="admin-input"
                        required
                      />
                    </div>
                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        SKU *
                      </label>
                      <input
                        type="text"
                        value={formData.sku}
                        onChange={(e) =>
                          setFormData({ ...formData, sku: e.target.value })
                        }
                        className="admin-input"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label
                      style={{
                        display: "block",
                        marginBottom: "0.5rem",
                        fontWeight: "500",
                      }}
                    >
                      Description
                    </label>
                    <textarea
                      value={formData.description}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          description: e.target.value,
                        })
                      }
                      className="admin-input"
                      rows="3"
                    />
                  </div>

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "1rem",
                    }}
                  >
                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        Category *
                      </label>
                      <select
                        value={formData.categoryId}
                        onChange={(e) =>
                          setFormData({ ...formData, categoryId: e.target.value })
                        }
                        className="admin-select"
                        required
                      >
                        <option value="">Select Category</option>
                        {categories.map((category) => (
                          <option key={category.id} value={category.id}>
                            {typeof category.name === 'string'
                              ? category.name
                              : (typeof category.categoryName === 'string'
                                  ? category.categoryName
                                  : "Unnamed Category")}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        Vendor
                      </label>
                      <select
                        value={formData.vendorId}
                        onChange={(e) =>
                          setFormData({ ...formData, vendorId: e.target.value })
                        }
                        className="admin-select"
                      >
                        <option value="">None (No Vendor)</option>
                        {vendors.map((vendor) => (
                          <option key={vendor.id} value={vendor.id}>
                            {vendor.name} ({vendor.code}) - {vendor.country}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div>
                    <label
                      style={{
                        display: "block",
                        marginBottom: "0.5rem",
                        fontWeight: "500",
                      }}
                    >
                      Price *
                    </label>
                    <input
                      type="number"
                      value={formData.price}
                      onChange={(e) =>
                        setFormData({ ...formData, price: e.target.value })
                      }
                      className="admin-input"
                      required
                      min="0"
                      step="0.01"
                    />
                  </div>

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr 1fr",
                      gap: "1rem",
                    }}
                  >
                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        Metal Type
                      </label>
                      <select
                        value={formData.metalType}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            metalType: e.target.value,
                          })
                        }
                        className="admin-select"
                      >
                        <option value="GOLD">Gold</option>
                        <option value="SILVER">Silver</option>
                        <option value="PLATINUM">Platinum</option>
                        <option value="PALLADIUM">Palladium</option>
                        <option value="TITANIUM">Titanium</option>
                        <option value="STAINLESS_STEEL">Stainless Steel</option>
                        <option value="MIXED">Mixed Metals</option>
                      </select>
                    </div>
                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        Metal Purity
                      </label>
                      <input
                        type="text"
                        value={formData.metalPurity}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            metalPurity: e.target.value,
                          })
                        }
                        className="admin-input"
                        placeholder="e.g., 18K, 14K"
                      />
                    </div>
                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        Weight (g)
                      </label>
                      <input
                        type="number"
                        value={formData.weightGrams}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            weightGrams: e.target.value,
                          })
                        }
                        className="admin-input"
                        min="0"
                        step="0.1"
                      />
                    </div>
                  </div>

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "1rem",
                    }}
                  >
                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        Stock Quantity
                      </label>
                      <input
                        type="number"
                        value={formData.stockQuantity}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            stockQuantity: e.target.value,
                          })
                        }
                        className="admin-input"
                        min="0"
                      />
                    </div>
                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        Min Stock Level
                      </label>
                      <input
                        type="number"
                        value={formData.minStockLevel}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            minStockLevel: e.target.value,
                          })
                        }
                        className="admin-input"
                        min="0"
                      />
                    </div>
                  </div>

                  {/* Included Certificates */}
                  <div>
                    <label
                      style={{
                        display: "block",
                        marginBottom: "0.5rem",
                        fontWeight: "500",
                      }}
                    >
                      Included Certificates
                    </label>

                    {/* Added Certificates List */}
                    {formData.certificateCodes.length > 0 && (
                      <div style={{ marginBottom: "0.75rem" }}>
                        {formData.certificateCodes.map((code) => {
                          const cert = certificates.find(c => c.certificateCode === code);
                          return (
                            <div
                              key={code}
                              style={{
                                display: "inline-flex",
                                alignItems: "center",
                                gap: "0.5rem",
                                padding: "0.375rem 0.75rem",
                                backgroundColor: "#dbeafe",
                                border: "1px solid #3b82f6",
                                borderRadius: "9999px",
                                marginRight: "0.5rem",
                                marginBottom: "0.5rem",
                                fontSize: "0.8125rem",
                              }}
                            >
                              <span style={{ fontWeight: "500", color: "#1d4ed8" }}>
                                {cert?.certificateType || "IGI"}: {code}
                              </span>
                              <button
                                type="button"
                                onClick={() => {
                                  setFormData({
                                    ...formData,
                                    certificateCodes: formData.certificateCodes.filter(c => c !== code),
                                  });
                                }}
                                style={{
                                  background: "none",
                                  border: "none",
                                  cursor: "pointer",
                                  padding: "0",
                                  color: "#dc2626",
                                  fontWeight: "bold",
                                  fontSize: "1rem",
                                  lineHeight: "1",
                                }}
                              >
                                ×
                              </button>
                            </div>
                          );
                        })}
                      </div>
                    )}

                    {/* Add Certificate Button */}
                    <button
                      type="button"
                      onClick={() => setIsCertificateModalOpen(true)}
                      className="admin-btn admin-btn-secondary"
                    >
                      + Add Certificate
                    </button>
                    <div style={{ fontSize: "0.75rem", color: "#64748b", marginTop: "0.25rem" }}>
                      Add diamond certificates (IGI, GIA, etc.) for this product
                    </div>
                  </div>

                  <div>
                    <label
                      style={{
                        display: "block",
                        marginBottom: "0.5rem",
                        fontWeight: "500",
                      }}
                    >
                      Product Images {uploading && <span style={{ color: "#ffc107" }}>⏳ Uploading...</span>}
                    </label>

                    {/* File Upload Input */}
                    <input
                      type="file"
                      accept="image/*"
                      multiple
                      onChange={handleMultipleFileSelect}
                      disabled={uploading}
                    />
                    <div className="image-upload-help">
                      Select multiple images (max 5MB each). First image will be the main product image.
                    </div>

                    {/* Image Gallery */}
                    {(existingImages.length > 0 || imageFiles.length > 0) && (
                      <div className="image-gallery-grid">
                        {/* Existing Images */}
                        {existingImages.map((url, index) => (
                          <div key={`existing-${index}`} className={`image-gallery-item ${index === 0 ? 'main-image' : ''}`}>
                            {index === 0 && (
                              <div className="image-badge main">
                                MAIN
                              </div>
                            )}
                            <img
                              src={url}
                              alt={`Product ${index + 1}`}
                            />
                            <div className="image-controls">
                              {index > 0 && (
                                <button
                                  type="button"
                                  onClick={() => moveImage(index, 'up', true)}
                                  title="Move up"
                                >
                                  ↑
                                </button>
                              )}
                              {index < existingImages.length + imageFiles.length - 1 && (
                                <button
                                  type="button"
                                  onClick={() => moveImage(index, 'down', true)}
                                  title="Move down"
                                >
                                  ↓
                                </button>
                              )}
                              <button
                                type="button"
                                onClick={() => removeImage(index, true)}
                                className="delete"
                                title="Delete image"
                              >
                                ✕
                              </button>
                            </div>
                          </div>
                        ))}

                        {/* New Image Files */}
                        {imageFiles.map((img, index) => {
                          const actualIndex = existingImages.length + index;
                          return (
                            <div key={`new-${index}`} className={`image-gallery-item ${actualIndex === 0 ? 'main-image' : ''}`}>
                              {actualIndex === 0 && (
                                <div className="image-badge main">
                                  MAIN
                                </div>
                              )}
                              <div className="image-badge new">
                                NEW
                              </div>
                              <img
                                src={img.preview}
                                alt={`New ${index + 1}`}
                              />
                              <div className="image-controls">
                                {actualIndex > 0 && (
                                  <button
                                    type="button"
                                    onClick={() => moveImage(index, 'up', false)}
                                    title="Move up"
                                  >
                                    ↑
                                  </button>
                                )}
                                {actualIndex < existingImages.length + imageFiles.length - 1 && (
                                  <button
                                    type="button"
                                    onClick={() => moveImage(index, 'down', false)}
                                    title="Move down"
                                  >
                                    ↓
                                  </button>
                                )}
                                <button
                                  type="button"
                                  onClick={() => removeImage(index, false)}
                                  className="delete"
                                  title="Delete image"
                                >
                                  ✕
                                </button>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>

                  <div>
                    <label
                      style={{
                        display: "block",
                        marginBottom: "0.5rem",
                        fontWeight: "500",
                      }}
                    >
                      Dimensions
                    </label>
                    <input
                      type="text"
                      value={formData.dimensions}
                      onChange={(e) =>
                        setFormData({ ...formData, dimensions: e.target.value })
                      }
                      className="admin-input"
                      placeholder="e.g., 20mm x 15mm x 10mm or Diameter: 17mm, Band width: 2.5mm"
                    />
                  </div>

                  <div>
                    <label
                      style={{
                        display: "block",
                        marginBottom: "0.5rem",
                        fontWeight: "500",
                      }}
                    >
                      Tags (comma-separated)
                    </label>
                    <input
                      type="text"
                      value={formData.tags}
                      onChange={(e) =>
                        setFormData({ ...formData, tags: e.target.value })
                      }
                      className="admin-input"
                      placeholder="luxury, engagement, diamond"
                    />
                  </div>

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "1rem",
                    }}
                  >
                    <div>
                      <label
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "500",
                        }}
                      >
                        Status
                      </label>
                      <select
                        value={formData.status}
                        onChange={(e) =>
                          setFormData({ ...formData, status: e.target.value })
                        }
                        className="admin-select"
                      >
                        <option value="ACTIVE">Active</option>
                        <option value="DISCONTINUED">Discontinued</option>
                        <option value="OUT_OF_STOCK">Out of Stock</option>
                      </select>
                    </div>
                    <div style={{ display: "flex", alignItems: "flex-end" }}>
                      <label
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                          cursor: "pointer",
                        }}
                      >
                        <input
                          type="checkbox"
                          checked={formData.featured}
                          onChange={(e) =>
                            setFormData({
                              ...formData,
                              featured: e.target.checked,
                            })
                          }
                        />
                        <span style={{ fontWeight: "500" }}>
                          Featured Product
                        </span>
                      </label>
                    </div>
                  </div>
                </div>

                <div
                  style={{
                    display: "flex",
                    gap: "1rem",
                    marginTop: "2rem",
                    justifyContent: "flex-end",
                  }}
                >
                  <button
                    type="button"
                    onClick={() => setIsModalOpen(false)}
                    className="admin-button admin-button-secondary"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="admin-button admin-button-primary"
                  >
                    {editingProduct ? "Update Product" : "Create Product"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Certificate Modal */}
      {isCertificateModalOpen && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0, 0, 0, 0.6)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 10000,
          }}
          onClick={() => {
            setIsCertificateModalOpen(false);
            setCertificateError(null);
            setCertificateFormData({
              certificateCode: "",
              certificateType: "IGI",
              certificateUrl: "",
            });
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              backgroundColor: "white",
              borderRadius: "8px",
              padding: "1.5rem",
              width: "100%",
              maxWidth: "450px",
              boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1)",
            }}
          >
            <h3
              style={{
                margin: "0 0 1.5rem 0",
                fontSize: "1.125rem",
                fontWeight: "600",
                color: "#0f172a",
              }}
            >
              Add New Certificate
            </h3>

            {certificateError && (
              <div
                style={{
                  padding: "0.75rem",
                  marginBottom: "1rem",
                  backgroundColor: "#fef2f2",
                  border: "1px solid #fecaca",
                  borderRadius: "6px",
                  color: "#dc2626",
                  fontSize: "0.875rem",
                }}
              >
                {certificateError}
              </div>
            )}

            <form
              onSubmit={async (e) => {
                e.preventDefault();
                setCertificateError(null);

                if (!certificateFormData.certificateCode.trim()) {
                  setCertificateError("Certificate code is required");
                  return;
                }

                try {
                  const response = await certificatesAPI.create({
                    certificateCode: certificateFormData.certificateCode.trim(),
                    certificateType: certificateFormData.certificateType,
                    certificateUrl: certificateFormData.certificateUrl.trim() || null,
                  });

                  // Add the new certificate to local state
                  const newCert = response.data;
                  setCertificates([...certificates, newCert]);

                  // Auto-select the new certificate
                  setFormData({
                    ...formData,
                    certificateCodes: [...formData.certificateCodes, newCert.certificateCode],
                  });

                  // Close modal and reset form
                  setIsCertificateModalOpen(false);
                  setCertificateFormData({
                    certificateCode: "",
                    certificateType: "IGI",
                    certificateUrl: "",
                  });
                } catch (err) {
                  const errorInfo = handleAPIError(err, "Failed to create certificate");
                  setCertificateError(errorInfo.message);
                }
              }}
            >
              {/* Certificate Code */}
              <div style={{ marginBottom: "1rem" }}>
                <label
                  style={{
                    display: "block",
                    marginBottom: "0.5rem",
                    fontSize: "0.875rem",
                    fontWeight: "500",
                    color: "#374151",
                  }}
                >
                  Certificate Code <span style={{ color: "#dc2626" }}>*</span>
                </label>
                <input
                  type="text"
                  value={certificateFormData.certificateCode}
                  onChange={(e) =>
                    setCertificateFormData({
                      ...certificateFormData,
                      certificateCode: e.target.value,
                    })
                  }
                  className="admin-input"
                  placeholder="e.g., 123456789"
                  style={{ width: "100%" }}
                  required
                />
              </div>

              {/* Certificate Type */}
              <div style={{ marginBottom: "1rem" }}>
                <label
                  style={{
                    display: "block",
                    marginBottom: "0.5rem",
                    fontSize: "0.875rem",
                    fontWeight: "500",
                    color: "#374151",
                  }}
                >
                  Certificate Type <span style={{ color: "#dc2626" }}>*</span>
                </label>
                <select
                  value={certificateFormData.certificateType}
                  onChange={(e) =>
                    setCertificateFormData({
                      ...certificateFormData,
                      certificateType: e.target.value,
                    })
                  }
                  className="admin-input"
                  style={{ width: "100%" }}
                  required
                >
                  {CERTIFICATE_TYPES.map((type) => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Certificate URL */}
              <div style={{ marginBottom: "1.5rem" }}>
                <label
                  style={{
                    display: "block",
                    marginBottom: "0.5rem",
                    fontSize: "0.875rem",
                    fontWeight: "500",
                    color: "#374151",
                  }}
                >
                  Certificate URL
                </label>
                <input
                  type="url"
                  value={certificateFormData.certificateUrl}
                  onChange={(e) =>
                    setCertificateFormData({
                      ...certificateFormData,
                      certificateUrl: e.target.value,
                    })
                  }
                  className="admin-input"
                  placeholder="https://..."
                  style={{ width: "100%" }}
                />
                <p
                  style={{
                    margin: "0.25rem 0 0 0",
                    fontSize: "0.75rem",
                    color: "#64748b",
                  }}
                >
                  Link to the certificate document or verification page
                </p>
              </div>

              {/* Actions */}
              <div
                style={{
                  display: "flex",
                  gap: "0.75rem",
                  justifyContent: "flex-end",
                }}
              >
                <button
                  type="button"
                  className="admin-btn admin-btn-secondary"
                  onClick={() => {
                    setIsCertificateModalOpen(false);
                    setCertificateError(null);
                    setCertificateFormData({
                      certificateCode: "",
                      certificateType: "IGI",
                      certificateUrl: "",
                    });
                  }}
                >
                  Cancel
                </button>
                <button type="submit" className="admin-btn admin-btn-primary">
                  Create & Add
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductsManager;
