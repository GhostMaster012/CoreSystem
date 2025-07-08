# CoreSystem Plugin

## 1. Resumen Ejecutivo e Introducción

**CoreSystem** es un plugin de Spigot/Paper para Minecraft (1.20.4+) que introduce una mecánica central y personal para cada jugador: un **Núcleo**. Este Núcleo es una entidad física, evolutiva y vulnerable que actúa como el centro de poder y progreso del jugador. Su supervivencia, desarrollo y personalización son clave para la modalidad de juego que desees construir a su alrededor, integrándose con sistemas de combate, misiones, construcción y economía.

**Filosofía del Plugin:** *"El corazón de la modalidad. Si este plugin falla o se queda corto, todo se cae."* - Esta filosofía guía el desarrollo hacia la estabilidad, configurabilidad y extensibilidad.

## 2. Instalación

Sigue estos pasos para instalar CoreSystem en tu servidor:

### Dependencias Requeridas:
Asegúrate de tener las siguientes dependencias instaladas en tu servidor, ya que CoreSystem las necesita para funcionar correctamente:

1.  **Vault**: Necesario para todas las funcionalidades económicas (costos de restauración del núcleo, mejoras futuras, etc.).
    *   Puedes descargarlo desde [SpigotMC](https://www.spigotmc.org/resources/vault.34315/) o [BukkitDev](https://dev.bukkit.org/projects/vault).
2.  **PlaceholderAPI (PAPI)**: Requerido para que los placeholders de CoreSystem funcionen en otros plugins (como scoreboards, tab, chat, etc.).
    *   Puedes descargarlo desde [SpigotMC](https://www.spigotmc.org/resources/placeholderapi.6245/).
    *   Después de instalar PAPI, necesitarás que los jugadores (o la consola) ejecuten `/papi ecloud download CoreSystem` y luego `/papi reload` una vez que CoreSystem esté cargado y su expansión PAPI registrada, para activar los placeholders. (Nota: La expansión se registra automáticamente si PAPI está presente).

### Dependencias Opcionales (Integraciones):
CoreSystem puede integrarse con los siguientes plugins para mejorar la experiencia. Son opcionales y el plugin funcionará sin ellos, usando sus sistemas internos como fallback cuando sea aplicable.

1.  **WorldGuard**: Si está presente y habilitado en la configuración de CoreSystem, se usará para la protección de la región del Núcleo.
    *   CoreSystem definirá automáticamente regiones de WorldGuard para proteger los núcleos.
    *   Los flags aplicados son configurables en `config.yml` de CoreSystem.
2.  **Citizens2**: (Integración futura) Podría usarse como fallback para la entidad visual del Núcleo si ModelEngine no está.
3.  **ModelEngine**: (Integración futura) Permitiría usar modelos 3D personalizados y animados para el Núcleo, haciendo su evolución visual mucho más impactante.

### Pasos de Instalación del Plugin:
1.  Descarga la última versión de `CoreSystem.jar`.
2.  Coloca el archivo `CoreSystem.jar` en la carpeta `plugins` de tu servidor Minecraft.
3.  Asegúrate de que todas las dependencias requeridas (Vault, PlaceholderAPI) también estén en tu carpeta `plugins`.
4.  Inicia o reinicia tu servidor.
5.  CoreSystem generará sus archivos de configuración por defecto (`config.yml`, `experience.yml`, `evolution.yml`, `skills.yml`, `mutations.yml`, `messages.yml`) en la carpeta `plugins/CoreSystem/`.
6.  Ajusta las configuraciones a tu gusto y reinicia el servidor o usa `/coreadmin reload` (algunos cambios profundos podrían requerir reinicio del servidor).

## 3. Concepto Central: El Núcleo del Jugador

El "Núcleo" es una manifestación física del poder y la progresión de un jugador en el servidor. Cada jugador tiene derecho a un único Núcleo.

*   **Entidad Física y Evolutiva**: El Núcleo comienza como una entidad simple (actualmente un ArmorStand con un ítem que cambia su CustomModelData) y su apariencia cambia a medida que sube de nivel, reflejando su creciente poder.
*   **Centro de Poder**: El estado de tu Núcleo (nivel, salud, energía, arquetipo, habilidades, mutaciones) dicta muchas de tus capacidades y cómo interactúas con el mundo del juego.
*   **Vulnerable**: Por defecto, el Núcleo es invulnerable. Sin embargo, bajo condiciones específicas configurables (ej. guerras de facciones, eventos especiales del servidor, o si `allow-damage-anytime` está activo en `config.yml`), puede ser dañado por otros jugadores o fuentes de daño definidas.
*   **Supervivencia Clave**: Si tu Núcleo es destruido, pierdes temporalmente sus beneficios y debes pasar por un proceso de restauración.

### 3.1. Ciclo de Vida del Núcleo

*   **Obtención y Colocación**:
    *   Al unirse por primera vez (o si no tienen un núcleo y el tutorial no está completado), los jugadores reciben un tutorial guiado por chat.
    *   Pueden usar `/core claim` para recibir una "Semilla del Núcleo". Este ítem es único y necesario para crear el Núcleo. Este comando tiene un cooldown.
    *   Con la semilla en mano, el jugador usa `/core place` en la ubicación deseada para invocar su Núcleo. Solo se puede tener un Núcleo activo.
*   **Protección**:
    *   Al colocarlo, se crea automáticamente una zona de protección alrededor del Núcleo.
    *   Si WorldGuard está instalado y habilitado en la configuración, CoreSystem creará una región de WorldGuard. Los flags son configurables en `config.yml`.
    *   Si WorldGuard no está, CoreSystem usa su sistema de protección interno básico que previene que otros jugadores modifiquen bloques en un radio configurable (`default-protection-radius` en `config.yml`) alrededor del Núcleo.
*   **Vulnerabilidad y Daño**:
    *   El Núcleo tiene puntos de salud (configurable `default-core-max-health`). Por defecto es invulnerable.
    *   Se vuelve vulnerable según las condiciones definidas en `config.yml` (sección `core-vulnerability`).
    *   Las fuentes de daño permitidas (Jugador, TNT, Creeper, etc., definidas en `damage-sources`) también son configurables.
*   **Destrucción y Restauración**:
    *   Si la salud del Núcleo llega a cero, se considera "destruido".
    *   Se guarda un backup de su estado (nivel, XP, mutaciones, stats de XP, etc.).
    *   El jugador recibe una notificación y se puede emitir un anuncio global (configurable).
    *   Para recuperarlo, el jugador debe usar `/core restore` en una nueva ubicación. Esto tiene un costo configurable (economía de Vault y/o ítems en `config.yml`) y un cooldown. El Núcleo se restaura con los datos del backup.
*   **Renacimiento (Prestigio)**:
    *   Al alcanzar el nivel máximo (`max-core-level` en `config.yml`), los jugadores pueden usar `/core rebirth`.
    *   Esto resetea el nivel del Núcleo a 1 y su XP a 0.
    *   A cambio, el jugador obtiene una "Mutación" permanente (un buff pasivo o una habilidad única) y su contador de renacimientos aumenta.
    *   Las mutaciones se definen en `mutations.yml` y se otorgan secuencialmente por defecto.
    *   El historial de renacimientos y mutaciones se puede ver con `/core history`.

---

## 4. Comandos de Usuario

Todos los comandos de usuario comienzan con `/core`.

*   `/core` o `/core gui` o `/core menu`: Abre el menú principal de información de tu Núcleo.
    *   Muestra: Nivel, Salud, XP, Energía, Arquetipo, Estado.
*   `/core claim`: Reclama una "Semilla del Núcleo" si no tienes un Núcleo activo o una semilla ya.
    *   Tiene un cooldown configurable (`command-cooldowns.core-claim` en `config.yml`).
*   `/core place`: Coloca tu Núcleo en tu ubicación actual usando una Semilla del Núcleo de tu mano.
    *   Solo puedes tener un Núcleo activo.
    *   Se crea una región de protección a su alrededor.
*   `/core restore`: Restaura tu Núcleo si ha sido destruido.
    *   Requiere costos (ítems y/o dinero de Vault) y tiene un cooldown, configurables en `config.yml`.
    *   El Núcleo se restaura con su nivel, XP y mutaciones previas.
*   `/core feed`: Sostén un ítem y usa este comando para alimentar tu Núcleo y darle XP.
    *   La cantidad de XP es fija por ahora (`core-feed.DEFAULT_ITEM_SACRIFICE_XP` en `experience.yml`).
    *   Tiene un cooldown configurable (`command-cooldowns.core-feed` en `config.yml`).
*   `/core energize`: Sostén un ítem configurado (ver `energy-items` en `config.yml`) y usa este comando para restaurar energía a tu Núcleo.
    *   Tiene un cooldown configurable (`command-cooldowns.core-energize` en `config.yml`).
*   `/core archetype <nombre_arquetipo>`: Elige un Arquetipo para tu Núcleo (ej. AGGRESSIVE, DEFENSIVE, MENTAL, MOBILE).
    *   Esta elección es, por lo general, permanente hasta el próximo renacimiento.
    *   Se recomienda elegirlo al Nivel 1 o poco después de colocar el núcleo.
*   `/core skill <id_habilidad>`: Activa una habilidad desbloqueada de tu arquetipo.
    *   Consume energía y tiene un cooldown.
*   `/core rebirth`: Si tu Núcleo ha alcanzado el nivel máximo, usa este comando para renacer.
    *   Resetea el nivel y XP de tu Núcleo.
    *   Otorga una Mutación permanente.
*   `/core history`: Muestra tu contador de renacimientos y las mutaciones que has adquirido.
*   `/core help`: Muestra una lista de estos comandos.

## 5. Progresión del Núcleo

### 5.1. Niveles y Experiencia (XP)
Tu Núcleo gana Experiencia (XP) para subir de nivel. El XP se puede obtener de varias fuentes, configurables en `experience.yml`:
*   **Matar Mobs**: Cada tipo de mob puede dar una cantidad diferente de XP. Hay un valor por defecto para mobs no especificados.
*   **Matar Jugadores**: Otorga una cantidad fija de XP.
*   **Alimentar el Núcleo (`/core feed`)**: Sacrificar ítems otorga XP.

Al acumular suficiente XP, tu Núcleo subirá de nivel automáticamente, hasta el `max-core-level` definido en `config.yml`.
*   **Desbloqueo de Habilidades**: Subir de nivel es el principal requisito para desbloquear nuevas habilidades de tu Arquetipo elegido.
*   **Evolución Visual**: La apariencia de tu Núcleo (el ArmorStand) cambiará en ciertos hitos de nivel, según se define en `evolution.yml` (usando CustomModelData en un ítem).

### 5.2. Energía del Núcleo
La energía es un recurso que tu Núcleo utiliza para activar habilidades.
*   **Máximo de Energía**: Definido por `default-core-max-energy` en `config.yml` (puede ser afectado por mutaciones).
*   **Regeneración Pasiva**: La energía se regenera lentamente con el tiempo si la opción `energy-regeneration.passive-enabled` está activa. La cantidad y el intervalo son configurables. Las mutaciones pueden mejorar esto.
*   **Recarga Activa**: Usa `/core energize` con ítems específicos (definidos en `energy-items` en `config.yml`) para restaurar energía instantáneamente.

### 5.3. Arquetipos y Habilidades
Una vez que colocas tu Núcleo, puedes elegir un Arquetipo usando `/core archetype <nombre>`. El arquetipo define el tipo de habilidades que tu Núcleo podrá aprender y usar.
*   Los arquetipos y sus habilidades (con sus costos de energía, cooldowns, niveles requeridos) se definen en `skills.yml`.
*   Las habilidades se desbloquean automáticamente al alcanzar el nivel requerido, siempre que hayas elegido un arquetipo.
*   Usa `/core skill <id_habilidad>` para activar una habilidad.
*   Efectos de Habilidad Implementados (Ejemplos):
    *   `aggressive_strike_1`: Causa daño directo a un objetivo en la mira.
    *   `defensive_harden_1`: Otorga Resistencia temporal al jugador.
    *   `core_launch_1`: Lanza a un objetivo por los aires.

### 5.4. Renacimiento y Mutaciones
Al llegar al nivel máximo, puedes realizar un "Renacimiento" (`/core rebirth`).
*   Tu nivel y XP se resetean.
*   Ganas una **Mutación** permanente. Las mutaciones son bonus pasivos definidos en `mutations.yml`.
*   Ejemplos de Mutaciones:
    *   Aumento de Salud Máxima del Núcleo.
    *   Aumento de Energía Máxima del Núcleo.
    *   Multiplicador de ganancia de XP.
    *   Mejora en la regeneración pasiva de energía.
*   Puedes ver tu historial de renacimientos y mutaciones con `/core history`.

---

## 6. Configuración para Administradores de Servidor

CoreSystem es altamente configurable. Los archivos de configuración principales se encuentran en la carpeta `plugins/CoreSystem/`:

*   **`config.yml`**: Configuración general del plugin.
    *   `storage`: Tipo de almacenamiento de datos (`YAML` o `MYSQL` - MySQL no implementado completamente aún).
    *   `default-protection-radius`: Radio de protección del sistema interno si WorldGuard no se usa.
    *   `default-core-max-health`, `default-core-max-energy`, `max-core-level`.
    *   `energy-regeneration`: Configuración para la regeneración pasiva de energía.
    *   `energy-items`: Define qué ítems recargan energía y cuánta.
    *   `core-vulnerability`: Define cuándo y cómo el núcleo puede ser dañado.
        *   `vulnerable-by-default`, `allow-damage-anytime`.
        *   `conditions`: (Actualmente solo `placeholder-condition-always-vulnerable` para pruebas).
        *   `damage-sources`: Lista de qué puede dañar el núcleo.
    *   `core-restoration`: Costos (Vault, ítems) y cooldown para `/core restore`.
    *   `core-seed-item`: Apariencia del ítem "Semilla del Núcleo".
    *   `command-cooldowns`: Cooldowns para `/core claim`, `/core feed`, `/core energize`.
    *   `announce-core-destruction`: Si se anuncia globalmente la destrucción de un núcleo.
    *   `action-bar`: Configuraciones para la action bar de energía/cooldowns.
    *   `worldguard`: Opciones para la integración con WorldGuard, incluyendo `enabled` y `default-flags`.
    *   `debug-mode`: Habilita mensajes de debug en consola.
*   **`experience.yml`**: Define cuánta XP se obtiene de diferentes fuentes (matar mobs, jugadores, `/core feed`).
*   **`evolution.yml`**: Define cómo cambia la apariencia del Núcleo (CustomModelData del ítem en el ArmorStand) según su nivel.
*   **`skills.yml`**: Define los Arquetipos y todas sus Habilidades (nombre, descripción, nivel requerido, costo de energía, cooldown, y detalles del efecto como daño, tipo de poción, etc.).
*   **`mutations.yml`**: Define las Mutaciones obtenidas por renacimiento (nombre, descripción, tipo de efecto, detalles del efecto).
*   **`messages.yml`**: Contiene todos los mensajes visibles para el usuario, permitiendo su traducción y personalización.

Se recomienda revisar cada archivo para ajustar el plugin al balance y estilo de juego deseado. El comando `/coreadmin reload` recarga la mayoría de estas configuraciones.

## 7. Permisos

CoreSystem utiliza un sistema de permisos para controlar el acceso a sus comandos y funcionalidades.

### Nodos Principales:
*   `coresystem.user`: Nodo padre que agrupa todos los permisos de jugador por defecto. Se recomienda dar este a los grupos de jugadores (`default: true` o `op` en `plugin.yml`).
*   `coresystem.admin`: Nodo padre que agrupa todos los permisos de administrador. Se recomienda para staff (`default: op` en `plugin.yml`).

### Permisos de Usuario Detallados (hijos de `coresystem.user`):
*   `coresystem.command.core`: Permite usar `/core` para abrir la GUI y ver `/core help`.
*   `coresystem.core.claim`: Permite usar `/core claim`.
*   `coresystem.core.place`: Permite usar `/core place`.
*   `coresystem.core.restore`: Permite usar `/core restore`.
*   `coresystem.core.feed`: Permite usar `/core feed`.
*   `coresystem.core.energize`: Permite usar `/core energize`.
*   `coresystem.core.archetype.choose`: Permite usar `/core archetype <nombre>`.
*   `coresystem.core.skill.use`: Permite usar `/core skill <id_habilidad>`.
*   `coresystem.core.rebirth`: Permite usar `/core rebirth`.
*   `coresystem.core.history`: Permite usar `/core history`.

### Permisos de Administrador Detallados (hijos de `coresystem.admin`):
*   `coresystem.command.coreadmin`: Permite usar el comando base `/coreadmin` (para el help de admin).
*   `coresystem.admin.damage`: Permite usar `/coreadmin damage`.
*   `coresystem.admin.destroy`: Permite usar `/coreadmin destroy`.
*   `coresystem.admin.sethealth`: Permite usar `/coreadmin sethealth`.
*   `coresystem.admin.setlevel`: Permite usar `/coreadmin setlevel`.
*   `coresystem.admin.setxp`: Permite usar `/coreadmin setxp`.
*   `coresystem.admin.setenergy`: Permite usar `/coreadmin setenergy`.
*   `coresystem.admin.setarchetype`: Permite usar `/coreadmin setarchetype`.
*   `coresystem.admin.reload`: Permite usar `/coreadmin reload`.
*   *(Se añadirán más permisos aquí para futuros comandos como `resetplayer`, `givemutation`)*.

## 8. Placeholders (PlaceholderAPI)

Si tienes PlaceholderAPI instalado, CoreSystem registrará los siguientes placeholders que puedes usar en otros plugins:

*   `%coresystem_level%`: Nivel actual del Núcleo.
*   `%coresystem_xp%`: XP actual del jugador dentro de su nivel actual (progreso para el siguiente nivel).
*   `%coresystem_xp_required%`: XP total necesario para completar el nivel actual.
*   `%coresystem_xp_total%`: XP total acumulado por el jugador.
*   `%coresystem_energy%`: Energía actual del Núcleo.
*   `%coresystem_max_energy%`: Energía máxima del Núcleo.
*   `%coresystem_health%`: Salud actual del Núcleo.
*   `%coresystem_max_health%`: Salud máxima del Núcleo.
*   `%coresystem_health_bar%`: Una barra de salud simple (ej. ███░░░░░░░).
*   `%coresystem_type%`: ID del Arquetipo del Núcleo (ej. AGGRESSIVE) o "None".
*   `%coresystem_archetype_displayname%`: Nombre formateado del Arquetipo del Núcleo.
*   `%coresystem_status%`: Estado actual del Núcleo (ej. Active, Destroyed, Inactive).
*   `%coresystem_rebirth_count%`: Número de renacimientos completados.
*   *(Se podrían añadir más placeholders en el futuro, como el cooldown de habilidades específicas si es necesario)*.

---

## 9. Documentación para Desarrolladores

CoreSystem ofrece una API para permitir que otros plugins interactúen con sus funcionalidades principales.

### 9.1. Acceso a la API

Para acceder a la API de CoreSystem desde tu plugin, primero asegúrate de que CoreSystem esté listado como una dependencia (o soft-dependencia) en tu `plugin.yml`. Luego, puedes obtener la instancia de la API a través del `ServicesManager` de Bukkit:

```java
import com.example.coresystem.api.CoreSystemAPI;
import org.bukkit.plugin.RegisteredServiceProvider;

// ... en tu método onEnable() o donde necesites la API
CoreSystemAPI coreApi = null;
RegisteredServiceProvider<CoreSystemAPI> rsp = getServer().getServicesManager().getRegistration(CoreSystemAPI.class);
if (rsp != null) {
    coreApi = rsp.getProvider();
}

if (coreApi != null) {
    // ¡Ahora puedes usar coreApi!
    // Ejemplo: PlayerData data = coreApi.getCoreData(player);
} else {
    getLogger().warning("CoreSystemAPI not found. Funcionalidad dependiente será desactivada.");
}
```

### 9.2. Métodos de la API (`CoreSystemAPI`)

La interfaz `CoreSystemAPI` (en `com.example.coresystem.api.CoreSystemAPI`) expone los siguientes métodos:

*   `PlayerData getCoreData(@NotNull Player player)`
*   `PlayerData getCoreData(@NotNull UUID playerUUID)`
    *   Devuelve el objeto `PlayerData` del jugador, que contiene toda la información sobre su núcleo (nivel, XP, energía, salud, arquetipo, habilidades desbloqueadas, mutaciones, etc.).
    *   **Nota Importante**: Si bien obtienes el objeto `PlayerData` real, se recomienda encarecidamente **no modificarlo directamente**. Para cambios, utiliza los métodos específicos de la API (como `addCoreXP`, `setCoreEnergy`) para asegurar que los eventos se disparen y los datos se manejen correctamente.

*   `boolean addCoreXP(@NotNull Player player, double amount, @NotNull String reason)`
*   `boolean addCoreXP(@NotNull UUID playerUUID, double amount, @NotNull String reason)`
    *   Añade la cantidad especificada de XP al núcleo del jugador.
    *   `reason` es una cadena descriptiva (ej. "QUEST_REWARD", "CUSTOM_EVENT").
    *   Dispara un `CoreGainXPEvent` (cancelable).
    *   Devuelve `true` si el XP fue procesado (evento no cancelado, jugador online y con núcleo activo para la versión `Player`), `false` en caso contrario.

*   `boolean setCoreEnergy(@NotNull Player player, double amount)`
*   `boolean setCoreEnergy(@NotNull UUID playerUUID, double amount)`
    *   Establece la energía del núcleo del jugador. Se ajustará automáticamente entre 0 y la energía máxima del jugador.
    *   Devuelve `true` si la energía se estableció (jugador tiene datos y, para la versión `Player`, un núcleo activo), `false` en otro caso.

*   `double getCoreEnergy(@NotNull Player player)`
*   `double getCoreEnergy(@NotNull UUID playerUUID)`
    *   Devuelve la energía actual del núcleo del jugador.

*   `double getMaxCoreEnergy(@NotNull Player player)`
*   `double getMaxCoreEnergy(@NotNull UUID playerUUID)`
    *   Devuelve la capacidad máxima de energía del núcleo del jugador.

*   `int getCoreLevel(@NotNull Player player)`
*   `int getCoreLevel(@NotNull UUID playerUUID)`
    *   Devuelve el nivel actual del núcleo.

*   `boolean hasRebirthed(@NotNull Player player)`
*   `boolean hasRebirthed(@NotNull UUID playerUUID)`
    *   Devuelve `true` si el jugador ha realizado al menos un renacimiento (`getRebirthCount() > 0`).

*   `int getRebirthCount(@NotNull Player player)`
*   `int getRebirthCount(@NotNull UUID playerUUID)`
    *   Devuelve el número de veces que el jugador ha renacido su núcleo.

*   `boolean isCoreActive(@NotNull Player player)`
*   `boolean isCoreActive(@NotNull UUID playerUUID)`
    *   Devuelve `true` si el núcleo del jugador está actualmente colocado y activo en el mundo.

*   `Location getCoreLocation(@NotNull Player player)`
*   `Location getCoreLocation(@NotNull UUID playerUUID)`
    *   Devuelve la `Location` del núcleo activo del jugador, o `null` si no tiene uno activo.

*Consulta el Javadoc de la interfaz `CoreSystemAPI.java` para más detalles sobre cada método.*

### 9.3. Eventos Personalizados

CoreSystem dispara varios eventos personalizados que puedes escuchar en tu plugin para reaccionar a acciones importantes relacionadas con los núcleos:

*   **`CorePlaceEvent(Player player, Location location, PlayerData playerData)`**:
    *   Se dispara cuando un jugador coloca su núcleo.
    *   No es cancelable.
*   **`CoreDestroyEvent(Player player, PlayerData playerData, Location oldLocation)`**:
    *   Se dispara cuando un núcleo es destruido (salud llega a 0).
    *   No es cancelable. Contiene la `PlayerData` *antes* del reseteo por destrucción (el backup ya se ha creado en este punto).
*   **`CoreLevelUpEvent(Player player, int oldLevel, int newLevel)`**:
    *   Se dispara cuando un núcleo sube de nivel.
    *   Provee el nivel antiguo y el nuevo.
    *   No es cancelable.
*   **`CoreRebirthEvent(Player player, int previousRebirthCount, int newRebirthCount, List<Mutation> mutationsAwarded)`**:
    *   Se dispara después de que un jugador completa un renacimiento.
    *   Provee los contadores de renacimiento y la(s) mutación(es) otorgada(s).
    *   No es cancelable.
*   **`CoreSkillUseEvent(Player player, Skill skill)`**:
    *   Se dispara justo antes de que el efecto de una habilidad sea aplicado.
    *   Es **cancelable**. Si se cancela, la energía no se consume y el cooldown no se aplica (o se revierten si la cancelación ocurre después de su aplicación inicial en el `SkillManager`). El `SkillManager` actual reembolsa la energía si se cancela.
*   **`CoreGainXPEvent(Player player, double amount, String reason)`**:
    *   Se dispara cuando un jugador está a punto de ganar XP para su núcleo.
    *   Es **cancelable**. También puedes modificar la cantidad de XP usando `setAmount()`.
    *   `reason` indica la fuente del XP.

**Ejemplo de escucha de evento:**
```java
import com.example.coresystem.events.CoreLevelUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {
    @EventHandler
    public void onCoreLevel(CoreLevelUpEvent event) {
        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();
        player.sendMessage("¡Felicidades por alcanzar el nivel de Núcleo " + newLevel + "!");
    }
}
```

### 9.4. Estructura del Proyecto (Breve)

El plugin se organiza en varios paquetes y managers principales:
*   `com.example.coresystem`: Clase principal del plugin.
*   `api`: Contiene `CoreSystemAPI` y su implementación.
*   `commands`: Manejadores de comandos (`/core`, `/coreadmin`).
*   `entity`: `CoreEntityManager` para la entidad visual del núcleo.
*   `gui`: Clases relacionadas con la interfaz gráfica de usuario.
*   `integration`: Clases para integraciones con otros plugins (ej. `WorldGuardIntegration`).
*   `listeners`: Todos los listeners de eventos de Bukkit y personalizados.
*   `mutation`: Clases `Mutation` y `MutationManager`.
*   `protection`: Sistema de protección interno (`ProtectedRegion`, `RegionManager` si WG no está).
*   `skill`: Clases `Archetype`, `Skill` y `SkillManager`.
*   `tutorial`: `TutorialManager`.
*   `display`: `ActionBarManager`.
*   `utils`: Clases de utilidad como `MessageManager`, `ExperienceManager`.

Esta documentación debería proporcionar una buena base para usuarios y desarrolladores.
Se añadirán más detalles y Javadoc a medida que el plugin evolucione.
