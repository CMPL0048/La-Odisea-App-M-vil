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
| **Más** | Cuenta, ajustes (nombre de jugador y tema claro u oscuro), estadísticas y cerrar sesión. |
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
| Backend | Supabase (Auth + PostgREST) con `supabase-kt` 3.8 y Ktor/OkHttp |
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
  - `AuthRepository`: inicio de sesión, registro, restauración y cierre de sesión.
  - `PlayerRepository`: descarga la foto del jugador, los minijuegos, las noticias y los catálogos, y los guarda.
  - `MinigameRepository`: llama a las funciones de los minijuegos.
  - `SettingsRepository`: preferencias del dispositivo (tema).
- **Dominio** (`domain`): funciones puras que combinan la foto del jugador con los catálogos para
  cada pantalla (`GameViews.kt`) y la lógica de los minijuegos (`SalvaGame`, `Negotiation`).
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
    ├── minigames/ stats/ settings/ more/
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
- **Splash**: al abrir la app se espera a que Supabase cargue la sesión guardada (mínimo 0.8 s
  para que se vea el logo):
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
| Foto del jugador (stats, monedas, zonas, mochila, bestiario, recompensas pendientes) | `public.get_player_snapshot()` |
| Minijuegos y sus intentos restantes | tabla `app.minigames` + `app.get_minigame_state(p_code)` |
| Noticias del campus (las últimas 50) | tabla `app.campus_news` ordenada por `published_at` desc |
| Catálogos: objetos, personajes, zonas | tablas `items`, `characters`, `zones` |

Detalles importantes:

- Solo corre **una sincronización a la vez**: si ya hay una en curso, no se lanza otra.
- El estado de la sincronización (`SyncStatus`: cargando, sin conexión o error) se comparte entre todas las pantallas.
- **Sin conexión**: aparece el aviso «Sin conexión. Datos guardados hace…» y se sigue mostrando la caché.
- **Error del servidor con datos guardados**: aparece un aviso rojo con el mensaje y la pantalla sigue usable.
- Si **solo fallan las noticias**, se conservan las que ya estaban guardadas.
- Las pantallas de lectura solo vuelven a descargar si lo guardado tiene **más de 1 minuto**.
  En cualquier pantalla se puede **deslizar hacia abajo** para forzar la actualización.
- Después de cada partida de minijuego se sincroniza otra vez para ver monedas e intentos al día.

---

## 8. Navegación

Hay dos niveles de navegación:

**Raíz** (`ui/navigation/AppNavHost.kt`):

```
Splash ──► Inicio de sesión ◄──► Registro
   │              │
   └──────────────┴──► Principal (barra inferior)
```

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
Más ──► Ajustes y perfil
    ──► Estadísticas
```

Al terminar una partida, el juego se reemplaza por la pantalla de **Recompensa**; «atrás» regresa a la lista de minijuegos.

---

## 9. Pantallas, una por una

### Splash
Logo, nombre y lema mientras se restaura la sesión. Decide a dónde ir (ver [§6](#6-sesión-y-cuentas)).

### Inicio de sesión y Registro
Formularios con validación inmediata, botón para mostrar u ocultar la contraseña y mensajes de error en español.

### Inicio
Arriba, la barra con el nombre del jugador, su nivel, su avatar y sus monedas. Debajo, tarjetas:
- **Tus stats**: barras de vida y resistencia, nivel, XP, enemigos derrotados y tiempo jugado. «Ver todo» abre Estadísticas.
- **Zona actual**: nombre del campus, subnivel y barra de progreso total de la odisea.
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
Tarjeta de la cuenta (nombre y correo), acceso a **Ajustes y perfil** y a **Estadísticas**, botón
**Cerrar sesión** y versión de la app.

### Ajustes y perfil
- **Nombre de jugador**: mínimo 3 letras. Se guarda en el servidor y se ve también en el juego de PC.
- **Tema**: igual que el teléfono, claro u oscuro. Se guarda en el dispositivo.

---

## 10. Minijuegos

Reglas generales, todas **validadas por el servidor**:
- Cada minijuego tiene **4 intentos cada 4 horas**.
- Las recompensas (monedas u objeto) las decide el servidor; la app solo muestra el resultado.
- Los objetos ganados **llegan a la mochila la próxima vez que se abre el juego de PC** (quedan como recompensas pendientes).

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
| `app` | Lo exclusivo de la app: `minigames`, negociaciones con Vicente, `campus_news`. |
| `game` | Lo exclusivo del juego de PC. |

**Funciones que usa la app**

| Función | Uso |
|---|---|
| `public.get_player_snapshot()` | Foto completa del jugador autenticado. |
| `app.get_minigame_state(p_code)` | Intentos restantes y hora de recarga. |
| `app.play_minigame_attempt(p_code, p_result)` | Registra una partida de Salva y da la recompensa. |
| `app.start_negotiation()` / `app.make_offer(...)` | Negociación con Vicente. |

Además, la app actualiza `players.display_name` directamente. Las políticas RLS solo permiten editar la fila propia.

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

---

## 13. Pruebas

**Pruebas unitarias** (`app/src/test`, se corren con `./gradlew :app:testDebugUnitTest`):

| Prueba | Qué verifica |
|---|---|
| `PlayerSnapshotTest` | Que la respuesta real de `get_player_snapshot` se decodifica bien. |
| `GameViewsTest` | Mapa, mochila, muestrario, bestiario y estadísticas. |
| `SalvaGameTest` | Movimiento, aciertos, fallos, aceleración y fin de partida de Salva. |
| `NegotiationTest` | Validación de ofertas, respuestas de Vicente y ánimo. |

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

---

## 15. Estado del proyecto y pendientes

| Sprint | Contenido | Estado |
|---|---|---|
| 1 | Proyecto base, tema, sesión (splash, inicio de sesión, registro) | ✅ |
| 2 | Inicio y pantallas de lectura: Mapa, Mochila, Muestrario, Bestiario, Estadísticas, Más, Ajustes | ✅ |
| 3 | Minijuegos: Despierta a Salva y Negocia con Vicente, pantalla de recompensa | ✅ |
| 4 | Noticias del campus y pruebas de integración con Unity | ✅ |

**Pendiente**
- **Tope diario de monedas** para `adjust_coins` (hoy permite +1000 por llamada sin límite).
  Hay una propuesta en `tools/proposals/adjust_coins_tope_diario.sql` (máximo +2000 por jugador
  al día), **sin aplicar** porque cambia el contrato con Unity y requiere aprobación.
- Cargar noticias reales en `app.campus_news` (hoy la tabla está vacía).
