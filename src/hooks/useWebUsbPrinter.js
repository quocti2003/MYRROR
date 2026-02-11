import { useState, useEffect, useCallback, useRef } from 'react';
import WebUsbPrinter, { getWebUsbPrinter } from '../utils/webUsbPrinter';

/**
 * React hook for WebUSB printer functionality
 *
 * @example
 * const {
 *   isSupported,
 *   isConnected,
 *   deviceInfo,
 *   connect,
 *   disconnect,
 *   print,
 *   printTestLabel,
 *   error,
 *   isPrinting
 * } = useWebUsbPrinter();
 *
 * // Connect to printer
 * await connect();
 *
 * // Print ZPL
 * await print(zplData);
 */
const useWebUsbPrinter = (options = {}) => {
  const { useGlobalInstance = true, autoReconnect = true } = options;

  const [isSupported] = useState(() => WebUsbPrinter.isSupported());
  const [isConnected, setIsConnected] = useState(false);
  const [deviceInfo, setDeviceInfo] = useState(null);
  const [error, setError] = useState(null);
  const [isPrinting, setIsPrinting] = useState(false);
  const [pairedDevices, setPairedDevices] = useState([]);

  const printerRef = useRef(null);

  // Get or create printer instance
  const getPrinter = useCallback(() => {
    if (useGlobalInstance) {
      return getWebUsbPrinter();
    }
    if (!printerRef.current) {
      printerRef.current = new WebUsbPrinter();
    }
    return printerRef.current;
  }, [useGlobalInstance]);

  // Load paired devices on mount
  useEffect(() => {
    if (!isSupported) return;

    const loadPairedDevices = async () => {
      const printer = getPrinter();
      const devices = await printer.getPairedDevices();
      setPairedDevices(devices);

      // Auto-reconnect to last used device if available
      if (autoReconnect && devices.length > 0 && !printer.isConnected()) {
        try {
          // Try to connect to the first paired device
          const device = devices[0];
          console.log('Auto-reconnecting to:', device.productName);
          // Note: This won't work automatically - user needs to trigger connect
        } catch (e) {
          console.log('Auto-reconnect skipped - user action required');
        }
      }
    };

    loadPairedDevices();
  }, [isSupported, autoReconnect, getPrinter]);

  // Set up event listeners
  useEffect(() => {
    if (!isSupported) return;

    const printer = getPrinter();

    const handleConnect = (info) => {
      setIsConnected(true);
      setDeviceInfo(info);
      setError(null);
    };

    const handleDisconnect = () => {
      setIsConnected(false);
      setDeviceInfo(null);
    };

    const handleError = (err) => {
      setError(err.message || 'Unknown error');
    };

    // Subscribe to events
    const unsubConnect = printer.on('connect', handleConnect);
    const unsubDisconnect = printer.on('disconnect', handleDisconnect);
    const unsubError = printer.on('error', handleError);

    // Check initial state
    setIsConnected(printer.isConnected());
    setDeviceInfo(printer.getDeviceInfo());

    return () => {
      unsubConnect();
      unsubDisconnect();
      unsubError();
    };
  }, [isSupported, getPrinter]);

  /**
   * Connect to printer (shows device picker dialog)
   * @param {boolean} showAllDevices - If true, show all USB devices (not just known printers)
   */
  const connect = useCallback(async (showAllDevices = false) => {
    if (!isSupported) {
      setError('WebUSB is not supported in this browser');
      return false;
    }

    const printer = getPrinter();
    setError(null);

    try {
      const info = await printer.connect(showAllDevices);
      setIsConnected(true);
      setDeviceInfo(info);

      // Refresh paired devices list
      const devices = await printer.getPairedDevices();
      setPairedDevices(devices);

      return true;
    } catch (err) {
      setError(err.message);
      return false;
    }
  }, [isSupported, getPrinter]);

  /**
   * Disconnect from printer
   */
  const disconnect = useCallback(async () => {
    const printer = getPrinter();
    await printer.disconnect();
    setIsConnected(false);
    setDeviceInfo(null);
  }, [getPrinter]);

  /**
   * Print data to printer
   * @param {string} data - ZPL/TSPL string to print
   */
  const print = useCallback(async (data) => {
    if (!isConnected) {
      setError('Printer not connected');
      return { success: false, error: 'Printer not connected' };
    }

    const printer = getPrinter();
    setIsPrinting(true);
    setError(null);

    try {
      const result = await printer.print(data);
      setIsPrinting(false);
      return { success: true, ...result };
    } catch (err) {
      setIsPrinting(false);
      setError(err.message);
      return { success: false, error: err.message };
    }
  }, [isConnected, getPrinter]);

  /**
   * Print ZPL with validation
   */
  const printZPL = useCallback(async (zpl) => {
    if (!isConnected) {
      setError('Printer not connected');
      return { success: false, error: 'Printer not connected' };
    }

    const printer = getPrinter();
    setIsPrinting(true);
    setError(null);

    try {
      const result = await printer.printZPL(zpl);
      setIsPrinting(false);
      return { success: true, ...result };
    } catch (err) {
      setIsPrinting(false);
      setError(err.message);
      return { success: false, error: err.message };
    }
  }, [isConnected, getPrinter]);

  /**
   * Print test label
   */
  const printTestLabel = useCallback(async () => {
    if (!isConnected) {
      // Try to connect first
      const connected = await connect();
      if (!connected) {
        return { success: false, error: 'Could not connect to printer' };
      }
    }

    const printer = getPrinter();
    setIsPrinting(true);
    setError(null);

    try {
      const result = await printer.printTestLabel();
      setIsPrinting(false);
      return { success: true, ...result };
    } catch (err) {
      setIsPrinting(false);
      setError(err.message);
      return { success: false, error: err.message };
    }
  }, [isConnected, connect, getPrinter]);

  /**
   * Clear error
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    // State
    isSupported,
    isConnected,
    deviceInfo,
    error,
    isPrinting,
    pairedDevices,

    // Actions
    connect,
    disconnect,
    print,
    printZPL,
    printTestLabel,
    clearError,
  };
};

export default useWebUsbPrinter;
