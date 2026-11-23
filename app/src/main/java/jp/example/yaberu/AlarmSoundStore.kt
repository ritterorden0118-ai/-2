package jp.example.yaberu

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

data class AlarmSoundOption(val uri: String, val title: String)

object AlarmSoundStore {
    private const val PREF = "yaberu_alarm_sounds"
    private const val KEY_SELECTED = "selected_uris"

    fun available(context: Context): List<AlarmSoundOption> {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val cursor = manager.cursor
        val result = mutableListOf<AlarmSoundOption>()
        if (cursor != null) {
            try {
                var position = 0
                while (cursor.moveToNext()) {
                    val uri = manager.getRingtoneUri(position)
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: "アラーム音 ${position + 1}"
                    if (uri != null) result += AlarmSoundOption(uri.toString(), title)
                    position++
                }
            } finally {
                cursor.close()
            }
        }
        return result.distinctBy { it.uri }
    }

    fun selected(context: Context): Set<String> =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getStringSet(KEY_SELECTED, emptySet())?.toSet() ?: emptySet()

    fun saveSelected(context: Context, uris: Set<String>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putStringSet(KEY_SELECTED, uris).apply()
    }

    fun chooseUri(context: Context): Uri? {
        val options = available(context)
        val chosen = options.filter { it.uri in selected(context) }.randomOrNull()
        return chosen?.let { Uri.parse(it.uri) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
}
