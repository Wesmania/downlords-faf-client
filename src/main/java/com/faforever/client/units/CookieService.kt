package com.faforever.client.units

import com.faforever.client.preferences.Preferences
import com.faforever.client.preferences.PreferencesService
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.stream.Collectors

@Component
class CookieService @Inject
constructor(private val preferencesService: PreferencesService) {
    private val storedCookies: MutableMap<URI, ArrayList<HttpCookie>>

    init {
        val preferences = preferencesService.preferences
        storedCookies = preferences!!.storedCookies
    }


    fun setUpCookieManger() {
        val manager = CookieManager(MyCookieStore(), CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(manager)
    }

    inner class MyCookieStore : CookieStore {

        override fun add(uri: URI, cookie: HttpCookie) {
            val base = URI.create(uri.host)
            if (!storedCookies.containsKey(base)) {
                storedCookies[base] = ArrayList()
            }
            storedCookies[base].stream()
                    .filter { httpCookie -> httpCookie == cookie }
                    .findFirst()
                    .ifPresent { httpCookie -> storedCookies[base].remove(httpCookie) }
            storedCookies[base].add(cookie)
            preferencesService.storeInBackground()
        }


        override fun get(uri: URI): List<HttpCookie> {
            return if (storedCookies.containsKey(URI.create(uri.host))) storedCookies[URI.create(uri.host)] else emptyList()
        }


        override fun getCookies(): List<HttpCookie> {
            return storedCookies.values.stream().flatMap<HttpCookie>(Function<ArrayList<HttpCookie>, Stream<out HttpCookie>> { it.stream() }).collect<List<HttpCookie>, Any>(Collectors.toList())
        }


        override fun getURIs(): List<URI> {
            return ArrayList(preferencesService.preferences!!.storedCookies.keys)
        }


        override fun remove(uri: URI, cookie: HttpCookie): Boolean {
            if (storedCookies.containsKey(URI.create(uri.host))) {
                val remove = storedCookies[URI.create(uri.host)].remove(cookie)
                preferencesService.storeInBackground()
                return remove
            }
            return false
        }


        override fun removeAll(): Boolean {
            storedCookies.clear()
            preferencesService.storeInBackground()
            return true
        }
    }
}
