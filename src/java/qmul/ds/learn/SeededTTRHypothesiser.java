package qmul.ds.learn;

import qmul.ds.formula.TTRRecordType;

public class SeededTTRHypothesiser extends  TTRHypothesiser{

    public SeededTTRHypothesiser(String resourceDirOrURL, TTRRecordType rt, String sent) {
        super(resourceDirOrURL, rt, sent);
    }

    public SeededTTRHypothesiser(String seedResourceDir) {
        super(seedResourceDir);
    }
}
