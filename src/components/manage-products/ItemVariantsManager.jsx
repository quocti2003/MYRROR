import React, { useState, useEffect } from "react";
import api, { r2API } from "@services/api";

const ItemVariantsManager = () => {
  const [itemVariants, setItemVariants] = useState([]);
  const [items, setItems] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVariant, setEditingVariant] = useState(null);
  const [formData, setFormData] = useState({
    name: "",
    itemVariantUrl: "",
    description: "",
    isActive: true,
  });
  const [selectedFile, setSelectedFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);

  const [errorMessage, setErrorMessage] = useState(null);
  const [isErrorFadingOut, setIsErrorFadingOut] = useState(false);

  // Auto-hide error message after 5 seconds
  useEffect(() => {
    if (errorMessage) {
      setIsErrorFadingOut(false);
      const timer = setTimeout(() => {
        setIsErrorFadingOut(true);
        setTimeout(() => {
          setErrorMessage(null);
          setIsErrorFadingOut(false);
        }, 300);
      }, 5000);

      return () => clearTimeout(timer);
    }
  }, [errorMessage]);

  // Close error message manually
  const closeErrorMessage = () => {
    setIsErrorFadingOut(true);
    setTimeout(() => {
      setErrorMessage(null);
      setIsErrorFadingOut(false);
    }, 300);
  };

  // Handle file selection
  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedFile(file);
      // Create preview URL
      const reader = new FileReader();
      reader.onload = (e) => {
        setImagePreview(e.target.result);
      };
      reader.readAsDataURL(file);
    } else {
      setSelectedFile(null);
      setImagePreview(null);
    }
  };

  // Upload file to R2
  const uploadFile = async (file) => {
    const response = await r2API.upload(file, "item-variants");
    return response.data.publicUrl;
  };

  // API calls
  const fetchData = async () => {
    try {
      // Fetch items (for dropdown)
      const itemsResponse = await api.get("/api/item-variants");
      if (itemsResponse.status === 200) {
        const itemsData = itemsResponse.data;

        // Transform API data to match our component structure
        const transformedItems = itemsData.map((item) => ({
          id: item.itemId || item.id,
          name: item.itemName || item.name,
        }));
        setItems(transformedItems);
      } else {
        console.error("Failed to fetch items");
        setItems([
          { id: 1, name: "Diamond Ring" },
          { id: 2, name: "Gold Necklace" },
          { id: 3, name: "Silver Earrings" },
        ]);
      }

      // Fetch item variants
      const variantsResponse = await api.get("/api/item-variants");
      if (variantsResponse.status === 200) {
        const variantsData = variantsResponse.data;

        // Transform API data to match our component structure
        const transformedVariants = variantsData.map((item) => ({
          id: item.itemVariantId,
          name: item.itemVariantName || item.name || "",
          itemVariantUrl: item.itemVariantUrl || "",
          description: item.description || "",
          createdAt: item.createdAt,
          updatedAt: item.updatedAt,
          createdBy: item.createdBy,
          updatedBy: item.updatedBy,
          isActive: item.isActive,
        }));

        setItemVariants(transformedVariants);
      } else {
        console.error("Failed to fetch item variants");
        setItemVariants([
          {
            id: "ITEM0001",
            name: "Sample Item Variant 1",
            itemVariantUrl: "test",
            description: "URL model 1",
            createdAt: "2025-07-23T02:45:38.454355Z",
            updatedAt: "2025-07-23T02:45:38.454355Z",
            createdBy: "system",
            updatedBy: "system",
            isActive: true,
          },
          {
            id: "ITEM0002",
            name: "Sample Item Variant 2",
            itemVariantUrl: "test-2",
            description: "URL model 2",
            createdAt: "2025-07-23T02:45:38.454355Z",
            updatedAt: "2025-07-23T02:45:38.454355Z",
            createdBy: "system",
            updatedBy: "system",
            isActive: false,
          },
        ]);
      }
    } catch (error) {
      console.error("Error fetching data:", error);
      // Fallback to mock data
      setItems([
        { id: 1, name: "Diamond Ring" },
        { id: 2, name: "Gold Necklace" },
        { id: 3, name: "Silver Earrings" },
      ]);
      setItemVariants([
        {
          id: "ITEM0001",
          name: "Sample Item Variant 1",
          itemVariantUrl: "test",
          description: "URL model 1",
          createdAt: "2025-07-23T02:45:38.454355Z",
          updatedAt: "2025-07-23T02:45:38.454355Z",
          createdBy: "system",
          updatedBy: "system",
          isActive: true,
        },
        {
          id: "ITEM0002",
          name: "Sample Item Variant 2",
          itemVariantUrl: "test-2",
          description: "URL model 2",
          createdAt: "2025-07-23T02:45:38.454355Z",
          updatedAt: "2025-07-23T02:45:38.454355Z",
          createdBy: "system",
          updatedBy: "system",
          isActive: false,
        },
      ]);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage(null);

    try {
      let imageUrl = formData.itemVariantUrl;

      // Upload file if a new file is selected
      if (selectedFile) {
        imageUrl = await uploadFile(selectedFile);
      }
      if (editingVariant) {
        // Update existing variant
        const response = await api.put(
          `/api/item-variants/${editingVariant.id}`,
          {
            itemVariantName: formData.name,
            itemVariantUrl: imageUrl,
            description: formData.description,
            isActive: formData.isActive,
          }
        );

        if (response.status === 200) {
          const updatedVariant = response.data;
          setItemVariants(
            itemVariants.map((variant) =>
              variant.id === editingVariant.id
                ? {
                    ...updatedVariant,
                    id: updatedVariant.itemVariantId,
                    name: updatedVariant.itemVariantName || updatedVariant.name,
                  }
                : variant
            )
          );
        } else {
          console.error("Failed to update item variant:", response.data);
          setErrorMessage(
            response.data?.message || "Failed to update item variant"
          );
          return;
        }
      } else {
        // Add new variant
        const response = await api.post("/api/item-variants", {
          itemVariantName: formData.name,
          itemVariantUrl: imageUrl,
          description: formData.description,
          isActive: formData.isActive,
        });

        if (response.status === 201 || response.status === 200) {
          const newVariant = response.data;
          setItemVariants([
            ...itemVariants,
            {
              ...newVariant,
              id: newVariant.itemVariantId,
              name: newVariant.itemVariantName || newVariant.name,
            },
          ]);
        } else {
          console.error("Failed to create item variant:", response.data);
          setErrorMessage(
            response.data?.message || "Failed to create item variant"
          );
          return;
        }
      }

      closeModal();
    } catch (error) {
      console.error("Error submitting item variant:", error);
      setErrorMessage("A network error occurred. Please try again.");
    }
  };

  const handleEdit = (variant) => {
    setErrorMessage(null);
    setEditingVariant(variant);
    setFormData({
      name: variant.name,
      itemVariantUrl: variant.itemVariantUrl,
      description: variant.description,
      isActive: variant.isActive,
    });
    setSelectedFile(null);
    setImagePreview(null);
    setIsModalOpen(true);
  };

  const handleDelete = async (id) => {
    setErrorMessage(null);
    if (window.confirm("Are you sure you want to delete this item variant?")) {
      try {
        const response = await api.delete(`/api/item-variants/${id}`);

        if (response.status === 200 || response.status === 204) {
          setItemVariants(itemVariants.filter((variant) => variant.id !== id));
        } else {
          console.error("Failed to delete item variant:", response.data);
          setErrorMessage(
            response.data?.message || "Failed to delete item variant"
          );
        }
      } catch (error) {
        console.error("Error deleting item variant:", error);
        setErrorMessage("A network error occurred. Please try again.");
      }
    }
  };

  const openModal = () => {
    setErrorMessage(null);
    setEditingVariant(null);
    setFormData({
      name: "",
      itemVariantUrl: "",
      description: "",
      isActive: true,
    });
    setSelectedFile(null);
    setImagePreview(null);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingVariant(null);
    setFormData({
      name: "",
      itemVariantUrl: "",
      description: "",
      isActive: true,
    });
    setSelectedFile(null);
    setImagePreview(null);
  };

  return (
    <div className="manager-container">
      <div className="manager-header">
        <h2>Item Variants Management</h2>
        <button className="add-button" onClick={openModal}>
          Add New Variant
        </button>
      </div>

      {errorMessage && (
        <div
          className={`error-message-container ${
            isErrorFadingOut ? "fade-out" : ""
          }`}
        >
          <p>{errorMessage}</p>
          <button className="error-close-button" onClick={closeErrorMessage}>
            ×
          </button>
        </div>
      )}

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Image</th>
              <th>Description</th>
              <th>Created At</th>
              <th>Created By</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {itemVariants.length === 0 ? (
              <tr>
                <td
                  colSpan="8"
                  style={{ textAlign: "center", padding: "2rem" }}
                >
                  Loading item variants...
                </td>
              </tr>
            ) : (
              itemVariants.map((variant, index) => {
                return (
                  <tr key={`variant-${variant.id || index}`}>
                    <td>{variant.id}</td>
                    <td>{variant.name}</td>
                    <td>
                      {variant.itemVariantUrl && (
                        <img
                          src={variant.itemVariantUrl}
                          alt={variant.name || "Item variant"}
                          style={{
                            width: "60px",
                            height: "60px",
                            objectFit: "cover",
                            borderRadius: "4px",
                          }}
                        />
                      )}
                    </td>
                    <td>{variant.description}</td>
                    <td>{new Date(variant.createdAt).toLocaleDateString()}</td>
                    <td>{variant.createdBy}</td>
                    <td>
                      <span
                        className={`status-badge ${
                          variant.isActive ? "active" : "inactive"
                        }`}
                      >
                        {variant.isActive ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td>
                      <button
                        className="edit-button"
                        onClick={() => handleEdit(variant)}
                      >
                        Edit
                      </button>
                      <button
                        className="delete-button"
                        onClick={() => handleDelete(variant.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>
                {editingVariant ? "Edit Item Variant" : "Add New Item Variant"}
              </h3>
              <button className="close-button" onClick={closeModal}>
                ×
              </button>
            </div>

            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-group">
                <label htmlFor="name">Name:</label>
                <input
                  type="text"
                  id="name"
                  value={formData.name}
                  onChange={(e) =>
                    setFormData({ ...formData, name: e.target.value })
                  }
                  placeholder="e.g., Diamond Ring Variant 1"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="imageFile">Image:</label>
                <input
                  type="file"
                  id="imageFile"
                  accept="image/*"
                  onChange={handleFileChange}
                  required={!editingVariant}
                />
                {imagePreview && (
                  <div style={{ marginTop: "10px" }}>
                    <img
                      src={imagePreview}
                      alt="Preview"
                      style={{
                        maxWidth: "200px",
                        maxHeight: "200px",
                        objectFit: "cover",
                      }}
                    />
                  </div>
                )}
                {editingVariant && formData.itemVariantUrl && !imagePreview && (
                  <div style={{ marginTop: "10px" }}>
                    <img
                      src={formData.itemVariantUrl}
                      alt="Current image"
                      style={{
                        maxWidth: "200px",
                        maxHeight: "200px",
                        objectFit: "cover",
                      }}
                    />
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="description">Description:</label>
                <textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) =>
                    setFormData({ ...formData, description: e.target.value })
                  }
                  rows="3"
                  placeholder="e.g., URL model 1"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="isActive">Status:</label>
                <select
                  id="isActive"
                  value={formData.isActive.toString()}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      isActive: e.target.value === "true",
                    })
                  }
                >
                  <option value="true">Active</option>
                  <option value="false">Inactive</option>
                </select>
              </div>

              <div className="form-actions">
                <button
                  type="button"
                  onClick={closeModal}
                  className="cancel-button"
                >
                  Cancel
                </button>
                <button type="submit" className="submit-button">
                  {editingVariant ? "Update" : "Create"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ItemVariantsManager;
