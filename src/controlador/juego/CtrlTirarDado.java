package controlador.juego;

import controlador.servidor.ClienteHandler;
import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.partida.MotorJuego;
import modelo.servicios.PersistenciaServicio;
import modelo.servicios.GestorMotores;
import com.google.gson.JsonObject;
import java.util.Optional;
import vista.VistaServidor;
import modelo.Ficha.Ficha;
import modelo.Tablero.Casilla;
import modelo.Tablero.Tablero;

/**
 * ✅ CORREGIDO: Manejo de dados independientes
 * - Si sale UN dado 5: Saca ficha y deja el otro dado disponible
 * - Si sale suma=5 (sin ningún 5): Saca ficha y consume ambos dados
 * - Si sale doble 5: Saca DOS fichas (si hay espacio) y permite volver a tirar
 */
public class CtrlTirarDado {
    
    private final PersistenciaServicio persistencia;
    private final CtrlMoverFicha ctrlMoverFicha;
    
    public CtrlTirarDado() {
        this.persistencia = PersistenciaServicio.getInstancia();
        this.ctrlMoverFicha = new CtrlMoverFicha();
    }
    
    public String ejecutar(ClienteHandler cliente, JsonObject datos) {
        try {
            // Validar jugador
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            // Obtener partida
            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (!partidaOpt.isPresent()) {
                return crearError("No estas en ninguna partida");
            }
            
            Partida partida = partidaOpt.get();
            
            // Validar estado
            if (partida.getEstado() != EstadoPartida.EN_PROGRESO) {
                return crearError("La partida no esta en progreso");
            }
            
            // Validar turno
            if (!partida.esTurnoDeJugador(jugador.getId())) {
                return crearError("No es tu turno");
            }
            
            // Obtener motor
            MotorJuego motor = obtenerMotorJuego(partida);
            
            // Tirar dados
            MotorJuego.ResultadoDados resultado = motor.tirarDados(jugador.getId());
            
            VistaServidor.mostrarTiradaDados(
                jugador,
                resultado.dado1,
                resultado.dado2,
                resultado.esDoble
            );
            
            // Notificar penalización por 3 dobles
            if (resultado.fichaPerdida) {
                System.out.println("[PENALIZACION] " + jugador.getNombre() + 
                                 " sacó 3 dobles consecutivos!");
                notificarPenalizacionTresDobles(partida, jugador, cliente);
            }
            
            // Notificar resultado a todos
            notificarResultadoDados(partida, jugador, resultado, cliente);
            
            // ========================================
            // ⭐ LÓGICA CRÍTICA: Manejo según tipo de dados
            // ========================================
            boolean puedeJugar = true;
            int dadoDisponible = 0;
            
            if (resultado.esDoble) {
                // ✅ SI ES DOBLE: Manejar según si es doble 5 o no
                if (resultado.dado1 == 5 && resultado.dado2 == 5) {
                    // DOBLE 5: Intentar sacar DOS fichas
                    puedeJugar = intentarSacarDosFichasConDoble5(partida, jugador, motor, cliente);
                } else {
                    // DOBLE normal: NO hacer nada automático
                    System.out.println("[DOBLE] " + jugador.getNombre() + " puede volver a tirar.");
                    puedeJugar = true;
                }
                
            } else {
                // ✅ SI NO ES DOBLE: Aplicar lógica automática
                ResultadoAutomatico resAuto = manejarTurnoAutomatico(
                    partida, 
                    jugador, 
                    motor, 
                    resultado.dado1, 
                    resultado.dado2,
                    cliente
                );
                puedeJugar = resAuto.puedeJugar;
                dadoDisponible = resAuto.dadoDisponible;
            }
            
            // Crear respuesta
            JsonObject respuesta = crearRespuestaResultado(resultado);
            respuesta.addProperty("puedeJugar", puedeJugar);
            
            // ✅ Si es doble, indicar que debe volver a tirar
            if (resultado.esDoble) {
                respuesta.addProperty("debeVolverATirar", true);
                respuesta.addProperty("mensaje", "¡Doble! Vuelve a tirar dados");
            }
            
            // ✅ Si hay un dado disponible, indicarlo
            if (dadoDisponible > 0) {
                respuesta.addProperty("dadoDisponible", dadoDisponible);
                respuesta.addProperty("mensaje", "Tienes un dado " + dadoDisponible + " disponible para mover");
            }
            
            return respuesta.toString();
            
        } catch (MotorJuego.NoEsTuTurnoException e) {
            return crearError("No es tu turno");
        } catch (Exception e) {
            System.err.println("Error tirando dados: " + e.getMessage());
            e.printStackTrace();
            return crearError("Error interno: " + e.getMessage());
        }
    }
    
    /**
     * ✅ NUEVO: Clase para resultado de manejo automático
     */
    private static class ResultadoAutomatico {
        boolean puedeJugar;
        int dadoDisponible; // Si queda un dado disponible después de sacar
        
        ResultadoAutomatico(boolean puedeJugar, int dadoDisponible) {
            this.puedeJugar = puedeJugar;
            this.dadoDisponible = dadoDisponible;
        }
    }
    
    /**
     * ✅ CORREGIDO: Maneja automáticamente el turno según los dados
     * 
     * REGLAS:
     * 1. Si tiene fichas en casa Y sale 5 → Saca automáticamente
     *    - Si es UN dado 5: Saca y deja el OTRO dado disponible
     *    - Si es suma=5: Saca y consume AMBOS dados (pasa turno)
     * 2. Si NO puede sacar Y tiene fichas en tablero → Jugador elige
     * 3. Si NO puede hacer nada → Pasa turno automáticamente
     * 
     * @return ResultadoAutomatico con puedeJugar y dadoDisponible
     */
    private ResultadoAutomatico manejarTurnoAutomatico(Partida partida, Jugador jugador, MotorJuego motor,
                                                        int dado1, int dado2, ClienteHandler cliente) {
        
        // Verificar regla del 5
        boolean dado1Es5 = (dado1 == 5);
        boolean dado2Es5 = (dado2 == 5);
        boolean sumaEs5 = (dado1 + dado2 == 5);
        boolean puedesSacar = dado1Es5 || dado2Es5 || sumaEs5;
        
        // Verificar si tiene fichas en casa
        boolean tieneFichasEnCasa = jugador.getFichas().stream()
            .anyMatch(f -> f.estaEnCasa());
        
        // Verificar si tiene fichas en tablero
        boolean tieneFichasEnTablero = jugador.getFichas().stream()
            .anyMatch(f -> !f.estaEnCasa() && !f.estaEnMeta());
        
        // CASO 1: Tiene fichas en casa Y puede sacar con 5 → Sacar automáticamente
        if (tieneFichasEnCasa && puedesSacar) {
            return intentarSacarFichaAutomatica(partida, jugador, motor, dado1, dado2, cliente);
        }
        
        // CASO 2: Tiene fichas en tablero → Dejar que el jugador elija
        if (tieneFichasEnTablero) {
            return new ResultadoAutomatico(true, 0); // Puede jugar, sin dado disponible especial
        }
        
        // CASO 3: No puede hacer nada → Pasar turno automáticamente
        System.out.println("[AUTO] " + jugador.getNombre() + " no puede jugar. Pasando turno...");
        pasarTurnoAutomaticamente(partida, jugador, cliente);
        return new ResultadoAutomatico(false, 0);
    }
    
    /**
     * ✅ NUEVO: Intenta sacar DOS fichas cuando sale doble 5
     */
    private boolean intentarSacarDosFichasConDoble5(Partida partida, Jugador jugador, MotorJuego motor,
                                                     ClienteHandler cliente) {
        try {
            // Buscar fichas en casa
            Ficha ficha1 = jugador.getFichas().stream()
                .filter(f -> f.estaEnCasa())
                .findFirst()
                .orElse(null);
            
            if (ficha1 == null) {
                // No tiene fichas en casa, dejar que el jugador juegue normal
                return true;
            }
            
            // Sacar primera ficha
            System.out.println("[AUTO DOBLE-5] Sacando primera ficha #" + ficha1.getId() + " de " + jugador.getNombre());
            
            MotorJuego.ResultadoSacar resultado1 = motor.sacarFichaDeCasa(
                jugador.getId(),
                ficha1.getId(),
                5,
                5
            );
            
            notificarSacarFicha(partida, jugador, resultado1, cliente);
            
            // Buscar segunda ficha en casa
            Ficha ficha2 = jugador.getFichas().stream()
                .filter(f -> f.estaEnCasa())
                .findFirst()
                .orElse(null);
            
            if (ficha2 == null) {
                System.out.println("[AUTO DOBLE-5] No hay más fichas en casa para sacar");
                // Ya se usaron ambos 5s, pero es doble así que puede volver a tirar
                return false; // Volver al menú para tirar de nuevo
            }
            
            // Verificar si hay espacio en el inicio
            Tablero tablero = partida.getTablero();
            Casilla salida = tablero.getCasillaSalidaParaJugador(jugador.getId());
            int fichasEnSalida = (int) jugador.getFichas().stream()
                .filter(f -> !f.estaEnCasa() && f.getCasillaActual() != null && 
                            f.getCasillaActual().getIndice() == salida.getIndice())
                .count();
            
            if (fichasEnSalida >= 2) {
                System.out.println("[AUTO DOBLE-5] Ya hay 2 fichas en el inicio, no se puede sacar la segunda");
                return false; // Volver al menú para tirar de nuevo
            }
            
            // Sacar segunda ficha
            System.out.println("[AUTO DOBLE-5] Sacando segunda ficha #" + ficha2.getId() + " de " + jugador.getNombre());
            
            MotorJuego.ResultadoSacar resultado2 = motor.sacarFichaDeCasa(
                jugador.getId(),
                ficha2.getId(),
                5,
                0 // Solo usar el segundo 5
            );
            
            notificarSacarFicha(partida, jugador, resultado2, cliente);
            
            // Ya se usaron ambos 5s, pero es doble así que puede volver a tirar
            return false; // Volver al menú para tirar de nuevo
            
        } catch (Exception e) {
            System.err.println("[AUTO DOBLE-5] Error: " + e.getMessage());
            return true; // Dejar que el jugador juegue normal
        }
    }
    
    /**
     * ✅ CORREGIDO: Intenta sacar automáticamente según regla del 5
     * 
     * - Si un dado es 5: Saca y deja el OTRO dado disponible
     * - Si suma es 5: Saca y consume AMBOS dados (pasa turno)
     */
    private ResultadoAutomatico intentarSacarFichaAutomatica(Partida partida, Jugador jugador, MotorJuego motor,
                                                              int dado1, int dado2, ClienteHandler cliente) {
        try {
            // Buscar la primera ficha en casa
            Ficha fichaEnCasa = jugador.getFichas().stream()
                .filter(f -> f.estaEnCasa())
                .findFirst()
                .orElse(null);
            
            if (fichaEnCasa == null) {
                // No hay fichas en casa, dejar que el jugador elija
                return new ResultadoAutomatico(true, 0);
            }
            
            int fichaId = fichaEnCasa.getId();
            
            System.out.println("[AUTO] Sacando automáticamente ficha #" + fichaId + " de " + jugador.getNombre());
            
            // ✅ SACAR usando el nuevo método que devuelve qué dados se usaron
            MotorJuego.ResultadoSacar resultado = motor.sacarFichaDeCasa(
                jugador.getId(),
                fichaId,
                dado1,
                dado2
            );
            
            // Mostrar en consola servidor
            VistaServidor.mostrarMovimientoFicha(
                jugador,
                fichaId,
                -1,
                resultado.casillaLlegada
            );
            
            // Notificar a todos
            notificarSacarFicha(partida, jugador, resultado, cliente);
            
            // ✅ Si hubo captura, notificar
            if (resultado.capturaRealizada) {
                Jugador capturado = partida.getJugadorPorId(resultado.jugadorCapturadoId);
                VistaServidor.mostrarCaptura(
                    jugador, 
                    capturado, 
                    resultado.fichaCapturadaId, 
                    resultado.bonusGanado
                );
                
                notificarCaptura(partida, jugador, resultado.fichaCapturadaId, 
                               resultado.jugadorCapturadoId, resultado.bonusGanado, cliente);
            }
            
            // ✅ DECISIÓN: ¿Qué hacer después de sacar?
            if (resultado.hayDadoDisponible()) {
                // Hay un dado disponible, el jugador puede elegir qué hacer
                int dadoDisp = resultado.getDadoDisponible();
                System.out.println("[AUTO] Ficha sacada. Dado " + dadoDisp + " disponible para mover.");
                return new ResultadoAutomatico(true, dadoDisp);
                
            } else {
                // Se usaron ambos dados, pasar turno automáticamente
                System.out.println("[AUTO] Ficha sacada con ambos dados. Pasando turno...");
                
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                partida.avanzarTurno();
                
                Jugador siguienteJugador = partida.getJugadorActual();
                if (siguienteJugador != null) {
                    notificarCambioTurno(partida, siguienteJugador, cliente);
                }
                
                return new ResultadoAutomatico(false, 0);
            }
            
        } catch (MotorJuego.MovimientoInvalidoException e) {
            // Si no puede sacar (ej: salida bloqueada), dejar que intente mover otra ficha
            System.out.println("[AUTO] No se pudo sacar automáticamente: " + e.getMessage());
            return new ResultadoAutomatico(true, 0);
        } catch (Exception e) {
            System.err.println("[AUTO] Error al sacar ficha: " + e.getMessage());
            e.printStackTrace();
            return new ResultadoAutomatico(true, 0);
        }
    }
    
    /**
     * ✅ Pasa el turno automáticamente
     */
    private void pasarTurnoAutomaticamente(Partida partida, Jugador jugador, ClienteHandler cliente) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        partida.avanzarTurno();
        
        Jugador siguienteJugador = partida.getJugadorActual();
        if (siguienteJugador != null) {
            // Notificar cambio de turno a todos PRIMERO
            JsonObject cambioTurno = new JsonObject();
            cambioTurno.addProperty("tipo", "cambio_turno");
            cambioTurno.addProperty("jugadorId", siguienteJugador.getId());
            cambioTurno.addProperty("jugadorNombre", siguienteJugador.getNombre());
            cambioTurno.addProperty("mensaje", jugador.getNombre() + " no pudo jugar. Turno pasado.");
            
            cliente.getServidor().broadcastAPartida(
                partida.getId(),
                cambioTurno.toString(),
                null
            );
            
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Notificar al siguiente jugador
            JsonObject tuTurno = new JsonObject();
            tuTurno.addProperty("tipo", "tu_turno");
            tuTurno.addProperty("jugadorId", siguienteJugador.getId());
            tuTurno.addProperty("jugadorNombre", siguienteJugador.getNombre());
            
            ClienteHandler handlerTurno = cliente.getServidor().getCliente(siguienteJugador.getSessionId());
            if (handlerTurno != null) {
                handlerTurno.enviarMensaje(tuTurno.toString());
                System.out.println("[TURNO AUTO] Notificado a " + siguienteJugador.getNombre() + " que es su turno");
            }
        }
    }
    
    private void notificarResultadoDados(Partida partida, Jugador jugador,
                                         MotorJuego.ResultadoDados resultado,
                                         ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "jugador_tiro_dados");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("jugadorNombre", jugador.getNombre());
        notificacion.addProperty("dado1", resultado.dado1);
        notificacion.addProperty("dado2", resultado.dado2);
        notificacion.addProperty("suma", resultado.getSuma());
        notificacion.addProperty("esDoble", resultado.esDoble);
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            notificacion.toString(),
            cliente.getSessionId()
        );
        
        // ✅ Usar el nuevo método que incluye tieneFichasEnJuego
        JsonObject respuestaLocal = crearRespuestaResultadoConJugador(resultado, jugador);
        cliente.enviarMensaje(respuestaLocal.toString());
    }
    
    /**
     * ✅ Notifica penalización por 3 dobles consecutivos
     */
    private void notificarPenalizacionTresDobles(Partida partida, Jugador jugador, 
                                                 ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "penalizacion_tres_dobles");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("jugadorNombre", jugador.getNombre());
        notificacion.addProperty("mensaje", jugador.getNombre() + 
                                           " sacó 3 dobles consecutivos! Ficha más adelantada regresa a casa");
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            notificacion.toString(),
            null
        );
    }
    
    /**
     * ✅ NUEVO: Notifica cuando se saca una ficha automáticamente
     */
    private void notificarSacarFicha(Partida partida, Jugador jugador, 
                                     MotorJuego.ResultadoSacar resultado,
                                     ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "ficha_movida");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("jugadorNombre", jugador.getNombre());
        notificacion.addProperty("fichaId", resultado.fichaId);
        notificacion.addProperty("desde", -1);
        notificacion.addProperty("hasta", resultado.casillaLlegada);
        notificacion.addProperty("automatico", true);
        
        // ✅ Indicar si hay dado disponible
        if (resultado.hayDadoDisponible()) {
            notificacion.addProperty("dadoDisponible", resultado.getDadoDisponible());
        }
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            notificacion.toString(),
            null
        );
        
        // Notificar estado del tablero
        if (partida.getTablero() != null) {
            JsonObject estadoTablero = partida.getTablero().generarEstadoJSON();
            JsonObject notifTablero = new JsonObject();
            notifTablero.addProperty("tipo", "estado_tablero");
            notifTablero.add("tablero", estadoTablero);
            
            cliente.getServidor().broadcastAPartida(
                partida.getId(),
                notifTablero.toString(),
                null
            );
        }
    }
    
    private void notificarCaptura(Partida partida, Jugador jugador, int fichaCapturadaId,
                                  int jugadorCapturadoId, int bonusGanado, ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "ficha_capturada");
        notificacion.addProperty("capturadorId", jugador.getId());
        notificacion.addProperty("capturadorNombre", jugador.getNombre());
        notificacion.addProperty("fichaCapturadaId", fichaCapturadaId);
        notificacion.addProperty("jugadorCapturadoId", jugadorCapturadoId);
        notificacion.addProperty("bonusGanado", bonusGanado);
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            notificacion.toString(),
            null
        );
    }
    
    private void notificarCambioTurno(Partida partida, Jugador jugadorTurno, ClienteHandler cliente) {
        // Notificar a todos sobre el cambio de turno PRIMERO
        JsonObject cambioTurno = new JsonObject();
        cambioTurno.addProperty("tipo", "cambio_turno");
        cambioTurno.addProperty("jugadorId", jugadorTurno.getId());
        cambioTurno.addProperty("jugadorNombre", jugadorTurno.getNombre());
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            cambioTurno.toString(),
            jugadorTurno.getSessionId()
        );
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Notificar específicamente al jugador que le toca
        JsonObject tuTurno = new JsonObject();
        tuTurno.addProperty("tipo", "tu_turno");
        tuTurno.addProperty("jugadorId", jugadorTurno.getId());
        tuTurno.addProperty("jugadorNombre", jugadorTurno.getNombre());
        
        ClienteHandler handlerTurno = cliente.getServidor().getCliente(jugadorTurno.getSessionId());
        if (handlerTurno != null) {
            handlerTurno.enviarMensaje(tuTurno.toString());
            System.out.println("[TURNO] Notificado a " + jugadorTurno.getNombre() + " que es su turno");
        }
    }
    
    private JsonObject crearRespuestaResultado(MotorJuego.ResultadoDados resultado) {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "resultado_dados");
        respuesta.addProperty("exito", true);
        
        JsonObject dados = new JsonObject();
        dados.addProperty("dado1", resultado.dado1);
        dados.addProperty("dado2", resultado.dado2);
        dados.addProperty("suma", resultado.getSuma());
        dados.addProperty("esDoble", resultado.esDoble);
        dados.addProperty("contadorDobles", resultado.contadorDobles);
        
        respuesta.add("dados", dados);
        
        if (resultado.bloqueoRoto) {
            respuesta.addProperty("bloqueoRoto", true);
            respuesta.addProperty("mensaje", "¡Doble! Se rompio tu bloqueo");
        } else if (resultado.fichaPerdida) {
            respuesta.addProperty("fichaPerdida", true);
            respuesta.addProperty("mensaje", "3 dobles consecutivos! Perdiste una ficha");
        } else if (resultado.esDoble) {
            respuesta.addProperty("mensaje", "¡Doble! Puedes volver a tirar");
        } else {
            respuesta.addProperty("mensaje", "Dados lanzados");
        }
        
        return respuesta;
    }
    
    /**
     * ✅ NUEVO: Versión mejorada que incluye información sobre fichas en juego
     */
    private JsonObject crearRespuestaResultadoConJugador(MotorJuego.ResultadoDados resultado, Jugador jugador) {
        JsonObject respuesta = crearRespuestaResultado(resultado);
        
        // ✅ Agregar si tiene fichas en juego
        boolean tieneFichasEnJuego = jugador.getFichas().stream()
            .anyMatch(f -> !f.estaEnCasa() && !f.estaEnMeta());
        
        respuesta.addProperty("tieneFichasEnJuego", tieneFichasEnJuego);
        
        return respuesta;
    }
    
    private MotorJuego obtenerMotorJuego(Partida partida) {
        GestorMotores gestorMotores = GestorMotores.getInstancia();
        return gestorMotores.obtenerMotor(partida);
    }
    
    private String crearError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo", "error");
        error.addProperty("exito", false);
        error.addProperty("mensaje", mensaje);
        return error.toString();
    }
}