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
 * 
 * @author arash
 * 
 */
public abstract class TTRFormula extends Formula {

	/**
	 * 
	 * 
	 */

	private static final long serialVersionUID = 1L;

	protected static final Logger logger = Logger.getLogger(TTRFormula.class);

	public abstract TTRFormula freshenVars(Tree t);
	
	
	public abstract <T extends DAGTuple, E extends DAGEdge> TTRFormula freshenVars(Context<T,E> c);
	
	public abstract <T extends DAGTuple, E extends DAGEdge> TTRFormula freshenVars(TTRRecordType r, Map<Variable, Variable> map);
	

	protected abstract List<TTRRecordType> getTypes();

	public abstract TTRField getHeadField();
	
	public abstract TTRFormula evaluate();

	public abstract TTRFormula substitute(Formula f1, Formula f2);

	public abstract TTRFormula asymmetricMerge(TTRFormula rt);

	
	public void collapseIsomorphicSuperTypes(HashMap<Variable, Variable> map)
	{
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
	
	public List<Tree> getAbstractions(DSType funcType, NodeAddress prefix) {
		return getAbstractions(funcType, prefix, funcType.getFinalType());
	}

	protected List<Tree> getAbstractions(DSType funcType, NodeAddress root, DSType rootType) {
		List<BasicType> list = funcType.getTypesSubjFirst();
		list.remove(0);
		return getAbstractions(list, root, rootType);
	}

	
	protected List<Tree> getAbstractions(List<BasicType> types, NodeAddress root, DSType rootType) {
		List<Tree> result = new ArrayList<Tree>();
		logger.debug("abstractions for:" + types);
		logger.debug("on:" + this);
		if (types.isEmpty()) {
			Tree local = new Tree(root);
			logger.debug("reached base.. returning one node tree");
			local.getPointedNode().addLabel(new FormulaLabel(this));
			local.getPointedNode().addLabel(new TypeLabel(rootType));
			local.getPointedNode().remove(new Requirement(new TypeLabel(DSType.t)));
			logger.debug("constructed:" + local);
			result.add(local);
			return result;
		}

		BasicType basic = (BasicType) types.get(0);

		List<Pair<TTRRecordType, TTRLambdaAbstract>> basicAbstracts = getAbstractions(basic, 1);

		for (Pair<TTRRecordType, TTRLambdaAbstract> pair : basicAbstracts) {
			Tree local = new Tree(root);
			logger.debug("constructing local tree at:" + root);
			local.getPointedNode().addLabel(new FormulaLabel(this));
			local.getPointedNode().addLabel(new TypeLabel(rootType));
			local.make(BasicOperator.DOWN_0);
			local.go(BasicOperator.DOWN_0);

			List<Pair<TTRRecordType, TTRLambdaAbstract>> argumentAbstracts = pair.first().getAbstractions(DSType.cn, 1);
			if (argumentAbstracts.size() == 1) {
				local.make(BasicOperator.DOWN_1);
				local.go(BasicOperator.DOWN_1);
				local.put(new FormulaLabel(argumentAbstracts.get(0).second()));
				DSType abstractedType = DSType.create(DSType.cn, basic);
				local.put(new TypeLabel(abstractedType));
				local.go(BasicOperator.UP_1);
				local.make(BasicOperator.DOWN_0);
				local.go(BasicOperator.DOWN_0);
				local.put(new TypeLabel(DSType.cn));
				local.put(new FormulaLabel(argumentAbstracts.get(0).first()));
				local.go(BasicOperator.UP_0);
			}
			local.put(new TypeLabel(basic));
			local.put(new FormulaLabel(pair.first()));
			local.go(BasicOperator.UP_0);
			local.make(BasicOperator.DOWN_1);
			local.go(BasicOperator.DOWN_1);
			local.put(new FormulaLabel(pair.second()));
			DSType abstractedType = DSType.create(basic, rootType);
			local.put(new TypeLabel(abstractedType));
			logger.debug("constructed:" + local);
			List<Tree> lowerAbstracts = pair.second().getAbstractions(types.subList(1, types.size()),
					local.getPointer(), abstractedType);
			logger.debug("now merging:" + local);
			for (Tree lower : lowerAbstracts) {
				logger.debug("with root=" + lower.getRootNode().getAddress());
				logger.debug("with:" + lower);
				Tree merged = local.merge(lower);
				if (merged.keySet().contains(merged.getPointer().down1()))
					merged.go(BasicOperator.DOWN_1);
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
