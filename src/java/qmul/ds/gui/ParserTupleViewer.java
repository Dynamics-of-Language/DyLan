package qmul.ds.gui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.apache.log4j.Logger;

import edu.stanford.nlp.trees.Tree;
import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;

@SuppressWarnings("serial")
public class ParserTupleViewer extends JTabbedPane {
	
	protected static Logger logger=Logger.getLogger(ParserTupleViewer.class);
	TreePanel treePanel=new TreePanel();
	FormulaPanel semPanel=new FormulaPanel();
	Context parserContext=null;//this is for the computation of maximal semantics only that is now relative to a context, rather than a tree
	public ParserTupleViewer(ParserTuple tuple, Context c)
	{
		this(c);
		setTuple(tuple);
	}
	
	public ParserTupleViewer()
	{
		this(null);
	}
	
	public ParserTupleViewer(Context c)
	{
		
		addTab("Tree", new JScrollPane(treePanel));
		JScrollPane fs = new JScrollPane(semPanel);
		semPanel.setContainer(fs);
		addTab("Semantics", fs);
		this.parserContext=c;
		
	}
	
	
	public void setTuple(ParserTuple tuple)
	{
		
		Tree tree = (tuple == null ? null : tuple.getTree().toStanfordTree());
		
		treePanel.setTree(tree);
		
		if (tree==null)
			return;
		
		if (tuple.getSemantics(parserContext) != null)
			semPanel.setFormula(tuple.getSemantics(parserContext));
		else
			logger.warn("setting null semantics");

	}
	
	public void setFont(Font f)
	{
		super.setFont(f);
		if (treePanel!=null && semPanel!=null)
		{
			treePanel.setFont(f);
			semPanel.setFont(f);
		}
	}
	
	public void setBackground(Color c)
	{
		super.setBackground(c);
		if (treePanel!=null && semPanel!=null)
		{
			treePanel.setBackground(c);
			semPanel.setBackground(c);
		}
	}

	public void setContext(Context context2) {
		this.parserContext=context2;
		
	}
	
	
	
	
	
	

}
