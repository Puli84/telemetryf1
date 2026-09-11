public class Estado {
    public float combustibleVueltas;
    public int   bandera;
    public int   vueltaActual;
    public int ultimaVueltaMs;
    public int   vueltasTotales ;
    public int[] tempGomas = new int[4];   // TI,TD,DI,DD
    public int drsMetros;
    public int drsPermitido;
    public int sector;         // 0, 1, 2
    public int sector1Ms;
    public int sector2Ms;
    public int tipoSesion;
    public int penalties;
    public int avisos;
    public float ersEnergia;
    public int ersModo;
    public int tyresAgeLaps;
    public int deltaCarDelanteMs;
    public int pitStatus;
    public float[] desgasteGomas = new float[4];   // % de desgaste TI,TD,DI,DD
    public int posicion;
    public String nombreLider;
    public int gapLiderMs;
    public String nombreDelante;
    public String nombreDetras;
    public int gapDetrasMs;
    public int idxDelante = -1;
    public int idxDetras = -1;
    public int safetyCarStatus; // 0 = sin SC, 1 = SC, 2 = virtual, 3 = vuelta de formación
    public int usableLifeGomas; // vida útil recomendada (vueltas) del compuesto puesto, calculada por el juego
    public int aleronTraseroDano; // % daño alerón trasero
    public int lateralesDano;     // % daño sidepods (laterales)
    public int trackId;           // circuito de la sesión actual (ver Circuitos.java)
    public int currentLapInvalid;      // 0 = válida, 1 = invalidada (límites de pista, etc.)
    public boolean ultimaVueltaInvalida; // snapshot: ¿la vuelta que acaba de cerrarse estaba invalidada?
}