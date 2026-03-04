package helium314.keyboard.latin.voice

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

/**
 * Transparent trampoline activity to request RECORD_AUDIO permission.
 * IME services cannot call requestPermissions() directly, so we launch
 * this lightweight activity which requests the permission and finishes.
 */
class VoicePermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // Show rationale if previously denied
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    AlertDialog.Builder(this)
                        .setTitle("Microphone Permission")
                        .setMessage("Voice input needs microphone access to transcribe your speech into text. Audio is processed locally on your device by default — nothing is sent to the internet unless you enable cloud mode.")
                        .setPositiveButton("Allow") { _, _ ->
                            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE)
                        }
                        .setNegativeButton("Not Now") { _, _ ->
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE)
                }
                return
            }
        }
        // Permission already granted
        setResult(RESULT_OK)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setResult(RESULT_OK)
            } else {
                setResult(RESULT_CANCELED)
            }
        }
        finish()
    }

    companion object {
        private const val REQUEST_CODE = 1001

        fun hasRecordPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun createIntent(context: Context): Intent {
            return Intent(context, VoicePermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
