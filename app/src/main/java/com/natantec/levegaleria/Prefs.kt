package com.natantec.levegaleria

import android.content.Context

/** Preferências simples guardadas no aparelho. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("leve_galeria", Context.MODE_PRIVATE)

    /** IDs das pastas (bucket) que o usuário escolheu ocultar. */
    var hidden: Set<Long>
        get() = (sp.getStringSet("hidden", emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        set(value) {
            sp.edit().putStringSet("hidden", value.map { it.toString() }.toSet()).apply()
        }

    var favorites: Set<String>
        get() = HashSet(sp.getStringSet("favorites", emptySet()) ?: emptySet())
        set(value) {
            sp.edit().putStringSet("favorites", HashSet(value)).apply()
        }

    /** 0 = sistema, 1 = claro, 2 = escuro. */
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(value) {
            sp.edit().putInt("theme", value).apply()
        }

    var columns: Int
        get() = sp.getInt("columns", 3)
        set(value) {
            sp.edit().putInt("columns", value).apply()
        }
}
