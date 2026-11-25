package com.example.rewardshub

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Account(val id: String, val name: String)
data class Task(val id: String, val label: String, val hint: String)

val TASKS = listOf(
    Task("daily_set", "Set diario", "Las actividades del día"),
    Task("cards", "Tarjetas de actividades", "Promociones y encuestas"),
    Task("search_pc", "Búsquedas de PC", "Con el modo PC activado"),
    Task("search_mobile", "Búsquedas móviles", "Con el modo Móvil activado"),
    Task("checkin", "Check-in y bonos", "Racha y bonos semanales")
)

/** Guardado local simple (SharedPreferences). Nada sale del teléfono. */
class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("rewards_hub", Context.MODE_PRIVATE)

    fun accounts(): List<Account> {
        val raw = p.getString("accounts", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            arr.getJSONObject(i).let { Account(it.getString("id"), it.getString("name")) }
        }
    }

    fun saveAccounts(list: List<Account>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name)) }
        p.edit().putString("accounts", arr.toString()).apply()
    }

    var selectedId: String?
        get() = p.getString("selected", null)
        set(v) { p.edit().putString("selected", v).apply() }

    // El checklist se reinicia solo cada día porque la clave incluye la fecha.
    private fun key(acc: String) = "done_${acc}_${LocalDate.now()}"

    fun done(acc: String): Set<String> =
        p.getStringSet(key(acc), emptySet())?.toSet() ?: emptySet()

    fun saveDone(acc: String, s: Set<String>) {
        p.edit().putStringSet(key(acc), s).apply()
    }
}
