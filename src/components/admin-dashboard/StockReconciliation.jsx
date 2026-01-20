import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { Html5Qrcode } from "html5-qrcode";
import { productsAPI, misaAmisAPI, stockReconciliationAPI } from "@/services/api";
import { generateStockReconciliationReport } from "@/utils/stockReconciliationExport";
import { saveAs } from "file-saver";
import "./StockReconciliation.css";

export default function StockReconciliation() {
  // Scanner state
  const [isScanning, setIsScanning] = useState(false);
  const [scannerReady, setScannerReady] = useState(false);
  const [lastScanned, setLastScanned] = useState(null);
  const [scanError, setScanError] = useState(null);
  const scannerRef = useRef(null);
  const html5QrCodeRef = useRef(null);

  // Stock count state
  const [scannedItems, setScannedItems] = useState({}); // { skuCode: { count, productInfo } }
  const [systemStock, setSystemStock] = useState({}); // { skuCode: { quantity, productInfo } }
  const [isLoadingSystem, setIsLoadingSystem] = useState(false);

  // View state - simplified to 2 views
  const [activeView, setActiveView] = useState("reconcile"); // 'reconcile' | 'inventory'

  // MISA AMIS state
  const [misaStock, setMisaStock] = useState([]); // MISA inventory balance data
  const [misaWarehouses, setMisaWarehouses] = useState([]); // Available warehouses
  const [selectedWarehouse, setSelectedWarehouse] = useState(""); // Selected warehouse ID
  const [isLoadingMisa, setIsLoadingMisa] = useState(false);
  const [misaError, setMisaError] = useState(null);
  const [misaStatus, setMisaStatus] = useState({ authenticated: false });

  // Search state
  const [searchQuery, setSearchQuery] = useState("");
  const [inventorySearch, setInventorySearch] = useState("");

  // Session state
  const [sessionStartTime, setSessionStartTime] = useState(null);
  const [totalScans, setTotalScans] = useState(0);

  // Audio feedback
  const successBeepRef = useRef(null);
  const errorBeepRef = useRef(null);
  const audioContextRef = useRef(null);

  // Build MISA lookup map for quick access
  // Supports both exact match and base SKU match (without variant suffix like -RO, -WH, etc.)
  const misaLookup = useMemo(() => {
    const lookup = {};
    const baseSkuMap = {}; // Maps base SKU (without variant) to full SKU codes

    misaStock.forEach(item => {
      const sku = item.inventory_item_code;
      if (!sku) return;
      // Filter by warehouse if selected
      if (selectedWarehouse && item.stock_id !== selectedWarehouse) return;

      if (!lookup[sku]) {
        lookup[sku] = {
          quantity: 0,
          name: item.inventory_item_name,
          warehouse: item.stock_name,
          unitPrice: item.unit_price || 0,
          amount: 0
        };
      }
      lookup[sku].quantity += item.quantity_balance || 0;
      lookup[sku].amount += item.amount_balance || 0;

      // Also index by base SKU (without variant suffix like -RO, -WH, -BL, etc.)
      // Pattern: SKU might end with -XX where XX is a 2-letter variant code
      const variantMatch = sku.match(/^(.+)-([A-Z]{2})$/);
      if (variantMatch) {
        const baseSku = variantMatch[1];
        if (!baseSkuMap[baseSku]) {
          baseSkuMap[baseSku] = [];
        }
        baseSkuMap[baseSku].push(sku);
      }
    });

    // Attach baseSkuMap to lookup for variant matching
    lookup._baseSkuMap = baseSkuMap;
    return lookup;
  }, [misaStock, selectedWarehouse]);

  // Helper function to find MISA item by SKU (supports exact and base SKU match)
  const findMisaItem = useCallback((skuCode) => {
    // Try exact match first
    if (misaLookup[skuCode]) {
      return { item: misaLookup[skuCode], matchedSku: skuCode };
    }

    // Try base SKU match (scanned barcode might not have variant suffix)
    const baseSkuMap = misaLookup._baseSkuMap || {};
    if (baseSkuMap[skuCode]) {
      // Found variants for this base SKU
      const variants = baseSkuMap[skuCode];
      if (variants.length === 1) {
        // Single variant - use it directly
        const fullSku = variants[0];
        return { item: misaLookup[fullSku], matchedSku: fullSku };
      } else if (variants.length > 1) {
        // Multiple variants - aggregate them
        const aggregated = {
          quantity: 0,
          name: misaLookup[variants[0]]?.name || "Multiple variants",
          warehouse: misaLookup[variants[0]]?.warehouse || "Multiple",
          unitPrice: 0,
          amount: 0,
          variants: variants
        };
        variants.forEach(v => {
          if (misaLookup[v]) {
            aggregated.quantity += misaLookup[v].quantity || 0;
            aggregated.amount += misaLookup[v].amount || 0;
          }
        });
        return { item: aggregated, matchedSku: skuCode, isAggregated: true };
      }
    }

    return { item: null, matchedSku: null };
  }, [misaLookup]);

  // Initialize audio
  useEffect(() => {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return;

    const audioContext = new AudioContextClass();
    audioContextRef.current = audioContext;

    successBeepRef.current = () => {
      if (audioContext.state === 'closed') return;
      try {
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();
        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);
        oscillator.frequency.value = 1200;
        oscillator.type = "sine";
        gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.1);
        oscillator.start(audioContext.currentTime);
        oscillator.stop(audioContext.currentTime + 0.1);
      } catch (e) {
        console.warn('Audio beep failed:', e);
      }
    };

    errorBeepRef.current = () => {
      if (audioContext.state === 'closed') return;
      try {
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();
        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);
        oscillator.frequency.value = 300;
        oscillator.type = "sine";
        gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3);
        oscillator.start(audioContext.currentTime);
        oscillator.stop(audioContext.currentTime + 0.3);
      } catch (e) {
        console.warn('Audio beep failed:', e);
      }
    };

    return () => {
      try {
        if (audioContext && typeof audioContext.close === 'function' && audioContext.state !== 'closed') {
          audioContext.close().catch(() => {});
        }
      } catch (e) {
        // Ignore cleanup errors
      }
    };
  }, []);

  // Cleanup scanner on unmount
  useEffect(() => {
    return () => {
      if (html5QrCodeRef.current) {
        try {
          html5QrCodeRef.current.stop().catch(() => {});
        } catch (e) {
          // Ignore cleanup errors
        }
        html5QrCodeRef.current = null;
      }
      const scannerDiv = document.getElementById('barcode-reader');
      if (scannerDiv) {
        scannerDiv.innerHTML = '';
      }
    };
  }, []);

  // Load MISA data on mount
  useEffect(() => {
    loadMisaStatus();
    loadMisaInventoryBalance();
  }, []);

  // Handle successful scan
  const onScanSuccess = useCallback(async (decodedText) => {
    const skuCode = decodedText.trim().toUpperCase();

    // Avoid duplicate rapid scans of same item (within 1 second)
    if (lastScanned?.sku === skuCode && Date.now() - lastScanned.time < 1000) {
      return;
    }

    setLastScanned({ sku: skuCode, time: Date.now() });
    setTotalScans(prev => prev + 1);

    try {
      // Check if we already have this item (by scanned code)
      if (scannedItems[skuCode]) {
        setScannedItems(prev => ({
          ...prev,
          [skuCode]: {
            ...prev[skuCode],
            count: prev[skuCode].count + 1
          }
        }));
        successBeepRef.current?.();
      } else {
        // Try to get info from MISA first (supports base SKU match), then fall back to API
        const { item: misaItem, matchedSku } = findMisaItem(skuCode);

        if (misaItem) {
          setScannedItems(prev => ({
            ...prev,
            [skuCode]: {
              count: 1,
              matchedMisaSku: matchedSku, // Store the matched MISA SKU for comparison
              productInfo: {
                id: null,
                name: misaItem.name,
                category: misaItem.warehouse || "N/A",
                systemStock: misaItem.quantity
              }
            }
          }));
          successBeepRef.current?.();
        } else {
          // Fetch product info from API
          try {
            const response = await productsAPI.getBySku(skuCode);
            const product = response.data;

            setScannedItems(prev => ({
              ...prev,
              [skuCode]: {
                count: 1,
                productInfo: {
                  id: product.id,
                  name: product.name,
                  category: product.category?.name || "N/A",
                  systemStock: product.stockQuantity || 0
                }
              }
            }));

            setSystemStock(prev => ({
              ...prev,
              [skuCode]: {
                quantity: product.stockQuantity || 0,
                productInfo: product
              }
            }));

            successBeepRef.current?.();
          } catch (apiError) {
            // Product not found in system or MISA
            setScannedItems(prev => ({
              ...prev,
              [skuCode]: {
                count: 1,
                productInfo: {
                  id: null,
                  name: "UNKNOWN PRODUCT",
                  category: "N/A",
                  systemStock: 0,
                  notInSystem: true
                }
              }
            }));
            errorBeepRef.current?.();
            setScanError(`SKU "${skuCode}" not found in system`);
            setTimeout(() => setScanError(null), 3000);
          }
        }
      }
    } catch (error) {
      console.error("Scan processing error:", error);
      errorBeepRef.current?.();
    }
  }, [lastScanned, scannedItems, findMisaItem]);

  // Start camera scanner
  const startScanner = async () => {
    if (!scannerRef.current) return;

    try {
      const html5QrCode = new Html5Qrcode("barcode-reader");
      html5QrCodeRef.current = html5QrCode;

      await html5QrCode.start(
        { facingMode: "environment" },
        {
          fps: 10,
          qrbox: { width: 300, height: 150 },
          aspectRatio: 1.777778,
          formatsToSupport: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16]
        },
        onScanSuccess,
        () => {}
      );

      setIsScanning(true);
      setScannerReady(true);
      if (!sessionStartTime) {
        setSessionStartTime(Date.now());
      }
    } catch (error) {
      console.error("Failed to start scanner:", error);
      setScanError("Failed to access camera. Please check permissions.");
    }
  };

  // Stop scanner
  const stopScanner = async () => {
    if (html5QrCodeRef.current) {
      try {
        await html5QrCodeRef.current.stop();
        html5QrCodeRef.current = null;
      } catch (error) {
        console.error("Error stopping scanner:", error);
      }
    }
    setIsScanning(false);
    setScannerReady(false);
  };

  // Load MISA AMIS status
  const loadMisaStatus = async () => {
    try {
      const response = await misaAmisAPI.getStatus();
      setMisaStatus(response.data);
    } catch (error) {
      console.error("Failed to load MISA status:", error);
      setMisaStatus({ authenticated: false });
    }
  };

  // Helper to parse MISA response
  const parseMisaResponse = (response) => {
    try {
      if (response.data?.Success && response.data?.Data) {
        const parsed = JSON.parse(response.data.Data);
        return Array.isArray(parsed) ? parsed : [];
      }
      const data = response.data?.data || response.data || [];
      return Array.isArray(data) ? data : [];
    } catch (e) {
      console.error("Failed to parse MISA response:", e);
      return [];
    }
  };

  // Load MISA inventory balance
  const loadMisaInventoryBalance = async () => {
    setIsLoadingMisa(true);
    setMisaError(null);
    try {
      const params = { page: 1, size: 100 };
      const response = await misaAmisAPI.getInventoryBalance(params);
      const data = parseMisaResponse(response);
      setMisaStock(data);

      // Extract unique warehouses from the data
      const warehouseMap = new Map();
      data.forEach(item => {
        if (item.stock_id && !warehouseMap.has(item.stock_id)) {
          warehouseMap.set(item.stock_id, {
            stock_id: item.stock_id,
            stock_code: item.stock_code,
            stock_name: item.stock_name
          });
        }
      });
      const warehouses = Array.from(warehouseMap.values());
      if (warehouses.length > 0) {
        setMisaWarehouses(warehouses);
      }

      return data;
    } catch (error) {
      console.error("Failed to load MISA inventory balance:", error);
      setMisaError("Failed to load inventory from MISA. Make sure vouchers are posted (ghi sổ).");
      setMisaStock([]);
      return [];
    } finally {
      setIsLoadingMisa(false);
    }
  };

  // Add item from MISA inventory to scanned items
  const addFromMisa = (item) => {
    const sku = item.inventory_item_code;
    if (!sku) return;

    if (scannedItems[sku]) {
      setScannedItems(prev => ({
        ...prev,
        [sku]: {
          ...prev[sku],
          count: prev[sku].count + 1
        }
      }));
    } else {
      setScannedItems(prev => ({
        ...prev,
        [sku]: {
          count: 1,
          productInfo: {
            id: null,
            name: item.inventory_item_name,
            category: item.stock_name || "N/A",
            systemStock: item.quantity_balance || 0
          }
        }
      }));
    }

    if (!sessionStartTime) {
      setSessionStartTime(Date.now());
    }
    setTotalScans(prev => prev + 1);
    successBeepRef.current?.();
  };

  // Clear session
  const clearSession = () => {
    setScannedItems({});
    setTotalScans(0);
    setSessionStartTime(null);
    setLastScanned(null);
    setSearchQuery("");
  };

  // Manual SKU entry
  const [manualSku, setManualSku] = useState("");
  const handleManualEntry = () => {
    if (manualSku.trim()) {
      onScanSuccess(manualSku.trim());
      setManualSku("");
    }
  };

  // Adjust count manually
  const adjustCount = (sku, delta) => {
    setScannedItems(prev => {
      const newCount = Math.max(0, (prev[sku]?.count || 0) + delta);
      if (newCount === 0) {
        const { [sku]: removed, ...rest } = prev;
        return rest;
      }
      return {
        ...prev,
        [sku]: {
          ...prev[sku],
          count: newCount
        }
      };
    });
  };

  // Get comparison data for scanned items
  const getComparisonData = useMemo(() => {
    const items = Object.entries(scannedItems).map(([sku, { count, productInfo, matchedMisaSku }]) => {
      // Use findMisaItem for flexible matching (supports base SKU without variant suffix)
      const { item: misaItem } = findMisaItem(sku);
      const misaQty = misaItem?.quantity || 0;
      const diff = count - misaQty;
      const notInMisa = !misaItem;

      return {
        sku,
        matchedMisaSku: matchedMisaSku || (misaItem ? sku : null), // Show matched MISA SKU if different
        name: misaItem?.name || productInfo.name,
        warehouse: misaItem?.warehouse || productInfo.category,
        physicalCount: count,
        misaCount: misaQty,
        difference: diff,
        status: notInMisa ? "not_in_misa" : (diff === 0 ? "match" : (diff > 0 ? "excess" : "missing")),
        notInSystem: productInfo.notInSystem
      };
    });

    // Filter by search query
    if (searchQuery) {
      return items.filter(item =>
        item.sku.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.name.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    return items.sort((a, b) => {
      // Sort: mismatches first, then by absolute difference
      if (a.status === "match" && b.status !== "match") return 1;
      if (a.status !== "match" && b.status === "match") return -1;
      return Math.abs(b.difference) - Math.abs(a.difference);
    });
  }, [scannedItems, findMisaItem, searchQuery]);

  // Get filtered MISA inventory
  const filteredMisaStock = useMemo(() => {
    let items = selectedWarehouse
      ? misaStock.filter(item => item.stock_id === selectedWarehouse)
      : misaStock;

    if (inventorySearch) {
      items = items.filter(item =>
        (item.inventory_item_code || "").toLowerCase().includes(inventorySearch.toLowerCase()) ||
        (item.inventory_item_name || "").toLowerCase().includes(inventorySearch.toLowerCase())
      );
    }

    return items;
  }, [misaStock, selectedWarehouse, inventorySearch]);

  // Calculate summary stats
  const summaryStats = useMemo(() => {
    const total = getComparisonData.length;
    const matches = getComparisonData.filter(i => i.status === "match").length;
    const mismatches = getComparisonData.filter(i => i.status !== "match").length;
    const missing = getComparisonData.filter(i => i.status === "missing").length;
    const excess = getComparisonData.filter(i => i.status === "excess").length;
    const notInMisa = getComparisonData.filter(i => i.status === "not_in_misa").length;

    // Progress: how many MISA items have been scanned (using flexible matching)
    // Count unique MISA SKUs (excluding the _baseSkuMap key)
    const misaItemsCount = Object.keys(misaLookup).filter(k => k !== '_baseSkuMap').length;
    const scannedMisaItems = Object.keys(scannedItems).filter(sku => {
      const { item } = findMisaItem(sku);
      return item !== null;
    }).length;
    const progress = misaItemsCount > 0 ? Math.round((scannedMisaItems / misaItemsCount) * 100) : 0;

    return { total, matches, mismatches, missing, excess, notInMisa, progress, misaItemsCount, scannedMisaItems };
  }, [getComparisonData, misaLookup, scannedItems, findMisaItem]);

  // Export report state
  const [isExporting, setIsExporting] = useState(false);
  const [showExportConfirm, setShowExportConfirm] = useState(false);

  // Handle export button click - show confirmation modal
  const handleExportClick = () => {
    setShowExportConfirm(true);
  };

  // Export report as DOCX and save to backend
  const exportReport = async () => {
    setShowExportConfirm(false); // Close modal
    setIsExporting(true);
    try {
      const warehouseName = selectedWarehouse
        ? misaWarehouses.find(w => w.stock_id === selectedWarehouse)?.stock_name || "All Warehouses"
        : "All Warehouses";

      // Prepare scanned items with unit price
      const scannedItemsForExport = getComparisonData.map(item => ({
        ...item,
        unitPrice: item.unitPrice || 0,
        unit: item.unit || "Cái",
        quality: "good"
      }));

      // Get set of scanned SKUs (including matched MISA SKUs)
      const scannedSkus = new Set();
      getComparisonData.forEach(item => {
        scannedSkus.add(item.sku);
        if (item.matchedMisaSku) {
          scannedSkus.add(item.matchedMisaSku);
        }
      });

      // Add unscanned MISA items (items in system but not counted = missing)
      const unscannedMisaItems = filteredMisaStock
        .filter(item => !scannedSkus.has(item.inventory_item_code))
        .map(item => ({
          sku: item.inventory_item_code,
          name: item.inventory_item_name || "",
          physicalCount: 0, // Not scanned = 0 physical count
          misaCount: item.quantity_balance || 0,
          difference: -(item.quantity_balance || 0), // Negative = missing
          status: "missing",
          unitPrice: item.unit_price || 0,
          unit: item.unit_name || "Cái",
          quality: "good"
        }));

      // Combine scanned and unscanned items
      const allItemsForExport = [...scannedItemsForExport, ...unscannedMisaItems];

      // Sort: first by status (match, excess, missing, not_in_misa), then by SKU
      const statusOrder = { match: 0, excess: 1, missing: 2, not_in_misa: 3 };
      allItemsForExport.sort((a, b) => {
        const statusDiff = (statusOrder[a.status] || 99) - (statusOrder[b.status] || 99);
        if (statusDiff !== 0) return statusDiff;
        return (a.sku || "").localeCompare(b.sku || "");
      });

      const documentNumber = `KK-${new Date().getTime()}`;
      const inventoryDate = new Date();

      // Generate DOCX report (return blob instead of saving)
      const { blob, fileName } = await generateStockReconciliationReport({
        companyName: "MIRROR DIAMOND",
        department: warehouseName,
        documentNumber,
        inventoryDate,
        committee: [], // Will show placeholder lines
        items: allItemsForExport,
        warehouseName,
        returnBlob: true // Return blob for API upload
      });

      // Save local copy first
      saveAs(blob, fileName);

      // Calculate summary stats
      const summary = {
        total: allItemsForExport.length,
        match: allItemsForExport.filter(i => i.status === "match").length,
        missing: allItemsForExport.filter(i => i.status === "missing").length,
        excess: allItemsForExport.filter(i => i.status === "excess").length,
        notInMisa: allItemsForExport.filter(i => i.status === "not_in_misa").length
      };

      // Prepare data for backend
      const recordData = {
        documentNumber,
        warehouseId: selectedWarehouse || null,
        warehouseName,
        reconciliationDate: inventoryDate.toISOString(),
        summary,
        items: allItemsForExport,
        notes: `Kiểm kê ${warehouseName} - ${inventoryDate.toLocaleDateString('vi-VN')}`
      };

      // Convert blob to File for upload
      const file = new File([blob], fileName, {
        type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      });

      // Save to backend
      try {
        const response = await stockReconciliationAPI.createRecord(recordData, file);
        console.log("Stock reconciliation record saved:", response.data);

        // Stop scanner and clear session after successful export
        await stopScanner();
        clearSession();
      } catch (apiError) {
        console.error("Failed to save to backend:", apiError);
        // Still show success for local download, warn about backend
        alert(`⚠️ Đã tải xuống biên bản!\nLưu ý: Không thể lưu vào hệ thống (${apiError.message})`);

        // Still stop scanner and clear session
        await stopScanner();
        clearSession();
      }

    } catch (error) {
      console.error("Export failed:", error);
      alert("Xuất báo cáo thất bại. Vui lòng thử lại.");
    } finally {
      setIsExporting(false);
    }
  };

  // Export report as JSON (alternative)
  const exportReportJSON = () => {
    const report = {
      sessionDate: new Date().toISOString(),
      warehouse: selectedWarehouse ? misaWarehouses.find(w => w.stock_id === selectedWarehouse)?.stock_name : "All Warehouses",
      totalScans,
      uniqueItems: Object.keys(scannedItems).length,
      summary: summaryStats,
      items: getComparisonData
    };

    const blob = new Blob([JSON.stringify(report, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `stock-reconciliation-${new Date().toISOString().split("T")[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const sessionDuration = sessionStartTime ? Math.floor((Date.now() - sessionStartTime) / 1000 / 60) : 0;

  return (
    <div className="stock-reconciliation-v2">
      {/* Global Header */}
      <div className="recon-global-header">
        <div className="header-left">
          <h2>Stock Reconciliation</h2>
          <span className={`connection-status ${misaStatus.authenticated ? "connected" : "disconnected"}`}>
            {misaStatus.authenticated ? "MISA Connected" : "MISA Disconnected"}
          </span>
        </div>

        <div className="header-center">
          <div className="warehouse-selector-global">
            <label>Warehouse:</label>
            <select
              value={selectedWarehouse}
              onChange={(e) => setSelectedWarehouse(e.target.value)}
              disabled={isLoadingMisa}
            >
              <option value="">All Warehouses</option>
              {misaWarehouses.map((wh) => (
                <option key={wh.stock_id} value={wh.stock_id}>
                  {wh.stock_name || wh.stock_code}
                </option>
              ))}
            </select>
            <button
              className="btn-icon"
              onClick={() => loadMisaInventoryBalance()}
              disabled={isLoadingMisa}
              title="Refresh MISA Data"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" className={isLoadingMisa ? "spinning" : ""}>
                <path d="M23 4v6h-6M1 20v-6h6" />
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
              </svg>
            </button>
          </div>
        </div>

        <div className="header-right">
          <div className="view-tabs">
            <button
              className={`view-tab ${activeView === "reconcile" ? "active" : ""}`}
              onClick={() => setActiveView("reconcile")}
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M16 3h5v5M8 3H3v5M3 16v5h5M21 16v5h-5" />
              </svg>
              Reconcile
            </button>
            <button
              className={`view-tab ${activeView === "inventory" ? "active" : ""}`}
              onClick={() => setActiveView("inventory")}
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
              </svg>
              MISA Inventory
            </button>
          </div>
        </div>
      </div>

      {/* Progress Bar */}
      {activeView === "reconcile" && summaryStats.misaItemsCount > 0 && (
        <div className="progress-section">
          <div className="progress-info">
            <span>Progress: {summaryStats.scannedMisaItems}/{summaryStats.misaItemsCount} items scanned</span>
            <span className="progress-percent">{summaryStats.progress}%</span>
          </div>
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${summaryStats.progress}%` }} />
          </div>
        </div>
      )}

      {/* Main Content */}
      {activeView === "reconcile" ? (
        <div className="reconcile-layout">
          {/* Left Panel - Scanner */}
          <div className="scanner-panel">
            <div className="panel-header">
              <h3>Scanner</h3>
              {sessionStartTime && (
                <span className="session-time">{sessionDuration}m</span>
              )}
            </div>

            <div className="scanner-container">
              {!isScanning && (
                <div className="scanner-placeholder">
                  <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2" />
                    <line x1="7" y1="9" x2="17" y2="9" />
                    <line x1="7" y1="12" x2="17" y2="12" />
                    <line x1="7" y1="15" x2="17" y2="15" />
                  </svg>
                  <p>Click Start to scan</p>
                </div>
              )}
              <div
                id="barcode-reader"
                ref={scannerRef}
                className={`barcode-reader ${isScanning ? "active" : ""}`}
                style={{ display: isScanning ? 'block' : 'none' }}
              />
            </div>

            <div className="scanner-actions">
              {!isScanning ? (
                <button className="btn-scan-start" onClick={startScanner}>
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
                    <circle cx="12" cy="13" r="4" />
                  </svg>
                  Start Scanner
                </button>
              ) : (
                <button className="btn-scan-stop" onClick={stopScanner}>
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="6" y="6" width="12" height="12" />
                  </svg>
                  Stop
                </button>
              )}
            </div>

            {/* Manual Entry */}
            <div className="manual-entry">
              <input
                type="text"
                placeholder="Enter SKU manually..."
                value={manualSku}
                onChange={(e) => setManualSku(e.target.value.toUpperCase())}
                onKeyDown={(e) => e.key === "Enter" && handleManualEntry()}
              />
              <button onClick={handleManualEntry} disabled={!manualSku.trim()}>
                Add
              </button>
            </div>

            {/* Status Messages */}
            {scanError && (
              <div className="scan-error">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                {scanError}
              </div>
            )}

            {lastScanned && (
              <div className="last-scanned">
                Last: <strong>{lastScanned.sku}</strong>
              </div>
            )}

            {/* Quick Stats */}
            <div className="quick-stats">
              <div className="quick-stat">
                <span className="stat-num">{totalScans}</span>
                <span className="stat-lbl">Scans</span>
              </div>
              <div className="quick-stat">
                <span className="stat-num">{Object.keys(scannedItems).length}</span>
                <span className="stat-lbl">Items</span>
              </div>
              <div className="quick-stat match">
                <span className="stat-num">{summaryStats.matches}</span>
                <span className="stat-lbl">Match</span>
              </div>
              <div className="quick-stat warning">
                <span className="stat-num">{summaryStats.mismatches}</span>
                <span className="stat-lbl">Mismatch</span>
              </div>
            </div>
          </div>

          {/* Right Panel - Live Comparison */}
          <div className="comparison-panel">
            <div className="panel-header">
              <h3>Live Comparison</h3>
              <div className="panel-actions">
                <div className="search-box">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="11" cy="11" r="8" />
                    <path d="M21 21l-4.35-4.35" />
                  </svg>
                  <input
                    type="text"
                    placeholder="Search..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                </div>
                <button
                  className="btn-export"
                  onClick={handleExportClick}
                  disabled={(getComparisonData.length === 0 && filteredMisaStock.length === 0) || isExporting}
                  title="Xuất biên bản kiểm kê (bao gồm cả hàng chưa kiểm)"
                >
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3" />
                  </svg>
                  {isExporting ? "Đang xuất..." : "Xuất biên bản"}
                </button>
                {Object.keys(scannedItems).length > 0 && (
                  <button className="btn-clear" onClick={clearSession}>Clear</button>
                )}
              </div>
            </div>

            {/* Summary Cards */}
            <div className="summary-cards">
              <div className="summary-card total">
                <span className="card-value">{summaryStats.total}</span>
                <span className="card-label">Scanned</span>
              </div>
              <div className="summary-card match">
                <span className="card-value">{summaryStats.matches}</span>
                <span className="card-label">Match</span>
              </div>
              <div className="summary-card missing">
                <span className="card-value">{summaryStats.missing}</span>
                <span className="card-label">Missing</span>
              </div>
              <div className="summary-card excess">
                <span className="card-value">{summaryStats.excess}</span>
                <span className="card-label">Excess</span>
              </div>
              <div className="summary-card not-found">
                <span className="card-value">{summaryStats.notInMisa}</span>
                <span className="card-label">Not in MISA</span>
              </div>
            </div>

            {/* Comparison Table */}
            <div className="comparison-table">
              {getComparisonData.length === 0 ? (
                <div className="empty-comparison">
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2" />
                    <line x1="7" y1="9" x2="17" y2="9" />
                    <line x1="7" y1="12" x2="17" y2="12" />
                    <line x1="7" y1="15" x2="17" y2="15" />
                  </svg>
                  <p>Scan items or select from MISA Inventory</p>
                  <p className="hint">Physical counts will be compared with MISA balance in real-time</p>
                </div>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>SKU / Name</th>
                      <th className="num-col">Physical</th>
                      <th className="num-col">MISA</th>
                      <th className="num-col">Diff</th>
                      <th>Status</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {getComparisonData.map((item) => (
                      <tr key={item.sku} className={`row-${item.status}`}>
                        <td className="sku-cell" data-label="SKU">
                          <span className="sku-code">{item.sku}</span>
                          <span className="name-cell">{item.name}</span>
                        </td>
                        <td className="num-col physical-count" data-label="Physical">{item.physicalCount}</td>
                        <td className="num-col misa-count" data-label="MISA">{item.misaCount}</td>
                        <td className={`num-col diff-cell ${item.difference > 0 ? "positive" : item.difference < 0 ? "negative" : ""}`} data-label="Diff">
                          {item.difference > 0 ? "+" : ""}{item.difference}
                        </td>
                        <td data-label="Status">
                          <span className={`status-badge ${item.status}`}>
                            {item.status === "match" && "✓ Match"}
                            {item.status === "excess" && "↑ Excess"}
                            {item.status === "missing" && "↓ Missing"}
                            {item.status === "not_in_misa" && "? Not in MISA"}
                          </span>
                        </td>
                        <td className="actions-cell">
                          <button
                            className="btn-adjust"
                            onClick={(e) => { e.stopPropagation(); adjustCount(item.sku, -1); }}
                            title="Decrease"
                          >−</button>
                          <button
                            className="btn-adjust"
                            onClick={(e) => { e.stopPropagation(); adjustCount(item.sku, 1); }}
                            title="Increase"
                          >+</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      ) : (
        /* Inventory View */
        <div className="inventory-view">
          <div className="inventory-header">
            <div className="inventory-summary">
              <div className="inv-stat">
                <span className="inv-value">{filteredMisaStock.length}</span>
                <span className="inv-label">Items</span>
              </div>
              <div className="inv-stat">
                <span className="inv-value">
                  {filteredMisaStock.reduce((sum, item) => sum + (item.quantity_balance || 0), 0).toLocaleString()}
                </span>
                <span className="inv-label">Total Qty</span>
              </div>
              <div className="inv-stat">
                <span className="inv-value">
                  {filteredMisaStock.reduce((sum, item) => sum + (item.amount_balance || 0), 0).toLocaleString()}
                </span>
                <span className="inv-label">Total Value (VND)</span>
              </div>
            </div>

            <div className="search-box large">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
              </svg>
              <input
                type="text"
                placeholder="Search by code or name..."
                value={inventorySearch}
                onChange={(e) => setInventorySearch(e.target.value)}
              />
              {inventorySearch && (
                <button className="clear-search" onClick={() => setInventorySearch("")}>×</button>
              )}
            </div>
          </div>

          {misaError && (
            <div className="misa-error">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              {misaError}
            </div>
          )}

          <div className="inventory-table">
            <p className="table-hint">Click an item to add to your physical count</p>

            {isLoadingMisa ? (
              <div className="loading-state">
                <div className="spinner"></div>
                <p>Loading inventory from MISA...</p>
              </div>
            ) : filteredMisaStock.length === 0 ? (
              <div className="empty-state">
                <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                </svg>
                <p>No inventory data found{selectedWarehouse ? " for this warehouse" : ""}.</p>
              </div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Item Code</th>
                    <th>Item Name</th>
                    {!selectedWarehouse && <th>Warehouse</th>}
                    <th className="num-col">Qty</th>
                    <th className="num-col">Unit Price</th>
                    <th className="num-col">Amount</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {filteredMisaStock.map((item, index) => {
                    const isScanned = scannedItems[item.inventory_item_code];
                    return (
                      <tr
                        key={item.inventory_item_id || index}
                        className={`clickable-row ${isScanned ? "already-scanned" : ""}`}
                        onClick={() => addFromMisa(item)}
                      >
                        <td className="code-cell">{item.inventory_item_code || "-"}</td>
                        <td>{item.inventory_item_name || "-"}</td>
                        {!selectedWarehouse && <td className="warehouse-cell">{item.stock_name || "-"}</td>}
                        <td className="num-col">{(item.quantity_balance || 0).toLocaleString()}</td>
                        <td className="num-col">{(item.unit_price || 0).toLocaleString()}</td>
                        <td className="num-col amount-cell">{(item.amount_balance || 0).toLocaleString()}</td>
                        <td className="action-cell">
                          {isScanned ? (
                            <div className="scanned-controls">
                              <button
                                className="btn-adjust-small"
                                onClick={(e) => { e.stopPropagation(); adjustCount(item.inventory_item_code, -1); }}
                                title="Remove one"
                              >−</button>
                              <span className="scanned-count">{isScanned.count}</span>
                              <button
                                className="btn-adjust-small"
                                onClick={(e) => { e.stopPropagation(); adjustCount(item.inventory_item_code, 1); }}
                                title="Add one"
                              >+</button>
                            </div>
                          ) : (
                            <button className="btn-add-small" onClick={(e) => { e.stopPropagation(); addFromMisa(item); }}>+ Add</button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {/* Export Confirmation Modal */}
      {showExportConfirm && (
        <div className="modal-overlay" onClick={() => setShowExportConfirm(false)}>
          <div className="modal-content export-confirm-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Xác nhận xuất biên bản kiểm kê</h3>
              <button className="modal-close" onClick={() => setShowExportConfirm(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="confirm-icon">
                <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M9 12l2 2 4-4" />
                  <path d="M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z" />
                </svg>
              </div>
              <p className="confirm-message">Bạn có chắc chắn muốn hoàn tất phiên kiểm kê này?</p>
              <div className="confirm-details">
                <div className="confirm-item">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="6" y="6" width="12" height="12" />
                  </svg>
                  <span>Dừng phiên kiểm kê hiện tại</span>
                </div>
                <div className="confirm-item">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="16" y1="13" x2="8" y2="13" />
                    <line x1="16" y1="17" x2="8" y2="17" />
                  </svg>
                  <span>Xuất biên bản kiểm kê (file DOCX)</span>
                </div>
                <div className="confirm-item">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                    <polyline points="17 21 17 13 7 13 7 21" />
                    <polyline points="7 3 7 8 15 8" />
                  </svg>
                  <span>Lưu biên bản vào hệ thống</span>
                </div>
              </div>
              <div className="confirm-summary">
                <span>Tổng số mặt hàng: <strong>{summaryStats.total + (filteredMisaStock.length - summaryStats.scannedMisaItems)}</strong></span>
                <span className="match">Khớp: <strong>{summaryStats.matches}</strong></span>
                <span className="missing">Thiếu: <strong>{summaryStats.missing + (filteredMisaStock.length - summaryStats.scannedMisaItems)}</strong></span>
                <span className="excess">Thừa: <strong>{summaryStats.excess}</strong></span>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-cancel" onClick={() => setShowExportConfirm(false)}>
                Hủy
              </button>
              <button className="btn-confirm" onClick={exportReport} disabled={isExporting}>
                {isExporting ? "Đang xuất..." : "Xác nhận & Xuất biên bản"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
