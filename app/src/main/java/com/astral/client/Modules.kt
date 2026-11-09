package com.astral.client

import android.content.Context

data class Module(val id: String, val category: String, val name: String, val hint: String)

/**
 * Список функций и их состояние (вкл/выкл).
 * Пока это только переключатели: сохраняются в настройках, но на игру ещё не влияют.
 */
object Modules {
    val categories = listOf(
        "combat" to "Бой",
        "visual" to "Визуал",
        "move" to "Движение",
        "misc" to "Прочее",
    )

    val all = listOf(
        Module("hitbox", "combat", "Hitbox", "Показ хитбоксов"),
        Module("nightvision", "visual", "Night Vision", "Яркость в темноте"),
        Module("zoom", "visual", "Zoom", "Приближение камеры"),
        Module("coords", "visual", "Координаты", "XYZ на экране"),
        Module("sprint", "move", "Auto Sprint", "Бег без удержания"),
        Module("fps", "misc", "FPS", "Счётчик кадров"),
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences("astral", Context.MODE_PRIVATE)

    fun isOn(context: Context, id: String): Boolean = prefs(context).getBoolean(id, false)

    fun set(context: Context, id: String, value: Boolean) {
        prefs(context).edit().putBoolean(id, value).apply()
    }

    fun activeCount(context: Context): Int = all.count { isOn(context, it.id) }
}
