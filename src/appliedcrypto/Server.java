/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AppliedCrypto;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.*;
import java.security.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
    // a unique ID for each connection

    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> al;
    // if I am in a GUI
    private ServerGUI sg;
    // to display time
    private SimpleDateFormat sdf;
    private SimpleDateFormat timeStamp;
    // the port number to listen for connection
    private int port;
    // the pin 
    private String pin;
    // the boolean that will be turned of to stop the server
    private boolean keepGoing;

    Key AESKey;
    PrivateKey privateKey;
    PublicKey publicKey;

    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;

    /*
	 *  server constructor that receive the port to listen to for connection as parameter
	 *  in console
     */
    public Server(int port, String pin) {
        this(port, null, pin);
    }

    public Server(int port, ServerGUI sg, String pin) {
        // GUI or not
        this.sg = sg;
        // the port
        this.port = port;
        // pin 
        this.pin = pin;
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        timeStamp = new SimpleDateFormat("HH:mm");
        // ArrayList for the Client list
        al = new ArrayList<ClientThread>();
    }

    public void start() throws Exception {
        display("Server started!");
        Socket socket = null;
        keepGoing = true;
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        AESKey = keyGenerator.generateKey();

        /*Create RSA keys*/
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // KeySize
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        /* create socket server and wait for connection requests */
        try {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while (keepGoing) {
                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                socket = serverSocket.accept();  	// accept connection

                if (!keepGoing) {
                    break;
                }
                ClientThread t = new ClientThread(socket);  // make a thread of it
                al.add(t);									// save it in the ArrayList
                t.start();
            }

            try {
                sInput = new ObjectInputStream(socket.getInputStream());
                sOutput = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException eIO) {
                display("Exception creating new Input/output Streams: " + eIO);
            }
            // I was asked to stop
            try {
                serverSocket.close();
                for (int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                        // not much I can do
                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        } // something went bad
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    /*
     * For the GUI to stop the server
     */
    protected void stop() {
        keepGoing = false;
        // connect to myself as Client to exit statement
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
            // nothing I can really do
        }
    }

    /*
	 * Display an event (not a message) to the console or the GUI
     */
    private void display(String msg) throws Exception {
        String time = sdf.format(new Date());
        if (sg == null) {
            System.out.println(time + ": " + msg);
        } else {
            sg.appendEvent(time + ": " + msg + "\n");
        }
    }

    /*
	 *  to broadcast a message to all Clients
     */
    private synchronized void broadcast(String message) throws Exception {
        // add HH:mm:ss and \n to the message
        String time = sdf.format(new Date());
        String messageLf = time + " " + message + "\n";
        // display message on console or GUI
        if (sg == null) {
            System.out.print(messageLf);
        } else {
            sg.appendRoom(messageLf);     // append in the room window
        }
        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for (int i = al.size(); --i >= 0;) {
            ClientThread ct = al.get(i);
            // try to write to the Client if it fails remove it from the list
            if (!ct.writeMsg(messageLf)) {
                al.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
        for (int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // found it
            if (ct.id == id) {
                al.remove(i);
                return;
            }
        }
    }

    /*
	 *  To run as a console application just open a console window and:
	 * > java Server
	 * > java Server portNumber
	 * If the port number is not specified 1500 is used
     */
    public static void main(String[] args) throws Exception {
        // start server on port 1500 unless a PortNumber is specified
        int portNumber = 1500;
        String pin = "";
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        // create a server object and start it
        Server server = new Server(portNumber, pin);
        server.start();
    }

    /**
     * One instance of this thread will run for each client
     */
    class ClientThread extends Thread {
        // the socket where to listen/talk

        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (easier for deconnection)
        int id;
        // the Username of the Client
        String username;
        // the only type of message a will receive
        ChatMessage cm;
        // the date I connect
        String date;
        PublicKey clientPub; 
        // Constructore
        ClientThread(Socket socket) throws Exception {
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
            /* Creating both Data Stream */
            System.out.println("Thread trying to create Object Input/Output Streams");
            
            try {
                // create output first
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
           
                clientPub = (PublicKey) sInput.readObject();
                byte[] clientSignedData = (byte[]) sInput.readObject();
                byte[] clientData = (byte[]) sInput.readObject();

                //Verify digital signature
                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initVerify(clientPub);
                signature.update(clientData);
                if (signature.verify(clientSignedData)) {
                    byte[] data = "Data received".getBytes();
                    signature.initSign(privateKey);
                    signature.update(data);
                    byte[] signedData = signature.sign();
                    sOutput.writeObject(publicKey);
                    sOutput.writeObject(signedData);
                    sOutput.writeObject(data);
                    
                    // Decrypt pin with private key
                    byte[] encPin = (byte[]) sInput.readObject();
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    cipher.init(Cipher.DECRYPT_MODE, privateKey);
                    String cPin = new String(cipher.doFinal(encPin));
                    
                    // Decrypt username
                    byte[] encUser = (byte[]) sInput.readObject();
                    username = new String(cipher.doFinal(encUser));
                    if (cPin.equals(pin)) {
                        cipher.init(Cipher.ENCRYPT_MODE, clientPub);
                        boolean used = false;
                        for (int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            try {
                                if (ct.username.equals(username)) {
                                    used = true;
                                }
                            } catch (Exception ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        
                        if (used) {
                            byte[] encAuth = cipher.doFinal(("Username " + username + " is taken!").getBytes());
                            sOutput.writeObject(encAuth);
                        } else {
                            byte[] encAuth = cipher.doFinal("True".getBytes());
                            sOutput.writeObject(encAuth);
                            byte[] encAESKey = cipher.doFinal(AESKey.getEncoded());
                            sOutput.writeObject(encAESKey);
                            display(username + " just connected.");
                        }

                        
                    } else {
                        cipher.init(Cipher.ENCRYPT_MODE, clientPub);
                        byte[] encAuth = cipher.doFinal("Wrong pin".getBytes());
                        sOutput.writeObject(encAuth);
                    }
                } else {
                    display("Possible man in the middle attack detected");
                }

            } catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            } // have to catch ClassNotFoundException
            // but I read a String, I am sure it will work
            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        // what will run forever
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            String message = "";
            int type;
            int spamCount = 0;
            while (keepGoing) {
                // read a String (which is an object)
                try {
                    long startTime = System.currentTimeMillis();
                    byte[] clientSignedData = (byte[])sInput.readObject();
                    byte[] clientData = (byte[])sInput.readObject();
                    byte[] encMessage = (byte[])sInput.readObject();
                    long endTime = System.currentTimeMillis();
                    long totalTime = endTime - startTime;
                    if (totalTime < 1000) {
                        spamCount++;
                        if (spamCount == 5) {
                            broadcast(username + " will be kicked if spamming continues");
                        }
                    } else {
                        spamCount = 0;
                    }
                    Signature signature = Signature.getInstance("SHA256withRSA");
                    signature.initVerify(clientPub);
                    signature.update(new byte[clientData.length + timeStamp.format(new Date()).getBytes().length]);
                    if (!signature.verify(clientSignedData)) {
                        display("User: " + username + " cert has changed.");
                        break;
                    }
                    if (spamCount < 10) {
                        Cipher aesCipher = Cipher.getInstance("AES");
                        aesCipher.init(Cipher.DECRYPT_MODE, AESKey);

                        byte[] byteMsg = aesCipher.doFinal(encMessage);
                        ByteArrayInputStream bi = new ByteArrayInputStream(byteMsg);
                        ObjectInputStream si = new ObjectInputStream(bi);
                        ChatMessage cm = (ChatMessage) si.readObject();
                        message = cm.getMessage();
                        type = cm.getType();
                    } else {
                        broadcast(username + " has been kicked for spamming");
                        type = 2;
                    }
                } catch (IOException e) {
                    try {
                        display(username + " Exception reading Streams: " + e);
                    } catch (Exception ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                } catch (Exception e2) {
                    break;
                }
                // the messaage part of the ChatMessage

                // Switch on the type of message receive
                switch (type) {

                    case ChatMessage.MESSAGE: {
                        try {
                            if (badWord(message)) {
                                broadcast(username + ": has been censored.");
                            } else {
                                broadcast(username + ": " + message);
                            }
                            
                        } catch (Exception ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    break;
                    case ChatMessage.LOGOUT: {
                        try {
                            display(username + " disconnected with a LOGOUT message.");
                        } catch (Exception ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    keepGoing = false;
                    break;
                    case ChatMessage.WHOISIN: {
                        try {
                            writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        } catch (Exception ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    // scan al the users connected
                    for (int i = 0; i < al.size(); ++i) {
                        ClientThread ct = al.get(i);
                        try {
                            writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
                        } catch (Exception ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    break;
                }
            }
            // remove myself from the arrayList containing the list of the
            // connected Clients
            remove(id);
            close();
        }

        // try to close everything
        private void close() {
            // try to close the connection
            try {
                if (sOutput != null) {
                    sOutput.close();
                }
            } catch (Exception e) {
            }
            try {
                if (sInput != null) {
                    sInput.close();
                }
            } catch (Exception e) {
            };
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
            }
        }

        /*
		 * Write a String to the Client output stream
         */
        private boolean writeMsg(String msg) throws Exception {
            // if Client is still connected send the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream

            try {
                Cipher cipherAES = Cipher.getInstance("AES");
                cipherAES.init(Cipher.ENCRYPT_MODE, AESKey);
                byte[] encMsg = cipherAES.doFinal(msg.getBytes());
                sOutput.writeObject(encMsg);
            } // if an error occurs, do not abort just inform the user
            catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
    
    private boolean badWord(String msg) {
        // Naughty words
        String[] bad = {"fuck", "shit", "gay"};
        
        for (String bad1 : bad) {
            if (msg.toLowerCase().contains(bad1.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
