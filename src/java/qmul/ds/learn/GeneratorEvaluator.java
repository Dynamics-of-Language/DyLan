package qmul.ds.learn;

import qmul.ds.InteractiveProbabilisticGenerator;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;

public class GeneratorEvaluator {

    static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static public String corpusPath = corpusFolderPath + "amat.txt";//"AA-train-lower-396-unique-top1.txt";//"AAtrain-3.txt";//AA-full-lower-4000-Copy.txt";//""LC-CHILDESconversion400FinalCopy.txt";//"AA-train-lower-396-matching-top1.txt";//"AAtrain-3.txt";//"AA-train-lower-396-matching-top1.txt";//"AAtrain-3.txt"; //"AA-train-lower-396-matching-top1.txt";//"LC-CHILDESconversion800TestFinalCopy.txt"; //"LC-CHILDESconversion3200TrainFinalCopy.txt";//"AAtrain-72.txt"; //"LC-CHILDESconversion3200TrainFinalCopy.txt"; //"AAtrain-72.txt"; // "LC-CHILDESconversion396FinalCopy.txt"; // "LC-CHILDESconversion3200TrainFinal.txt"; //"LC-CHILDESconversion396FinalCopy.txt"; //"LC-CHILDESconversion400FinalCopy.txt";//"AAtrain-3.txt";//"CHILDESconversion100TestFinalCopy.txt";
    static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));

    public void evaluateOnPerturbations(){
        // Load a generator.
        InteractiveProbabilisticGenerator generator = new InteractiveProbabilisticGenerator(grammarPath, grammarPath);
        try {
            // Load data.
            List<PerturbationSample> data = PerturbationSample.loadPerturbationData("perturbations.txt");
            for(PerturbationSample sample : data){
                // Start generation with goal rG, until index pI is reached.
                generator.setGoal(sample.rG);
                int currentIndex = 0;
                while(currentIndex <= sample.pI){
                    generator.generateNextWord();
                    currentIndex++;
                }
                // if index of word is reached, update rG and continue generation.
                generator.setGoal(sample.rP);
                generator.generate();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load perturbation data.");
        }
    }


    public static void main(String[] args) {
        GeneratorEvaluator evaluator = new GeneratorEvaluator();
        evaluator.evaluateOnPerturbations();
    }
}
