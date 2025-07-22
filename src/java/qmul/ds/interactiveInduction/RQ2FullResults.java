package qmul.ds.interactiveInduction;

import org.apache.log4j.Logger;

import java.util.HashMap;

public class RQ2FullResults {
    private static final Logger logger = Logger.getLogger(RQ2FullResults.class);

    private final HashMap<Integer, HashMap<Boolean, RQ2SeedResult>> rq2FullResults;


    public RQ2FullResults () {
        this.rq2FullResults = new HashMap<>();
    }


    public void addResult(int seed, boolean withCurriculum, RQ2SeedResult seedResult) {
        HashMap<Boolean, RQ2SeedResult> currResults = rq2FullResults.getOrDefault(seed, new HashMap<>());
        currResults.put(withCurriculum, seedResult);
        rq2FullResults.put(seed, currResults);
    }

}
