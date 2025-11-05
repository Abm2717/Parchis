package modelo.partida;

import java.util.Random;

/**
 * Dado.
 */
public class Dado {
    private int ultimoValor;         // ultimo valor obtenido
    private final Random random;     // generador de numeros aleatorios

    // Constructor por defecto
    public Dado() {
        this(new Random());
    }

    // Constructor para inyectar Random (util en tests)
    public Dado(Random random) {
        this.random = random;
        this.ultimoValor = 1;
    }

    // Metodo para tirar el dado
    public int tirar() {
        ultimoValor = random.nextInt(6) + 1; // genera numero entre 1 y 6
        return ultimoValor;
    }

    // Getter del ultimo valor
    public int getUltimoValor() {
        return ultimoValor;
    }

    // Setter de ultimo valor (paquetes/tests si necesitas forzar un valor)
    void setUltimoValor(int v) {
        this.ultimoValor = v;
    }
}

