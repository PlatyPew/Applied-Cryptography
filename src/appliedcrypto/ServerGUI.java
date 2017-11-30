// package 
package AppliedCrypto;

// import the modules 
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.border.EmptyBorder;

// server graphical user interface 
public class ServerGUI extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 1L;

    // global declarations 
    // display variables 
    private JButton stopStart;
    private JTextArea chat, event;
    private JTextField tPin, tPortNumber;

    // process variables 
    private Server server;

    // function: receive socket number 
    ServerGUI(int port, String pin) {

        // front-end display (window) 
        super("Chat Server");
        server = null;

        // front-end display (north panel) 
        JPanel north = new JPanel(new GridLayout(2, 1));
        add(north, BorderLayout.NORTH);

        // front-end display (north panel - title)
        JPanel title = new JPanel(new GridLayout(1, 1));
        title.setBackground(Color.decode("#0088cc"));
        JLabel titleL = new JLabel("WELCOME TO THE SERVER", SwingConstants.CENTER);
        titleL.setForeground(Color.decode("#ffffff"));
        title.add(titleL);
        north.add(title);

        // front-end display (north panel - configurations
        JPanel config = new JPanel(new GridLayout(1, 3, 1, 1));
        config.setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, Color.decode("#5d6d7e")));
        config.setBackground(Color.decode("#0088cc"));

        // port number 
        JLabel portNoL = new JLabel("PORT", SwingConstants.CENTER);
        portNoL.setForeground(Color.decode("#ffffff"));
        config.add(portNoL);
        tPortNumber = new JTextField("  " + port);
        config.add(tPortNumber);

        // pin 
        JLabel pinL = new JLabel("PIN", SwingConstants.CENTER);
        pinL.setForeground(Color.decode("#ffffff"));
        config.add(pinL);
        tPin = new JTextField("  " + pin);
        config.add(tPin);

        // start/stop button 
        stopStart = new JButton("START");
        stopStart.setBackground(Color.decode("#5d6d7e"));
        stopStart.setForeground(Color.WHITE);
        stopStart.addActionListener(this);
        config.add(stopStart);
        north.add(config);

        // front-end display (center panel)
        JPanel center = new JPanel(new GridLayout(2, 1));
        center.setBackground(Color.decode("#85c1e9"));
        center.setBorder(new EmptyBorder(20, 20, 20, 20));

        // front-end display (center panel - chatroom) 
        chat = new JTextArea(80, 80);
        chat.setBackground(Color.decode("#ffffff"));
        chat.setBorder(new EmptyBorder(10, 10, 10, 10));
        chat.setEditable(false);
        appendRoom("CHAT ROOM\n");
        center.add(new JScrollPane(chat));

        // front-end display (center panel - events log) 
        event = new JTextArea(80, 80);
        event.setBackground(Color.decode("#ffffff"));
        event.setBorder(new EmptyBorder(10, 10, 10, 10));
        event.setEditable(false);
        appendEvent("EVENTS LOG\n");
        center.add(new JScrollPane(event));
        add(center);

        // front-end process (client close window notification) 
        addWindowListener(this);
        setSize(400, 600);
        setVisible(true);
    }

    // function: front-end process (center panel - append text to chatroom)
    void appendRoom(String str) {
        chat.append(str);
        chat.setCaretPosition(chat.getText().length() - 1);
    }

    // function: front-end process (center panel - append text to events log) 
    void appendEvent(String str) {
        event.append(str);
        event.setCaretPosition(chat.getText().length() - 1);

    }

    // function: front-end process (north panel - perform action) 
    public void actionPerformed(ActionEvent e) {
        // if running we have to stop
        if (server != null) {
            server.stop();
            server = null;
            tPortNumber.setEditable(true);
            tPin.setEditable(true);
            stopStart.setText("START");
            return;
        }

        // OK start the server
        int port;
        String pin = tPin.getText().trim();
        if (pin.length() == 0) {
            return;
        }
        try {
            port = Integer.parseInt(tPortNumber.getText().trim());
            pin = tPin.getText().trim();
        } catch (Exception er) {
            appendEvent("Invalid port number");
            return;
        }

        // ceate new server
        server = new Server(port, this, pin);
        new ServerRunning().start();
        stopStart.setText("STOP");
        tPortNumber.setEditable(false);
        tPin.setEditable(false);
    }

    // main function 
    public static void main(String[] arg) {
        // start server default port 1500
        new ServerGUI(1500, "1234");
    }

    // function: front-end process (client close window; end server connection with it)
    public void windowClosing(WindowEvent e) {
        // if server exist
        if (server != null) {
            try {
                server.stop();			// request server to close connection
            } catch (Exception eClose) {
            }
            server = null;
        }
        // dispose frame 
        dispose();
        System.exit(0);
    }

    // ignore other window listener functions
    public void windowClosed(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    // thread: server running thread 
    class ServerRunning extends Thread {

        public void run() {
            try {
                server.start();         // should execute until if fails
            } catch (Exception ex) {
                Logger.getLogger(ServerGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            // the server failed
            stopStart.setText("START");
            tPortNumber.setEditable(true);
            tPin.setEditable(true);
            appendEvent("Server crashed\n");
            server = null;
        }
    }

}
