package com.mrtdk.liquid_glass.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class ReleaseItem(
    val title: String,
    val description: String,
    val tag: String? = null
)

data class ReleaseCategory(
    val categoryName: String,
    val icon: ImageVector,
    val items: List<ReleaseItem>
)

object ReleaseNotes {
    const val VERSION_NAME = "0.6.7"
    const val VERSION_TAG = "v0.6.7"
    const val RELEASE_TITLE = "Rayen v0.6.7 - Nuevos Ajustes de Calidad de Sonido y Problemas de Reproducción Solucionados"

    val categories: List<ReleaseCategory>
        get() = getCategoriesForLocale(java.util.Locale.getDefault().language)

    fun getCategoriesForLocale(locale: String): List<ReleaseCategory> {
        val lang = locale.lowercase().take(2)
        return when (lang) {
            "en" -> categoriesEn
            "pt" -> categoriesPt
            "tr" -> categoriesTr
            else -> categoriesEs
        }
    }

    // ==========================================
    // 🇪🇸 ESPAÑOL
    // ==========================================
    private val categoriesEs: List<ReleaseCategory> = listOf(
        ReleaseCategory(
            categoryName = "Calidad de sonido",
            icon = Icons.Default.GraphicEq,
            items = listOf(
                ReleaseItem(
                    title = "Nuevos ajustes de calidad de sonido",
                    description = "7 niveles a elegir: Dolby Atmos, Hi-Res Max 24-bit/192 kHz, Hi-Res 24-bit/96 kHz, CD Lossless, Standard 320 kbps, Data Saver y YouTube Music. Tu elección se respeta al reproducir.",
                    tag = "Audio"
                ),
                ReleaseItem(
                    title = "Calidad de descarga independiente",
                    description = "Nuevo ajuste separado para las descargas: escucha en una calidad y guarda en otra, cada una con sus 7 niveles.",
                    tag = "Descargas"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Reproducción solucionada",
            icon = Icons.Default.PlayArrow,
            items = listOf(
                ReleaseItem(
                    title = "Adiós al error de reproducción (403)",
                    description = "Nuevo motor de streams con cipher actualizado, headers por cliente y cambio automático de cliente si uno falla: las canciones que antes no abrían ahora suenan a la primera.",
                    tag = "Streaming"
                ),
                ReleaseItem(
                    title = "Carga más rápida y estable",
                    description = "Descarga por rangos acotados a velocidad de línea y colchón de 6 minutos: menos tirones, menos esperas y mejor aguante con mala red.",
                    tag = "Velocidad"
                ),
                ReleaseItem(
                    title = "Controles siempre visibles",
                    description = "Los botones del reproductor y el volumen ya no quedan debajo de los botones de navegación del sistema.",
                    tag = "Visual"
                )
            )
        )
    )

    // ==========================================
    // 🇺🇸 ENGLISH
    // ==========================================
    private val categoriesEn: List<ReleaseCategory> = listOf(
        ReleaseCategory(
            categoryName = "Sound Quality",
            icon = Icons.Default.GraphicEq,
            items = listOf(
                ReleaseItem(
                    title = "New sound quality settings",
                    description = "7 levels to choose from: Dolby Atmos, Hi-Res Max 24-bit/192 kHz, Hi-Res 24-bit/96 kHz, CD Lossless, Standard 320 kbps, Data Saver and YouTube Music. Your choice is honored on playback.",
                    tag = "Audio"
                ),
                ReleaseItem(
                    title = "Independent download quality",
                    description = "New separate setting for downloads: stream in one quality and save in another, each with its own 7 levels.",
                    tag = "Downloads"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Playback Fixed",
            icon = Icons.Default.PlayArrow,
            items = listOf(
                ReleaseItem(
                    title = "Goodbye playback error (403)",
                    description = "New stream engine with updated cipher, per-client headers and automatic client switching on failure: songs that refused to open now play first try.",
                    tag = "Streaming"
                ),
                ReleaseItem(
                    title = "Faster, steadier loading",
                    description = "Bounded-range fetching at line rate plus a 6-minute buffer: fewer stalls, shorter waits, better survival on weak networks.",
                    tag = "Speed"
                ),
                ReleaseItem(
                    title = "Controls always visible",
                    description = "Player buttons and volume no longer hide under the system navigation buttons.",
                    tag = "Visual"
                )
            )
        )
    )

    // ==========================================
    // 🇧🇷 PORTUGUÊS
    // ==========================================
    private val categoriesPt: List<ReleaseCategory> = listOf(
        ReleaseCategory(
            categoryName = "Qualidade de som",
            icon = Icons.Default.GraphicEq,
            items = listOf(
                ReleaseItem(
                    title = "Novos ajustes de qualidade de som",
                    description = "7 níveis à escolha: Dolby Atmos, Hi-Res Max 24-bit/192 kHz, Hi-Res 24-bit/96 kHz, CD Lossless, Standard 320 kbps, Data Saver e YouTube Music. Sua escolha é respeitada na reprodução.",
                    tag = "Áudio"
                ),
                ReleaseItem(
                    title = "Qualidade de download independente",
                    description = "Novo ajuste separado para downloads: ouça numa qualidade e salve noutra, cada uma com seus 7 níveis.",
                    tag = "Downloads"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Reprodução corrigida",
            icon = Icons.Default.PlayArrow,
            items = listOf(
                ReleaseItem(
                    title = "Adeus ao erro de reprodução (403)",
                    description = "Novo motor de streams com cipher atualizado, headers por cliente e troca automática de cliente em caso de falha: músicas que não abriam agora tocam de primeira.",
                    tag = "Streaming"
                ),
                ReleaseItem(
                    title = "Carregamento mais rápido e estável",
                    description = "Busca por intervalos limitados na velocidade da linha e buffer de 6 minutos: menos travamentos e mais resistência em redes fracas.",
                    tag = "Velocidade"
                ),
                ReleaseItem(
                    title = "Controles sempre visíveis",
                    description = "Os botões do reprodutor e o volume não ficam mais embaixo dos botões de navegação do sistema.",
                    tag = "Visual"
                )
            )
        )
    )

    // ==========================================
    // 🇹🇷 TÜRKÇE
    // ==========================================
    private val categoriesTr: List<ReleaseCategory> = listOf(
        ReleaseCategory(
            categoryName = "Ses Kalitesi",
            icon = Icons.Default.GraphicEq,
            items = listOf(
                ReleaseItem(
                    title = "Yeni ses kalitesi ayarları",
                    description = "7 seviye: Dolby Atmos, Hi-Res Max 24-bit/192 kHz, Hi-Res 24-bit/96 kHz, CD Lossless, Standard 320 kbps, Data Saver ve YouTube Music. Seçiminiz çalmada uygulanır.",
                    tag = "Ses"
                ),
                ReleaseItem(
                    title = "Bağımsız indirme kalitesi",
                    description = "İndirmeler için ayrı ayar: bir kalitede dinleyin, başka kalitede kaydedin; her birinde 7 seviye.",
                    tag = "İndirme"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Oynatma Düzeltildi",
            icon = Icons.Default.PlayArrow,
            items = listOf(
                ReleaseItem(
                    title = "Oynatma hatasına (403) veda",
                    description = "Güncel cipher, istemci başına headers ve arızada otomatik istemci değişimiyle yeni stream motoru: açılmayan şarkılar artık ilk seferde çalıyor.",
                    tag = "Akış"
                ),
                ReleaseItem(
                    title = "Daha hızlı ve kararlı yükleme",
                    description = "Hat hızında sınırlı aralıklı indirme ve 6 dakikalık tampon: daha az takılma, zayıf ağda daha iyi dayanıklılık.",
                    tag = "Hız"
                ),
                ReleaseItem(
                    title = "Kontroller her zaman görünür",
                    description = "Oynatıcı düğmeleri ve ses, artık sistem gezinti düğmelerinin altında kalmıyor.",
                    tag = "Görsel"
                )
            )
        )
    )

    val markdownChangelog: String
        get() = when (java.util.Locale.getDefault().language.lowercase().take(2)) {
            "en" -> """
# 🚀 Rayen v0.6.7 — New Sound Quality Settings & Playback Fixes

### 🎚️ Sound Quality
* **New sound quality settings**: 7 levels — Dolby Atmos, Hi-Res Max 24-bit/192 kHz, Hi-Res 24-bit/96 kHz, CD Lossless, Standard 320 kbps, Data Saver and YouTube Music.
* **Independent download quality**: Stream in one quality, save in another.

### 🎵 Playback Fixes
* **Goodbye playback error (403)**: New stream engine with updated cipher, per-client headers and automatic client switching.
* **Faster, steadier loading**: Bounded-range fetching plus a 6-minute buffer.
* **Controls always visible**: Player buttons no longer hide under the system navigation buttons.
            """.trimIndent()

            "pt" -> """
# 🚀 Rayen v0.6.7 — Novos Ajustes de Qualidade de Som e Correções de Reprodução

### 🎚️ Qualidade de Som
* **Novos ajustes de qualidade de som**: 7 níveis — Dolby Atmos, Hi-Res Max 24-bit/192 kHz, Hi-Res 24-bit/96 kHz, CD Lossless, Standard 320 kbps, Data Saver e YouTube Music.
* **Qualidade de download independente**: Ouça numa qualidade e salve noutra.

### 🎵 Correções de Reprodução
* **Adeus ao erro de reprodução (403)**: Novo motor de streams com cipher atualizado, headers por cliente e troca automática de cliente.
* **Carregamento mais rápido e estável**: Busca por intervalos limitados e buffer de 6 minutos.
* **Controles sempre visíveis**: Botões do reprodutor fora dos botões de navegação do sistema.
            """.trimIndent()

            "tr" -> """
# 🚀 Rayen v0.6.7 — Yeni Ses Kalitesi Ayarları ve Oynatma Düzeltmeleri

### 🎚️ Ses Kalitesi
* **Yeni ses kalitesi ayarları**: 7 seviye — Dolby Atmos, Hi-Res Max 24-bit/192 kHz, Hi-Res 24-bit/96 kHz, CD Lossless, Standard 320 kbps, Data Saver ve YouTube Music.
* **Bağımsız indirme kalitesi**: Bir kalitede dinleyin, başka kalitede kaydedin.

### 🎵 Oynatma Düzeltmeleri
* **Oynatma hatasına (403) veda**: Güncel cipher, istemci başına headers ve otomatik istemci değişimiyle yeni motor.
* **Daha hızlı ve kararlı yükleme**: Sınırlı aralıklı indirme ve 6 dakikalık tampon.
* **Kontroller her zaman görünür**: Oynatıcı düğmeleri sistem gezinti düğmelerinin altında kalmıyor.
            """.trimIndent()

            else -> """
# 🚀 Rayen v0.6.7 — Nuevos Ajustes de Calidad de Sonido y Problemas de Reproducción Solucionados

### 🎚️ Calidad de Sonido
* **Nuevos ajustes de calidad de sonido**: 7 niveles — Dolby Atmos, Hi-Res Max 24-bit/192 kHz, Hi-Res 24-bit/96 kHz, CD Lossless, Standard 320 kbps, Data Saver y YouTube Music.
* **Calidad de descarga independiente**: Escucha en una calidad y guarda en otra.

### 🎵 Problemas de Reproducción Solucionados
* **Adiós al error de reproducción (403)**: Nuevo motor de streams con cipher actualizado, headers por cliente y cambio automático de cliente.
* **Carga más rápida y estable**: Descarga por rangos acotados y colchón de 6 minutos.
* **Controles siempre visibles**: Botones del reproductor fuera de los botones de navegación del sistema.
            """.trimIndent()
        }
}
