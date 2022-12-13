/**
 * A class to test methods for the generation paper.
 * Classes to be tested are `GeneratorLearner` & `BestFirstGenerator`.
 *
 * @author Ash
 *
 */

package qmul.ds.learn;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;

import qmul.ds.formula.TTRRecordType;

public class GeneratorTester
{
    // TODO make the appropriate changes here so that I don't have to have the same path here and in the GeneratorLearner class.
    static final String corpusPath = "dsttr/corpus/CHILDES/eveTrainPairs/CHILDESconversion400Final.txt".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static GeneratorLearner g = new GeneratorLearner(grammarPath, corpusPath); // TODO is this meaningful?


    public static void testComputeRInc()
    {
        // below is the semantics for "I took it" from "CHILDESconversion400Final", 8th sentence.
        TTRRecordType rG = TTRRecordType.parse("[x1==it : e|x==i : e|e1==take : es|p2==subj(e1, x) : t|p3==obj(e1, x1) : t|head==e1 : es]");
        TTRRecordType rCurr = TTRRecordType.parse("[x==i : e|head==e1 : es]"); // TODO is this a valid RT for "I" only?

        TTRRecordType rInc = g.computeRInc(rG, rCurr);
        System.out.println("rG = " + rG.toString());
        System.out.println("rCurr = " + rCurr.toString());
        System.out.println("rInc = " + rInc.toString());
    }


    public static HashMap<String, HashMap<TTRRecordType, Double>> testNormaliseCountTable()
    {
        HashMap<String, HashMap<TTRRecordType, Integer>> myCountTable = new HashMap<String, HashMap<TTRRecordType, Integer>>();
        // TODO is there a better way to make this more compact?

        HashMap<TTRRecordType, Integer> v1 = new HashMap<TTRRecordType, Integer>(); // value1
        HashMap<TTRRecordType, Integer> v2 = new HashMap<TTRRecordType, Integer>(); // value2
        v1.put(TTRRecordType.parse("[s==i : e]"), 2);
        v1.put(TTRRecordType.parse("[s==you : e]"), 1);
        myCountTable.put("i", v1);
        v2.put(TTRRecordType.parse("[s==you : e]"), 3);
        v2.put(TTRRecordType.parse("[s==i : e]"), 0);
        myCountTable.put("you", v2);

        HashMap<String, HashMap<TTRRecordType, Double>> myProbTable = g.normaliseCountTable(myCountTable);

        // print table // TODO make this cleaner
        System.out.println("\nThe probTable is: ");
        for (String k: myProbTable.keySet())
        {
            System.out.print(" word: " + k + " | ");
            HashMap<TTRRecordType, Double> row = myProbTable.get(k);
            for (TTRRecordType rt: row.keySet())
                System.out.print(Double.toString(row.get(rt)) + ",  ");
            System.out.println("\n");
        }
        return myProbTable;
    }


    public static void testSaveModelToFile(HashMap<String, HashMap<TTRRecordType, Double>> table) throws IOException {
        g.saveModelToFile(table);
    }

    public void testLearn()
    {

    }


    public static void main(String[] args) throws IOException {
//        testComputeRInc(); // Seems to work fine.
//        testNormaliseCountTable();
        testSaveModelToFile(testNormaliseCountTable());
    }
}
