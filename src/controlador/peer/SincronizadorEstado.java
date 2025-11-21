package controlador.peer;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SincronizadorEstado - Sincroniza estado local con servidor.
 * 
 * En arquitectura híbrida:
 * - El cliente muestra estado "optimista" inmediatamente (P2P)
 * - El servidor envía el estado "oficial" después (validado)
 * - Este sincronizador reconcilia ambos estados
 * 
 * Responsabilidades:
 * - Mantener estado local temporal
 * - Aplicar actualizaciones del servidor
 * - Detectar conflictos (estado local vs servidor)
 * - Corregir estado local si el servidor difiere
 */
public class SincronizadorEstado {
    
    // Estado local temporal (optimista)
    private int dado1Local = -1;
    private int dado2Local = -1;
    private List<MovimientoLocal> movimientosLocales;
    
    // Estado oficial del servidor
    private int dado1Oficial = -1;
    private int dado2Oficial = -1;
    
    // Lock para thread-safety
    private final ReentrantLock lock;
    
    // Timestamps
    private long timestampUltimaActualizacion;
    
    public SincronizadorEstado() {
        this.movimientosLocales = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.timestampUltimaActualizacion = 0;
    }
    
    // ============================
    // ACTUALIZACIONES LOCALES (OPTIMISTAS)
    // ============================
    
    /**
     * Registra una tirada de dados local (antes de confirmación del servidor).
     */
    public void registrarTiradaLocal(int dado1, int dado2) {
        lock.lock();
        try {
            this.dado1Local = dado1;
            this.dado2Local = dado2;
            System.out.println("[SINCRONIZADOR] Tirada local: [" + dado1 + "] [" + dado2 + "]");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Registra un movimiento local (antes de confirmación del servidor).
     */
    public void registrarMovimientoLocal(int fichaId, int origenCasilla, int destinoCasilla) {
        lock.lock();
        try {
            MovimientoLocal mov = new MovimientoLocal(
                fichaId, 
                origenCasilla, 
                destinoCasilla, 
                System.currentTimeMillis()
            );
            movimientosLocales.add(mov);
            System.out.println("[SINCRONIZADOR] Movimiento local: ficha " + fichaId + 
                " (" + origenCasilla + " -> " + destinoCasilla + ")");
        } finally {
            lock.unlock();
        }
    }
    
    // ============================
    // ACTUALIZACIONES DEL SERVIDOR (OFICIALES)
    // ============================
    
    /**
     * Actualiza los dados con el valor oficial del servidor.
     * 
     * @return true si hubo diferencia con el estado local
     */
    public boolean actualizarDados(int dado1, int dado2) {
        lock.lock();
        try {
            boolean hayConflicto = false;
            
            // Verificar si hay diferencia con el estado local
            if (dado1Local != -1 && dado2Local != -1) {
                if (dado1Local != dado1 || dado2Local != dado2) {
                    System.out.println("[SINCRONIZADOR] CONFLICTO en dados!");
                    System.out.println("  Local: [" + dado1Local + "] [" + dado2Local + "]");
                    System.out.println("  Servidor: [" + dado1 + "] [" + dado2 + "]");
                    hayConflicto = true;
                }
            }
            
            // Actualizar con valores oficiales
            this.dado1Oficial = dado1;
            this.dado2Oficial = dado2;
            this.dado1Local = -1; // Limpiar local
            this.dado2Local = -1;
            
            timestampUltimaActualizacion = System.currentTimeMillis();
            
            return hayConflicto;
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Actualiza un movimiento con la confirmación oficial del servidor.
     * 
     * @return true si hubo diferencia con el estado local
     */
    public boolean actualizarMovimiento(int fichaId, int origenCasilla, int destinoCasilla) {
        lock.lock();
        try {
            boolean hayConflicto = false;
            
            // Buscar movimiento local correspondiente
            MovimientoLocal movLocal = buscarMovimientoLocal(fichaId);
            
            if (movLocal != null) {
                // Verificar si coincide
                if (movLocal.destinoCasilla != destinoCasilla) {
                    System.out.println("[SINCRONIZADOR] CONFLICTO en movimiento!");
                    System.out.println("  Local: ficha " + fichaId + " -> casilla " + movLocal.destinoCasilla);
                    System.out.println("  Servidor: ficha " + fichaId + " -> casilla " + destinoCasilla);
                    hayConflicto = true;
                }
                
                // Remover movimiento local (ya confirmado)
                movimientosLocales.remove(movLocal);
            }
            
            timestampUltimaActualizacion = System.currentTimeMillis();
            
            return hayConflicto;
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Rechaza una acción local (el servidor la invalidó).
     */
    public void rechazarAccionLocal(String tipoAccion, String razon) {
        lock.lock();
        try {
            System.out.println("[SINCRONIZADOR] Accion rechazada por servidor:");
            System.out.println("  Tipo: " + tipoAccion);
            System.out.println("  Razon: " + razon);
            
            // Limpiar estado local
            if (tipoAccion.equals("tirada")) {
                dado1Local = -1;
                dado2Local = -1;
            } else if (tipoAccion.equals("movimiento")) {
                movimientosLocales.clear();
            }
            
        } finally {
            lock.unlock();
        }
    }
    
    // ============================
    // CONSULTAS DE ESTADO
    // ============================
    
    /**
     * Obtiene los dados oficiales actuales.
     */
    public int[] getDadosOficiales() {
        lock.lock();
        try {
            return new int[] { dado1Oficial, dado2Oficial };
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene los dados locales (optimistas).
     */
    public int[] getDadosLocales() {
        lock.lock();
        try {
            return new int[] { dado1Local, dado2Local };
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si hay movimientos pendientes de confirmación.
     */
    public boolean hayMovimientosPendientes() {
        lock.lock();
        try {
            return !movimientosLocales.isEmpty();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el número de movimientos pendientes.
     */
    public int getNumeroMovimientosPendientes() {
        lock.lock();
        try {
            return movimientosLocales.size();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el timestamp de la última actualización.
     */
    public long getTimestampUltimaActualizacion() {
        return timestampUltimaActualizacion;
    }
    
    // ============================
    // LIMPIEZA
    // ============================
    
    /**
     * Limpia todo el estado local.
     */
    public void limpiarEstadoLocal() {
        lock.lock();
        try {
            dado1Local = -1;
            dado2Local = -1;
            movimientosLocales.clear();
            System.out.println("[SINCRONIZADOR] Estado local limpiado");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Reinicia el sincronizador completamente.
     */
    public void reiniciar() {
        lock.lock();
        try {
            dado1Local = -1;
            dado2Local = -1;
            dado1Oficial = -1;
            dado2Oficial = -1;
            movimientosLocales.clear();
            timestampUltimaActualizacion = 0;
            System.out.println("[SINCRONIZADOR] Reiniciado");
        } finally {
            lock.unlock();
        }
    }
    
    // ============================
    // UTILIDADES PRIVADAS
    // ============================
    
    /**
     * Busca un movimiento local por ficha.
     */
    private MovimientoLocal buscarMovimientoLocal(int fichaId) {
        for (MovimientoLocal mov : movimientosLocales) {
            if (mov.fichaId == fichaId) {
                return mov;
            }
        }
        return null;
    }
    
    // ============================
    // CLASE INTERNA: MOVIMIENTO LOCAL
    // ============================
    
    /**
     * Representa un movimiento local pendiente de confirmación.
     */
    private static class MovimientoLocal {
        int fichaId;
        int origenCasilla;
        int destinoCasilla;
        long timestamp;
        
        MovimientoLocal(int fichaId, int origenCasilla, int destinoCasilla, long timestamp) {
            this.fichaId = fichaId;
            this.origenCasilla = origenCasilla;
            this.destinoCasilla = destinoCasilla;
            this.timestamp = timestamp;
        }
    }
}