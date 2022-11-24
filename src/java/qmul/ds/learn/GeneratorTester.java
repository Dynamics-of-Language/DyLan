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
import qmul.ds.learn.GeneratorLearner;

public class GeneratorTester
{
    static final String corpusPath = "corpus/CHILDES/eveTrainPairs/CHILDESconversion400Final.txt".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static final String grammarPath = "resource/2013-ttr-learner-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static GeneratorLearner g = new GeneratorLearner(corpusPath, grammarPath); // is this ok that I'm instansiating here?


    public static void testComputeRInc()
    {
        // I need a method like "string to ttr-rt".
        // below is the semantics for "I took it" from "CHILDESconversion400Final", 8th sentence.
        TTRRecordType rG = TTRRecordType.parse("[x1==it : e|x==i : e|e1==take : es|p2==subj(e1, x) : t|p3==obj(e1, x1) : t|head==e1 : es]");
        TTRRecordType rCurr = TTRRecordType.parse("[x==i : e|head==e1 : es]"); // is this a valid TTR for "I" only?

        TTRRecordType rInc = g.computeRInc(rG, rCurr);
        System.out.println(rInc.toString());

    }

    public static void main(String[] args)
    {

//		HashMap<String, HashMap<TTRRecordType, Integer>> conditionalCountTable = new HashMap<String, HashMap<TTRRecordType, Integer>>();
//		HashMap<String, HashMap<TTRRecordType, Double>> conditionalProbTable = new HashMap<String, HashMap<TTRRecordType, Double>>();
        testComputeRInc();
    }
}
