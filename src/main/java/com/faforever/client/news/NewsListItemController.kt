package com.faforever.client.news

import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.theme.UiService
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class NewsListItemController(private val i18n: I18n, private val uiService: UiService) : Controller<Node> {
    override var root: Node? = null
    var imageView: ImageView? = null
    var titleLabel: Label? = null
    var authoredLabel: Label? = null

    fun setNewsItem(newsItem: NewsItem) {
        // TODO only use this if there's no thumbnail. However, there's never a thumbnail ATM.
        imageView!!.image = uiService.getThemeImage(newsItem.getNewsCategory().getImagePath())

        titleLabel!!.text = newsItem.getTitle()
        authoredLabel!!.text = i18n.get("news.authoredFormat", newsItem.getAuthor(), newsItem.getDate())
    }
}
