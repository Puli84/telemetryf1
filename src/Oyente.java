import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Oyente {

    // Gramática cerrada: mucho más fiable que dictado libre con ruido de fondo de simulador.
    public String escuchar(String[] apellidos) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("Add-Type -AssemblyName System.Speech; ")
           .append("try { $culture = New-Object System.Globalization.CultureInfo('es-ES'); ")
           .append("$rec = New-Object System.Speech.Recognition.SpeechRecognitionEngine($culture) } ")
           .append("catch { $rec = New-Object System.Speech.Recognition.SpeechRecognitionEngine }; ")
           .append("$choices = New-Object System.Speech.Recognition.Choices; ")
           .append("$choices.Add([string[]]('gasolina','posicion','gomas','lider','delante','detras','vuelta','estado')); ")
           .append("$gb = New-Object System.Speech.Recognition.GrammarBuilder; ")
           .append("$gb.Append($choices); ")
           .append("$grammar = New-Object System.Speech.Recognition.Grammar($gb); ")
           .append("$rec.LoadGrammar($grammar); ");

        if (apellidos != null && apellidos.length > 0) {
            StringBuilder lista = new StringBuilder();
            for (String a : apellidos) {
                if (lista.length() > 0) lista.append(",");
                lista.append("'").append(a.replace("'", "''")).append("'");
            }
            cmd.append("$apellidos = New-Object System.Speech.Recognition.Choices; ")
               .append("$apellidos.Add([string[]](").append(lista).append(")); ")
               .append("$gbNombre = New-Object System.Speech.Recognition.GrammarBuilder; ")
               .append("$gbNombre.Append($apellidos); ")
               .append("$gbNombre.Append('boxes'); ")
               .append("$grammarNombre = New-Object System.Speech.Recognition.Grammar($gbNombre); ")
               .append("$rec.LoadGrammar($grammarNombre); ");
        }

        cmd.append("$rec.SetInputToDefaultAudioDevice(); ")
           .append("[console]::beep(1000,150); ") // aviso audible del instante exacto en que empieza a escuchar
           .append("$result = $rec.Recognize([TimeSpan]::FromSeconds(5)); ")
           .append("if ($result -ne $null) { Write-Output $result.Text } else { Write-Output 'NADA' }");

        try {
            Process p = new ProcessBuilder("powershell", "-Command", cmd.toString()).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String linea = reader.readLine();
            p.waitFor();
            return linea;
        } catch (Exception ex) {
            System.err.println("Error de reconocimiento: " + ex.getMessage());
            return null;
        }
    }
}
