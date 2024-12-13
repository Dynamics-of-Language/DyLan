package qmul.ds.formula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.stanford.nlp.util.Pair;
import qmul.ds.Context;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.BasicOperator;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.BasicType;
import qmul.ds.type.DSType;

/**
 * an interface where TTR specific methods could go
 * @author arash E and Arash A
 */
public abstract class TTRFormula extends Formula {

	private static final long serialVersionUID = 1L;

	protected static final Logger logger = Logger.getLogger(TTRFormula.class);
	public static int abstractionOrder = 1;  // AA added this to keep track of the abstraction order.
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";
	// Below added by AA for efficiency and is a global cache for previous abstractions, so that we don't have to
	// compute them again and again. Once computed, they are stored in this cache for future use.
	public HashMap<Pair<TTRRecordType, BasicType>, List<Pair<TTRRecordType, TTRLambdaAbstract>>> abstractsCache = new HashMap<>();

	public abstract TTRFormula freshenVars(Tree t);
	
	
	public abstract <T extends DAGTuple, E extends DAGEdge> TTRFormula freshenVars(Context<T,E> c);
	
	public abstract <T extends DAGTuple, E extends DAGEdge> TTRFormula freshenVars(TTRRecordType r, Map<Variable, Variable> map);
	

	protected abstract List<TTRRecordType> getTypes();

	public abstract TTRField getHeadField();
	
	public abstract TTRFormula evaluate();

	public abstract TTRFormula substitute(Formula f1, Formula f2);

	public abstract TTRFormula asymmetricMerge(TTRFormula rt);

	
	public void collapseIsomorphicSuperTypes(HashMap<Variable, Variable> map) {
		throw new UnsupportedOperationException();
	}


	@Override
	public TTRFormula conjoin(Formula f) {
		if (f == null)
			return this;
		if (f instanceof TTRFormula)
			return ((TTRFormula) f).asymmetricMerge(this);
		throw new IllegalArgumentException("Can only conjoin a TTRFormula with TTRFormula, got a:" + f + ": "
				+ f.getClass());
	}

	public abstract TTRFormula clone();

	public abstract TTRFormula asymmetricMergeSameType(TTRFormula f);

	/**
	 * Docs by AA.
	 * The first and most general getAbstractions method, which is an interface for the next ones.
	 * So go below to find the main one!
	 * These are the ones that build the tree abstractions of a RT, and call the {@link TTRRecordType}'s
	 * getAbstractions method that works at a RT level.
	 * @param funcType
	 * @param prefix
	 * @return
	 */
	public List<Tree> getAbstractions(DSType funcType, NodeAddress prefix) {
		return getAbstractions(funcType, prefix, funcType.getFinalType());
	}


	/**
	 * Docs by AA.
	 * The second getAbstractions method, which is an interface for the main one, coming next.
	 * @param funcType
	 * @param root
	 * @param rootType
	 * @return
	 */
	protected List<Tree> getAbstractions(DSType funcType, NodeAddress root, DSType rootType) {
		List<BasicType> list = funcType.getTypesSubjFirst();
		logger.trace("list of basic types on funcType " + funcType + " is: " + list);
		list.remove(0);
		return getAbstractions(list, root, rootType);
	}


	/**
	 * Docs by AA.
	 * The main method for computing tree abstractions of a RT. The ones above it are calling this one.
	 * This builds an abstraction tree, and annotates it with outputs of the {@link TTRRecordType}'s getAbstractions
	 * method (the RT-level one).
	 * How it works [roughly]: First we get some basic abstractions to begin building the tree with. Then we continue
	 * by recursively getting abstractions on the right node and the left node, and then merging them.
	 *
	 * @param types
	 * @param root
	 * @param rootType
	 * @return
	 */
	protected List<Tree> getAbstractions(List<BasicType> types, NodeAddress root, DSType rootType) {
		List<Tree> result = new ArrayList<Tree>();
		logger.debug("Abstractions for: " + types);
		logger.debug("on: " + this);
		if (types.isEmpty()) {
			Tree local = new Tree(root);
			logger.debug("Reached base! returning one node tree...");
			local.getPointedNode().addLabel(new FormulaLabel(this));
			local.getPointedNode().addLabel(new TypeLabel(rootType));
			local.getPointedNode().remove(new Requirement(new TypeLabel(DSType.t)));
			logger.debug("Constructed: " + local);
			result.add(local);
			return result;
		}

		BasicType basic = (BasicType) types.getFirst();
		List<Pair<TTRRecordType, TTRLambdaAbstract>> basicAbstracts = getAbstractions(basic, 1);
		// AA before getting abstractions, check if it is already computed and stored in the cache
		// TODO: for lambdaAbstracts it doesn't work (as it is not the class/type of the key for my cache) so has to be fixed.
//		List<Pair<TTRRecordType, TTRLambdaAbstract>> basicAbstracts;
//		boolean isTTRRT = this instanceof TTRRecordType;
//		if (!isTTRRT)
//			TTRRecordType rtKey = this.;
//
//		if (abstractsCache.containsKey(new Pair<>(this, basic))) {
//			logger.trace("Already in cache: Abstraction of " + basic + " on " + this + " .");
//			basicAbstracts= abstractsCache.get(new Pair<>(this, basic));
//			logger.trace("it is: " + basicAbstracts);
//		} else {
//			basicAbstracts = getAbstractions(basic, 1);
//			abstractsCache.put(new Pair<>(this, basic), basicAbstracts);
//			logger.trace("Updated cache with abstraction of " + basic + " on " + this + " .");
//		}

		logger.trace(ANSI_BLUE+"Got " + basicAbstracts.size()+  " basic abstractions with " + basic + ANSI_RESET);
		logger.trace(basicAbstracts);
		for (Pair<TTRRecordType, TTRLambdaAbstract> pair : basicAbstracts) {
			Tree local = new Tree(root);
			logger.debug("constructing local tree at: " + root);
			local.getPointedNode().addLabel(new FormulaLabel(this));
			local.getPointedNode().addLabel(new TypeLabel(rootType));
			local.make(BasicOperator.DOWN_0);
			local.go(BasicOperator.DOWN_0);

			logger.trace("AA now trying to get cn abstracts on " + pair.first());
			List<Pair<TTRRecordType, TTRLambdaAbstract>> argumentAbstracts;
			if (abstractsCache.containsKey(new Pair<>(pair.first(), DSType.cn))) {
				logger.trace("Already in cache: Abstraction of " + DSType.cn + " on " + pair.first() + " .");
				argumentAbstracts = abstractsCache.get(new Pair<>(pair.first(), DSType.cn));
				logger.trace("it is: " + argumentAbstracts);
			} else {
				argumentAbstracts = pair.first().getAbstractions(DSType.cn, 1); // AA THIS LOOKS SUSPICIOUS
				abstractsCache.put(new Pair<>(pair.first(), DSType.cn), argumentAbstracts);
				logger.trace("Updated cache with abstraction of " + DSType.cn + " on " + pair.first() + " .");
			}
			logger.trace("cn abstracts size:  "+argumentAbstracts.size());
			logger.warn("AA changed by me from size=1 to more sizes...");

			List<Tree> prematureTreeCopies = new ArrayList<>();
			if (!argumentAbstracts.isEmpty()) {
				logger.warn(ANSI_RED+"AA DANGEROUS test going on!"+ANSI_RESET);
//				for (Pair<TTRRecordType, TTRLambdaAbstract> argumentAbstract : argumentAbstracts) {  // AA Replace this with "only the first one"
				// AA First, make the right node for the first one (here), and then put the rest under it (below). This
				// is an assumption I made in the way I am getting the abstractions from the RTs, in this specific case.
				Pair<TTRRecordType, TTRLambdaAbstract> argumentAbstract = argumentAbstracts.getFirst();
				local.make(BasicOperator.DOWN_1);
				local.go(BasicOperator.DOWN_1);
				local.put(new FormulaLabel(argumentAbstract.second()));
				DSType abstractedType = DSType.create(DSType.cn, basic);
				local.put(new TypeLabel(abstractedType));
				local.go(BasicOperator.UP_1);
				local.make(BasicOperator.DOWN_0);
				local.go(BasicOperator.DOWN_0);
				local.put(new TypeLabel(DSType.cn));
				local.put(new FormulaLabel(argumentAbstract.first()));
				local.go(BasicOperator.UP_0);

				// AA Now the rest of the argumentAbstracts are supposed to go under this one:
				for (int i=1; i<argumentAbstracts.size(); i++) {
					Tree copyLocal = local.clone();
					Pair<TTRRecordType, TTRLambdaAbstract> argumentAbstractionChildren = argumentAbstracts.get(i);  // AA These should be added under argumentAbstracts.get(0).
					copyLocal.go(BasicOperator.DOWN_0);
					copyLocal.make(BasicOperator.DOWN_1);
					copyLocal.go(BasicOperator.DOWN_1);
					copyLocal.put(new FormulaLabel(argumentAbstractionChildren.second()));
					DSType abstractedType2 = DSType.create(DSType.cn, DSType.cn);
					copyLocal.put(new TypeLabel(abstractedType2));
					copyLocal.go(BasicOperator.UP_1);
					copyLocal.make(BasicOperator.DOWN_0);
					copyLocal.go(BasicOperator.DOWN_0);
					copyLocal.put(new TypeLabel(DSType.cn));
					copyLocal.put(new FormulaLabel(argumentAbstractionChildren.first()));

					copyLocal.go(BasicOperator.UP_0);
					logger.debug("constructed: " + copyLocal);
					result.add(copyLocal); // AA I think it's correct.
					copyLocal.go(BasicOperator.UP_0);

					logger.warn(ANSI_RED+"AA DANGEROUS test!"+ANSI_RESET);
					copyLocal.put(new TypeLabel(basic));
					copyLocal.put(new FormulaLabel(pair.first()));
					copyLocal.go(BasicOperator.UP_0);
					copyLocal.make(BasicOperator.DOWN_1);
					copyLocal.go(BasicOperator.DOWN_1);
					copyLocal.put(new FormulaLabel(pair.second()));
					DSType abstractedTypeLeft = DSType.create(basic, rootType);
					copyLocal.put(new TypeLabel(abstractedTypeLeft));
					logger.debug("constructed ash: " + copyLocal);

					logger.trace("trying to get lower abstracts of: " + pair.second());
					List<Tree> lowerAbstracts;
					// aa fixing the below bug with a try-catch, see what happens. It's doing the job, but is it ok? idk.
					logger.trace(ANSI_RED+"AA doing some unverified try-catch here for lower abstracts..."+ANSI_RESET);
					try {
						lowerAbstracts = pair.second().getAbstractions(types.subList(1, types.size()), copyLocal.getPointer(), abstractedTypeLeft);
					}
					catch (Exception e) {
						logger.warn(ANSI_RED + "AA lower abstractions is empty." + ANSI_RESET);
						lowerAbstracts = new ArrayList<>();
					}
					logger.trace("lower abstracts are: " + lowerAbstracts);
					logger.debug("now merging: " + copyLocal);
					for (Tree lower : lowerAbstracts) {
						logger.debug("with root= " + lower.getRootNode().getAddress());
						logger.debug("with: " + lower);
						Tree merged = copyLocal.merge(lower);
						if (merged.keySet().contains(merged.getPointer().down1()))
							merged.go(BasicOperator.DOWN_1);
						logger.warn(ANSI_RED+"AA very dangerous test here!"+ANSI_RESET);
						prematureTreeCopies.add(merged);
					}
					if (lowerAbstracts.isEmpty()) {
						prematureTreeCopies.add(copyLocal); // todo donno if this is correct...
					}
				}
			}

			logger.debug("len of prematureTreeCopies: " + prematureTreeCopies.size());
			local.put(new TypeLabel(basic));
			local.put(new FormulaLabel(pair.first()));
			local.go(BasicOperator.UP_0);
			local.make(BasicOperator.DOWN_1);
			local.go(BasicOperator.DOWN_1);
			local.put(new FormulaLabel(pair.second()));
			DSType abstractedType = DSType.create(basic, rootType);
			local.put(new TypeLabel(abstractedType));
			logger.debug("constructed: " + local);
			logger.trace("trying to get lower abstracts of: " + pair.second());
			List<Tree> lowerAbstracts = pair.second().getAbstractions(types.subList(1, types.size()), local.getPointer(), abstractedType);
			logger.trace("lower abstracts size: " + lowerAbstracts.size());
			logger.trace("lower abstracts are: " + lowerAbstracts);
			logger.debug("now merging: " + local);
			for (Tree lower : lowerAbstracts) {
				logger.debug("with root= " + lower.getRootNode().getAddress());
				logger.debug("with: " + lower);
				Tree merged = local.merge(lower);
				if (merged.keySet().contains(merged.getPointer().down1()))
					merged.go(BasicOperator.DOWN_1);
				for (Tree prematureTreeCopy : prematureTreeCopies) {
					Tree merged2 = prematureTreeCopy.merge(merged);
					if (merged2.keySet().contains(merged2.getPointer().down1()))
						merged2.go(BasicOperator.DOWN_1);
					result.add(merged2);
				}
				result.add(merged);
			}
		}
		return result;
	}


	/**
	 * Constructs all lambda abstractions of this TTRFormula based on (lambda) abstracting out something of the type specified by
	 * the first argument. The second argument is the base suffix for new lambda variables (e.g. R1, R2 etc)
	 * @param basic the type of the function argument to be abstracted
	 * @param newVarSuffix the base suffix for new lambda variables
	 * @return list of all possible abstractions
	 */
	protected abstract List<Pair<TTRRecordType, TTRLambdaAbstract>> getAbstractions(BasicType basic, int newVarSuffix);


	public TTRFormula removeHead()
	{
		throw new UnsupportedOperationException("Operation unsupported for the TTRFormula class:"+this.getClass());
	}
	public TTRFormula instantiate()
	{
		return this.clone();
	}

	public TTRFormula removeHeadIfManifest() {
		throw new UnsupportedOperationException("Operation unsupported for the TTRFormula class:"+this.getClass());
	}

	/**
	 * for the purpose of computing maximally specific common supertype between a TTRRecordType and a disjunctive type
	 * @return
	 */
	public TTRFormula sortFieldsBySpecificity() {
		throw new IllegalArgumentException("Cannot do this for: "+this.getClass());
	}
}
