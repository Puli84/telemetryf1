import java.util.Map;

// Nombres de circuito por trackId, según la tabla "Track IDs" del spec oficial
// "Data Output from F1 25 Game" (Apéndices). Los IDs que no aparecen en esa tabla
// no existen en el calendario de F1 25 (p.ej. Paul Ricard, Hockenheim o Sochi ya no están).
public class Circuitos {

    private static final Map<Integer, String> NOMBRES = Map.ofEntries(
            Map.entry(0, "Melbourne"),
            Map.entry(2, "Shanghái"),
            Map.entry(3, "Baréin"),
            Map.entry(4, "Catalunya"),
            Map.entry(5, "Mónaco"),
            Map.entry(6, "Montreal"),
            Map.entry(7, "Silverstone"),
            Map.entry(9, "Hungaroring"),
            Map.entry(10, "Spa"),
            Map.entry(11, "Monza"),
            Map.entry(12, "Singapur"),
            Map.entry(13, "Suzuka"),
            Map.entry(14, "Abu Dabi"),
            Map.entry(15, "Austin"),
            Map.entry(16, "Interlagos"),
            Map.entry(17, "Red Bull Ring"),
            Map.entry(19, "Ciudad de México"),
            Map.entry(20, "Bakú"),
            Map.entry(26, "Zandvoort"),
            Map.entry(27, "Imola"),
            Map.entry(29, "Jeddah"),
            Map.entry(30, "Miami"),
            Map.entry(31, "Las Vegas"),
            Map.entry(32, "Losail"),
            Map.entry(39, "Silverstone invertido"),
            Map.entry(40, "Red Bull Ring invertido"),
            Map.entry(41, "Zandvoort invertido")
    );

    public static String nombre(int trackId) {
        return NOMBRES.getOrDefault(trackId, "este circuito");
    }
}
