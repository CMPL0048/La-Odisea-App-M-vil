package com.laodisea.app.data

import kotlinx.serialization.json.Json

/** Configuración JSON común para Supabase y la caché local: tolera campos nuevos del servidor. */
val OdiseaJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
