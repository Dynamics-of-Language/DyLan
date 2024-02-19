package qmul.ds.learn;

import org.apache.log4j.Logger;
import qmul.ds.InteractiveProbabilisticGenerator;
import qmul.ds.formula.TTRRecordType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PerturbationSample {
    public String originalSent = null;
    public TTRRecordType rG = null;
    public TTRRecordType rP = null;
    public String perturbedSent = null;
    public int pI = -1;
    public boolean isForward = false;
    public int distance = -1;
    public String pos = null;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";


    static final String grammarFolderPath = GeneratorEvaluator.grammarPath;
    static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    protected static org.apache.log4j.Logger logger = Logger.getLogger(PerturbationSample.class);

    // --------------------- constructors ---------------------
    public PerturbationSample(String originalSent, TTRRecordType rG, TTRRecordType rP, String perturbedSent, int pI, boolean isForward, int distance, String pos) {
        this.originalSent = originalSent;
        this.rG = rG;
        this.rP = rP;
        this.perturbedSent = perturbedSent;
        this.pI = pI;
        this.isForward = isForward;
        this.distance = distance;
        this.pos = pos; // Currently not used.
    }

    // --------------------- methods ---------------------

    /**
     * Reminder for the format of the perturbation data file:
     * - rG is the original goal sem
     * - rP is the perturbed goal sem
     * - pI is the index of the word after which the goal is perturbed [I believe this means the last generated word before a perturbation taking place.]
     *
     * @param fileName the name of the file containing the perturbation data
     * @return a list of perturbation samples
     * @throws IOException
     */
    public static List<PerturbationSample> loadPerturbationData(String fileName) throws IOException {
        List<PerturbationSample> samples = new ArrayList<>();

        FileReader reader = new FileReader(grammarFolderPath + fileName);
        BufferedReader stream = new BufferedReader(reader);
        String line;
        while ((line = stream.readLine()) != null) {
            String originalSent = line;
            line = stream.readLine();
            TTRRecordType rG = TTRRecordType.parse(line);
            line = stream.readLine();
            TTRRecordType rP = TTRRecordType.parse(line);
            line = stream.readLine();
            String perturbedSent = line;
            line = stream.readLine();
            int pI = Integer.parseInt(line);
            line = stream.readLine();
            boolean isForward = Boolean.parseBoolean(line.toLowerCase());
            line = stream.readLine();
            int distance = Integer.parseInt(line);
            line = stream.readLine();
            String pos = line;
            samples.add(new PerturbationSample(originalSent, rG, rP, perturbedSent, pI, isForward, distance, pos));
            String emptyLine = stream.readLine(); // Empty line between samples.
        }
        stream.close();
        return samples;
    }


    public static List<PerturbationSample> findGeneratableSamples(List<PerturbationSample> allSamples) {
        logger.info("Loading generator...");
        InteractiveProbabilisticGenerator generator = new InteractiveProbabilisticGenerator(grammarPath, grammarPath);
        logger.info("Generator loaded.");
        logger.info("Number of all samples: " + allSamples.size());
        List<PerturbationSample> generatableSamples = new ArrayList<>();

        for (PerturbationSample sample : allSamples) {
            generator.init();
            generator.setGoal(sample.rP);
            if (generator.generate()) {
                generatableSamples.add(sample);
                logger.trace("Was able to generate sample: " + sample + "\n");
            }
        }
        logger.info("Number of generatable samples: " + generatableSamples.size());
        return generatableSamples;
    }


    public static void filterPerturbationData(String fileName) throws IOException {
        // Criteria:
        // - local, non-local
        // forward, backward
        // - pos

        List<PerturbationSample> samples = loadPerturbationData(fileName);
        List<PerturbationSample> generatableSamples = findGeneratableSamples(samples);
        logger.info(ANSI_GREEN + "Number of generatable samples: " + generatableSamples.size() + ANSI_RESET);
    }


    public static void writePerturbationDataTofFile(List<PerturbationSample> samples, String fileName) {
        logger.info("Number of samples to be written to file \"" + fileName + "\": " + samples.size());
        String fileDir = grammarFolderPath + fileName;
        PrintStream out = null;
        FileOutputStream fileOpen = null;
        try {
            fileOpen = new FileOutputStream(fileDir);
            out = new PrintStream(fileOpen);
            for (PerturbationSample sample : samples) {
                out.print(sample.toStringAsRows());
                out.print("\n");
            }
        } catch (Exception e) {
            logger.error(ANSI_RED + "Couldn't write to \"" + fileDir + "\"!" + ANSI_RESET);
        } finally {
            if (out != null)
                out.close();
        }
    }


    public static void writeEvalOutputTofFile(List<PerturbationSample> samples, String fileName, List<String> generated) {
        logger.info("Number of samples in " + fileName + ": " + samples.size());
        String fileDir = grammarFolderPath + fileName;
        PrintStream out = null;
        FileOutputStream fileOpen = null;
        try {
            fileOpen = new FileOutputStream(fileDir);
            out = new PrintStream(fileOpen);
            for (int i=0; i<samples.size(); i++) {
                PerturbationSample sample = samples.get(i);
                String generatedSentence = generated.get(i);
                out.print(sample.toStringAsRows());
                out.print(generatedSentence+"\n");
                out.print("\n");
            }
        } catch (Exception e) {
            logger.error(ANSI_RED + "Couldn't write to \"" + fileDir + "\"!" + ANSI_RESET);
        } finally {
            if (out != null)
                out.close();
        }
    }


    @Override
    public String toString() {
        return  "\noriginalSent= " + originalSent +
                ",\nrG= " + rG +
                ",\nrP= " + rP +
                ",\nperturbedSent= " + perturbedSent +
                ",\npI= " + pI +
                ",\nisForward= " + isForward +
                ",\ndistance= " + distance +
                ",\npos= " + pos;
    }


    public String toStringAsRows() {
        return  originalSent +
                "\n" + rG +
                "\n" + rP +
                "\n" + perturbedSent +
                "\n" + pI +
                "\n" + isForward +
                "\n" + distance +
                "\n" + pos + "\n";
    }


    public static void main(String[] args) throws IOException {  // Just to test the above.
        List<PerturbationSample> samples = loadPerturbationData("perturbationsE.txt"); // used ot be prtbAll
        List<PerturbationSample> generatableSamples = findGeneratableSamples(samples);
        writePerturbationDataTofFile(generatableSamples, "prtbGeneratables.txt");
    }
}
