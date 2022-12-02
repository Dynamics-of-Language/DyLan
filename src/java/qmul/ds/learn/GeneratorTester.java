/**
 * A class to test methods for the generation paper.
 * Classes to be tested are `GeneratorLearner` & `BestFirstGenerator`.
 *
 * @author Ash
 *
 */

package qmul.ds.learn;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;

import qmul.ds.formula.TTRRecordType;
//import qmul.ds.learn.GeneratorLearner;

public class GeneratorTester
{
    //TODO make the appropriate changes here so that I don't have to have the same path here and in the GeneratorLearner class.
    static final String corpusPath = "dsttr/corpus/CHILDES/eveTrainPairs/CHILDESconversion400Final.txt";//.replaceAll("/", Matcher.quoteReplacement(File.separator));
    static final String grammarPath = "dsttr/resource/2013-ttr-learner-output/";//.replaceAll("/", Matcher.quoteReplacement(File.separator));
    static GeneratorLearner g = new GeneratorLearner(grammarPath, corpusPath); // is this ok that I'm instansiating here?


    public static void testComputeRInc()
    {
        // below is the semantics for "I took it" from "CHILDESconversion400Final", 8th sentence.
        TTRRecordType rG = TTRRecordType.parse("[x1==it : e|x==i : e|e1==take : es|p2==subj(e1, x) : t|p3==obj(e1, x1) : t|head==e1 : es]");
        TTRRecordType rCurr = TTRRecordType.parse("[x==i : e|head==e1 : es]"); // is this a valid TTR for "I" only?

//        System.out.println(rG.toString());
        TTRRecordType rInc = g.computeRInc(rG, rCurr);
        System.out.println("rInc = " + rInc.toString());
    }


    public void testNormaliseCountTable()
    {
        HashMap<String, HashMap<TTRRecordType, Integer>> myCountTable = null; // remaining
        HashMap<String, HashMap<TTRRecordType, Double>> myProbTable = g.normaliseCountTable(myCountTable);

        // how tf should I print this? with for loops?
        // print table
        for (String k: myProbTable.keySet())
        {
            System.out.println(" word: " + k + " | ");
            HashMap<TTRRecordType, Double> row = myProbTable.get(k);
            for (TTRRecordType rt: row.keySet())
                System.out.println(Double.toString(row.get(rt)) + ",  ");

            System.out.println("\n");
        }
    }


    public void testSaveModelToFile()
    {

    }

    public void testLearn()
    {

    }


    public static void main(String[] args)
    {

//		HashMap<String, HashMap<TTRRecordType, Integer>> conditionalCountTable = new HashMap<String, HashMap<TTRRecordType, Integer>>();
//		HashMap<String, HashMap<TTRRecordType, Double>> conditionalProbTable = new HashMap<String, HashMap<TTRRecordType, Double>>();
        testComputeRInc();
    }
}
