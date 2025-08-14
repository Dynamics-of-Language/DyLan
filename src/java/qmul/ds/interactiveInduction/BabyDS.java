package qmul.ds.interactiveInduction;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import org.apache.log4j.Logger;
import qmul.ds.InteractiveContextParser;
import qmul.ds.formula.TTRRecordType;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BabyDS Parser Server
 * This server listens for incoming connections and processes instructions sent by the client.
 * It parses the instructions and returns a semantic representation.
 *
 * The server runs on port 5000 and expects instructions to be sent in a specific format.
 * Instructions should end with "END_INSTR" to signal the end of an instruction.
 */
public class BabyDS {
    private static final Logger logger = Logger.getLogger(BabyDS.class);
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";

    private static final int PORT = 5000;
    private static boolean running = true;
    static final String modelPath = "resource\\2025-babyds-RQ2\\forgetting\\".replace("\\", File.separator);  //the dir that works!
    
    /**
     * Set this string to process a direct instruction without socket communication.
     * If null or empty, the server will wait for socket input.
     */
    private static String directInstruction = "";


    /**
     * Sends all interpretations of the instruction to the client, separated by newlines.
     * @param clientSocket
     * @throws IOException
     */
    private static void handleClient(Socket clientSocket) throws IOException {
        try (InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            StringBuilder instructionBuilder = new StringBuilder();

            logger.info("Starting to handle client request");

            while (true) {
                int bytesRead = input.read(buffer);
                if (bytesRead == -1) break;

                String data = new String(buffer, 0, bytesRead);
                logger.debug("Received data chunk: " + data);

                // Accumulate all data
                instructionBuilder.append(data);

                // Check for end of instruction
                if (data.contains("END_INSTR")) {
                    try {
                        // Extract the instruction by removing "END_INSTR"
                        String fullInstruction = instructionBuilder.toString().replace("END_INSTR", "").trim();
                        logger.info("Complete instruction received: '" + fullInstruction + "'");

                        if (fullInstruction.isEmpty()) {
                            logger.warn("Empty instruction received");
                            StringBuilder response = new StringBuilder();
                            response.append("NO_INTERPRETATIONS");
                            output.write(response.toString().getBytes());
                            output.flush();
                            break;
                        }
                        InteractiveContextParser babydsParser = new InteractiveContextParser(modelPath, 3); //TODO
                        logger.info("BabyDS top-" + 3 + " actions loaded from: " + modelPath); //TODO

                        List<TTRRecordType> allSems = processInstruction(fullInstruction, babydsParser);
                        StringBuilder response = new StringBuilder();

                        // Handle case where parsing fails or returns empty list
                        if (allSems == null || allSems.isEmpty()) {
                            logger.warn("No semantic interpretations found for instruction");
                            response.append("NO_INTERPRETATIONS");
                        } else {
                            for (int i = 0; i < allSems.size(); i++) {
                                try {
                                    if (i > 0) {
                                        response.append("\t"); // Tab separator between interpretations
                                    }
                                    String semString;
                                    try {
                                        // Try to use toPythonDictString() if it exists
                                        semString = allSems.get(i).toPythonDictString(); //use unembed here?
                                        logger.debug("Using toPythonDictString() method");
                                    } catch (Exception e) {
                                        // Fallback to toString() if toPythonDictString() doesn't exist
                                        semString = allSems.get(i).toString();
                                        logger.debug("toPythonDictString() not available, using toString(): " + e.getMessage());
                                    }

                                    String edittedSem = removeCharFromEnd(semString, ',');
                                    edittedSem = "{'x1': {"+ edittedSem+"}}";
                                    logger.debug("Processed semantic interpretation: " + edittedSem);
                                    response.append(edittedSem);

                                } catch (Exception e) {
                                    logger.error("Error processing semantic interpretation " + i + ": " + e.getMessage());
                                    // Add a placeholder for this failed interpretation
                                    if (i > 0) {
                                        response.append("\t");
                                    }
                                    response.append("{'x1': {'error': 'failed_to_process'}}");
                                }
                            }
                        }

                        output.write(response.toString().getBytes());
                        output.flush(); // Ensure data is sent immediately

                    } catch (Exception e) {
                        logger.error("Critical error processing instruction: " + e.getMessage(), e);
                        // Send error response to prevent client from hanging
                        String errorResponse = "{'x1': {'error': 'server_error', 'message': '" + e.getMessage() + "'}}";
                        output.write(errorResponse.getBytes());
                        output.flush();
                    }

                    break;
                } else {
                    // Continue accumulating data
                    logger.debug("Continuing to accumulate instruction data");
                }
            }
        }
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


    /**
     * Tokenizes and standardizes instruction words according to predefined rules.
     *
     * Rules:
     * - "go to" -> "goto"
     * - "pick up" -> "pickup"
     * - "put X next to Y" -> "putnextto X Y"
     *
     * @param rawWords the raw instruction words
     * @return standardized list of words
     */
    private static List<String> tokenizeInstruction(List<String> rawWords) {
        List<String> standardized = new ArrayList<>();

        for (int i = 0; i < rawWords.size(); i++) {
            String currentWord = rawWords.get(i).toLowerCase();

            // Handle "go to" -> "goto"
            if (currentWord.equals("go") && i + 1 < rawWords.size()
                && rawWords.get(i + 1).toLowerCase().equals("to")) {
                standardized.add("goto");
                i++; // Skip the "to" word
                continue;
            }

            // Handle "pick up" -> "pickup"
            if (currentWord.equals("pick") && i + 1 < rawWords.size()
                && rawWords.get(i + 1).toLowerCase().equals("up")) {
                standardized.add("pickup");
                i++; // Skip the "up" word
                continue;
            }

            // Handle "put X next to Y" -> "putnextto X Y"
            if (currentWord.equals("put") && i + 3 < rawWords.size()
                && rawWords.get(i + 2).toLowerCase().equals("next")
                && rawWords.get(i + 3).toLowerCase().equals("to")) {
                standardized.add("putnextto");
                standardized.add(rawWords.get(i + 1)); // Add X (the object being moved)
                i += 3; // Skip "X next to", continue to add Y in next iteration
                continue;
            }

            // Default case: add the word as-is (but preserve original case for non-keywords)
            standardized.add(rawWords.get(i));
        }

        logger.debug("Tokenized instruction: " + String.join(" ", rawWords) + " -> " + String.join(" ", standardized));
        return standardized;
    }

    /**
     * Process a single instruction string without requiring socket communication.
     * This method is useful for testing and direct API usage.
     * 
     * @param instruction The instruction string to process
     * @return List of semantic interpretations, or empty list if parsing fails
     */
    public static List<TTRRecordType> processInstruction(String instruction, InteractiveContextParser babydsParser) {
        if (instruction == null || instruction.trim().isEmpty()) {
            logger.warn("Empty or null instruction provided");
            return new ArrayList<>();
        }

        // Split instruction words by whitespace
        String[] words = instruction.split("\\s+");
        List<String> rawWords = new ArrayList<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                rawWords.add(word);
            }
        }

        // Apply tokenization rules to standardize the instruction
        List<String> tokenizedWords = tokenizeInstruction(rawWords);
        logger.info("Raw instruction: " + String.join(" ", rawWords));
        logger.info("Tokenized instruction: " + String.join(" ", tokenizedWords));

        return parseInstruction(tokenizedWords, babydsParser);
    }

    /**
     * Parse the instruction and return the semantic representation.
     * This is the internal implementation used by both socket and direct processing paths.
     */
    private static List<TTRRecordType> parseInstruction(List<String> words, InteractiveContextParser babydsParser) {
        Sentence<Word> instruction = Sentence.toSentence(words);
        List<TTRRecordType> allInterpretations = new ArrayList<>();
        // InteractiveContextParser babydsParser;
        // babydsParser = new InteractiveContextParser(modelPath, 3); //TODO
        // logger.info("BabyDS top-" + 3 + " actions loaded from: " + modelPath);
        babydsParser.init();
        try {
            boolean isParsed = babydsParser.parse(instruction);
            if (!isParsed) {
                logger.error("Failed to parse instruction: " + instruction);
                return new ArrayList<>(); // Return empty list instead of null
            } else {
                TTRRecordType parsedSem = (TTRRecordType)babydsParser.getState().getCurrentTuple().getSemantics();
                allInterpretations.add(parsedSem);
                logger.debug("Looking for more possible interpretations...");
                int parseIdx = 0;
                while (true) {  // Steps through all different semantic interpretations.
                    try {
                        if (!babydsParser.parse()) {  // Check if parsing should continue
                            break;
                        }
                        logger.trace(ANSI_YELLOW + parseIdx + "- next interpretations: " + babydsParser.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
                        TTRRecordType newSem = (TTRRecordType) babydsParser.getState().getCurrentTuple().getSemantics();
                        parseIdx++;
                        if (!newSem.isIn(allInterpretations)) {
                            allInterpretations.add(newSem);
                            logger.debug(ANSI_GREEN + "New interpretation found: " + newSem + ANSI_RESET);
                        } else {
                            logger.debug("Already seen this interpretation: " + newSem);
                        }
                    } catch (Exception e) {
                        logger.warn(ANSI_RED + "Sem is probs DisjunctiveType? (or could be other problems): " + babydsParser.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing instruction: " + e.getMessage(), e);
            return new ArrayList<>(); // Return empty list instead of null
        }
        logger.info(ANSI_BLUE + "Number of unique interpretations: " + allInterpretations.size() + ANSI_RESET);
        for (TTRRecordType sem : allInterpretations) {
            logger.info("Semantics: " + sem);
        }
        // Remove the empty TTRRecordType that was being added - this might be causing issues
        // allInterpretations.add(0, new TTRRecordType());
        return allInterpretations;
    }


    /**
     * Shutdown the server
     */
    public static void shutdown() {
        running = false;
    }


    /**
     * Set the direct instruction to be processed without socket communication.
     * @param instruction The instruction to process, or null/empty to use socket mode
     */
    public static void setDirectInstruction(String instruction) {
        directInstruction = instruction;
    }

    public static void main(String[] args) {
        // Run in server mode (default)
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("BabyDS Parser Server started on port " + PORT);
            while (running) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected from " + clientSocket.getInetAddress());
                    handleClient(clientSocket);
                    System.out.println("Client request handled successfully");
                } catch (IOException e) {
                    System.err.println("Error handling client connection: " + e.getMessage());
                    logger.error("IOException in client handling: " + e.getMessage(), e);
                } catch (Exception e) {
                    System.err.println("Unexpected error handling client: " + e.getMessage());
                    logger.error("Unexpected error in client handling: " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + PORT);
            System.exit(-1);
        }
    }
}