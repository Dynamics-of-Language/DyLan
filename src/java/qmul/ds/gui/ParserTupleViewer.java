package qmul.ds.gui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import edu.stanford.nlp.trees.Tree;

@SuppressWarnings("serial")
public class ParserTupleViewer extends JTabbedPane {
	
	protected static Logger logger=Logger.getLogger(ParserTupleViewer.class);
	TreePanel treePanel=new TreePanel();
	FormulaPanel semPanel=new FormulaPanel();
	
	public ParserTupleViewer(ParserTuple tuple)
	{
		this();
		setTuple(tuple);
	}
	
	public ParserTupleViewer()
	{
		
		addTab("Tree", new JScrollPane(treePanel));
		JScrollPane fs = new JScrollPane(semPanel);
		semPanel.setContainer(fs);
		addTab("Semantics", fs);
		
	}
	
	
	public void setTuple(ParserTuple tuple)
	{
		Tree tree = (tuple == null ? null : tuple.getTree().toStanfordTree());
		
		treePanel.setTree(tree);
		if (tuple.getSemantics() != null)
			semPanel.setFormula(tuple.getSemantics());
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
	
	
	
	
	
	

}
