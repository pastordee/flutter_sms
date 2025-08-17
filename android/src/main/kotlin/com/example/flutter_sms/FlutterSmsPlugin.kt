package com.example.flutter_sms

import android.annotation.TargetApi
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager

class FlutterSmsPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    companion object {
        private const val REQUEST_CODE_SEND_SMS = 1001
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_sms")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when {
            call.method == "sendSMS" -> {
                if (!canSendSMS()) {
                    result.error(
                        "device_not_capable",
                        "The current device is not capable of sending text messages.",
                        "A device may be unable to send messages if it does not support messaging or if it is not currently configured to send messages. This only applies to the ability to send text messages via iMessage, SMS, and MMS."
                    )
                    return
                }
                val message = call.argument<String?>("message")
                val recipients = call.argument<String?>("recipients")
                sendSMS(result, recipients, message!!)
                result.success("SMS Sent!")
            }
            call.method == "canSendSMS" -> result.success(canSendSMS())
            else -> result.notImplemented()
        }
    }

    private fun canSendSMS(): Boolean {
        val packageManager = activity?.packageManager ?: return false
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            return false
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:")
        val activityInfo = intent.resolveActivityInfo(packageManager, intent.flags)
        return !(activityInfo == null || !activityInfo.exported)
    }

    private fun sendSMS(result: Result, phones: String?, message: String?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$phones")
        intent.putExtra("sms_body", message)
        intent.putExtra(Intent.EXTRA_TEXT, message)
        activity?.startActivity(intent) // Changed from startActivityForResult to startActivity
    }
}