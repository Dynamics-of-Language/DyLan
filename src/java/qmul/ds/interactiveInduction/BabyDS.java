//package qmul.ds.interactiveInduction;
//
//import org.apache.log4j.Logger;
//import qmul.ds.InteractiveContextParser;
//import qmul.ds.Utterance;
//import qmul.ds.dag.UtteredWord;
//import qmul.ds.formula.TTRRecordType;
//
//import java.io.File;
//
//
///**
// * A class to load the learned BabyDS model, receive BabyAI instructions from Python, parse them, and send the
// * semantics back to Python.
// * @author: Arash A.
// */
//public class BabyDS {
//
//    private static final Logger logger = Logger.getLogger(BabyDS.class);
//    public static final String ANSI_RESET = "\u001B[0m";
//    public static final String ANSI_GREEN = "\u001B[32m";
//    public static final String ANSI_YELLOW = "\u001B[33m";
//    public static final String ANSI_BLUE = "\u001B[34m";
//    public static final String ANSI_PURPLE = "\u001B[35m";
//    public static final String ANSI_CYAN = "\u001B[36m";
//    public static final String ANSI_RED = "\u001B[31m";
//
//    static final String modelPath = "resource\\2024_babyds_induction\\".replace("\\", File.separator);
//
//    private static final int N = 3;  // topN learned actions to be used as model
//
//    public static void main(String[] args) {
//        PyCommunicator babyDSClient = new PyCommunicator("localhost", 12345);
//        InteractiveContextParser babyDS = new InteractiveContextParser(modelPath, N);  // The learned parsing model.
//        // I assume that the best model has been chosen based on the metrics in RQ1,
//        // so here I just load it.
//        logger.info("Loaded BabyDS successfully with topN=" + N + " actions.");
//        try {
//            babyDSClient.connectToServer();
//            logger.info("Connected to Python.");
////            String m = babyDSClient.receiveMessage();
////            System.out.println((ANSI_GREEN + m + ANSI_RESET));
//            babyDSClient.sendMessage("Hello from DS!");
//            String w = babyDSClient.receiveMessage();
//            System.out.println((ANSI_GREEN + w + ANSI_RESET));
////            logger.info("Connected to Python.");
////            while (true) {
////                TTRRecordType instructionSem;
//                String instruction = "";
//                String word;
//                babyDS.init();
//                logger.trace("BabyDS initialised.");
//                // Receive the instruction from Python word by word:
//                do {
////                    babyDSClient.sendMessage("next please!");
//                    word = babyDSClient.receiveMessage();
//                    logger.debug("Received word: " + word);
//                    babyDS.parseWord(new UtteredWord(word));  // do word by word
////                    if (isParsed) {
//                    logger.trace("Parsing word: " + word);
////                    TTRRecordType parsedSem = (TTRRecordType) babyDS.getState().getCurrentTuple().getSemantics();
////                        logger.debug("Semantics: " + parsedSem);
////                        babyDSClient.sendMessage(parsedSem.toString());
////                } else {
////                    logger.error("Failed to parse the instruction: " + instruction);
////                    babyDSClient.sendMessage("Failed to parse the instruction: " + instruction);
////                }
//                    instruction += word + " ";
//                } while (!word.equals("END_INSTR"));
////                String instructionWords = babyDSClient.receiveMessage();
//                // TODO figure out how to handle mis-parsed instructions...
//                logger.debug("Received instruction: " + instruction);
//                TTRRecordType instructionSem = babyDS.getFinalSemantics().removeHead();
//                logger.debug("semantics is: " + instructionSem);
//                babyDSClient.sendMessage(instructionSem.toString());
//                logger.debug("Sent semantics back to Python.");
//                // Parse the message and send the semantics back to Python:
////                babyDS.init();
////                boolean isParsed = babyDS.parseUtterance(new Utterance(instruction));
////                if (isParsed) {
////                    TTRRecordType parsedSem = (TTRRecordType) babyDS.getState().getCurrentTuple().getSemantics();
////                    logger.debug("Semantics: " + parsedSem);
////                    babyDSClient.sendMessage(parsedSem.toString());
////                } else {
////                    logger.error("Failed to parse the instruction: " + instruction);
//////                    babyDSClient.sendMessage("Failed to parse the instruction: " + instruction);
////                }
////            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
