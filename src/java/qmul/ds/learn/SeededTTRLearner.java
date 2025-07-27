package qmul.ds.learn;

import qmul.ds.formula.TTRRecordType;

import java.io.File;
import java.io.IOException;

    // Need a seed lexicon.
public class SeededTTRLearner extends WordLearner<TTRRecordType> {
    public SeededTTRLearner(String seedResourceDir) {
        super(seedResourceDir);
    }

    public SeededTTRLearner() {
    }

    @Override
    public boolean learnOnce() {
        return false;
    }

    @Override
    public void loadCorpus(File corpusFile) throws IOException, ClassNotFoundException {

    }



}
