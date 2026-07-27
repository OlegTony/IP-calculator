package com.oleg.ipcalculator

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

        // 2. Инициализация элементов вывода результатов
        val tvNetmask = findViewById<TextView>(R.id.tvNetmask)
        val tvNetworkId = findViewById<TextView>(R.id.tvNetworkId)
        val tvBroadcast = findViewById<TextView>(R.id.tvBroadcast)
        val tvFirstHost = findViewById<TextView>(R.id.tvFirstHost)
        val tvLastHost = findViewById<TextView>(R.id.tvLastHost)
        val tvUsableHosts = findViewById<TextView>(R.id.tvUsableHosts)

        btnCalculate.setOnClickListener {
            val ipString = etIpAddress.text.toString().trim()
            val prefixString = etPrefix.text.toString().trim()

            // Проверка на пустые поля (Берем строку из ресурсов)
            if (ipString.isEmpty() || prefixString.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Валидация префикса CIDR (Берем строку из ресурсов)
            val prefix = prefixString.toIntOrNull()
            if (prefix == null || prefix < 0 || prefix > 32) {
                Toast.makeText(this, getString(R.string.error_invalid_prefix), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Парсинг IP-адреса в число (Берем строку из ресурсов)
            val ipInt = parseIpToBytes(ipString)
            if (ipInt == null) {
                Toast.makeText(this, getString(R.string.error_invalid_ip), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Вычисление маски подсети по префиксу
            val maskInt = if (prefix == 0) 0 else (-1 shl (32 - prefix))

            // Побитовые операции (AND и NOT) для расчетов
            val networkIdInt = ipInt and maskInt
            val broadcastInt = networkIdInt or maskInt.inv()

            // Расчет диапазона хостов по сисадминским стандартам
            val firstHostString: String
            val lastHostString: String
            val hostCount: Long

            if (prefix >= 31) {
                // В сетях /31 (RFC 3021) и /32 хосты рассчитываются как исключение (Берем строку из ресурсов)
                val naString = getString(R.string.not_available)
                firstHostString = naString
                lastHostString = naString
                hostCount = 0
            } else {
                // Стандартный расчет диапазона: NetworkID + 1 и Broadcast - 1
                firstHostString = intToIpString(networkIdInt + 1)
                lastHostString = intToIpString(broadcastInt - 1)
                hostCount = 2.0.pow(32 - prefix).toLong() - 2
            }

            // 3. Вывод результатов в отдельные ячейки интерфейса
            tvNetmask.text = intToIpString(maskInt)
            tvNetworkId.text = intToIpString(networkIdInt)
            tvBroadcast.text = intToIpString(broadcastInt)
            tvFirstHost.text = firstHostString
            tvLastHost.text = lastHostString
            tvUsableHosts.text = hostCount.toString()
        }
    }

    // Функция перевода строки IP в 32-битное целое число
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

    // Функция перевода 32-битного целого числа обратно в строку IP формата X.X.X.X
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
