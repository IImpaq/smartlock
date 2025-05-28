package com.smartlock.key

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.smartlock.key.databinding.ActivityMainBinding
import com.github.pingpong.prover.Verifier

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check permissions
        checkAndRequestPermissions()

        // Hide action bar for full-screen experience
        supportActionBar?.hide()
    }

    private fun setupNavigation() {
        // Get NavHostFragment and NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        val navGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
        if (setupCompleted()) {     // Decide start destination based on previous setups
            navGraph.setStartDestination(R.id.navigation_verifier_details)
        } else {
            navGraph.setStartDestination(R.id.navigation_qr_scan)
        }
        navController.graph = navGraph

        // Force recreation of the current fragment to ensure initialization with permissions
        val currentDestination = navController.currentDestination?.id
        if (currentDestination != null) {
            navController.navigate(currentDestination)
        }
    }

    private fun setupCompleted(): Boolean {
        // TODO CHECK IF DESIRED VERIFIER WAS CONNECTED
        val lastUsed = Verifier.getLastConnectedVerifierDeviceID(applicationContext)
        return lastUsed != null
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()

        // Camera is always required
        requiredPermissions.add(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): Only Bluetooth permissions
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            // Android 11 and below: Location and legacy Bluetooth permissions
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            requiredPermissions.add(Manifest.permission.BLUETOOTH)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // Filter out already granted permissions
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            setupNavigation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()
                && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupNavigation()   // All permissions granted
            } else {
                Toast.makeText( // Some permissions denied
                    this,
                    "This app requires all requested permissions to function properly",
                    Toast.LENGTH_LONG
                ).show()

                // Delay for 2 seconds before closing the app
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 2000)
            }
        }
    }
}
