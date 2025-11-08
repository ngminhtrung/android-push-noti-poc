package com.travisnguyen.pushpocc

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.travisnguyen.pushpocc.data.MessageStore
import com.travisnguyen.pushpocc.databinding.ActivityMainBinding
import com.travisnguyen.pushpocc.messaging.PushMessagingService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PushMessagingService.ACTION_TOKEN_REFRESHED -> {
                    val token = intent.getStringExtra(PushMessagingService.EXTRA_TOKEN)
                    binding.tokenText.text = token ?: getString(R.string.token_error)
                }

                PushMessagingService.ACTION_NEW_MESSAGE -> {
                    binding.messageLogText.text = MessageStore.getMessages(context)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.refreshTokenButton.setOnClickListener { fetchToken() }
        binding.tokenText.text = MessageStore.getToken(this) ?: getString(R.string.token_pending)
        binding.messageLogText.text = MessageStore.getMessages(this)

        maybeRequestNotificationPermission()
        fetchToken()
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                messageReceiver,
                IntentFilter().apply {
                    addAction(PushMessagingService.ACTION_TOKEN_REFRESHED)
                    addAction(PushMessagingService.ACTION_NEW_MESSAGE)
                }
            )
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
        super.onStop()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val currentStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (currentStatus == PackageManager.PERMISSION_GRANTED) return

        val shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        if (shouldShowRationale) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.notification_permission_rationale)
                .setPositiveButton("OK") { _, _ ->
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun fetchToken() {
        binding.tokenText.text = getString(R.string.token_pending)
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    binding.tokenText.text = getString(R.string.token_error)
                    Log.e(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                binding.tokenText.text = token
                MessageStore.saveToken(this, token)
                Log.d(TAG, "FCM token: $token")
            }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
