package qmul.ds.interactiveInduction;//package qmul.ds.interactiveInduction;
//
//import java.io.*;
//import java.net.*;
//
//public class PyCommunicator {
//    private String host;
//    private int port;
//    private Socket socket;
//    private ServerSocket serverSocket;
//    private BufferedReader input;
//    private PrintWriter output;
//
//    public PyCommunicator(String host, int port) {
//        this.host = host;
//        this.port = port;
//    }
//
//    public void startServer() throws IOException {
//        serverSocket = new ServerSocket(port);
//        System.out.println("Server listening on " + host + ":" + port);
//        socket = serverSocket.accept();
//        System.out.println("Connected to client at " + socket.getInetAddress());
//        setupStreams();
//    }
//
//    public void connectToServer() throws IOException {
//        socket = new Socket(host, port);
//        System.out.println("Connected to server at " + host + ":" + port);
//        setupStreams();
//    }
//
//    private void setupStreams() throws IOException {
//        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//        output = new PrintWriter(socket.getOutputStream(), true);
//    }
//
//    public void sendMessage(String message) {
//        output.println(message);
//    }
//
//    public String receiveMessage() throws IOException {
//        return input.readLine();
//    }
//
//    public void close() {
//        try {
//            if (input != null) input.close();
//            if (output != null) output.close();
//            if (socket != null) socket.close();
//            if (serverSocket != null) serverSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void main(String[] args) {
//        // Example server usage:
//        /*
//        JavaSocket server = new JavaSocket("localhost", 12345);
//        try {
//            server.startServer();
//
//            while (true) {
//                try {
//                    // Wait for message
//                    String msg = server.receiveMessage();
//                    if (msg == null) break;  // Connection closed by client
//                    System.out.println("Received: " + msg);
//
//                    // Send response
//                    String response = "Message received: " + msg;
//                    server.sendMessage(response);
//                } catch (IOException e) {
//                    break;
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            server.close();
//        }
//        */
//
//        // Example client usage:
//
//        PyCommunicator client = new PyCommunicator("localhost", 12345);
//        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
//
//        try {
//            client.connectToServer();
//
//            while (true) {
//                try {
//                    // Get input from user
////                    System.out.print("Enter message (or 'quit' to exit): ");
////                    String message = consoleInput.readLine();
////                    if (message.equalsIgnoreCase("quit")) break;
//                    String message = "Hello from Java!";
//                    // Send message
//                    client.sendMessage(message);
//
//                    // Get response
//                    String response = client.receiveMessage();
//                    System.out.println("Server response: " + response);
//                } catch (IOException e) {
//                    break;
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            client.close();
//        }
//
//    }
//}


import qmul.ds.InteractiveContextParser;
import qmul.ds.dag.UtteredWord;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import qmul.ds.formula.TTRRecordType;

public class PyCommunicator {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";

    private static final Logger logger = Logger.getLogger(PyCommunicator.class);

    static final String modelPath = "resource\\2024_babyds_induction\\".replace("\\", File.separator);
    private static final int N = 1;  // topN learned actions to be used as model

    public static void main(String[] args) {
        InteractiveContextParser babyDS = new InteractiveContextParser(modelPath, N);
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server is listening on port 5000...");
            List<TTRRecordType> allSemantics = new ArrayList<>();
            String allInterpretations = "";
            // Accept client connection
            try (Socket socket = serverSocket.accept()) {
                babyDS.init();
                System.out.println("Client connected!");

                // Create input and output streams
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

                String inputLine;
                TTRRecordType sem = null;
                // Keep reading messages until client sends "exit"
                while ((inputLine = in.readLine()) != null) {
                    if ("END_INSTR".equalsIgnoreCase(inputLine)) {
                        logger.debug("Received end instr command!");
                        sem = (TTRRecordType) babyDS.getState().getCurrentTuple().getSemantics();
                        allSemantics.add(sem);
                        while (babyDS.parse()) {
                                try {
                                logger.debug(ANSI_YELLOW + "Other semantics: " + babyDS.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
                                TTRRecordType newSem = (TTRRecordType) babyDS.getState().getCurrentTuple().getSemantics();
                                if (!allSemantics.contains(newSem)) {
                                    allSemantics.add(newSem);
                                }
                            } catch (Exception e) {
                                logger.warn(ANSI_RED + "Sem is probs DisjunctiveType?: " + babyDS.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
                                break;
                            }
                        }
                        logger.debug("All semantics: " + allSemantics);
                        for (TTRRecordType s : allSemantics) {
                            logger.debug("Interpretation: " + s);
                            String semAsDict = s.toPythonDictString();
//                            logger.trace("Incomplete SemAsDict: " + semAsDict);
//                            String edittedSem = trimCharacter(semAsDict, ',');
                            String edittedSem = removeCharFromEnd(semAsDict, ',');

//                            logger.trace("Edited SemAsDict: " + edittedSem);
                            edittedSem = "{'x1': {"+ edittedSem+"}}";
                            allInterpretations += edittedSem + "\n";
                        }

                        // The older working code...
//                        String semAsDict = sem.toPythonDictString();
//                        logger.info("SemAsDict: " + semAsDict);
//                        String edittedSem  = semAsDict.trim().replaceFirst(".$","");
////                        String edittedSem = trimCharacter(semAsDict, ',');
//                        edittedSem = "{\"x1\": {"+ edittedSem+"}}";
//                        out.println(edittedSem);
                        out.println(allInterpretations);
                        logger.trace("Sent final semantics to Python.");
                        babyDS.init();
                        break;

                    }
                    logger.debug("Received: " + inputLine);
                    babyDS.parseWord(new UtteredWord(inputLine));
                    sem = (TTRRecordType) babyDS.getState().getCurrentTuple().getSemantics();
                    logger.debug("current semantics: " + sem);
                    // Process the received string (example: reverse it)
//                    String result = new StringBuilder(inputLine).reverse().toString();
//                    String result = sem;

                    // Send the processed result back
//                    out.println(sem);  // AA COMMENTED OUT!
                }
                // here is where I should step through...
//                allSemantics.add(sem);
//                while (babyDS.parse()) {
//                        try {
//                        logger.debug(ANSI_YELLOW + "Other semantics: " + babyDS.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
//                        TTRRecordType newSem = (TTRRecordType) babyDS.getState().getCurrentTuple().getSemantics();
//                        if (!allSemantics.contains(newSem)) {
//                            allSemantics.add(newSem);
//                        }
//                    } catch (Exception e) {
//                        logger.warn(ANSI_RED + "Sem is probs DisjunctiveType?: " + babyDS.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
//                        break;
//                    }
//                }
//                logger.debug("All semantics: " + allSemantics);
//                for (TTRRecordType s : allSemantics) {
//                    String semAsDict = s.toPythonDictString();
//                    String edittedSem = trimCharacter(semAsDict, ',');
//                    edittedSem = "{x1: {"+ edittedSem+"}}";
//                    allInterpretations += edittedSem + "\n";
//                }
//                logger.info("Final semantics: " + sem);
//                String semAsDict = sem.toPythonDictString();
//                String edittedSem = trimCharacter(semAsDict, ',');
//                edittedSem = "{x1: {"+ edittedSem+"}}";
//                out.println(allInterpretations);
                // Now have to empty the final semantics list for the next instruction...
                logger.trace("Sent final semantics to Python.");
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static String trimCharacter(String str, char ch) {
        String regex = "^" + ch + "+|" + ch + "+$";
        return str.replaceAll(regex, "");
        }

    public static String removeCharFromEnd(String str, char charToRemove) {
        str = str.trim();
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.charAt(str.length() - 1) == charToRemove) {
            return str.substring(0, str.length() - 1);
        }

        return str;
    }
}
