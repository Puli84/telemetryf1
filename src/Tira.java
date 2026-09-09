import javax.swing.*;
import java.awt.*;

public class Tira extends JPanel {
    private static final int LEDS_ABAJO = 60;
    private static final int LEDS_LADO = 30;
    private static final int GROSOR = 20;

    private int encendidos = 0;
    private int bandera = 0;
    private final Color[] sectores = { Color.DARK_GRAY, Color.DARK_GRAY, Color.DARK_GRAY };

    public void setEncendidos(int n) {
        this.encendidos = n;
        repaint();
    }

    public void setBandera(int b) {
        this.bandera = b;
        repaint();
    }

    public void setSector(int n, Color c) {
        sectores[n] = c;
        repaint();
    }

    public void limpiarSectores() {
        for (int i = 0; i < 3; i++) {
            sectores[i] = Color.DARK_GRAY;
        }
        repaint();
    }

    private Color colorBandera() {
        switch (bandera) {
            case 1: return Color.GREEN;
            case 2: return Color.BLUE;
            case 3: return Color.YELLOW;
            default: return Color.DARK_GRAY;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        int anchoUtil = w - 2 * GROSOR;

        // barra inferior: rev lights
        int ancho = anchoUtil / LEDS_ABAJO;
        for (int i = 0; i < LEDS_ABAJO; i++) {
            Color c;
            if (i < LEDS_ABAJO / 3) c = Color.GREEN;
            else if (i < LEDS_ABAJO * 2 / 3) c = Color.RED;
            else c = Color.MAGENTA;

            g.setColor(i < encendidos ? c : Color.DARK_GRAY);
            g.fillRect(GROSOR + i * ancho, h - GROSOR, ancho - 2, GROSOR);
        }

        // barra superior: 3 sectores
        int anchoSector = anchoUtil / 3;
        for (int s = 0; s < 3; s++) {
            g.setColor(sectores[s]);
            g.fillRect(GROSOR + s * anchoSector, 0, anchoSector - 4, GROSOR);
        }

        // columnas laterales: banderas
        int alto = (h - 2 * GROSOR) / LEDS_LADO;
        g.setColor(colorBandera());
        for (int i = 0; i < LEDS_LADO; i++) {
            g.fillRect(0, GROSOR + i * alto, GROSOR, alto - 2);
            g.fillRect(w - GROSOR, GROSOR + i * alto, GROSOR, alto - 2);
        }
    }
}
