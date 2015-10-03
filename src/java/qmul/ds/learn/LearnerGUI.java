package qmul.ds.learn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;

import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTupleSet;
import qmul.ds.dag.HorizontalFlipTreeLayout;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;

public class LearnerGUI extends JFrame implements ActionListener, MouseListener {

	private static Logger logger = Logger.getLogger(LearnerGUI.class);

	JPanel topButtonPanel = new JPanel();
	JPanel middleButtonPanel = new JPanel();

	JTable hypDisplayTable = new JTable();

	WordLearner learner;
	JButton parseButton = new JButton("Parse Corpus");
	JButton loadButton = new JButton("Load Corpus");
	JButton learnButton = new JButton("Learn All");
	JButton learnNextButton = new JButton("Learn Next");
	JButton resetButton = new JButton("Reset");
	@SuppressWarnings("serial")
	JTable hypTuplesTable = new JTable();

	JButton saveLexiconButton = new JButton("Save Lexicon");

	

	public LearnerGUI(String type) {
		super("Learner");
		if (type.equals("ttr"))
			learner = new TTRWordLearner();
		else
			learner = new TreeWordLearner();
		init();
		pack();
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	public void init() {
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		topButtonPanel.setLayout(new BoxLayout(topButtonPanel, BoxLayout.X_AXIS));
		parseButton.addActionListener(this);
		learnButton.addActionListener(this);
		loadButton.addActionListener(this);
		saveLexiconButton.addActionListener(this);
		this.learnNextButton.addActionListener(this);
		this.resetButton.addActionListener(this);
		topButtonPanel.add(saveLexiconButton);
		topButtonPanel.add(loadButton);
		topButtonPanel.add(parseButton);
		topButtonPanel.add(learnButton);
		topButtonPanel.add(learnNextButton);
		topButtonPanel.add(resetButton);
		getContentPane().add(topButtonPanel);
		hypTuplesTable.setFillsViewportHeight(true);
		hypTuplesTable.addMouseListener(this);
		hypTuplesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane tableScrollPane = new JScrollPane(this.hypTuplesTable);
		tableScrollPane.setPreferredSize(new Dimension(700, 400));
		getContentPane().add(tableScrollPane);
		hypDisplayTable.addMouseListener(this);
		JScrollPane hypothesisScrollPane = new JScrollPane(hypDisplayTable);

		hypothesisScrollPane.setPreferredSize(new Dimension(700, 400));
		hypothesisScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(hypothesisScrollPane);
		// this.hypothesisDisplay.setLayout(new BoxLayout(hypothesisDisplay,
		// BoxLayout.Y_AXIS));
	}

	private void loadHypTableData() {
		logger.info("Loading Hypothesis Sequences");

		// tm.setRowCount(0);
		DefaultTableModel tm = new DefaultTableModel(3, 10);
		this.hypTuplesTable.setModel(tm);
		tm.setRowCount(0);

		List<List<WordHypothesis>> baseTuples = learner.getHypothesisBase().getHypothesisTuples();
		logger.debug("adding " + baseTuples.size() + " rows");
		Integer i = 0;
		for (List<WordHypothesis> hypList : baseTuples) {
			Vector<String> row = new Vector<String>();
			row.add((i++).toString());
			for (WordHypothesis wh : hypList)
				row.add(wh.toString());

			logger.debug("Adding row:" + row);
			tm.addRow(row);

		}
		// this.hypDataTable.setModel(tm);

		// Integer[] row={1,2,3,4,5,6,7,8,9,0};
		// tm.addRow(row);
		tm.fireTableDataChanged();
		this.hypTuplesTable.setVisible(true);

	}

	private void loadHypSets() {
		logger.info("loading hyps");

		Set<HasWord> words = learner.getHypothesisBase().getWords();

		Vector<String> columnHeaders = new Vector<String>();
		List<Iterator<WordHypothesis>> iterators = new ArrayList<Iterator<WordHypothesis>>();
		for (HasWord w : words) {
			columnHeaders.add(w.word());
			iterators.add(learner.getHypothesisBase().getWordHyps(w).iterator());

		}

		Vector<Vector<String>> data = new Vector<Vector<String>>();

		boolean hasNext = false;
		do {
			Vector<String> newRow = new Vector<String>();
			hasNext = false;
			for (Iterator<WordHypothesis> i : iterators) {

				if (i.hasNext()) {
					WordHypothesis wh = i.next();
					newRow.add(wh.toString());
				} else
					newRow.add("");

				if (i.hasNext()) {
					hasNext = true;

				}
			}
			data.add(newRow);
		} while (hasNext);

		DefaultTableModel tm = new DefaultTableModel(data, columnHeaders);
		this.hypDisplayTable.setModel(tm);
		setColumnWidths(20);
		tm.fireTableDataChanged();
		this.hypDisplayTable.setVisible(true);

	}

	private void setColumnWidths(int a) {
		TableColumnModel m = this.hypDisplayTable.getColumnModel();
		for (int i = 0; i < m.getColumnCount(); i++) {
			// m.getColumn(i).setMaxWidth(150);
			// m.getColumn(i).setMinWidth(150);
			// m.getColumn(i).setWidth(150);
			m.getColumn(i).setPreferredWidth(150);
		}
	}

	public static void main(String[] a) {
		LearnerGUI gui = new LearnerGUI("ttr");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == learnButton) {
			if (!learner.corpusLoaded()) {
				JOptionPane.showMessageDialog(this, "Corpus not loaded");
				return;
			}
			learner.learn();
			loadHypTableData();
			loadHypSets();
		} else if (e.getSource() == this.resetButton) {
			resetTables();
			learner.resetCorpus();

		} else if (e.getSource() == learnNextButton) {
			if (!learner.corpusLoaded()) {
				JOptionPane.showMessageDialog(this, "Corpus not loaded");
				return;
			}
			if (!learner.learnOnce()) {
				JOptionPane.showMessageDialog(this, "End of corpus reached");
				return;
			}
			loadHypTableData();
			loadHypSets();

		} else if (e.getSource() == parseButton) {
			JFileChooser fc = new JFileChooser(Corpus.CORPUS_FOLDER);
			int returnVal = fc.showOpenDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File corpusFile = fc.getSelectedFile();
				try {
					learner.loadAndParseCorpus(corpusFile);
				} catch (IOException ex) {
					logger.error("Could not open corpus file");
					ex.printStackTrace();
					return;
				}
				// This is where a real application would open the file.
				logger.info("loaded and parsed corpus: " + corpusFile.getName());
			} else {
				logger.info("Open copus cancelled by user.");
			}

		} else if (e.getSource() == loadButton) {
			JFileChooser fc = new JFileChooser(Corpus.CORPUS_FOLDER);
			int returnVal = fc.showOpenDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File corpusFile = fc.getSelectedFile();
				try {
					learner.loadCorpus(corpusFile);
					logger.info("loaded corpus");
				} catch (Exception ex) {
					logger.error("Could not open corpus file");
					ex.printStackTrace();
					return;
				}
				// This is where a real application would open the file.
				logger.info("loaded and parsed corpus: " + corpusFile.getName());
			} else {
				logger.info("Open copus cancelled by user.");
			}
		} else if (e.getSource() == this.saveLexiconButton) {
			String answer = JOptionPane.showInputDialog(this, "Howmany hyps from the top?", 5);
			int topN = 0;
			try {
				topN = Integer.parseInt(answer);
			} catch (NumberFormatException ex) {
				ex.printStackTrace();
				return;
			}
			String answerFile = JOptionPane.showInputDialog(this, "Enter file name:", "lexicon.lex");
			if (answerFile == null || answerFile.isEmpty()) {
				logger.info("no file name entered, returning");
				return;
			}

			try {
				learner.getHypothesisBase().saveLearnedLexicon(answerFile, topN);
			} catch (Exception ex) {
				logger.error("Could not save lexicon");
				ex.printStackTrace();
				return;
			}
			// This is where a real application would open the file.
			logger.info("Saved Lexicon to: " + answerFile);

		}

	}

	private void resetTables() {
		DefaultTableModel tm = new DefaultTableModel(3, 10);
		this.hypTuplesTable.setModel(tm);
		tm.setRowCount(0);
		DefaultTableModel tm2 = new DefaultTableModel(3, 5);
		this.hypDisplayTable.setModel(tm2);
		tm2.setRowCount(0);

	}

	@Override
	public void mouseClicked(MouseEvent event) {
		int rowTuples = hypTuplesTable.rowAtPoint(event.getPoint());
		int colTuples = hypTuplesTable.columnAtPoint(event.getPoint()) - 1;
		int rowHypTable = hypDisplayTable.rowAtPoint(event.getPoint());
		int colHypTable = hypDisplayTable.columnAtPoint(event.getPoint());

		WordHypothesis selected;
		if (event.getSource() == hypTuplesTable) {

			selected = this.learner.getHypothesisBase().getHypothesisTuples().get(rowTuples).get(colTuples);
		} else if (event.getSource() == hypDisplayTable) {

			selected = this.learner.getHypothesisBase()
					.getWordHyps(new Word(hypDisplayTable.getColumnName(colHypTable))).get(rowHypTable);
		} else
			return;

		JDialog hypDisplay = new JDialog(this, selected.getName());
		hypDisplay.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);

		Layout<DAGTupleSet, DAGEdge> layout = new HorizontalFlipTreeLayout<DAGTupleSet, DAGEdge>(selected);

		// layout.setSize(new Dimension(500,700));
		VisualizationViewer<DAGTupleSet, DAGEdge> vv = new VisualizationViewer<DAGTupleSet, DAGEdge>(layout);

		// vv.getRenderContext().setVertexLabelTransformer()

		// vv.setSize(800, 300);
		vv.getRenderContext().setEdgeLabelTransformer(new Transformer<DAGEdge, String>() {
			public String transform(DAGEdge edge) {
				String name = edge.getAction().getName();
				if (name.contains("copy"))
					return edge.getAction().toString();

				else
					return edge.getAction().getName();
			}
		});
		vv.getRenderContext().setVertexLabelTransformer(new DAGTupleSetTransformer());

		// vv.getRenderContext().setEdgeArrowPredicate(new Predicate(){
		// public boolean evaluate()
		// })
		// scrollPane.setSize(500, 500);
		vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<DAGTupleSet, DAGEdge>());
		vv.getRenderContext().setEdgeArrowPredicate(new Predicate<Context<Graph<DAGTupleSet, DAGEdge>, DAGEdge>>() {
			@Override
			public boolean evaluate(Context<Graph<DAGTupleSet, qmul.ds.dag.DAGEdge>, qmul.ds.dag.DAGEdge> arg0) {

				return false;
			}
		});

		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.S);
		// vv.getRenderer().get
		vv.setPreferredSize(new Dimension(1200, 200));
		GraphZoomScrollPane scrollPane = new GraphZoomScrollPane(vv);
		// scrollPane.getHorizontalScrollBar().setMaximum(1000);
		// scrollPane.getHorizontalScrollBar().setMinimum(0);
		// scrollPane.setPreferredSize(new Dimension(1000,200));
		// scrollPane.setSize(200, 200);
		// scrollPane.setSize(new Dimension(800,300));
		hypDisplay.add(scrollPane);
		// hypDisplay.setPreferredSize(new Dimension(800,300));
		hypDisplay.pack();
		hypDisplay.setVisible(true);

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}
}

class StringLabeller<DAGEdge> {
	public String transform(DAGEdge edge) {
		return "";
	}

}

class VRenderer implements Renderer.Vertex<DAGTupleSet, DAGEdge> {

	@Override
	public void paintVertex(RenderContext<DAGTupleSet, qmul.ds.dag.DAGEdge> rc,
			Layout<DAGTupleSet, qmul.ds.dag.DAGEdge> layout, DAGTupleSet vertex) {
		GraphicsDecorator graphicsContext = rc.getGraphicsContext();
		Point2D center = layout.transform(vertex);
		Shape shape = null;
		Color color = Color.red;
		FontMetrics fm = graphicsContext.getFontMetrics();
		DAGTupleSetTransformer t = new DAGTupleSetTransformer();
		String label = t.transform(vertex);
		int width = fm.stringWidth(label);
		if (!label.isEmpty())
			shape = new Ellipse2D.Double(center.getX(), center.getY(), width, 20);
		else
			shape = new Ellipse2D.Double(center.getX(), center.getY(), 20, 20);
		graphicsContext.setPaint(color);
		graphicsContext.fill(shape);

	}
}
