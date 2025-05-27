package com.smartlock.key.ui.qrscan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.pingpong.prover.Verifier
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.smartlock.key.R
import com.smartlock.key.databinding.FragmentQrBinding
import com.smartlock.key.services.ScannerService

class QRFragment : Fragment(), ScannerService.ScannerCallback {
    private var _binding: FragmentQrBinding? = null
    private val binding get() = _binding!!
    private lateinit var verifier: Verifier

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize services
        ScannerService.initialize(requireContext(), binding.scannerView)
        ScannerService.setCallback(this)

        // Setup rescan button
        binding.buttonRescan.setOnClickListener {
            ScannerService.resume()
            binding.buttonRescan.visibility = View.GONE
        }
        ScannerService.start()
    }

    //==============================================================================================
    //==============================================================================================

    override fun onVerifierDataDetected(data: JSONObject) {
        activity?.runOnUiThread {
            // Create and connect to the verifier
            binding.statusText.text = getString(R.string.scanning)
            verifier = Verifier.createFromQRCode(requireContext(), data)

            // Connect using your library
            lifecycleScope.launch {
                val success = verifier.connect()
                if (success) {
                    binding.statusText.text = getString(R.string.setup_complete)
                    findNavController().navigate(R.id.action_navigation_qr_scan_to_navigation_verifier_details)
                } else {
                    binding.statusText.text = getString(R.string.data_send_failed)
                    binding.buttonRescan.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onInvalidQRCode(message: String) {
        activity?.runOnUiThread {
            binding.statusText.text = getString(R.string.invalid_qr_code)
            binding.buttonRescan.visibility = View.VISIBLE
        }
    }

    override fun onScanError(message: String) {
        activity?.runOnUiThread {
            binding.statusText.text = getString(R.string.scanner_error)
            binding.buttonRescan.visibility = View.VISIBLE
        }
    }

    //==============================================================================================
    // Fragment Lifecycle Methods
    //==============================================================================================

    override fun onResume() {
        super.onResume()
        ScannerService.resume()
        ScannerService.setCallback(this)
    }

    override fun onPause() {
        super.onPause()
        ScannerService.pause()
        ScannerService.setCallback(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ScannerService.setCallback(null)
        if (::verifier.isInitialized) {
            verifier.disconnect()
        }
        _binding = null
    }
}
