package controlador.juego;

import controlador.servidor.ClienteHandler;
import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.partida.MotorJuego;
import modelo.servicios.PersistenciaServicio;
import com.google.gson.JsonObject;
import java.util.Optional;
import modelo.servicios.GestorMotores;
import vista.VistaServidor;

/**
 * Controlador para manejar movimientos de fichas.
 */
public class CtrlMoverFicha {
    
    private final PersistenciaServicio persistencia;
    
    public CtrlMoverFicha() {
        this.persistencia = PersistenciaServicio.getInstancia();
    }
 
    /**
     * Ejecuta un movimiento normal de ficha.
     * 
     * @param cliente ClienteHandler del jugador
     * @param fichaId ID de la ficha a mover
     * @param pasos Numero de casillas a mover
     * @return Respuesta JSON
     */
    public String ejecutar(ClienteHandler cliente, int fichaId, int pasos) {
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
            
            // Ejecutar movimiento
            MotorJuego.ResultadoMovimiento resultado = motor.moverFicha(
                jugador.getId(), 
                fichaId, 
                pasos
            );
            
            VistaServidor.mostrarMovimientoFicha(
                jugador, 
                fichaId, 
                resultado.casillaSalida, 
                resultado.casillaLlegada
            );

            // Si hubo captura
            if (resultado.capturaRealizada) {
                Jugador capturado = partida.getJugadorPorId(resultado.jugadorCapturadoId);
                VistaServidor.mostrarCaptura(
                    jugador, 
                    capturado, 
                    resultado.fichaCapturadaId, 
                    resultado.bonusGanado
                );
            }

            // Si llego a meta
            if (resultado.llegadaMeta) {
                VistaServidor.mostrarLlegadaMeta(
                    jugador, 
                    fichaId, 
                    jugador.contarFichasEnMeta()
                );

                // Si gano
                if (jugador.haGanado()) {
                    VistaServidor.mostrarGanador(partida, jugador);
                }
            }
            
            // Notificar movimiento
            notificarMovimiento(partida, jugador, fichaId, resultado, cliente);
            
            // Si hubo captura, notificar
            if (resultado.capturaRealizada) {
                notificarCaptura(partida, jugador, resultado, cliente);
            }
            
            // Si llego a meta, notificar
            if (resultado.llegadaMeta) {
                notificarLlegadaMeta(partida, jugador, fichaId, cliente);
                
                // Verificar si gano
                if (jugador.haGanado()) {
                    notificarGanador(partida, jugador, cliente);
                    partida.setEstado(EstadoPartida.FINALIZADA);
                }
            }
            
            //  Avanzar turno solo si el movimiento fue exitoso
            partida.avanzarTurno();
            
            // Notificar estado del tablero a todos
            notificarEstadoTablero(partida, cliente);
            
            //  Notificar al siguiente jugador
            Jugador siguienteJugador = partida.getJugadorActual();
            if (siguienteJugador != null) {
                notificarCambioTurno(partida, siguienteJugador, cliente);
            }
            
            // Crear respuesta
            JsonObject respuesta = crearRespuestaMovimiento(resultado);
            
            return respuesta.toString();
            
        } catch (MotorJuego.MovimientoInvalidoException e) {
            //  No cambiar turno si el movimiento es invalido
            return crearError("Movimiento invalido: " + e.getMessage());
        } catch (MotorJuego.NoEsTuTurnoException e) {
            return crearError("No es tu turno");
        } catch (MotorJuego.JuegoException e) {
            return crearError("Error en el juego: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error moviendo ficha: " + e.getMessage());
            e.printStackTrace();
            return crearError("Error interno: " + e.getMessage());
        }
    }
    
    
    /**
     * Usa movimientos bonus acumulados.
     * 
     * @param cliente ClienteHandler del jugador
     * @param fichaId ID de la ficha a mover
     * @param pasos Cantidad de bonus a usar
     * @return Respuesta JSON
     */
    public String usarBonus(ClienteHandler cliente, int fichaId, int pasos) {
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
            
            // Obtener motor
            MotorJuego motor = obtenerMotorJuego(partida);
            
            // Verificar bonus disponible
            int bonusDisponible = motor.getBonusDisponible(jugador.getId());
            if (bonusDisponible <= 0) {
                return crearError("No tienes movimientos bonus disponibles");
            }
            
            if (pasos > bonusDisponible) {
                return crearError("Solo tienes " + bonusDisponible + " movimientos bonus");
            }
            
            // Usar bonus
            MotorJuego.ResultadoMovimiento resultado = motor.usarBonus(
                jugador.getId(), 
                fichaId, 
                pasos
            );
            
            // Usar vista
            VistaServidor.mostrarUsoBonus(
                jugador, 
                fichaId, 
                resultado.bonusConsumido, 
                resultado.bonusRestante
            );
            
            // Notificar
            notificarUsoBonus(partida, jugador, fichaId, pasos, resultado, cliente);
            
            //  Notificar estado del tablero
            notificarEstadoTablero(partida, cliente);
            
            // Crear respuesta
            JsonObject respuesta = crearRespuestaMovimiento(resultado);
            respuesta.addProperty("bonusUsado", resultado.bonusConsumido);
            respuesta.addProperty("bonusRestante", resultado.bonusRestante);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            System.err.println("Error usando bonus: " + e.getMessage());
            return crearError("Error: " + e.getMessage());
        }
    }

    
    /**
     * Crea respuesta JSON para un movimiento.
     */
    private JsonObject crearRespuestaMovimiento(MotorJuego.ResultadoMovimiento resultado) {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "movimiento_exitoso");
        respuesta.addProperty("exito", true);
        
        // Datos del movimiento
        JsonObject movimiento = new JsonObject();
        movimiento.addProperty("desde", resultado.casillaSalida);
        movimiento.addProperty("hasta", resultado.casillaLlegada);
        
        respuesta.add("movimiento", movimiento);
        
        // Informacion adicional
        if (resultado.capturaRealizada) {
            respuesta.addProperty("captura", true);
            respuesta.addProperty("fichaCapturadaId", resultado.fichaCapturadaId);
            respuesta.addProperty("bonusGanado", resultado.bonusGanado);
            respuesta.addProperty("bonusTotal", resultado.bonusTotal);
        }
        
        if (resultado.llegadaMeta) {
            respuesta.addProperty("meta", true);
            respuesta.addProperty("puntosMeta", resultado.bonusPuntosMeta);
            respuesta.addProperty("puntosTotal", resultado.puntosTotal);
        }
        
        // Mensaje
        String mensaje = "Ficha movida exitosamente";
        if (resultado.capturaRealizada) {
            mensaje = "¡Capturaste una ficha! +" + resultado.bonusGanado + " bonus";
        }
        if (resultado.llegadaMeta) {
            mensaje = "¡Llegaste a la meta! +" + resultado.bonusPuntosMeta + " puntos";
        }
        respuesta.addProperty("mensaje", mensaje);
        
        return respuesta;
    }
    
    // ============================
    // NOTIFICACIONES
    // ============================
    
    /**
     * Notifica movimiento a todos los jugadores.
     */
    private void notificarMovimiento(Partida partida, Jugador jugador, int fichaId, 
                                     MotorJuego.ResultadoMovimiento resultado, 
                                     ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "ficha_movida");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("jugadorNombre", jugador.getNombre());
        notificacion.addProperty("fichaId", fichaId);
        notificacion.addProperty("desde", resultado.casillaSalida);
        notificacion.addProperty("hasta", resultado.casillaLlegada);
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            cliente.getSessionId()
        );
    }
    
    /**
     * Notifica captura de ficha.
     */
    private void notificarCaptura(Partida partida, Jugador jugador, 
                                  MotorJuego.ResultadoMovimiento resultado, 
                                  ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "ficha_capturada");
        notificacion.addProperty("capturadorId", jugador.getId());
        notificacion.addProperty("capturadorNombre", jugador.getNombre());
        notificacion.addProperty("fichaCapturadaId", resultado.fichaCapturadaId);
        notificacion.addProperty("jugadorCapturadoId", resultado.jugadorCapturadoId);
        notificacion.addProperty("bonusGanado", resultado.bonusGanado);
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            null // Enviar a todos
        );
    }
    
    /**
     * Notifica llegada a meta.
     */
    private void notificarLlegadaMeta(Partida partida, Jugador jugador, int fichaId, 
                                      ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "ficha_en_meta");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("jugadorNombre", jugador.getNombre());
        notificacion.addProperty("fichaId", fichaId);
        notificacion.addProperty("fichasEnMeta", jugador.contarFichasEnMeta());
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            null
        );
    }
    
    /**
     * Notifica ganador.
     */
    private void notificarGanador(Partida partida, Jugador ganador, ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "partida_ganada");
        notificacion.addProperty("ganadorId", ganador.getId());
        notificacion.addProperty("ganadorNombre", ganador.getNombre());
        notificacion.addProperty("mensaje", "¡" + ganador.getNombre() + " ha ganado la partida!");
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            null
        );
    }
    
    /**
     * Notifica uso de bonus.
     */
    private void notificarUsoBonus(Partida partida, Jugador jugador, int fichaId, int pasos,
                                   MotorJuego.ResultadoMovimiento resultado, 
                                   ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "bonus_usado");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("jugadorNombre", jugador.getNombre());
        notificacion.addProperty("fichaId", fichaId);
        notificacion.addProperty("pasosBonus", pasos);
        notificacion.addProperty("bonusRestante", resultado.bonusRestante);
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            cliente.getSessionId()
        );
    }
    
    /**
     * ✅ NUEVO: Notifica cambio de turno
     */
    private void notificarCambioTurno(Partida partida, Jugador jugadorTurno, ClienteHandler cliente) {
        // Notificar al jugador cuyo turno es
        JsonObject tuTurno = new JsonObject();
        tuTurno.addProperty("tipo", "tu_turno");
        tuTurno.addProperty("jugadorId", jugadorTurno.getId());
        tuTurno.addProperty("jugadorNombre", jugadorTurno.getNombre());
        
        ClienteHandler handlerTurno = cliente.getServidor().getCliente(jugadorTurno.getSessionId());
        if (handlerTurno != null) {
            handlerTurno.enviarMensaje(tuTurno.toString());
        }
        
        // Notificar a los demas que cambio el turno
        JsonObject cambioTurno = new JsonObject();
        cambioTurno.addProperty("tipo", "cambio_turno");
        cambioTurno.addProperty("jugadorId", jugadorTurno.getId());
        cambioTurno.addProperty("jugadorNombre", jugadorTurno.getNombre());
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            cambioTurno.toString(),
            jugadorTurno.getSessionId()
        );
    }

    private void notificarEstadoTablero(Partida partida, ClienteHandler cliente) {
        if (partida.getTablero() == null) {
            return;
        }
        

        JsonObject estadoTablero = partida.getTablero().generarEstadoJSON();
        
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "estado_tablero");
        notificacion.add("tablero", estadoTablero); // add() para JsonObject, no addProperty()
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            notificacion.toString(),
            null // Enviar a todos
        );
    }
    
    /**
     * Obtiene el motor de juego de la partida.
     */
    private MotorJuego obtenerMotorJuego(Partida partida) {
        GestorMotores gestorMotores = GestorMotores.getInstancia();
        return gestorMotores.obtenerMotor(partida);
    }
    
    /**
     * Crea respuesta de error.
     */
    private String crearError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo", "error");
        error.addProperty("exito", false);
        error.addProperty("mensaje", mensaje);
        return error.toString();
    }
}