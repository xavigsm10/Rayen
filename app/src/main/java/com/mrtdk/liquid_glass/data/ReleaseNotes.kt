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
    const val VERSION_NAME = "0.6.6"
    const val VERSION_TAG = "v0.6.6"
    const val RELEASE_TITLE = "Rayen v0.6.6 - Novedades Apple Music, Optimización Total, Reproducción Sin Cortes & Letras Traducidas"

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
            categoryName = "Novedades y Búsqueda",
            icon = Icons.Default.Explore,
            items = listOf(
                ReleaseItem(
                    title = "Lanzamientos al día de Apple Music",
                    description = "Nuevas canciones destacadas, álbumes del momento y tendencias sincronizados con Innertube (YouTube Music) para reproducirse al instante.",
                    tag = "Novedades"
                ),
                ReleaseItem(
                    title = "48 categorías renovadas",
                    description = "Portadas en ultra alta resolución y contenido actualizado en la vista de Explorar y Búsqueda.",
                    tag = "Búsqueda"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Optimización general",
            icon = Icons.Default.Speed,
            items = listOf(
                ReleaseItem(
                    title = "Optimizaciones generales de la aplicación",
                    description = "Menor consumo de CPU, GPU, memoria RAM y batería sin perder la apariencia: mismos desenfoques, difuminados, animaciones y Liquid Glass.",
                    tag = "Rendimiento"
                ),
                ReleaseItem(
                    title = "Caché y carga inteligentes",
                    description = "Shaders compartidos, imágenes al tamaño justo y menos trabajo fuera de pantalla para una app más fluida y fría.",
                    tag = "Fluidez"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Reproducción sin interrupciones",
            icon = Icons.Default.PlayArrow,
            items = listOf(
                ReleaseItem(
                    title = "Se arreglaron los problemas de reproducción",
                    description = "Adiós a las pausas a las 2-3 canciones con el dispositivo bloqueado: servicio en primer plano permanente, red protegida en Doze y precarga anticipada de la siguiente canción.",
                    tag = "Bloqueo"
                ),
                ReleaseItem(
                    title = "Cerrar y reabrir donde quedaste",
                    description = "Al cerrar la app y volver a abrirla, la cola, la posición, la repetición y el aleatorio se restauran listos para seguir.",
                    tag = "Continuidad"
                ),
                ReleaseItem(
                    title = "Insignia Dolby Atmos exclusiva",
                    description = "Con Dolby Atmos activado ahora se muestra únicamente la insignia Dolby Atmos, sin duplicar la de Lossless.",
                    tag = "Audio"
                ),
                ReleaseItem(
                    title = "Carátulas siempre en alta",
                    description = "La portada principal, la notificación y la pantalla de bloqueo usan siempre la imagen en alta resolución al elegir desde la cola.",
                    tag = "Visual"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Traducciones de letras",
            icon = Icons.Default.Translate,
            items = listOf(
                ReleaseItem(
                    title = "Traducciones de letras implementadas",
                    description = "Traduce cualquier letra con un toque y mírala en pequeño bajo el original, estilo Apple Music, con romanización para otros alfabetos.",
                    tag = "Letras"
                ),
                ReleaseItem(
                    title = "Elige entre 133 idiomas",
                    description = "Nuevo ajuste «Idioma de traducción» en Contenido con buscador: sigue al idioma de la app o fija el tuyo para siempre.",
                    tag = "Idiomas"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Canciones favoritas",
            icon = Icons.Default.Favorite,
            items = listOf(
                ReleaseItem(
                    title = "Sistema de favoritas arreglado",
                    description = "La última canción guardada queda primera y ya no la entierra la sincronización de Spotify; cada favorita conserva su lugar.",
                    tag = "Favoritas"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Nueva apariencia iPod",
            icon = Icons.Default.Album,
            items = listOf(
                ReleaseItem(
                    title = "Nueva apariencia iPod implementada",
                    description = "Rueda de clic con iconos vectoriales, sin símbolos raros, y cola de reproducción totalmente navegable canción por canción.",
                    tag = "iPod"
                ),
                ReleaseItem(
                    title = "Acerca de renovado",
                    description = "La vista Acerca de estrena el nuevo logo y el nombre Rayen.",
                    tag = "Marca"
                )
            )
        )
    )

    // ==========================================
    // 🇺🇸 ENGLISH
    // ==========================================
    private val categoriesEn: List<ReleaseCategory> = listOf(
        ReleaseCategory(
            categoryName = "New Releases & Search",
            icon = Icons.Default.Explore,
            items = listOf(
                ReleaseItem(
                    title = "Latest Apple Music Releases",
                    description = "Featured new songs, trending tracks, and hot albums synced with Innertube (YouTube Music) for instant playback.",
                    tag = "Releases"
                ),
                ReleaseItem(
                    title = "48 Refreshed Categories",
                    description = "Ultra-high-resolution artwork and updated curation across Explore and Search.",
                    tag = "Search"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "General Optimization",
            icon = Icons.Default.Speed,
            items = listOf(
                ReleaseItem(
                    title = "General App Optimizations",
                    description = "Drastically reduced CPU, GPU, RAM, and battery consumption while preserving the gorgeous Liquid Glass aesthetics.",
                    tag = "Performance"
                ),
                ReleaseItem(
                    title = "Smart Cache & Loading",
                    description = "Shared shaders, properly sized images, and minimal off-screen overhead for a cooler, smoother app.",
                    tag = "Smoothness"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Uninterrupted Playback",
            icon = Icons.Default.PlayArrow,
            items = listOf(
                ReleaseItem(
                    title = "Playback Issues Resolved",
                    description = "No more lockscreen pauses: persistent foreground service, Doze mode network protection, and early track preloading.",
                    tag = "Lockscreen"
                ),
                ReleaseItem(
                    title = "Resume Where You Left Off",
                    description = "Playback queue, track position, repeat, and shuffle restore automatically when reopening the app.",
                    tag = "Session"
                ),
                ReleaseItem(
                    title = "Exclusive Dolby Atmos Badge",
                    description = "Displays only the Dolby Atmos badge without duplicating Lossless.",
                    tag = "Audio"
                ),
                ReleaseItem(
                    title = "Always High-Res Artwork",
                    description = "Notification, lock screen, and main player covers in maximum resolution when choosing tracks from the queue.",
                    tag = "Visual"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Lyrics Translations",
            icon = Icons.Default.Translate,
            items = listOf(
                ReleaseItem(
                    title = "Lyrics Translations Implemented",
                    description = "Translated lyrics appear right below the original text in Apple Music style, with romanization support.",
                    tag = "Lyrics"
                ),
                ReleaseItem(
                    title = "Choose from 133 Languages",
                    description = "New 'Translation Language' setting with instant search under Content.",
                    tag = "Languages"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Favorite Songs",
            icon = Icons.Default.Favorite,
            items = listOf(
                ReleaseItem(
                    title = "Favorites Sorting Fixed",
                    description = "Newly saved songs stay at the top; Spotify sync no longer buries your manually saved favorites.",
                    tag = "Favorites"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "New iPod Experience",
            icon = Icons.Default.Album,
            items = listOf(
                ReleaseItem(
                    title = "New iPod Interface Implemented",
                    description = "Crisp vector icons and full track-by-track queue navigation.",
                    tag = "iPod"
                ),
                ReleaseItem(
                    title = "Updated About View",
                    description = "Fresh branding with the new Rayen logo and name.",
                    tag = "Brand"
                )
            )
        )
    )

    // ==========================================
    // 🇧🇷 PORTUGUÊS
    // ==========================================
    private val categoriesPt: List<ReleaseCategory> = listOf(
        ReleaseCategory(
            categoryName = "Novidades e Busca",
            icon = Icons.Default.Explore,
            items = listOf(
                ReleaseItem(
                    title = "Lançamentos da Apple Music",
                    description = "Músicas em destaque, álbuns do momento e tendências sincronizados com o Innertube (YouTube Music) para reprodução imediata.",
                    tag = "Novidades"
                ),
                ReleaseItem(
                    title = "48 Categorias Renovadas",
                    description = "Capas em altíssima resolução e conteúdo atualizado na tela de Explorar e Busca.",
                    tag = "Busca"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Otimização Geral",
            icon = Icons.Default.Speed,
            items = listOf(
                ReleaseItem(
                    title = "Otimizações Gerais do Aplicativo",
                    description = "Menor consumo de CPU, GPU, RAM e bateria mantendo todo o visual Liquid Glass.",
                    tag = "Desempenho"
                ),
                ReleaseItem(
                    title = "Cache e Carregamento Inteligentes",
                    description = "Shaders compartilhados, imagens otimizadas e fluidez constante.",
                    tag = "Fluidez"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Reprodução Contínua",
            icon = Icons.Default.PlayArrow,
            items = listOf(
                ReleaseItem(
                    title = "Problemas de Reprodução Corrigidos",
                    description = "Sem interrupções com a tela bloqueada: serviço em primeiro plano permanente, proteção de rede no Doze e pré-carregamento.",
                    tag = "Bloqueio"
                ),
                ReleaseItem(
                    title = "Retome de Onde Parou",
                    description = "Fila, posição, repetição e modo aleatório são restaurados ao reabrir o app.",
                    tag = "Sessão"
                ),
                ReleaseItem(
                    title = "Selo Dolby Atmos Exclusivo",
                    description = "Exibe somente o selo Dolby Atmos sem duplicar o Lossless.",
                    tag = "Áudio"
                ),
                ReleaseItem(
                    title = "Capas Sempre em Alta Definição",
                    description = "Notificação, tela de bloqueio e reprodutor com a máxima nitidez.",
                    tag = "Visual"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Tradução de Letras",
            icon = Icons.Default.Translate,
            items = listOf(
                ReleaseItem(
                    title = "Tradução de Letras Implementada",
                    description = "Letras traduzidas logo abaixo do original no estilo Apple Music, com suporte a romanização.",
                    tag = "Letras"
                ),
                ReleaseItem(
                    title = "133 Idiomas Disponíveis",
                    description = "Nova opção «Idioma de tradução» com pesquisa rápida em Conteúdo.",
                    tag = "Idiomas"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Músicas Favoritas",
            icon = Icons.Default.Favorite,
            items = listOf(
                ReleaseItem(
                    title = "Organização das Favoritas Corrigida",
                    description = "A última música adicionada fica no topo e a sincronização do Spotify não altera sua ordem.",
                    tag = "Favoritas"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Novo Visual iPod",
            icon = Icons.Default.Album,
            items = listOf(
                ReleaseItem(
                    title = "Nova Interface iPod Implementada",
                    description = "Ícones vetoriais nítidos e navegação completa faixa a faixa na fila de reprodução.",
                    tag = "iPod"
                ),
                ReleaseItem(
                    title = "Sobre Renovado",
                    description = "Nova identidade visual e nome Rayen.",
                    tag = "Marca"
                )
            )
        )
    )

    // ==========================================
    // 🇹🇷 TÜRKÇE
    // ==========================================
    private val categoriesTr: List<ReleaseCategory> = listOf(
        ReleaseCategory(
            categoryName = "Yenilikler ve Arama",
            icon = Icons.Default.Explore,
            items = listOf(
                ReleaseItem(
                    title = "Apple Music Güncel Çıkışları",
                    description = "Öne çıkan yeni şarkılar, trendler ve en yeni albümler anında çalınabilmesi için Innertube (YouTube Music) ile senkronize edildi.",
                    tag = "Yenilikler"
                ),
                ReleaseItem(
                    title = "Yenilenen 48 Kategori",
                    description = "Keşfet ve Arama bölümünde ultra yüksek çözünürlüklü kapak görselleri ve güncel içerikler.",
                    tag = "Arama"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Genel Optimizasyon",
            icon = Icons.Default.Speed,
            items = listOf(
                ReleaseItem(
                    title = "Genel Uygulama Optimizasyonları",
                    description = "Liquid Glass estetiğini korurken CPU, GPU, RAM ve pil tüketiminde ciddi azalma.",
                    tag = "Performans"
                ),
                ReleaseItem(
                    title = "Akıllı Önbellek ve Yükleme",
                    description = "Paylaşılan gölgelendiriciler, doğru boyutta görseller ve akıcı bir deneyim.",
                    tag = "Akıcılık"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Kesintisiz Oynatma",
            icon = Icons.Default.PlayArrow,
            items = listOf(
                ReleaseItem(
                    title = "Oynatma Sorunları Giderildi",
                    description = "Kilit ekranında duraklama yok: kalıcı ön plan hizmeti, Doze modu ağ koruması ve bir sonraki şarkının önceden yüklenmesi.",
                    tag = "Kilit Ekranı"
                ),
                ReleaseItem(
                    title = "Kaldığınız Yerden Devam Edin",
                    description = "Uygulama yeniden açıldığında çalma sırası, konum, tekrar ve karışık çalma modları otomatik olarak geri yüklenir.",
                    tag = "Oturum"
                ),
                ReleaseItem(
                    title = "Özel Dolby Atmos Rozeti",
                    description = "Dolby Atmos etkinken Lossless rozeti yinelenmeden yalnızca Dolby Atmos gösterilir.",
                    tag = "Ses"
                ),
                ReleaseItem(
                    title = "Her Zaman Yüksek Çözünürlüklü Kapaklar",
                    description = "Bildirim, kilit ekranı ve ana oynatıcıda en yüksek kalitede kapak görselleri.",
                    tag = "Görsel"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Şarkı Sözü Çevirileri",
            icon = Icons.Default.Translate,
            items = listOf(
                ReleaseItem(
                    title = "Şarkı Sözü Çevirileri Eklendi",
                    description = "Orijinal sözlerin hemen altında Apple Music tarzında çevrilmiş sözler ve romanizasyon desteği.",
                    tag = "Sözler"
                ),
                ReleaseItem(
                    title = "133 Dil Seçeneği",
                    description = "İçerik ayarları altında anında aramalı yeni «Çeviri Dili» seçeneği.",
                    tag = "Diller"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Favori Şarkılar",
            icon = Icons.Default.Favorite,
            items = listOf(
                ReleaseItem(
                    title = "Favoriler Sıralaması Düzeltildi",
                    description = "En son eklenen favori şarkı en üstte kalır ve Spotify senkronizasyonu manuel listenizi bozmaz.",
                    tag = "Favoriler"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Yeni iPod Görünümü",
            icon = Icons.Default.Album,
            items = listOf(
                ReleaseItem(
                    title = "Yeni iPod Arayüzü Eklendi",
                    description = "Vektör simgeler ve şarkı şarkı gezilebilen çalma sırası.",
                    tag = "iPod"
                ),
                ReleaseItem(
                    title = "Yenilenen Hakkında Bölümü",
                    description = "Yeni Rayen logosu ve ismi.",
                    tag = "Marka"
                )
            )
        )
    )

    val markdownChangelog: String
        get() = when (java.util.Locale.getDefault().language.lowercase().take(2)) {
            "en" -> """
# 🚀 Rayen v0.6.6 — Apple Music New Releases, Peak Optimization, Gapless Playback & Translated Lyrics

### 🌟 Updated New Releases & Search
* **Latest Apple Music Releases**: Featured new tracks, hot albums, and trending charts synced with Innertube (YouTube Music) for instant playback.
* **48 Refreshed Categories**: Ultra-high-resolution artwork and updated curation across Explore and Search.

### ⚡ General App Optimizations
* **Performance & Battery**: Drastically reduced CPU, GPU, RAM, and battery consumption while preserving Liquid Glass.
* **Smart Cache & Loading**: Shared shaders, properly sized images, and minimal off-screen overhead.

### 🎵 Uninterrupted Playback
* **No more lockscreen pauses**: Persistent foreground service, Doze mode network protection, and early track preloading.
* **Resume where you left off**: Playback queue, track position, repeat, and shuffle restore automatically.
* **Exclusive Dolby Atmos Badge**: Displays only the Dolby Atmos badge without duplicating Lossless.
* **Always High-Res Artwork**: Notification, lock screen, and main player covers in maximum resolution.

### 🌍 Lyrics Translations
* **One-tap Translation**: Translated lyrics appear right below the original text in Apple Music style, with romanization.
* **133 Languages Available**: New "Translation Language" setting with instant search under Content.

### ❤️ Favorite Songs System
* **Proper sorting preserved**: Newly saved songs stay at the top; Spotify sync no longer buries favorites.

### 📱 New iPod Experience
* **Revamped Click Wheel**: Crisp vector icons and full track-by-track queue navigation.
* **Updated About View**: Fresh branding with the new Rayen logo and name.
            """.trimIndent()

            "pt" -> """
# 🚀 Rayen v0.6.6 — Novidades da Apple Music, Otimização Total, Reprodução Sem Interrupções & Letras Traduzidas

### 🌟 Novidades e Busca Atualizadas
* **Lançamentos da Apple Music**: Músicas em destaque, álbuns do momento e tendências sincronizados com o Innertube (YouTube Music).
* **48 Categorias Renovadas**: Capas em altíssima resolução e conteúdo atualizado na tela de Explorar e Busca.

### ⚡ Otimizações Gerais do Aplicativo
* **Desempenho e Bateria**: Menor consumo de CPU, GPU, RAM e bateria mantendo todo o visual Liquid Glass.
* **Cache e Carregamento Inteligentes**: Shaders compartilhados, imagens otimizadas e fluidez constante.

### 🎵 Reprodução Contínua Sem Pausas
* **Sem interrupções com a tela bloqueada**: Serviço em primeiro plano permanente, proteção de rede no Doze e pré-carregamento.
* **Retome de onde parou**: Fila, posição, repetição e modo aleatório são restaurados ao reabrir o app.
* **Selo Dolby Atmos Exclusivo**: Exibe somente o selo Dolby Atmos sem duplicar o Lossless.
* **Capas sempre em alta definição**: Notificação, tela de bloqueio e reprodutor com a máxima nitidez.

### 🌍 Tradução de Letras
* **Tradução com um toque**: Letras traduzidas logo abaixo do original no estilo Apple Music, com romanização.
* **133 Idiomas Disponíveis**: Nova opção «Idioma de tradução» com pesquisa rápida em Conteúdo.

### ❤️ Sistema de Músicas Favoritas
* **Organização correta garantida**: A última música adicionada fica no topo e a sincronização do Spotify não altera a ordem.

### 📱 Novo Visual iPod
* **Click Wheel Renovada**: Ícones vetoriais nítidos e navegação completa faixa a faixa na fila de reprodução.
* **Sobre Renovado**: Nova identidade visual e nome Rayen.
            """.trimIndent()

            "tr" -> """
# 🚀 Rayen v0.6.6 — Apple Music Yenilikleri, Tam Optimizasyon, Kesintisiz Çalma ve Çevrilmiş Şarkı Sözleri

### 🌟 Güncellenen Yenilikler ve Arama
* **Apple Music Güncel Çıkışları**: Öne çıkan yeni şarkılar, trendler ve albümler anında çalınabilmesi için Innertube (YouTube Music) ile senkronize edildi.
* **Yenilenen 48 Kategori**: Keşfet ve Arama bölümünde ultra yüksek çözünürlüklü kapak görselleri ve güncel içerikler.

### ⚡ Genel Uygulama Optimizasyonları
* **Performans ve Pil Tasarrufu**: Liquid Glass estetiğini korurken CPU, GPU, RAM ve pil tüketiminde ciddi azalma.
* **Akıllı Önbellek ve Yükleme**: Paylaşılan gölgelendiriciler, doğru boyutta görseller ve akıcı bir deneyim.

### 🎵 Kesintisiz Oynatma
* **Kilit ekranında duraklama yok**: Kalıcı ön plan hizmeti, Doze modu ağ koruması ve bir sonraki şarkının önceden yüklenmesi.
* **Kaldığınız yerden devam edin**: Çalma sırası, konum, tekrar ve karışık çalma modları otomatik olarak geri yüklenir.
* **Özel Dolby Atmos Rozeti**: Dolby Atmos etkinken Lossless rozeti yinelenmeden yalnızca Dolby Atmos gösterilir.
* **Her Zaman Yüksek Çözünürlüklü Kapaklar**: Bildirim, kilit ekranı ve ana oynatıcıda en yüksek kalitede kapak görselleri.

### 🌍 Şarkı Sözü Çevirileri
* **Tek Dokunuşla Çeviri**: Orijinal sözlerin hemen altında Apple Music tarzında çevrilmiş sözler ve romanizasyon.
* **133 Dil Seçeneği**: İçerik ayarları altında anında aramalı yeni «Çeviri Dili» seçeneği.

### ❤️ Favori Şarkılar Sistemi
* **Doğru sıralama**: En son eklenen favori şarkı en üstte kalır ve Spotify senkronizasyonu listenizi bozmaz.

### 📱 Yeni iPod Görünümü
* **Yenilenen Tıklama Tekerleği**: Vektör simgeler ve şarkı şarkı gezilebilen çalma sırası.
* **Yenilenen Hakkında Bölümü**: Yeni Rayen logosu ve ismi.
            """.trimIndent()

            else -> """
# 🚀 Rayen v0.6.6 — Novedades Apple Music, Optimización Total, Reproducción Sin Cortes & Letras Traducidas

### 🌟 Novedades y Búsqueda Actualizadas
* **Lanzamientos al día de Apple Music**: Nuevas canciones destacadas, álbumes del momento y tendencias directamente sincronizados con Innertube (YouTube Music).
* **48 categorías renovadas**: Portadas en ultra alta resolución y contenido actualizado en la vista de Explorar y Búsqueda.

### ⚡ Optimizaciones Generales de la Aplicación
* **Rendimiento y batería**: Menor consumo de CPU, GPU, memoria RAM y batería sin perder la estética Liquid Glass.
* **Caché y carga inteligentes**: Shaders compartidos, imágenes al tamaño justo y menor trabajo fuera de pantalla.

### 🎵 Reproducción Sin Cortes
* **Sin pausas con el dispositivo bloqueado**: Servicio en primer plano permanente, red protegida en modo Doze y precarga anticipada de la siguiente canción.
* **Cerrar y reabrir donde quedaste**: La cola de reproducción, la posición, la repetición y el aleatorio se restauran automáticamente.
* **Insignia Dolby Atmos exclusiva**: Con Dolby Atmos activado se muestra únicamente su insignia, sin duplicar la de Lossless.
* **Carátulas siempre en alta**: Notificación, pantalla de bloqueo y portada principal en máxima resolución al elegir desde la cola.

### 🌍 Traducciones de Letras
* **Traduce con un toque**: Letras traducidas bajo el texto original estilo Apple Music, con romanización para otros alfabetos.
* **133 idiomas disponibles**: Nuevo ajuste «Idioma de traducción» con buscador rápido en Contenido.

### ❤️ Canciones Favoritas
* **Orden correcto garantizado**: La última canción guardada permanece primera y la sincronización de Spotify ya no entierra tus canciones guardadas.

### 📱 Nueva Apariencia iPod
* **Rueda de clic renovada**: Iconos vectoriales nítidos y cola de reproducción totalmente navegable canción por canción.
* **Acerca de renovado**: Nuevo logo y marca Rayen.
            """.trimIndent()
        }
}
