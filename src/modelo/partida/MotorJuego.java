package modelo.partida;

import java.util.Arrays;
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
 * MotorJuego - Motor de reglas del Parchis.
 * ✅ CORREGIDO: 
 * - Dados independientes
 * - Regla del 5 mejorada (si sale un 5, el otro dado queda disponible)
 * - Barreras corregidas (casillas de INICIO son independientes)
 */
public class MotorJuego {

    private final Partida partida;
    private final Dado dado1;
    private final Dado dado2;
    private final Map<Integer, Integer> contadorDobles = new HashMap<>();
    private final Map<Integer, Integer> bonusMoves = new HashMap<>();


    public MotorJuego(Partida partida) {
        this(partida, new Dado(), new Dado());
    }

    public MotorJuego(Partida partida, Dado dado1, Dado dado2) {
        this.partida = Objects.requireNonNull(partida, "Partida no puede ser null");
        this.dado1 = Objects.requireNonNull(dado1, "Dado1 no puede ser null");
        this.dado2 = Objects.requireNonNull(dado2, "Dado2 no puede ser null");
    }


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


    public synchronized ResultadoDados tirarDados(int jugadorId) {
        validarTurno(jugadorId);
        
        int v1 = dado1.tirar();
        int v2 = dado2.tirar();
        boolean esDoble = (v1 == v2);
        
        if (!esDoble) {
            contadorDobles.put(jugadorId, 0);
            return new ResultadoDados(v1, v2, false, false);
        }
        
        int cont = contadorDobles.getOrDefault(jugadorId, 0) + 1;
        contadorDobles.put(jugadorId, cont);
        
        boolean bloqueoRoto = false;
        boolean fichaPerdida = false;
        
        if (tieneBloqueoPropio(jugadorId)) {
            bloqueoRoto = romperBloqueoPropioSiExiste(jugadorId);
        }
        
        if (cont >= 3) {
            perderFichaPorTresDobles(jugadorId);
            contadorDobles.put(jugadorId, 0);
            fichaPerdida = true;
        }
        
        return new ResultadoDados(v1, v2, esDoble, bloqueoRoto, fichaPerdida, cont);
    }

    /**
     * ✅ NUEVO: Mueve una ficha con UN SOLO dado
     * Esto permite usar dados independientemente
     * 
     * @param jugadorId ID del jugador
     * @param fichaId ID de la ficha a mover
     * @param valorDado Valor de UN solo dado (no la suma)
     * @return Resultado del movimiento
     */
    public synchronized ResultadoMovimiento moverFichaConUnDado(int jugadorId, int fichaId, int valorDado) {
        validarTurno(jugadorId);
        
        if (valorDado <= 0) {
            throw new MovimientoInvalidoException("El valor del dado debe ser positivo");
        }

        Ficha ficha = partida.getFicha(jugadorId, fichaId);
        if (ficha == null) {
            throw new FichaNoEncontradaException("Ficha no encontrada: " + fichaId);
        }

        // ✅ Si la ficha está en casa, no puede moverse con un solo dado
        // (debe usar la regla del 5)
        if (ficha.estaEnCasa()) {
            throw new MovimientoInvalidoException(
                "No puedes mover una ficha en casa. Debes sacarla primero con un 5."
            );
        }

        Tablero tablero = partida.getTablero();
        ResultadoMovimiento resultado = new ResultadoMovimiento();
        
        return moverFichaEnTablero(ficha, jugadorId, valorDado, tablero, resultado);
    }

    /**
     * ✅ CORREGIDO: Saca una ficha de casa según la regla del 5
     * 
     * REGLAS:
     * 1. Si dado1==5 O dado2==5 → Saca con ese dado, el OTRO queda disponible
     * 2. Si dado1+dado2==5 (sin ningún 5) → Saca con la suma, consume AMBOS dados
     * 
     * @param jugadorId ID del jugador
     * @param fichaId ID de la ficha a sacar
     * @param dado1 Valor del primer dado
     * @param dado2 Valor del segundo dado
     * @return Resultado que indica qué dados se usaron
     */
    public synchronized ResultadoSacar sacarFichaDeCasa(int jugadorId, int fichaId, int dado1, int dado2) {
        validarTurno(jugadorId);
        
        Ficha ficha = partida.getFicha(jugadorId, fichaId);
        if (ficha == null) {
            throw new FichaNoEncontradaException("Ficha no encontrada: " + fichaId);
        }
        
        if (!ficha.estaEnCasa()) {
            throw new MovimientoInvalidoException("La ficha no está en casa");
        }
        
        // ✅ VERIFICAR REGLA DEL 5
        boolean dado1Es5 = (dado1 == 5);
        boolean dado2Es5 = (dado2 == 5);
        boolean sumaEs5 = (dado1 + dado2 == 5);
        
        if (!dado1Es5 && !dado2Es5 && !sumaEs5) {
            throw new MovimientoInvalidoException(
                "No puedes sacar ficha. Necesitas un 5 en cualquier dado o que la suma sea 5."
            );
        }
        
        Tablero tablero = partida.getTablero();
        Casilla salida = tablero.getCasillaSalidaParaJugador(jugadorId);
        
        // Verificar bloqueos
        if (salidaBloqueadaPorRival(salida, jugadorId)) {
            throw new MovimientoInvalidoException("La casilla de salida está bloqueada por un rival");
        }
        
        int fichasEnSalida = contarFichasPropiasEnCasilla(salida, jugadorId);
        if (fichasEnSalida >= 2) {
            throw new MovimientoInvalidoException("Ya tienes 2 fichas en la salida");
        }
        
        // ✅ SACAR LA FICHA
        ficha.moverA(salida);
        ficha.setEstado(EstadoFicha.EN_TABLERO);
        
        ResultadoSacar resultado = new ResultadoSacar();
        resultado.fichaId = fichaId;
        resultado.casillaLlegada = salida.getIndice();
        resultado.movimientoExitoso = true;
        
        // ✅ DETERMINAR QUÉ DADOS SE USARON
        if (dado1Es5 && dado2Es5) {
            // Ambos son 5 (es doble) - se usa solo uno para sacar
            resultado.dado1Usado = true;
            resultado.dado2Disponible = dado2;
            resultado.mensajeExtra = "Sacaste con el primer 5. Tienes un 5 disponible para mover.";
            
        } else if (dado1Es5) {
            // Solo dado1 es 5 - el dado2 queda disponible
            resultado.dado1Usado = true;
            resultado.dado2Disponible = dado2;
            resultado.mensajeExtra = "Sacaste con el 5. Tienes un " + dado2 + " disponible para mover.";
            
        } else if (dado2Es5) {
            // Solo dado2 es 5 - el dado1 queda disponible
            resultado.dado2Usado = true;
            resultado.dado1Disponible = dado1;
            resultado.mensajeExtra = "Sacaste con el 5. Tienes un " + dado1 + " disponible para mover.";
            
        } else if (sumaEs5) {
            // La suma es 5 (ej: 3+2, 4+1) - se consumen AMBOS dados
            resultado.dado1Usado = true;
            resultado.dado2Usado = true;
            resultado.mensajeExtra = "Sacaste con la suma de ambos dados (" + dado1 + "+" + dado2 + "=5).";
        }
        
        // Verificar captura al sacar
        if (!salida.isSegura()) {
            Ficha rival = buscarFichaRivalEnCasilla(salida, jugadorId);
            if (rival != null) {
                procesarCaptura(rival, jugadorId, resultado);
            }
        }
        
        return resultado;
    }

    /**
     * ✅ MANTENER: Método original para compatibilidad
     * Este se usa cuando el servidor maneja automáticamente el sacar con 5
     */
    public synchronized ResultadoMovimiento moverFicha(int jugadorId, int fichaId, int dado1, int dado2) {
        validarTurno(jugadorId);
        
        if (dado1 <= 0 || dado2 <= 0) {
            throw new MovimientoInvalidoException("Los dados deben ser positivos");
        }

        Ficha ficha = partida.getFicha(jugadorId, fichaId);
        if (ficha == null) {
            throw new FichaNoEncontradaException("Ficha no encontrada: " + fichaId);
        }

        Tablero tablero = partida.getTablero();
        ResultadoMovimiento resultado = new ResultadoMovimiento();

        // CASO 1: Ficha en casa -> sacar con regla del 5
        if (ficha.estaEnCasa()) {
            return sacarYMoverConAmbos(ficha, jugadorId, dado1, dado2, resultado, tablero);
        }

        // CASO 2: Ficha en tablero -> usar la suma
        int pasos = dado1 + dado2;
        return moverFichaEnTablero(ficha, jugadorId, pasos, tablero, resultado);
    }
    
    /**
     * ✅ HELPER: Saca y mueve con ambos dados (para auto-movimientos)
     */
    private ResultadoMovimiento sacarYMoverConAmbos(Ficha ficha, int jugadorId, int dado1, int dado2,
                                                     ResultadoMovimiento resultado, Tablero tablero) {
        boolean dado1Es5 = (dado1 == 5);
        boolean dado2Es5 = (dado2 == 5);
        boolean sumaEs5 = (dado1 + dado2 == 5);
        
        if (!dado1Es5 && !dado2Es5 && !sumaEs5) {
            throw new MovimientoInvalidoException(
                "No puedes sacar ficha. Necesitas un 5 en cualquier dado o que la suma sea 5."
            );
        }

        Casilla salida = tablero.getCasillaSalidaParaJugador(jugadorId);
        
        if (salidaBloqueadaPorRival(salida, jugadorId)) {
            throw new MovimientoInvalidoException("La casilla de salida está bloqueada por un rival");
        }
        
        int fichasEnSalida = contarFichasPropiasEnCasilla(salida, jugadorId);
        if (fichasEnSalida >= 2) {
            throw new MovimientoInvalidoException("Ya tienes 2 fichas en la salida");
        }
        
        ficha.moverA(salida);
        ficha.setEstado(EstadoFicha.EN_TABLERO);
        resultado.movimientoExitoso = true;
        resultado.casillaSalida = -1;
        resultado.casillaLlegada = salida.getIndice();
        
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
            throw new MovimientoInvalidoException("Ficha sin casilla valida");
        }

        int indiceOrigen = origen.getIndice();
        int indiceDestino = tablero.calcularDestino(indiceOrigen, pasos, jugadorId);
        
        // ✅ CORREGIDO: Verificar barreras correctamente
        if (hayBarreraEnRuta(indiceOrigen, indiceDestino, jugadorId, tablero)) {
            throw new MovimientoInvalidoException("Hay una barrera bloqueando el camino");
        }
        
        Casilla destino = tablero.getCasilla(indiceDestino);
        
        // ✅ CRÍTICO: Verificar que el destino existe
        if (destino == null) {
            throw new MovimientoInvalidoException(
                "Movimiento invalido: la casilla destino (" + indiceDestino + ") no existe. " +
                "Origen: " + indiceOrigen + ", Pasos: " + pasos
            );
        }
        
        resultado.casillaSalida = indiceOrigen;
        resultado.casillaLlegada = indiceDestino;
        
        ficha.moverA(destino);
        resultado.movimientoExitoso = true;
        
        if (!destino.isSegura()) {
            Ficha rival = buscarFichaRivalEnCasilla(destino, jugadorId);
            if (rival != null) {
                procesarCaptura(rival, jugadorId, resultado);
            }
        }
        
        if (tablero.esMeta(destino, jugadorId)) {
            procesarLlegadaMeta(jugadorId, ficha, resultado);
        }
        
        return resultado;
    }

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
        
        bonusMoves.put(jugadorId, disponible - pasos);
        
        ResultadoMovimiento resultado = moverFichaSinValidarTurno(jugadorId, fichaId, pasos);
        resultado.bonusConsumido = pasos;
        resultado.bonusRestante = bonusMoves.get(jugadorId);
        
        return resultado;
    }

    private void procesarCaptura(Ficha fichaCapturada, int jugadorCapturador, 
                                  ResultadoMovimiento resultado) {
        int jugadorCapturado = fichaCapturada.getIdJugador();
        enviarFichaACasa(fichaCapturada);
        
        int bonusActual = bonusMoves.getOrDefault(jugadorCapturador, 0);
        bonusMoves.put(jugadorCapturador, bonusActual + 20);
        
        resultado.capturaRealizada = true;
        resultado.fichaCapturadaId = fichaCapturada.getId();
        resultado.jugadorCapturadoId = jugadorCapturado;
        resultado.bonusGanado = 20;
        resultado.bonusTotal = bonusActual + 20;
    }
    
    /**
     * ✅ SOBRECARGA: Para ResultadoSacar también
     */
    private void procesarCaptura(Ficha fichaCapturada, int jugadorCapturador, 
                                  ResultadoSacar resultado) {
        int jugadorCapturado = fichaCapturada.getIdJugador();
        enviarFichaACasa(fichaCapturada);
        
        int bonusActual = bonusMoves.getOrDefault(jugadorCapturador, 0);
        bonusMoves.put(jugadorCapturador, bonusActual + 20);
        
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

  
    private boolean tieneBloqueoPropio(int jugadorId) {
        Tablero tablero = partida.getTablero();
        Jugador jugador = partida.getJugadorPorId(jugadorId);
        
        if (jugador == null) return false;
        
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
            return false;
        }
    }

    /**
     * ✅ CORREGIDO: Detección de barreras mejorada
     * - Las casillas de INICIO (1, 18, 35, 52) son INDEPENDIENTES entre sí
     * - La casilla de ORIGEN se EXCLUYE (puedes salir de tu propia barrera)
     * - Solo se verifica barreras EN EL CAMINO (excluyendo origen y destino)
     */
    private boolean hayBarreraEnRuta(int origenIdx, int destinoIdx, int jugadorId, Tablero tablero) {
        // ✅ Casillas de INICIO por color
        List<Integer> casillasInicio = Arrays.asList(1, 18, 35, 52);
        
        // Si AMBAS casillas son de INICIO pero DIFERENTES colores
        // NO puede haber barrera entre ellas (son independientes)
        if (casillasInicio.contains(origenIdx) && casillasInicio.contains(destinoIdx)) {
            if (origenIdx != destinoIdx) {
                // Diferentes casillas de inicio = independientes = sin barrera
                return false;
            }
        }
        
        // ✅ CRÍTICO: Verificar barreras EXCLUYENDO la casilla de origen
        // Si tienes 2 fichas en el origen, puedes salir (no estás bloqueado por tu propia barrera)
        return tablero.rutaContieneBarreraExcluyendoOrigen(origenIdx, destinoIdx);
    }

    private boolean salidaBloqueadaPorRival(Casilla salida, int jugadorId) {
        List<Ficha> fichasEnSalida = obtenerFichasEnCasilla(salida);
        
        Map<Integer, Long> fichasPorJugador = fichasEnSalida.stream()
            .collect(Collectors.groupingBy(Ficha::getIdJugador, Collectors.counting()));
        
        return fichasPorJugador.entrySet().stream()
            .anyMatch(entry -> entry.getKey() != jugadorId && entry.getValue() >= 2);
    }


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

    private void validarTurno(int jugadorId) {
        Jugador actual = partida.getJugadorActual();
        if (actual == null || actual.getId() != jugadorId) {
            throw new NoEsTuTurnoException(
                "No es tu turno. Turno actual: " + (actual != null ? actual.getId() : "ninguno")
            );
        }
    }

    public synchronized ResultadoMovimiento moverFichaSinValidarTurno(int jugadorId, int fichaId, int pasos) {
        Ficha ficha = partida.getFicha(jugadorId, fichaId);
        if (ficha == null) {
            throw new FichaNoEncontradaException("Ficha no encontrada: " + fichaId);
        }
        
        if (ficha.estaEnCasa()) {
            throw new MovimientoInvalidoException("No puedes mover una ficha que esta en casa con bonus");
        }
        
        Tablero tablero = partida.getTablero();
        ResultadoMovimiento resultado = new ResultadoMovimiento();
        
        return moverFichaEnTablero(ficha, jugadorId, pasos, tablero, resultado);
    }
  
    /**
     * ✅ Penalización por 3 dobles consecutivos
     */
    private void perderFichaPorTresDobles(int jugadorId) {
        Jugador jugador = partida.getJugadorPorId(jugadorId);
        if (jugador == null) return;
        
        Ficha fichaMasAdelantada = jugador.getFichas().stream()
            .filter(f -> !f.estaEnCasa() && !f.estaEnMeta())
            .max((f1, f2) -> {
                Casilla c1 = f1.getCasillaActual();
                Casilla c2 = f2.getCasillaActual();
                if (c1 == null) return -1;
                if (c2 == null) return 1;
                return Integer.compare(c1.getIndice(), c2.getIndice());
            })
            .orElse(null);
        
        if (fichaMasAdelantada != null) {
            System.out.println("[PENALIZACION] " + jugador.getNombre() + 
                             " sacó 3 dobles. Ficha #" + fichaMasAdelantada.getId() + 
                             " regresa a casa.");
            enviarFichaACasa(fichaMasAdelantada);
        } else {
            System.out.println("[PENALIZACION] " + jugador.getNombre() + 
                             " sacó 3 dobles pero no tiene fichas en tablero.");
        }
    }

   
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
          throw new UnsupportedOperationException(
            "Implementar Partida.snapshot() para obtener estado completo"
        );
    }


    // ========================================
    // CLASES DE RESULTADO
    // ========================================
    
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
     * ✅ NUEVO: Resultado específico para sacar fichas
     * Indica qué dados se usaron y cuáles quedan disponibles
     */
    public static class ResultadoSacar {
        public boolean movimientoExitoso = false;
        public int fichaId = -1;
        public int casillaLlegada = -1;
        
        // ✅ Control de dados usados
        public boolean dado1Usado = false;
        public boolean dado2Usado = false;
        public int dado1Disponible = -1; // -1 si no está disponible
        public int dado2Disponible = -1; // -1 si no está disponible
        public String mensajeExtra = "";
        
        // Captura (igual que ResultadoMovimiento)
        public boolean capturaRealizada = false;
        public int fichaCapturadaId = -1;
        public int jugadorCapturadoId = -1;
        public int bonusGanado = 0;
        public int bonusTotal = 0;
        
        /**
         * Verifica si hay un dado disponible para usar
         */
        public boolean hayDadoDisponible() {
            return dado1Disponible > 0 || dado2Disponible > 0;
        }
        
        /**
         * Obtiene el valor del dado disponible (o 0 si ninguno)
         */
        public int getDadoDisponible() {
            if (dado1Disponible > 0) return dado1Disponible;
            if (dado2Disponible > 0) return dado2Disponible;
            return 0;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Sacar[");
            sb.append("ficha:").append(fichaId);
            sb.append(" -> casilla:").append(casillaLlegada);
            if (dado1Usado && dado2Usado) {
                sb.append(" (ambos dados usados)");
            } else if (dado1Disponible > 0) {
                sb.append(" (dado ").append(dado1Disponible).append(" disponible)");
            } else if (dado2Disponible > 0) {
                sb.append(" (dado ").append(dado2Disponible).append(" disponible)");
            }
            if (capturaRealizada) {
                sb.append(" CAPTURA (+").append(bonusGanado).append(")");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static class ResultadoMovimiento {
        public boolean movimientoExitoso = false;
        public int casillaSalida = -1;
        public int casillaLlegada = -1;
        
        public boolean capturaRealizada = false;
        public int fichaCapturadaId = -1;
        public int jugadorCapturadoId = -1;
        public int bonusGanado = 0;
        public int bonusTotal = 0;
        
        public boolean llegadaMeta = false;
        public int bonusPuntosMeta = 0;
        public int puntosTotal = 0;
        
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