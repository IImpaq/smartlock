package com.smartlock.key.services

import android.annotation.SuppressLint
import android.content.Context
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.zxing.Result
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Interface defining the publicly accessible API of the ScannerService
 * after it has been initialized.
 */
interface ScannerServiceApi {
    fun setCallback(scannerCallback: ScannerService.ScannerCallback?)
    fun start()
    fun stop()
    fun resume()
    fun pause()
}

object ScannerService : ScannerServiceApi {
    @SuppressLint("StaticFieldLeak")
    private lateinit var applicationContext: Context
    @SuppressLint("StaticFieldLeak")
    private lateinit var scanner: CodeScanner

    private var currentScannerViewRef: WeakReference<CodeScannerView>? = null
    private var scannerCallback: ScannerCallback? = null

    // Callback interface for scanner events
    interface ScannerCallback {
        fun onVerifierDataDetected(data: JSONObject)
        fun onInvalidQRCode(message: String)
        fun onScanError(message: String)
    }
    override fun setCallback(scannerCallback: ScannerCallback?) {
        this.scannerCallback = scannerCallback
    }

    /**
     * Initializes or re-initializes the ScannerService with a Context and CodeScannerView.
     * This method MUST be called when the scanning UI is ready.
     *
     * @param activityContext The Context of the Activity or Fragment hosting the scanner.
     *                        This is important for UI components like CodeScanner.
     * @param scannerView The CodeScannerView instance to use for scanning.
     * @return An instance of [ScannerServiceApi] for interacting with the service.
     */
    fun initialize(activityContext: Context, scannerView: CodeScannerView): ScannerServiceApi {
        this.applicationContext = activityContext.applicationContext

        // If scanner is already initialized, release resources
        if (::scanner.isInitialized) {
            scanner.releaseResources()
        }

        this.currentScannerViewRef = WeakReference(scannerView)
        this.scanner = CodeScanner(activityContext, scannerView)
        setupScanner()
        return this
    }

    //==============================================================================================
    // QR Code Scanner Setup and Decoding
    //==============================================================================================

    private fun setupScanner() {
        if (!::scanner.isInitialized) return

        // Configure scanner settings
        scanner.scanMode = ScanMode.SINGLE
        scanner.isAutoFocusEnabled = true
        scanner.isFlashEnabled = false

        // Setup decode Callback
        scanner.decodeCallback = DecodeCallback { result ->
            try {
                processQRCode(result)
            } catch (e: Exception) {
                currentScannerViewRef?.get()?.post {
                    scannerCallback?.onInvalidQRCode("Invalid QR format: ${e.message}")
                }
            }
        }

        // Setup error Callback
        scanner.errorCallback = ErrorCallback { error ->
            currentScannerViewRef?.get()?.post {
                scannerCallback?.onScanError("Scanner error: ${error.message}")
            }
        }
    }

    private fun processQRCode(result: Result) {
        val json = JSONObject(result.text)
        val publicKey = json.optString("public_key")
        val deviceID = json.optString("name")
        val serviceUUID = json.optString("service")
        val characteristicUUID = json.optString("characteristic")
        
        currentScannerViewRef?.get()?.post {
            if (publicKey.isNotEmpty() && deviceID.isNotEmpty() &&
                serviceUUID.isNotEmpty() && characteristicUUID.isNotEmpty()
            ) {
                scannerCallback?.onVerifierDataDetected(json)
            } else {
                scannerCallback?.onInvalidQRCode("QR code missing required fields")
            }
        }
    }

    //==============================================================================================
    // Scanning lifecycle methods (from ScannerServiceApi)
    //==============================================================================================

    override fun start() {
        if (::scanner.isInitialized && currentScannerViewRef?.get() != null) {
            scanner.startPreview()
        }
    }

    override fun stop() {
        if (::scanner.isInitialized) {
            scanner.releaseResources()
        }
    }

    override fun resume() {
        if (::scanner.isInitialized && currentScannerViewRef?.get() != null) {
            scanner.startPreview()
        }
    }

    override fun pause() {
        if (::scanner.isInitialized) {
            scanner.releaseResources()
        }
    }
}
