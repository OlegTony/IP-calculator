package com.oleg.ipcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import kotlin.math.pow
import androidx.core.graphics.toColorInt
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var tvPublicIp: TextView
    private var publicIp = ""

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)

            lifecycleScope.launch {
                try {
                    publicIp = withContext(Dispatchers.IO) {
                        getPublicIp()
                    }

                    tvPublicIp.text = getString(R.string.public_ip_loading) + " " + publicIp

                } catch (e: Exception) {
                    publicIp = ""
                    tvPublicIp.text = getString(R.string.public_ip_error)
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            tvPublicIp.text = getString(R.string.public_ip_error)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        tvPublicIp = findViewById(R.id.tvPublicIp)


        lifecycleScope.launch {
            try {
                publicIp = withContext(Dispatchers.IO) {
                    getPublicIp()
                }

                tvPublicIp.text = getString(R.string.public_ip_loading) + " " + publicIp

            } catch (e: Exception) {
                publicIp = ""
                tvPublicIp.text = getString(R.string.public_ip_error)
            }
        }

        tvPublicIp.setOnClickListener {
            if (publicIp.isEmpty()) {
                return@setOnClickListener
            }
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Public IP", publicIp)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                getString(R.string.public_ip_copied),
                Toast.LENGTH_SHORT
            ).show()
        }

        val sharedPreferences = getSharedPreferences("ip_calculator_prefs", Context.MODE_PRIVATE)

        val etIpAddress = findViewById<EditText>(R.id.etIpAddress)
        val etPrefix = findViewById<EditText>(R.id.etPrefix)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnCopy = findViewById<Button>(R.id.btnCopy)
        val btnHistory = findViewById<Button>(R.id.btnHistory)


        val tvNetmask = findViewById<TextView>(R.id.tvNetmask)
        val tvNetworkId = findViewById<TextView>(R.id.tvNetworkId)
        val tvBroadcast = findViewById<TextView>(R.id.tvBroadcast)
        val tvFirstHost = findViewById<TextView>(R.id.tvFirstHost)
        val tvLastHost = findViewById<TextView>(R.id.tvLastHost)
        val tvUsableHosts = findViewById<TextView>(R.id.tvUsableHosts)
        val tvNetworkClass = findViewById<TextView>(R.id.tvNetworkClass)
        val tvNetworkType = findViewById<TextView>(R.id.tvNetworkType)
        val tvWildcard = findViewById<TextView>(R.id.tvWildcard)

        val placeholder = getString(R.string.default_placeholder)
        tvNetmask.text = getString(R.string.label_subnet_mask, placeholder, placeholder)
        tvNetworkClass.text = getString(R.string.label_network_class, placeholder)
        tvNetworkType.text = getString(R.string.label_network_type, placeholder)

        btnCalculate.setOnClickListener {
            val ipString = etIpAddress.text.toString().trim()
            val prefixString = etPrefix.text.toString().trim()

            if (ipString.isEmpty() || prefixString.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val prefix = prefixString.toIntOrNull()
            if (prefix == null || prefix < 0 || prefix > 32) {
                Toast.makeText(this, getString(R.string.error_invalid_prefix), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val ipInt = parseIpToBytes(ipString)
            if (ipInt == null) {
                Toast.makeText(this, getString(R.string.error_invalid_ip), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val maskInt = if (prefix == 0) 0 else (-1 shl (32 - prefix))
            val networkIdInt = ipInt and maskInt
            val broadcastInt = networkIdInt or maskInt.inv()

            val firstHostString: String
            val lastHostString: String
            val hostCount: Long

            if (prefix >= 31) {
                val naString = getString(R.string.not_available)
                firstHostString = naString
                lastHostString = naString
                hostCount = 0
            } else {
                firstHostString = intToIpString(networkIdInt + 1)
                lastHostString = intToIpString(broadcastInt - 1)
                hostCount = 2.0.pow(32 - prefix).toLong() - 2
            }

            hideKeyboard()

            val netmaskString =
                getString(R.string.label_subnet_mask, intToIpString(maskInt), prefix.toString())
            tvNetmask.text = netmaskString
            tvNetworkId.text = intToIpString(networkIdInt)
            tvBroadcast.text = intToIpString(broadcastInt)
            tvFirstHost.text = firstHostString
            tvLastHost.text = lastHostString
            tvUsableHosts.text = hostCount.toString()

            val wildcardInt = maskInt.inv()
            tvWildcard.text = intToIpString(wildcardInt)

            val firstOctet = (ipInt shr 24) and 0xFF
            val networkClass = when (firstOctet) {
                in 1..126 -> "A"
                127 -> "A (Loopback)"
                in 128..191 -> "B"
                in 192..223 -> "C"
                in 224..239 -> "D (Multicast)"
                else -> "E (Experimental)"
            }
            tvNetworkClass.text = getString(R.string.label_network_class, networkClass)

            val secondOctet = (ipInt shr 16) and 0xFF
            val isPrivate = when {
                firstOctet == 10 -> true
                firstOctet == 172 && secondOctet in 16..31 -> true
                firstOctet == 192 && secondOctet == 168 -> true
                else -> false
            }

            val typeString =
                if (isPrivate) getString(R.string.type_private) else getString(R.string.type_public)
            tvNetworkType.text = getString(R.string.label_network_type, typeString)

            val historyEntry = "$ipString/$prefix -> ID: ${intToIpString(networkIdInt)} | Mask: ${
                intToIpString(maskInt)
            }"
            val currentHistorySet =
                sharedPreferences.getStringSet("calculation_history", emptySet()) ?: emptySet()
            val currentHistoryList = currentHistorySet.toMutableList()
            currentHistoryList.remove(historyEntry)
            currentHistoryList.add(0, historyEntry)

            if (currentHistoryList.size > 10) {
                currentHistoryList.removeAt(currentHistoryList.size - 1)
            }

            sharedPreferences.edit {
                putStringSet("calculation_history", currentHistoryList.toSet())
            }
        }
        btnClear.setOnClickListener {
            etIpAddress.text.clear()
            etPrefix.text.clear()
            tvNetmask.text = getString(R.string.label_subnet_mask, placeholder, placeholder)
            tvNetworkClass.text = getString(R.string.label_network_class, placeholder)
            tvNetworkType.text = getString(R.string.label_network_type, placeholder)
            tvNetworkId.text = placeholder
            tvBroadcast.text = placeholder
            tvFirstHost.text = placeholder
            tvLastHost.text = placeholder
            tvUsableHosts.text = placeholder
            tvWildcard.text = placeholder
        }

        btnCopy.setOnClickListener {
            if (tvNetworkId.text.toString() == placeholder) {
                Toast.makeText(
                    this,
                    getString(R.string.msg_nothing_to_copy),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val resultText = """
            ${getString(R.string.copy_ip_prefix)}: ${etIpAddress.text}/${etPrefix.text}
            ${getString(R.string.copy_subnet_mask)}: ${tvNetmask.text}
            ${getString(R.string.copy_network_id)}: ${tvNetworkId.text}
            ${getString(R.string.copy_broadcast)}: ${tvBroadcast.text}
            ${getString(R.string.copy_first_host)}: ${tvFirstHost.text}
            ${getString(R.string.copy_last_host)}: ${tvLastHost.text}
            ${getString(R.string.copy_total_hosts)}: ${tvUsableHosts.text}
            ${getString(R.string.copy_wildcard)}: ${tvWildcard.text}
         """.trimIndent()

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("IP Calculator Result", resultText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                this,
                getString(R.string.msg_copied),
                Toast.LENGTH_SHORT
            ).show()
        }

        btnHistory.setOnClickListener {
            val historySet = sharedPreferences.getStringSet(
                "calculation_history",
                emptySet()
            ) ?: emptySet()

            if (historySet.isEmpty()) {
                showCustomToast(getString(R.string.history_empty))
                return@setOnClickListener
            }

            val historyArray = historySet.toTypedArray()

            val adapter = android.widget.ArrayAdapter(
                this,
                R.layout.item_history,
                historyArray
            )

            // 1. Создаем кастомный заголовок
            val customTitle = TextView(this)
            customTitle.text = getString(R.string.title_history)
            customTitle.setTextColor("#FF8800".toColorInt())
            customTitle.textSize = 20f
            customTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
            customTitle.gravity = android.view.Gravity.CENTER
            customTitle.setPadding(
                0,
                (2 * resources.displayMetrics.density).toInt(),
                0,
                (2 * resources.displayMetrics.density).toInt()
            )

            // 2. Строим диалог
            val builder = AlertDialog.Builder(
                this,
                android.R.style.Theme_DeviceDefault_Dialog_Alert
            )
                .setCustomTitle(customTitle)
                .setAdapter(adapter, null)
                .setPositiveButton(getString(R.string.button_ok), null)
                .setNegativeButton(getString(R.string.btn_clear)) { _, _ ->
                    sharedPreferences.edit {
                        remove("calculation_history")
                    }
                    showCustomToast(getString(R.string.history_cleared))
                }

            val dialog = builder.create()
            dialog.show()

            // 3. Красим нижние кнопки управления
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor("#FF8800".toColorInt())

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor("#FF8800".toColorInt())
        }
    }

    override fun onDestroy() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.unregisterNetworkCallback(networkCallback)

        super.onDestroy()
    }

    private fun parseIpToBytes(ipString: String): Int? {
        val parts = ipString.split(".")
        if (parts.size != 4) return null
        return try {
            var ipInt = 0
            for (i in 0..3) {
                val part = parts[i].toInt()
                if (part !in 0..255) return null
                ipInt = (ipInt shl 8) or part
            }
            ipInt
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun intToIpString(ipInt: Int): String {
        return String.format(
            java.util.Locale.US,
            "%d.%d.%d.%d",
            (ipInt shr 24) and 0xFF,
            (ipInt shr 16) and 0xFF,
            (ipInt shr 8) and 0xFF,
            ipInt and 0xFF
        )
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // вне метода onCreate, на уровне класса
    private fun showCustomToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private suspend fun getPublicIp(): String {
        val url = URL("https://api.ipify.org")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val result = connection.inputStream.bufferedReader().use {
            it.readText()
        }

        connection.disconnect()

        return result
    }

    } // СКОБКА ФАЙЛА, ЗАКРЫВАЮЩАЯ КЛАСС.
