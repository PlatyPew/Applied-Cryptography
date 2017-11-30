// package 
package AppliedCrypto;

// import the modules 
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.border.EmptyBorder;

// client graphical user interface 
public class ClientGUI extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    // global declarations  
    // display variables
    private JLabel label;                       // username            
    private JTextField tf;
    private JTextField tPin, tfServer, tfPort;  // pin, server address and port number 
    private JButton login, logout, whoIsIn;     // navigation 
    private JTextArea ta;                       // chat room 
    private boolean connected;                  // connection status 

    // process variables 
    private Client client;                      // client object 
    private int defaultPort;                    // default socket 
    private String defaultHost;

    // function: receive socket number 
    ClientGUI(String host, int port) {

        // front-end display (window)  
        super("Chat Client");
        defaultPort = port;
        defaultHost = host;

        // front-end display (north panel) 
        JPanel northPanel = new JPanel(new GridLayout(3, 1));
        northPanel.setBackground(Color.decode("#d6dbdf"));
        add(northPanel, BorderLayout.NORTH);

        // front-end display (north panel - title)
        JPanel title = new JPanel(new GridLayout(1, 1));
        title.setBackground(Color.decode("#0088cc"));
        JLabel titleL = new JLabel("WELCOME TO THE CLIENT", SwingConstants.CENTER);
        titleL.setForeground(Color.decode("#ffffff"));
        title.add(titleL);
        northPanel.add(title);

        // front-end display (north panel - serverAndPort) 
        JPanel serverAndPort = new JPanel(new GridLayout(1, 3, 1, 1));
        serverAndPort.setBackground(Color.decode("#0088cc"));
        serverAndPort.setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, Color.decode("#5d6d7e")));

        // server address 
        JLabel serverAddL = new JLabel("SERVER", SwingConstants.CENTER);
        serverAddL.setForeground(Color.decode("#ffffff"));
        serverAndPort.add(serverAddL);
        tfServer = new JTextField(host);
        serverAndPort.add(tfServer);

        // port number 
        JLabel portNoL = new JLabel("PORT", SwingConstants.CENTER);
        portNoL.setForeground(Color.decode("#ffffff"));
        serverAndPort.add(portNoL);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.LEFT);
        serverAndPort.add(tfPort);

        // pin 
        JLabel pinL = new JLabel("PIN", SwingConstants.CENTER);
        pinL.setForeground(Color.decode("#ffffff"));
        serverAndPort.add(pinL);
        tPin = new JTextField("             ");
        tPin.setHorizontalAlignment(SwingConstants.LEFT);
        serverAndPort.add(tPin);
        northPanel.add(serverAndPort);

        // front-end display (north panel - username) 
        JPanel usernamePanel = new JPanel(new GridLayout(2, 1));
        usernamePanel.setBackground(Color.decode("#85c1e9"));
        usernamePanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        label = new JLabel("Enter your username below", SwingConstants.CENTER);
        usernamePanel.add(label);
        tf = new JTextField("Anonymous");
        tf.setBackground(Color.WHITE);
        usernamePanel.add(tf);
        northPanel.add(usernamePanel);

        // front-end display (center panel - chatroom) 
        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.setBackground(Color.decode("#85c1e9"));
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        ta = new JTextArea("CHAT ROOM\n", 80, 80);
        ta.setBorder(new EmptyBorder(10, 10, 10, 10));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);
        add(centerPanel, BorderLayout.CENTER);

        // front-end display (south panel)  
        JPanel southPanel = new JPanel();
        southPanel.setBackground(Color.decode("#0088cc"));
        add(southPanel, BorderLayout.SOUTH);

        // front-end display (south panel - buttons) 
        login = new JButton("LOGIN");
        login.setBackground(Color.decode("#5d6d7e"));
        login.setForeground(Color.WHITE);
        login.addActionListener(this);
        logout = new JButton("LOGOUT");
        logout.setBackground(Color.decode("#5d6d7e"));
        logout.setForeground(Color.WHITE);
        logout.addActionListener(this);
        logout.setEnabled(false);		// login --> logout 
        whoIsIn = new JButton("WHO IS IN");
        whoIsIn.setBackground(Color.decode("#5d6d7e"));
        whoIsIn.setForeground(Color.WHITE);
        whoIsIn.addActionListener(this);
        whoIsIn.setEnabled(false);		// login --> who is in
        southPanel.add(login);
        southPanel.add(logout);
        southPanel.add(whoIsIn);

        // front-end process (close window) 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        tf.requestFocus();

    }

    // function: front-end process (center panel - append text to chatroom) 
    void append(String str) {
        ta.append(str);
        ta.setCaretPosition(ta.getText().length() - 1);
    }

    // function: front-end process (south panel - reset variables) 
    void connectionFailed() {
        login.setEnabled(true);
        logout.setEnabled(false);
        whoIsIn.setEnabled(false);
        label.setText("Enter your username below");
        tf.setText("Anonymous");
        // reset port number and host name as a construction time
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        // let the user change them
        tfServer.setEditable(true);
        tfPort.setEditable(true);
        tPin.setEditable(true);
        // don't react to a <CR> after the username
        tf.removeActionListener(this);
        connected = false;
    }

    // function: front-end process (south panel - perform action) 
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        // logout button 
        try {
            if (o == logout) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                // enable back the fields 
                tfServer.setEditable(true);
                tfPort.setEditable(true);
                tPin.setEditable(true);
                return;
            }
            // who is in button 
            if (o == whoIsIn) {
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
                return;
            }

            // jText field 
            if (connected) {
                // send the message 
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, tf.getText()));
                tf.setText("");
                return;
            }
        } catch (Exception ex) {
            Logger.getLogger(ClientGUI.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (o == login) {
            // ok it is a connection request
            // ignore empty username 
            String username = tf.getText().trim();
            if (username.length() == 0) {
                return;
            }
            
            String pin = tPin.getText().trim();
            if (pin.length() == 0) {
                return;
            }

            // ignore empty server address
            String server = tfServer.getText().trim();
            if (server.length() == 0) {
                return;
            }

            // ignore empty / invalid port number 
            String portNumber = tfPort.getText().trim();
            if (portNumber.length() == 0) {
                return;
            }
            int port = 0;
            try {
                port = Integer.parseInt(portNumber);
            } catch (Exception en) {
                return;
            }

            // create new client with GUI 
            client = new Client(server, port, username, pin, this);
            // start the client 
            if (!client.start()) {
                return;
            }
            tf.setText("");
            label.setText("Enter your message below");
            connected = true;

            // disable login button
            login.setEnabled(false);
            // enable the two buttons
            logout.setEnabled(true);
            whoIsIn.setEnabled(true);
            // disable server and port JTextField  
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            // disable pin JTextField 
            tPin.setEditable(false);
            // Action listener for when the user enter a message
            tf.addActionListener(this);
        }

    }

    // main function 
    public static void main(String[] args) {
        new ClientGUI("localhost", 1500);
    }

}
