package ui;

import cliente.Cliente;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import modelo.Mensaje;

public class VentanaCliente extends JFrame implements Cliente.GestorEventos {
    private static final long serialVersionUID = 1L;
    
    private Cliente cliente;
    private JTextPane areaChat;    private JTextField campoMensaje;
    private JTextField campoIP;
    private JTextField campoPuerto;
    private JButton botonEnviar;
    private JButton botonArchivo;
    private JList<String> listaUsuarios;
    private DefaultListModel<String> modeloUsuarios;
    private JButton botonConectar;
    private JButton botonDesconectar;
    private JComboBox<String> comboDestinatario;
    private DefaultComboBoxModel<String> modeloDestinatarios;
    private SimpleDateFormat formatoHora;
    
    public VentanaCliente() {
        super("Zigna");
        formatoHora = new SimpleDateFormat("HH:mm:ss");
        inicializarComponentes();
        configurarEventos();
        
        try {
            ImageIcon iconoApp = new ImageIcon("icono.png");
            if (iconoApp != null && iconoApp.getIconWidth() > 0) {
                setIconImage(iconoApp.getImage());
            }
        } catch (Exception e) {
            System.out.println("No se pudo cargar el icono: " + e.getMessage());
        }
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void inicializarComponentes() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(5, 5));
        panelPrincipal.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel panelConexion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel etiquetaNombre = new JLabel("Nombre de usuario:");
        JTextField campoNombre = new JTextField(15);
        JLabel etiquetaIP = new JLabel("Servidor:");
        campoIP = new JTextField("", 10);
        JLabel etiquetaPuerto = new JLabel("Puerto:");
        campoPuerto = new JTextField("", 4);
        
        botonConectar = new JButton("Conectar");
        botonDesconectar = new JButton("Desconectar");
        botonDesconectar.setEnabled(false);
        
        panelConexion.add(etiquetaNombre);
        panelConexion.add(campoNombre);
        panelConexion.add(etiquetaIP);
        panelConexion.add(campoIP);
        panelConexion.add(etiquetaPuerto);
        panelConexion.add(campoPuerto);
        panelConexion.add(botonConectar);
        panelConexion.add(botonDesconectar);
        panelPrincipal.add(panelConexion, BorderLayout.NORTH);
        
        JSplitPane panelCentral = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        panelCentral.setResizeWeight(0.8);
        
        areaChat = new JTextPane();
        areaChat.setEditable(false);
        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setPreferredSize(new Dimension(600, 400));
        
        modeloUsuarios = new DefaultListModel<>();
        listaUsuarios = new JList<>(modeloUsuarios);
        listaUsuarios.setBorder(BorderFactory.createTitledBorder("Usuarios conectados"));
        JScrollPane scrollUsuarios = new JScrollPane(listaUsuarios);
        scrollUsuarios.setPreferredSize(new Dimension(150, 400));
        
        panelCentral.setLeftComponent(scrollChat);
        panelCentral.setRightComponent(scrollUsuarios);
        panelPrincipal.add(panelCentral, BorderLayout.CENTER);
        
        JPanel panelMensaje = new JPanel(new BorderLayout(5, 5));
        
        JPanel panelDestinatario = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel etiquetaDestinatario = new JLabel("Enviar a:");
        modeloDestinatarios = new DefaultComboBoxModel<>();
        modeloDestinatarios.addElement("Todos");
        comboDestinatario = new JComboBox<>(modeloDestinatarios);
        
        panelDestinatario.add(etiquetaDestinatario);
        panelDestinatario.add(comboDestinatario);
        panelMensaje.add(panelDestinatario, BorderLayout.NORTH);
        
        JPanel panelEnvio = new JPanel(new BorderLayout(5, 0));
        campoMensaje = new JTextField();
        botonEnviar = new JButton("Enviar");
        botonEnviar.setEnabled(false);
        botonArchivo = new JButton("Archivo");
        botonArchivo.setEnabled(false);
        
        JPanel panelBotones = new JPanel(new GridLayout(1, 2, 5, 0));
        panelBotones.add(botonArchivo);
        panelBotones.add(botonEnviar);
        
        panelEnvio.add(campoMensaje, BorderLayout.CENTER);
        panelEnvio.add(panelBotones, BorderLayout.EAST);
        panelMensaje.add(panelEnvio, BorderLayout.SOUTH);
        
        panelPrincipal.add(panelMensaje, BorderLayout.SOUTH);
        
        setContentPane(panelPrincipal);
        
        botonConectar.addActionListener(e -> {
            if (!campoNombre.getText().trim().isEmpty()) {
                conectar(campoNombre.getText().trim());
            } else {
                mostrarMensajeError("Ingrese un nombre de usuario");
            }
        });
        
        campoNombre.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    botonConectar.doClick();
                }
            }
        });
    }
    
    private void configurarEventos() {
        botonDesconectar.addActionListener(e -> {
            if (cliente != null && cliente.estaConectado()) {
                cliente.desconectar();
            }
        });
        
        botonEnviar.addActionListener(e -> enviarMensaje());
        
        campoMensaje.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enviarMensaje();
                }
            }
        });
        
        botonArchivo.addActionListener(e -> seleccionarArchivo());
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (cliente != null && cliente.estaConectado()) {
                    cliente.desconectar();
                }
            }
        });
    }
      
    private void conectar(String nombreUsuario) {
        String ipServidor = campoIP.getText().trim();
        String puertotexto = campoPuerto.getText().trim();
        int puerto;
        
        try {
            puerto = Integer.parseInt(puertotexto);
        } catch (NumberFormatException e) {
            mostrarMensajeError("El puerto debe ser un número válido");
            return;
        }
        
        cliente = new Cliente(nombreUsuario, ipServidor, puerto);
        cliente.setGestorEventos(this);
        
        if (cliente.conectar()) {
            botonConectar.setEnabled(false);
            botonDesconectar.setEnabled(true);
            botonEnviar.setEnabled(true);
            botonArchivo.setEnabled(true);
            campoMensaje.setEnabled(true);
            comboDestinatario.setEnabled(true);
            
            SwingUtilities.invokeLater(() -> {
                campoMensaje.requestFocus();
                setTitle("Zigna Chat - " + nombreUsuario + " (" + ipServidor + ":" + puerto + ")");
            });
        } else {
            cliente = null;
        }
    }
    
    private void enviarMensaje() {
        if (cliente == null || !cliente.estaConectado() || campoMensaje.getText().trim().isEmpty()) {
            return;
        }
        
        String destinatario = (String) comboDestinatario.getSelectedItem();
        String mensaje = campoMensaje.getText().trim();
        
        if ("Todos".equals(destinatario)) {
            cliente.enviarMensajeTexto(mensaje);
        } else {
            cliente.enviarMensajePrivado(destinatario, mensaje);
        }
        
        campoMensaje.setText("");
        campoMensaje.requestFocus();
    }
    
    private void seleccionarArchivo() {
        if (cliente == null || !cliente.estaConectado()) {
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        int resultado = fileChooser.showOpenDialog(this);
        
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            enviarArchivo(archivo);
        }
    }
      private void enviarArchivo(File archivo) {
        try {
            byte[] datosArchivo = new byte[(int) archivo.length()];
            FileInputStream fis = new FileInputStream(archivo);
            fis.read(datosArchivo);
            fis.close();
            
            String destinatario = (String) comboDestinatario.getSelectedItem();
            
            if ("Todos".equals(destinatario)) {
                cliente.enviarArchivo(archivo.getName(), datosArchivo);
                agregarMensajeChat("Has enviado un archivo a todos: " + archivo.getName(), new Color(0, 128, 0));
            } else {
                cliente.enviarArchivoPrivado(destinatario, archivo.getName(), datosArchivo);
                agregarMensajeChat("Has enviado un archivo privado a " + destinatario + ": " + archivo.getName(), new Color(0, 128, 0));
            }
        } catch (IOException e) {
            mostrarMensajeError("Error al leer el archivo: " + e.getMessage());
        }
    }
    
    private void agregarMensajeChat(String mensaje, Color color) {
        StyledDocument doc = areaChat.getStyledDocument();
        Style estilo = areaChat.addStyle("Estilo", null);
        StyleConstants.setForeground(estilo, color);
        
        String horaActual = formatoHora.format(new Date());
        String mensajeFormateado = "[" + horaActual + "] " + mensaje + "\n";
        
        try {
            doc.insertString(doc.getLength(), mensajeFormateado, estilo);
            areaChat.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            System.err.println("Error al agregar mensaje: " + e.getMessage());
        }
    }
    
    private void mostrarMensajeError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void guardarArchivo(String nombre, byte[] datos) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(nombre));
        int resultado = fileChooser.showSaveDialog(this);
        
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            try {
                FileOutputStream fos = new FileOutputStream(archivo);
                fos.write(datos);
                fos.close();
                agregarMensajeChat("Archivo guardado: " + archivo.getName(), Color.BLUE);
            } catch (IOException e) {
                mostrarMensajeError("Error al guardar el archivo: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        SwingUtilities.invokeLater(() -> {
            Color color = mensaje.getTipo() == Mensaje.TipoMensaje.PRIVADO ? Color.MAGENTA : Color.BLACK;
            agregarMensajeChat(mensaje.toString(), color);
        });
    }
      @Override
    public void onArchivoRecibido(Mensaje mensaje) {
        SwingUtilities.invokeLater(() -> {
            String textoMensaje;
            Color color;
            
            if (mensaje.getTipo() == Mensaje.TipoMensaje.PRIVADO) {
                if (mensaje.getRemitente().equals(cliente.getNombreUsuario())) {
                    textoMensaje = "[Privado a " + mensaje.getDestinatario() + "] Has enviado un archivo: " + mensaje.getNombreArchivo();
                } else {
                    textoMensaje = "[Privado] " + mensaje.getRemitente() + " te ha enviado un archivo: " + mensaje.getNombreArchivo();
                }
                color = new Color(128, 0, 128);
            } else {
                textoMensaje = mensaje.getRemitente() + " ha enviado un archivo: " + mensaje.getNombreArchivo();
                color = Color.BLUE;
            }            
            agregarMensajeChat(textoMensaje, color);
            
            if (!mensaje.getRemitente().equals(cliente.getNombreUsuario())) {
                int opcion = JOptionPane.showConfirmDialog(
                    this, 
                    "¿Desea guardar el archivo " + mensaje.getNombreArchivo() + "?", 
                    "Archivo recibido", 
                    JOptionPane.YES_NO_OPTION);
                    
                if (opcion == JOptionPane.YES_OPTION) {
                    guardarArchivo(mensaje.getNombreArchivo(), mensaje.getDatosArchivo());
                }
            }
        });
    }
    
    @Override
    public void onActualizacionUsuarios(Mensaje mensaje) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Recibida actualización de usuarios: " + mensaje);
            
            String mensajeFormateado;
            Color color;
            
            if (mensaje.getTipo() == Mensaje.TipoMensaje.CONEXION) {
                mensajeFormateado = "Usuario \"" + mensaje.getRemitente() + "\" se ha conectado al chat";
                color = new Color(0, 128, 0);
            } else {
                mensajeFormateado = "Usuario \"" + mensaje.getRemitente() + "\" se ha desconectado del chat";
                color = new Color(192, 0, 0);
            }
            
            agregarMensajeChat(mensajeFormateado, color);
        });
    }
      
    @Override
    public void onListaUsuarios(List<String> usuarios) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Actualizando lista de usuarios: " + usuarios);
            
            String seleccionActual = (String) comboDestinatario.getSelectedItem();
            
            modeloUsuarios.clear();
            modeloDestinatarios.removeAllElements();
            modeloDestinatarios.addElement("Todos");
            
            int contadorUsuarios = 0;
            for (String usuario : usuarios) {
                if (usuario != null && !usuario.isEmpty() && !usuario.equals(cliente.getNombreUsuario())) {
                    modeloUsuarios.addElement(usuario);
                    modeloDestinatarios.addElement(usuario);
                    contadorUsuarios++;
                }
            }
            
            if (seleccionActual != null) {
                for (int i = 0; i < modeloDestinatarios.getSize(); i++) {
                    if (seleccionActual.equals(modeloDestinatarios.getElementAt(i))) {
                        comboDestinatario.setSelectedIndex(i);
                        break;
                    }
                }            
            }
            
            System.out.println("Se agregaron " + contadorUsuarios + " usuarios a la lista");
        });
    }
    
    @Override
    public void onDesconexion() {
        SwingUtilities.invokeLater(() -> {
            botonConectar.setEnabled(true);
            botonDesconectar.setEnabled(false);
            botonEnviar.setEnabled(false);
            botonArchivo.setEnabled(false);
            campoMensaje.setEnabled(false);
            comboDestinatario.setEnabled(false);
            modeloUsuarios.clear();
            setTitle("Zigna Chat");
        });
    }
    
    @Override
    public void onError(String error) {
        SwingUtilities.invokeLater(() -> {
            mostrarMensajeError(error);
        });
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new VentanaCliente();
        });
    }
}
