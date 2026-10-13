package com.mrtdk.liquid_glass.data.lyrics

import java.util.Locale

/**
 * Punto único que conoce zh-Hant / zh-Hans.
 *
 * La app guarda y compara tags completos con script ("zh-Hant", "zh-Hans").
 * Cada servicio recibe el dialecto que entiende: YouTube / Google Translate
 * quieren tags de región ("zh-TW" / "zh-CN").
 */
object LocaleTags {
    const val ZH_HANT = "zh-Hant"
    const val ZH_HANS = "zh-Hans"
    const val ZH_TW_WIRE = "zh-TW"
    const val ZH_CN_WIRE = "zh-CN"

    /**
     * Normaliza cualquier tag entrante ("zh_TW", "zh-HK", "zh", "en-US", ...)
     * a "zh-Hant" / "zh-Hans" / idioma base. "zh" pelado queda "zh-Hans"
     * porque los catálogos existentes son simplificados.
     */
    fun normalizeAppTag(raw: String?): String {
        val tag = raw?.trim()?.replace('_', '-')?.takeIf { it.isNotBlank() } ?: return "en"
        val lower = tag.lowercase(Locale.ROOT)
        if (lower.startsWith("zh")) {
            if ("hant" in lower) return ZH_HANT
            if ("hans" in lower) return ZH_HANS
            if ("tw" in lower || "hk" in lower || "mo" in lower) return ZH_HANT
            if ("cn" in lower || "sg" in lower || "my" in lower) return ZH_HANS
            return ZH_HANS
        }
        val base = Locale.forLanguageTag(tag).language.lowercase(Locale.ROOT).ifBlank { lower.substringBefore('-') }
        return when (base) {
            "iw" -> "he"
            "in" -> "id"
            "ji" -> "yi"
            else -> base.ifBlank { "en" }
        }
    }

    /** Tag completo del locale actual, preservando script. */
    fun systemTag(): String = normalizeAppTag(Locale.getDefault().toLanguageTag())

    /**
     * Lo que el endpoint de letras quiere como `tl`. Google entiende zh-TW /
     * zh-CN como scripts; mandar "zh" pelado siempre devuelve simplificado y
     * "zh-Hant" no siempre se respeta — así que script se mapea a región.
     * Una elección explícita en forma zh-TW/zh-CN pasa intacta.
     */
    fun translationWireTarget(tag: String): String {
        val t = tag.trim().replace('_', '-')
        if (t.equals("zh-TW", ignoreCase = true) || t.equals("zh-Hant", ignoreCase = true)) return ZH_TW_WIRE
        if (t.equals("zh-CN", ignoreCase = true) || t.equals("zh-Hans", ignoreCase = true)) return ZH_CN_WIRE
        val norm = normalizeAppTag(t)
        return when (norm) {
            ZH_HANT -> ZH_TW_WIRE
            ZH_HANS -> ZH_CN_WIRE
            else -> t.ifBlank { "en" }
        }
    }

    /** Igualdad con script: zh-Hant != zh-Hans != zh, a diferencia de base. */
    fun sameLanguageScriptAware(first: String, second: String): Boolean {
        fun canon(tag: String): String {
            val t = tag.trim().replace('_', '-')
            val lower = t.lowercase(Locale.ROOT)
            if (lower.startsWith("zh")) {
                if ("tw" in lower || "hant" in lower || "hk" in lower || "mo" in lower) return "zh-hant"
                if ("cn" in lower || "hans" in lower || "sg" in lower) return "zh-hans"
                return "zh"
            }
            return Locale.forLanguageTag(t).language.lowercase(Locale.ROOT).ifBlank { lower.substringBefore('-') }
        }
        return canon(first) == canon(second)
    }

    fun isChineseTag(tag: String): Boolean =
        tag.trim().replace('_', '-').lowercase(Locale.ROOT).startsWith("zh")

    /**
     * Idioma destino del botón de traducir:
     * 1. Elección explícita del usuario ([stored]) viaja intacta.
     * 2. Si está en blanco, sigue al idioma de la app ([appLanguage]).
     * 3. El chino mapea a región wire; el resto a idioma base, "en" de fallback.
     */
    fun resolveTranslationTarget(stored: String?, appLanguage: String?, device: Locale = Locale.getDefault()): String {
        val explicit = stored?.trim().orEmpty()
        if (explicit.isNotBlank()) return explicit
        val configuredTag = when {
            appLanguage.isNullOrBlank() ||
                appLanguage.equals("SYSTEM_DEFAULT", ignoreCase = true) ||
                appLanguage.equals("system", ignoreCase = true) ->
                device.toLanguageTag()
            else -> appLanguage
        }
        return if (isChineseTag(configuredTag)) translationWireTarget(configuredTag)
        else Locale.forLanguageTag(configuredTag).language.ifBlank { "en" }
    }
}
