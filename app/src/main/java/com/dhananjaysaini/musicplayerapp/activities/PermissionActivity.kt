package com.dhananjaysaini.musicplayerapp.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.ActivityPermissionBinding
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding
    private val handler = Handler(Looper.getMainLooper())
    private var isNavigated = false
    private var hasPromptedOnce = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navigateToMain()
        } else {
            hasPromptedOnce = true
            updateButtonState()
        }
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasMediaPermission()) {
            navigateToMain()
        } else {
            updateButtonState()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasMediaPermission()) {
            navigateToMain()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.permissionRoot) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        setupClickListeners()

        // Auto prompt permission on initial entrance so user gets immediate flow
        handler.postDelayed({
            if (!isFinishing && !isDestroyed && !hasMediaPermission() && !isNavigated) {
                requestPermissionLauncher.launch(getRequiredMediaPermission())
            }
        }, 350)
    }

    private fun setupClickListeners() {
        binding.btnAllowAccess.setOnClickListener {
            handlePermissionRequest()
        }

        binding.btnExit.setOnClickListener {
            finish()
        }
    }

    private fun handlePermissionRequest() {
        if (hasMediaPermission()) {
            navigateToMain()
            return
        }

        val permission = getRequiredMediaPermission()
        val canShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

        if (hasPromptedOnce && !canShowRationale) {
            // Permanently denied: guide user to Settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            settingsLauncher.launch(intent)
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun updateButtonState() {
        if (isFinishing || isDestroyed) return
        val permission = getRequiredMediaPermission()
        val canShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

        if (hasPromptedOnce && !canShowRationale) {
            binding.btnAllowAccess.setText(R.string.btn_open_settings)
            binding.tvPermissionSubtitle.setText(R.string.media_permission_required_desc)
        } else {
            binding.btnAllowAccess.setText(R.string.btn_allow_access)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasMediaPermission()) {
            navigateToMain()
        } else if (hasPromptedOnce) {
            updateButtonState()
        }
    }

    private fun navigateToMain() {
        if (isNavigated || isFinishing || isDestroyed) return
        isNavigated = true

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun getRequiredMediaPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun hasMediaPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            getRequiredMediaPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
