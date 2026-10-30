# La Odisea — Aplicación móvil (Android)

Documentación de la app móvil que acompaña al videojuego de PC **La Odisea**, una aventura por los
campus de Nayarit. La app no es el juego: es el **compañero de bolsillo** del jugador. Con la misma
cuenta del juego de PC puede consultar su avance, revisar su mochila y su bestiario, leer las noticias
del campus y jugar dos minijuegos que dan monedas y objetos que después aparecen en el juego.

---

## Índice

1. [Qué hace la app](#1-qué-hace-la-app)
2. [Tecnologías](#2-tecnologías)
3. [Cómo compilar y ejecutar](#3-cómo-compilar-y-ejecutar)
4. [Arquitectura](#4-arquitectura)
5. [Estructura de carpetas](#5-estructura-de-carpetas)
6. [Sesión y cuentas](#6-sesión-y-cuentas)
7. [Datos, sincronización y modo sin conexión](#7-datos-sincronización-y-modo-sin-conexión)
8. [Navegación](#8-navegación)
9. [Pantallas, una por una](#9-pantallas-una-por-una)
10. [Minijuegos](#10-minijuegos)
11. [Backend en Supabase](#11-backend-en-supabase)
12. [Relación con el juego de PC (Unity)](#12-relación-con-el-juego-de-pc-unity)
13. [Pruebas](#13-pruebas)
14. [Seguridad](#14-seguridad)
15. [Estado del proyecto y pendientes](#15-estado-del-proyecto-y-pendientes)

---

## 1. Qué hace la app

| Sección | Para qué sirve |
|---|---|
| **Inicio** | Resumen del jugador: vida, resistencia, nivel, XP, zona actual, progreso, consejo de Juanma, intentos de minijuegos y última noticia. |
| **Mapa** | Campus recorridos, el actual y los que faltan. Solo se ve si el jugador tiene el objeto **Mapa**. |
| **Mochila** | Objetos que lleva el jugador, con su detalle, y el **Muestrario** con todos los objetos del juego. |
| **Bestiario** | Personajes descubiertos (protagonistas, aliados y jefes). Los secretos solo aparecen al desbloquearse. |
| **Más** | Perfil (avatar, nombre de jugador, correo y **Mis partidas**), ajustes (tema claro u oscuro), estadísticas y cerrar sesión. |
| **Mis partidas** | Hasta 5 partidas por cuenta: la principal y las del juego de PC jugadas sin cuenta y vinculadas con un QR. La **activa** es la que se ve en toda la app y la que recibe los premios. |
| **Minijuegos** | *Despierta a Salva* y *Negocia con Vicente*: dan monedas y objetos. |
| **Noticias** | Avisos del campus, de la más reciente a la más antigua. |

Toda la información viene del mismo servidor que usa el juego de PC (Supabase), así que lo que
el jugador hace en un lado se ve en el otro.

---

## 2. Tecnologías

| Área | Herramienta |
|---|---|
| Lenguaje | Kotlin 2.4 |
| Interfaz | Jetpack Compose + Material 3 |
| Arquitectura | MVVM (ViewModel + StateFlow) con repositorios |
| Inyección de dependencias | Hilt |
| Navegación | Navigation Compose con rutas tipadas (`@Serializable`) |
| Backend | Supabase (Auth + PostgREST + Storage) con `supabase-kt` 3.8 y Ktor/OkHttp |
| Caché local | DataStore Preferences (JSON con kotlinx.serialization) |
| Imágenes | Coil 3 |
| Pruebas | JUnit 4 |

Requisitos del dispositivo: **Android 8.0 (API 26)** o superior.

---

## 3. Cómo compilar y ejecutar

1. Instalar **Android Studio** (trae su propio JDK 17+).
2. Clonar el repositorio y abrir la carpeta raíz en Android Studio.
3. Crear o editar el archivo `local.properties` en la raíz (no se sube a git) con:

   ```properties
   sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
   SUPABASE_URL=https://<ref-del-proyecto>.supabase.co
   SUPABASE_KEY=<llave pública (anon/publishable) de Supabase>
   ```

   Gradle lee estos dos valores y los pone en `BuildConfig.SUPABASE_URL` y `BuildConfig.SUPABASE_KEY`.
   Sin ellos la app compila, pero no puede conectarse.
4. Sincronizar Gradle y ejecutar la configuración `app` en un emulador o teléfono.

Desde la terminal:

```bash
./gradlew :app:assembleDebug        # genera el APK de depuración
./gradlew :app:testDebugUnitTest    # corre las pruebas unitarias
```

Para entrar se puede usar cualquier cuenta del juego, o crear una nueva desde la pantalla de registro.

---

## 4. Arquitectura

La app sigue tres capas. Cada una solo conoce a la de abajo:

```
┌──────────────────────────────────────────────┐
│  UI (Compose)                                │  pantallas + ViewModels
│  ui/…                                        │  observan StateFlow y llaman funciones
├──────────────────────────────────────────────┤
│  Dominio (Kotlin puro)                       │  reglas que se pueden probar sin Android:
│  domain/…                                    │  mapa, mochila, bestiario, estadísticas,
│                                              │  lógica de los minijuegos
├──────────────────────────────────────────────┤
│  Datos                                       │  repositorios: hablan con Supabase
│  data/…                                      │  y guardan la caché en DataStore
└──────────────────────────────────────────────┘
```

- **Repositorios** (`data/repository`): son la única puerta al servidor y a la caché.
  - `AuthRepository`: inicio de sesión, registro, recuperación de contraseña, restauración y cierre de sesión.
  - `PlayerRepository`: descarga la foto del jugador, su perfil (nombre y avatar), los minijuegos, las noticias y los catálogos, y los guarda.
  - `ProfileRepository`: cambia el nombre de jugador y el avatar (precargado, foto propia o ninguno) y sube la foto a Storage.
  - `SavesRepository`: vincula, activa, renombra y desvincula partidas del juego de PC.
  - `MinigameRepository`: llama a las funciones de los minijuegos.
  - `SettingsRepository`: preferencias del dispositivo (tema).
- **Dominio** (`domain`): funciones puras que combinan la foto del jugador con los catálogos para
  cada pantalla (`GameViews.kt`), la lógica de los minijuegos (`SalvaGame`, `Negotiation`), el
  formato de `avatar_url` y la validación del nombre (`Profile.kt`) y el recorte de la foto de perfil (`AvatarImage.kt`).
- **`data/AvatarImageProcessor.kt`**: prepara la foto elegida en la galería (rotación EXIF, recorte
  cuadrado centrado, 512 × 512, WEBP calidad 85, menos de 1 MB).
- **UI** (`ui`): cada pantalla tiene su `@Composable` y lee el estado de un ViewModel con
  `collectAsStateWithLifecycle()`. Las pantallas de solo lectura comparten `GameViewModel`.
- **Plantillas** (`ui/templates/ScreenTemplates.kt`): todas las pantallas se arman sobre 4
  plantillas (lista/cuadrícula, detalle, formulario y minijuego). Así comparten la barra superior,
  el gesto de deslizar para actualizar, el aviso sin conexión y los estados de carga, vacío y error.
- **Hilt** (`di/AppModule.kt`) crea una sola instancia del cliente de Supabase, del JSON y del DataStore.

---

## 5. Estructura de carpetas

```
app/src/main/java/com/laodisea/app/
├── LaOdiseaApp.kt            # Application (Hilt)
├── MainActivity.kt           # Actividad única; aplica el tema y monta la navegación
├── di/AppModule.kt           # Supabase, JSON y DataStore
├── data/
│   ├── OdiseaJson.kt         # Configuración de kotlinx.serialization
│   ├── ErrorMessages.kt      # Traduce errores de red/servidor a mensajes en español
│   ├── local/PlayerCache.kt  # Caché en DataStore (foto del jugador y catálogos)
│   ├── model/                # Modelos: PlayerSnapshot, HomeData, Catalog…
│   └── repository/           # Auth, Player, Minigame, Settings
├── domain/
│   ├── GameViews.kt          # Mapa, mochila, muestrario, bestiario, estadísticas
│   └── minigames/            # SalvaGame.kt, Negotiation.kt
└── ui/
    ├── navigation/AppNavHost.kt   # Splash → Login/Registro → Principal
    ├── main/MainScreen.kt         # Barra inferior y rutas de las 5 secciones
    ├── splash/ auth/ home/ news/ map/ backpack/ bestiary/
    ├── minigames/ stats/ settings/ more/ profile/
    ├── common/GameViewModel.kt    # ViewModel compartido de lectura
    ├── components/                # Tarjetas, barras, chips, barra superior e inferior
    ├── templates/                 # Las 4 plantillas de pantalla
    ├── theme/                     # Colores, tipografía y tema claro/oscuro
    └── util/                      # Formatos de fecha y tiempo, etiquetas

app/src/test/…                # Pruebas unitarias (JUnit)
tools/integration/            # Pruebas del contrato con Unity contra Supabase
tools/proposals/              # Cambios de servidor propuestos (sin aplicar)
```

---

## 6. Sesión y cuentas

- **Inicio de sesión** con correo y contraseña. Es la **misma cuenta del juego de PC**.
- **Registro**: nombre de jugador (mínimo 3 letras), correo válido, contraseña (mínimo 6
  caracteres) y su confirmación. El nombre se manda como `display_name`. Si el proyecto pide
  confirmar el correo, la app avisa «Revisa tu correo» y regresa al inicio de sesión.
- **Recuperar contraseña** («¿Olvidaste tu contraseña?» en el inicio de sesión). Supabase Auth genera
  y manda un **código de 6 dígitos**; la app solo tiene las pantallas:
  1. *Recuperar acceso*: el jugador escribe su correo (prellenado si ya lo había escrito) y la app
     llama a `resetPasswordForEmail`. Siempre responde «Si el correo está registrado, te enviamos un
     código de 6 dígitos.», exista o no la cuenta, para no revelar qué correos están registrados.
     Solo la falta de red o el límite de envíos se muestran como error.
  2. *Nueva contraseña*: código (solo dígitos, exactamente 6), nueva contraseña (mínimo 6) y su
     confirmación. La app valida el código con `verifyEmailOtp(type = RECOVERY)`, que abre la sesión,
     y después cambia la contraseña con `updateUser`. Si el cambio falla, al reintentar no se vuelve a
     validar el código (ya se usó). «Reenviar código» se habilita tras 60 s.
     Si con el código ya validado el jugador regresa, se cierra esa sesión (como «Cerrar sesión»)
     y vuelve al inicio de sesión; por eso «Cambiar de correo» se oculta en ese momento.
  3. Al terminar entra a la pantalla principal igual que un inicio de sesión normal (incluida la
     limpieza de caché si es otra cuenta).
  - Errores en español: código inválido o caducado, «Espera un momento antes de pedir otro código»
    y contraseña igual a la anterior.
  - Las reglas de código y contraseña son funciones puras en `ui/auth/AuthValidation.kt`.
- **Splash**: el splash del sistema muestra el ícono de la app (la cara de Víctor) y luego la
  pantalla de la app muestra el logo completo. Se espera a que Supabase cargue la sesión guardada
  (mínimo 0.8 s para que se vea el logo):
  - Sesión válida → pantalla principal.
  - Sin red para renovar el token → **también entra**, en modo sin conexión con los datos guardados.
  - Sin sesión → inicio de sesión.
- **Cambio de cuenta**: si entra una cuenta distinta a la dueña de la caché, la caché se borra
  para no mostrar datos de otra persona.
- **Cerrar sesión** (en *Más*) borra la sesión y la caché del jugador. El tema elegido se conserva
  porque es del dispositivo, no de la cuenta.

---

## 7. Datos, sincronización y modo sin conexión

La app funciona con el principio **«primero lo guardado, luego lo nuevo»**:

1. Al abrir una pantalla se muestra al instante lo que hay en la caché (DataStore).
2. En paralelo se pide al servidor la versión más reciente con `PlayerRepository.sync()`.
3. Al llegar, se guarda en la caché y, como las pantallas observan la caché, se actualizan solas.

**Qué se descarga en cada sincronización** (en paralelo):

| Dato | Origen en Supabase |
|---|---|
| Foto de la **partida activa** (stats, monedas, zonas, mochila, bestiario, recompensas pendientes y `save`) | `app.get_active_snapshot()`; si el servidor aún no la tiene, `public.get_player_snapshot()` |
| Partidas de la cuenta (principal y vinculadas) | `app.list_my_saves()` |
| Nombre y avatar | tabla `public.players` (fila propia) |
| Minijuegos y sus intentos restantes | tabla `app.minigames` + `app.get_minigame_state(p_code)` |
| Noticias del campus (las últimas 50) | tabla `app.campus_news` ordenada por `published_at` desc |
| Catálogos: objetos, personajes, zonas | tablas `items`, `characters`, `zones` |

Detalles importantes:

- Solo corre **una sincronización a la vez**: si ya hay una en curso, no se lanza otra.
- El estado de la sincronización (`SyncStatus`: cargando, sin conexión o error) se comparte entre todas las pantallas.
- **Sin conexión**: aparece el aviso «Sin conexión. Datos guardados hace…» y se sigue mostrando la caché.
- **Error del servidor con datos guardados**: aparece un aviso rojo con el mensaje y la pantalla sigue usable.
- Si **solo fallan las noticias, el perfil o la lista de partidas**, se conserva lo que ya estaba guardado.
- **Cambiar de partida** (activar, vincular o desvincular) marca la foto guardada como vieja y
  sincroniza de inmediato, para no mostrar datos de otra partida.
- **Sin la migración 08 en el servidor**: si `app.get_active_snapshot` no existe, la app usa
  `public.get_player_snapshot` y ya no lo vuelve a intentar mientras siga abierta. Si
  `list_my_saves` tampoco existe, no se muestra nada de partidas.
- Las pantallas de lectura solo vuelven a descargar si lo guardado tiene **más de 1 minuto**.
  En cualquier pantalla se puede **deslizar hacia abajo** para forzar la actualización.
- Después de cada partida de minijuego se sincroniza otra vez para ver monedas e intentos al día.

---

## 8. Navegación

Hay dos niveles de navegación:

**Raíz** (`ui/navigation/AppNavHost.kt`):

```
Splash ──► Inicio de sesión ◄──► Registro
   │              │      └──► Recuperar acceso ──► Nueva contraseña
   │              │                                      │
   └──────────────┴──────────────────────────────────────┴──► Principal (barra inferior)
```

Las rutas son tipadas: `ForgotPasswordRoute(email = "")` lleva el correo prellenado y
`ResetPasswordRoute(email)` el correo al que se mandó el código. Entrar a Principal (desde el
splash, el inicio de sesión, el registro o la nueva contraseña) limpia la pila.

**Principal** (`ui/main/MainScreen.kt`): una barra inferior con 5 secciones. Cada sección tiene
su propio grafo, así la barra marca la sección correcta aunque se esté en un detalle, y cada
sección recuerda dónde se quedó.

```
Inicio ──► Estadísticas
       ──► Noticias del campus
       ──► Minijuegos ──► Despierta a Salva ──► Recompensa
                      └─► Negocia con Vicente ──► Recompensa
Mapa
Mochila ──► Detalle de objeto
        ──► Muestrario ──► Detalle de objeto
Bestiario ──► Detalle de personaje
Más ──► Perfil
    ──► Ajustes
    ──► Estadísticas
```

Tocar el avatar de la barra superior de **Inicio** cambia a la pestaña **Más** y abre **Perfil**
(la barra inferior marca Más). En **Inicio**, el chip con el nombre de la partida activa abre una
hoja para cambiar de partida, sin salir de Inicio.

Al terminar una partida, el juego se reemplaza por la pantalla de **Recompensa**; «atrás» regresa a la lista de minijuegos.

---

## 9. Pantallas, una por una

### Splash
Logo de La Odisea (ya trae el nombre) y lema mientras se restaura la sesión. Decide a dónde ir (ver [§6](#6-sesión-y-cuentas)).

### Inicio de sesión y Registro
Logo arriba y formularios con validación inmediata, botón para mostrar u ocultar la contraseña y
mensajes de error en español. Debajo de la contraseña, «¿Olvidaste tu contraseña?» abre la recuperación.

### Recuperar acceso
Campo de correo (misma validación que el registro) y botón «Enviar código». Siempre avisa «Si el
correo está registrado, te enviamos un código de 6 dígitos.» y pasa a *Nueva contraseña*.

### Nueva contraseña
Código de 6 dígitos (teclado numérico), nueva contraseña y confirmación, ambas con mostrar u
ocultar. «Reenviar código» queda deshabilitado con una cuenta regresiva de 60 s. Al cambiar la
contraseña entra directo a la pantalla principal.

### Inicio
Arriba, la barra con el nombre del jugador, su nivel, su avatar y sus monedas. Debajo, tarjetas:
- **Tus stats**: barras de vida y resistencia, nivel, XP, enemigos derrotados y tiempo jugado. «Ver todo» abre Estadísticas.
- **Zona actual**: nombre del campus, subnivel y barra de progreso total de la odisea. Arriba a la
  derecha, un chip con la **partida activa**: al tocarlo se abre una hoja para elegir otra.
- **Consejo de Juanma**: un consejo que cambia según la situación del jugador.
- **Minijuegos**: intentos restantes de cada uno y cuándo se recargan. «Jugar» abre la lista.
- **Última noticia**: la noticia más reciente. «Ver todas» abre Noticias del campus.

### Noticias del campus
Lista de las noticias (categoría, título, resumen y fecha), de la más reciente a la más antigua.
Si no hay noticias, muestra «Aún no hay noticias». Funciona sin conexión con las últimas descargadas.

### Estadísticas
Resumen completo: datos de Víctor (el protagonista), avance (subniveles y campus completados,
enemigos derrotados, tiempo de juego) y colección (objetos en la mochila contra la capacidad,
personajes descubiertos).

### Mapa
- **Sin el objeto Mapa**: mensaje «Aún no tienes el mapa».
- **Con el objeto Mapa**: lista de campus en orden de recorrido, cada uno con su estado:
  - completada, actual o bloqueada;
  - subniveles completados y jefe del campus;
  - si la ruta en combi ya está desbloqueada.
- Los campus ocultos que el jugador aún no alcanza aparecen como «Campus misterioso».

### Mochila
Cuadrícula con los objetos y los huecos libres hasta la capacidad. Al tocar un objeto se abre su
**detalle**: categoría, rareza, cantidad, descripción, efecto y precio de referencia con Vicente.
También da acceso al **Muestrario**.

### Muestrario
Todos los objetos del juego, agrupados por categoría (clave, equipo, curación). Sin el objeto
**Muestrario de materiales**, solo se ve la información de los objetos que el jugador ya tiene;
los demás aparecen como «Objeto no descubierto».

### Bestiario
Cuadrícula de personajes ordenada: primero los descubiertos y después por papel (protagonista,
aliado, jefe). Los no descubiertos aparecen con candado y «???». Los secretos no aparecen hasta
desbloquearse. Al tocar uno descubierto se abre su detalle.

### Más
Tarjeta de la cuenta (avatar, nombre y correo) que abre **Perfil**, acceso a **Ajustes** y a **Estadísticas**, botón
**Cerrar sesión** y, al final, el logo pequeño con la versión de la app.

### Perfil
- **Avatar** de 120 dp con el botón **Cambiar**, que abre una hoja con:
  - los 8 avatares precargados en 4 columnas (el actual va marcado);
  - **Subir foto de la galería**: abre el selector de fotos del sistema, la app recorta el cuadrado
    central y muestra una vista previa con **Usar esta foto** antes de subirla;
  - **Quitar foto** (solo si hay foto propia): vuelve al avatar de Víctor.
- **Nombre de jugador**: mínimo 3 letras y máximo 24. Se guarda en el servidor y se ve también en el juego de PC.
- **Correo**: solo lectura.
- **Mis partidas (n/5)**:
  - Una tarjeta por partida con nombre, campus, nivel, tiempo jugado y «Última vez hace…». La
    activa lleva el chip **Activa**.
  - Al tocar otra partida, la app pregunta «¿Ver y jugar minijuegos con <nombre>?» y luego la activa.
  - Las vinculadas tienen un menú con **Renombrar** (1 a 24 caracteres) y **Desvincular**. Al
    desvincular, la partida sigue en la PC y se puede volver a vincular. La principal no tiene menú.
  - **Escanear QR** abre el escáner de Google Play (`play-services-code-scanner`, solo QR). La
    cámara la maneja Google Play, así que la app **no pide permiso de cámara**. Si el escáner no
    está disponible (sin Play Services o sin cámara), la app lo avisa y abre **Escribir código**.
  - **Escribir código**: un campo de 8 caracteres que se pasa a mayúsculas y pierde espacios y guiones.
  - Al vincular sale el aviso «Partida vinculada: <nombre>», más el nombre del equipo si viene.
  - Con 5 partidas, los dos botones se deshabilitan con «Desvincula una partida para agregar otra».
  - Debajo se explica: «Juega en la PC sin cuenta y escanea el QR que aparece en el juego para
    traer esa partida aquí.»

Los avatares precargados están en una sola lista (`ui/components/AvatarPresets.kt`). Por ahora
Víctor usa su cara y los demás (Salva, Juanma, Vicente, Boniel, Osiel, Elías y Toño) son un círculo
de color con su inicial. Para poner el arte final basta con agregar el drawable a esa lista.
Todas las pantallas dibujan el avatar con el mismo componente, `OdiseaAvatar`.

### Ajustes
- **Tema**: igual que el teléfono, claro u oscuro. Se guarda en el dispositivo.

---

## 10. Minijuegos

Reglas generales, todas **validadas por el servidor**:
- Cada minijuego tiene **4 intentos cada 4 horas**.
- Las recompensas (monedas u objeto) las decide el servidor; la app solo muestra el resultado.
- Los **intentos son por cuenta**, pero las monedas y los objetos van a la **partida activa**.
  Minijuegos y Recompensa muestran «Premios para: <partida activa>». Con la migración 08,
  `play_minigame_attempt` y `make_offer` también devuelven el `save_id` que recibió el premio.
- Los objetos ganados **llegan a esa partida la próxima vez que se sincronice el juego** (quedan como recompensas pendientes).

### Despierta a Salva (`DESPIERTA_SALVA`)
Barra de precisión: un marcador va y viene, y hay que tocar cuando pasa por la **zona dorada**.
- **3 aciertos** despiertan a Salva (éxito); **3 fallos** lo dejan dormido (fallo).
- Cada acierto **acelera** el marcador y **achica** la zona.
- Salir a media partida cuenta como fallo (la app pide confirmación).
- Al terminar se manda el resultado con `app.play_minigame_attempt(p_code, p_result)`. Si falla
  la red, se puede reintentar el envío.
- La lógica está en `domain/minigames/SalvaGame.kt` (Kotlin puro, con pruebas).

### Negocia con Vicente (`NEGOCIA_VICENTE`)
Vicente el Águila ofrece un objeto a precio inflado y el jugador regatea:
- `app.start_negotiation()` abre una negociación y gasta un intento. Si había una negociación abierta, se retoma sin gastar otro.
- El jugador hace ofertas con `app.make_offer(p_negotiation_id, p_offer)`. Hay botones de oferta rápida (40, 60 y 80 % del precio).
- El servidor guarda un **precio mínimo secreto**. Si la oferta lo alcanza, Vicente **acepta** y
  vende por lo ofrecido (se descuentan las monedas). Si no, hace una **contraoferta**.
- Las ofertas muy bajas le quitan más **paciencia**. Su ánimo pasa de *Tranquilo* a *Impaciente*,
  *Molesto* y *A punto de irse*. Si se le acaba la paciencia o los turnos, **se retira**.
- Antes de enviar, la app revisa que la oferta no esté vacía, no supere las monedas del jugador ni el precio pedido.
- La lógica está en `domain/minigames/Negotiation.kt` (Kotlin puro, con pruebas).

---

## 11. Backend en Supabase

Un solo proyecto de Supabase lo comparten el juego de PC y la app. Tiene tres esquemas:

| Esquema | Contenido |
|---|---|
| `public` | Datos del jugador compartidos con el juego: `players`, `player_stats`, `player_wallet`, `coin_transactions`, `player_inventory`, `player_zone_progress`, `player_bestiary_unlocks`, `pending_rewards`, catálogos `items`, `characters`, `zones`. |
| `app` | Lo exclusivo de la app: `minigames`, negociaciones con Vicente, `campus_news` y, con la migración 08, `linked_saves` y `active_save`. |
| `game` | Lo exclusivo del juego de PC. |

**Funciones que usa la app**

| Función | Uso |
|---|---|
| `public.get_player_snapshot()` | Foto completa del jugador autenticado. |
| `app.get_minigame_state(p_code)` | Intentos restantes y hora de recarga. |
| `app.play_minigame_attempt(p_code, p_result)` | Registra una partida de Salva y da la recompensa. |
| `app.start_negotiation()` / `app.make_offer(...)` | Negociación con Vicente. |
| `app.get_active_snapshot()` | Foto de la partida activa, más `save` (migración 08). |
| `app.list_my_saves()` | Partidas de la cuenta, la principal primero (migración 08). |
| `app.link_save(p_code)` / `app.set_active_save(p_save_id)` | Vincular una partida (queda activa) y cambiar la activa (migración 08). |
| `app.rename_save(p_save_id, p_name)` / `app.unlink_save(p_save_id)` | Renombrar o desvincular una vinculada (migración 08). |

Además, la app lee y actualiza `players.display_name` y `players.avatar_url` directamente (el
snapshot no trae el avatar). Las políticas RLS solo permiten leer y editar la fila propia.

**Avatares** (migración `07_avatars_storage`)
- `players.avatar_url` tiene un `CHECK` que solo acepta:
  - `null`: sin avatar (la app muestra el de Víctor);
  - `preset:<clave>`, con clave `^[a-z0-9_]{1,32}$`: avatar precargado;
  - `<uid>/avatar.webp?v=<número>`: foto propia. El número cambia en cada subida para que la caché
    de imágenes no muestre la foto anterior.
- Bucket **público** `avatars` en Storage: máximo 1 MB, solo `image/webp`, `image/jpeg` o `image/png`.
  Cada jugador solo puede subir o actualizar dentro de su carpeta `{auth.uid()}/`. No hay política
  de borrado: la app siempre sobrescribe el mismo archivo (`upsert`).
- La URL pública es `<SUPABASE_URL>/storage/v1/object/public/avatars/<uid>/avatar.webp?v=<número>`.

**Partidas vinculadas** (migración `08_partidas_vinculadas_qr`, **sin aplicar todavía**)
- `app.linked_saves`: partidas del juego de PC (jugadores anónimos) vinculadas a cada cuenta.
  Hasta 4 por cuenta, más la principal.
- `app.active_save`: la partida activa de cada cuenta (null = la principal).
- Los errores de `link_save` ya vienen en español y la app los muestra tal cual. El contrato
  completo está en `tools/proposals/08_partidas_vinculadas_qr.md`.

**Seguridad en el servidor**
- Toda función identifica al jugador con `auth.uid()` (el token de la sesión). Nunca recibe un id de jugador desde la app.
- Las funciones son `SECURITY DEFINER` con `search_path` vacío, y validan intentos, saldo y montos.
- Las tablas tienen RLS: cada jugador solo lee y escribe lo suyo. Las noticias y los catálogos son de lectura pública.

---

## 12. Relación con el juego de PC (Unity)

El juego de PC usa la misma cuenta y el mismo servidor. Sus llamadas principales son:

| Llamada | Qué hace |
|---|---|
| Inicio de sesión con contraseña (`/auth/v1/token?grant_type=password`) | Obtiene el token del jugador. |
| `public.get_player_snapshot()` | Carga la partida. |
| `public.claim_pending_rewards()` | Entrega a la mochila y al saldo lo ganado en la app (minijuegos). |
| `public.save_game_progress(p_payload)` | Guarda stats, zonas, mochila, capacidad y bestiario. La vida y la resistencia se recortan a su máximo. Unity debe mandar siempre los máximos: si faltan, vuelven a 100. |
| `public.adjust_coins(p_amount, p_reason)` | Suma o gasta monedas (de 1 a 1000 por llamada; sin saldo negativo). |

Así fluye una recompensa: **app** gana objeto o monedas → queda en `pending_rewards` → **juego de PC**
llama a `claim_pending_rewards` al iniciar → aparece en la mochila o el saldo → la **app** lo ve en
la siguiente sincronización.

**Jugar sin cuenta y vincular con QR** (migración 08). Lo que debe hacer Unity:

1. Si no hay cuenta, iniciar una **sesión anónima** con `signInAnonymously`
   (`/auth/v1/signup` sin correo). El progreso se guarda igual, con las mismas funciones.
2. Para vincular, llamar a `public.create_save_link_code(p_device_name)` y mostrar:
   - el **QR** generado con `qr_payload` (`laodisea://vincular?c=CODIGO`);
   - el **código en texto** (8 caracteres), por si el teléfono no puede escanear.
3. Consultar `public.get_save_link_status()` para mostrar «Vinculada a <nombre>» cuando el
   jugador ya escaneó el QR.
4. Para la **entrega inmediata** de premios, escuchar por Realtime los `INSERT` de
   `public.pending_rewards` de su jugador y llamar a `claim_pending_rewards` al recibirlos, sin
   esperar a reiniciar el juego.

---

## 13. Pruebas

**Pruebas unitarias** (`app/src/test`, se corren con `./gradlew :app:testDebugUnitTest`):

| Prueba | Qué verifica |
|---|---|
| `PlayerSnapshotTest` | Que la respuesta real de `get_player_snapshot` se decodifica bien. |
| `GameViewsTest` | Mapa, mochila, muestrario, bestiario y estadísticas. |
| `SalvaGameTest` | Movimiento, aciertos, fallos, aceleración y fin de partida de Salva. |
| `NegotiationTest` | Validación de ofertas, respuestas de Vicente y ánimo. |
| `ProfileTest` | Lectura y escritura de `avatar_url`, URL pública de la foto y validación del nombre de jugador. |
| `AvatarImageTest` | Recorte cuadrado centrado y submuestreo de la foto de perfil. |
| `SaveLinkTest` | Lectura del QR y del código escrito a mano (normalización y errores) y nombre de partida. |
| `SavesTest` | Decodifica `list_my_saves`, `get_active_snapshot` con `save` y `link_save` desde los ejemplos de `test/resources`. También revisa los mensajes del servidor, la detección de «función no existe» y «Última vez hace…». |
| `AuthValidationTest` | Código de recuperación (solo dígitos, exactamente 6) y reglas de la contraseña nueva. |

**Pruebas de integración con Unity** (`tools/integration`):

- **`unity_contract_test.sql`** se pega en el SQL Editor de Supabase. Simula, como el jugador demo:
  - `claim_pending_rewards` y `save_game_progress`;
  - `adjust_coins` con montos válidos y con montos que deben rechazarse;
  - una llamada sin sesión.

  Al final **deshace todos los cambios** y devuelve un reporte en JSON.
- **`unity_http_test.ps1`** hace las mismas llamadas **por HTTP**, como Unity:
  - login con contraseña, snapshot, claim, save y adjust_coins, además de las llamadas sin sesión;
  - lee la URL y la llave de `local.properties` y pide la contraseña al ejecutarse;
  - restaura los stats al terminar y solo prueba montos que el servidor rechaza, así que no deja cambios.

  ```powershell
  powershell -ExecutionPolicy Bypass -File tools\integration\unity_http_test.ps1
  ```

  Opciones:
  - `-Claim`: entrega de verdad las recompensas pendientes.
  - `-WithCoins`: prueba +1/-1. El saldo queda igual, pero quedan dos movimientos registrados.

---

## 14. Seguridad

- La **llave pública** y la URL de Supabase viven **solo en `local.properties`**, que está en
  `.gitignore`. Nunca se suben al repositorio.
- La app nunca usa la llave de servicio (`service_role`). Todo lo hace como el jugador
  autenticado, y las reglas las impone el servidor (RLS y funciones).
- Las reglas sensibles (precio mínimo de Vicente, recompensas, intentos, saldo) **solo existen en
  el servidor**. Aunque alguien modifique la app, no puede darse monedas ni objetos.
- La compilación de *release* activa la minificación (R8).
- **Permisos**: solo `INTERNET` y `ACCESS_NETWORK_STATE`. Para la foto de perfil se usa el selector de
  fotos del sistema (`PickVisualMedia`), que **no pide permisos** de almacenamiento ni de cámara: la
  app solo recibe la foto que el jugador elige.
- El **escáner de QR** es el de Google Play (`GmsBarcodeScanning`). La cámara la abre Google Play
  Services, así que la app **no gana permisos nuevos** (ni `CAMERA`): solo recibe el texto del QR.
- Las fotos de perfil están en un bucket público: cualquiera con la URL puede verlas, pero solo su
  dueño puede reemplazarlas.

---

## 15. Estado del proyecto y pendientes

| Sprint | Contenido | Estado |
|---|---|---|
| 1 | Proyecto base, tema, sesión (splash, inicio de sesión, registro) | ✅ |
| 2 | Inicio y pantallas de lectura: Mapa, Mochila, Muestrario, Bestiario, Estadísticas, Más, Ajustes | ✅ |
| 3 | Minijuegos: Despierta a Salva y Negocia con Vicente, pantalla de recompensa | ✅ |
| 4 | Noticias del campus y pruebas de integración con Unity | ✅ |
| UX docente, fase 1 | Logo nuevo (ícono, splash, sesión, Más) y recuperar contraseña con código de 6 dígitos | ✅ |
| UX docente, fase 2 | Perfil con avatares (8 precargados y foto propia); Ajustes queda solo con el tema | ✅ |
| UX docente, fase 3 | Partidas vinculadas por QR (lado app), contra el contrato de la migración 08 | ✅ app / ⏳ servidor |

**Pendiente**
- **Aplicar la migración 08** (partidas vinculadas por QR): Martin la aprobó el 30 oct 2026. Hasta
  que esté aplicada, la app usa `get_player_snapshot` y oculta todo lo de partidas.
  «Allow anonymous sign-ins» ya está activado en Supabase (30 oct 2026).
- **Unity**: sesión anónima, QR de vinculación, estado de vinculación y Realtime de premios (ver §12).
- **Arte final de los avatares**: hoy, salvo Víctor, son círculos con la inicial. Basta con agregar
  cada imagen en `ui/components/AvatarPresets.kt`.
- **Plantilla de correo «Reset password» de Supabase** (Authentication → Email Templates): debe
  mostrar el código con `{{ .Token }}`. La plantilla por defecto solo trae el enlace
  (`{{ .ConfirmationURL }}`), y con ella al jugador no le llega ningún código que escribir en la app.
- **Tope diario de monedas** para `adjust_coins` (hoy permite +1000 por llamada sin límite).
  Hay una propuesta en `tools/proposals/adjust_coins_tope_diario.sql` (máximo +2000 por jugador
  al día), **sin aplicar** porque cambia el contrato con Unity y requiere aprobación.
- Cargar noticias reales en `app.campus_news` (hoy la tabla está vacía).
