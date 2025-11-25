package controlador.juego;

import controlador.servidor.ClienteHandler;
import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.partida.MotorJuego;
import modelo.servicios.PersistenciaServicio;
import modelo.servicios.GestorMotores;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import vista.VistaServidor;
import modelo.Ficha.Ficha;
import modelo.Tablero.Casilla;
import modelo.Tablero.Tablero;

/**
 * ✅ ACTUALIZADO: Lógica completa de dados dobles
 * 
 * CASO 1: Doble SIN fichas fuera → Vuelve a tirar (3er doble = pierde turno)
 * CASO 2: Doble CON fichas fuera → Usa dados independientes, luego vuelve a tirar
 * CASO 3: 3er doble CON fichas fuera → Penalización (ficha a casa), NO usa dados, pierde turno
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
            
            if (!partida.esTurnoDeJugador(jugador.getId())) {
                return crearError("No es tu turno");
            }
            
            MotorJuego motor = obtenerMotorJuego(partida);
            
            // Tirar dados
            MotorJuego.ResultadoDados resultado = motor.tirarDados(jugador.getId());
            
            VistaServidor.mostrarTiradaDados(
                jugador,
                resultado.dado1,
                resultado.dado2,
                resultado.esDoble
            );
            
            // Verificar si tiene fichas en tablero (fuera de casa y meta)
            boolean tieneFichasEnTablero = jugador.getFichas().stream()
                .anyMatch(f -> !f.estaEnCasa() && !f.estaEnMeta());
            
            // ========================================
            // CASO 3: TERCER DOBLE CON FICHAS FUERA
            // ========================================
            if (resultado.esDoble && resultado.contadorDobles >= 3 && tieneFichasEnTablero) {
                System.out.println("[PENALIZACION] " + jugador.getNombre() + 
                                 " sacó 3 dobles consecutivos!");
                
                // Enviar ficha más adelantada a casa
                Ficha fichaMasAdelantada = obtenerFichaMasAdelantada(jugador);
                if (fichaMasAdelantada != null) {
                    enviarFichaACasa(fichaMasAdelantada);
                    
                    VistaServidor.mostrarFichaPerdida(jugador);
                    notificarPenalizacionTresDobles(partida, jugador, cliente);
                }
                
                // Resetear contador de dobles
                motor.resetearContadorDobles(jugador.getId());
                
                // Pasar turno automáticamente (NO usa los dados del 3er doble)
                partida.avanzarTurno();
                
                Jugador siguienteJugador = partida.getJugadorActual();
                if (siguienteJugador != null) {
                    notificarCambioTurno(partida, siguienteJugador, cliente);
                }
                
                // Respuesta indicando penalización y turno perdido
                JsonObject respuesta = new JsonObject();
                respuesta.addProperty("tipo", "resultado_dados");
                respuesta.addProperty("exito", true);
                
                JsonObject dadosObj = new JsonObject();
                dadosObj.addProperty("dado1", resultado.dado1);
                dadosObj.addProperty("dado2", resultado.dado2);
                dadosObj.addProperty("esDoble", true);
                dadosObj.addProperty("contadorDobles", resultado.contadorDobles);
                respuesta.add("dados", dadosObj);
                
                respuesta.addProperty("penalizacion", true);
                respuesta.addProperty("fichaPerdida", true);
                respuesta.addProperty("puedeJugar", false);
                respuesta.addProperty("turnoTerminado", true);
                respuesta.addProperty("mensaje", "3 dobles consecutivos! Perdiste una ficha y tu turno");
                
                return respuesta.toString();
            }
            
            // ========================================
            // CASO 1: TERCER DOBLE SIN FICHAS FUERA
            // ========================================
            if (resultado.esDoble && resultado.contadorDobles >= 3 && !tieneFichasEnTablero) {
                System.out.println("[TURNO PERDIDO] " + jugador.getNombre() + 
                                 " sacó 3 dobles sin fichas fuera");
                
                // Resetear contador de dobles
                motor.resetearContadorDobles(jugador.getId());
                
                // Pasar turno automáticamente
                partida.avanzarTurno();
                
                Jugador siguienteJugador = partida.getJugadorActual();
                if (siguienteJugador != null) {
                    notificarCambioTurno(partida, siguienteJugador, cliente);
                }
                
                // Respuesta indicando turno perdido
                JsonObject respuesta = new JsonObject();
                respuesta.addProperty("tipo", "resultado_dados");
                respuesta.addProperty("exito", true);
                
                JsonObject dadosObj = new JsonObject();
                dadosObj.addProperty("dado1", resultado.dado1);
                dadosObj.addProperty("dado2", resultado.dado2);
                dadosObj.addProperty("esDoble", true);
                dadosObj.addProperty("contadorDobles", resultado.contadorDobles);
                respuesta.add("dados", dadosObj);
                
                respuesta.addProperty("puedeJugar", false);
                respuesta.addProperty("turnoTerminado", true);
                respuesta.addProperty("mensaje", "3 dobles consecutivos sin fichas fuera. Pierdes turno");
                
                return respuesta.toString();
            }
            
            // ========================================
            // MANEJO NORMAL DE DOBLES
            // ========================================
            
            // Notificar resultado a todos
            notificarResultadoDados(partida, jugador, resultado, cliente);
            
            boolean puedeJugar = true;
            int dadoDisponible = 0;
            
            if (resultado.esDoble) {
                // ========================================
                // CASO ESPECIAL: DOBLE 5
                // ========================================
                if (resultado.dado1 == 5 && resultado.dado2 == 5) {
                    puedeJugar = intentarSacarDosFichasConDoble5(partida, jugador, motor, cliente);
                    
                } else {
                    // ========================================
                    // CASO 1: DOBLE SIN FICHAS FUERA
                    // ========================================
                    if (!tieneFichasEnTablero) {
                        System.out.println("[DOBLE] " + jugador.getNombre() + 
                                         " puede volver a tirar (sin fichas fuera)");
                        puedeJugar = true;
                        
                    } else {
                        // ========================================
                        // CASO 2: DOBLE CON FICHAS FUERA
                        // ========================================
                        System.out.println("[DOBLE] " + jugador.getNombre() + 
                                         " debe usar dados antes de volver a tirar");
                        puedeJugar = true;
                    }
                }
                
            } else {
                // ========================================
                // NO ES DOBLE: Resetear contador
                // ========================================
                motor.resetearContadorDobles(jugador.getId());
                
                // Manejo automático normal (sacar con 5, etc.)
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
            respuesta.addProperty("tieneFichasEnJuego", tieneFichasEnTablero);
            
            if (resultado.esDoble) {
                respuesta.addProperty("debeVolverATirar", true);
                
                if (tieneFichasEnTablero) {
                    respuesta.addProperty("mensaje", "¡Doble! Usa tus dados y podrás volver a tirar");
                } else {
                    respuesta.addProperty("mensaje", "¡Doble! Vuelve a tirar dados");
                }
            }
            
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
    
    private static class ResultadoAutomatico {
        boolean puedeJugar;
        int dadoDisponible;
        
        ResultadoAutomatico(boolean puedeJugar, int dadoDisponible) {
            this.puedeJugar = puedeJugar;
            this.dadoDisponible = dadoDisponible;
        }
    }
    
    /**
     * Obtiene la ficha más adelantada (excluyendo meta y casa)
     */
    private Ficha obtenerFichaMasAdelantada(Jugador jugador) {
        return jugador.getFichas().stream()
            .filter(f -> !f.estaEnCasa() && !f.estaEnMeta())
            .max((f1, f2) -> {
                Casilla c1 = f1.getCasillaActual();
                Casilla c2 = f2.getCasillaActual();
                if (c1 == null) return -1;
                if (c2 == null) return 1;
                return Integer.compare(c1.getIndice(), c2.getIndice());
            })
            .orElse(null);
    }
    
    private void enviarFichaACasa(Ficha ficha) {
        ficha.setCasillaActual(null);
        ficha.setEstado(modelo.Ficha.EstadoFicha.EN_CASA);
    }
    
    /**
    * ✅ ACTUALIZADO: Manejo de turno con reglas completas de salida
    */
   private ResultadoAutomatico manejarTurnoAutomatico(Partida partida, Jugador jugador, MotorJuego motor,
                                                       int dado1, int dado2, ClienteHandler cliente) {

       boolean dado1Es5 = (dado1 == 5);
       boolean dado2Es5 = (dado2 == 5);
       boolean sumaEs5 = (dado1 + dado2 == 5);
       boolean puedesSacar = dado1Es5 || dado2Es5 || sumaEs5;

       boolean tieneFichasEnCasa = jugador.getFichas().stream()
           .anyMatch(f -> f.estaEnCasa());

       boolean tieneFichasEnTablero = jugador.getFichas().stream()
           .anyMatch(f -> !f.estaEnCasa() && !f.estaEnMeta());

       Tablero tablero = partida.getTablero();

       // ========================================
       // VERIFICAR ESTADO DE LA SALIDA
       // ========================================
       boolean salidaBloqueadaPorPropias = tablero.salidaBloqueadaPorPropias(jugador.getId());
       List<Ficha> rivalesEnSalida = tablero.obtenerFichasRivalesEnSalida(jugador.getId());
       boolean hayRivalesEnSalida = !rivalesEnSalida.isEmpty();

       // ========================================
       // CASO: Tiene fichas en casa Y puede sacar con 5
       // ========================================
       if (tieneFichasEnCasa && puedesSacar) {

           // ----------------------------------------
           // CASO 3: Hay rivales en salida → Sacar y capturar automáticamente
           // ----------------------------------------
           if (hayRivalesEnSalida) {
               System.out.println("[AUTO] Hay fichas rivales en tu salida. Sacando y capturando...");
               return sacarYCapturarRivales(partida, jugador, motor, dado1, dado2, rivalesEnSalida, cliente);
           }

           // ----------------------------------------
           // CASO 1 y 2: Salida bloqueada por propias → NO sacar automáticamente
           // ----------------------------------------
           if (salidaBloqueadaPorPropias) {
               System.out.println("[INFO] Tu salida está bloqueada por tus propias fichas");

               // Si es suma = 5 (sin ningún 5), el jugador debe elegir
               if (sumaEs5 && !dado1Es5 && !dado2Es5) {
                   System.out.println("[INFO] Suma = 5 pero salida bloqueada. Usa tus dados para desbloquear");
                   return new ResultadoAutomatico(true, 0); // Jugador elige qué hacer
               }

               // Si tiene un dado 5, el jugador puede mover primero con el otro dado
               System.out.println("[INFO] Puedes mover con el otro dado para desbloquear y luego sacar con el 5");
               return new ResultadoAutomatico(true, 0); // Jugador elige qué hacer
           }

           // ----------------------------------------
           // CASO NORMAL: Salida libre → Sacar automáticamente
           // ----------------------------------------
           return intentarSacarFichaAutomatica(partida, jugador, motor, dado1, dado2, cliente);
       }

       // ========================================
       // CASO: Tiene fichas en tablero → Dejar que el jugador elija
       // ========================================
       if (tieneFichasEnTablero) {
           return new ResultadoAutomatico(true, 0);
       }

       // ========================================
       // CASO: No puede hacer nada → Pasar turno
       // ========================================
       System.out.println("[AUTO] " + jugador.getNombre() + " no puede jugar. Pasando turno...");
       pasarTurnoAutomaticamente(partida, jugador, cliente);
       return new ResultadoAutomatico(false, 0);
   }
    
   /**
    * ✅ NUEVO: CASO 3 - Saca ficha(s) y captura rival(es) en la salida
    */
   private ResultadoAutomatico sacarYCapturarRivales(Partida partida, Jugador jugador, MotorJuego motor,
                                                       int dado1, int dado2, List<Ficha> rivalesEnSalida,
                                                       ClienteHandler cliente) {
       try {
           boolean dado1Es5 = (dado1 == 5);
           boolean dado2Es5 = (dado2 == 5);
           boolean esDoble5 = (dado1Es5 && dado2Es5);

           // ----------------------------------------
           // CASO 3.1: Doble 5 con 2 rivales → Sacar 2 fichas, capturar 2
           // ----------------------------------------
           if (esDoble5 && rivalesEnSalida.size() >= 2) {
               System.out.println("[AUTO] Doble 5 con 2 rivales en salida. Sacando 2 fichas y capturando...");

               // Sacar primera ficha
               Ficha ficha1 = jugador.getFichas().stream()
                   .filter(f -> f.estaEnCasa())
                   .findFirst()
                   .orElse(null);

               if (ficha1 == null) return new ResultadoAutomatico(true, 0);

               MotorJuego.ResultadoSacar resultado1 = motor.sacarFichaDeCasa(
                   jugador.getId(), ficha1.getId(), 5, 5
               );

               notificarSacarFicha(partida, jugador, resultado1, cliente);

               // Capturar primer rival
               Ficha rival1 = rivalesEnSalida.get(0);
               enviarFichaACasa(rival1);

               int bonusActual1 = motor.getBonusDisponible(jugador.getId());
               // Agregar bonus manualmente (el motor no lo hace en sacar)
               notificarCaptura(partida, jugador, rival1.getId(), rival1.getIdJugador(), 20, cliente);
               System.out.println("[CAPTURA] " + jugador.getNombre() + " capturó ficha rival. +20 bonus");

               // Sacar segunda ficha
               Ficha ficha2 = jugador.getFichas().stream()
                   .filter(f -> f.estaEnCasa())
                   .findFirst()
                   .orElse(null);

               if (ficha2 != null) {
                   MotorJuego.ResultadoSacar resultado2 = motor.sacarFichaDeCasa(
                       jugador.getId(), ficha2.getId(), 5, 0
                   );

                   notificarSacarFicha(partida, jugador, resultado2, cliente);

                   // Capturar segundo rival
                   Ficha rival2 = rivalesEnSalida.get(1);
                   enviarFichaACasa(rival2);

                   notificarCaptura(partida, jugador, rival2.getId(), rival2.getIdJugador(), 20, cliente);
                   System.out.println("[CAPTURA] " + jugador.getNombre() + " capturó segunda ficha rival. +20 bonus");
               }

               // Notificar 2 bonos separados disponibles
               JsonObject notifBonus = new JsonObject();
               notifBonus.addProperty("tipo", "bonus_disponibles");
               notifBonus.addProperty("cantidad", 2);
               notifBonus.addProperty("valorCadaUno", 20);
               notifBonus.addProperty("mensaje", "Tienes 2 bonos de 20 casillas cada uno");

               ClienteHandler handler = cliente.getServidor().getCliente(jugador.getSessionId());
               if (handler != null) {
                   handler.enviarMensaje(notifBonus.toString());
               }

               return new ResultadoAutomatico(false, 0); // Doble 5, vuelve a tirar
           }

           // ----------------------------------------
           // CASO 3.2: Un solo 5 o suma = 5 → Sacar 1 ficha, capturar 1 rival
           // ----------------------------------------
           Ficha fichaEnCasa = jugador.getFichas().stream()
               .filter(f -> f.estaEnCasa())
               .findFirst()
               .orElse(null);

           if (fichaEnCasa == null) return new ResultadoAutomatico(true, 0);

           System.out.println("[AUTO] Sacando ficha y capturando rival en salida...");

           MotorJuego.ResultadoSacar resultado = motor.sacarFichaDeCasa(
               jugador.getId(),
               fichaEnCasa.getId(),
               dado1,
               dado2
           );

           notificarSacarFicha(partida, jugador, resultado, cliente);

           // Capturar rival
           Ficha rivalCapturado = rivalesEnSalida.get(0);
           enviarFichaACasa(rivalCapturado);

           notificarCaptura(partida, jugador, rivalCapturado.getId(), 
                           rivalCapturado.getIdJugador(), 20, cliente);

           System.out.println("[CAPTURA] " + jugador.getNombre() + " capturó ficha rival. +20 bonus");

           // Verificar si hay dado disponible
           if (resultado.hayDadoDisponible()) {
               int dadoDisp = resultado.getDadoDisponible();
               System.out.println("[AUTO] Ficha sacada y rival capturado. Dado " + dadoDisp + " disponible.");

               // Notificar bonus disponible
               JsonObject notifBonus = new JsonObject();
               notifBonus.addProperty("tipo", "bonus_disponible");
               notifBonus.addProperty("valor", 20);
               notifBonus.addProperty("mensaje", "Tienes 20 casillas de bonus por captura");

               ClienteHandler handler = cliente.getServidor().getCliente(jugador.getSessionId());
               if (handler != null) {
                   handler.enviarMensaje(notifBonus.toString());
               }

               return new ResultadoAutomatico(true, dadoDisp);
           } else {
               // Usó ambos dados, pasar turno
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

       } catch (Exception e) {
           System.err.println("[ERROR] Error al sacar y capturar: " + e.getMessage());
           e.printStackTrace();
           return new ResultadoAutomatico(true, 0);
       }
   }
   
    private boolean intentarSacarDosFichasConDoble5(Partida partida, Jugador jugador, MotorJuego motor,
                                                     ClienteHandler cliente) {
        try {
            Ficha ficha1 = jugador.getFichas().stream()
                .filter(f -> f.estaEnCasa())
                .findFirst()
                .orElse(null);
            
            if (ficha1 == null) {
                return true;
            }
            
            System.out.println("[AUTO DOBLE-5] Sacando primera ficha #" + ficha1.getId() + " de " + jugador.getNombre());
            
            MotorJuego.ResultadoSacar resultado1 = motor.sacarFichaDeCasa(
                jugador.getId(),
                ficha1.getId(),
                5,
                5
            );
            
            notificarSacarFicha(partida, jugador, resultado1, cliente);
            
            Ficha ficha2 = jugador.getFichas().stream()
                .filter(f -> f.estaEnCasa())
                .findFirst()
                .orElse(null);
            
            if (ficha2 == null) {
                System.out.println("[AUTO DOBLE-5] No hay más fichas en casa para sacar");
                return false;
            }
            
            Tablero tablero = partida.getTablero();
            Casilla salida = tablero.getCasillaSalidaParaJugador(jugador.getId());
            int fichasEnSalida = (int) jugador.getFichas().stream()
                .filter(f -> !f.estaEnCasa() && f.getCasillaActual() != null && 
                            f.getCasillaActual().getIndice() == salida.getIndice())
                .count();
            
            if (fichasEnSalida >= 2) {
                System.out.println("[AUTO DOBLE-5] Ya hay 2 fichas en el inicio, no se puede sacar la segunda");
                return false;
            }
            
            System.out.println("[AUTO DOBLE-5] Sacando segunda ficha #" + ficha2.getId() + " de " + jugador.getNombre());
            
            MotorJuego.ResultadoSacar resultado2 = motor.sacarFichaDeCasa(
                jugador.getId(),
                ficha2.getId(),
                5,
                0
            );
            
            notificarSacarFicha(partida, jugador, resultado2, cliente);
            
            return false;
            
        } catch (Exception e) {
            System.err.println("[AUTO DOBLE-5] Error: " + e.getMessage());
            return true;
        }
    }
    
    private ResultadoAutomatico intentarSacarFichaAutomatica(Partida partida, Jugador jugador, MotorJuego motor,
                                                              int dado1, int dado2, ClienteHandler cliente) {
        try {
            Ficha fichaEnCasa = jugador.getFichas().stream()
                .filter(f -> f.estaEnCasa())
                .findFirst()
                .orElse(null);
            
            if (fichaEnCasa == null) {
                return new ResultadoAutomatico(true, 0);
            }
            
            int fichaId = fichaEnCasa.getId();
            
            System.out.println("[AUTO] Sacando automáticamente ficha #" + fichaId + " de " + jugador.getNombre());
            
            MotorJuego.ResultadoSacar resultado = motor.sacarFichaDeCasa(
                jugador.getId(),
                fichaId,
                dado1,
                dado2
            );
            
            VistaServidor.mostrarMovimientoFicha(
                jugador,
                fichaId,
                -1,
                resultado.casillaLlegada
            );
            
            notificarSacarFicha(partida, jugador, resultado, cliente);
            
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
            
            if (resultado.hayDadoDisponible()) {
                int dadoDisp = resultado.getDadoDisponible();
                System.out.println("[AUTO] Ficha sacada. Dado " + dadoDisp + " disponible para mover.");
                return new ResultadoAutomatico(true, dadoDisp);
                
            } else {
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
            System.out.println("[AUTO] No se pudo sacar automáticamente: " + e.getMessage());
            return new ResultadoAutomatico(true, 0);
        } catch (Exception e) {
            System.err.println("[AUTO] Error al sacar ficha: " + e.getMessage());
            e.printStackTrace();
            return new ResultadoAutomatico(true, 0);
        }
    }
    
    private void pasarTurnoAutomaticamente(Partida partida, Jugador jugador, ClienteHandler cliente) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        partida.avanzarTurno();
        
        Jugador siguienteJugador = partida.getJugadorActual();
        if (siguienteJugador != null) {
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
        
        JsonObject respuestaLocal = crearRespuestaResultadoConJugador(resultado, jugador);
        cliente.enviarMensaje(respuestaLocal.toString());
    }
    
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
        
        if (resultado.hayDadoDisponible()) {
            notificacion.addProperty("dadoDisponible", resultado.getDadoDisponible());
        }
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(),
            notificacion.toString(),
            null
        );
        
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
    
    private JsonObject crearRespuestaResultadoConJugador(MotorJuego.ResultadoDados resultado, Jugador jugador) {
        JsonObject respuesta = crearRespuestaResultado(resultado);
        
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