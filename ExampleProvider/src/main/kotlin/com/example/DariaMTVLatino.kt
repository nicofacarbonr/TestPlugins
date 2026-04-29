package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element

class DariaMTVLatino : MainAPI() {
    // Apuntamos a la página que me pasaste
    override var mainUrl = "https://dariamtv.blogspot.com"
    override var name = "Daria MTV"
    override val hasMainPage = true
    override var lang = "es"
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    // 1. EL BUSCADOR
    override suspend fun search(query: String): List<SearchResponse> {
        // Usamos el buscador nativo de Blogger (?q=)
        val document = app.get("$mainUrl/search?q=$query").document
        
        // Seleccionamos la caja de cada resultado (div.post-outer)
        return document.select("div.post-outer").mapNotNull { post ->
            val title = post.selectFirst("h3.post-title a")?.text() ?: return@mapNotNull null
            val url = post.selectFirst("h3.post-title a")?.attr("href") ?: return@mapNotNull null
            val image = post.selectFirst("img")?.attr("src")

            newTvSeriesSearchResponse(title, url, TvType.TvSeries) {
                this.posterUrl = image
            }
        }
    }

    // 2. CARGAR LOS CAPÍTULOS
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h3.post-title")?.text() ?: "Daria"
        val poster = document.selectFirst("img")?.attr("src")
        
        // Buscamos todas las etiquetas <a> que tengan la palabra "Capítulo"
        val episodes = document.select("a").filter { 
            it.text().contains("Capítulo", ignoreCase = true) 
        }.mapIndexed { index, element ->
            Episode(
                data = element.attr("href"),
                name = element.text(),
                episode = index + 1
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
        }
    }

    // 3. EXTRAER EL VIDEO
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Los videos en ese blog suelen estar metidos en iframes (Drive, Mega, etc.)
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            // CloudStream ya tiene extractores automáticos para Drive/Mega
            loadExtractor(src, data, subtitleCallback, callback)
        }
        return true
    }
}




