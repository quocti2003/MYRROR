import React, { useState } from 'react';
import useWebUsbPrinter from '../../hooks/useWebUsbPrinter';
import WebUsbPrinter from '../../utils/webUsbPrinter';

/**
 * WebUSB Printer Connection Component
 *
 * Provides UI for connecting to USB printers via WebUSB API.
 * Shows connection status, device info, and allows printing.
 *
 * @example
 * <WebUsbPrinterConnect
 *   onConnected={(deviceInfo) => console.log('Connected:', deviceInfo)}
 *   onPrint={(zpl) => printer.print(zpl)}
 * />
 */
const WebUsbPrinterConnect = ({
  onConnected,
  onDisconnected,
  onPrintSuccess,
  onPrintError,
  showTestButton = true,
  compact = false,
  className = '',
}) => {
  const {
    isSupported,
    isConnected,
    deviceInfo,
    error,
    isPrinting,
    pairedDevices,
    connect,
    disconnect,
    printTestLabel,
    clearError,
  } = useWebUsbPrinter();

  const [showDetails, setShowDetails] = useState(false);

  // Handle connect
  const handleConnect = async () => {
    clearError();
    const success = await connect();
    if (success && onConnected) {
      onConnected(deviceInfo);
    }
  };

  // Handle disconnect
  const handleDisconnect = async () => {
    await disconnect();
    if (onDisconnected) {
      onDisconnected();
    }
  };

  // Handle test print
  const handleTestPrint = async () => {
    const result = await printTestLabel();
    if (result.success) {
      onPrintSuccess?.('Test label printed successfully');
    } else {
      onPrintError?.(result.error);
    }
  };

  // Not supported message
  if (!isSupported) {
    return (
      <div className={`p-4 bg-yellow-50 border border-yellow-200 rounded-lg ${className}`}>
        <div className="flex items-center gap-2 text-yellow-800">
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
          <span className="font-medium">WebUSB not supported</span>
        </div>
        <p className="mt-1 text-sm text-yellow-700">
          Please use Chrome or Edge browser to connect to USB printers directly.
        </p>
      </div>
    );
  }

  // Compact mode
  if (compact) {
    return (
      <div className={`flex items-center gap-2 ${className}`}>
        <div
          className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500' : 'bg-gray-300'}`}
          title={isConnected ? 'Printer connected' : 'Printer not connected'}
        />
        {isConnected ? (
          <>
            <span className="text-sm text-gray-600 truncate max-w-[150px]">
              {deviceInfo?.productName || 'Connected'}
            </span>
            <button
              onClick={handleDisconnect}
              className="text-xs text-red-600 hover:text-red-800"
            >
              Disconnect
            </button>
          </>
        ) : (
          <button
            onClick={handleConnect}
            className="text-sm text-blue-600 hover:text-blue-800 font-medium"
          >
            Connect Printer
          </button>
        )}
      </div>
    );
  }

  // Full mode
  return (
    <div className={`bg-white border rounded-lg shadow-sm ${className}`}>
      {/* Header */}
      <div className="p-4 border-b">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            {/* Printer Icon */}
            <div className={`p-2 rounded-lg ${isConnected ? 'bg-green-100' : 'bg-gray-100'}`}>
              <svg
                className={`w-6 h-6 ${isConnected ? 'text-green-600' : 'text-gray-400'}`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z"
                />
              </svg>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">USB Printer</h3>
              <p className="text-sm text-gray-500">
                {isConnected
                  ? deviceInfo?.productName || 'Connected'
                  : 'Not connected'}
              </p>
            </div>
          </div>

          {/* Status indicator */}
          <div className="flex items-center gap-2">
            <span
              className={`px-2 py-1 text-xs font-medium rounded-full ${
                isConnected
                  ? 'bg-green-100 text-green-800'
                  : 'bg-gray-100 text-gray-600'
              }`}
            >
              {isConnected ? 'Connected' : 'Disconnected'}
            </span>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="p-4">
        {/* Error message */}
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex items-start gap-2">
              <svg className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
              <div className="flex-1">
                <p className="text-sm text-red-800">{error}</p>
              </div>
              <button
                onClick={clearError}
                className="text-red-500 hover:text-red-700"
              >
                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </button>
            </div>
          </div>
        )}

        {/* Connected state */}
        {isConnected && deviceInfo && (
          <div className="mb-4">
            <button
              onClick={() => setShowDetails(!showDetails)}
              className="text-sm text-blue-600 hover:text-blue-800 flex items-center gap-1"
            >
              <span>{showDetails ? 'Hide' : 'Show'} details</span>
              <svg
                className={`w-4 h-4 transition-transform ${showDetails ? 'rotate-180' : ''}`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            {showDetails && (
              <div className="mt-2 p-3 bg-gray-50 rounded-lg text-sm">
                <div className="grid grid-cols-2 gap-2">
                  <div className="text-gray-500">Product:</div>
                  <div className="font-medium">{deviceInfo.productName}</div>
                  <div className="text-gray-500">Manufacturer:</div>
                  <div className="font-medium">{deviceInfo.manufacturerName}</div>
                  {deviceInfo.serialNumber && (
                    <>
                      <div className="text-gray-500">Serial:</div>
                      <div className="font-medium font-mono text-xs">{deviceInfo.serialNumber}</div>
                    </>
                  )}
                  <div className="text-gray-500">Vendor ID:</div>
                  <div className="font-medium font-mono">0x{deviceInfo.vendorId?.toString(16).toUpperCase()}</div>
                  <div className="text-gray-500">Product ID:</div>
                  <div className="font-medium font-mono">0x{deviceInfo.productId?.toString(16).toUpperCase()}</div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Paired devices (when not connected) */}
        {!isConnected && pairedDevices.length > 0 && (
          <div className="mb-4">
            <p className="text-sm text-gray-600 mb-2">Previously connected printers:</p>
            <div className="space-y-2">
              {pairedDevices.map((device, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between p-2 bg-gray-50 rounded"
                >
                  <span className="text-sm">{device.productName}</span>
                  <button
                    onClick={handleConnect}
                    className="text-xs text-blue-600 hover:text-blue-800"
                  >
                    Reconnect
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Action buttons */}
        <div className="flex flex-wrap gap-2">
          {!isConnected ? (
            <button
              onClick={handleConnect}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
              </svg>
              Connect Printer
            </button>
          ) : (
            <>
              {showTestButton && (
                <button
                  onClick={handleTestPrint}
                  disabled={isPrinting}
                  className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isPrinting ? (
                    <>
                      <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                      </svg>
                      Printing...
                    </>
                  ) : (
                    <>
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      Test Print
                    </>
                  )}
                </button>
              )}
              <button
                onClick={handleDisconnect}
                className="flex items-center gap-2 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                Disconnect
              </button>
            </>
          )}
        </div>
      </div>

      {/* Footer info */}
      <div className="px-4 py-3 bg-gray-50 border-t rounded-b-lg">
        <p className="text-xs text-gray-500">
          Connect directly to USB printers using WebUSB. Supported: Zebra, TSC, HPRT.
          {!isConnected && ' Click "Connect Printer" and select your printer from the dialog.'}
        </p>
      </div>
    </div>
  );
};

export default WebUsbPrinterConnect;
