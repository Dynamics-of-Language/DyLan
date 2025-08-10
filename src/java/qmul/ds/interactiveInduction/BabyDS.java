package qmul.ds.interactiveInduction;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class BabyDS {
    private static final int PORT = 5000;
    private static boolean running = true;


    private static void handleClient(Socket clientSocket) throws IOException {
        try (InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            List<String> instructionWords = new ArrayList<>();
            
            while (true) {
                int bytesRead = input.read(buffer);
                if (bytesRead == -1) break;
                
                String data = new String(buffer, 0, bytesRead);
                
                // Check for end of instruction
                if (data.contains("END_INSTR")) {
                    String result = parseInstruction(instructionWords);
                    output.write(result.getBytes());
                    instructionWords.clear();
                    break;
                } else {
                    instructionWords.add(data);
                }
            }
        }
    }

    /**
     * Parse the instruction and return the semantic representation.
     * This is a placeholder - you will replace this with your actual parsing logic.
     */
    private static String parseInstruction(List<String> words) {
        // TODO: Replace this with your actual parsing logic
        // This is just a placeholder that returns a dummy semantic representation
        StringBuilder instruction = new StringBuilder();
        for (String word : words) {
            instruction.append(word).append(" ");
        }
        
        // For now, return a simple dictionary-like string that the Python code can parse
        // You will replace this with your actual parsing logic
        return "{\"x\": {\"type\": \"key\", \"color\": \"green\", \"state\": \"facing\"}}";
    }

    /**
     * Gracefully shutdown the server
     */
    public static void shutdown() {
        running = false;
    }


    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("BabyDS Parser Server started on port " + PORT);

            while (running) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected from " + clientSocket.getInetAddress());
                    handleClient(clientSocket);
                } catch (IOException e) {
                    System.err.println("Error handling client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + PORT);
            System.exit(-1);
        }
    }
}