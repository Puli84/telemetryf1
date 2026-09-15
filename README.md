# Crew chief casero — F1 25 / PS5

## Qué es

Aplicación Java que escucha la telemetría UDP de F1 25 en PS5 (formato 2025,
puerto 20777) y actúa como ingeniero de carrera: avisa por voz de gasolina,
banderas, gomas, ERS, DRS, sectores, tiempos de vuelta, sanciones, daños,
coche de seguridad, entradas a boxes de cualquier piloto y vueltas rápidas
ajenas — y responde por voz a preguntas puntuales (radio bidireccional).

Proyecto Maven / Spring Boot. Todo el código vive en el paquete `crewchief`,
bajo la estructura estándar `src/main/java`. Repositorio en GitHub
(`Puli84/telemetryf1`); si se alterna entre dos PCs, sincronizar con
`git push`/`git pull` normal — y que ambos PCs tengan JDK 17 (ver más abajo).

## Cómo ejecutar

```
mvn spring-boot:run
```

Esto arranca **una sola cosa**: la app Spring Boot, que a su vez lanza el
`Listener` (socket UDP + ventana de la tira LED) en un hilo de fondo
(`ListenerRunner.java`) y sirve la web en `http://localhost:8080`. No hace
falta arrancar el listener y la web por separado.

Si no hay `mvn` en el PATH, IntelliJ trae uno propio en
`plugins/maven/lib/maven3/bin/mvn.cmd` dentro de su carpeta de instalación —
sirve igual sin instalar nada aparte.

## Qué hay montado

**CrewChiefApplication.java** — punto de entrada Spring Boot (`@SpringBootApplication`).

**ListenerRunner.java** — `CommandLineRunner` que arranca `Listener.main()` en
un hilo de fondo al levantar la app, para no bloquear el servidor web. El
`Listener` en sí no sabe que vive dentro de Spring; sigue siendo el mismo
`main()` de siempre.

**SesionesController.java** — primer controlador web: lista los JSON de
`sesiones/` (vía Jackson) y también los récords de `HistorialCircuitos`
(vía su método `todos()`), y los muestra en `templates/sesiones.html`. Es
el arranque del dashboard del paso 4, no las pantallas del mockup todavía.

**Listener.java** — abre el socket UDP, lee la cabecera, reparte por `packetId`
en un `switch` y llama a un método por paquete (`leerSesion`, `leerVuelta`,
`leerTelemetria`, `leerEstado`, `leerEvento`, `leerParticipantes`,
`leerClasificacionFinal`, `leerDanos`, `leerTyreSets`, `leerSetup`). Cada método rellena el
`Estado`, evalúa las reglas correspondientes y pasa el resultado al `Locutor`.
También detecta cambio de `m_sessionUID` para reiniciar todas las reglas al
empezar sesión nueva, y escanea el array completo de 22 coches (no solo el
propio) para calcular huecos con rivales, detectar quién entra a boxes y
cachear nombres/compuestos de toda la parrilla.

**Estado.java** — foto de la carrera en el instante actual. Solo campos
públicos, sin lógica. Es lo que leen todas las reglas.

**Locutor.java** — decide si una frase se dice o se calla. Guarda en un
`HashMap<String, Long>` cuándo se dijo cada clave por última vez y aplica el
cooldown. Tiene un hilo demonio con una `PriorityBlockingQueue` (dos niveles:
`PRIORIDAD_NORMAL`/`PRIORIDAD_ALTA`) que reproduce las frases de una en una
(con `waitFor`) para que no se solapen; el DRS usa prioridad alta para
colarse delante de avisos normales en espera. La voz es el TTS de Windows
(Helena) llamado por PowerShell. Reescribe "boxes" a "bokses" antes de
hablar porque la voz en español lo pronuncia mal en crudo.

**Oyente.java** — radio bidireccional. Al pulsar el botón de push-to-talk,
lanza PowerShell con `System.Speech.Recognition` (gramática cerrada, no
dictado libre) y devuelve la palabra reconocida. Carga dos gramáticas a la
vez: la lista fija de comandos, y una dinámica con los apellidos de los
pilotos presentes (sacados de `nombres[]`) seguidos de la palabra "boxes",
para poder preguntar "Verstappen boxes" y saber si/cuándo entró. Necesita el
reconocimiento de voz en español activado en Windows (Configuración → Hora e
idioma → Voz) — es un componente distinto a la síntesis. Suena un pitido
justo cuando empieza a escuchar, para no depender de mirar pantalla.

**HistorialCircuitos.java** / **Circuitos.java** — guardan en
`historial_vueltas.properties` la mejor vuelta de carrera de siempre por
circuito (tiempo + fecha), persistente entre sesiones y reinicios del
programa. Solo cuenta vueltas válidas (`ultimaVueltaInvalida == false`) y
solo en sesión de carrera. Se consulta por voz con "vuelta historica".
`Circuitos.java` traduce `trackId` a nombre de circuito.

**RegistradorSesion.java** — guarda un JSON por sesión (no solo el mejor
tiempo) en la carpeta `sesiones/`, con clima, temperaturas, setup y el
historial completo de vueltas (tiempo, tres sectores, validez, compuesto y
edad de goma en el momento de cada una). Se **reescribe tras cada vuelta**,
no solo al final — si el juego se cierra de golpe o se pierde la conexión
sin que llegue nunca un evento de fin de sesión, no se pierde lo ya
corrido. También se guarda (redundante pero inofensivo) al terminar la
sesión de forma normal (evento `SEND` o paquete 8, Clasificación Final).
El nombre del fichero se fija en la primera vuelta y no cambia aunque se
reescriba muchas veces. Sin Maven puro/Jackson explícito todavía en el
`pom.xml` (el `ObjectMapper` que usa `SesionesController` para *leer* estos
JSON llega transitivo con `spring-boot-starter-web`), así que este JSON se
sigue serializando a mano en `construirJson()` — el formato sigue
exactamente el esquema del roadmap más abajo. Es la base de datos que le
falta a la web (paso 4).

**Emisor.java** — simulador. Construye paquetes byte a byte con los offsets
reales y los manda a 127.0.0.1:20777. Simula una carrera de 20 vueltas pasando
por los tres sectores, con gasolina que baja, amarilla en la vuelta 12, gomas
que se calientan desde la 15 y zona de DRS en cada vuelta. Permite desarrollar
sin la consola encendida. **Desactualizado**: solo emite los paquetes 2, 6 y
7 con los campos de las primeras reglas; no cubre daños, ERS, participantes,
Time Trial ni el resto de paquetes que se fueron añadiendo después.

**Tira.java** — control de la tira LED (proyecto paralelo, misma telemetría).

**PruebaOyente.java** / **Prueba.java** — programas main sueltos para probar
el reconocimiento de voz y una regla concreta sin arrancar el listener
completo ni el juego.

### Reglas

Todas siguen el mismo contrato: `String evaluar(Estado e)`, devuelven la frase
o `null` si no hay nada que decir. `Frases.java` centraliza el `elegir(...)`
con varargs que usan casi todas para no repetir siempre la misma frase.

| Clase | Qué avisa | Tipo | Cooldown |
|---|---|---|---|
| `ReglaCombustible` | Margen de gasolina (vueltas de superávit) por debajo de 0.5 | condición continua | 80000 |
| `ReglaBanderas` | Verde, azul, amarilla | cambio de valor | 0 |
| `ReglaDrs` | "Prepara DRS" en cuanto aparece la cuenta atrás (una vez) | fase con booleano | 0 (prioridad alta) |
| `ReglaSectores` | S1, S2 y S3 contra el mejor de la sesión de hoy; ignora vueltas invalidadas | cambio de valor | 0 |
| `ReglaVuelta` | Tiempo al cruzar meta contra el mejor de la sesión; dice "invalidada" si toca | cambio de valor | 0 |
| `ReglaSanciones` | Penalizaciones y avisos de límites nuevos | cambio de valor | 0 |
| `ReglaErs` | Carga de ERS a 100/80/50/20%, con histéresis para no disparar por ruido ni al arrancar ya cargado | umbrales armados | 0 |
| `ReglaUndercut` | Hueco corto + gomas gastadas → sugiere valorar undercut | una vez por stint | 0 |
| `ReglaDesgaste` | Desgaste de goma ≥ 15% (rendimiento ya se nota) | una vez por stint | 0 |
| `ReglaDanos` | Alerón trasero o laterales dañados ≥ 40% → sugiere boxes | una vez hasta que baje del umbral | 0 |
| `ReglaGomas` | Temperatura fuera de ventana | condición continua | — (desconectada, ver abajo) |

`ReglaGomas` (temperatura) sigue en el repo pero **no está enchufada en
`Listener`** — molestaba más de lo que ayudaba. El resto de avisos puntuales
(coche de seguridad, retirada, descalificación, vuelta rápida ajena,
clasificación final, entradas a boxes) están directamente en `Listener.java`
como métodos `mensajeX`/manejadores de evento, no como clase `Regla*`, porque
reaccionan a eventos del paquete 3 en vez de a un estado continuo.

Tres patrones que se repiten:
- **Condición continua** → cooldown en el Locutor.
- **Cambio de valor** → la regla guarda el valor anterior y solo habla en la
  transición. Cooldown 0.
- **Umbral armado** → un booleano por umbral que se rearma solo cuando el
  valor vuelve a cruzar hacia el otro lado con margen (histéresis), para que
  el ruido de la telemetría no dispare avisos falsos justo en el límite.

### Radio: botón y comandos de voz

R3 asignado a UDP Action 1 en la configuración del mando. Llega en el evento
BUTN con la máscara `0x00100000` (puede venir mezclado con otros bits sueltos
del stick derecho; el `AND` de la máscara lo ignora sin problema). Usar ese
bit, no el del botón físico, para que la asignación se pueda cambiar sin
tocar código.

Al pulsar, escucha 5 segundos y reconoce: `gasolina`, `posicion`, `gomas`,
`lider`, `delante`, `detras` (estos dos incluyen compuesto y edad de goma del
rival), `vuelta`, `estado` (resumen completo), `vuelta historica` (mejor
marca histórica en este circuito) y `<apellido> boxes` (si ese piloto entró
y en qué vuelta). Si no reconoce nada, se calla — no suelta el resumen por
defecto.

## Detalles técnicos que ya costaron una vez

- `m_packetId` está en el **byte 6**, no en el 5. F1 25 añadió `m_gameYear`
  a la cabecera y desplazó todo. Las guías de F1 23/24 dicen 5.
- `ByteOrder.LITTLE_ENDIAN` obligatorio en todos los `ByteBuffer`.
- Java no tiene tipos sin signo: `& 0xFF` para uint8, `& 0xFFFF` para uint16.
  Sin eso una temperatura de 150 se lee como -106. **No** aplicar la máscara a
  int8/int16 (`m_vehicleFiaFlags` puede valer -1 legítimamente).
- Fórmula de offset: `base = 29 + indiceCoche * tamañoBloque`. El índice del
  coche propio viene en el byte 27 de la cabecera. El tamaño de bloque varía
  por paquete: Lap Data 57, Car Status 55, Car Telemetry 60, Car Damage 46,
  Participants 57 (con `m_numActiveCars` de 1 byte antes del array), Final
  Classification 46 (con `m_numCars` antes del array), Car Setups 50.
- Sectores y deltas vienen partidos en minutos + milisegundos:
  `ms = minutos * 60000 + milisegundos`. El Sector 3 **no** viene en el
  paquete: se calcula `ultimaVueltaMs - sector1Ms - sector2Ms` en el instante
  exacto en que se detecta el cambio de vuelta (antes de que esos campos se
  sobrescriban con los de la vuelta nueva).
- `m_currentLapInvalid` (Lap Data, +37) hay que leerlo cada paquete y guardar
  una foto fija (`ultimaVueltaInvalida`) justo antes de que cambie
  `m_currentLapNum`, porque el campo siempre habla de la vuelta "actual" — en
  cuanto empieza la vuelta nueva se pierde si no se captura ese instante.
  Sin esto, una vuelta invalidada por salirse de pista se cuela como mejor
  marca.
- `m_totalLaps` vale 0 en libres y clasificación. Las reglas que dependen de
  vueltas restantes necesitan una guarda `if (vueltasTotales <= 0) return null`.
- El socket **no** debe atarse a una IP fija salvo que haga falta. En el PC con
  VirtualBox hubo que usar `new DatagramSocket(20777, InetAddress.getByName(...))`
  porque Java escuchaba en el adaptador equivocado.
- `.idea/` y `*.iml` fuera del repositorio, o los dos PCs se pelean en cada pull.
- **JDK**: usar solo API compatible con Java 17 (`Locale.forLanguageTag`, no
  `Locale.of`, que es de Java 19+) — los dos PCs no tienen la misma versión
  de JDK y el código tiene que compilar en ambos.
- **Multijugador**: si el rival no tiene activado "Mostrar ID online /
  gamertags", el juego manda un nombre genérico en el paquete de
  participantes en vez de su gamertag real. No es un bug de la app: cada
  jugador controla su propia visibilidad, así que en una misma carrera unos
  pilotos pueden salir con nombre real y otros con el genérico.
- `ReglaSectores` tiene un `System.out.println("DEBUG ...")` activo en
  `comparar()` para diagnosticar discrepancias entre lo que anuncia la app y
  lo que muestra el juego. Quitarlo cuando ya no haga falta.
- `sesiones/*.json` y `historial_vueltas.properties` son datos generados por
  cada jugador, no código — están fuera del repo (`.gitignore`). Al cambiar
  de PC no se sincronizan solos; si hace falta compararlos entre máquinas,
  copiarlos a mano.

## Documentación

`Telemetria_UDP_F1_25_ES.pdf` — guía en español con los offsets ya calculados
de los paquetes 1, 2, 3, 6, 7 y 10, más las tablas de valores. Desde entonces
se han añadido a mano los offsets de los paquetes 4 (Participants), 5 (Car
Setups), 8 (Final Classification), 12 (Tyre Sets) y 14 (Time Trial),
documentados como comentarios en el propio `Listener.java`.

La especificación oficial de EA está en el foro (buscar "F1 25 UDP
Specification"). Es la fuente autorizada ante cualquier discrepancia.

---

# Hoja de ruta

## 1. Ajustar umbrales con sesiones reales

En marcha. Ya calibrados con conducción real: desgaste de gomas (15%, antes
85%), margen de gasolina (superávit real en vez de comparar contra vueltas
totales), coche de seguridad + consejo de boxes usando `m_usableLife` real
del compuesto en vez de un número inventado. Pendientes de rodar más:
umbral de daños (40%, sin contrastar todavía), hueco de undercut (3.5s) y
vueltas mínimas de goma para plantearlo (8).

- El aviso de gomas frías/calientes (`ReglaGomas`) se quitó del `Listener`:
  interfería y no decía nada útil. El archivo sigue en el repo por si se
  recupera en otra forma.
- **Hecho:** el mejor tiempo se guarda por circuito (`HistorialCircuitos`,
  usando `m_trackId` del paquete 1, offset 36), no de forma global.

## 2. Maven

**Hecho.** Se saltó directamente a Spring Boot (paso 4) en vez de montar un
Maven suelto primero — Spring Boot ya trae el `pom.xml` con todo, así que
hacerlo en dos pasos era trabajo doble. `spring-boot-starter-parent` 3.3.4,
Java 17, `spring-boot-starter-web` + `spring-boot-starter-thymeleaf`
(Jackson llega transitivo con el primero). IntelliJ abre el proyecto Maven
sin más: no hizo falta dejarlo ni instalar nada, trae su propio Maven
embebido en `plugins/maven/lib/maven3/`.

## 3. Persistencia en JSON

**Hecho, con matices** (`RegistradorSesion.java`): un fichero por sesión en
`sesiones/`, con el esquema de abajo. Se reescribe tras cada vuelta (no solo
al terminar la sesión), para no perder nada si el juego se cierra de golpe.
Ya hay Maven (paso 2, hecho antes de lo previsto), pero el JSON de esta
clase se sigue serializando a mano en `construirJson()` en vez de con
Jackson — `SesionesController` sí usa `ObjectMapper` para *leer* estos
mismos ficheros en la web, así que Jackson ya está en el `pom.xml`;
cambiar `construirJson()` por un objeto + `ObjectMapper` para *escribir*
es ahora un cambio trivial y aislado a esta clase, cuando haga falta.
`HistorialCircuitos` sigue aparte (mejor vuelta por circuito, en
`Properties`, no en JSON) pero **ya está conectado a la web**
(`SesionesController` lee ambas fuentes y las muestra en la misma página);
unificarlas en un solo formato de persistencia sigue pendiente, pero ya no
es necesario para que la web muestre todo lo que hay.

Sin resolver todavía:
- **Setup por vuelta, no por sesión.** El JSON guarda un único `setup` (el
  que esté puesto en el momento de guardar), tal como decía este mismo
  apartado. Si el reparto de frenada o el diferencial cambian en carrera, se
  pierde ese detalle. Falta comprobar con datos reales si `m_brakeBias`
  cambia; si cambia, mover `setup` a dentro de cada vuelta.
- **Regla de los dos compuestos obligatorios** y cualquier otro cruce entre
  este JSON y las reglas en vivo: de momento son mundos separados.

Estructura por sesión (implementada tal cual):

```json
{
  "circuito": 11,
  "nombreCircuito": "Monza",
  "fecha": "2026-09-13",
  "tipoSesion": 15,
  "tempPista": 42,
  "tempAire": 27,
  "clima": 0,
  "setup": { "aleronDelantero": 3, "aleronTrasero": 2, "difEnAcelerador": 65,
             "repartoFreno": 58, "presionFreno": 95 },
  "vueltas": [
    { "numero": 1, "tiempoMs": 85210, "sector1Ms": 28100, "sector2Ms": 31050,
      "sector3Ms": 26060, "valida": true, "compuesto": 17, "vueltasGoma": 1 }
  ]
}
```

Notas (ya resueltas en la implementación, quedan documentadas por si acaso):
- Claves sin acentos.
- El setup viene en el paquete 5 (bloque de 50 bytes), dos veces por segundo.
- El sector 3 no viene en el paquete: se calcula restando S1 y S2 al tiempo
  total de vuelta, igual que en `ReglaSectores`.
- Clima y temperaturas se guardan (paquete 1) precisamente para no perder de
  vista, dentro de unos meses, si un tiempo malo fue del piloto o de la pista.

## 4. Web con Spring Boot

**Arrancado.** Monolítico: Spring Boot + Thymeleaf, HTML servido desde Java,
todo en el mismo proceso que el `Listener` (ver `ListenerRunner.java`). Sin
frontend separado, sin Node. Gráficas con una librería JS por CDN dentro de
la plantilla, cuando toque.

Lo que hay ahora (`SesionesController.java` + `templates/sesiones.html`) es
solo una lista de sesiones guardadas — confirma que la web lee datos reales
de `sesiones/`, nada más. Falta todo lo de abajo, y el mockup en
`design/crew-chief-mockup.html` (y su versión `.dc.html` para el skill de
diseño) ya tiene resuelto el diseño visual de las tres pantallas; falta
conectarlo a datos reales en vez de a los de ejemplo.

**Pantalla por circuito**
- Mejor vuelta histórica con sus tres sectores y el setup con que se hizo.
- Vuelta teórica: suma de los tres mejores sectores históricos, aunque sean de
  sesiones distintas. La diferencia con la mejor real es el tiempo disponible
  sin tocar nada.
- Marcar en qué vuelta y sesión se logró cada mejor sector.
- Enlace desde cada récord a la sesión donde se hizo.

**Pantalla por sesión**
- Todas las vueltas en orden cronológico, no solo las mejores. Un ranking dice
  quién ganó; una cronología dice qué pasó: dónde apareció el ritmo, cuándo
  cayeron las gomas, cómo se degradó el stint largo.
- Sectores por separado, no solo el total.
- Condiciones y setup de la sesión.

**Pantalla de configuración**
- Umbrales editables sin recompilar (margen de gasolina, ventana de gomas,
  cooldowns, umbral de daños, hueco de undercut).
- Silenciar categorías de aviso.

**Sobre las imágenes de circuitos:** los trazados de F1 tienen derechos. Si el
repo se hace público, generar un SVG a partir de las coordenadas del paquete 0
en vez de usar imágenes descargadas.

## 5. Spring AI (último)

Solo con veinte sesiones acumuladas; con tres no hay patrón que encontrar.

**Análisis en frío, nunca en carrera.** La latencia descarta la IA para avisos
en pista, y las reglas ya lo hacen mejor.

- Análisis de sesión: pasar el JSON completo y pedir interpretación. Dónde se
  degradaron las gomas, si el ritmo cayó por goma o por combustible, qué sector
  es el punto débil frente a la vuelta teórica.
- Comparación de setups: dos sesiones del mismo circuito con reglajes
  distintos, qué cambió y dónde se notó.

Procesar en lote y **guardar el análisis dentro del propio JSON**, para que la
web lo muestre sin llamar a la API en cada visita.

## Ideas aparcadas

- **Spotter.** Avisar de coches al costado usando las coordenadas del paquete 0
  (Motion) y el yaw propio. Es lo más útil que falta y lo más complejo:
  trigonometría para saber si el rival está a la izquierda, derecha o detrás.
  Encaja especialmente bien con la tira LED, que ya está en los laterales del
  televisor: aviso visual en la posición física correcta, sin voz.
- **Voces grabadas.** Generar las frases fijas con un TTS bueno y reproducir
  los wav en vez de sintetizar. Cero latencia y mejor calidad. Los números
  seguirían en TTS de Windows. Sistema mixto.
- **Undercut con datos del rival.** La versión actual solo mira las gomas
  propias y el hueco; una versión más fina compararía también el desgaste del
  rival de delante y el tiempo real de pit lane por circuito (no viene en el
  UDP, habría que tabularlo a mano).
- **Regla de los dos compuestos obligatorios.** El consejo de boxes bajo
  coche de seguridad no comprueba si ya se cumplió la norma de usar dos
  compuestos distintos en carrera; habría que llevar la cuenta de qué
  compuestos se han usado.

## Hecho (estaba aquí como idea aparcada)

- **Preguntar por voz.** Resuelto con `Oyente.java`: gramática cerrada,
  soporta comandos fijos y consulta de pilotos por apellido ("Verstappen
  boxes").
- **Rivales humanos en online.** `m_aiControlled` distingue humano de IA, y
  `m_name` (Participants) ya se usa para nombrar a los rivales en avisos de
  boxes, huecos y clasificación final. Pendiente: el nombre puede salir
  genérico si el rival no tiene activado "Mostrar ID online".
