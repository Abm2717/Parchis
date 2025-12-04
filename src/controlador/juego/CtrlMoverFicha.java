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
 * ✅ CORREGIDO: Notificación completa de capturas
 * - Envía ficha capturada a CASA
 * - Aplica bonus +20 casillas al capturador
 * - Soporte para mover fichas con un solo dado
 */
public class CtrlMoverFicha {
    
    private final PersistenciaServicio persistencia;
    
    public CtrlMoverFicha() {
        this.persistencia = PersistenciaServicio.getInstancia();
    }
 
    /**
     * ✅ NUEVO: Mueve una ficha con UN SOLO dado
     * Se usa cuando el jugador debe elegir qué hacer con cada dado por separado
     * 
     * @param cliente ClienteHandler del jugador
     * @param fichaId ID de la ficha a mover
     * @param valorDado Valor de UN solo dado (no la suma)
     * @param pasarTurno Si true, pasa el turno después del movimiento
     * @return Respuesta JSON
     */
    public String moverConUnDado(ClienteHandler cliente, int fichaId, int valorDado, boolean pasarTurno) {
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
            
            // ✅ Mover con UN solo dado
            MotorJuego.ResultadoMovimiento resultado = motor.moverFichaConUnDado(
                jugador.getId(), 
                fichaId, 
                valorDado
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
            
            // ✅ Pasar turno solo si se indica
            if (pasarTurno) {
                partida.avanzarTurno();
                
                // Notificar estado del tablero a todos
                notificarEstadoTablero(partida, cliente);
                
                // Notificar al siguiente jugador
                Jugador siguienteJugador = partida.getJugadorActual();
                if (siguienteJugador != null) {
                    notificarCambioTurno(partida, siguienteJugador, cliente);
                }
            } else {
                // Solo actualizar tablero sin pasar turno
                notificarEstadoTablero(partida, cliente);
            }
            
            // Crear respuesta
            JsonObject respuesta = crearRespuestaMovimiento(resultado);
            respuesta.addProperty("turnoTerminado", pasarTurno);
            
            return respuesta.toString();
            
        } catch (MotorJuego.MovimientoInvalidoException e) {
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
     * ✅ MANTENER: Método original para compatibilidad
     * Ejecuta un movimiento usando ambos dados
     */
    public String ejecutar(ClienteHandler cliente, int fichaId, int dado1, int dado2) {
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
            
            // Ejecutar movimiento con ambos dados
            MotorJuego.ResultadoMovimiento resultado = motor.moverFicha(
                jugador.getId(), 
                fichaId, 
                dado1,
                dado2
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
            
            // Avanzar turno
            partida.avanzarTurno();
            
            // Notificar estado del tablero a todos
            notificarEstadoTablero(partida, cliente);
            
            // Notificar al siguiente jugador
            Jugador siguienteJugador = partida.getJugadorActual();
            if (siguienteJugador != null) {
                notificarCambioTurno(partida, siguienteJugador, cliente);
            }
            
            // Crear respuesta
            JsonObject respuesta = crearRespuestaMovimiento(resultado);
            
            return respuesta.toString();
            
        } catch (MotorJuego.MovimientoInvalidoException e) {
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
     */
    public String usarBonus(ClienteHandler cliente, int fichaId, int pasos) {
        try {
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (!partidaOpt.isPresent()) {
                return crearError("No estas en ninguna partida");
            }
            
            Partida partida = partidaOpt.get();
            
            if (partida.getEstado() != EstadoPartida.EN_PROGRESO) {
                return crearError("La partida no esta en progreso");
            }
            
            MotorJuego motor = obtenerMotorJuego(partida);
            
            int bonusDisponible = motor.getBonusDisponible(jugador.getId());
            if (bonusDisponible <= 0) {
                return crearError("No tienes movimientos bonus disponibles");
            }
            
            if (pasos > bonusDisponible) {
                return crearError("Solo tienes " + bonusDisponible + " movimientos bonus");
            }
            
            MotorJuego.ResultadoMovimiento resultado = motor.usarBonus(
                jugador.getId(), 
                fichaId, 
                pasos
            );
            
            VistaServidor.mostrarUsoBonus(
                jugador, 
                fichaId, 
                resultado.bonusConsumido, 
                resultado.bonusRestante
            );
            
            notificarUsoBonus(partida, jugador, fichaId, pasos, resultado, cliente);
            notificarEstadoTablero(partida, cliente);
            
            JsonObject respuesta = crearRespuestaMovimiento(resultado);
            respuesta.addProperty("bonusUsado", resultado.bonusConsumido);
            respuesta.addProperty("bonusRestante", resultado.bonusRestante);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            System.err.println("Error usando bonus: " + e.getMessage());
            return crearError("Error: " + e.getMessage());
        }
    }

    
    private JsonObject crearRespuestaMovimiento(MotorJuego.ResultadoMovimiento resultado) {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "movimiento_exitoso");
        respuesta.addProperty("exito", true);
        
        JsonObject movimiento = new JsonObject();
        movimiento.addProperty("desde", resultado.casillaSalida);
        movimiento.addProperty("hasta", resultado.casillaLlegada);
        
        respuesta.add("movimiento", movimiento);
        
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
     * ✅ CORREGIDO: Notifica captura completa
     * 1. Mensaje de captura
     * 2. Envía ficha capturada a CASA
     * 3. Aplica bonus +20 al capturador
     */
    private void notificarCaptura(Partida partida, Jugador jugador, 
                                  MotorJuego.ResultadoMovimiento resultado, 
                                  ClienteHandler cliente) {
        // 1️⃣ Notificar que hubo captura
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
            null
        );
        
        // 2️⃣ ✅ NUEVO: Enviar ficha capturada a CASA
        Jugador capturado = partida.getJugadorPorId(resultado.jugadorCapturadoId);
        JsonObject moverACasa = new JsonObject();
        moverACasa.addProperty("tipo", "ficha_movida");
        moverACasa.addProperty("jugadorId", capturado.getId());
        moverACasa.addProperty("jugadorNombre", capturado.getNombre());
        moverACasa.addProperty("fichaId", resultado.fichaCapturadaId);
        moverACasa.addProperty("desde", -2);  // -2 = señal de captura
        moverACasa.addProperty("hasta", -1);  // -1 = casa
        moverACasa.addProperty("automatico", true);
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            moverACasa.toString(),
            null
        );
        
        // 3️⃣ ✅ NUEVO: Aplicar bonus +20 al capturador
        JsonObject aplicarBonus = new JsonObject();
        aplicarBonus.addProperty("tipo", "aplicar_bonus_captura");
        aplicarBonus.addProperty("jugadorId", jugador.getId());
        aplicarBonus.addProperty("jugadorNombre", jugador.getNombre());
        aplicarBonus.addProperty("fichaCapturadaId", resultado.fichaCapturadaId);
        aplicarBonus.addProperty("bonusGanado", resultado.bonusGanado);
        aplicarBonus.addProperty("bonusTotal", resultado.bonusTotal);
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            aplicarBonus.toString(),
            null
        );
    }
    
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
    
    private void notificarCambioTurno(Partida partida, Jugador jugadorTurno, ClienteHandler cliente) {
        // Notificar a todos PRIMERO
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
        
        // Notificar al jugador específico
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

    private void notificarEstadoTablero(Partida partida, ClienteHandler cliente) {
        if (partida.getTablero() == null) {
            return;
        }
        
        JsonObject estadoTablero = partida.getTablero().generarEstadoJSON();
        
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "estado_tablero");
        notificacion.add("tablero", estadoTablero);
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            notificacion.toString(),
            null
        );
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