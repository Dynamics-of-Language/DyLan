package qmul.ds.interactiveInduction;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;

public class SocketReceiver {

    protected static Logger logger = Logger.getLogger(SocketReceiver.class);


    public String receive() {
        int port = 12345;
        String receivedData = "";

        try (ServerSocket serverSocket = new ServerSocket(port);
            Socket socket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            logger.info("Socket opened. Waiting for data...");
            receivedData = in.readLine();
            logger.info("Received data from Python: " + receivedData);

            // Process the data as needed
        } catch (IOException e) {
            e.printStackTrace();
        }
        return receivedData;
    }

//    public static void main(String[] args) {
//        SocketReceiver socketReceiver = new SocketReceiver();
//        socketReceiver.receive();
//    }

}
