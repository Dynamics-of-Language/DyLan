package qmul.ds.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultCaret;

import qmul.ds.Generator;
import qmul.ds.ParserTuple;
import qmul.ds.TTRGenerator;
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRRecordType;

public class TTRGeneratorGUI implements ActionListener, MouseListener {

	JTextArea j_ta; // text area containing descriptions
	JPanel j_da; // display area containing images

	public static TTRGenerator<ParserTuple> generator;
	public static List<TTRRecordType> ttrformulae;

	public TTRGeneratorGUI(TTRGenerator<ParserTuple> gen, ArrayList<TTRRecordType> myList) {
		this.generator = gen;
		this.ttrformulae = myList;
		init();
	}

	public void setTTRList(ArrayList<TTRRecordType> myList) {
		this.ttrformulae = myList;
	}

	public void addTTR(TTRRecordType ttr) {
		this.ttrformulae.add(ttr);
	}

	public void init() {
		JFrame frame = new JFrame("Generator");
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		frame.setSize(screenSize.width, screenSize.height - 30);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		j_da = new JPanel();
		j_da.setPreferredSize(new Dimension(250, 250));
		j_da.setBackground(Color.WHITE);
		j_da.setLayout(new FlowLayout());
		JLabel lab = new JLabel("This is where the ttr goals go");
		j_da.add(lab);

		j_ta = new JTextArea();
		// j_ta.setPreferredSize(new Dimension(750,1500)); ///this limits the size, no need, allows constant append
		j_ta.setEditable(true); // /should this be true?- allows text area tp be editable/ not that important
		j_ta.setLineWrap(true);
		j_ta.setWrapStyleWord(true);
		DefaultCaret caret = (DefaultCaret) j_ta.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // NEED THIS FOR SCROLLING!
		j_ta.setVisible(true);// can be turned off to false to test vocals..

		JScrollPane pane1 = new JScrollPane(j_da);
		JScrollPane pane2 = new JScrollPane(j_ta, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		// pane2.setPreferredSize(new Dimension(250,250));

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, pane1, pane2);
		frame.add(split);
		frame.setVisible(true);
		displayTTR();

	}

	// respond to mouse clicks on objects
	public void mouseClicked(MouseEvent me) {
		TTRLab lab = (TTRLab) me.getComponent();

		if (lab != null) {

			this.generator.generate(lab.getTTR());

		}

	}

	// add the objects to the display panel
	public void displayTTR() {
		j_da.removeAll();
		JLabel titlelab = new JLabel("TTR Record Type Goals");
		j_da.add(titlelab);
		final JButton reset = new JButton("reset");
		reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {

				getGenerator().init();
				j_ta.setText("");

			}
		});
		j_da.add(reset);
		for (TTRRecordType myttr : ttrformulae) {
			TTRLab lab = new TTRLab((Formula) myttr);
			lab.addMouseListener(this);
			Border border = LineBorder.createGrayLineBorder();
			lab.setBorder(border);
			lab.setSize(20, 20);
			j_da.add(lab);

		}
		// refresh the panel.
		j_da.updateUI();
	}

	public class TTRLab extends JLabel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		TTRRecordType ttr;

		// static final String name;

		public TTRLab(Formula myTtr) {
			super("<HTML>" + myTtr.toUnicodeString().replaceAll(TTRRecordType.TTR_LINE_BREAK, "<BR>") + "</HTML>");
			ttr = (TTRRecordType) myTtr;

		}

		TTRRecordType getTTR() {
			return ttr;
		}

	}

	public void addGeneratorOutput(String string) {

		this.j_ta.append(string);
		// j_ta.selectAll();

	}

	public Generator getGenerator() {

		return this.generator;
	}

	public void update() {

		int x = j_ta.getText().length();
		j_ta.setCaretPosition(x);
		j_ta.setRows(j_ta.getLineCount()); // this is important for chattool
		// this.j_ta.updateUI();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

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
