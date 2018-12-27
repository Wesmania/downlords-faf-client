package com.faforever.client.news

import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.WebViewConfigurer
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.ShowLadderMapsEvent
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.theme.UiService
import com.google.common.eventbus.EventBus
import com.google.common.io.CharStreams
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.control.ListView
import javafx.scene.layout.Pane
import javafx.scene.web.WebView
import lombok.SneakyThrows
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

import java.io.InputStreamReader
import java.io.Reader

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class NewsController(private val preferencesService: PreferencesService, private val i18n: I18n, private val newsService: NewsService, private val uiService: UiService, private val eventBus: EventBus, private val webViewConfigurer: WebViewConfigurer) : AbstractViewController<Node>() {
    var newsRoot: Pane? = null
    var newsDetailWebView: WebView? = null
    var showLadderMapsButton: Button? = null
    var newsListView: ListView<NewsItem>? = null
    var loadingIndicator: Control? = null
    private val loadingIndicatorListener: ChangeListener<Boolean>

    override val root: Node?
        get() = newsRoot

    init {

        loadingIndicatorListener = { observable, oldValue, newValue ->
            loadingIndicator!!.parent.childrenUnmodifiable.stream()
                    .filter { node -> node !== loadingIndicator }
                    .forEach { node -> node.isVisible = (!newValue)!! }
        }
    }

    override fun initialize() {
        newsListView!!.setCellFactory { param -> NewsItemListCell(uiService) }
        newsListView!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue -> displayNewsItem(newValue) }

        loadingIndicator!!.managedProperty().bind(loadingIndicator!!.visibleProperty())
        loadingIndicator!!.visibleProperty().addListener(loadingIndicatorListener)
        loadingIndicatorListener.changed(loadingIndicator!!.visibleProperty(), null, true)

        loadingIndicator!!.parent.childrenUnmodifiable
                .forEach { node -> node.managedProperty().bind(node.visibleProperty()) }
    }

    private fun onLoadingStart() {
        Platform.runLater { loadingIndicator!!.isVisible = true }
    }

    private fun onLoadingStop() {
        Platform.runLater { loadingIndicator!!.isVisible = false }
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        if (!newsListView!!.items.isEmpty()) {
            return
        }

        showLadderMapsButton!!.managedProperty().bind(showLadderMapsButton!!.visibleProperty())
        showLadderMapsButton!!.isVisible = false
        newsDetailWebView!!.isContextMenuEnabled = false
        webViewConfigurer.configureWebView(newsDetailWebView)

        onLoadingStart()
        newsService.fetchNews().thenAccept { newsItems ->
            newsListView!!.items.setAll(newsItems)
            onLoadingStop()
            if (!newsItems.isEmpty()) {
                val mostRecentItem = newsItems[0]
                preferencesService.preferences!!.news.lastReadNewsUrl = mostRecentItem.getLink()
                preferencesService.storeInBackground()
            }
            newsListView!!.selectionModel.selectFirst()
        }
    }

    @SneakyThrows
    private fun displayNewsItem(newsItem: NewsItem) {
        showLadderMapsButton!!.isVisible = newsItem.getNewsCategory().equals(NewsCategory.LADDER)
        eventBus.post(UnreadNewsEvent(false))

        InputStreamReader(NEWS_DETAIL_HTML_RESOURCE.inputStream).use { reader ->
            val html = CharStreams.toString(reader).replace("{title}", newsItem.getTitle())
                    .replace("{content}", newsItem.getContent())
                    .replace("{authored}", i18n.get("news.authoredFormat", newsItem.getAuthor(), newsItem.getDate()))

            Platform.runLater { newsDetailWebView!!.engine.loadContent(html) }
        }
    }

    fun showLadderMaps() {
        eventBus.post(ShowLadderMapsEvent())
    }

    companion object {

        private val NEWS_DETAIL_HTML_RESOURCE = ClassPathResource("/theme/news_detail.html")
    }
}
