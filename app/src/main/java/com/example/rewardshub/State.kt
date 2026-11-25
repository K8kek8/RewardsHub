package com.example.rewardshub

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.UUID

enum class Tab(val label: String, val url: String?) {
    HOY("Hoy", null),
    BUSCAR("Buscar", "https://www.bing.com"),
    TARJETAS("Tarjetas", "https://rewards.bing.com"),
    PUNTOS("Puntos", "https://rewards.bing.com/pointsbreakdown")
}

const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"

class WebSession(val webView: WebView) {
    var tab: Tab? = null
    var progress by mutableIntStateOf(100)
    var desktop by mutableStateOf(true)
    var mobileUa: String = ""
}

class AppState(private val ctx: Context) {
    private val store = Store(ctx)
    private val sessions = mutableMapOf<String, WebSession>()

    val accounts = mutableStateListOf<Account>().apply { addAll(store.accounts()) }
    private val doneMap = mutableStateMapOf<String, Set<String>>()
    var selectedId by mutableStateOf(store.selectedId ?: accounts.firstOrNull()?.id)
    var tab by mutableStateOf(Tab.HOY)
    var profilesSupported = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

    init { reload() }

    val selected: Account? get() = accounts.firstOrNull { it.id == selectedId }

    /** Recarga el checklist (útil al volver a la app en un día nuevo). */
    fun reload() {
        doneMap.clear()
        accounts.forEach { doneMap[it.id] = store.done(it.id) }
    }

    fun done(accId: String): Set<String> = doneMap[accId] ?: emptySet()

    fun toggleTask(accId: String, taskId: String) {
        val cur = done(accId)
        val next = if (taskId in cur) cur - taskId else cur + taskId
        doneMap[accId] = next
        store.saveDone(accId, next)
    }

    fun resetToday(accId: String) {
        doneMap[accId] = emptySet()
        store.saveDone(accId, emptySet())
    }

    fun select(acc: Account) {
        selectedId = acc.id
        store.selectedId = acc.id
    }

    fun addAccount(name: String) {
        val acc = Account(UUID.randomUUID().toString().replace("-", "").take(10), name.trim())
        accounts.add(acc)
        store.saveAccounts(accounts.toList())
        doneMap[acc.id] = emptySet()
        select(acc)
    }

    fun rename(acc: Account, name: String) {
        val i = accounts.indexOfFirst { it.id == acc.id }
        if (i >= 0) {
            accounts[i] = acc.copy(name = name.trim())
            store.saveAccounts(accounts.toList())
        }
    }

    fun remove(acc: Account) {
        sessions.remove(acc.id)?.webView?.destroy()
        if (profilesSupported) {
            runCatching { ProfileStore.getInstance().deleteProfile(profileName(acc)) }
        }
        accounts.removeAll { it.id == acc.id }
        doneMap.remove(acc.id)
        store.saveAccounts(accounts.toList())
        val next = accounts.firstOrNull()
        selectedId = next?.id
        store.selectedId = next?.id
    }

    fun sessionFor(acc: Account): WebSession =
        sessions.getOrPut(acc.id) { createSession(acc) }

    fun toggleDesktop(s: WebSession) {
        s.desktop = !s.desktop
        applyUa(s)
        s.webView.reload()
    }

    private fun profileName(acc: Account) = "acc_${acc.id}"

    private fun applyUa(s: WebSession) {
        s.webView.settings.userAgentString = if (s.desktop) DESKTOP_UA else s.mobileUa
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createSession(acc: Account): WebSession {
        val wv = WebView(ctx)

        // Cada cuenta tiene su propio perfil (cookies y almacenamiento aislados).
        if (profilesSupported) {
            val name = profileName(acc)
            val profile = ProfileStore.getInstance().getOrCreateProfile(name)
            WebViewCompat.setProfile(wv, name)
            profile.cookieManager.setAcceptThirdPartyCookies(wv, true)
        } else {
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
        }

        val st = wv.settings
        st.javaScriptEnabled = true
        st.domStorageEnabled = true
        st.useWideViewPort = true
        st.loadWithOverviewMode = true
        st.setSupportZoom(true)
        st.builtInZoomControls = true
        st.displayZoomControls = false

        val session = WebSession(wv)
        session.mobileUa = st.userAgentString.replace("; wv", "")
        applyUa(session)

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme
                // Solo http/https dentro de la app; esquemas raros (intent://, ms-...) se ignoran.
                return scheme != "http" && scheme != "https"
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                session.progress = newProgress
            }
        }
        return session
    }
}
