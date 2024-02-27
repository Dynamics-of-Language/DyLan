package qmul.ds.interactiveInduction;

import qmul.ds.formula.TTRRecordType;

/**
 * A class to represent the observation RT for Baby AI.
 * Mainly made up of agent information, instruction action (verb), begin state and end state.
 */
public class Observation {
//    private TTRRecordType agent;  // Useless, it's always Dylan, better be beginState.
    //= TTRRecordType.parse("[agent==dylan : e]");
    private TTRRecordType event;
    //= TTRRecordType.parse("[action==action_go : t]");
    private TTRRecordType currentState;  // Initialised to the beginState.
    private TTRRecordType endState;

    public Observation(String action, String currentState, String endState){
//        this.agent = TTRRecordType.parse("[agent==dylan : e]");
        this.event = TTRRecordType.parse(String.format("[event==action_%s : t]", action));
        this.currentState = TTRRecordType.parse("[agent==dylan : e]");
        this.endState = TTRRecordType.parse(endState);
    }

    public static TTRRecordType updateCurrentState(TTRRecordType update){  // todo
//        this.currentState = newState;
        return null;
    }

    public Boolean isGoodUpdate(TTRRecordType update){
        // make a copy of the current state, update it.
        // If it subsumes the end state, return true. Else return false.

        TTRRecordType currentStateCopy = this.currentState.clone();  // TODO check if this is a deep copy and correct.
        currentStateCopy = updateCurrentState(update);
        if (currentStateCopy.subsumes(this.endState))
            return true;
        else
            return false;
    }

//    public TTRRecordType update_AT(){
//        this.currentState.asymmetricMerge(TTRRecordType.parse("[agent==dylan : e]"));
////        return null;
//    }


}
