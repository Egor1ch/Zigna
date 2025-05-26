import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import modelo.Mensaje;
import servidor.Servidor;
import ui.VentanaCliente;
public class App {
    private static ImageIcon iconoApp;
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            cargarIcono();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            mostrarVentanaInicio();
        });
    }
    
    private static void cargarIcono() {
        iconoApp = new ImageIcon("icono.png");
    }
      private static void mostrarVentanaInicio() {
        JFrame ventana = new JFrame("Zigna");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Establecer el icono
        if (iconoApp != null) {
            ventana.setIconImage(iconoApp.getImage());
        }
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titulo = new JLabel("Menu Principal");
        titulo.setHorizontalAlignment(JLabel.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(18.0f));
        panel.add(titulo, BorderLayout.NORTH);
        
        JPanel panelBotones = new JPanel(new GridLayout(2, 1, 0, 10));
        
        JButton botonServidor = new JButton("Iniciar Servidor");
        botonServidor.setPreferredSize(new Dimension(200, 50));
        
        JButton botonCliente = new JButton("Entrar como cliente");
        botonCliente.setPreferredSize(new Dimension(200, 50));
        
        panelBotones.add(botonServidor);
        panelBotones.add(botonCliente);
        
        JPanel panelCentro = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelCentro.add(panelBotones);
        
        panel.add(panelCentro, BorderLayout.CENTER);
          botonServidor.addActionListener(e -> {
            ventana.dispose();
            iniciarServidor();
        });
          botonCliente.addActionListener(e -> {
            ventana.dispose();
            iniciarCliente();
        });
        
        ventana.setContentPane(panel);
        ventana.setSize(400, 250);
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
    }    private static void iniciarServidor() {
        JFrame ventanaServidor = new JFrame("Zigna");
        ventanaServidor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Establecer el icono
        if (iconoApp != null) {
            ventanaServidor.setIconImage(iconoApp.getImage());
        }
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
          JPanel panelSuperior = new JPanel(new BorderLayout(5, 5));
          JPanel panelCentral = new JPanel(new BorderLayout(10, 0));
        
        JTextPane areaMensajes = new JTextPane();
        areaMensajes.setEditable(false);
        JScrollPane scrollMensajes = new JScrollPane(areaMensajes);
        scrollMensajes.setPreferredSize(new Dimension(500, 400));
        scrollMensajes.setBorder(BorderFactory.createTitledBorder("Mensajes"));
        
        DefaultListModel<String> modeloUsuarios = new DefaultListModel<>();
        JList<String> listaUsuarios = new JList<>(modeloUsuarios);
        JScrollPane scrollUsuarios = new JScrollPane(listaUsuarios);
        scrollUsuarios.setPreferredSize(new Dimension(150, 400));
        scrollUsuarios.setBorder(BorderFactory.createTitledBorder("Usuarios Conectados"));        JPopupMenu menuContextual = new JPopupMenu();
        
        listaUsuarios.setComponentPopupMenu(menuContextual);
        
        JButton botonExpulsar = new JButton("Expulsar Usuario");
        botonExpulsar.setEnabled(false);
        
        JPanel panelUsuarios = new JPanel(new BorderLayout(5, 5));
        panelUsuarios.add(scrollUsuarios, BorderLayout.CENTER);
        panelUsuarios.add(botonExpulsar, BorderLayout.SOUTH);
        
        panelCentral.add(scrollMensajes, BorderLayout.CENTER);
        panelCentral.add(panelUsuarios, BorderLayout.EAST);
        
        JButton botonDetener = new JButton("Detener Servidor");
        
        panel.add(panelSuperior, BorderLayout.NORTH);
        panel.add(panelCentral, BorderLayout.CENTER);
        panel.add(botonDetener, BorderLayout.SOUTH);
        
        ventanaServidor.setContentPane(panel);
        ventanaServidor.setSize(700, 600);
        ventanaServidor.setLocationRelativeTo(null);
        ventanaServidor.setVisible(true);
        
        SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm:ss");
        
        class GestorMensajes {
            void agregarMensaje(String texto, Color color) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        StyledDocument doc = areaMensajes.getStyledDocument();
                        Style estilo = areaMensajes.addStyle("Estilo", null);
                        StyleConstants.setForeground(estilo, color);
                        
                        String horaActual = formatoHora.format(new Date());
                        String mensajeFormateado = "[" + horaActual + "] " + texto + "\n";
                        
                        doc.insertString(doc.getLength(), mensajeFormateado, estilo);
                        areaMensajes.setCaretPosition(doc.getLength());
                    } catch (BadLocationException e) {
                        System.err.println("Error al agregar mensaje: " + e.getMessage());
                    }
                });
            }
        }
        
        GestorMensajes gestorMensajes = new GestorMensajes();
        
        Servidor servidor = new Servidor();
        
        servidor.setListener(new Servidor.ServidorListener() {
            @Override
            public void onMensajeRecibido(Mensaje mensaje) {
                String textoMensaje;
                Color color;
                
                switch (mensaje.getTipo()) {
                    case MENSAJE_TEXTO:
                        textoMensaje = mensaje.getRemitente() + ": " + mensaje.getContenido();
                        color = Color.BLACK;
                        break;
                    case PRIVADO:
                        textoMensaje = "[Privado] " + mensaje.getRemitente() + " a " + 
                                      mensaje.getDestinatario() + ": " + mensaje.getContenido();
                        color = new Color(128, 0, 128);
                        break;
                    case ARCHIVO:
                        textoMensaje = mensaje.getRemitente() + " ha enviado un archivo: " + 
                                      mensaje.getNombreArchivo();
                        color = Color.BLUE;
                        break;
                    default:
                        return;
                }
                
                gestorMensajes.agregarMensaje(textoMensaje, color);
            }
            
            @Override
            public void onClienteConectado(String nombreUsuario) {
                gestorMensajes.agregarMensaje(
                    "Usuario \"" + nombreUsuario + "\" se ha conectado al chat", 
                    new Color(0, 128, 0)
                );
            }
            
            @Override
            public void onClienteDesconectado(String nombreUsuario) {
                gestorMensajes.agregarMensaje(
                    "Usuario \"" + nombreUsuario + "\" se ha desconectado del chat", 
                    new Color(192, 0, 0)
                );
            }
            
            @Override
            public void onListaUsuariosActualizada(String[] usuarios) {
                SwingUtilities.invokeLater(() -> {
                    modeloUsuarios.clear();
                    for (String usuario : usuarios) {
                        modeloUsuarios.addElement(usuario);
                    }
                });
            }
        });
        
        servidor.iniciar();
        gestorMensajes.agregarMensaje("Servidor iniciado en el puerto 5000", Color.BLUE);
        
        botonDetener.addActionListener(e -> {
            servidor.detener();
            gestorMensajes.agregarMensaje("Servidor detenido", Color.RED);
            
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    SwingUtilities.invokeLater(() -> {
                        ventanaServidor.dispose();
                        mostrarVentanaInicio();
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
        
        listaUsuarios.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listaUsuarios.getSelectedIndex() != -1) {
                botonExpulsar.setEnabled(true);
            } else {
                botonExpulsar.setEnabled(false);
            }
        });
        
        botonExpulsar.addActionListener(e -> {
            String usuarioSeleccionado = listaUsuarios.getSelectedValue();
            if (usuarioSeleccionado != null) {
                boolean expulsado = servidor.expulsarCliente(usuarioSeleccionado);
                if (expulsado) {
                    gestorMensajes.agregarMensaje(
                        "Usuario \"" + usuarioSeleccionado + "\" ha sido expulsado del chat", 
                        new Color(192, 0, 0)
                    );
                }
                listaUsuarios.clearSelection();
                botonExpulsar.setEnabled(false);            }
        });
        
        ventanaServidor.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                servidor.detener();
            }
        });
    }
    
    private static void iniciarCliente() {
        SwingUtilities.invokeLater(() -> {
            VentanaCliente cliente = new VentanaCliente();
            if (iconoApp != null) {
                cliente.setIconImage(iconoApp.getImage());
            }
        });
    }
    
    // Método para obtener el icono desde otras clases
    public static ImageIcon getIconoApp() {
        return iconoApp;
    }
}
