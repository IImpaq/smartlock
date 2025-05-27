package com.smartlock.key.ui.verifier

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.pingpong.prover.Verifier
import kotlinx.coroutines.launch
import com.smartlock.key.R
import com.smartlock.key.databinding.FragmentDetailsBinding
import java.net.UnknownServiceException

class DetailsFragment : Fragment() {
    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var verifier: Verifier
    private var unlocking = false

    companion object {
        private const val POLLING_INTERVAL_MS = 200L
        private const val UNLOCK_THRESHOLD_M = 0.5
    }

    private val distancePollRunnable = object : Runnable {
        override fun run() {
            pollDistance()
            handler.postDelayed(this, POLLING_INTERVAL_MS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeVerifier()
        fillVerifierDetails()
        updateDistanceUI(-1.0) // Initial "unknown" state

        binding.buttonUnlock.setOnClickListener { performUnlock() }
        binding.buttonRemoveVerifier.setOnClickListener { removeVerifier() }
    }

    private fun initializeVerifier() {
        val lastConnectedDeviceID = Verifier.getLastConnectedVerifierDeviceID(requireContext())
        if (lastConnectedDeviceID != null) {
            verifier = Verifier.createFromDeviceID(requireContext(), lastConnectedDeviceID)
            connectToVerifier()
        } else {
            navigateBackToSetup()
        }
    }

    private fun connectToVerifier() {
        lifecycleScope.launch {
            if (verifier.connect()) {
                binding.textViewProximityStatus.text = getString(R.string.bluetooth_status_connected_polling)
                binding.textViewProximityStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                startDistancePolling()
            } else {
                binding.textViewProximityStatus.text = getString(R.string.bluetooth_status_error_connection)
                binding.textViewProximityStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            }
        }
    }

    private fun navigateBackToSetup() {
        stopDistancePolling()
        if (::verifier.isInitialized) {
            verifier.disconnect()
        }
        findNavController().navigate(R.id.action_navigation_verifier_details_to_navigation_qr_scan)
    }

    private fun fillVerifierDetails() {
        val lastConnectedDeviceID = Verifier.getLastConnectedVerifierDeviceID(requireContext())
        if (lastConnectedDeviceID != null) {
            binding.textViewVerifierName.text = lastConnectedDeviceID
            binding.textViewVerifierId.text = getString(R.string.connected_verifier_id_label) + " (Device Name)"
        } else {
            binding.textViewVerifierName.text = "Unknown Verifier"
            binding.textViewVerifierId.text = ""
            binding.textViewProximityStatus.text = getString(R.string.status_unknown_proximity)
            binding.buttonUnlock.isEnabled = false
        }
    }

    //==============================================================================================
    // Distance Polling Implementation
    //==============================================================================================

    private fun startDistancePolling() {
        stopDistancePolling()
        handler.post(distancePollRunnable)
        binding.textViewProximityStatus.text = getString(R.string.polling_distance)
    }

    private fun stopDistancePolling() {
        handler.removeCallbacks(distancePollRunnable)
    }

    private fun pollDistance() {
        lifecycleScope.launch {
            val distance = verifier.distanceRSSI()
            updateDistanceUI(distance)
        }
    }

    private fun updateDistanceUI(distance: Double) {
        when {
            distance < 0 -> {
                val errorCode = distance.toInt()
                if (errorCode == -2) return

                binding.textViewDistance.text = "-- m"
                binding.progressDistance.progress = 0
                binding.textViewProximityStatus.text = when (errorCode) {
                    -3 -> getString(R.string.status_unknown_proximity)
                    else -> getString(R.string.communication_error_send)
                }
                binding.textViewProximityStatus.setTextColor(
                    ContextCompat.getColor(requireContext(),
                        if (errorCode == -1) android.R.color.darker_gray
                        else android.R.color.holo_red_dark)
                )
                setUnlockButton(false)
                return
            }
        }
        binding.textViewDistance.text = String.format("%.1f m", distance)

        // Simple progress bar: closer = higher progress
        val maxRange = 5.0
        val progress = ((maxRange - distance) / maxRange * 100).coerceIn(0.0, 100.0).toInt()
        binding.progressDistance.progress = progress

        // Update UI state based on distance threshold
        if (!unlocking) {
            val canUnlock = distance <= UNLOCK_THRESHOLD_M

            binding.textViewProximityStatus.text = when {
                canUnlock -> getString(R.string.status_unlockable)
                distance <= 2.0 -> getString(R.string.status_getting_closer)
                else -> getString(R.string.status_too_far)
            }

            binding.textViewProximityStatus.setTextColor(ContextCompat.getColor(requireContext(), when {
                canUnlock -> android.R.color.holo_green_dark
                distance <= 2.0 -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }))
            setUnlockButton(canUnlock)
        }
    }

    //==============================================================================================
    // Button Request Handling
    //==============================================================================================

    private fun setUnlockButton(enabled: Boolean, text: String = "Unlock") {
        binding.buttonUnlock.isEnabled = enabled
        binding.buttonUnlock.text = text
    }

    private fun resetUnlockUI() {
        setUnlockButton(false, "Unlock")
        unlocking = false
        startDistancePolling()
    }

    private fun performUnlock() {
        unlocking = true
        stopDistancePolling()
        setUnlockButton(false, "Unlocking...")

        lifecycleScope.launch {
            val result = verifier.unlock()
            when (result) {
                Verifier.UnlockResult.SUCCESS -> {
                    Toast.makeText(requireContext(), "Unlocked successfully!", Toast.LENGTH_SHORT).show()
                }
                Verifier.UnlockResult.PROXIMITY -> {
                    Toast.makeText(requireContext(), "Unlock failed: Move closer to the device", Toast.LENGTH_SHORT).show()
                }
                Verifier.UnlockResult.FAILED -> {
                    Toast.makeText(requireContext(), "Unlock failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
            resetUnlockUI()
        }
    }

    private fun removeVerifier() {
        // Show confirmation dialog first
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Remove Verifier")
            .setMessage("Are you sure you want to remove this verifier? You'll need to scan the QR code again to reconnect.")
            .setPositiveButton("Remove") { _, _ ->
                performRemoveVerifier()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRemoveVerifier() {
        // Disable the button and show loading state
        binding.buttonRemoveVerifier.isEnabled = false
        binding.buttonRemoveVerifier.text = "Removing..."

        lifecycleScope.launch {
            try {
                // Stop polling and remove the verifier
                stopDistancePolling()

                val success = if (::verifier.isInitialized) {
                    verifier.remove()
                } else {
                    throw UnknownServiceException("Storage removal of Verifier did not work!")
                }

                if (success) {
                    Toast.makeText(requireContext(), "Verifier removed successfully", Toast.LENGTH_SHORT).show()
                    // Navigate back to QR scan
                    findNavController().navigate(R.id.action_navigation_verifier_details_to_navigation_qr_scan)
                } else {
                    Toast.makeText(requireContext(), "Failed to remove verifier", Toast.LENGTH_SHORT).show()
                    binding.buttonRemoveVerifier.isEnabled = true
                    binding.buttonRemoveVerifier.text = "Remove Verifier"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error removing verifier: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.buttonRemoveVerifier.isEnabled = true
                binding.buttonRemoveVerifier.text = "Remove Verifier"
            }
        }
    }

    //==============================================================================================
    // Fragment Lifecycle Methods
    //==============================================================================================

    override fun onResume() {
        super.onResume()
        if (::verifier.isInitialized) startDistancePolling()
    }

    override fun onPause() {
        super.onPause()
        stopDistancePolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopDistancePolling()
        if (::verifier.isInitialized) {
            verifier.disconnect()
        }
        _binding = null
    }
}
