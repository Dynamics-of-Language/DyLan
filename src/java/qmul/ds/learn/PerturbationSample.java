package qmul.ds.learn;

import qmul.ds.Utterance;
import qmul.ds.formula.TTRRecordType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PerturbationSample {
    public Utterance originalSent = null;
    public TTRRecordType rG = null;
    public TTRRecordType rP = null;
    public Utterance perturbedSent = null;
    public int pI = -1;
    public boolean isForward = false;
    public int distance = -1;
    public String pos = null;

    static final String grammarFolderPath = GeneratorEvaluator.grammarPath;

    public PerturbationSample(Utterance originalSent, TTRRecordType rG, TTRRecordType rP, Utterance perturbedSent, int pI, boolean isForward, int distance, String pos){
        this.originalSent = originalSent;
        this.rG = rG;
        this.rP = rP;
        this.perturbedSent = perturbedSent;
        this.pI = pI;
        this.isForward = isForward;
        this.distance = distance;
        this.pos = pos; // Currently not used.
    }

    /**
     * Reminder for the format of the perturbation data file:
     * - rG is the original goal sem
     * - rP is the perturbed goal sem
     * - pI is the index of the word after which the goal is perturbed [I believe this means the last generated word before a perturbation taking place.]
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static List<PerturbationSample> loadPerturbationData(String fileName) throws IOException {
        List<PerturbationSample> samples = new ArrayList<>();

        FileReader reader = new FileReader(grammarFolderPath + fileName);
        BufferedReader stream = new BufferedReader(reader);
        String line;
        while ((line = stream.readLine()) != null) {
            Utterance originalSent = new Utterance(line);
            line = stream.readLine();
            TTRRecordType rG = TTRRecordType.parse(line);
            line = stream.readLine();
            TTRRecordType rP = TTRRecordType.parse(line);
            line = stream.readLine();
            Utterance perturbedSent = new Utterance(line);
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


    public static void main(String[] args) throws IOException {  // Just to test the above.
        List<PerturbationSample> samples = loadPerturbationData("perturbations.txt");
        for (PerturbationSample sample : samples)
            System.out.println(sample);
    }
}
