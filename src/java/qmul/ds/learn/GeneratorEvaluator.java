package qmul.ds.learn;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import org.apache.log4j.Logger;
import qmul.ds.InteractiveProbabilisticGenerator;
import qmul.ds.dag.UtteredWord;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class GeneratorEvaluator {

    //    static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
//    static public String corpusPath = corpusFolderPath + "amat.txt";//"AA-train-lower-396-unique-top1.txt";//"AAtrain-3.txt";//AA-full-lower-4000-Copy.txt";//""LC-CHILDESconversion400FinalCopy.txt";//"AA-train-lower-396-matching-top1.txt";//"AAtrain-3.txt";//"AA-train-lower-396-matching-top1.txt";//"AAtrain-3.txt"; //"AA-train-lower-396-matching-top1.txt";//"LC-CHILDESconversion800TestFinalCopy.txt"; //"LC-CHILDESconversion3200TrainFinalCopy.txt";//"AAtrain-72.txt"; //"LC-CHILDESconversion3200TrainFinalCopy.txt"; //"AAtrain-72.txt"; // "LC-CHILDESconversion396FinalCopy.txt"; // "LC-CHILDESconversion3200TrainFinal.txt"; //"LC-CHILDESconversion396FinalCopy.txt"; //"LC-CHILDESconversion400FinalCopy.txt";//"AAtrain-3.txt";//"CHILDESconversion100TestFinalCopy.txt";
    static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    protected static Logger logger = Logger.getLogger(GeneratorEvaluator.class);

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";

    InteractiveProbabilisticGenerator generator;

    public GeneratorEvaluator(String grammarPath, String modelPath) {
        generator = new InteractiveProbabilisticGenerator(grammarPath, modelPath);
    }

    public void evaluateOnPerturbations(String perturbationsFile) {
        // Load a generator.
        try {
            // Load data.
            List<PerturbationSample> data = PerturbationSample.loadPerturbationData(perturbationsFile);
            List<PerturbationSample> fullyGeneratedPrtbs = new ArrayList<>();
            List<PerturbationSample> partiallyGeneratedPrtbs = new ArrayList<>();
            List<String> fullyGeneratedSentences = new ArrayList<>();
            List<String> partiallyGeneratedSentences = new ArrayList<>();

            for (PerturbationSample sample : data) {
                generator.setRepairGeneration(true);
                generator.init();
                // Start generation with goal rG, until index pI is reached.
                logger.debug(ANSI_CYAN + "Processing perturbation sample: " + sample.toString() + ANSI_RESET);
                generator.setGoal(sample.rG);
                int currentIndex = 0;
                while (currentIndex <= sample.pI) {
                    generator.generateNextWord();
                    currentIndex++;
                }
                logger.debug(ANSI_PURPLE + "Perturbing goal to: " + sample.rP + ANSI_RESET);
                // When the index of the last word is reached, change goal to rP and continue generation.
                generator.setGoal(sample.rP);

                Sentence<Word> generatedSentence = new Sentence<>();


                if (generator.generate()) {
                    logger.debug(ANSI_CYAN + "Fully generated: " + sample + ANSI_RESET);
                    List<UtteredWord> sent = generator.getGenerated().getWords();
                    sent.forEach((w) -> generatedSentence.add(new Word(w.word())));
                    fullyGeneratedSentences.add(generatedSentence.toString());
                    fullyGeneratedPrtbs.add(sample);
                } else {
                    logger.debug(ANSI_RED + "Partially generated: " + sample + ANSI_RESET);
                    List<UtteredWord> sent = generator.getGenerated().getWords();
                    sent.forEach((w) -> generatedSentence.add(new Word(w.word())));
                    partiallyGeneratedPrtbs.add(sample);
                    partiallyGeneratedSentences.add(generatedSentence.toString());
                }
            }
            PerturbationSample.writeEvalOutputTofFile(fullyGeneratedPrtbs, "prtbFullyGenerated.txt", fullyGeneratedSentences);
            PerturbationSample.writeEvalOutputTofFile(partiallyGeneratedPrtbs, "prtbPartiallyGenerated.txt", partiallyGeneratedSentences);
            logger.info(ANSI_GREEN + "Count of fully generated perturbations: " + fullyGeneratedPrtbs.size() + ANSI_RESET);
            logger.info(ANSI_GREEN + "Count of partially generated perturbations: " + partiallyGeneratedPrtbs.size() + ANSI_RESET);
        } catch (IOException e) {
            throw new RuntimeException("Could not load perturbation data.");
        }
    }

    public void setRepairGeneration(boolean b) {
        generator.setRepairGeneration(b);
    }

    public static void main(String[] args) {
        GeneratorEvaluator evaluator = new GeneratorEvaluator(grammarPath, grammarPath);
        evaluator.evaluateOnPerturbations("prtbAll.txt");
    }
}
