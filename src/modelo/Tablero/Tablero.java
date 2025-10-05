    package modelo.Tablero;

import java.util.ArrayList;
import java.util.List;

public class Tablero {
    // Atributos
    private List<Casilla> casillas;

    // Constructor
    public Tablero() {
        casillas = new ArrayList<>(); // 🔹 inicializar la lista
        inicializarCasillas();
    }  

    // Inicializar casillas (ejemplo basico, puedes ajustar tipos y colores)
    public void inicializarCasillas() {
        casillas = new ArrayList<>();

        // Casillas normales
        for (int i = 1; i <= 68; i++) {
            TipoCasilla tipo = TipoCasilla.NORMAL;

            // Casilla de salida/inicio roja
            if (i == 1) tipo = TipoCasilla.INICIO;

            // Casilla de salida/inicio azul
            if (i == 18) tipo = TipoCasilla.INICIO;

            casillas.add(new Casilla(i, i, ColorCasilla.NINGUNO, tipo, 2));
        }

        // Casillas meta / casa final (ejemplo para rojo y azul)
        for (int i = 69; i <= 72; i++) {
            casillas.add(new Casilla(i, 0, ColorCasilla.ROJO, TipoCasilla.META, 1));
        }
        for (int i = 73; i <= 76; i++) {
            casillas.add(new Casilla(i, 0, ColorCasilla.AZUL, TipoCasilla.META, 1));
        }
    }



    // Obtener casilla por indice
    public Casilla getCasillaPorIndice(int indice) {
        for (Casilla c : casillas) {
            if (c.getIndice() == indice) return c;
        }
        return null;
    }

    // Obtener casilla por posicion
    public Casilla getCasillaPorPosicion(int posicion) {
        for (Casilla c : casillas) {
            if (c.getPosicion() == posicion) return c;
        }
        return null;
    }

    // Numero total de casillas
    public int getNumeroCasillas() {
        return casillas.size();
    }

    // Getter
    public List<Casilla> getCasillas() {
        return casillas;
    }
}
