package modelo.partida;

import java.util.Random;

/**
 * Dado con posibilidad de inyección de Random para tests.
 */
public class Dado {
    private int ultimoValor;         // último valor obtenido
    private final Random random;     // generador de números aleatorios

    // Constructor por defecto
    public Dado() {
        this(new Random());
    }

    // Constructor para inyectar Random (útil en tests)
    public Dado(Random random) {
        this.random = random;
        this.ultimoValor = 1;
    }

    // Método para tirar el dado
    public int tirar() {
        ultimoValor = random.nextInt(6) + 1; // genera número entre 1 y 6
        return ultimoValor;
    }

    // Getter del último valor
    public int getUltimoValor() {
        return ultimoValor;
    }

    // Setter de último valor (paquetes/tests si necesitas forzar un valor)
    void setUltimoValor(int v) {
        this.ultimoValor = v;
    }
}

