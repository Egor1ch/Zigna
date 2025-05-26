package modelo;

import java.io.Serializable;
import java.util.Date;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static enum TipoMensaje {
        MENSAJE_TEXTO,
        ARCHIVO,
        CONEXION,
        DESCONEXION,
        PRIVADO,
        LISTA_USUARIOS
    }
    
    private String remitente;
    private String destinatario;
    private String contenido;
    private Date timestamp;
    private TipoMensaje tipo;
    private byte[] datosArchivo;
    private String nombreArchivo;
    
    public Mensaje(String remitente, String contenido, TipoMensaje tipo) {
        this.remitente = remitente;
        this.contenido = contenido;
        this.timestamp = new Date();
        this.tipo = tipo;
        this.destinatario = null;
    }
    
    public Mensaje(String remitente, String destinatario, String contenido, TipoMensaje tipo) {
        this(remitente, contenido, tipo);
        this.destinatario = destinatario;
    }
      public Mensaje(String remitente, String nombreArchivo, byte[] datosArchivo) {
        this(remitente, "Archivo: " + nombreArchivo, TipoMensaje.ARCHIVO);
        this.nombreArchivo = nombreArchivo;
        this.datosArchivo = datosArchivo;
    }
    
    public Mensaje(String remitente, String destinatario, String nombreArchivo, byte[] datosArchivo) {
        this(remitente, "Archivo: " + nombreArchivo, TipoMensaje.PRIVADO);
        this.destinatario = destinatario;
        this.nombreArchivo = nombreArchivo;
        this.datosArchivo = datosArchivo;
    }
    
    public String getRemitente() {
        return remitente;
    }
    
    public String getDestinatario() {
        return destinatario;
    }
    
    public boolean esPrivado() {
        return destinatario != null && !destinatario.isEmpty();
    }
    
    public String getContenido() {
        return contenido;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public TipoMensaje getTipo() {
        return tipo;
    }
    
    public byte[] getDatosArchivo() {
        return datosArchivo;
    }
    
    public String getNombreArchivo() {
        return nombreArchivo;
    }
      @Override
    public String toString() {
        String formatoMensaje;
        if (tipo == TipoMensaje.CONEXION) {
            formatoMensaje = "** " + remitente + " se ha conectado **";
        } else if (tipo == TipoMensaje.DESCONEXION) {
            formatoMensaje = "** " + remitente + " se ha desconectado **";
        } else if (esPrivado()) {            if (datosArchivo != null && nombreArchivo != null) {
                // Es un archivo privado
                formatoMensaje = "[Privado a " + destinatario + "] " + remitente + " ha enviado un archivo: " + nombreArchivo;
            } else {
                // Es un mensaje privado normal
                formatoMensaje = "[Privado a " + destinatario + "] " + remitente + ": " + contenido;
            }        } else {
            formatoMensaje = remitente + ": " + contenido;
        }
        return formatoMensaje;
    }
}
