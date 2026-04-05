package com.alex.hopper.ocr

import kotlin.math.max

data class ExtractedNumbers(
    val primaryNumber: String?,
    val allNumbers: List<String>,
)

class WagonNumberExtractor {
    private val chunkRegex = Regex("[0-9OОIILSZBВ\\s\\-]{6,18}")

    fun extract(result: OcrResult): ExtractedNumbers {
        val ranked = linkedMapOf<String, RankedCandidate>()
        val lines = result.lines.ifEmpty {
            result.fullText.lines().map(String::trim).filter(String::isNotEmpty)
        }

        lines.forEachIndexed { lineIndex, line ->
            collectCandidates(line).forEachIndexed { order, weighted ->
                val score = scoreCandidate(
                    value = weighted.value,
                    lineIndex = lineIndex,
                    baseWeight = weighted.weight,
                )
                val previous = ranked[weighted.value]
                if (previous == null || score > previous.score) {
                    ranked[weighted.value] = RankedCandidate(
                        value = weighted.value,
                        score = score,
                        lineIndex = lineIndex,
                        order = order,
                    )
                }
            }
        }

        val ordered = ranked.values
            .sortedWith(
                compareByDescending<RankedCandidate> { it.score }
                    .thenBy { it.lineIndex }
                    .thenBy { it.order },
            )
            .map { it.value }
            .take(6)

        return ExtractedNumbers(
            primaryNumber = ordered.firstOrNull(),
            allNumbers = ordered,
        )
    }

    private fun collectCandidates(rawLine: String): List<WeightedCandidate> {
        val normalized = normalize(rawLine)
        val candidates = mutableListOf<WeightedCandidate>()

        fun consider(rawCandidate: String, weight: Int) {
            val digits = rawCandidate.filter(Char::isDigit)
            if (digits.length !in 6..12) return

            candidates += WeightedCandidate(digits, weight)

            if (digits.length >= 8) {
                candidates += WeightedCandidate(digits.take(8), weight + 30)
                candidates += WeightedCandidate(digits.takeLast(8), weight + 12)
            }

            if (digits.length == 10) {
                candidates += WeightedCandidate(digits.dropLast(2), weight + 40)
            }
        }

        chunkRegex.findAll(normalized).forEach { match ->
            consider(match.value, 45)
        }
        consider(normalized, 20)

        return candidates.distinctBy { it.value }
    }

    private fun normalize(text: String): String = buildString(text.length) {
        text.uppercase().forEach { symbol ->
            append(
                when (symbol) {
                    'O', 'О' -> '0'
                    'I', 'L', '|' -> '1'
                    'S' -> '5'
                    'Z', 'З' -> '3'
                    'B', 'В' -> '8'
                    else -> symbol
                },
            )
        }
    }

    private fun scoreCandidate(
        value: String,
        lineIndex: Int,
        baseWeight: Int,
    ): Int {
        var score = baseWeight

        score += when (value.length) {
            8 -> 120
            7, 9 -> 84
            10 -> 60
            else -> 28
        }

        score -= lineIndex * 4
        score -= max(0, value.length - 8) * 6

        if (value.toSet().size <= 2) {
            score -= 16
        }
        if (value.startsWith("20")) {
            score -= 10
        }

        return score
    }

    private data class WeightedCandidate(
        val value: String,
        val weight: Int,
    )

    private data class RankedCandidate(
        val value: String,
        val score: Int,
        val lineIndex: Int,
        val order: Int,
    )
}
