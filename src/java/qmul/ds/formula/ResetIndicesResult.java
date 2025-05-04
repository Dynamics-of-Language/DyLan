package qmul.ds.formula;

import java.util.List;

/**
 * used in TTRRecordType for resetting indices of every var/label in a RT.
 * @author: AA
 */
public class ResetIndicesResult {
    private final TTRRecordType recordType;
    private final List<Integer> maxIndices;  // [maxP, maxX, maxE]

    public ResetIndicesResult(TTRRecordType recordType, List<Integer> maxIndices) {
        this.recordType = recordType;
        this.maxIndices = maxIndices;
    }

    public TTRRecordType getRecordType() {
        return recordType;
    }

    public List<Integer> getMaxIndices() {
        return maxIndices;
    }
}