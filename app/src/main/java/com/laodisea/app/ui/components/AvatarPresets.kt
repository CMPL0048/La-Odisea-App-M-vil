package com.laodisea.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.laodisea.app.R
import com.laodisea.app.ui.theme.BosqueClaro
import com.laodisea.app.ui.theme.BosqueMedio
import com.laodisea.app.ui.theme.BosqueProfundo
import com.laodisea.app.ui.theme.NocheAzul
import com.laodisea.app.ui.theme.OroMoneda
import com.laodisea.app.ui.theme.OroOscuro
import com.laodisea.app.ui.theme.Papel
import com.laodisea.app.ui.theme.Resistencia
import com.laodisea.app.ui.theme.Tinta
import com.laodisea.app.ui.theme.Vida

/**
 * Avatar precargado. Si tiene [drawable] se muestra esa imagen; si no, un círculo de [color] con
 * la inicial del nombre. Para poner el arte final basta con agregar el drawable aquí.
 */
data class AvatarPreset(
    /** Lo que se guarda en el servidor como `preset:<key>`. */
    val key: String,
    val name: String,
    @param:DrawableRes val drawable: Int? = null,
    val color: Color = BosqueMedio,
    val contentColor: Color = Papel,
)

/** Única lista de avatares precargados (provisionales hasta tener el arte de las diseñadoras). */
object AvatarPresets {
    val all: List<AvatarPreset> = listOf(
        AvatarPreset("victor", "Víctor", drawable = R.drawable.avatar_victor),
        AvatarPreset("salva", "Salva", color = Resistencia),
        AvatarPreset("juanma", "Juanma", color = BosqueMedio),
        AvatarPreset("vicente", "Vicente", color = OroMoneda, contentColor = Tinta),
        AvatarPreset("boniel", "Boniel", color = Vida),
        AvatarPreset("osiel", "Osiel", color = NocheAzul),
        AvatarPreset("elias", "Elías", color = OroOscuro),
        AvatarPreset("tono", "Toño", color = BosqueClaro, contentColor = BosqueProfundo),
    )

    /** El que se ve cuando el jugador no ha elegido ninguno (o la clave ya no existe). */
    val default: AvatarPreset = all.first()

    fun find(key: String): AvatarPreset? = all.firstOrNull { it.key == key }
}
