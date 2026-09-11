import javax.swing.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Listener {

    private static final int LEDS_ABAJO = 60;
    private static Tira tira;

    static Estado estado = new Estado();
    static ReglaCombustible reglaCombustible = new ReglaCombustible(0.5f);
    static ReglaBanderas reglaBanderas = new ReglaBanderas();
    static ReglaDrs reglaDrs =new ReglaDrs();
    static ReglaSectores reglaSectores = new ReglaSectores();
    static ReglaVuelta reglaVuelta = new ReglaVuelta();
    static ReglaSanciones reglaSanciones = new ReglaSanciones();
    static ReglaErs reglaErs = new ReglaErs();
    static ReglaUndercut reglaUndercut = new ReglaUndercut();
    static ReglaDesgaste reglaDesgaste = new ReglaDesgaste();
    static ReglaDanos reglaDanos = new ReglaDanos();
    static Locutor locutor = new Locutor();
    static Oyente oyente = new Oyente();
    static HistorialCircuitos historial = new HistorialCircuitos();
    static long sessionUIDAnterior = 0;
    static int[] pitStatusAnterior = new int[22];
    static String[] nombres = new String[22];
    static int[] compuestoTodos = new int[22];
    static int[] edadTodos = new int[22];
    static int[] ultimaVueltaBoxes = new int[22];
    static long botonesAnterior = 0;
    static volatile boolean escuchando = false;
    // AJUSTAR: pulsa tu botón de push-to-talk, mira en consola "BUTN: 0x........"
    // y pon aquí ese valor exacto.
    static final long PTT_MASK = 0x00100000L;

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket (20777 );
        byte[] buffer = new byte[2048];

        JFrame ventana = new JFrame ( "Rev lights" );
        tira = new Tira ();

        ventana.add ( tira );
        ventana.setSize ( 900, 500 );
        ventana.setDefaultCloseOperation ( JFrame.EXIT_ON_CLOSE );
        ventana.setVisible ( true );

        System.out.println ( "Escuchando en 20777..." );


        while (true) {
            DatagramPacket paquete = new DatagramPacket ( buffer, buffer.length );
            socket.receive ( paquete );

            ByteBuffer bb = ByteBuffer.wrap ( buffer );
            bb.order ( ByteOrder.LITTLE_ENDIAN );

            int packetId = bb.get ( 6 );
            int miCoche = bb.get ( 27 );
            long sessionUID = bb.getLong(7);

            if (sessionUID != sessionUIDAnterior) {
                sessionUIDAnterior = sessionUID;
                reiniciarReglas();
            }

            switch (packetId) {
                case 2 -> { leerVuelta ( bb, miCoche ); detectarBoxes(bb); actualizarGaps(bb, miCoche); }
                case 3 ->leerEvento(bb, miCoche);

                case 6 -> leerTelemetria ( bb, miCoche );
                case 7 -> leerEstado ( bb, miCoche );
                case 1 -> leerSesion ( bb );
                case 4 -> leerParticipantes ( bb );
                case 8 -> leerClasificacionFinal ( bb );
                case 10 -> leerDanos ( bb, miCoche );
                case 12 -> leerTyreSets ( bb, miCoche );
            }
        }
    }

    private static void reiniciarReglas() {
        estado = new Estado();
        reglaCombustible = new ReglaCombustible(0.5f);
        reglaBanderas = new ReglaBanderas();
        reglaDrs = new ReglaDrs();
        reglaSectores = new ReglaSectores();
        reglaVuelta = new ReglaVuelta();
        reglaSanciones = new ReglaSanciones();
        reglaErs = new ReglaErs();
        reglaUndercut = new ReglaUndercut();
        reglaDesgaste = new ReglaDesgaste();
        reglaDanos = new ReglaDanos();
        java.util.Arrays.fill(pitStatusAnterior, -1);
        java.util.Arrays.fill(compuestoTodos, -1);
        java.util.Arrays.fill(edadTodos, -1);
        java.util.Arrays.fill(ultimaVueltaBoxes, -1);
        botonesAnterior = 0;
        System.out.println("Nueva sesión detectada, reglas reiniciadas.");
    }

    private static void leerTelemetria(ByteBuffer bb, int coche) {
        int base = 29 + coche * 60;

        int velocidad = bb.getShort ( base );
        int rpm = bb.getShort ( base + 16 );
        int drs = bb.get ( base + 18 );
        int revLights = bb.get ( base + 19 );

        tira.setEncendidos ( (revLights * LEDS_ABAJO) / 100 );

        for (int i = 0; i < 4; i++) {
            estado.tempGomas[i] = bb.get(base + 30 + i) & 0xFF;
        }
    }

    private static void leerEstado(ByteBuffer bb, int coche) {
        int base = 29 + coche * 55;

        estado.combustibleVueltas = bb.getFloat(base + 13);
        estado.bandera  = bb.get ( base + 28 );
        estado.drsMetros    = bb.getShort(base + 23) & 0xFFFF;
        estado.drsPermitido = bb.get(base + 22);
        estado.ersEnergia = bb.getFloat(base + 37);
        estado.ersModo = bb.get(base + 41) & 0xFF;
        estado.tyresAgeLaps = bb.get(base + 27) & 0xFF;

        for (int i = 0; i < 22; i++) {
            int b = 29 + i * 55;
            compuestoTodos[i] = bb.get(b + 26) & 0xFF; // compuesto visual
            edadTodos[i] = bb.get(b + 27) & 0xFF;
        }

        tira.setBandera ( estado.bandera );
        System.out.println("Gasolina: " + estado.combustibleVueltas + " | Bandera: " + estado.bandera);
        String msg = reglaCombustible.evaluar(estado);
        if (msg != null) locutor.decir("combustible", msg, 80000);
        String flag = reglaBanderas.evaluar(estado);
        if (flag != null) locutor.decir("bandera", flag, 0);
        String drs = reglaDrs.evaluar(estado);
        if(drs!=null)locutor.decir("drs",drs,0,Locutor.PRIORIDAD_ALTA);
        String ers = reglaErs.evaluar(estado);
        if (ers != null) locutor.decir("ers", ers, 0);
    }

    private static void leerVuelta(ByteBuffer bb, int coche) {
        int base = 29 + coche * 57;
        int posicion = bb.get ( base + 32 );
        estado.sector = bb.get(base + 36);
        estado.ultimaVueltaMs = bb.getInt(base);

        int s1ms  = bb.getShort(base + 8)  & 0xFFFF;
        int s1min = bb.get(base + 10)      & 0xFF;
        estado.sector1Ms = s1min * 60000 + s1ms;

        int s2ms  = bb.getShort(base + 11) & 0xFFFF;
        int s2min = bb.get(base + 13)      & 0xFF;
        estado.sector2Ms = s2min * 60000 + s2ms;
        estado.posicion = posicion;

        int nuevaVuelta = bb.get(base + 33);
        if (nuevaVuelta != estado.vueltaActual) {
            // snapshot: cómo quedó la vuelta que ACABA de cerrarse, antes de pisar el dato con el de la nueva
            estado.ultimaVueltaInvalida = (estado.currentLapInvalid == 1);
        }
        estado.vueltaActual = nuevaVuelta;
        estado.currentLapInvalid = bb.get(base + 37) & 0xFF;

        estado.penalties = bb.get(base + 38) & 0xFF;
        estado.avisos = bb.get(base + 40) & 0xFF;
        estado.pitStatus = bb.get(base + 34) & 0xFF;

        int deltaMs  = bb.getShort(base + 14) & 0xFFFF;
        int deltaMin = bb.get(base + 16)      & 0xFF;
        estado.deltaCarDelanteMs = deltaMin * 60000 + deltaMs;

        String sec = reglaSectores.evaluar(estado);
        if (sec != null && estado.safetyCarStatus == 0) locutor.decir("sector", sec, 0);
        String v = reglaVuelta.evaluar(estado);
        if (v != null && estado.safetyCarStatus == 0) locutor.decir("vuelta", v, 0);
        if (reglaVuelta.esNuevoMejorSesion() && esSesionCarrera(estado.tipoSesion)) {
            String record = historial.registrar(estado.trackId, estado.ultimaVueltaMs);
            if (record != null) locutor.decir("record", record, 0);
        }
        String sancion = reglaSanciones.evaluar(estado);
        if (sancion != null) locutor.decir("sancion", sancion, 0);
        String undercut = reglaUndercut.evaluar(estado);
        if (undercut != null) locutor.decir("undercut", undercut, 0);

        System.out.println ( "pos=" + posicion + " vuelta=" + estado.vueltaActual );
    }

    private static void leerSesion(ByteBuffer bb) {

        estado.tipoSesion = bb.get(35) & 0xFF;
        estado.vueltasTotales = bb.get(32) & 0xFF;
        estado.safetyCarStatus = bb.get(153) & 0xFF;
        estado.trackId = bb.get(36);
    }
    private static void leerEvento(ByteBuffer bb, int miCoche) {
        byte[] cod = new byte[4];
        bb.position(29);
        bb.get(cod);
        String evento = new String(cod, java.nio.charset.StandardCharsets.US_ASCII);

        switch (evento) {
            case "BUTN" -> {
                long botones = bb.getInt(33) & 0xFFFFFFFFL;
                System.out.printf("BUTN: 0x%08X%n", botones);

                boolean pttAhora = (botones & PTT_MASK) != 0;
                boolean pttAntes = (botonesAnterior & PTT_MASK) != 0;
                if (pttAhora && !pttAntes && !escuchando) {
                    escuchando = true;
                    new Thread(() -> {
                        try {
                            String comando = oyente.escuchar(apellidosConocidos());
                            manejarComando(comando);
                        } finally {
                            escuchando = false;
                        }
                    }, "ptt-oyente").start();
                }
                botonesAnterior = botones;
            }
            case "PENA" -> {
                int vehicleIdx = bb.get(35) & 0xFF;
                if (vehicleIdx != miCoche) return;
                int penaltyType = bb.get(33) & 0xFF;
                String msg = mensajePenalidad(penaltyType);
                if (msg != null) locutor.decir("penalidad", msg, 0);
            }
            case "RTMT" -> {
                int vehicleIdx = bb.get(33) & 0xFF;
                if (vehicleIdx != miCoche) return;
                int reason = bb.get(34) & 0xFF;
                String msg = mensajeRetirada(reason);
                if (msg != null) locutor.decir("retirada", msg, 0);
            }
            case "FTLP" -> {
                int vehicleIdx = bb.get(33) & 0xFF;
                int lapTimeMs = Math.round(bb.getFloat(34) * 1000);
                String tiempo = ReglaVuelta.formatearTiempo(lapTimeMs);
                String nombre = nombreDe(vehicleIdx);

                String msg;
                if (vehicleIdx == miCoche) {
                    msg = "Vuelta rápida para ti, " + tiempo;
                } else {
                    int mejorMio = reglaVuelta.getMejor();
                    if (mejorMio > 0) {
                        float diff = (lapTimeMs - mejorMio) / 1000f;
                        String comp = diff >= 0
                                ? String.format("%.2f por encima de tu mejor vuelta", diff)
                                : String.format("%.2f por debajo de tu mejor vuelta", -diff);
                        msg = "Vuelta rápida de " + nombre + ", " + tiempo + ", " + comp;
                    } else {
                        msg = "Vuelta rápida de " + nombre + ", " + tiempo;
                    }
                }
                locutor.decir("vueltarapida", msg, 0);
            }
            case "SCAR" -> {
                int safetyCarType = bb.get(33) & 0xFF;
                int eventType = bb.get(34) & 0xFF;
                String msg = mensajeSafetyCar(safetyCarType, eventType);
                if (msg != null) locutor.decir("safetycar", msg, 0);

                if (eventType == 0) { // desplegado
                    String consejo = consejoBoxesSafetyCar();
                    if (consejo != null) locutor.decir("consejoboxes", consejo, 0);
                }
            }
        }
    }

    private static String mensajeSafetyCar(int tipo, int evento) {
        String nombreTipo = switch (tipo) {
            case 1 -> "Coche de seguridad";
            case 2 -> "Coche de seguridad virtual";
            case 3 -> "Coche de seguridad de formación";
            default -> "Coche de seguridad";
        };
        return switch (evento) {
            case 0 -> nombreTipo + " desplegado";
            case 1 -> nombreTipo + " entra a boxes, prepárate para reanudar";
            case 3 -> "Se reanuda la carrera";
            default -> null;
        };
    }

    private static final int DESGASTE_MIN_PARA_BOXES_SC = 5;   // vueltas de gomas a partir de las cuales conviene parar bajo SC

    private static String consejoBoxesSafetyCar() {
        if (estado.pitStatus != 0) return null; // ya está entrando o en boxes
        if (estado.tyresAgeLaps < DESGASTE_MIN_PARA_BOXES_SC) return null;

        if (estado.vueltasTotales <= 0 || estado.usableLifeGomas <= 0) {
            return "Con el desgaste que llevas, es buen momento para entrar a boxes";
        }

        int quedan = estado.vueltasTotales - estado.vueltaActual;
        if (quedan <= estado.usableLifeGomas) {
            return "Con el desgaste que llevas y las vueltas que quedan, entra a boxes, llegas a meta con estas gomas";
        }
        return "Con el desgaste que llevas, aprovecha para entrar a boxes, aunque tendrás que parar otra vez antes de meta";
    }

    private static String mensajePenalidad(int penaltyType) {
        return switch (penaltyType) {
            case 0 -> "Drive through";
            case 1 -> "Stop and go";
            case 2 -> "Penalización de parrilla";
            case 4 -> "Sanción de tiempo";
            case 5 -> "Aviso de dirección de carrera, cuidado";
            case 6 -> "Has sido Descalificado";
            default -> null;
        };
    }

    private static String mensajeRetirada(int reason) {
        return switch (reason) {
            case 1 -> "Te has retirado de la carrera";
            case 3 -> "Daño irreparable, te retiras de la carrera";
            case 6 -> "Bandera negra, quedas descalificado";
            case 8 -> "Fallo mecánico, te retiras de la carrera";
            default -> null;
        };
    }

    private static void leerParticipantes(ByteBuffer bb) {
        for (int i = 0; i < 22; i++) {
            int base = 30 + i * 57;
            byte[] raw = new byte[32];
            bb.position(base + 7);
            bb.get(raw);
            int len = 0;
            while (len < raw.length && raw[len] != 0) len++;
            nombres[i] = new String(raw, 0, len, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static String nombreDe(int idx) {
        String n = (idx >= 0 && idx < nombres.length) ? nombres[idx] : null;
        return (n == null || n.isBlank()) ? ("Coche " + idx) : n;
    }

    private static void detectarBoxes(ByteBuffer bb) {
        StringBuilder mensaje = new StringBuilder();
        for (int i = 0; i < 22; i++) {
            int base = 29 + i * 57;
            int pitStatus = bb.get(base + 34) & 0xFF;
            if (pitStatus == 1 && pitStatusAnterior[i] != 1) {
                int vueltaCoche = bb.get(base + 33) & 0xFF;
                ultimaVueltaBoxes[i] = vueltaCoche;
                mensaje.append(nombreDe(i)).append(" entra en boxes. ");
            }
            pitStatusAnterior[i] = pitStatus;
        }
        if (mensaje.length() > 0) {
            locutor.decir("boxes", mensaje.toString().trim(), 0);
        }
    }

    private static void actualizarGaps(ByteBuffer bb, int miCoche) {
        int[] posiciones = new int[22];
        int[] deltaLiderMs = new int[22];

        for (int i = 0; i < 22; i++) {
            int base = 29 + i * 57;
            posiciones[i] = bb.get(base + 32) & 0xFF;
            int msPart  = bb.getShort(base + 17) & 0xFFFF;
            int minPart = bb.get(base + 19)      & 0xFF;
            deltaLiderMs[i] = minPart * 60000 + msPart;
        }

        int miPos = posiciones[miCoche];
        int miDeltaLider = deltaLiderMs[miCoche];

        estado.gapLiderMs = miDeltaLider;
        estado.nombreLider = null;
        estado.nombreDelante = null;
        estado.nombreDetras = null;
        estado.gapDetrasMs = 0;
        estado.idxDelante = -1;
        estado.idxDetras = -1;

        for (int i = 0; i < 22; i++) {
            if (posiciones[i] == 1) estado.nombreLider = nombreDe(i);
            if (posiciones[i] == miPos - 1) {
                estado.nombreDelante = nombreDe(i);
                estado.idxDelante = i;
            }
            if (posiciones[i] == miPos + 1) {
                estado.nombreDetras = nombreDe(i);
                estado.idxDetras = i;
                estado.gapDetrasMs = deltaLiderMs[i] - miDeltaLider; // aprox., asume misma vuelta
            }
        }
    }

    private static void leerTyreSets(ByteBuffer bb, int miCoche) {
        int carIdx = bb.get(29) & 0xFF;
        if (carIdx != miCoche) return; // este paquete cicla por coche, solo nos interesa el nuestro

        int fittedIdx = bb.get(230) & 0xFF;
        int base = 30 + fittedIdx * 10;
        estado.usableLifeGomas = bb.get(base + 6) & 0xFF;
    }

    private static void leerDanos(ByteBuffer bb, int coche) {
        int base = 29 + coche * 46;
        for (int i = 0; i < 4; i++) {
            estado.desgasteGomas[i] = bb.getFloat(base + i * 4);
        }
        estado.aleronTraseroDano = bb.get(base + 30) & 0xFF;
        estado.lateralesDano = bb.get(base + 33) & 0xFF;

        String desgaste = reglaDesgaste.evaluar(estado);
        if (desgaste != null) locutor.decir("desgaste", desgaste, 0);
        String danos = reglaDanos.evaluar(estado);
        if (danos != null) locutor.decir("danos", danos, 0);
    }

    private static void manejarComando(String comando) {
        if (comando == null) return; // no se reconoció nada: silencio, no el resumen

        String c = comando.toLowerCase().trim();

        String[] partes = c.split("\\s+");
        if (partes.length >= 2 && partes[partes.length - 1].equals("boxes")) {
            String apellido = String.join(" ", java.util.Arrays.copyOf(partes, partes.length - 1));
            responderBoxesPiloto(apellido);
            return;
        }

        switch (c) {
            case "gasolina" -> locutor.decir("respuesta",
                    String.format("Gasolina, %.2f vueltas de margen", estado.combustibleVueltas), 0);
            case "posicion" -> locutor.decir("respuesta", "Vas en posición " + estado.posicion, 0);
            case "gomas" -> {
                float peor = 0;
                for (float d : estado.desgasteGomas) if (d > peor) peor = d;
                locutor.decir("respuesta",
                        String.format("Gomas con %d vueltas, %.0f por ciento de desgaste", estado.tyresAgeLaps, peor), 0);
            }
            case "lider" -> locutor.decir("respuesta", estado.posicion == 1
                    ? "Vas líder"
                    : (estado.nombreLider != null
                        ? estado.nombreLider + String.format(" lidera, a %.1f segundos", estado.gapLiderMs / 1000f)
                        : "Aún no hay datos del líder"), 0);
            case "delante" -> locutor.decir("respuesta", estado.nombreDelante != null
                    ? estado.nombreDelante
                        + String.format(" delante, a %.1f segundos. ", estado.deltaCarDelanteMs / 1000f)
                        + infoGomasRival(estado.idxDelante)
                    : "No hay nadie delante", 0);
            case "detras" -> locutor.decir("respuesta", estado.nombreDetras != null
                    ? estado.nombreDetras
                        + String.format(" detrás, a %.1f segundos. ", estado.gapDetrasMs / 1000f)
                        + infoGomasRival(estado.idxDetras)
                    : "No hay nadie detrás", 0);
            case "vuelta" -> locutor.decir("respuesta", estado.ultimaVueltaMs > 0
                    ? "Tu última vuelta, " + ReglaVuelta.formatearTiempo(estado.ultimaVueltaMs)
                    : "Aún no hay vuelta registrada", 0);
            case "estado" -> anunciarEstado();
            case "vuelta historica" -> locutor.decir("respuesta", historial.consultar(estado.trackId), 0);
            default -> { } // "nada" (silencio o no reconocido): no decir nada
        }
    }

    // Tipos de sesión (PacketSessionData, spec F1 25): 15=Carrera, 16=Carrera 2, 17=Carrera 3.
    private static boolean esSesionCarrera(int tipoSesion) {
        return tipoSesion == 15 || tipoSesion == 16 || tipoSesion == 17;
    }

    private static String limpiar(String s) {
        String normalizado = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{M}", "");
    }

    private static String[] apellidosConocidos() {
        java.util.LinkedHashSet<String> apellidos = new java.util.LinkedHashSet<>();
        for (String n : nombres) {
            if (n == null || n.isBlank()) continue;
            String limpio = limpiar(n).toLowerCase().trim();
            int idx = limpio.lastIndexOf(' ');
            apellidos.add(idx >= 0 ? limpio.substring(idx + 1) : limpio);
        }
        return apellidos.toArray(new String[0]);
    }

    private static int buscarPorApellido(String apellidoBuscado) {
        for (int i = 0; i < nombres.length; i++) {
            if (nombres[i] == null) continue;
            String limpio = limpiar(nombres[i]).toLowerCase();
            if (limpio.endsWith(apellidoBuscado)) return i;
        }
        return -1;
    }

    private static void responderBoxesPiloto(String apellidoBuscado) {
        int idx = buscarPorApellido(apellidoBuscado);
        if (idx < 0) {
            locutor.decir("respuesta", "No encuentro a ese piloto", 0);
            return;
        }
        String nombre = nombreDe(idx);
        if (ultimaVueltaBoxes[idx] < 0) {
            locutor.decir("respuesta", nombre + " no ha entrado a boxes todavía", 0);
        } else {
            locutor.decir("respuesta", nombre + " entró a boxes en la vuelta " + ultimaVueltaBoxes[idx], 0);
        }
    }

    private static String infoGomasRival(int idx) {
        if (idx < 0 || idx >= compuestoTodos.length || compuestoTodos[idx] < 0) return "";
        return "Lleva " + nombreCompuesto(compuestoTodos[idx]) + ", " + edadTodos[idx] + " vueltas";
    }

    private static String nombreCompuesto(int visual) {
        return switch (visual) {
            case 16 -> "blandos";
            case 17 -> "medios";
            case 18 -> "duros";
            case 7 -> "intermedios";
            case 8 -> "de lluvia";
            default -> "compuesto desconocido";
        };
    }

    private static void anunciarEstado() {
        float peorGoma = 0;
        for (float d : estado.desgasteGomas) {
            if (d > peorGoma) peorGoma = d;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Posición ").append(estado.posicion).append(". ");
        msg.append(String.format("Gasolina, %.2f vueltas de margen. ", estado.combustibleVueltas));
        msg.append(String.format("Gomas al %.0f por ciento de desgaste. ", peorGoma));

        if (estado.posicion == 1) {
            msg.append("Vas líder. ");
        } else if (estado.nombreLider != null) {
            msg.append(estado.nombreLider)
               .append(String.format(" lidera, a %.1f segundos. ", estado.gapLiderMs / 1000f));
        }

        if (estado.nombreDelante != null && estado.deltaCarDelanteMs > 0) {
            msg.append(estado.nombreDelante)
               .append(String.format(" delante, a %.1f segundos. ", estado.deltaCarDelanteMs / 1000f));
        }

        if (estado.nombreDetras != null) {
            msg.append(estado.nombreDetras)
               .append(String.format(" detrás, a %.1f segundos.", estado.gapDetrasMs / 1000f));
        }

        locutor.decir("estado", msg.toString().trim(), 5000);
    }

    private static void leerClasificacionFinal(ByteBuffer bb) {
        int numCars = bb.get(29) & 0xFF;
        String[] podio = new String[4]; // índice 1,2,3 = posición

        for (int i = 0; i < numCars; i++) {
            int base = 30 + i * 46;
            int posicion = bb.get(base) & 0xFF;
            if (posicion >= 1 && posicion <= 3) {
                podio[posicion] = nombreDe(i);
            }
        }

        if (podio[1] != null) {
            StringBuilder msg = new StringBuilder("Clasificación final: primero " + podio[1]);
            if (podio[2] != null) msg.append(", segundo ").append(podio[2]);
            if (podio[3] != null) msg.append(", tercero ").append(podio[3]);
            locutor.decir("final", msg.toString(), 0);
        }
    }
}


