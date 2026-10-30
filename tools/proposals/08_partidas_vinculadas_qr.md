# Migración 08: partidas vinculadas por QR (contrato)

> **Estado: APROBADA por Martin (30 oct 2026), pendiente de aplicar.** La app ya está
> programada contra este contrato. Mientras la migración no exista, la app sigue funcionando
> como antes (ver «Compatibilidad»).

## Idea

El juego de PC se puede jugar **sin cuenta**: Unity abre una **sesión anónima** de Supabase y esa
partida queda en el servidor. Desde la app, el jugador escanea el QR que muestra el juego y esa
partida se **agrega a su cuenta**.

Cada cuenta ve **hasta 5 partidas**:

- su **Partida principal**: la de la propia cuenta. Siempre está y no se renombra ni se desvincula;
- **hasta 4 vinculadas**.

Una de ellas es la **activa**. Es la que se ve en la app (Inicio, Mapa, Mochila, Bestiario y
Estadísticas) y a la que llegan los premios de los minijuegos. Los **intentos** de los minijuegos
siguen siendo **por cuenta**.

## Tablas nuevas

| Tabla | Contenido |
|---|---|
| `app.linked_saves` | Partidas vinculadas a cada cuenta: dueño, jugador anónimo, nombre y fecha. |
| `app.active_save` | Partida activa de cada cuenta (null = la principal). |

## Funciones para la app

Todas están en el esquema `app`, se llaman por RPC con la sesión de la cuenta y lanzan los
errores con el mensaje ya en español.

| Función | Respuesta | Notas |
|---|---|---|
| `list_my_saves()` | arreglo jsonb de `{save_id, name, is_principal, is_active, linked_at, level, zone_code, zone_name, playtime_seconds, coins, last_played}` | La principal va primero. Los datos de juego pueden venir null. |
| `link_save(p_code text)` | `{save_id, name, device_name}` | La partida vinculada queda como la activa. |
| `set_active_save(p_save_id uuid)` | `{active_save_id}` | Con el id de la principal o con null vuelve a la principal. |
| `rename_save(p_save_id, p_name)` | `{save_id, name}` | De 1 a 24 caracteres. Solo para vinculadas. |
| `unlink_save(p_save_id)` | `{unlinked}` | **No borra el progreso**: la partida sigue en la PC y se puede volver a vincular. Si era la activa, vuelve la principal. |
| `get_active_snapshot()` | el mismo JSON que `public.get_player_snapshot()`, pero de la partida activa, más `"save": {save_id, name, is_principal}` | La app lo usa en lugar de `get_player_snapshot` al sincronizar. |

Errores de `link_save` (la app los muestra tal cual):

- «El código no es válido o ya caducó»
- «Esta partida ya está vinculada a una cuenta»
- «Ya tienes 5 partidas. Desvincula una para agregar otra»
- «No puedes vincular tu propia cuenta»

Minijuegos: `play_minigame_attempt` y `make_offer` mandan las monedas y los objetos a la
**partida activa** y además devuelven `save_id`.

## Funciones para Unity (`public`)

| Función | Uso |
|---|---|
| `create_save_link_code(p_device_name)` | Genera el código y devuelve `qr_payload` para dibujar el QR. |
| `get_save_link_status()` | Dice si la partida ya está vinculada y a qué cuenta, para mostrar «Vinculada a <nombre>». |

## Código y QR

- El código tiene **8 caracteres** de `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`, sin O, I, 0 ni 1 para que
  no se confundan.
- El texto del QR es `laodisea://vincular?c=CODIGO`.
- La app también acepta el código solo. Lo pasa a mayúsculas y le quita espacios y guiones
  (`domain/SaveLink.kt`, con pruebas en `SaveLinkTest`).

## Compatibilidad en la app

- `PlayerRepository` llama a `app.get_active_snapshot`. Si el servidor responde que la función no
  existe (PGRST202 o 42883), cae a `public.get_player_snapshot` y no lo vuelve a intentar mientras
  la app siga abierta. Si `list_my_saves` no existe, la lista queda vacía.
- Con la lista vacía no aparecen la sección «Mis partidas», el chip de Inicio ni «Premios para».
- `PlayerRepository.USE_ACTIVE_SNAPSHOT = false` obliga a usar siempre `get_player_snapshot`.

## Pendiente en Supabase

1. ~~Aprobar~~ (aprobada el 30 oct 2026) y aplicar la migración 08.
2. ~~Activar **Allow anonymous sign-ins**~~ (activado el 30 oct 2026).
