# Crew chief casero — F1 25 / PS5

## Qué es

Aplicación Java que escucha la telemetría UDP de F1 25 en PS5 (formato 2025,
puerto 20777) y actúa como ingeniero de carrera: avisa por voz de gasolina,
banderas, gomas, DRS, sectores, tiempos de vuelta y sanciones.

Proyecto Java sin dependencias externas por ahora. Sin Maven todavía.

## Qué hay montado

**Listener.java** — abre el socket UDP, lee la cabecera, reparte por `packetId`
en un `switch` y llama a un método por paquete (`leerSesion`, `leerVuelta`,
`leerTelemetria`, `leerEstado`, `leerEvento`). Cada método rellena el `Estado`,
evalúa las reglas correspondientes y pasa el resultado al `Locutor`.

**Estado.java** — foto de la carrera en el instante actual. Solo campos
públicos, sin lógica. Es lo que leen todas las reglas.

**Locutor.java** — decide si una frase se dice o se calla. Guarda en un
`HashMap<String, Long>` cuándo se dijo cada clave por última vez y aplica el
cooldown. Tiene un hilo demonio con una `BlockingQueue` que reproduce las
frases de una en una (con `waitFor`) para que no se solapen. La voz es el TTS
de Windows (Helena) llamado por PowerShell.

**Emisor.java** — simulador. Construye paquetes byte a byte con los offsets
reales y los manda a 127.0.0.1:20777. Simula una carrera de 20 vueltas pasando
por los tres sectores, con gasolina que baja, amarilla en la vuelta 12, gomas
que se calientan desde la 15 y zona de DRS en cada vuelta. Permite desarrollar
sin la consola encendida.

**Tira.java** — control de la tira LED (proyecto paralelo, misma telemetría).

### Reglas

Todas siguen el mismo contrato: `String evaluar(Estado e)`, devuelven la frase
o `null` si no hay nada que decir.

| Clase | Qué avisa | Tipo | Cooldown |
|---|---|---|---|
| `ReglaCombustible` | Vueltas de gasolina < vueltas restantes | condición continua | 60000 |
| `ReglaBanderas` | Verde, azul, amarilla | cambio de valor | 0 |
| `ReglaGomas` | Temperatura fuera de ventana | condición continua | 20000 |
| `ReglaDrs` | Cuenta atrás y disponible | fases con booleanos | 0 |
| `ReglaSectores` | S1 y S2 contra el mejor de la sesión | cambio de valor | 0 |
| `ReglaVuelta` | Tiempo al cruzar meta contra el mejor | cambio de valor | 0 |
| `ReglaSanciones` | Penalizaciones y avisos nuevos | cambio de valor | 0 |

Tres patrones que se repiten:
- **Condición continua** → cooldown en el Locutor.
- **Cambio de valor** → la regla guarda el valor anterior y solo habla en la
  transición. Cooldown 0.
- **Fases** → booleanos que se resetean al salir de cada fase (caso DRS).

### Botón del volante

R3 asignado a UDP Action 1 en la configuración del mando. Llega en el evento
BUTN con la máscara `0x00100000`. Usar ese bit, no el del botón físico, para
que la asignación se pueda cambiar sin tocar código.

## Detalles técnicos que ya costaron una vez

- `m_packetId` está en el **byte 6**, no en el 5. F1 25 añadió `m_gameYear`
  a la cabecera y desplazó todo. Las guías de F1 23/24 dicen 5.
- `ByteOrder.LITTLE_ENDIAN` obligatorio en todos los `ByteBuffer`.
- Java no tiene tipos sin signo: `& 0xFF` para uint8, `& 0xFFFF` para uint16.
  Sin eso una temperatura de 150 se lee como -106. **No** aplicar la máscara a
  int8/int16 (`m_vehicleFiaFlags` puede valer -1 legítimamente).
- Fórmula de offset: `base = 29 + indiceCoche * tamañoBloque`. El índice del
  coche propio viene en el byte 27 de la cabecera.
- Sectores y deltas vienen partidos en minutos + milisegundos:
  `ms = minutos * 60000 + milisegundos`.
- `m_totalLaps` vale 0 en libres y clasificación. Las reglas que dependen de
  vueltas restantes necesitan una guarda `if (vueltasTotales <= 0) return null`.
- El socket **no** debe atarse a una IP fija salvo que haga falta. En el PC con
  VirtualBox hubo que usar `new DatagramSocket(20777, InetAddress.getByName(...))`
  porque Java escuchaba en el adaptador equivocado.
- `.idea/` y `*.iml` fuera del repositorio, o los dos PCs se pelean en cada pull.

## Documentación

`Telemetria_UDP_F1_25_ES.pdf` — guía en español con los offsets ya calculados
de los paquetes 1, 2, 3, 6, 7 y 10, más las tablas de valores.

La especificación oficial de EA está en el foro (buscar "F1 25 UDP
Specification"). Es la fuente autorizada ante cualquier discrepancia.

---

# Hoja de ruta

## 1. Ajustar umbrales con sesiones reales

Antes de añadir nada, correr tres o cuatro grandes premios y calibrar. Los
valores actuales son estimaciones.

- Margen de la regla de combustible (ahora 0.5, probablemente corto).
- Máximo de temperatura de gomas (ahora 110).
- El aviso de gomas frías se quitó: saltaba cada vez que se levantaba el pie.
  Si se recupera, limitarlo a las dos primeras vueltas de cada juego usando
  `m_tyresAgeLaps`.
- Anotar qué avisos molestan o llegan tarde. Es la información que no se puede
  obtener sin conducir.

**Comprobar también:** si el mejor tiempo se guarda por circuito. `m_trackId`
está en el paquete 1, offset 36. Un mejor tiempo global compararía Spa contra
Mónaco.

## 2. Maven

Primera dependencia externa, así que toca. IntelliJ: clic derecho en el
proyecto → Add Framework Support → Maven. Genera el `pom.xml` y mueve el código
a `src/main/java`.

## 3. Persistencia en JSON

Añadir Jackson al `pom.xml`. Guardar una sesión por fichero al terminar
(evento SEND o paquete 8, Final Classification), no en cada vuelta.

Estructura por sesión:

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

Notas:
- Claves sin acentos.
- El setup viene en el paquete 5 (bloque de 50 bytes), dos veces por segundo.
- El reparto de frenada y el diferencial se tocan en carrera, así que
  comprobar si `m_brakeBias` cambia durante la sesión. Si cambia, guardarlo por
  vuelta en vez de por sesión.
- El sector 3 no viene en el paquete: se calcula restando S1 y S2 al tiempo
  total de vuelta.
- Guardar clima y temperaturas (paquete 1) o dentro de unos meses no se sabrá
  si un tiempo malo fue del piloto o de la pista.

## 4. Web con Spring Boot

Monolítico: Spring Boot + Thymeleaf, HTML servido desde Java. Sin frontend
separado, sin Node. Gráficas con una librería JS por CDN dentro de la
plantilla.

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
  cooldowns).
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
- **Preguntar por voz.** Ya hay botón (R3 → UDP Action 1). Falta decidir qué
  responde: delta contra la mejor vuelta (requiere tabla distancia-tiempo de la
  vuelta de referencia), estado de gomas, o sector en curso.
- **Voces grabadas.** Generar las frases fijas con un TTS bueno y reproducir
  los wav en vez de sintetizar. Cero latencia y mejor calidad. Los números
  seguirían en TTS de Windows. Sistema mixto.
- **Rivales humanos en online.** `m_aiControlled` del paquete 4 distingue
  humano (0) de IA (1). Con `m_name` se pueden nombrar en los avisos.
