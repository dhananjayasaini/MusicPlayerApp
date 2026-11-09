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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.dhananjaysaini.musicplayerapp.databinding.FragmentSettingsBinding
import com.dhananjaysaini.musicplayerapp.repository.ThemeRepository
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.EqualizerManager
import com.dhananjaysaini.musicplayerapp.utils.SleepTimerBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.utils.ThemeSelectionBottomSheet

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

        setupProBanner()
        setupAppearance()
        setupAudioFx()
        setupAboutAndFeedback()
    }

    private fun setupProBanner() {
        binding.cardProBanner.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("MusicAir Pro ✨")
                .setMessage("Upgrade to MusicAir Pro for:\n\n• 10-Band Studio Equalizer & Bass Boost\n• Ultra HD Ambient Glow & Visualizer\n• Dynamic Palette Art Tinting\n• 100% Ad-Free Experience\n• Lifetime Updates & Support")
                .setPositiveButton("Upgrade") { _, _ ->
                    Toast.makeText(requireContext(), "In-app purchases coming soon to Google Play!", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Maybe Later", null)
                .show()
        }
    }

    private fun setupAppearance() {
        binding.btnSettingTheme.setOnClickListener {
            startActivity(Intent(requireContext(), com.dhananjaysaini.musicplayerapp.activities.SkinThemeActivity::class.java))
        }

        val sharedPref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDark = sharedPref.getBoolean("dark_mode", false)
        binding.switchDarkMode.isChecked = isDark
        binding.tvDarkModeStatus.text = if (isDark) "On" else "Off"

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            binding.tvDarkModeStatus.text = if (isChecked) "On" else "Off"

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                val currentTheme = ThemeManager.getCurrentTheme(requireContext())
                if (!currentTheme.isDark) {
                    val darkTheme = ThemeRepository.findThemeById("color_2") ?: ThemeRepository.getColorThemes()[1]
                    ThemeManager.saveTheme(requireContext(), darkTheme)
                } else {
                    ThemeManager.saveTheme(requireContext(), currentTheme)
                }
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                val currentTheme = ThemeManager.getCurrentTheme(requireContext())
                if (currentTheme.isDark) {
                    val lightTheme = ThemeRepository.findThemeById("color_6") ?: ThemeRepository.getColorThemes()[5]
                    ThemeManager.saveTheme(requireContext(), lightTheme)
                } else {
                    ThemeManager.saveTheme(requireContext(), currentTheme)
                }
            }
            requireActivity().recreate()
        }
    }

    private fun setupAudioFx() {
        equalizerManager = EqualizerManager(requireContext())

        binding.btnSettingEqualizer.setOnClickListener {
            val sessionId = MusicService.mediaPlayer?.audioSessionId ?: -1
            if (sessionId != -1) {
                equalizerManager?.showEqualizer(sessionId)
            } else {
                Toast.makeText(requireContext(), "Play a song first to open the Equalizer!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSettingSleepTimer.setOnClickListener {
            val bottomSheet = SleepTimerBottomSheet()
            bottomSheet.show(parentFragmentManager, "SleepTimerBottomSheet")
        }
    }

    private fun setupAboutAndFeedback() {
        val appVersion = getAppVersion(requireContext())
        binding.tvVersionTag.text = "v$appVersion"

        binding.btnSettingFeedback.setOnClickListener {
            sendFeedback(requireContext())
        }

        binding.btnSettingAbout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("About MusicAir")
                .setMessage("MusicAir Audio Player\nVersion: $appVersion\n\nExperience high-fidelity local music playback with dynamic visuals, fluid animations, and custom equalizers.\n\nCrafted with ❤️ for music lovers.")
                .setPositiveButton("OK", null)
                .show()
        }
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
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val versionInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        equalizerManager?.release()
        _binding = null
    }
}
