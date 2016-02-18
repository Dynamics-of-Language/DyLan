package qmul.ds.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import qmul.ds.formula.Formula;

public class FormulaPanel extends JPanel {

	Formula f;
	JScrollPane container;

	public FormulaPanel(Formula f, JScrollPane s) {
		this.f = f;
		setPreferredSize(getMaximumSize());
		this.container = s;
	}

	public FormulaPanel() {

	}

	public void paint(Graphics g) {
		super.paint(g);
		int margin = 5;
		if (f == null)
			return;
		Graphics2D g2 = (Graphics2D) g;
		f.draw(g2, margin, margin);
		Dimension d = f.getDimensionsWhenDrawn(g2);
		Dimension d2 = new Dimension();
		d2.setSize(d.getWidth() + margin, d.getHeight() + margin);
		this.setPreferredSize(d2);
		container.revalidate();
	}

	public void setFormula(Formula f) {
		this.f = f;
		repaint();

	}

	public void setContainer(JScrollPane fs) {
		this.container = fs;

	}
	
	public void clear(){
		this.f = null;
		this.validate();
		this.repaint();
	}

}
