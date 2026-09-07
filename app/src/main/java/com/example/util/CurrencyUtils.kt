package com.example.util

import java.text.NumberFormat
import java.util.Locale

val BR_LOCALE: Locale = Locale.forLanguageTag("pt-BR")

private val brCurrencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(BR_LOCALE).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

/**
 * Formata um valor numérico para a moeda brasileira (ex: R$ 1.500,50).
 */
fun Double.formatBr(): String {
    return try {
        brCurrencyFormat.format(this)
    } catch (e: Exception) {
        String.format(BR_LOCALE, "R$ %,.2f", this)
    }
}

/**
 * Formata um valor numérico para exibição sem o símbolo R$ (ex: 1.500,50).
 */
fun Double.formatBrNumber(): String {
    return String.format(BR_LOCALE, "%,.2f", this)
}

/**
 * Formata um valor numérico inteiro (ex: R$ 1.500).
 */
fun Double.formatBrCompact(): String {
    return String.format(BR_LOCALE, "R$ %,.0f", this)
}

/**
 * Converte uma string de valor em formato brasileiro ou americano para Double.
 * Aceita "1.500,50", "1500,50", "1500.50", "R$ 1.500,50", etc.
 */
fun String.parseBrDouble(): Double? {
    val clean = this.replace("R$", "")
        .replace("BRL", "")
        .replace(" ", "")
        .trim()
    if (clean.isBlank()) return null
    return if (clean.contains(",") && clean.contains(".")) {
        // Formato brasileiro com milhar: 1.250,50
        clean.replace(".", "").replace(",", ".").toDoubleOrNull()
    } else if (clean.contains(",")) {
        // Formato com vírgula decimal: 1250,50
        clean.replace(",", ".").toDoubleOrNull()
    } else {
        // Formato normal: 1250.50
        clean.toDoubleOrNull()
    }
}

// Funções top-level com @JvmName para evitar colisões no bytecode Java
@JvmName("formatBrFn")
fun formatBr(value: Double): String = value.formatBr()

@JvmName("formatBrNumberFn")
fun formatBrNumber(value: Double): String = value.formatBrNumber()

@JvmName("formatBrCompactFn")
fun formatBrCompact(value: Double): String = value.formatBrCompact()

@JvmName("parseBrDoubleFn")
fun parseBrDouble(text: String): Double = text.parseBrDouble() ?: 0.0

object CurrencyUtils {
    fun formatBr(value: Double): String = value.formatBr()
    fun formatBrNumber(value: Double): String = value.formatBrNumber()
    fun formatBrCompact(value: Double): String = value.formatBrCompact()
    fun parseBrDouble(text: String): Double = text.parseBrDouble() ?: 0.0
}
