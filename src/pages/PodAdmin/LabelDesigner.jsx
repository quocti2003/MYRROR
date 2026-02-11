import { useState, useEffect, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { labelTemplateApi } from "@/services/labelApi";
import { ROUTES } from "@/constants/routes";
import "@/components/pod-admin/PodAdminLayout.css";
import "./LabelDesigner.css";

const LABEL_PRESETS = [
  { name: "40x30mm (Jewelry)", width: 40, height: 30, dpi: 300 },
  { name: "50x30mm (Standard)", width: 50, height: 30, dpi: 300 },
  { name: "60x40mm (Medium)", width: 60, height: 40, dpi: 300 },
  { name: "80x50mm (Large)", width: 80, height: 50, dpi: 300 },
];

const DATA_BINDINGS = [
  { value: "name", label: "Product Name" },
  { value: "sku", label: "SKU Code" },
  { value: "barcode", label: "Barcode" },
  { value: "price", label: "Price" },
  { value: "description", label: "Description" },
  { value: "category", label: "Category" },
  { value: "qrUrl", label: "QR URL" },
  { value: "id", label: "Product ID" },
];

export default function LabelDesigner() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [canvasElement, setCanvasElement] = useState(null);
  const fabricCanvasRef = useRef(null);
  const fabricRef = useRef(null);

  const [loading, setLoading] = useState(false);
  const [fabricLoaded, setFabricLoaded] = useState(false);
  const [canvasReady, setCanvasReady] = useState(false);

  // Callback ref to capture canvas element
  const canvasRef = useCallback((node) => {
    if (node !== null) {
      setCanvasElement(node);
    }
  }, []);
  const [saving, setSaving] = useState(false);
  const [template, setTemplate] = useState({
    name: "",
    description: "",
    labelWidth: 50,
    labelHeight: 30,
    dpi: 300,
    isDefault: false,
    canvasJson: null,
  });
  const [selectedObject, setSelectedObject] = useState(null);
  const [activeTab, setActiveTab] = useState("elements");

  // Calculate canvas dimensions based on label size and DPI
  const getCanvasDimensions = useCallback(() => {
    const mmToPixels = (mm, dpi) => Math.round((mm / 25.4) * dpi);
    const scale = 1; // Scale factor for preview
    return {
      width: mmToPixels(template.labelWidth, template.dpi) * scale,
      height: mmToPixels(template.labelHeight, template.dpi) * scale,
    };
  }, [template.labelWidth, template.labelHeight, template.dpi]);

  // Load Fabric.js dynamically
  useEffect(() => {
    if (typeof window === 'undefined') return;

    import('fabric').then(module => {
      console.log('Fabric module loaded:', module);
      // Fabric.js v7+ exports differently
      if (module.Canvas) {
        // v7+ direct export
        fabricRef.current = module;
      } else if (module.fabric) {
        // v5/v6 style
        fabricRef.current = module.fabric;
      } else if (module.default?.Canvas) {
        fabricRef.current = module.default;
      } else if (module.default?.fabric) {
        fabricRef.current = module.default.fabric;
      } else {
        fabricRef.current = module.default || module;
      }
      console.log('Fabric ref set to:', fabricRef.current);
      setFabricLoaded(true);
    }).catch((err) => {
      console.error('Failed to load Fabric.js:', err);
      setLoading(false);
    });
  }, []);

  // Initialize Fabric.js canvas after fabric is loaded and canvas element is mounted
  useEffect(() => {
    console.log('Canvas init effect - fabricLoaded:', fabricLoaded, 'canvasElement:', !!canvasElement, 'fabricRef:', !!fabricRef.current);
    if (!canvasElement || !fabricLoaded || !fabricRef.current) return;
    if (fabricCanvasRef.current) return; // Already initialized

    const fabric = fabricRef.current;
    console.log('Initializing canvas with fabric:', fabric);

    try {
      const dims = getCanvasDimensions();
      // Fabric v7 uses Canvas directly
      const CanvasClass = fabric.Canvas;
      console.log('Canvas class:', CanvasClass);

      const canvas = new CanvasClass(canvasElement, {
        width: dims.width,
        height: dims.height,
        backgroundColor: "#ffffff",
        selection: true,
      });

      fabricCanvasRef.current = canvas;
      setCanvasReady(true);
      console.log('Canvas created successfully');

      // Selection events
      canvas.on("selection:created", (e) => setSelectedObject(e.selected?.[0]));
      canvas.on("selection:updated", (e) => setSelectedObject(e.selected?.[0]));
      canvas.on("selection:cleared", () => setSelectedObject(null));

      // Load template if editing
      if (id && id !== "new") {
        loadTemplate(id);
      }

      return () => {
        canvas.dispose();
        fabricCanvasRef.current = null;
      };
    } catch (err) {
      console.error('Error initializing canvas:', err);
    }
  }, [fabricLoaded, canvasElement]);

  // Update canvas dimensions when label size changes
  useEffect(() => {
    if (!fabricCanvasRef.current) return;
    const dims = getCanvasDimensions();
    fabricCanvasRef.current.setDimensions(dims);
    fabricCanvasRef.current.renderAll();
  }, [template.labelWidth, template.labelHeight, template.dpi, getCanvasDimensions]);

  const loadTemplate = async (templateId) => {
    try {
      console.log('Loading template:', templateId);
      setLoading(true);
      const response = await labelTemplateApi.getById(templateId);
      const data = response.data;
      console.log('Template data:', data);

      setTemplate({
        name: data.name || "",
        description: data.description || "",
        labelWidth: data.labelWidth || 50,
        labelHeight: data.labelHeight || 30,
        dpi: data.dpi || 300,
        isDefault: data.isDefault || false,
        canvasJson: data.canvasJson,
      });

      // Load canvas JSON
      if (data.canvasJson && fabricCanvasRef.current) {
        fabricCanvasRef.current.loadFromJSON(
          typeof data.canvasJson === "string"
            ? JSON.parse(data.canvasJson)
            : data.canvasJson,
          () => fabricCanvasRef.current.renderAll()
        );
      }
    } catch (err) {
      console.error("Error loading template:", err);
      alert("Failed to load template");
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!template.name.trim()) {
      alert("Please enter a template name");
      return;
    }

    try {
      setSaving(true);
      const canvas = fabricCanvasRef.current;
      const canvasJson = canvas ? JSON.stringify(canvas.toJSON(["dataBinding"])) : null;

      // Generate preview image
      let previewImage = null;
      if (canvas) {
        previewImage = canvas.toDataURL({ format: "png", quality: 0.8 });
      }

      const data = {
        name: template.name,
        description: template.description,
        labelWidth: template.labelWidth,
        labelHeight: template.labelHeight,
        dpi: template.dpi,
        isDefault: template.isDefault,
        canvasJson: canvasJson,
        previewImage: previewImage,
      };

      if (id && id !== "new") {
        await labelTemplateApi.update(id, data);
        alert("Template saved successfully!");
      } else {
        // Create new template and navigate to edit page with new ID
        const response = await labelTemplateApi.create(data);
        const newId = response.data.id;
        alert("Template created successfully!");
        // Navigate to edit URL so user can continue editing
        navigate(ROUTES.LABEL_DESIGNER_EDIT.replace(":id", newId), { replace: true });
      }
    } catch (err) {
      console.error("Error saving template:", err);
      alert("Failed to save template: " + (err.response?.data?.message || err.message));
    } finally {
      setSaving(false);
    }
  };

  // Add text element
  const addText = (text = "Text", isDataBound = false, binding = null) => {
    if (!fabricCanvasRef.current || !fabricRef.current) return;

    const fabric = fabricRef.current;
    // Fabric v7 uses IText or FabricText
    const TextClass = fabric.IText || fabric.FabricText || fabric.Textbox;
    const textObj = new TextClass(isDataBound ? `{${binding}}` : text, {
      left: 50,
      top: 50,
      fontSize: 24,
      fontFamily: "Arial",
      fill: "#000000",
      dataBinding: binding,
    });

    fabricCanvasRef.current.add(textObj);
    fabricCanvasRef.current.setActiveObject(textObj);
    fabricCanvasRef.current.renderAll();
  };

  // Add barcode placeholder
  const addBarcode = (binding = "barcode") => {
    if (!fabricCanvasRef.current || !fabricRef.current) return;

    const fabric = fabricRef.current;
    const RectClass = fabric.Rect || fabric.FabricRect;
    const TextClass = fabric.Text || fabric.FabricText;
    const GroupClass = fabric.Group || fabric.FabricGroup;

    const group = new GroupClass([
      new RectClass({
        width: 150,
        height: 50,
        fill: "#f3f4f6",
        stroke: "#d1d5db",
        strokeWidth: 1,
      }),
      new TextClass(`[Barcode: {${binding}}]`, {
        fontSize: 12,
        fill: "#6b7280",
        originX: "center",
        originY: "center",
      }),
    ], {
      left: 50,
      top: 50,
      dataBinding: binding,
      elementType: "barcode",
    });

    fabricCanvasRef.current.add(group);
    fabricCanvasRef.current.setActiveObject(group);
    fabricCanvasRef.current.renderAll();
  };

  // Add QR code placeholder
  const addQRCode = (binding = "qrUrl") => {
    if (!fabricCanvasRef.current || !fabricRef.current) return;

    const fabric = fabricRef.current;
    const RectClass = fabric.Rect || fabric.FabricRect;
    const TextClass = fabric.Text || fabric.FabricText;
    const GroupClass = fabric.Group || fabric.FabricGroup;

    const group = new GroupClass([
      new RectClass({
        width: 80,
        height: 80,
        fill: "#f3f4f6",
        stroke: "#d1d5db",
        strokeWidth: 1,
      }),
      new TextClass(`[QR]\n{${binding}}`, {
        fontSize: 10,
        fill: "#6b7280",
        textAlign: "center",
        originX: "center",
        originY: "center",
      }),
    ], {
      left: 50,
      top: 50,
      dataBinding: binding,
      elementType: "qrcode",
    });

    fabricCanvasRef.current.add(group);
    fabricCanvasRef.current.setActiveObject(group);
    fabricCanvasRef.current.renderAll();
  };

  // Add rectangle
  const addRectangle = () => {
    if (!fabricCanvasRef.current || !fabricRef.current) return;

    const fabric = fabricRef.current;
    const RectClass = fabric.Rect || fabric.FabricRect;
    const rect = new RectClass({
      left: 50,
      top: 50,
      width: 100,
      height: 60,
      fill: "transparent",
      stroke: "#000000",
      strokeWidth: 1,
    });

    fabricCanvasRef.current.add(rect);
    fabricCanvasRef.current.setActiveObject(rect);
    fabricCanvasRef.current.renderAll();
  };

  // Add line
  const addLine = () => {
    if (!fabricCanvasRef.current || !fabricRef.current) return;

    const fabric = fabricRef.current;
    const LineClass = fabric.Line || fabric.FabricLine;
    const line = new LineClass([50, 50, 200, 50], {
      stroke: "#000000",
      strokeWidth: 1,
    });

    fabricCanvasRef.current.add(line);
    fabricCanvasRef.current.setActiveObject(line);
    fabricCanvasRef.current.renderAll();
  };

  // Delete selected object
  const deleteSelected = () => {
    if (!fabricCanvasRef.current || !selectedObject) return;

    fabricCanvasRef.current.remove(selectedObject);
    setSelectedObject(null);
    fabricCanvasRef.current.renderAll();
  };

  // Update selected object property
  const updateSelectedProperty = (property, value) => {
    if (!selectedObject) return;

    selectedObject.set(property, value);
    fabricCanvasRef.current.renderAll();
  };

  const applyPreset = (preset) => {
    setTemplate({
      ...template,
      labelWidth: preset.width,
      labelHeight: preset.height,
      dpi: preset.dpi,
    });
  };

  const isInitializing = !fabricLoaded || !canvasReady;

  return (
    <div className="label-designer">
      {/* Header */}
      <div className="designer-header">
        <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
          <button className="pod-btn" onClick={() => navigate(ROUTES.LABEL_TEMPLATES)}>
            &larr; Back
          </button>
          <input
            type="text"
            className="pod-input"
            placeholder="Template Name"
            value={template.name}
            onChange={(e) => setTemplate({ ...template, name: e.target.value })}
            style={{ width: "300px", fontSize: "1.1rem", fontWeight: "500" }}
          />
        </div>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button className="pod-btn pod-btn-secondary" onClick={() => navigate(ROUTES.LABEL_TEMPLATES)}>
            Cancel
          </button>
          <button className="pod-btn pod-btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? "Saving..." : "Save Template"}
          </button>
        </div>
      </div>

      <div className="designer-body">
        {/* Left Sidebar - Elements */}
        <div className="designer-sidebar">
          <div className="sidebar-tabs">
            <button
              className={`sidebar-tab ${activeTab === "elements" ? "active" : ""}`}
              onClick={() => setActiveTab("elements")}
            >
              Elements
            </button>
            <button
              className={`sidebar-tab ${activeTab === "settings" ? "active" : ""}`}
              onClick={() => setActiveTab("settings")}
            >
              Settings
            </button>
          </div>

          {activeTab === "elements" && (
            <div className="sidebar-content">
              <h4>Basic Elements</h4>
              <div className="element-buttons">
                <button className="element-btn" onClick={() => addText()}>
                  <span className="element-icon">T</span>
                  <span>Text</span>
                </button>
                <button className="element-btn" onClick={() => addRectangle()}>
                  <span className="element-icon">[]</span>
                  <span>Rectangle</span>
                </button>
                <button className="element-btn" onClick={() => addLine()}>
                  <span className="element-icon">—</span>
                  <span>Line</span>
                </button>
              </div>

              <h4>Data Bindings</h4>
              <div className="element-buttons">
                {DATA_BINDINGS.map((binding) => (
                  <button
                    key={binding.value}
                    className="element-btn small"
                    onClick={() => addText(`{${binding.value}}`, true, binding.value)}
                  >
                    <span>{binding.label}</span>
                  </button>
                ))}
              </div>

              <h4>Barcodes</h4>
              <div className="element-buttons">
                <button className="element-btn" onClick={() => addBarcode("barcode")}>
                  <span className="element-icon">|||</span>
                  <span>Barcode</span>
                </button>
                <button className="element-btn" onClick={() => addQRCode("qrUrl")}>
                  <span className="element-icon">QR</span>
                  <span>QR Code</span>
                </button>
              </div>
            </div>
          )}

          {activeTab === "settings" && (
            <div className="sidebar-content">
              <h4>Label Size</h4>
              <div className="settings-form">
                <label>
                  Preset
                  <select
                    className="pod-input"
                    onChange={(e) => {
                      const preset = LABEL_PRESETS.find((p) => p.name === e.target.value);
                      if (preset) applyPreset(preset);
                    }}
                  >
                    <option value="">Custom</option>
                    {LABEL_PRESETS.map((preset) => (
                      <option key={preset.name} value={preset.name}>
                        {preset.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Width (mm)
                  <input
                    type="number"
                    className="pod-input"
                    value={template.labelWidth}
                    onChange={(e) => setTemplate({ ...template, labelWidth: Number(e.target.value) })}
                  />
                </label>
                <label>
                  Height (mm)
                  <input
                    type="number"
                    className="pod-input"
                    value={template.labelHeight}
                    onChange={(e) => setTemplate({ ...template, labelHeight: Number(e.target.value) })}
                  />
                </label>
                <label>
                  DPI
                  <select
                    className="pod-input"
                    value={template.dpi}
                    onChange={(e) => setTemplate({ ...template, dpi: Number(e.target.value) })}
                  >
                    <option value={203}>203 DPI</option>
                    <option value={300}>300 DPI</option>
                    <option value={600}>600 DPI</option>
                  </select>
                </label>
              </div>

              <h4>Template Info</h4>
              <div className="settings-form">
                <label>
                  Description
                  <textarea
                    className="pod-input"
                    rows={3}
                    value={template.description}
                    onChange={(e) => setTemplate({ ...template, description: e.target.value })}
                    placeholder="Optional description..."
                  />
                </label>
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={template.isDefault}
                    onChange={(e) => setTemplate({ ...template, isDefault: e.target.checked })}
                  />
                  Set as default template
                </label>
              </div>
            </div>
          )}
        </div>

        {/* Canvas Area */}
        <div className="designer-canvas-area">
          <div className="canvas-info">
            {template.labelWidth}mm x {template.labelHeight}mm @ {template.dpi}DPI
          </div>
          <div className="canvas-container" style={{ position: "relative" }}>
            {(isInitializing || loading) && (
              <div style={{
                position: "absolute",
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                background: "rgba(255,255,255,0.8)",
                zIndex: 10,
              }}>
                <div className="pod-loading">
                  <div className="pod-loading-spinner" />
                  <p>{isInitializing ? "Initializing designer..." : "Loading template..."}</p>
                </div>
              </div>
            )}
            <canvas ref={canvasRef} />
          </div>
        </div>

        {/* Right Sidebar - Properties */}
        <div className="designer-properties">
          <h4>Properties</h4>
          {selectedObject ? (
            <div className="properties-form">
              {selectedObject.type === "i-text" && (
                <>
                  <label>
                    Text
                    <input
                      type="text"
                      className="pod-input"
                      value={selectedObject.text || ""}
                      onChange={(e) => {
                        selectedObject.set("text", e.target.value);
                        fabricCanvasRef.current.renderAll();
                      }}
                    />
                  </label>
                  <label>
                    Font Size
                    <input
                      type="number"
                      className="pod-input"
                      value={selectedObject.fontSize || 24}
                      onChange={(e) => updateSelectedProperty("fontSize", Number(e.target.value))}
                    />
                  </label>
                  <label>
                    Font Family
                    <select
                      className="pod-input"
                      value={selectedObject.fontFamily || "Arial"}
                      onChange={(e) => updateSelectedProperty("fontFamily", e.target.value)}
                    >
                      <option value="Arial">Arial</option>
                      <option value="Helvetica">Helvetica</option>
                      <option value="Times New Roman">Times New Roman</option>
                      <option value="Courier New">Courier New</option>
                    </select>
                  </label>
                  <label>
                    Bold
                    <input
                      type="checkbox"
                      checked={selectedObject.fontWeight === "bold"}
                      onChange={(e) => updateSelectedProperty("fontWeight", e.target.checked ? "bold" : "normal")}
                    />
                  </label>
                </>
              )}

              <label>
                Fill Color
                <input
                  type="color"
                  value={selectedObject.fill || "#000000"}
                  onChange={(e) => updateSelectedProperty("fill", e.target.value)}
                />
              </label>

              {selectedObject.stroke !== undefined && (
                <label>
                  Stroke Color
                  <input
                    type="color"
                    value={selectedObject.stroke || "#000000"}
                    onChange={(e) => updateSelectedProperty("stroke", e.target.value)}
                  />
                </label>
              )}

              <label>
                X Position
                <input
                  type="number"
                  className="pod-input"
                  value={Math.round(selectedObject.left || 0)}
                  onChange={(e) => updateSelectedProperty("left", Number(e.target.value))}
                />
              </label>
              <label>
                Y Position
                <input
                  type="number"
                  className="pod-input"
                  value={Math.round(selectedObject.top || 0)}
                  onChange={(e) => updateSelectedProperty("top", Number(e.target.value))}
                />
              </label>

              <button
                className="pod-btn"
                style={{ color: "#dc2626", width: "100%", marginTop: "1rem" }}
                onClick={deleteSelected}
              >
                Delete Element
              </button>
            </div>
          ) : (
            <p style={{ color: "#9ca3af", fontSize: "0.875rem" }}>
              Select an element to edit its properties
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
