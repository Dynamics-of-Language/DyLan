package qmul.ds.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.StringReader;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import qmul.ds.formula.TTRRecordType;
import edu.stanford.nlp.ling.StringLabelFactory;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;

/**
 * Class for displaying a Tree.
 * 
 * @author Dan Klein
 */
@SuppressWarnings("serial")
public class TreePanel extends JPanel {

	private static Logger logger = Logger.getLogger(TreePanel.class);

	protected int VERTICAL_ALIGN = SwingConstants.CENTER;
	protected int HORIZONTAL_ALIGN = SwingConstants.CENTER;

	private final Dimension DEFAULT_SIZE = new Dimension(400, 300);
	private Dimension size = DEFAULT_SIZE;
	private int maxFontSize = 36;
	private int minFontSize = 12;

	protected static final double sisterSkip = 7.5;
	protected static final double parentSkip = 1.35;
	protected static final double belowLineSkip = 0.075;
	protected static final double aboveLineSkip = 0.075;

	private static final Stroke UNFIXED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
			1.0f, new float[] { 4.0f }, 0.0f);
	private static final Stroke LINKED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
			1.0f, new float[] { 1.0f }, 0.0f);
	private static final Stroke LOCAL_UNFIXED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, 1.0f, new float[] { 4.0f }, 0.0f);

	private static Stroke defaultStroke;

	private static final Color UNFIXED_COLOR = Color.GRAY;
	private static final Color LINKED_COLOR = Color.BLUE;
	private static final Color LOCAL_UNFIXED_COLOR = Color.RED;
	private static Color defaultColor;

	private static final int LINKED_CURVE_OFFSET = 50;

	protected Tree tree;

	public Tree getTree() {
		return tree;
	}

	public void setTree(Tree tree) {
		this.tree = tree;
		repaint();
	}

	protected static String nodeToString(Tree t) {
		if (t == null) {
			return " ";
		}
		edu.stanford.nlp.ling.Label l = t.label();
		if (l == null) {
			return " ";
		}
		String str = l.value();
		if (str == null) {
			return " ";
		}
		return str;
	}

	public static class NodeDimensions {
		public final String[] rows;
		public final double[] tabs;
		public final double width; // = 0.0;
		public final double height; // = 0.0;
		public final double lineHeight; // = 0.0;
		public final double lineAscent; // = 0.0;

		public NodeDimensions(String[] rows, double[] tabs, double width, double height, double lineHeight,
				double lineAscent) {
			this.rows = rows;
			this.tabs = tabs;
			this.width = width;
			this.height = height;
			this.lineHeight = lineHeight;
			this.lineAscent = lineAscent;
		}
	}

	private static NodeDimensions nodeDimensions(Tree tree, FontMetrics fM) {
		// ArrayList<String> strings = new ArrayList<String>();
		String s = nodeToString(tree);
		String[] strings = s.split(TTRRecordType.TTR_LINE_BREAK);

		double[] centres = new double[strings.length - 1];
		double[] tabs = new double[strings.length];
		double maxlwidth = 0.0;
		double maxrwidth = 0.0;
		for (int i = 0; i < (strings.length - 1); i++) {
			int ci = strings[i].lastIndexOf(TTRRecordType.TTR_FIELD_SEPARATOR);
			double lwidth = (ci >= 0 ? fM.stringWidth(strings[i].substring(0, ci)) : 0);
			double rwidth = (ci >= 0 ? fM.stringWidth(strings[i].substring(ci)) : 0);
			maxlwidth = Math.max(maxlwidth, lwidth);
			maxrwidth = Math.max(maxrwidth, rwidth);
			centres[i] = lwidth;
		}
		double ttrWidth = maxlwidth + maxrwidth;
		double nodeWidth = fM.stringWidth(strings[strings.length - 1]);
		double width, ttrTab, nodeTab;
		if (nodeWidth > ttrWidth) {
			width = nodeWidth;
			ttrTab = ((nodeWidth - ttrWidth) / 2) + maxlwidth;
			nodeTab = 0.0;
		} else {
			width = ttrWidth;
			ttrTab = maxlwidth;
			nodeTab = (ttrWidth - nodeWidth) / 2;
		}
		for (int i = 0; i < (strings.length - 1); i++) {
			tabs[i] = ttrTab - centres[i];
		}
		tabs[strings.length - 1] = nodeTab;
		return new NodeDimensions(strings, tabs, width, fM.getAscent() * strings.length + fM.getDescent(),
				fM.getHeight(), fM.getAscent());
	}

	public static class TreeDimensions {
		public final double width; // = 0.0;
		public final double nodeTab; // = 0.0;
		public final double nodeCenter; // = 0.0;
		public final double childTab; // = 0.0;
		public final double height; // = 0.0;

		public TreeDimensions(double width, double nodeTab, double nodeCenter, double childTab, double height) {
			this.width = width;
			this.nodeTab = nodeTab;
			this.nodeCenter = nodeCenter;
			this.childTab = childTab;
			this.height = height;
		}
	}

	protected static TreeDimensions treeDimensions(Tree tree, FontMetrics fM) {
		if (tree == null) {
			return new TreeDimensions(0.0, 0.0, 0.0, 0.0, 0.0);
		}
		NodeDimensions local = nodeDimensions(tree, fM);
		if (tree.isLeaf()) {
			return new TreeDimensions(local.width, 0.0, local.width / 2.0, 0.0, local.height);
		}
		double sub = 0.0;
		double nodeCenter = 0.0;
		// double childTab = 0.0;
		double subHeight = 0.0;
		for (int i = 0, numKids = tree.numChildren(); i < numKids; i++) {
			TreeDimensions subWR = treeDimensions(tree.getChild(i), fM);
			if (i == 0) {
				nodeCenter += (sub + subWR.nodeCenter) / 2.0;
			}
			if (i == numKids - 1) {
				nodeCenter += (sub + subWR.nodeCenter) / 2.0;
			}
			sub += subWR.width;
			if (i < numKids - 1) {
				sub += sisterSkip * fM.stringWidth(" ");
			}
			subHeight = Math.max(subHeight, subWR.height);
		}
		double height = subHeight + local.height + local.lineHeight * (parentSkip + aboveLineSkip + belowLineSkip);
		double localLeft = local.width / 2.0;
		double subLeft = nodeCenter;
		double totalLeft = Math.max(localLeft, subLeft);
		double localRight = local.width / 2.0;
		double subRight = sub - nodeCenter;
		double totalRight = Math.max(localRight, subRight);
		return new TreeDimensions(totalLeft + totalRight, totalLeft - localLeft, nodeCenter + totalLeft - subLeft,
				totalLeft - subLeft, height);
	}

	protected FontMetrics pickFont(Graphics2D g2, Tree tree, Dimension space) {
		Font font = g2.getFont();
		String fontName = font.getName();
		int style = font.getStyle();

		for (int size = maxFontSize; size > minFontSize; size--) {
			font = new Font(fontName, style, size);
			g2.setFont(font);
			FontMetrics fontMetrics = g2.getFontMetrics();
			TreeDimensions dim = treeDimensions(tree, fontMetrics);
			if (dim.height > space.getHeight()) {
				continue;
			}
			if (dim.width > space.getWidth()) {
				continue;
			}
			// logger.debug("Chose: " + size + " for space: " +
			// space.getWidth());
			return fontMetrics;
		}
		font = new Font(fontName, style, minFontSize);
		g2.setFont(font);
		return g2.getFontMetrics();
	}

	private static double paintTree(Tree t, Point2D start, Graphics2D g2, FontMetrics fM) {

		if (t == null) {
			return 0.0;
		}
		NodeDimensions nd = nodeDimensions(t, fM);
		String nodeStr = nd.rows[0];
		double nodeWidth = nd.width;
		double nodeHeight = nd.height;
		double lineHeight = nd.lineHeight;
		double lineAscent = nd.lineAscent;
		TreeDimensions wr = treeDimensions(t, fM);
		double treeWidth = wr.width;
		double nodeTab = wr.nodeTab;
		double childTab = wr.childTab;
		double nodeCenter = wr.nodeCenter;
		// double treeHeight = height(t, fM);
		// draw root
		for (int i = 0; i < nd.rows.length; i++) {
			double myTab = nodeTab + start.getX() + nd.tabs[i];
			g2.drawString(nd.rows[i], (float) myTab, (float) (start.getY() + lineAscent * (i + 1)));
		}
		if (t.isLeaf()) {
			return nodeWidth;
		}
		double layerHeight = nodeHeight + lineHeight * (belowLineSkip + aboveLineSkip + parentSkip);
		double childStartX = start.getX() + childTab;
		double childStartY = start.getY() + layerHeight;
		double lineStartX = start.getX() + nodeCenter;
		double lineStartY = start.getY() + nodeHeight + lineHeight * (belowLineSkip);
		double lineEndY = lineStartY + lineHeight * parentSkip;
		// recursively draw children
		for (int i = 0; i < t.children().length; i++) {
			Tree child = t.children()[i];
			double cWidth = paintTree(child, new Point2D.Double(childStartX, childStartY), g2, fM);
			// draw connectors
			wr = treeDimensions(child, fM);
			double lineEndX = childStartX + wr.nodeCenter;
			if (child.label().toString().matches(".*\\*:.*")) {

				g2.setStroke(UNFIXED_STROKE);
				g2.setColor(UNFIXED_COLOR);
				g2.draw(new Line2D.Double(lineStartX, lineStartY, lineEndX, lineEndY));
				g2.setStroke(defaultStroke);
				g2.setColor(defaultColor);
			} else if (child.label().toString().matches(".*L:.*")) {

				double lineCtrl1X = lineStartX + ((lineEndX - lineStartX) / 3) - LINKED_CURVE_OFFSET;
				double lineCtrl1Y = lineStartY + ((lineEndY - lineStartY) / 3);
				double lineCtrl2X = lineStartX + ((lineEndX - lineStartX) / 1.5) + LINKED_CURVE_OFFSET;
				double lineCtrl2Y = lineStartY + ((lineEndY - lineStartY) / 1.5);
				g2.setStroke(LINKED_STROKE);
				g2.setColor(LINKED_COLOR);
				g2.draw(new CubicCurve2D.Double(lineStartX, lineStartY, lineCtrl1X, lineCtrl1Y, lineCtrl2X, lineCtrl2Y,
						lineEndX, lineEndY));
				g2.setStroke(defaultStroke);
				g2.setColor(defaultColor);
			} else if (child.label().toString().matches(".*U:.*")) {

				g2.setStroke(LOCAL_UNFIXED_STROKE);
				g2.setColor(LOCAL_UNFIXED_COLOR);
				g2.draw(new Line2D.Double(lineStartX, lineStartY, lineEndX, lineEndY));
				g2.setStroke(defaultStroke);
				g2.setColor(defaultColor);
			} else {
				g2.draw(new Line2D.Double(lineStartX, lineStartY, lineEndX, lineEndY));
			}
			childStartX += cWidth;
			if (i < t.children().length - 1) {
				childStartX += sisterSkip * fM.stringWidth(" ");
			}
		}
		return treeWidth;
	}

	protected void superPaint(Graphics g) {
		super.paintComponent(g);
	}

	@Override
	public void paintComponent(Graphics g) {
		superPaint(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		defaultStroke = g2.getStroke();
		defaultColor = g2.getColor();
		Dimension space = getSize();
		FontMetrics fM = pickFont(g2, tree, space);
		TreeDimensions dim = treeDimensions(tree, fM);
		double width = dim.width;
		double height = dim.height;
		double startX = 0.0;
		double startY = 0.0;
		if (HORIZONTAL_ALIGN == SwingConstants.CENTER) {
			startX = (space.getWidth() - width) / 2.0;
		}
		if (HORIZONTAL_ALIGN == SwingConstants.RIGHT) {
			startX = space.getWidth() - width;
		}
		if (VERTICAL_ALIGN == SwingConstants.CENTER) {
			startY = (space.getHeight() - height) / 2.0;
		}
		if (VERTICAL_ALIGN == SwingConstants.BOTTOM) {
			startY = space.getHeight() - height;
		}
		paintTree(tree, new Point2D.Double(startX, startY), g2, fM);
		// set preferred size for JScrollPane parent
		int w = size.width;
		int h = size.height;
		if ((fM.getFont().getSize() <= minFontSize) && (width > w)) {
			w = (int) width;
		}
		if ((fM.getFont().getSize() <= minFontSize) && (height > h)) {
			h = (int) height;
		}
		setPreferredSize(new Dimension(w, h));
		revalidate();
	}

	public TreePanel() {
		this(SwingConstants.CENTER, SwingConstants.CENTER, null);
	}

	public TreePanel(Dimension size) {
		this(SwingConstants.CENTER, SwingConstants.CENTER, size);
	}

	public TreePanel(int hAlign, int vAlign, Dimension size) {
		HORIZONTAL_ALIGN = hAlign;
		VERTICAL_ALIGN = vAlign;
		if (size == null) {
			this.size = DEFAULT_SIZE;
		} else {
			this.size = size;
		}
		setPreferredSize(this.size);
	}

	public void setMinFontSize(int size) {
		minFontSize = size;
	}

	public void setMaxFontSize(int size) {
		maxFontSize = size;
	}

	public Font pickFont() {
		Font font = getFont();
		String fontName = font.getName();
		int style = font.getStyle();
		int size = (maxFontSize + minFontSize) / 2;
		return new Font(fontName, style, size);
	}

	public Dimension getTreeDimension(Tree tree, Font font) {
		FontMetrics fM = getFontMetrics(font);
		TreeDimensions dim = treeDimensions(tree, fM);
		return new Dimension((int) dim.width, (int) dim.height);
	}

	public static void main(String[] args) throws IOException {
		TreePanel tjp = new TreePanel();
		// String ptbTreeString1 =
		// "(ROOT (S (NP (DT This)) (VP (VBZ is) (NP (DT a) (NN test))) (. .)))";
		String ptbTreeString = "(ROOT (S (NP (NNP Interactive_Tregex)) (VP (VBZ works)) (PP (IN for) (PRP me)) (. !))))";
		if (args.length > 0) {
			ptbTreeString = args[0];
		}
		Tree tree = (new PennTreeReader(new StringReader(ptbTreeString), new LabeledScoredTreeFactory(
				new StringLabelFactory()))).readTree();
		tjp.setTree(tree);
		tjp.setBackground(Color.white);
		JFrame frame = new JFrame();
		frame.getContentPane().add(tjp, BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.pack();
		frame.setVisible(true);
		frame.setVisible(true);
	}

}
