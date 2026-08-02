package com.oleg.ipcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Инициализация элементов ввода данных
        val etIpAddress = findViewById<EditText>(R.id.etIpAddress)
        val etPrefix = findViewById<EditText>(R.id.etPrefix)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)

        // Кнопки из Шага 2
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnCopy = findViewById<Button>(R.id.btnCopy)

        // 2. Инициализация элементов вывода результатов
        val tvNetmask = findViewById<TextView>(R.id.tvNetmask)
        val tvNetworkId = findViewById<TextView>(R.id.tvNetworkId)
        val tvBroadcast = findViewById<TextView>(R.id.tvBroadcast)
        val tvFirstHost = findViewById<TextView>(R.id.tvFirstHost)
        val tvLastHost = findViewById<TextView>(R.id.tvLastHost)
        val tvUsableHosts = findViewById<TextView>(R.id.tvUsableHosts)

        // Инициализация начального состояния маски с прочерками при старте приложения
        val placeholder = getString(R.string.default_placeholder)
        tvNetmask.text = getString(R.string.label_subnet_mask, placeholder, placeholder)

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

            // ИСПРАВЛЕНО: Передаем маску и префикс как аргументы в шаблон строки ресурсов
            tvNetmask.text = getString(R.string.label_subnet_mask, intToIpString(maskInt), prefix.toString())

            tvNetworkId.text = intToIpString(networkIdInt)
            tvBroadcast.text = intToIpString(broadcastInt)
            tvFirstHost.text = firstHostString
            tvLastHost.text = lastHostString
            tvUsableHosts.text = hostCount.toString()
        }

        // Кнопка "Очистить" — сбрасывает ввод и возвращает плейсхолдеры
        btnClear.setOnClickListener {
            etIpAddress.text.clear()
            etPrefix.text.clear()

            // ИСПРАВЛЕНО: Возвращаем маску к дефолтному шаблону с прочерками
            tvNetmask.text = getString(R.string.label_subnet_mask, placeholder, placeholder)

            tvNetworkId.text = placeholder
            tvBroadcast.text = placeholder
            tvFirstHost.text = placeholder
            tvLastHost.text = placeholder
            tvUsableHosts.text = placeholder
        }

        // Кнопка "Копировать" — собирает результаты в буфер обмена
        btnCopy.setOnClickListener {
            // Если расчет еще не производился (в тексте маски есть прочерк), копировать нечего
            if (tvNetmask.text.contains(placeholder)) {
                Toast.makeText(this, getString(R.string.msg_nothing_to_copy), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Формируем красивый структурированный текст для буфера
            val shareText = """
                ${getString(R.string.title_analysis_results)}
                IP: ${etIpAddress.text} /${etPrefix.text}
                ${tvNetmask.text}
                ${getString(R.string.label_network_id)} ${tvNetworkId.text}
                ${getString(R.string.label_broadcast_address)} ${tvBroadcast.text}
                ${getString(R.string.label_first_usable_host)} ${tvFirstHost.text}
                ${getString(R.string.label_last_usable_host)} ${tvLastHost.text}
                ${getString(R.string.label_total_usable_hosts)} ${tvUsableHosts.text}
            """.trimIndent()

            // Копируем в системный буфер обмена Android
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("IP Calculator Results", shareText)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, getString(R.string.msg_copied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseIpToBytes(ipStr: String): Int? {
        val parts = ipStr.split(".")
        if (parts.size != 4) return null
        return try {
            var ip = 0
            for (i in 0..3) {
                val part = parts[i].toInt()
                if (part < 0 || part > 255) return null
                ip = (ip shl 8) or part
            }
            ip
        } catch (e: NumberFormatException) {
            null
        }
    }

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
}
