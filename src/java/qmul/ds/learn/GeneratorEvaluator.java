package qmul.ds.learn;

import org.apache.log4j.Logger;
import qmul.ds.InteractiveProbabilisticGenerator;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;

public class GeneratorEvaluator {

//    static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
//    static public String corpusPath = corpusFolderPath + "amat.txt";//"AA-train-lower-396-unique-top1.txt";//"AAtrain-3.txt";//AA-full-lower-4000-Copy.txt";//""LC-CHILDESconversion400FinalCopy.txt";//"AA-train-lower-396-matching-top1.txt";//"AAtrain-3.txt";//"AA-train-lower-396-matching-top1.txt";//"AAtrain-3.txt"; //"AA-train-lower-396-matching-top1.txt";//"LC-CHILDESconversion800TestFinalCopy.txt"; //"LC-CHILDESconversion3200TrainFinalCopy.txt";//"AAtrain-72.txt"; //"LC-CHILDESconversion3200TrainFinalCopy.txt"; //"AAtrain-72.txt"; // "LC-CHILDESconversion396FinalCopy.txt"; // "LC-CHILDESconversion3200TrainFinal.txt"; //"LC-CHILDESconversion396FinalCopy.txt"; //"LC-CHILDESconversion400FinalCopy.txt";//"AAtrain-3.txt";//"CHILDESconversion100TestFinalCopy.txt";
    static final String grammarPath = "resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    protected static Logger logger = Logger.getLogger(GeneratorEvaluator.class);
    
    InteractiveProbabilisticGenerator generator;
    
    public GeneratorEvaluator(String grammarPath, String modelPath)
    {
    	generator = new InteractiveProbabilisticGenerator(grammarPath, modelPath);
    }
    
    public void evaluateOnPerturbations(String perturbationsFile){
        // Load a generator.
        try {
            // Load data.
            List<PerturbationSample> data = PerturbationSample.loadPerturbationData(perturbationsFile);
            for(PerturbationSample sample : data){
            	generator.setRepairGeneration(true);
                generator.init();
                // Start generation with goal rG, until index pI is reached.
                logger.info("processing perturbation sample: " + sample.toString());
                generator.setGoal(sample.rG);
                int currentIndex = 0;
                while(currentIndex <= sample.pI){
                    generator.generateNextWord();
                    currentIndex++;
                }
                logger.info("Perturbing goal to: "+sample.rP);
                
                // When the index of the last word is reached, change goal to rP and continue generation.
                generator.setGoal(sample.rP);
                generator.generate();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load perturbation data.");
        }
    }

    public void setRepairGeneration(boolean b)
    {
    	generator.setRepairGeneration(b);
    }

    public static void main(String[] args) {
        GeneratorEvaluator evaluator = new GeneratorEvaluator(grammarPath, grammarPath);
        
        
        evaluator.evaluateOnPerturbations("perturbationData.txt");
    }
}
