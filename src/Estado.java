public class Estado {
    public float combustibleVueltas;
    public int   bandera;
    public int   vueltaActual;
    public int ultimaVueltaMs;
    public int   vueltasTotales = 20;
    public int[] tempGomas = new int[4];   // TI,TD,DI,DD
    public int drsMetros;
    public int drsPermitido;
    public int sector;         // 0, 1, 2
    public int sector1Ms;
    public int sector2Ms;
}