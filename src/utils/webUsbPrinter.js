/**
 * WebUSB Printer Utility
 *
 * Allows direct communication with USB printers (like Zebra) from the browser
 * without needing any additional software or drivers.
 *
 * Requirements:
 * - Chrome/Edge browser (Chromium-based)
 * - HTTPS or localhost
 * - User permission for USB access
 *
 * Supported Printers:
 * - UROVO (vendorId: 0x2B4E) - D81R, D813R series
 * - Zebra (vendorId: 0x0A5F)
 * - TSC (vendorId: 0x1203)
 * - HPRT (vendorId: 0x0493)
 *
 * @example
 * const printer = new WebUsbPrinter();
 * await printer.connect();
 * await printer.print(zplData);
 */

// Known printer vendor IDs
const PRINTER_VENDORS = {
  UROVO: 0x0471,      // UROVO D81R, D813R series (confirmed)
  UROVO_ALT: 0x2B4E,  // Alternative UROVO vendor ID
  ZEBRA: 0x0A5F,
  TSC: 0x1203,
  HPRT: 0x0493,
  EPSON: 0x04B8,
  BROTHER: 0x04F9,
};

// All supported vendor IDs for device filter
const ALL_VENDOR_IDS = Object.values(PRINTER_VENDORS);

class WebUsbPrinter {
  constructor() {
    this.device = null;
    this.interfaceNumber = 0;
    this.endpointOut = null;
    this.endpointIn = null;
    this._listeners = {
      connect: [],
      disconnect: [],
      error: [],
    };
  }

  /**
   * Check if WebUSB is supported in current browser
   */
  static isSupported() {
    return typeof navigator !== 'undefined' && 'usb' in navigator;
  }

  /**
   * Get list of already paired/authorized devices
   */
  async getPairedDevices() {
    if (!WebUsbPrinter.isSupported()) {
      return [];
    }

    try {
      const devices = await navigator.usb.getDevices();
      return devices.map(device => ({
        vendorId: device.vendorId,
        productId: device.productId,
        productName: device.productName || 'Unknown Printer',
        manufacturerName: device.manufacturerName || 'Unknown',
        serialNumber: device.serialNumber || '',
      }));
    } catch (error) {
      console.error('Failed to get paired devices:', error);
      return [];
    }
  }

  /**
   * Request user to select a printer
   * This will show browser's USB device picker dialog
   * @param {boolean} showAllDevices - If true, show all USB devices (not just known printers)
   */
  async requestDevice(showAllDevices = false) {
    if (!WebUsbPrinter.isSupported()) {
      throw new Error('WebUSB is not supported in this browser. Please use Chrome or Edge.');
    }

    try {
      let filters;

      if (showAllDevices) {
        // Show all printer-class devices (class 0x07) plus vendor-specific (0xFF)
        // This is broader than vendor ID filter
        filters = [
          { classCode: 0x07 },  // Printer class
          { classCode: 0xFF },  // Vendor-specific (many thermal printers use this)
          // Also include all known vendors as fallback
          ...ALL_VENDOR_IDS.map(vendorId => ({ vendorId })),
        ];
      } else {
        // Create filters for known printer vendors only
        filters = ALL_VENDOR_IDS.map(vendorId => ({ vendorId }));
      }

      console.log('Requesting USB device with filters:', filters);
      this.device = await navigator.usb.requestDevice({ filters });

      console.log('Selected device:', {
        vendorId: '0x' + this.device.vendorId.toString(16).toUpperCase(),
        productId: '0x' + this.device.productId.toString(16).toUpperCase(),
        productName: this.device.productName,
        manufacturerName: this.device.manufacturerName,
      });

      return {
        vendorId: this.device.vendorId,
        productId: this.device.productId,
        productName: this.device.productName || 'Unknown Printer',
        manufacturerName: this.device.manufacturerName || 'Unknown',
        serialNumber: this.device.serialNumber || '',
      };
    } catch (error) {
      if (error.name === 'NotFoundError') {
        throw new Error('Không tìm thấy máy in. Vui lòng chọn máy in từ dialog hoặc thử "Tất cả USB".');
      }
      throw error;
    }
  }

  /**
   * Request any USB device (broader filter - for unknown printers)
   */
  async requestAnyDevice() {
    return this.requestDevice(true);
  }

  /**
   * Connect to the selected device
   * @param {boolean} showAllDevices - If true, show all USB devices in picker
   */
  async connect(showAllDevices = false) {
    if (!this.device) {
      // If no device selected, request one
      await this.requestDevice(showAllDevices);
    }

    try {
      // Open device
      await this.device.open();
      console.log('Device opened successfully');

      // Select configuration (usually 1)
      if (this.device.configuration === null) {
        await this.device.selectConfiguration(1);
        console.log('Selected configuration 1');
      }

      // Find the printer interface and endpoints
      const interfaces = this.device.configuration.interfaces;
      console.log(`Found ${interfaces.length} interfaces`);

      // Log all interfaces for debugging
      for (const iface of interfaces) {
        for (const alternate of iface.alternates) {
          console.log(`Interface ${iface.interfaceNumber}: class=${alternate.interfaceClass}, subclass=${alternate.interfaceSubclass}, endpoints=${alternate.endpoints.length}`);
          for (const endpoint of alternate.endpoints) {
            console.log(`  Endpoint ${endpoint.endpointNumber}: direction=${endpoint.direction}, type=${endpoint.type}`);
          }
        }
      }

      // Find suitable interface - try multiple strategies
      for (const iface of interfaces) {
        for (const alternate of iface.alternates) {
          // Class 7 is Printer, 255 is vendor-specific (common for thermal printers)
          // Also try class 0 (composite device) which some printers use
          if (alternate.interfaceClass === 7 || alternate.interfaceClass === 255 || alternate.interfaceClass === 0) {
            // Check if this interface has an OUT endpoint
            const outEndpoint = alternate.endpoints.find(ep => ep.direction === 'out');
            if (outEndpoint) {
              this.interfaceNumber = iface.interfaceNumber;
              this.endpointOut = outEndpoint.endpointNumber;
              const inEndpoint = alternate.endpoints.find(ep => ep.direction === 'in');
              if (inEndpoint) {
                this.endpointIn = inEndpoint.endpointNumber;
              }
              console.log(`Found suitable interface: ${this.interfaceNumber}, endpoint OUT: ${this.endpointOut}`);
              break;
            }
          }
        }
        if (this.endpointOut !== null) break;
      }

      // If no endpoint found, try first interface with OUT endpoint
      if (this.endpointOut === null) {
        for (const iface of interfaces) {
          for (const alternate of iface.alternates) {
            const outEndpoint = alternate.endpoints.find(ep => ep.direction === 'out');
            if (outEndpoint) {
              this.interfaceNumber = iface.interfaceNumber;
              this.endpointOut = outEndpoint.endpointNumber;
              console.log(`Fallback: using interface ${this.interfaceNumber}, endpoint ${this.endpointOut}`);
              break;
            }
          }
          if (this.endpointOut !== null) break;
        }
      }

      // Last resort: try interface 0 with endpoint 1
      if (this.endpointOut === null) {
        this.interfaceNumber = 0;
        this.endpointOut = 1;
        console.log('Last resort: using interface 0, endpoint 1');
      }

      // Claim the interface
      await this.device.claimInterface(this.interfaceNumber);
      console.log(`Interface ${this.interfaceNumber} claimed`);

      console.log(`✅ Connected to ${this.device.productName} (interface: ${this.interfaceNumber}, endpoint OUT: ${this.endpointOut})`);

      // Emit connect event
      this._emit('connect', this.getDeviceInfo());

      // Listen for disconnect
      navigator.usb.addEventListener('disconnect', this._handleDisconnect);

      return this.getDeviceInfo();
    } catch (error) {
      console.error('Connection failed:', error);
      this._emit('error', error);
      throw new Error(`Failed to connect to printer: ${error.message}`);
    }
  }

  /**
   * Handle device disconnect event
   */
  _handleDisconnect = (event) => {
    if (event.device === this.device) {
      console.log('Printer disconnected');
      this.device = null;
      this.endpointOut = null;
      this.endpointIn = null;
      this._emit('disconnect');
    }
  };

  /**
   * Check if printer is connected
   */
  isConnected() {
    return this.device !== null && this.device.opened;
  }

  /**
   * Get device info
   */
  getDeviceInfo() {
    if (!this.device) return null;

    return {
      vendorId: this.device.vendorId,
      productId: this.device.productId,
      productName: this.device.productName || 'Unknown Printer',
      manufacturerName: this.device.manufacturerName || 'Unknown',
      serialNumber: this.device.serialNumber || '',
      connected: this.isConnected(),
    };
  }

  /**
   * Send data to printer
   * @param {string|Uint8Array} data - ZPL/TSPL string or raw bytes
   */
  async print(data) {
    if (!this.isConnected()) {
      throw new Error('Printer not connected. Call connect() first.');
    }

    try {
      let bytes;
      if (typeof data === 'string') {
        const encoder = new TextEncoder();
        bytes = encoder.encode(data);
      } else {
        bytes = data;
      }

      const result = await this.device.transferOut(this.endpointOut, bytes);

      if (result.status !== 'ok') {
        throw new Error(`Transfer failed with status: ${result.status}`);
      }

      console.log(`Sent ${bytes.length} bytes to printer`);
      return {
        success: true,
        bytesWritten: result.bytesWritten,
      };
    } catch (error) {
      console.error('Print failed:', error);
      this._emit('error', error);
      throw new Error(`Failed to send data to printer: ${error.message}`);
    }
  }

  /**
   * Print ZPL label
   * @param {string} zpl - ZPL command string
   */
  async printZPL(zpl) {
    // Ensure ZPL starts with ^XA and ends with ^XZ
    let normalizedZpl = zpl.trim();
    if (!normalizedZpl.startsWith('^XA')) {
      normalizedZpl = '^XA\n' + normalizedZpl;
    }
    if (!normalizedZpl.endsWith('^XZ')) {
      normalizedZpl = normalizedZpl + '\n^XZ';
    }

    return this.print(normalizedZpl);
  }

  /**
   * Print test label to verify connection
   */
  async printTestLabel() {
    const testZpl = `^XA
^FO50,50^A0N,50,50^FDWebUSB Test Print^FS
^FO50,120^A0N,30,30^FD${new Date().toLocaleString()}^FS
^FO50,170^BY2^BCN,80,Y,N,N^FD123456789^FS
^XZ`;

    return this.printZPL(testZpl);
  }

  /**
   * Read response from printer (if supported)
   */
  async read(timeout = 5000) {
    if (!this.isConnected() || !this.endpointIn) {
      throw new Error('Printer not connected or does not support reading');
    }

    try {
      const result = await Promise.race([
        this.device.transferIn(this.endpointIn, 64),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Read timeout')), timeout)
        ),
      ]);

      const decoder = new TextDecoder();
      return decoder.decode(result.data);
    } catch (error) {
      console.error('Read failed:', error);
      throw error;
    }
  }

  /**
   * Disconnect from printer
   */
  async disconnect() {
    if (this.device) {
      try {
        navigator.usb.removeEventListener('disconnect', this._handleDisconnect);

        if (this.device.opened) {
          await this.device.releaseInterface(this.interfaceNumber);
          await this.device.close();
        }
      } catch (error) {
        console.error('Disconnect error:', error);
      }

      this.device = null;
      this.endpointOut = null;
      this.endpointIn = null;

      this._emit('disconnect');
    }
  }

  /**
   * Event listener management
   */
  on(event, callback) {
    if (this._listeners[event]) {
      this._listeners[event].push(callback);
    }
    return () => this.off(event, callback);
  }

  off(event, callback) {
    if (this._listeners[event]) {
      this._listeners[event] = this._listeners[event].filter(cb => cb !== callback);
    }
  }

  _emit(event, data) {
    if (this._listeners[event]) {
      this._listeners[event].forEach(callback => callback(data));
    }
  }
}

// Singleton instance for global use
let printerInstance = null;

/**
 * Get global printer instance (singleton)
 */
export const getWebUsbPrinter = () => {
  if (!printerInstance) {
    printerInstance = new WebUsbPrinter();
  }
  return printerInstance;
};

/**
 * Reset global printer instance
 */
export const resetWebUsbPrinter = async () => {
  if (printerInstance) {
    await printerInstance.disconnect();
    printerInstance = null;
  }
};

export default WebUsbPrinter;
export { PRINTER_VENDORS };
