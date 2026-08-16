package com.oleg.ipcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация SharedPreferences для хранения истории
        val sharedPreferences = getSharedPreferences("ip_calculator_prefs", Context.MODE_PRIVATE)

        // 1. Инициализация элементов ввода данных
        val etIpAddress = findViewById<EditText>(R.id.etIpAddress)
        val etPrefix = findViewById<EditText>(R.id.etPrefix)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)

        // Кнопки из Шага 2 и Шага 5
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnCopy = findViewById<Button>(R.id.btnCopy)
        val btnHistory = findViewById<Button>(R.id.btnHistory)

        // 2. Инициализация элементов вывода результатов
        val tvNetmask = findViewById<TextView>(R.id.tvNetmask)
        val tvNetworkId = findViewById<TextView>(R.id.tvNetworkId)
        val tvBroadcast = findViewById<TextView>(R.id.tvBroadcast)
        val tvFirstHost = findViewById<TextView>(R.id.tvFirstHost)
        val tvLastHost = findViewById<TextView>(R.id.tvLastHost)
        val tvUsableHosts = findViewById<TextView>(R.id.tvUsableHosts)
        val tvNetworkClass = findViewById<TextView>(R.id.tvNetworkClass)
        val tvNetworkType = findViewById<TextView>(R.id.tvNetworkType)

        // Инициализация начального состояния полей шаблонами с прочерками
        val placeholder = getString(R.string.default_placeholder)
        tvNetmask.text = getString(R.string.label_subnet_mask, placeholder, placeholder)
        tvNetworkClass.text = getString(R.string.label_network_class, placeholder)
        tvNetworkType.text = getString(R.string.label_network_type, placeholder)

        // Кнопка "Рассчитать"
        btnCalculate.setOnClickListener {
            val ipString = etIpAddress.text.toString().trim()
            val prefixString = etPrefix.text.toString().trim()

            if (ipString.isEmpty() || prefixString.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefix = prefixString.toIntOrNull()
            if (prefix == null || prefix < 0 || prefix > 32) {
                Toast.makeText(this, getString(R.string.error_invalid_prefix), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ipInt = parseIpToBytes(ipString)
            if (ipInt == null) {
                Toast.makeText(this, getString(R.string.error_invalid_ip), Toast.LENGTH_SHORT).show()
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

            // Выводим основные результаты расчетов
            val netmaskString = getString(R.string.label_subnet_mask, intToIpString(maskInt), prefix.toString())
            tvNetmask.text = netmaskString
            tvNetworkId.text = intToIpString(networkIdInt)
            tvBroadcast.text = intToIpString(broadcastInt)
            tvFirstHost.text = firstHostString
            tvLastHost.text = lastHostString
            tvUsableHosts.text = hostCount.toString()

            // Блок аналитики
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

            val typeString = if (isPrivate) getString(R.string.type_private) else getString(R.string.type_public)
            tvNetworkType.text = getString(R.string.label_network_type, typeString)

            // --- СОХРАНЕНИЕ В ИСТОРИЮ ---
            val historyEntry = "$ipString/$prefix -> ID: ${intToIpString(networkIdInt)} | Mask: ${intToIpString(maskInt)}"
            val currentHistory = sharedPreferences.getStringSet("calculation_history", emptySet())?.toMutableList() ?: mutableListOf()

            currentHistory.remove(historyEntry)
            currentHistory.add(0, historyEntry)

            if (currentHistory.size > 10) {
                currentHistory.removeAt(currentHistory.size - 1)
            }

            sharedPreferences.edit {
                putStringSet("calculation_history", currentHistory.toSet())
            }
        } // Скобка закрывает btnCalculate

        // Кнопка "Очистить" — сбрасывает ввод и возвращает плейсхолдеры
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
        }

        // Кнопка "Копировать" — собирает результаты в буфер обмена
        btnCopy.setOnClickListener {
            if (tvNetmask.text.contains(placeholder)) {
                Toast.makeText(this, getString(R.string.msg_nothing_to_copy), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val shareText = """
                ${getString(R.string.title_analysis_results)}
                IP: ${etIpAddress.text} /${etPrefix.text}
                ${tvNetmask.text}
                ${getString(R.string.label_network_id)} ${tvNetworkId.text}
                ${getString(R.string.label_broadcast_address)} ${tvBroadcast.text}
                ${getString(R.string.label_first_usable_host)} ${tvFirstHost.text}
                ${getString(R.string.label_last_usable_host)} ${tvLastHost.text}
                ${getString(R.string.label_total_usable_hosts)} ${tvUsableHosts.text}
                ${tvNetworkClass.text}
                ${tvNetworkType.text}
            """.trimIndent()

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("IP Calculator Results", shareText)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, getString(R.string.msg_copied), Toast.LENGTH_SHORT).show()
        }

        // Кнопка "История" — показывает диалоговое окно
        btnHistory.setOnClickListener {
            val historySet = sharedPreferences.getStringSet("calculation_history", emptySet()) ?: emptySet()
            val historyList = historySet.toList()

            if (historyList.isEmpty()) {
                Toast.makeText(this, "История пуста", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("История расчетов")
                .setItems(historyList.toTypedArray(), null)
                .setPositiveButton("ОК", null)
                .setNeutralButton("Очистить") { _, _ ->
                    sharedPreferences.edit { remove("calculation_history") }
                    Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    } // Скобка закрывает метод onCreate

    // Вспомогательная функция для парсинга IP
    private fun parseIpToBytes(ipString: String): Int? {
        val parts = ipString.split(".")
        if (parts.size != 4) return null
        return try {
            var ip = 0
            for (i in 0..3) {
                val part = parts[i].toInt()
                if (part !in 0..255) return null
                ip = (ip shl 8) or part
            }
            ip
        } catch (e: Exception) {
            null
        }
    }

    // Вспомогательная функция для перевода Int в строку IP
    private fun intToIpString(ipInt: Int): String {
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            (ipInt shr 24) and 0xFF,
            (ipInt shr 16) and 0xFF,
            (ipInt shr 8) and 0xFF,
            ipInt and 0xFF
        )
    }
} // Скобка закрывает класс MainActivity
