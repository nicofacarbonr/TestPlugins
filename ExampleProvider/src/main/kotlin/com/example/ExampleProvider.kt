package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class ExampleProvider : MainAPI() {
    // Apuntamos a la web de Daria
    override var mainUrl = "https://dariamtv.blogspot.com"
    override var name = "Daria MTV"
    override val hasMainPage = true
    override var lang = "es"
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search?q=$query").document
        
        return document.select("div.post-outer").mapNotNull { post ->
            val title = post.selectFirst("h3.post-title a")?.text() ?: return@mapNotNull null
            val url = post.selectFirst("h3.post-title a")?.attr("href") ?: return@mapNotNull null
            val image = post.selectFirst("img")?.attr("src")

            newTvSeriesSearchResponse(title, url, TvType.TvSeries) {
                this.posterUrl = image
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h3.post-title")?.text() ?: "Daria"
        val poster = document.selectFirst("img")?.attr("src")
        
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

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            loadExtractor(src, data, subtitleCallback, callback)
        }
        return true
    }
}
