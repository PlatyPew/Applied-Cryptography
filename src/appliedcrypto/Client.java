package AppliedCrypto;

import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.*;
import javax.crypto.spec.*;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client {

    // for I/O
    private ObjectInputStream sInput;		// to read from the socket
    private ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;

    // if I use a GUI or not
    private ClientGUI cg;

    // the server, the port and the username
    private String server, username, pin;
    private int port;
    private SimpleDateFormat sdf;
    private SimpleDateFormat timeStamp;
    SecretKey newAESKey;
    PrivateKey privateKey;
    PublicKey publicKey;

    /*
	 *  Constructor called by console mode
	 *  server: the server address
	 *  port: the port number
	 *  username: the username
     */
    Client(String server, int port, String username, String pin) {
        // which calls the common constructor with the GUI set to null
        this(server, port, username, pin, null);
    }

    /*
	 * Constructor call when used from a GUI
	 * in console mode the ClienGUI parameter is null
     */
    Client(String server, int port, String username, String pin, ClientGUI cg) {
        this.server = server;
        this.port = port;
        this.username = username;
        this.pin = pin;
        // save if we are in GUI mode or not
        this.cg = cg;
        sdf = new SimpleDateFormat("HH:mm:ss");
        timeStamp = new SimpleDateFormat("HH:mm");
    }

    /*
	 * To start the dialog
     */
    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        } // if it failed not much I can so
        catch (Exception ec) {
            display("No server available at that address or port");
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        /* Creating both Data Stream */
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams");
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects
        try {
            // Send initialising things to the server.
            // Sends public key, signed data and data
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048); // KeySize
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
            byte[] data = "Please let me connect".getBytes();
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data);
            byte[] signedData = signature.sign();

            sOutput.writeObject(publicKey);
            sOutput.writeObject(signedData);
            sOutput.writeObject(data);

        } catch (Exception eIO) {
            display("Exception doing login");
            disconnect();
            return false;
        }
        // success we inform the caller that it worked
        return true;
    }

    /*
	 * To send a message to the console or the GUI
     */
    private void display(String msg) {
        String time = sdf.format(new Date());
        if (cg == null) {
            System.out.println(time + ": " + msg);      // println in console mode
        } else {
            cg.append(time + ": " + msg + "\n");		// append to the ClientGUI JTextArea (or whatever)
        }
    }

    /*
	 * To send a message to the server
     */
    void sendMessage(ChatMessage msg) throws Exception {
        String otp = timeStamp.format(new Date());
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(msg);
            so.flush();
            byte[] byteMsg = bo.toByteArray();
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, newAESKey);
            byte[] encMsg = aesCipher.doFinal(byteMsg);
            byte[] data = "Am I legit?".getBytes();
            byte[] timeData = new byte[data.length + otp.getBytes().length];
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(timeData);
            byte[] signedData = signature.sign();
            sOutput.writeObject(signedData);
            sOutput.writeObject(data);
            sOutput.writeObject(encMsg);
        } catch (IOException e) {
            display("Exception writing to server");
        }
    }

    /*
	 * When something goes wrong
	 * Close the Input/Output streams and disconnect not much to do in the catch clause
     */
    private void disconnect() {
        try {
            if (sInput != null) {
                sInput.close();
            }
        } catch (Exception e) {
        } // not much else I can do
        try {
            if (sOutput != null) {
                sOutput.close();
            }
        } catch (Exception e) {
        } // not much else I can do
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
        } // not much else I can do

        // inform the GUI
        if (cg != null) {
            cg.connectionFailed();
        }

    }

    /*
	 * To start the Client in console mode use one of the following command
	 * > java Client
	 * > java Client username
	 * > java Client username portNumber
	 * > java Client username portNumber serverAddress
	 * at the console prompt
	 * If the portNumber is not specified 1500 is used
	 * If the serverAddress is not specified "localHost" is used
	 * If the username is not specified "Anonymous" is used
	 * > java Client
	 * is equivalent to
	 * > java Client Anonymous 1500 localhost
	 * are eqquivalent
	 *
	 * In console mode, if an error occurs the program simply stops
	 * when a GUI id used, the GUI is informed of the disconnection
     */
    public static void main(String[] args) throws Exception {
        // default values
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Anonymous";
        String pin = "1234";
        // depending of the number of arguments provided we fall through
        switch (args.length) {
            // > javac Client username portNumber serverAddr
            case 3:
                serverAddress = args[2];
            // > javac Client username portNumber
            case 2:
                try {
                    portNumber = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
            // > javac Client username
            case 1:
                userName = args[0];
            // > java Client
            case 0:
                break;
            // invalid number of arguments
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName, pin);
        // test if we can start the connection to the Server
        // if it failed nothing we can do
        if (!client.start()) {
            return;
        }

        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while (true) {

            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if (msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                // break to do the disconnect
                break;
            } // message WhoIsIn
            else if (msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            } else {				// default to ordinary message
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        // done disconnect
        client.disconnect();
    }

    /*
	 * a class that waits for the message from the server and append them to the JTextArea
	 * if we have a GUI or simply System.out.println() it in console mode
     */
    class ListenFromServer extends Thread {

        public void run() {
            boolean setup = true;
            while (true) {
                try {
                    if (setup) {
                        // Verifying server's public key
                        PublicKey serverPub = (PublicKey) sInput.readObject();
                        byte[] serverSignedData = (byte[]) sInput.readObject();
                        byte[] serverData = (byte[]) sInput.readObject();
                        Signature signature = Signature.getInstance("SHA256withRSA");
                        signature.initVerify(serverPub);
                        signature.update(serverData);
                        if (signature.verify(serverSignedData)) {
                            // Encrypt pin with server pub
                            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                            cipher.init(Cipher.ENCRYPT_MODE, serverPub);
                            byte[] encPin = cipher.doFinal(pin.getBytes());
                            sOutput.writeObject(encPin);
                            
                            // Encrypt username with server pub
                            byte[] encUser = cipher.doFinal(username.getBytes());
                            sOutput.writeObject(encUser);

                            // Check if authenticated using private key
                            byte[] encAuth = (byte[]) sInput.readObject();
                            cipher.init(Cipher.DECRYPT_MODE, privateKey);
                            String auth = new String(cipher.doFinal(encAuth));
                            if (auth.equals("True")) {
                                byte[] encAESKey = (byte[]) sInput.readObject();
                                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                                byte[] AESKey = cipher.doFinal(encAESKey);
                                newAESKey = new SecretKeySpec(AESKey, 0, AESKey.length, "AES");
                                display("Successfully connected");
                                display("\nRULES OF CONDUCT\n 1) No Vulgarities\n 2) No Spamming\n");
                            } else {
                                // Prints error message
                                disconnect();
                                display(auth);
                                break;
                            }
                        } else {
                            display("Something went wrong");
                        }
                        setup = false;
                    } else {
                        byte[] encMsg = (byte[]) sInput.readObject();
                        Cipher cipherAES = Cipher.getInstance("AES");
                        cipherAES.init(Cipher.DECRYPT_MODE, newAESKey);
                        String msg = new String(cipherAES.doFinal(encMsg));
                        
                        if (cg == null) {
                            System.out.println(msg);
                            System.out.print("> ");
                        } else {
                            cg.append(msg);
                        }
                    }

                } catch (IOException e) {
                    display("You have been disconnected");
                    if (cg != null) {
                        cg.connectionFailed();
                    }
                    break;
                } // can't happen with a String object but need the catch anyhow
                catch (Exception ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
