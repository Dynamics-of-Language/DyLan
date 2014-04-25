package qmul.ds.learn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;

/**
 * This class represents hypothesised sequences of actions in induction as produced by the {@link Hypothesiser} class.
 * The sequence of actions is composed of computational actions together with local lexical hypotheses as hypothesised
 * by the {@link Hypothesiser}.
 * 
 * @author arash
 * 
 */
public class CandidateSequence extends ArrayList<Action> {
	private static Logger logger = Logger.getLogger(CandidateSequence.class);

	private static final long serialVersionUID = 1L;

	private ParserTuple start;

	private List<HasWord> words;

	public CandidateSequence(ParserTuple start, List<Action> actions) {
		super(actions);
		this.start = new ParserTuple(start);
		words = new ArrayList<HasWord>();
	}

	public CandidateSequence(ParserTuple start, List<Action> actions, String words) {
		this(start, actions);
		String[] ws = words.trim().split("\\s");
		for (String w : ws)
			this.words.add(new Word(w));
		logger.info("created sequence with " + this.words);

	}

	public CandidateSequence(ParserTuple start, List<Action> actions, List<HasWord> words) {
		this(start, actions);
		this.words = new ArrayList<HasWord>(words);
		// logger.info("created sequence with "+this.words);

	}
	
	

	public CandidateSequence(CandidateSequence other) {
		this(other.getStart(), other, other.getWords());
	}

	public CandidateSequence() {
		super();
		this.start = null;
	}

	public String toString() {

		String s = "Start:" + this.start.toString() + "\nWords:" + words + "\nAction Sequence: ";
		for (Action a : this) {
			if (a instanceof LexicalHypothesis)
				s += a + " | ";
			else
				s += a.getName() + "|";
		}
		return s;
	}

	public void setStartTtuple(ParserTuple tuple) {
		this.start = new ParserTuple(tuple);
	}

	public ParserTuple getStart() {
		return start;
	}

	/**
	 * This method produces a list of all candidate sequences that are equivalent to this sequence modulo computational
	 * actions on either side of the sequence. The list produced is ordered by the length of these sequences, from
	 * highest (this sequence itself) to lowest (all computational actions popped off from both sides of this sequence).
	 * 
	 * @return the equivalence class of candidate sequences represented by this sequence.
	 */
	public List<CandidateSequence> getEquivalenceClass() {
		List<CandidateSequence> result = new ArrayList<CandidateSequence>();
		if (this.size() < 2) {
			result.add(this);
			return result;
		}

		int leftI = 0;
		int rightI = size();
		Action curLeft = get(leftI);
		Action prevLeft = null;
		// Action curLeftNext=get(leftI+1);
		Action curRight = get(rightI - 1);
		ParserTuple curStart = new ParserTuple(start);

		do {

			List<Action> curList = subList(leftI, rightI);
			CandidateSequence cs = new CandidateSequence(curStart, new ArrayList<Action>(curList), this.words);
			addSequence(cs, result);

			while (curRight instanceof ComputationalAction) {
				rightI--;

				curList = subList(leftI, rightI);
				cs = new CandidateSequence(curStart, curList, this.words);
				addSequence(cs, result);
				curRight = get(rightI - 1);
			}

			curStart = new ParserTuple(curLeft.exec(curStart.getTree(), curStart));
			leftI++;
			prevLeft = curLeft;
			curLeft = get(leftI);

			rightI = size();
			curRight = get(rightI - 1);

		} while (prevLeft instanceof ComputationalAction);

		return result;
	}

	/**
	 * adds a candidate sequence to a list of candidate sequences in the right place as determined by the length of the
	 * sequence.
	 * 
	 * @param s
	 * @param l
	 */
	private void addSequence(CandidateSequence s, List<CandidateSequence> l) {
		for (int i = 0; i < l.size(); i++) {
			if (s.size() >= l.get(i).size()) {
				l.add(i, s);
				return;
			}
		}
		l.add(s);

	}

	public boolean equals(Object o) {
		if (!(o instanceof CandidateSequence))
			return false;
		CandidateSequence other = (CandidateSequence) o;
		if (!this.start.equals(other.start))
			return false;
		// if ((this.words==null & other.words!=null)||(this.words!=null & other.words==null)) return false;

		// if (!this.words.equals(other.words)) return false;
		return super.equals(o);
	}

	public List<HasWord> getWords() {
		return words;
	}
	//TODO: Check this...
	private int numFormulaDecorations() {
		int c = 0;
		for (Action a : this) {
			if (a instanceof LexicalHypothesis) {
				LexicalHypothesis lh = (LexicalHypothesis) a;
				if (lh.containsContentDecoration())
					c++;
			}
		}
		return c;
	}

	/*
	 * Computationallly maximal on both sides. The new split method results in sequences that are computationally
	 * maximal only on the left. public Set<List<CandidateSequence>> split() { int numFormulae=numFormulaDecorations();
	 * if (this.words.size()!=numFormulae) throw new
	 * IllegalStateException("Candidate Sequence must contain the same number of " +
	 * "lex hyps with formula decorations as the number of words in this sequence. But num words="
	 * +words.size()+"; num formulae="+numFormulae); Set<List<CandidateSequence>> result=new
	 * HashSet<List<CandidateSequence>>(); //__________________________________ //base case for recursion: if
	 * (this.words.size()==1){ List<CandidateSequence> l=new ArrayList<CandidateSequence>(); l.add(this); result.add(l);
	 * return result; } //__________________________________ ParserTuple start=this.start; Action a; int i=0; //first
	 * find the first Lexical Hyp that contains a formula decoration. for(;i<size();i++) { a=get(i); start=new
	 * ParserTuple(a.exec(start.getTree().clone(), start)); if (a instanceof LexicalHypothesis) { LexicalHypothesis
	 * lh=(LexicalHypothesis)a; if (lh.containsFormulaDecoration()) break;
	 * 
	 * 
	 * } } //i is now the index of the first Lex Hyp that contains a formula decoration
	 * 
	 * for(int j=i+1;j<size()-1;j++) { //skip current sequence of computational actions, if any int l=j; ParserTuple
	 * newStart=start; while(l<size() && get(l) instanceof ComputationalAction) { newStart=new
	 * ParserTuple(get(l).exec(newStart.getTree().clone(), newStart)); l++; } //l will be the index of the first lexical
	 * hypothesis after the skipped computational sequence //now chop the sequence into two, chopLeft, and rest, keeping
	 * both computationally maximal on either side CandidateSequence chopLeft=new CandidateSequence(this.start,
	 * this.subList(0, l), this.words.subList(0, 1)); CandidateSequence rest=new CandidateSequence(start,
	 * this.subList(j, this.size()), this.words.subList(1, this.words.size())); //get all possible splits of the rest of
	 * this sequence recursively. Set<List<CandidateSequence>> restSplits=rest.split(); //now merge chopLeft with the
	 * result of split(rest) for(List<CandidateSequence> listRest:restSplits) { List<CandidateSequence> li=new
	 * ArrayList<CandidateSequence>(); li.add(chopLeft); li.addAll(listRest); result.add(li); } j=l; start=new
	 * ParserTuple(get(j).exec(newStart.getTree(), newStart)); //we want to end the process if we've reached a second
	 * formula decoration: if (get(j) instanceof LexicalHypothesis) { LexicalHypothesis lh=(LexicalHypothesis) get(j);
	 * if (lh.containsFormulaDecoration()) break; }
	 * 
	 * }
	 * 
	 * return result; }
	 */

	private CandidateSequence removeComputationalFromRight() {

		CandidateSequence result = new CandidateSequence(this);
		int i = result.size() - 1;
		while (result.get(i) instanceof ComputationalAction) {
			result.remove(i);

			i--;
		}
		return result;
	}

	/**
	 * splits this sequence recursively into n sequences where n is {@code this.words.size()}. follows the constraint
	 * that each of these sequences must contain exactly one lexical hypothesis with formula decoration(s). Accordingly,
	 * it assumes that in this candidate sequence for n words there are exactly n Lex Hyps with formula decorations.
	 * Otherwise will result in {@link IllegalStateException}.
	 * 
	 * @return set of all possible splits of this sequence into subsequences corresponding to the words of this
	 *         sequence.
	 */

	public Set<List<CandidateSequence>> split() {
		int numFormulae = numFormulaDecorations();
		logger.debug("Splitting:" + this);
		logger.debug("words are " + this.words);

		if (this.words.size() != numFormulae)
			throw new IllegalStateException("Candidate Sequence must contain the same number of "
					+ "lex hyps with formula decorations as the number of words in this sequence. But num words="
					+ words.size() + "; num formulae=" + numFormulae);
		Set<List<CandidateSequence>> result = new HashSet<List<CandidateSequence>>();
		// __________________________________
		// base case for recursion:
		if (this.words.size() == 1) {
			List<CandidateSequence> l = new ArrayList<CandidateSequence>();
			l.add(this.removeComputationalFromRight());
			result.add(l);
			return result;
		}
		// __________________________________
		ParserTuple start = this.start;
		Action a;
		int i = 0;
		// first find the first Lexical Hyp that contains a formula decoration.
		for (; i < size(); i++) {
			a = get(i);
			logger.debug("applying " + a + " to:");
			logger.debug(start);
			Tree t = a.exec(start.getTree().clone(), start);
			logger.debug("result was:" + t);
			start = new ParserTuple(t);
			if (a instanceof LexicalHypothesis) {
				LexicalHypothesis lh = (LexicalHypothesis) a;
				if (lh.containsContentDecoration())
					break;

			}
		}
		// i is now the index of the first Lex Hyp that contains a formula decoration

		for (int j = i + 1; j < size() - 1; j++) {
			if (get(j).getName().startsWith(TTRHypothesiser.HYP_ADJUNCTION_PREFIX))
				continue;
			
			CandidateSequence chopLeft = new CandidateSequence(this.start, this.subList(0, j), this.words.subList(0, 1));
			CandidateSequence rest = new CandidateSequence(start, this.subList(j, this.size()), this.words.subList(1,
					this.words.size()));
			// get all possible splits of the rest of this sequence recursively.
			Set<List<CandidateSequence>> restSplits = rest.split();
			// now merge chopLeft with the result of split(rest)
			for (List<CandidateSequence> listRest : restSplits) {
				List<CandidateSequence> li = new ArrayList<CandidateSequence>();
				li.add(chopLeft);
				li.addAll(listRest);
				result.add(li);
			}
			// skip current sequence of computational actions, if any
			while (j < size() && (get(j) instanceof ComputationalAction || get(j).getName().startsWith(TTRHypothesiser.HYP_ADJUNCTION_PREFIX))) {
				Tree clone = start.getTree().clone();
				logger.debug("applying " + get(j) + " to " + clone);
				logger.debug("action: " + get(j) + " to " + clone);
				Tree res = get(j).exec(clone, start);
				logger.debug("result was:" + res);
				if (res == null)
					throw new IllegalStateException("Result of action application was null");
				start = new ParserTuple(res);
				j++;
			}
			start = new ParserTuple(get(j).exec(start.getTree().clone(), start));
			// we want to end the process if we've reached a second formula decoration:
			if (get(j) instanceof LexicalHypothesis) {
				LexicalHypothesis lh = (LexicalHypothesis) get(j);
				if (lh.containsContentDecoration())
					break;
			}

		}

		return result;
	}

	public int getFirstLexicalIndex() {

		for (int i = 0; i < size(); i++) {
			Action a = get(i);
			if (!(a instanceof ComputationalAction))
				return i;

		}

		return -1;
	}

}
