package com.sulav.fflikesender

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sulav.fflikesender.databinding.ActivityMainBinding
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var lastUrl = ""
    private var lastResponse = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sendButton.setOnClickListener { sendLike() }
        binding.copyUrlButton.setOnClickListener { copyText("Request URL", lastUrl) }
        binding.copyResponseButton.setOnClickListener { copyText("API Response", lastResponse) }
        binding.saveButton.setOnClickListener { saveResponse() }
    }

    private fun sendLike() {
        val uid = binding.uidInput.text?.toString()?.trim().orEmpty()
        val region = binding.regionInput.text?.toString()?.trim().orEmpty()

        if (uid.isEmpty()) {
            binding.uidLayout.error = "Enter a UID"
            return
        }
        binding.uidLayout.error = null

        if (region.isEmpty()) {
            binding.regionLayout.error = "Enter a region"
            return
        }
        binding.regionLayout.error = null

        val encodedUid = URLEncoder.encode(uid, StandardCharsets.UTF_8.toString())
        val encodedRegion = URLEncoder.encode(region, StandardCharsets.UTF_8.toString())
        lastUrl = "https://like.sulavcodex.com/like?uid=$encodedUid&server_name=$encodedRegion&key=SulavOp"

        setLoading(true)
        binding.urlText.text = lastUrl
        binding.statusText.text = "Sending request…"

        thread {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(lastUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.setRequestProperty("Accept", "application/json, text/plain, */*")
                connection.setRequestProperty("User-Agent", "FFLikeSender/1.0")
                val code = connection.responseCode
                val stream = if (code in 200..399) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                lastResponse = "HTTP $code\n$body"

                runOnUiThread {
                    binding.responseText.text = lastResponse
                    binding.statusText.text = if (code in 200..299) "Request successful" else "Request returned HTTP $code"
                    setLoading(false)
                }
            } catch (e: Exception) {
                lastResponse = "ERROR\n${e.message ?: "Unknown network error"}"
                runOnUiThread {
                    binding.responseText.text = lastResponse
                    binding.statusText.text = "Request failed"
                    setLoading(false)
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.sendButton.isEnabled = !loading
        binding.sendButton.text = if (loading) "SENDING…" else "SEND LIKE"
    }

    private fun copyText(label: String, value: String) {
        if (value.isEmpty()) {
            Toast.makeText(this, "Nothing to copy yet", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
    }

    private fun saveResponse() {
        if (lastResponse.isEmpty()) {
            Toast.makeText(this, "Send a request first", Toast.LENGTH_SHORT).show()
            return
        }

        val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir == null) {
            Toast.makeText(this, "Storage unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(dir, "ff_like_response_${System.currentTimeMillis()}.txt")
        try {
            file.writeText(
                "FF Like Sender\nOwner: @zestyji\n\nRequest URL:\n$lastUrl\n\nResponse:\n$lastResponse",
                Charsets.UTF_8
            )
            MaterialAlertDialogBuilder(this)
                .setTitle("Saved successfully")
                .setMessage(file.absolutePath)
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not save file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
