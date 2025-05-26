package servidor;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import modelo.Mensaje;
import modelo.Mensaje.TipoMensaje;

public class Servidor {
    private static final int PUERTO = 5000;
    private ServerSocket serverSocket;
    private final ConcurrentHashMap<String, GestorCliente> clientesConectados;
    private final ExecutorService poolHilos;
    private boolean ejecutando;
    private ServidorListener listener;
    
    public Servidor() {
        clientesConectados = new ConcurrentHashMap<>();
        poolHilos = Executors.newCachedThreadPool();
        ejecutando = false;
    }
    
    public void setListener(ServidorListener listener) {
        this.listener = listener;
    }
    
    public void iniciar() {
        try {
            serverSocket = new ServerSocket(PUERTO);
            ejecutando = true;
            System.out.println("Servidor iniciado en el puerto " + PUERTO);
            System.out.println("Esperando conexiones...");
            
            new Thread(() -> aceptarConexiones()).start();
            
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
            detener();
        }
    }
    
    public void detener() {
        ejecutando = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            for (GestorCliente cliente : clientesConectados.values()) {
                cliente.cerrarConexion();
            }
            
            poolHilos.shutdown();
            try {
                if (!poolHilos.awaitTermination(5, TimeUnit.SECONDS)) {
                    poolHilos.shutdownNow();
                }
            } catch (InterruptedException e) {
                poolHilos.shutdownNow();
            }
            
            System.out.println("Servidor detenido");
        } catch (IOException e) {
            System.err.println("Error al detener el servidor: " + e.getMessage());
        }
    }
    
    private void aceptarConexiones() {
        while (ejecutando) {
            try {
                Socket socketCliente = serverSocket.accept();
                poolHilos.execute(() -> manejarCliente(socketCliente));
            } catch (IOException e) {
                if (ejecutando) {
                    System.err.println("Error al aceptar una conexión: " + e.getMessage());
                }
            }
        }
    }
    
    private void manejarCliente(Socket socketCliente) {
        try {
            ObjectOutputStream salida = new ObjectOutputStream(socketCliente.getOutputStream());
            salida.flush();
            
            ObjectInputStream entrada = new ObjectInputStream(socketCliente.getInputStream());
            
            Mensaje mensajeConexion = (Mensaje) entrada.readObject();
            String nombreUsuario = mensajeConexion.getRemitente();
            
            if (clientesConectados.containsKey(nombreUsuario)) {
                Mensaje rechazo = new Mensaje("Servidor", "El nombre de usuario ya está en uso", TipoMensaje.DESCONEXION);
                salida.writeObject(rechazo);
                salida.flush();
                socketCliente.close();
                return;
            }
            
            GestorCliente gestorCliente = new GestorCliente(nombreUsuario, socketCliente, entrada, salida);
            clientesConectados.put(nombreUsuario, gestorCliente);
            System.out.println("Nuevo cliente conectado: " + nombreUsuario);
            System.out.println("Clientes conectados: " + clientesConectados.keySet());
            
            if (listener != null) {
                listener.onClienteConectado(nombreUsuario);
            }
            
            difundirMensaje(mensajeConexion);
            
            gestorCliente.iniciarRecepcion();
            
            actualizarListaUsuariosParaTodos();
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al manejar un cliente: " + e.getMessage());
            try {
                socketCliente.close();
            } catch (IOException ex) {
                System.err.println("Error al cerrar socket: " + ex.getMessage());
            }
        }
    }
    
    private void actualizarListaUsuariosParaTodos() {
        StringBuilder listaUsuarios = new StringBuilder();
        for (String usuario : clientesConectados.keySet()) {
            listaUsuarios.append(usuario).append(",");
        }
        
        if (listener != null) {
            String[] usuarios = clientesConectados.keySet().toArray(new String[0]);
            listener.onListaUsuariosActualizada(usuarios);
        }
        
        Mensaje mensaje = new Mensaje("Servidor", listaUsuarios.toString(), TipoMensaje.LISTA_USUARIOS);
        
        for (GestorCliente cliente : clientesConectados.values()) {
            try {
                cliente.enviarMensaje(mensaje);
                System.out.println("Lista de usuarios enviada a: " + cliente.nombreUsuario);
            } catch (IOException e) {
                System.err.println("Error al enviar lista de usuarios a " + cliente.nombreUsuario + ": " + e.getMessage());
            }
        }
    }
      public void difundirMensaje(Mensaje mensaje) {
        if (mensaje.getTipo() == TipoMensaje.PRIVADO) {
            String destinatario = mensaje.getDestinatario();
            GestorCliente clienteDestinatario = clientesConectados.get(destinatario);
            
            if (clienteDestinatario != null) {
                try {
                    clienteDestinatario.enviarMensaje(mensaje);
                    GestorCliente clienteRemitente = clientesConectados.get(mensaje.getRemitente());
                    if (clienteRemitente != null && !clienteRemitente.equals(clienteDestinatario)) {
                        clienteRemitente.enviarMensaje(mensaje);
                    }
                } catch (IOException e) {
                    System.err.println("Error al enviar mensaje privado: " + e.getMessage());
                }
            }
        } else {
            for (GestorCliente cliente : clientesConectados.values()) {
                try {
                    cliente.enviarMensaje(mensaje);
                } catch (IOException e) {
                    System.err.println("Error al difundir mensaje: " + e.getMessage());
                }
            }
        }
    }
    
    public void eliminarCliente(String nombreUsuario) {
        GestorCliente cliente = clientesConectados.remove(nombreUsuario);
        if (cliente != null) {
            try {
                cliente.cerrarConexion();
            } catch (IOException e) {
                System.err.println("Error al cerrar conexión de cliente: " + e.getMessage());
            }
            
            if (listener != null) {
                listener.onClienteDesconectado(nombreUsuario);
            }
            
            Mensaje mensajeDesconexion = new Mensaje(nombreUsuario, "se ha desconectado", TipoMensaje.DESCONEXION);
            difundirMensaje(mensajeDesconexion);
            
            actualizarListaUsuariosParaTodos();
        }
    }
    
    public boolean expulsarCliente(String nombreUsuario) {
        GestorCliente cliente = clientesConectados.get(nombreUsuario);
        if (cliente != null) {
            try {
                Mensaje mensajeExpulsion = new Mensaje("Servidor", "Has sido expulsado del chat por el administrador", TipoMensaje.DESCONEXION);
                
                cliente.enviarMensaje(mensajeExpulsion);
                
                eliminarCliente(nombreUsuario);
                
                Mensaje mensajeNotificacion = new Mensaje("Servidor", nombreUsuario + " ha sido expulsado del chat", TipoMensaje.MENSAJE_TEXTO);
                difundirMensaje(mensajeNotificacion);
                
                return true;
            } catch (IOException e) {
                System.err.println("Error al expulsar al cliente " + nombreUsuario + ": " + e.getMessage());
                eliminarCliente(nombreUsuario);
            }
        }
        return false;
    }
    
    private class GestorCliente {
        private final String nombreUsuario;
        private final Socket socket;
        private final ObjectInputStream entrada;
        private final ObjectOutputStream salida;
        private boolean conectado;
        
        public GestorCliente(String nombreUsuario, Socket socket, ObjectInputStream entrada, ObjectOutputStream salida) {
            this.nombreUsuario = nombreUsuario;
            this.socket = socket;
            this.entrada = entrada;
            this.salida = salida;
            this.conectado = true;
        }
        
        public void iniciarRecepcion() {
            poolHilos.execute(() -> {
                while (conectado) {
                    try {
                        Mensaje mensaje = (Mensaje) entrada.readObject();
                        procesarMensaje(mensaje);
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("Error al recibir mensaje de " + nombreUsuario + ": " + e.getMessage());
                        conectado = false;
                        eliminarCliente(nombreUsuario);
                        break;
                    }
                }
            });
        }
        
        private void procesarMensaje(Mensaje mensaje) {
            if (listener != null) {
                listener.onMensajeRecibido(mensaje);
            }
            
            if (mensaje.getTipo() == TipoMensaje.DESCONEXION) {
                conectado = false;
                eliminarCliente(nombreUsuario);
            } else {
                difundirMensaje(mensaje);
            }
        }
        
        public void enviarMensaje(Mensaje mensaje) throws IOException {
            synchronized (this) {
                salida.reset();
                salida.writeObject(mensaje);
                salida.flush();
                System.out.println("Mensaje enviado a " + nombreUsuario + ": Tipo=" + mensaje.getTipo());
            }
        }
        
        public void cerrarConexion() throws IOException {
            conectado = false;
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null) socket.close();
        }
    }
    
    public interface ServidorListener {
        void onMensajeRecibido(Mensaje mensaje);
        void onClienteConectado(String nombreUsuario);
        void onClienteDesconectado(String nombreUsuario);
        void onListaUsuariosActualizada(String[] usuarios);
    }
    
    public static void main(String[] args) {
        Servidor servidor = new Servidor();
        servidor.iniciar();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Cerrando servidor...");
            servidor.detener();
        }));
    }
}
