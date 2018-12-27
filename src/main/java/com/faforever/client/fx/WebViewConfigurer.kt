package com.faforever.client.fx

import com.faforever.client.preferences.PreferencesService
import com.faforever.client.theme.UiService
import javafx.concurrent.Worker.State
import javafx.event.EventHandler
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class WebViewConfigurer(private val uiService: UiService, private val applicationContext: ApplicationContext, private val preferencesService: PreferencesService) {

    fun configureWebView(webView: WebView) {
        val engine = webView.engine
        //    Accessor.getPageFor(engine).setBackgroundColor(0);
        webView.isContextMenuEnabled = false
        webView.setOnScroll { event ->
            if (event.isControlDown) {
                webView.zoom = webView.zoom + ZOOM_STEP * Math.signum(event.deltaY)
            }
        }
        webView.setOnKeyPressed { event ->
            if (event.isControlDown && (event.code == KeyCode.DIGIT0 || event.code == KeyCode.NUMPAD0)) {
                webView.zoom = 1.0
            }
        }

        val browserCallback = applicationContext.getBean(BrowserCallback::class.java)
        val moveHandler = { event ->
            browserCallback.setLastMouseX(event.getScreenX())
            browserCallback.setLastMouseY(event.getScreenY())
        }
        webView.addEventHandler(MouseEvent.MOUSE_MOVED, moveHandler)

        engine.userDataDirectory = preferencesService.cacheDirectory.toFile()
        uiService.registerWebView(webView)
        JavaFxUtil.addListener(engine.loadWorker.stateProperty()) { observable, oldValue, newValue ->
            if (newValue != State.SUCCEEDED) {
                return@JavaFxUtil.addListener
            }
            uiService.registerWebView(webView)

            (engine.executeScript("window") as JSObject).setMember(JAVA_REFERENCE_IN_JAVASCRIPT, browserCallback)

            val document = webView.engine.document
            if (document == null) {
                return@JavaFxUtil.addListener
            }

            val nodeList = document!!.getElementsByTagName("a")
            for (i in 0 until nodeList.length) {
                val link = nodeList.item(i) as Element
                val href = link.getAttribute("href")

                link.setAttribute("onMouseOver", "java.previewUrl('$href')")
                link.setAttribute("onMouseOut", "java.hideUrlPreview()")
                link.setAttribute("href", "javascript:java.openUrl('$href');")
            }
        }
    }

    companion object {

        /**
         * This is the member name within the JavaScript code that provides access to the Java callback instance.
         */
        private val JAVA_REFERENCE_IN_JAVASCRIPT = "java"
        private val ZOOM_STEP = 0.2
    }
}
