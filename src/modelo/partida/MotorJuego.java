package modelo.partida;

import modelo.Jugador.Jugador;
import modelo.Ficha.Ficha;
import modelo.Ficha.EstadoFicha;
import modelo.Tablero.Casilla;
import modelo.Tablero.Tablero;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MotorJuego - Motor de reglas del Parchís con variantes específicas:
 * 
 * REGLAS IMPLEMENTADAS:
 * 1. Cada jugador tiene hasta 4 fichas
 * 2. En la salida pueden haber dos fichas del mismo jugador
 * 3. Para sacar una ficha de casa se necesita sacar un 5
 * 4. Sistema de dobles (dos dados iguales):
 *    - Rompe bloqueo PROPIO del jugador que tiró (si existe)
 *    - Da derecho a volver a tirar
 *    - Si sacas 3 dobles seguidos, pierdes una ficha
 * 5. Al comer una ficha rival:
 *    - La ficha capturada vuelve a casa
 *    - Se otorgan 20 casillas de bonus que pueden usarse en CUALQUIER ficha
 * 6. Al llegar a meta: bonus de 10 puntos
 * 7. Bloqueos:
 *    - Dos fichas del mismo jugador en misma casilla forman bloqueo
 *    - Nadie puede pasar por una casilla con bloqueo
 *    - En casillas seguras se puede formar bloqueo con cualquier ficha
 * 8. Casillas seguras: no permiten captura, permiten coexistencia
 * 
 * Thread-safety: Todos los métodos públicos están sincronizados
 */
public class MotorJuego {

    private final Partida partida;
    private final Dado dado1;
    private final Dado dado2;

    // Control de dobles consecutivos por jugador
    private final Map<Integer, Integer> contadorDobles = new HashMap<>();

    // Pool de movimientos bonus por jugador (se ganan al comer fichas)
    private final Map<Integer, Integer> bonusMoves = new HashMap<>();

    // ============================
    // CONSTRUCTORES
    // ============================

    public MotorJuego(Partida partida) {
        this(partida, new Dado(), new Dado());
    }

    public MotorJuego(Partida partida, Dado dado1, Dado dado2) {
        this.partida = Objects.requireNonNull(partida, "Partida no puede ser null");
        this.dado1 = Objects.requireNonNull(dado1, "Dado1 no puede ser null");
        this.dado2 = Objects.requireNonNull(dado2, "Dado2 no puede ser null");
    }

    // ============================
    // EXCEPCIONES DE DOMINIO
    // ============================

    public static class JuegoException extends RuntimeException {
        public JuegoException(String msg) { 
            super(msg); 
        }
        public JuegoException(String msg, Throwable cause) { 
            super(msg, cause); 
        }
    }

    public static class MovimientoInvalidoException extends JuegoException {
        public MovimientoInvalidoException(String msg) { 
            super(msg); 
        }
    }

    public static class NoEsTuTurnoException extends JuegoException {
        public NoEsTuTurnoException(String msg) { 
            super(msg); 
        }
    }

    public static class FichaNoEncontradaException extends JuegoException {
        public FichaNoEncontradaException(String msg) { 
            super(msg); 
        }
    }

    // ============================
    // OPERACIONES PRINCIPALES
    // ============================

    /**
     * Tira ambos dados y aplica reglas de doble:
     * - Si es doble, intenta romper bloqueo PROPIO del jugador
     * - Si es doble, el jugador puede volver a tirar (manejado por controlador)
     * - Si saca 3 dobles consecutivos, pierde una ficha
     * 
     * @param jugadorId ID del jugador que tira los dados
     * @return Array [dado1, dado2] con los valores obtenidos
     * @throws NoEsTuTurnoException si no es el turno del jugador
     */
    public synchronized ResultadoDados tirarDados(int jugadorId) {
        validarTurno(jugadorId);
        
        int v1 = dado1.tirar();
        int v2 = dado2.tirar();
        boolean esDoble = (v1 == v2);
        
        if (!esDoble) {
            // No es doble -> resetear contador
            contadorDobles.put(jugadorId, 0);
            return new ResultadoDados(v1, v2, false, false);
        }
        
        // ES DOBLE
        int cont = contadorDobles.getOrDefault(jugadorId, 0) + 1;
        contadorDobles.put(jugadorId, cont);
        
        boolean bloqueoRoto = false;
        boolean fichaPerdida = false;
        
        // Intentar romper bloqueo PROPIO
        if (tieneBloqueoPropio(jugadorId)) {
            bloqueoRoto = romperBloqueoPropioSiExiste(jugadorId);
        }
        
        // Penalización por 3 dobles consecutivos
        if (cont >= 3) {
            perderFichaPorTresDobles(jugadorId);
            contadorDobles.put(jugadorId, 0);
            fichaPerdida = true;
        }
        
        return new ResultadoDados(v1, v2, esDoble, bloqueoRoto, fichaPerdida, cont);
    }

    /**
     * Mueve una ficha según el número de pasos indicado.
     * Valida todas las reglas: salida, bloqueos, capturas, casillas seguras.
     * 
     * @param jugadorId ID del jugador
     * @param fichaId ID de la ficha a mover
     * @param pasos Número de casillas a avanzar
     * @return Resultado del movimiento con información de capturas y bonos
     * @throws NoEsTuTurnoException si no es el turno del jugador
     * @throws MovimientoInvalidoException si el movimiento viola alguna regla
     */
    public synchronized ResultadoMovimiento moverFicha(int jugadorId, int fichaId, int pasos) {
        validarTurno(jugadorId);
        
        if (pasos <= 0) {
            throw new MovimientoInvalidoException("Los pasos deben ser positivos");
        }

        Ficha ficha = partida.getFicha(jugadorId, fichaId);
        if (ficha == null) {
            throw new FichaNoEncontradaException("Ficha no encontrada: " + fichaId);
        }

        Tablero tablero = partida.getTablero();
        ResultadoMovimiento resultado = new ResultadoMovimiento();

        // CASO 1: Ficha está en casa -> intentar sacar
        if (ficha.estaEnCasa()) {
            return sacarFichaDeCasa(ficha, jugadorId, pasos, resultado);
        }

        // CASO 2: Ficha está en tablero -> mover normal
        return moverFichaEnTablero(ficha, jugadorId, pasos, tablero, resultado);
    }

    /**
     * Permite usar movimientos bonus acumulados para mover cualquier ficha.
     * 
     * @param jugadorId ID del jugador
     * @param fichaId ID de la ficha a mover
     * @param pasos Cantidad de bonus a consumir
     * @return Resultado del movimiento
     */
    public synchronized ResultadoMovimiento usarBonus(int jugadorId, int fichaId, int pasos) {
        int disponible = bonusMoves.getOrDefault(jugadorId, 0);
        
        if (disponible <= 0) {
            throw new MovimientoInvalidoException("No tienes movimientos bonus disponibles");
        }
        
        if (pasos > disponible) {
            throw new MovimientoInvalidoException(
                "No tienes suficientes bonus. Disponible: " + disponible + ", solicitado: " + pasos
            );
        }
        
        // Consumir bonus
        bonusMoves.put(jugadorId, disponible - pasos);
        
        // Realizar movimiento (sin avanzar turno)
        ResultadoMovimiento resultado = moverFichaSinValidarTurno(jugadorId, fichaId, pasos);
        resultado.bonusConsumido = pasos;
        resultado.bonusRestante = bonusMoves.get(jugadorId);
        
        return resultado;
    }

    // ============================
    // MÉTODOS DE MOVIMIENTO INTERNOS
    // ============================

    private ResultadoMovimiento sacarFichaDeCasa(Ficha ficha, int jugadorId, int pasos, 
                                                  ResultadoMovimiento resultado) {
        if (!puedeSacarConPasos(pasos)) {
            throw new MovimientoInvalidoException(
                "No puedes sacar ficha con " + pasos + ". Necesitas un 5."
            );
        }

        Tablero tablero = partida.getTablero();
        Casilla salida = tablero.getCasillaSalidaParaJugador(jugadorId);
        
        // Validar que la salida no esté bloqueada por rival
        if (salidaBloqueadaPorRival(salida, jugadorId)) {
            throw new MovimientoInvalidoException("La casilla de salida está bloqueada por un rival");
        }
        
        // Validar límite de 2 fichas en salida
        int fichasEnSalida = contarFichasPropiasEnCasilla(salida, jugadorId);
        if (fichasEnSalida >= 2) {
            throw new MovimientoInvalidoException("Ya tienes 2 fichas en la salida");
        }
        
        // Sacar ficha
        ficha.moverA(salida);
        ficha.setEstado(EstadoFicha.EN_TABLERO);
        resultado.movimientoExitoso = true;
        resultado.casillaSalida = -1; // casa
        resultado.casillaLlegada = salida.getIndice();
        
        // Verificar captura en salida
        if (!salida.isSegura()) {
            Ficha rival = buscarFichaRivalEnCasilla(salida, jugadorId);
            if (rival != null) {
                procesarCaptura(rival, jugadorId, resultado);
            }
        }
        
        return resultado;
    }

    private ResultadoMovimiento moverFichaEnTablero(Ficha ficha, int jugadorId, int pasos,
                                                     Tablero tablero, ResultadoMovimiento resultado) {
        Casilla origen = ficha.getCasillaActual();
        if (origen == null) {
            throw new MovimientoInvalidoException("Ficha sin casilla válida");
        }

        int indiceOrigen = origen.getIndice();
        int indiceDestino = tablero.calcularDestino(indiceOrigen, pasos, jugadorId);
        
        // Validar que no haya barreras en el camino
        if (hayBarreraEnRuta(indiceOrigen, indiceDestino, jugadorId)) {
            throw new MovimientoInvalidoException("Hay una barrera bloqueando el camino");
        }
        
        Casilla destino = tablero.getCasilla(indiceDestino);
        
        // Registrar posiciones
        resultado.casillaSalida = indiceOrigen;
        resultado.casillaLlegada = indiceDestino;
        
        // Mover ficha
        ficha.moverA(destino);
        resultado.movimientoExitoso = true;
        
        // Verificar captura (solo si no es casilla segura)
        if (!destino.isSegura()) {
            Ficha rival = buscarFichaRivalEnCasilla(destino, jugadorId);
            if (rival != null) {
                procesarCaptura(rival, jugadorId, resultado);
            }
        }
        
        // Verificar llegada a meta
        if (tablero.esMeta(destino, jugadorId)) {
            procesarLlegadaMeta(jugadorId, ficha, resultado);
        }
        
        return resultado;
    }

    private ResultadoMovimiento moverFichaSinValidarTurno(int jugadorId, int fichaId, int pasos) {
        Ficha ficha = partida.getFicha(jugadorId, fichaId);
        if (ficha == null) {
            throw new FichaNoEncontradaException("Ficha no encontrada: " + fichaId);
        }
        
        if (ficha.estaEnCasa()) {
            throw new MovimientoInvalidoException("No puedes mover una ficha que está en casa con bonus");
        }
        
        Tablero tablero = partida.getTablero();
        ResultadoMovimiento resultado = new ResultadoMovimiento();
        
        return moverFichaEnTablero(ficha, jugadorId, pasos, tablero, resultado);
    }

    // ============================
    // PROCESAMIENTO DE EVENTOS
    // ============================

    private void procesarCaptura(Ficha fichaCapturada, int jugadorCapturador, 
                                  ResultadoMovimiento resultado) {
        // Enviar ficha capturada a casa
        int jugadorCapturado = fichaCapturada.getIdJugador();
        enviarFichaACasa(fichaCapturada);
        
        // Otorgar 20 casillas de bonus al capturador
        int bonusActual = bonusMoves.getOrDefault(jugadorCapturador, 0);
        bonusMoves.put(jugadorCapturador, bonusActual + 20);
        
        // Registrar en resultado
        resultado.capturaRealizada = true;
        resultado.fichaCapturadaId = fichaCapturada.getId();
        resultado.jugadorCapturadoId = jugadorCapturado;
        resultado.bonusGanado = 20;
        resultado.bonusTotal = bonusActual + 20;
    }

    private void procesarLlegadaMeta(int jugadorId, Ficha ficha, ResultadoMovimiento resultado) {
        Jugador jugador = partida.getJugadorPorId(jugadorId);
        if (jugador != null) {
            int puntosActuales = jugador.getPuntos();
            jugador.setPuntos(puntosActuales + 10);
            
            resultado.llegadaMeta = true;
            resultado.bonusPuntosMeta = 10;
            resultado.puntosTotal = puntosActuales + 10;
        }
        
        ficha.setEstado(EstadoFicha.EN_META);
    }

    // ============================
    // MANEJO DE BLOQUEOS
    // ============================

    private boolean tieneBloqueoPropio(int jugadorId) {
        Tablero tablero = partida.getTablero();
        Jugador jugador = partida.getJugadorPorId(jugadorId);
        
        if (jugador == null) return false;
        
        // Buscar si alguna casilla tiene 2+ fichas propias
        Map<Integer, List<Ficha>> fichasPorCasilla = new HashMap<>();
        
        for (Ficha ficha : jugador.getFichas()) {
            if (!ficha.estaEnCasa() && ficha.getCasillaActual() != null) {
                int idx = ficha.getCasillaActual().getIndice();
                fichasPorCasilla.computeIfAbsent(idx, k -> new java.util.ArrayList<>()).add(ficha);
            }
        }
        
        return fichasPorCasilla.values().stream().anyMatch(lista -> lista.size() >= 2);
    }

    private boolean romperBloqueoPropioSiExiste(int jugadorId) {
        try {
            Tablero tablero = partida.getTablero();
            return tablero.romperBloqueoPropio(jugadorId);
        } catch (UnsupportedOperationException e) {
            // Si Tablero no implementa este método aún, no hacer nada
            return false;
        }
    }

    private boolean hayBarreraEnRuta(int origenIdx, int destinoIdx, int jugadorId) {
        Tablero tablero = partida.getTablero();
        return tablero.rutaContieneBarrera(origenIdx, destinoIdx);
    }

    private boolean salidaBloqueadaPorRival(Casilla salida, int jugadorId) {
        // Verificar si hay un bloqueo rival en la casilla de salida
        List<Ficha> fichasEnSalida = obtenerFichasEnCasilla(salida);
        
        // Contar fichas por jugador
        Map<Integer, Long> fichasPorJugador = fichasEnSalida.stream()
            .collect(Collectors.groupingBy(Ficha::getIdJugador, Collectors.counting()));
        
        // Hay bloqueo rival si algún otro jugador tiene 2+ fichas
        return fichasPorJugador.entrySet().stream()
            .anyMatch(entry -> entry.getKey() != jugadorId && entry.getValue() >= 2);
    }

    // ============================
    // BÚSQUEDA Y VALIDACIÓN
    // ============================

    private Ficha buscarFichaRivalEnCasilla(Casilla casilla, int jugadorId) {
        if (casilla == null) return null;
        
        Tablero tablero = partida.getTablero();
        return tablero.buscarFichaRivalEnCasilla(casilla.getIndice(), jugadorId);
    }

    private int contarFichasPropiasEnCasilla(Casilla casilla, int jugadorId) {
        List<Ficha> fichas = obtenerFichasEnCasilla(casilla);
        return (int) fichas.stream()
            .filter(f -> f.getIdJugador() == jugadorId)
            .count();
    }

    private List<Ficha> obtenerFichasEnCasilla(Casilla casilla) {
        if (casilla == null) return java.util.Collections.emptyList();
        
        return partida.getJugadores().stream()
            .flatMap(j -> j.getFichas().stream())
            .filter(f -> f.getCasillaActual() != null && 
                        f.getCasillaActual().getIndice() == casilla.getIndice())
            .collect(Collectors.toList());
    }

    private void enviarFichaACasa(Ficha ficha) {
        ficha.setCasillaActual(null);
        ficha.setEstado(EstadoFicha.EN_CASA);
    }

    private boolean puedeSacarConPasos(int pasos) {
        return pasos == 5; // Regla: solo se saca con 5
    }

    private void validarTurno(int jugadorId) {
        Jugador actual = partida.getJugadorActual();
        if (actual == null || actual.getId() != jugadorId) {
            throw new NoEsTuTurnoException(
                "No es tu turno. Turno actual: " + (actual != null ? actual.getId() : "ninguno")
            );
        }
    }

    // ============================
    // PENALIZACIONES
    // ============================

    private void perderFichaPorTresDobles(int jugadorId) {
        Jugador jugador = partida.getJugadorPorId(jugadorId);
        if (jugador == null) return;
        
        // Buscar una ficha en tablero para enviarla a casa
        Ficha fichaAPenalizar = jugador.getFichas().stream()
            .filter(f -> !f.estaEnCasa())
            .findFirst()
            .orElse(null);
        
        if (fichaAPenalizar != null) {
            enviarFichaACasa(fichaAPenalizar);
        } else {
            // Si no hay fichas en tablero, penalizar con puntos
            int puntosActuales = jugador.getPuntos();
            jugador.setPuntos(Math.max(0, puntosActuales - 5));
        }
    }

    // ============================
    // CONSULTAS
    // ============================

    public synchronized int getBonusDisponible(int jugadorId) {
        return bonusMoves.getOrDefault(jugadorId, 0);
    }

    public synchronized int getContadorDobles(int jugadorId) {
        return contadorDobles.getOrDefault(jugadorId, 0);
    }

    public synchronized void resetearContadorDobles(int jugadorId) {
        contadorDobles.put(jugadorId, 0);
    }

    public synchronized EstadoPartida obtenerEstado() {
        // Delegar a Partida.snapshot() si existe
        // return partida.snapshot();
        throw new UnsupportedOperationException(
            "Implementar Partida.snapshot() para obtener estado completo"
        );
    }

    // ============================
    // CLASES DE RESULTADO
    // ============================

    /**
     * Resultado de tirar los dados
     */
    public static class ResultadoDados {
        public final int dado1;
        public final int dado2;
        public final boolean esDoble;
        public final boolean bloqueoRoto;
        public final boolean fichaPerdida;
        public final int contadorDobles;
        
        public ResultadoDados(int dado1, int dado2, boolean esDoble, boolean bloqueoRoto) {
            this(dado1, dado2, esDoble, bloqueoRoto, false, 0);
        }
        
        public ResultadoDados(int dado1, int dado2, boolean esDoble, boolean bloqueoRoto,
                            boolean fichaPerdida, int contadorDobles) {
            this.dado1 = dado1;
            this.dado2 = dado2;
            this.esDoble = esDoble;
            this.bloqueoRoto = bloqueoRoto;
            this.fichaPerdida = fichaPerdida;
            this.contadorDobles = contadorDobles;
        }
        
        public int getSuma() {
            return dado1 + dado2;
        }
        
        @Override
        public String toString() {
            return String.format("Dados[%d,%d] doble=%s bloqueoRoto=%s fichaPerdida=%s contador=%d",
                dado1, dado2, esDoble, bloqueoRoto, fichaPerdida, contadorDobles);
        }
    }

    /**
     * Resultado de un movimiento de ficha
     */
    public static class ResultadoMovimiento {
        public boolean movimientoExitoso = false;
        public int casillaSalida = -1;
        public int casillaLlegada = -1;
        
        // Captura
        public boolean capturaRealizada = false;
        public int fichaCapturadaId = -1;
        public int jugadorCapturadoId = -1;
        public int bonusGanado = 0;
        public int bonusTotal = 0;
        
        // Meta
        public boolean llegadaMeta = false;
        public int bonusPuntosMeta = 0;
        public int puntosTotal = 0;
        
        // Uso de bonus
        public int bonusConsumido = 0;
        public int bonusRestante = 0;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Movimiento[");
            sb.append(casillaSalida).append("->").append(casillaLlegada);
            if (capturaRealizada) {
                sb.append(" CAPTURA (ficha:").append(fichaCapturadaId)
                  .append(" bonus:+").append(bonusGanado).append(")");
            }
            if (llegadaMeta) {
                sb.append(" META (+").append(bonusPuntosMeta).append(" pts)");
            }
            if (bonusConsumido > 0) {
                sb.append(" bonus usado:").append(bonusConsumido);
            }
            sb.append("]");
            return sb.toString();
        }
    }
}