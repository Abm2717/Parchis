package modelo.cache;

import java.util.concurrent.locks.ReentrantLock;

/**
 * EstadoLocal - Estado temporal optimista del cliente.
 * 
 * En arquitectura híbrida:
 * - El cliente muestra cambios INMEDIATAMENTE (optimista)
 * - Este estado es TEMPORAL hasta confirmación del servidor
 * - Si el servidor rechaza, este estado se descarta
 * 
 * Propósito:
 * - Reducir latencia percibida por el usuario
 * - Mostrar feedback inmediato de acciones
 * - Permitir rollback si el servidor rechaza
 * 
 * Ciclo de vida:
 * 1. Usuario hace acción → EstadoLocal se actualiza
 * 2. Se envía al servidor
 * 3. Servidor responde:
 *    - OK → EstadoLocal se confirma y limpia
 *    - ERROR → EstadoLocal se descarta (rollback)
 */
public class EstadoLocal {
    
    // Estado de dados optimista
    private boolean tiradaPendiente;
    private int dado1Optimista;
    private int dado2Optimista;
    private long timestampTirada;
    
    // Estado de movimiento optimista
    private boolean movimientoPendiente;
    private int fichaIdMovimiento;
    private int posicionOrigenMovimiento;
    private int posicionDestinoMovimiento;
    private long timestampMovimiento;
    
    // Estado de acción pendiente
    private String accionPendiente; // "TIRAR_DADOS", "MOVER_FICHA", "SACAR_FICHA", etc.
    private long timestampAccion;
    
    // Flags
    private boolean esperandoConfirmacion;
    
    // Thread-safety
    private final ReentrantLock lock;
    
    public EstadoLocal() {
        this.tiradaPendiente = false;
        this.movimientoPendiente = false;
        this.esperandoConfirmacion = false;
        this.accionPendiente = null;
        this.lock = new ReentrantLock();
    }
    
    // ============================
    // ACTUALIZACIONES OPTIMISTAS
    // ============================
    
    /**
     * Registra una tirada optimista.
     */
    public void registrarTiradaOptimista(int dado1, int dado2) {
        lock.lock();
        try {
            this.tiradaPendiente = true;
            this.dado1Optimista = dado1;
            this.dado2Optimista = dado2;
            this.timestampTirada = System.currentTimeMillis();
            this.accionPendiente = "TIRAR_DADOS";
            this.esperandoConfirmacion = true;
            
            System.out.println("[ESTADO LOCAL] Tirada optimista: [" + dado1 + "] [" + dado2 + "]");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Registra un movimiento optimista.
     */
    public void registrarMovimientoOptimista(int fichaId, int origenCasilla, int destinoCasilla) {
        lock.lock();
        try {
            this.movimientoPendiente = true;
            this.fichaIdMovimiento = fichaId;
            this.posicionOrigenMovimiento = origenCasilla;
            this.posicionDestinoMovimiento = destinoCasilla;
            this.timestampMovimiento = System.currentTimeMillis();
            this.accionPendiente = "MOVER_FICHA";
            this.esperandoConfirmacion = true;
            
            System.out.println("[ESTADO LOCAL] Movimiento optimista: ficha " + fichaId + 
                " (" + origenCasilla + " -> " + destinoCasilla + ")");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Registra una acción pendiente genérica.
     */
    public void registrarAccionPendiente(String tipoAccion) {
        lock.lock();
        try {
            this.accionPendiente = tipoAccion;
            this.timestampAccion = System.currentTimeMillis();
            this.esperandoConfirmacion = true;
            
            System.out.println("[ESTADO LOCAL] Accion pendiente: " + tipoAccion);
        } finally {
            lock.unlock();
        }
    }
    
    // ============================
    // CONFIRMACIÓN / ROLLBACK
    // ============================
    
    /**
     * Confirma la tirada (el servidor la validó).
     */
    public void confirmarTirada() {
        lock.lock();
        try {
            if (tiradaPendiente) {
                System.out.println("[ESTADO LOCAL] Tirada confirmada por servidor");
                limpiarTirada();
            }
            esperandoConfirmacion = false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Rechaza la tirada (rollback).
     */
    public void rechazarTirada(String razon) {
        lock.lock();
        try {
            if (tiradaPendiente) {
                System.out.println("[ESTADO LOCAL] Tirada RECHAZADA: " + razon);
                System.out.println("  Descartando: [" + dado1Optimista + "] [" + dado2Optimista + "]");
                limpiarTirada();
            }
            esperandoConfirmacion = false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Confirma el movimiento (el servidor lo validó).
     */
    public void confirmarMovimiento() {
        lock.lock();
        try {
            if (movimientoPendiente) {
                System.out.println("[ESTADO LOCAL] Movimiento confirmado por servidor");
                limpiarMovimiento();
            }
            esperandoConfirmacion = false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Rechaza el movimiento (rollback).
     */
    public void rechazarMovimiento(String razon) {
        lock.lock();
        try {
            if (movimientoPendiente) {
                System.out.println("[ESTADO LOCAL] Movimiento RECHAZADO: " + razon);
                System.out.println("  Descartando: ficha " + fichaIdMovimiento + 
                    " (" + posicionOrigenMovimiento + " -> " + posicionDestinoMovimiento + ")");
                limpiarMovimiento();
            }
            esperandoConfirmacion = false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Confirma cualquier acción pendiente.
     */
    public void confirmarAccion() {
        lock.lock();
        try {
            if (accionPendiente != null) {
                System.out.println("[ESTADO LOCAL] Accion confirmada: " + accionPendiente);
            }
            limpiarTodo();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Rechaza cualquier acción pendiente (rollback completo).
     */
    public void rechazarAccion(String razon) {
        lock.lock();
        try {
            if (accionPendiente != null) {
                System.out.println("[ESTADO LOCAL] Accion RECHAZADA: " + accionPendiente);
                System.out.println("  Razon: " + razon);
            }
            limpiarTodo();
        } finally {
            lock.unlock();
        }
    }
    
    // ============================
    // CONSULTAS
    // ============================
    
    /**
     * Verifica si hay una tirada pendiente.
     */
    public boolean hasTiradaPendiente() {
        lock.lock();
        try {
            return tiradaPendiente;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene los dados optimistas.
     */
    public int[] getDadosOptimistas() {
        lock.lock();
        try {
            return new int[] { dado1Optimista, dado2Optimista };
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si hay un movimiento pendiente.
     */
    public boolean hasMovimientoPendiente() {
        lock.lock();
        try {
            return movimientoPendiente;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene la ficha del movimiento pendiente.
     */
    public int getFichaIdMovimiento() {
        lock.lock();
        try {
            return fichaIdMovimiento;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene la posición destino del movimiento pendiente.
     */
    public int getPosicionDestinoMovimiento() {
        lock.lock();
        try {
            return posicionDestinoMovimiento;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si hay alguna acción esperando confirmación.
     */
    public boolean isEsperandoConfirmacion() {
        lock.lock();
        try {
            return esperandoConfirmacion;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el tipo de acción pendiente.
     */
    public String getAccionPendiente() {
        lock.lock();
        try {
            return accionPendiente;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el tiempo transcurrido desde la última acción (ms).
     */
    public long getTiempoDesdeUltimaAccion() {
        lock.lock();
        try {
            if (!esperandoConfirmacion) {
                return 0;
            }
            
            long timestampRelevante = timestampAccion;
            if (tiradaPendiente) timestampRelevante = timestampTirada;
            if (movimientoPendiente) timestampRelevante = timestampMovimiento;
            
            return System.currentTimeMillis() - timestampRelevante;
        } finally {
            lock.unlock();
        }
    }
    
    // ============================
    // LIMPIEZA
    // ============================
    
    /**
     * Limpia el estado de la tirada.
     */
    private void limpiarTirada() {
        tiradaPendiente = false;
        dado1Optimista = -1;
        dado2Optimista = -1;
        timestampTirada = 0;
    }
    
    /**
     * Limpia el estado del movimiento.
     */
    private void limpiarMovimiento() {
        movimientoPendiente = false;
        fichaIdMovimiento = -1;
        posicionOrigenMovimiento = -1;
        posicionDestinoMovimiento = -1;
        timestampMovimiento = 0;
    }
    
    /**
     * Limpia todo el estado local.
     */
    public void limpiarTodo() {
        lock.lock();
        try {
            limpiarTirada();
            limpiarMovimiento();
            accionPendiente = null;
            timestampAccion = 0;
            esperandoConfirmacion = false;
            
            System.out.println("[ESTADO LOCAL] Todo limpiado");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Reinicia completamente el estado local.
     */
    public void reiniciar() {
        limpiarTodo();
    }
}