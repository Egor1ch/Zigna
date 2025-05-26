package cliente;

import java.io.*;
import java.net.*;
import java.util.*;
import modelo.Mensaje;
import modelo.Mensaje.TipoMensaje;

public class Cliente {
    private static final String HOST_DEFAULT = "localhost";
    private static final int PUERTO_DEFAULT = 5000;
    
    private String nombreUsuario;
    private String hostServidor;
    private int puertoServidor;
    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private boolean conectado;
    private GestorEventos gestorEventos;
    
    public Cliente(String nombreUsuario) {
        this(nombreUsuario, HOST_DEFAULT, PUERTO_DEFAULT);
    }
    
    public Cliente(String nombreUsuario, String hostServidor, int puertoServidor) {
        this.nombreUsuario = nombreUsuario;
        this.hostServidor = hostServidor;
        this.puertoServidor = puertoServidor;
        this.conectado = false;
    }
    
    public void setGestorEventos(GestorEventos gestor) {
        this.gestorEventos = gestor;
    }
    
    public boolean conectar() {
        try {
            socket = new Socket(hostServidor, puertoServidor);
            
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            
            entrada = new ObjectInputStream(socket.getInputStream());
            
            conectado = true;
            new Thread(this::recibirMensajes).start();
            
            Mensaje mensajeConexion = new Mensaje(nombreUsuario, "se ha conectado", TipoMensaje.CONEXION);
            enviarMensaje(mensajeConexion);
            
            System.out.println("Cliente conectado: " + nombreUsuario);
            
            return true;
        } catch (IOException e) {
            if (gestorEventos != null) {
                gestorEventos.onError("Error al conectar: " + e.getMessage());
            }
            return false;
        }
    }
    
    public void desconectar() {
        if (!conectado) return;
        
        try {
            conectado = false;
            
            Mensaje mensajeDesconexion = new Mensaje(nombreUsuario, "se ha desconectado", TipoMensaje.DESCONEXION);
            enviarMensaje(mensajeDesconexion);
            
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null) socket.close();
            
            if (gestorEventos != null) {
                gestorEventos.onDesconexion();
            }
        } catch (IOException e) {
            if (gestorEventos != null) {
                gestorEventos.onError("Error al desconectar: " + e.getMessage());
            }
        }
    }
    
    public void enviarMensajeTexto(String texto) {
        Mensaje mensaje = new Mensaje(nombreUsuario, texto, TipoMensaje.MENSAJE_TEXTO);
        enviarMensaje(mensaje);
    }
      public void enviarMensajePrivado(String destinatario, String texto) {
        Mensaje mensaje = new Mensaje(nombreUsuario, destinatario, texto, TipoMensaje.PRIVADO);
        enviarMensaje(mensaje);
    }
    
    public void enviarArchivo(String nombreArchivo, byte[] datosArchivo) {
        Mensaje mensaje = new Mensaje(nombreUsuario, nombreArchivo, datosArchivo);
        enviarMensaje(mensaje);
    }
    
    public void enviarArchivoPrivado(String destinatario, String nombreArchivo, byte[] datosArchivo) {
        Mensaje mensaje = new Mensaje(nombreUsuario, destinatario, nombreArchivo, datosArchivo);
        enviarMensaje(mensaje);
    }
    
    private void enviarMensaje(Mensaje mensaje) {
        if (!conectado) return;
        
        try {
            synchronized (salida) {
                salida.reset();
                salida.writeObject(mensaje);
                salida.flush();
                System.out.println("Mensaje enviado: " + mensaje);
            }
        } catch (IOException e) {
            if (gestorEventos != null) {
                gestorEventos.onError("Error al enviar mensaje: " + e.getMessage());
            }
            desconectar();
        }
    }
    
    private void recibirMensajes() {
        while (conectado) {
            try {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                procesarMensaje(mensaje);
            } catch (IOException | ClassNotFoundException e) {
                if (conectado) {
                    if (gestorEventos != null) {
                        gestorEventos.onError("Error de conexión: " + e.getMessage());
                    }
                    desconectar();
                }
                break;
            }
        }
    }
      private void procesarMensaje(Mensaje mensaje) {
        if (gestorEventos == null) return;
        
        switch (mensaje.getTipo()) {
            case MENSAJE_TEXTO:
                gestorEventos.onMensajeRecibido(mensaje);
                break;
            case PRIVADO:
                if (mensaje.getDatosArchivo() != null && mensaje.getNombreArchivo() != null) {
                    // Es un archivo privado
                    gestorEventos.onArchivoRecibido(mensaje);
                } else {
                    // Es un mensaje privado normal
                    gestorEventos.onMensajeRecibido(mensaje);
                }
                break;
            case ARCHIVO:
                gestorEventos.onArchivoRecibido(mensaje);
                break;
            case CONEXION:
            case DESCONEXION:
                gestorEventos.onActualizacionUsuarios(mensaje);
                break;
            case LISTA_USUARIOS:
                String contenido = mensaje.getContenido();
                if (contenido != null && !contenido.isEmpty()) {
                    if (contenido.endsWith(",")) {
                        contenido = contenido.substring(0, contenido.length() - 1);
                    }
                    
                    String[] listaUsuarios = contenido.split(",");
                    System.out.println("Recibida lista de usuarios: " + Arrays.toString(listaUsuarios));
                    
                    List<String> listaFiltrada = new ArrayList<>();
                    for (String usuario : listaUsuarios) {
                        if (usuario != null && !usuario.trim().isEmpty()) {
                            listaFiltrada.add(usuario.trim());
                        }
                    }
                    
                    System.out.println("Lista filtrada: " + listaFiltrada);
                    gestorEventos.onListaUsuarios(listaFiltrada);
                } else {
                    System.out.println("Recibida lista de usuarios vacía");
                    gestorEventos.onListaUsuarios(new ArrayList<>());
                }
                break;
            default:
                break;
        }
    }
    
    public interface GestorEventos {
        void onMensajeRecibido(Mensaje mensaje);
        void onArchivoRecibido(Mensaje mensaje);
        void onActualizacionUsuarios(Mensaje mensaje);
        void onListaUsuarios(List<String> usuarios);
        void onDesconexion();
        void onError(String error);
    }
    
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    
    public boolean estaConectado() {
        return conectado;
    }
}
