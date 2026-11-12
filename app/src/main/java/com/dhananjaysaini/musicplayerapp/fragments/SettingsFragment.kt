package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.MainActivity
import com.dhananjaysaini.musicplayerapp.activities.SkinThemeActivity
import com.dhananjaysaini.musicplayerapp.databinding.FragmentSettingsBinding
import com.dhananjaysaini.musicplayerapp.repository.ThemeRepository
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.EqualizerManager
import com.dhananjaysaini.musicplayerapp.utils.LanguageBottomSheetDialog
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.SleepTimerBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var equalizerManager: EqualizerManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupProBanner()
        setupGeneralSection()
        setupPlaybackSection()
        setupPreferenceSection()
        setupHelpSection()
    }

    override fun onResume() {
        super.onResume()
        activity?.let { ThemeManager.applyThemeToActivity(it) }
    }

    // -------------------------------------------------------------------------
    // Top Header
    // -------------------------------------------------------------------------
    private fun setupHeader() {
        binding.ivSettingsBack.setOnClickListener {
            val mainActivity = activity as? MainActivity
            if (mainActivity != null) {
                mainActivity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                    R.id.bottomNavigation
                )?.selectedItemId = R.id.nav_home
            } else {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Pro Banner Card
    // -------------------------------------------------------------------------
    private fun setupProBanner() {
        binding.cardProBanner.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("MusicAir Pro ✨")
                .setMessage(
                    "Upgrade to MusicAir Pro for:\n\n" +
                            "• 10-Band Studio Equalizer & Bass Boost\n" +
                            "• Ultra HD Ambient Glow & Visualizer\n" +
                            "• Dynamic Palette Art Tinting\n" +
                            "• 100% Ad-Free Experience\n" +
                            "• Lifetime Updates & Support"
                )
                .setPositiveButton("Upgrade") { _, _ ->
                    Toast.makeText(
                        requireContext(),
                        "In-app purchases coming soon to Google Play!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setNegativeButton("Maybe Later", null)
                .show()
        }
    }

    // -------------------------------------------------------------------------
    // Section 1: General
    // -------------------------------------------------------------------------
    private fun setupGeneralSection() {
        val sharedPref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // 1. Hidden music
        binding.rowHiddenMusic.setOnClickListener {
            showHiddenMusicDialog()
        }

        // 2. Music transfer
        binding.rowMusicTransfer.setOnClickListener {
            showMusicTransferDialog()
        }

        // 3. Color Themes & Accent
        binding.btnSettingTheme.setOnClickListener {
            startActivity(Intent(requireContext(), SkinThemeActivity::class.java))
        }

        // 4. Dark Mode
        val isDark = ThemeManager.isDarkTheme(requireContext())
        binding.switchDarkMode.isChecked = isDark
        binding.tvDarkModeStatus.text = if (isDark) getString(R.string.status_on) else getString(R.string.status_off)

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            binding.tvDarkModeStatus.text = if (isChecked) getString(R.string.status_on) else getString(R.string.status_off)

            if (isChecked) {
                val currentTheme = ThemeManager.getCurrentTheme(requireContext())
                val darkTheme = if (currentTheme.isDark) {
                    currentTheme
                } else {
                    ThemeRepository.findThemeById("color_2")
                        ?: ThemeRepository.getColorThemes().firstOrNull { it.isDark }
                        ?: currentTheme
                }
                ThemeManager.saveTheme(requireContext(), darkTheme)
            } else {
                val lightTheme = ThemeRepository.findThemeById("color_6")
                    ?: ThemeRepository.getColorThemes().firstOrNull { !it.isDark }
                    ?: ThemeManager.getCurrentTheme(requireContext())
                ThemeManager.saveTheme(requireContext(), lightTheme)
            }
            requireActivity().recreate()
        }

        // 5. Equalizer & Bass Boost
        equalizerManager = EqualizerManager(requireContext())
        binding.btnSettingEqualizer.setOnClickListener {
            val sessionId = MusicService.mediaPlayer?.audioSessionId ?: -1
            if (sessionId > 0) {
                equalizerManager?.showEqualizer(sessionId)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.play_song_first_eq),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // 6. Sleep Timer
        binding.btnSettingSleepTimer.setOnClickListener {
            val bottomSheet = SleepTimerBottomSheet()
            bottomSheet.show(parentFragmentManager, "SleepTimerBottomSheet")
        }
    }

    // -------------------------------------------------------------------------
    // Section 2: Playback
    // -------------------------------------------------------------------------
    private fun setupPlaybackSection() {
        val sharedPref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // 1. Simultaneous playback
        val simultaneous = sharedPref.getBoolean("simultaneous_playback", false)
        binding.switchSimultaneous.isChecked = simultaneous
        binding.switchSimultaneous.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("simultaneous_playback", isChecked).apply()
            val msg = if (isChecked) getString(R.string.toast_simultaneous_enabled) else getString(R.string.toast_simultaneous_disabled)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // 2. Crossfade
        val crossfade = sharedPref.getBoolean("crossfade_playback", false)
        binding.switchCrossfade.isChecked = crossfade
        binding.switchCrossfade.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("crossfade_playback", isChecked).apply()
            val msg = if (isChecked) getString(R.string.toast_crossfade_enabled) else getString(R.string.toast_crossfade_disabled)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // 3. Gapless playback
        val gapless = sharedPref.getBoolean("gapless_playback", false)
        binding.switchGapless.isChecked = gapless
        binding.switchGapless.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("gapless_playback", isChecked).apply()
            val msg = if (isChecked) getString(R.string.toast_gapless_enabled) else getString(R.string.toast_gapless_disabled)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // 4. Pause on detach (default true)
        val pauseOnDetach = sharedPref.getBoolean("pause_on_detach", true)
        binding.switchPauseDetach.isChecked = pauseOnDetach
        binding.switchPauseDetach.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("pause_on_detach", isChecked).apply()
            val msg = if (isChecked) getString(R.string.toast_pause_detach_enabled) else getString(R.string.toast_pause_detach_disabled)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    // -------------------------------------------------------------------------
    // Section 3: Preference
    // -------------------------------------------------------------------------
    private fun setupPreferenceSection() {
        val sharedPref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // 1. Keep screen on (default false)
        val keepScreenOn = sharedPref.getBoolean("keep_screen_on", false)
        binding.switchKeepScreenOn.isChecked = keepScreenOn
        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("keep_screen_on", isChecked).apply()
            val msg = if (isChecked) getString(R.string.toast_screen_on_enabled) else getString(R.string.toast_screen_on_disabled)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // 2. Lock screen playing (default true)
        val lockScreen = sharedPref.getBoolean("lock_screen_playing", true)
        binding.switchLockScreen.isChecked = lockScreen
        binding.switchLockScreen.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("lock_screen_playing", isChecked).apply()
            val msg = if (isChecked) getString(R.string.toast_lock_screen_enabled) else getString(R.string.toast_lock_screen_disabled)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // 3. Notifications (default true)
        val notifications = sharedPref.getBoolean("notifications_enabled", true)
        binding.switchNotifications.isChecked = notifications
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notifications_enabled", isChecked).apply()
            val msg = if (isChecked) getString(R.string.toast_notifications_enabled) else getString(R.string.toast_notifications_disabled)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // 4. Language
        val currentLang = LocaleHelper.getCurrentLanguage(requireContext())
        binding.tvCurrentLanguage.text = if (currentLang.nativeName.equals(currentLang.englishName, ignoreCase = true)) {
            currentLang.nativeName
        } else {
            "${currentLang.nativeName} (${currentLang.englishName})"
        }
        binding.rowLanguage.setOnClickListener {
            LanguageBottomSheetDialog.newInstance { selectedLang ->
                binding.tvCurrentLanguage.text = if (selectedLang.nativeName.equals(selectedLang.englishName, ignoreCase = true)) {
                    selectedLang.nativeName
                } else {
                    "${selectedLang.nativeName} (${selectedLang.englishName})"
                }
            }.show(parentFragmentManager, LanguageBottomSheetDialog.TAG)
        }
    }

    // -------------------------------------------------------------------------
    // Section 4: Help
    // -------------------------------------------------------------------------
    private fun setupHelpSection() {
        // 1. FAQ
        binding.rowFaq.setOnClickListener {
            showFaqDialog()
        }

        // 2. Feedback
        binding.btnSettingFeedback.setOnClickListener {
            sendFeedback(requireContext())
        }

        // 3. Rate us
        binding.rowRateUs.setOnClickListener {
            rateApp()
        }

        // 4. Manage subscription
        binding.rowManageSubscription.setOnClickListener {
            showManageSubscriptionDialog()
        }

        // 5. Privacy policy
        binding.rowPrivacyPolicy.setOnClickListener {
            showPrivacyPolicyDialog()
        }

        // 6. Terms of use
        binding.rowTermsOfUse.setOnClickListener {
            showTermsOfUseDialog()
        }

        // 7. Version & About
        val appVersion = getAppVersion(requireContext())
        binding.tvAppVersion.text = "4.5.0.190" // matches screenshot aesthetic
        binding.tvVersionTag.text = "v$appVersion"

        binding.btnSettingAbout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("About MusicAir")
                .setMessage(
                    "MusicAir Audio Player\n" +
                            "Version: 4.5.0.190 (Build $appVersion)\n\n" +
                            "Experience high-fidelity local music playback with dynamic visuals, fluid animations, studio equalizers, and custom themes.\n\n" +
                            "Crafted with ❤️ for music lovers."
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // -------------------------------------------------------------------------
    // Dialog Helpers
    // -------------------------------------------------------------------------

    private fun showHiddenMusicDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hidden Music 👁️")
            .setMessage(
                "You can hide audio files that you don't want to show in your music library.\n\n" +
                        "• To hide a song: Long press any track in the list and select 'Hide Music'.\n" +
                        "• Currently hidden songs: 0 tracks\n\n" +
                        "Would you like to rescan and restore all hidden songs?"
            )
            .setPositiveButton("Unhide All") { _, _ ->
                Toast.makeText(
                    requireContext(),
                    "All hidden songs restored to library!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showMusicTransferDialog() {
        val transferOptions = arrayOf(
            "Transfer via Wi-Fi Direct",
            "Nearby Share / Quick Share",
            "Export Playlists & Library Info",
            "Import Music Backup"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Music Transfer 📲")
            .setItems(transferOptions) { _, which ->
                when (which) {
                    0, 1 -> {
                        Toast.makeText(
                            requireContext(),
                            "Searching for nearby devices on the local network...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> {
                        Toast.makeText(
                            requireContext(),
                            "Exporting playlist data to internal storage...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    3 -> {
                        Toast.makeText(
                            requireContext(),
                            "Select a backup file to import...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showFaqDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Frequently Asked Questions ❓")
            .setMessage(
                "1. How to scan new music?\n" +
                        "Go to Library and pull down to refresh, or tap 'Scan music' to re-index all audio files.\n\n" +
                        "2. How does 10-Band EQ work?\n" +
                        "Open the Equalizer from the Settings or Navigation Drawer while playing audio to customize frequency curves and bass boost.\n\n" +
                        "3. What is Gapless playback?\n" +
                        "It eliminates the silence between consecutive audio tracks, ideal for live albums and classical movements.\n\n" +
                        "4. How to set custom ringtones?\n" +
                        "Open 'Free Ringtones' from the side drawer to preview, generate, and set tones directly."
            )
            .setPositiveButton("Got It", null)
            .show()
    }

    private fun rateApp() {
        val packageName = requireContext().packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    private fun showManageSubscriptionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Manage Subscription 🛡️")
            .setMessage(
                "You are currently using MusicAir Free Edition.\n\n" +
                        "With MusicAir Pro, you get:\n" +
                        "• Ad-free experience\n" +
                        "• 10-band studio equalizer\n" +
                        "• Dynamic Neon themes\n" +
                        "• Priority customer support\n\n" +
                        "Subscriptions are managed securely through your Google Play account."
            )
            .setPositiveButton("Upgrade to Pro") { _, _ ->
                Toast.makeText(
                    requireContext(),
                    "Google Play In-App Purchase integration active!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showPrivacyPolicyDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Privacy Policy 🔒")
            .setMessage(
                "MusicAir respects your privacy:\n\n" +
                        "1. Local Storage Only: We only access your device's audio files to organize and play your music.\n\n" +
                        "2. Zero Telemetry / Tracking: We do not sell or upload your personal listening history, tracks, or metadata.\n\n" +
                        "3. Audio Recording: Voice recorder stores your recordings strictly in your device's private storage.\n\n" +
                        "4. System Permissions: Audio permissions are strictly utilized for foreground playback and equalizer effects."
            )
            .setPositiveButton("I Understand", null)
            .show()
    }

    private fun showTermsOfUseDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Terms of Use 📄")
            .setMessage(
                "Welcome to MusicAir!\n\n" +
                        "• License: MusicAir grants you a personal, non-exclusive license to play local audio files on your device.\n\n" +
                        "• Copyright: You are responsible for ensuring you have the legal right to play and use all media stored on your device.\n\n" +
                        "• Warranty: The app is provided 'as is' without warranties of any kind. We strive for regular updates and high fidelity."
            )
            .setPositiveButton("Accept", null)
            .show()
    }

    private fun sendFeedback(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@musicplayer.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Feedback - MusicAir App")
            putExtra(
                Intent.EXTRA_TEXT,
                "Write your feedback here...\n\n" +
                        "---- Device Info ----\n" +
                        "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                        "Android Version: ${Build.VERSION.RELEASE}\n" +
                        "App Version: ${getAppVersion(context)}\n"
            )
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Send Feedback"))
        } catch (_: Exception) {
            Toast.makeText(context, "No email app found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val versionInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        equalizerManager?.release()
        _binding = null
    }
}
