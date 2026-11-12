# Eventia — Aplicación móvil para descubrir eventos y lugares

Proyecto final de **Programación de Aplicaciones Móviles 1**.
Aplicación Android en **Kotlin** que permite explorar eventos próximos y lugares desde una API REST,
con funcionamiento **sin Internet** (caché local Room), favoritos persistentes, sesión y mapa.

Documento de referencia: *Proyecto Final Eventia* (RF-01 a RF-14).

---

## 1. Estado actual del desarrollo

| Módulo | Requisitos | Estado |
|---|---|---|
| A — Catálogo | RF-01, RF-02, RF-03, RF-06 | ✅ Implementado y compilando |
| B — Lugares | RF-13 | ✅ Implementado y compilando |
| C — Favoritos y offline | RF-04, RF-05, RF-14 | ✅ Implementado y compilando |
| D — Cuenta e inscripciones | RF-11, RF-12 | ✅ Implementado y compilando |
| E — Mapa | RF-07, RF-08, RF-09 | ✅ Implementado y compilando (requiere API key de Google Maps para teselas) |

**Verificación de build (ejecutada):**

```
gradlew :app:compileDebugKotlin   → BUILD SUCCESSFUL
gradlew :app:assembleDebug        → BUILD SUCCESSFUL  (genera app-debug.apk)
```

Incluye el procesamiento de **Hilt** y **Room** (KSP) sin errores.

---

## 2. Reglas del proyecto y tecnologías usadas

| Área | Regla obligatoria | Implementación en el proyecto |
|---|---|---|
| Lenguaje | Kotlin | `language`/`jvmTarget` 17, todo el código en Kotlin |
| Interfaz | Jetpack Compose + Material 3 | `ui/` (pantallas, componentes, tema) |
| Arquitectura | MVVM + Repository | `ui/viewmodel/` + `domain/repository/` + `data/repository/` |
| API | Retrofit + Gson | `di/NetworkModule.kt`, `data/remote/EventiaApi.kt` |
| Base de datos local | Room | `data/local/EventiaDatabase.kt` (events, categories, favorites, venues) |
| Sesión | DataStore | `data/datastore/SessionDataStore.kt` (token y última sincronización) |
| Inyección de dependencias | Hilt + `@HiltViewModel` | `di/` (3 módulos) + ViewModels anotados |
| Estado | StateFlow | Todos los ViewModels exponen `StateFlow<UiState>` |
| Procesos asíncronos | Kotlin Coroutines | `viewModelScope`, `Flow`, `withTransaction` |
| Navegación | Navigation Compose | `ui/navigation/EventiaNavGraph.kt` |
| Imágenes | Coil | `AsyncImage` en `EventCard` |
| Mapas | Maps Compose (aprobada) | Dependencia agregada, pendiente de implementar |
| Versionamiento | Git con etiqueta final `v1.0` | Pendiente: etiqueta al cierre |
| DTO → Dominio | Obligatorio | `data/mapper/EventMapper.kt`, `VenueMapper.kt` |

---

## 3. Estructura de carpetas (real)

```
Prueva_evidia/
├── build.gradle.kts                  # Plugins de nivel raíz (AGP, Kotlin, Hilt, KSP)
├── settings.gradle.kts               # Incluye :app
├── gradle/
│   ├── libs.versions.toml            # Catálogo de versiones/dependencias
│   └── wrapper/                      # Gradle 8.11.1
├── app/
│   ├── build.gradle.kts              # Dependencias + BuildConfig.BASE_URL
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml       # INTERNET, estado de red, cleartext para localhost
│       ├── res/values/               # strings, themes
│       └── java/com/example/eventia/
│           ├── EventiaApplication.kt         # @HiltAndroidApp
│           ├── MainActivity.kt               # @AndroidEntryPoint + setContent
│           ├── di/
│           │   ├── NetworkModule.kt          # OkHttp (AuthInterceptor) + Retrofit + Gson
│           │   ├── DatabaseModule.kt         # Room + DAOs
│           │   └── RepositoryModule.kt       # @Binds de repositorios
│           ├── data/
│           │   ├── NetworkMonitor.kt         # Estado de conexión (StateFlow<Boolean>)
│           │   ├── remote/
│           │   │   ├── EventiaApi.kt         # Interfaz Retrofit (12 endpoints)
│           │   │   ├── AuthInterceptor.kt    # Cabecera Authorization: Bearer
│           │   │   ├── ApiError.kt           # ApiException(code) + mensajes del servidor
│           │   │   └── dto/EventDto.kt       # DTOs (provisionales, ajustar al JSON real)
│           │   ├── local/
│           │   │   ├── EventiaDatabase.kt    # v3: events, categories, favorites, venues
│           │   │   ├── dao/                  # EventDao, VenueDao, FavoriteDao
│           │   │   └── entities/             # EventEntity, CategoryEntity, FavoriteEntity (snapshot), VenueEntity
│           │   ├── datastore/SessionDataStore.kt  # token (+ tokenFlow) + lastSync
│           │   ├── mapper/                   # EventMapper, VenueMapper, FavoriteMapper,
│           │   │                             # AttendanceMapper, UserMapper (DTO→Dominio)
│           │   └── repository/
│           │       ├── EventsRepositoryImpl.kt
│           │       ├── VenuesRepositoryImpl.kt
│           │       ├── FavoritesRepositoryImpl.kt  # solo Room (offline)
│           │       ├── AuthRepositoryImpl.kt       # token en DataStore, 401 limpia sesión
│           │       └── AttendanceRepositoryImpl.kt # POST/DELETE attendance + /me/attendances
│           ├── domain/
│           │   ├── model/                    # Event, Category, Venue, User, Attendance
│           │   └── repository/               # Interfaces: Events, Venues, Favorites, Auth, Attendance
│           └── ui/
│               ├── navigation/EventiaNavGraph.kt   # Rutas + barra inferior (4 pestañas)
│               ├── screens/                        # EventsScreen, EventDetailScreen,
│               │                                    # VenuesScreen, VenueDetailScreen,
│               │                                    # FavoritesScreen, AuthScreen,
│               │                                    # ProfileScreen, MapScreen
│               ├── components/                     # EventCard, VenueCard
│               ├── viewmodel/                      # Events, EventDetail, Venues, VenueDetail,
│               │                                   # Favorites, Auth, Profile, Map
│               └── theme/                          # Color, Type, Theme (Material 3)
├── local.properties                  # sdk.dir (solo local, ignorado por Git)
└── README.md                         # Este archivo
```

---

## 4. Flujo de datos

### 4.1 Catálogo con Internet (RF-01, RF-03, RF-06)

```
EventsScreen
   │  (búsqueda con debounce 300 ms + filtro por categoría combinables,
   │   botón "Actualizar", posición con rememberSaveable)
   ▼
EventsViewModel  ──  StateFlow<EventsUiState>  (SavedStateHandle conserva filtros)
   │                     ▲
   │ refresh()/loadInitial()                     Room (Flow)
   ▼                                                │
EventsRepository (interfaz) ◄──────────────── EventsRepositoryImpl
   │                                                ▲
   │ decide: red o caché                            │ transacción conRoom.withTransaction
   ▼                                                │ (reemplaza caché SIN tocar favoritos)
EventiaApi (Retrofit+Gson) ── GET /events ──────────┘
   │
   └── al responder OK: DTO ──EventMapper──► EventEntity ──► observeEvents(): Flow<List<Event>>
                                           (se guarda lastSync en DataStore)
```

- **Éxito:** los DTO se mapean a entidades Room y la UI observa `Flow` (Room es la fuente única de verdad).
- **Fallo:** no se borra nada; se emite `errorMessage` y la lista visible se conserva.
- La pantalla muestra la antigüedad real: `Sin conexión/datos · hace X h` calculada con `lastSync`.

### 4.2 Sin Internet / modo avión

```
EventiaApi falla (sin red)
   ▼
EventsRepositoryImpl cae a Room:
   - observeEvents()      → catálogo guardado
   - observeCategories()  → categorías guardadas
   - FavoriteDao          → favoritos (funcionan 100% offline)
Si Room también está vacío → estado de error/vacío con botón "Reintentar"
```

### 4.3 Lugares (RF-13)

```
VenuesScreen ──► VenuesViewModel ──► VenuesRepository ──► VenuesRepositoryImpl
                                                                │
                                          GET /venues ─────────┤
                                          Room (venues) ◄──────┘ (caché + fallback offline)

VenueDetailScreen ──► VenueDetailViewModel ──► GET /venues/{id} y GET /venues/{id}/events
                                   │              (fallback: eventDao.getEventsByVenue)
                                   └─► reutiliza EventCard y navega a event_detail/{id}
```

### 4.4 Favoritos (RF-04, RF-05, RF-14)

```
Detalle del evento (corazón)  o  pantalla Favoritos (botón quitar)
   ▼
FavoritesViewModel / EventDetailViewModel
   ▼
FavoritesRepository (interfaz) ── FavoritesRepositoryImpl ──► FavoriteDao ──► Room
                                          │
                    ┌─────────────────────┴──────────────────────┐
                    │  SOLO Room: nunca consulta Retrofit         │
                    │  → agregar/quitar funciona sin Internet     │
                    │  → snapshot completo del evento             │
                    │  → la sincronización del catálogo NO         │
                    │    modifica la tabla favorites              │
                    └────────────────────────────────────────────┘

Pantalla Favoritos: eliminar → SnackBar "Favorito eliminado" → [Deshacer] → re-inserta el snapshot
Detalle: observeFavoriteIds() → estado del corazón en tiempo real (Room → Flow → UI)

Aviso de conexión (NetworkMonitor):
   isConnected = false y lastSync = X  →  "Sin conexión · datos de hace X h"
   isConnected = false y sin caché     →  estado vacío/error + "Reintentar"
```

### 4.5 Cuenta e inscripciones (RF-11, RF-12)

```
AuthScreen (login/registro)                     ProfileScreen (pestaña 👤)
   │ validación local: correo + contraseña ≥ 8     │ sin sesión → botón "Iniciar sesión"
   ▼                                               │ con sesión → /me + /me/attendances
AuthViewModel ──► AuthRepository ──► AuthRepositoryImpl
                       │                 │ POST /auth/login | /auth/register
                       │                 │ guarda token en DataStore ──► AuthInterceptor
                       │                 │ (se agrega en Bearer a cada petición)
                       │                 └─ 401 en /me → clearToken → UI vuelve a "sin sesión"
                       └─ observeIsLoggedIn(): Flow<Boolean>  ← reacciona a login/logout/401

Detalle del evento:
   "Inscribirme" ── sin sesión ──► navega a /auth ── éxito ──► popBackStack() (regresa al evento)
       │ con sesión
       ▼
   AttendanceRepositoryImpl ── POST /events/{id}/attendance
       ├── 2xx  → isAttending = true, cupos -1, SnackBar "Inscripción confirmada"
       ├── 401  → clearToken + requiresLogin → redirige a login (sin cerrar la app)
       ├── 409  → SnackBar con el conflicto (ya inscrito / sin cupos)
       └── otro → mensaje de error; NO se marca como inscrito (regla 41)

Perfil: /me/attendances → split por fecha local → "Próximas" y "Pasadas" (EventCard)
```

### 4.6 Mapa (RF-07, RF-08, RF-09)

```
MapScreen (pestaña 📍 Mapa)
   │ chips de categoría (SavedStateHandle, igual que el catálogo)  ── RF-08
   ▼
MapViewModel ── combine(observeEvents(), observeCategories(), filtro)
   │             │ filtra con hasValidCoordinates():              ── RF-07
   │             │  lat ∈ [-90,90], lng ∈ [-180,180], no (0,0)
   ▼
GoogleMap (maps-compose) ── Marker(solo eventos válidos)
   │ tap marcador ──► EventSummaryCard (título, fecha, lugar, categoría)  ── RF-09
   │                     ├── [Ver detalle]  ──► event_detail/{id}
   │                     └── [Indicaciones] ──► Intent geo:lat,lng  (app externa)  ── RF-61
   │                            └─ sin app compatible ──► SnackBar de aviso       ── RF-62
   └── datos desde Room → funciona sin Internet

Sin marcadores: "No hay eventos cargados" o "Ningún evento tiene coordenadas válidas"
Ubicación actual y eventos cercanos: opcionales (no implementados, sin permisos)
```

### 4.7 Navegación (Navigation Compose)

```
bottomBar:  [ 🏠 Eventos ] [ 📍 Lugares ] [ 🗺 Mapa ] [ 🤍 Favoritos ] [ 👤 Perfil ]

events ──tap evento──► event_detail/{eventId} ──"Ver lugar →"──► venue_detail/{venueId}
   ▲                        │                                        │
   └──── back ◄─────────────┘◄──── tap evento (EventCard) ◄──────────┘

favorites ──tap evento──► event_detail/{eventId}   (corazón para quitar/agregar)
profile ──"Iniciar sesión"──► auth ──éxito──► popBackStack()  (perfil o evento de origen)
event_detail (sin sesión) ──"Inscribirme"──► auth ──éxito──► regresa al mismo evento
```

---

## 5. Endpoints consumidos hasta ahora

| Método | Endpoint | Uso | Estado |
|---|---|---|---|
| GET | `/events` | Listado con `search`/`category`/`upcoming` (RF-01/RF-03) | ✅ usado |
| GET | `/events/{id}` | Detalle con cupos (RF-02) | ✅ usado |
| GET | `/categories` | Filtro por categoría | ✅ usado |
| GET | `/venues` | Lista de lugares (RF-13) | ✅ usado |
| GET | `/venues/{id}` | Detalle de lugar | ✅ usado |
| GET | `/venues/{id}/events` | Eventos del lugar (RF-13) | ✅ usado |
| POST | `/auth/register`, `/auth/login` | Registro/login + token en DataStore (RF-11) | ✅ usado |
| GET | `/me` | Perfil autenticado; 401 limpia la sesión | ✅ usado |
| GET | `/me/attendances` | Inscripciones próximas/pasadas (RF-12) | ✅ usado |
| POST/DELETE | `/events/{id}/attendance` | Inscribirse/cancelar con confirmación del servidor | ✅ usado |

Base URL configurada en `app/build.gradle.kts` → `BuildConfig.BASE_URL = "http://localhost:8000/api/"`.

> **Importante sobre `localhost`:**
> - **Emulador:** `localhost` es el propio emulador; se debe usar `http://10.0.2.2:8000/api/`.
> - **Dispositivo físico:** la IP de la PC en la red local, p. ej. `http://192.168.x.x:8000/api/`.
> - `AndroidManifest.xml` incluye `usesCleartextTraffic="true"` porque la API es HTTP en desarrollo.
> - La URL no se ha cambiado de la indicada (`http://localhost:8000/api/`) para respetar el dato entregado.

---

## 6. Cómo compilar y probar

**Requisitos:** Android Studio (con SDK 35 y Build-Tools 35.x), JDK 17, backend de la asignatura corriendo.

```powershell
cd C:\Users\Usuario\Desktop\moviles2026\PRueva\Prueva_evidia
.\gradlew.bat :app:assembleDebug          # genera app-debug.apk
.\gradlew.bat :app:compileDebugKotlin     # solo compila (más rápido)
```

**Pruebas manuales — Módulo A (Catálogo):**
1. Abrir la app → se listan eventos próximos (si la API responde) o estado vacío con *Reintentar*.
2. Escribir en el buscador → la lista se filtra tras ~300 ms.
3. Elegir una categoría → se combina con el texto buscado; *Limpiar filtros* restablece.
4. Tocar *Actualizar* sin conexión → aparece el aviso, **la lista no se borra**.
5. Abrir un detalle y volver → se conservan búsqueda, categoría y posición de scroll.

**Pruebas manuales — Módulo B (Lugares):**
1. Pestaña *Lugares* → lista con nombre, dirección, horario y descripción.
2. Abrir un lugar → muestra sus datos y **próximos eventos** (con `EventCard`).
3. Tocar un evento → detalle del evento; desde el detalle, *Ver lugar →* regresa al lugar.
4. Rotar el dispositivo → `SavedStateHandle`/`rememberSaveable` conservan el estado.

**Pruebas manuales — Módulo C (Favoritos y offline):**
1. Abrir un evento → tocar el **corazón** → "Agregado a favoritos" (funciona en modo avión).
2. Ir a la pestaña *Favoritos* → aparece el evento guardado con la nota *"Guardados en tu dispositivo · disponibles sin Internet"*.
3. Tocar el corazón rojo para quitar → aparece SnackBar **"Favorito eliminado"** con acción **Deshacer** → al presionarlo, el favorito vuelve.
4. **Modo avión:** reiniciar la app → catálogo desde Room con aviso *"Sin conexión · datos de hace X h"* (antigüedad real desde la última sincronización); favoritos siguen visibles y editables.
5. Actualizar el catálogo con conexión → los favoritos **no desaparecen** (tabla separada con snapshot).
6. Sin conexión y sin caché → estado vacío con *Reintentar*.
7. Eliminar todos los favoritos → estado vacío explicativo.

**Pruebas manuales — Módulo D (Cuenta e inscripciones):**
1. Pestaña *Perfil* sin sesión → botón **Iniciar sesión** → pantalla `auth`.
2. Registrarse con correo inválido o contraseña < 8 caracteres → **errores de validación locales** (sin llamar a la API).
3. Registrarse/iniciar sesión con datos válidos → vuelve a la pantalla anterior (perfil o el evento de origen); el token queda en DataStore.
4. Cerrar sesión → el perfil vuelve al estado sin sesión; explorar el catálogo sigue funcionando **sin cuenta**.
5. En un evento con sesión: **Inscribirme** → tras confirmar el servidor cambia a **Cancelar inscripción** y los cupos bajan en 1.
6. **Cancelar inscripción** → vuelve a *Inscribirme* y los cupos suben.
7. Inscribirse dos veces (o sin cupos) → el servidor responde **409** y se muestra el mensaje de conflicto **sin** marcar la inscripción.
8. Sesión expirada (borrar token o expirarlo en el backend) → operación protegida con **401**: se limpia la sesión y se redirige al login **sin cerrar la app**.
9. *Perfil* con sesión → secciones **Próximas inscripciones** y **Pasadas** con `EventCard`; rotar el dispositivo conserva el estado.
10. Reiniciar la app con token → la sesión se **restaura** automáticamente (DataStore).

**Pruebas manuales — Módulo E (Mapa):**
1. Pestaña *Mapa* → se muestran marcadores **solo** de eventos con coordenadas válidas (la API puede devolver eventos sin ubicación → no aparecen).
2. Cambiar la categoría en los chips → el mapa se actualiza y la selección se limpia (filtro combinable).
3. Tocar un marcador → aparece el **resumen** del evento (título, fecha, lugar, categoría).
4. **Ver detalle** → abre `event_detail` del evento seleccionado; *volver* regresa al mapa (filtro conservado por `SavedStateHandle`).
5. **Indicaciones** → se abre Google Maps (u otra app de mapas) con la ruta al lugar.
6. Desinstar/deshabilitar apps de mapas → **Indicaciones** muestra SnackBar *“No se encontró una aplicación de mapas…”*.
7. Modo avión → el mapa sigue mostrando marcadores desde Room (teselas de fondo requieren conexión/key).
8. Catálogo vacío → estado *“No hay eventos cargados. Actualiza el catálogo.”*

---

## 7. Limitaciones y pendientes

1. **DTOs provisionales**: los campos de `EventDto`/`VenueDto`/`AttendanceDto`/`ServerErrorDto` fueron inferidos del documento; se ajustarán a los JSON reales de la API (no hay ejemplos de respuesta aún). Si un campo real difiere, el mapeo en `data/mapper/` es el único lugar a corregir.
2. **API key de Google Maps pendiente**: `MapScreen` está implementado, pero el manifest tiene el placeholder `YOUR_GOOGLE_MAPS_API_KEY`; sin la key real los marcadores y el resumen funcionan, pero **no se dibujan las teselas** del mapa. Reemplazarla en `AndroidManifest.xml` (`com.google.android.geo.API_KEY`).
3. **Clasificación próximas/pasadas**: usa `SimpleDateFormat` con varios patrones sobre la fecha local; si la API usa un formato no contemplado, las inscripciones se mostrarán como "próximas" (ajustar `ProfileViewModel.parseDateMillis`).
4. `AuthInterceptor` usa `runBlocking` para leer el token (limitación conocida: bloquea el hilo de OkHttp, no la UI).
5. La base de datos usa `fallbackToDestructiveMigration`: al subir la versión de esquema (v2 → v3 en este desarrollo) se borran los datos locales; en una instalación final no afecta porque es primera instalación.
6. El aviso de antigüedad usa la última sincronización registrada en DataStore; si el usuario borra los datos de la app, se pierde ese registro (como se espera).
7. Git: se trabajará durante las 4 semanas con etiqueta final **`v1.0`** al cierre (aún no creada).

---

## 8. Criterios de evaluación que se buscan cumplir

- **45 % funcionalidad y cumplimiento** → los cinco módulos obligatorios (RF-01/02/03/06, RF-13, RF-04/05/14, RF-11/12 y RF-07/08/09) implementados y verificados por build.
- **20 % arquitectura y estado** → capas separadas (UI / ViewModel / dominio / datos), repositorios con interfaz, `StateFlow`, Hilt, Room, Retrofit, DataStore.
- **20 % comprensión y modificación individual** → cada ViewModel/Repository tiene una responsabilidad acotada y documentada en este README para la microdefensa (5–10 min).
