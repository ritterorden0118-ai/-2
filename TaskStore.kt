package jp.example.yaberu

import android.content.Context
import org.json.JSONArray

object TaskStore {
    private const val PREF = "yaberu_tasks"
    fun load(context: Context): MutableList<TodoTask> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("items", "[]") ?: "[]"
        return runCatching { val a=JSONArray(raw); MutableList(a.length()) { i -> TodoTask.fromJson(a.getJSONObject(i)) } }.getOrDefault(mutableListOf())
    }
    fun save(context: Context, tasks: List<TodoTask>) {
        val a=JSONArray(); tasks.forEach { a.put(it.toJson()) }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("items", a.toString()).apply()
    }
}
