package com.faforever.client.news

import com.faforever.client.config.ClientProperties
import com.faforever.client.preferences.PreferencesService
import com.google.common.eventbus.EventBus
import com.rometools.rome.feed.synd.SyndCategory
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.Comparator
import java.util.Date
import java.util.Objects
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

import com.github.nocatch.NoCatch.noCatch

@Lazy
@Service
class NewsService(clientProperties: ClientProperties, private val preferencesService: PreferencesService, private val eventBus: EventBus,
                  private val taskScheduler: TaskScheduler) {

    private val newsFeedUrl: String

    init {
        this.newsFeedUrl = clientProperties.getNews().getFeedUrl()
    }

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
        taskScheduler.scheduleWithFixedDelay({ this.pollForNews() }, Date.from(Instant.now().plusSeconds(5)), POLL_DELAY)
    }

    private fun pollForNews() {
        fetchNews().thenAccept { newsItems ->
            newsItems.stream().findFirst()
                    .ifPresent { newsItem ->
                        val lastReadNewsUrl = preferencesService.preferences!!.news.lastReadNewsUrl
                        if (newsItem.getLink() != lastReadNewsUrl) {
                            eventBus.post(UnreadNewsEvent(true))
                        }
                    }
        }
    }

    @Async
    fun fetchNews(): CompletableFuture<List<NewsItem>> {
        return CompletableFuture.completedFuture(
                noCatch<SyndFeed> { SyndFeedInput().build(XmlReader(URL(newsFeedUrl))) }.getEntries().stream()
                        .map(Function<SyndEntry, NewsItem> { this.toNewsItem(it) })
                        .sorted(Comparator.comparing(Function<Any, Any> { getDate() }).reversed())
                        .collect(Collectors.toList<NewsItem>()))
    }

    private fun toNewsItem(syndEntry: SyndEntry): NewsItem {
        val author = syndEntry.author
        val link = syndEntry.link
        val title = syndEntry.title
        val content = syndEntry.contents[0].value
        val publishedDate = syndEntry.publishedDate

        val newsCategory = syndEntry.categories.stream()
                .filter(Predicate<SyndCategory> { Objects.nonNull(it) })
                .findFirst()
                .map<String>(Function<SyndCategory, String> { it.getName() })
                .map<NewsCategory>(Function<String, NewsCategory> { NewsCategory.fromString(it) })
                .orElse(NewsCategory.UNCATEGORIZED)

        return NewsItem(author, link, title, content, publishedDate, newsCategory)
    }

    companion object {

        /** The delay (in seconds) between polling for new news.  */
        private val POLL_DELAY = Duration.ofMinutes(10).toMillis()
    }
}
