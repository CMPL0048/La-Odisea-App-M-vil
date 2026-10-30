package com.laodisea.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.laodisea.app.BuildConfig
import com.laodisea.app.domain.AvatarRef
import com.laodisea.app.domain.avatarPublicUrl
import com.laodisea.app.domain.parseAvatarUrl
import com.laodisea.app.ui.theme.Fredoka

/**
 * Avatar del jugador a partir de `players.avatar_url`: avatar precargado, foto propia (Coil)
 * o, si no hay ninguno, el de Víctor.
 */
@Composable
fun OdiseaAvatar(avatarUrl: String?, displayName: String?, size: Dp, modifier: Modifier = Modifier) {
    val description = "Avatar de ${displayName?.takeIf { it.isNotBlank() } ?: "jugador"}"
    when (val ref = remember(avatarUrl) { parseAvatarUrl(avatarUrl) }) {
        is AvatarRef.Photo -> AsyncImage(
            model = avatarPublicUrl(BuildConfig.SUPABASE_URL, ref),
            contentDescription = description,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = painterResource(AvatarPresets.default.drawable!!),
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
        is AvatarRef.Preset -> PresetAvatar(AvatarPresets.find(ref.key) ?: AvatarPresets.default, size, modifier, description)
        AvatarRef.Default -> PresetAvatar(AvatarPresets.default, size, modifier, description)
    }
}

/** Avatar precargado: su imagen o, mientras no haya arte, un círculo de color con la inicial. */
@Composable
fun PresetAvatar(preset: AvatarPreset, size: Dp, modifier: Modifier = Modifier, contentDescription: String? = preset.name) {
    if (preset.drawable != null) {
        Image(
            painter = painterResource(preset.drawable),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
        return
    }
    val fontSize = with(LocalDensity.current) { (size * 0.45f).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(preset.color)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            preset.name.take(1),
            color = preset.contentColor,
            style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = fontSize),
        )
    }
}
