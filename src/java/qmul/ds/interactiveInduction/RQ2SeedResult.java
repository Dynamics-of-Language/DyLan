package qmul.ds.interactiveInduction;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RQ2SeedResult {
    private static final Logger logger = Logger.getLogger(RQ2SeedResult.class);

    private final List<EvalResult> generalisationResults;
    private final List<EvalResult> forgettingResults;


    // ----------------------------- Constructors -----------------------------
    public RQ2SeedResult () {
        this.generalisationResults = new ArrayList<>();
        this.forgettingResults = new ArrayList<>();
    }

    public RQ2SeedResult(List<EvalResult> generalisationResults, List<EvalResult> forgettingResults) {
        this.generalisationResults = generalisationResults;
        this.forgettingResults = forgettingResults;
    }


    // ----------------------------- Methods -----------------------------
    public List<EvalResult> getGeneralisationResults() {
        return generalisationResults;
    }

    public List<EvalResult> gen() {
        return generalisationResults;
//        return getGeneralisationResults();
    }


    public List<EvalResult> getForgettingResults() {
        return forgettingResults;
    }

    public List<EvalResult> forg() {
        return forgettingResults;
    }



}
