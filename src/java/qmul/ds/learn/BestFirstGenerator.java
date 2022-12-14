package qmul.ds.learn;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.github.andrewoma.dexx.collection.Set;

import java.util.regex.Matcher;

import org.apache.jena.tdb.store.Hash;
import qmul.ds.formula.TTRRecordType;

public class BestFirstGenerator
{
	static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
	/**
	 * Reads the learnt (word, feature, prob) table from a csv file.
	 *
	 * @return table: A HashMap<String, HashMap<TTRRecordType, Double>>
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static HashMap<String, HashMap<TTRRecordType, Double>> loadModelFromFile() throws IOException, ClassNotFoundException
	{
		HashMap<String, HashMap<TTRRecordType, Double>> table = new HashMap<String, HashMap<TTRRecordType, Double>>();
		int lineNumber = 0;
		FileReader reader = new FileReader(grammarPath+"model.csv");
		BufferedReader stream = new BufferedReader(reader);
		ArrayList<TTRRecordType> features = new ArrayList<TTRRecordType>();
		String line;
		while((line = stream.readLine()) != null){
			String lineList[] = line.split(",");
			if (lineNumber == 0) // Features line // todo make this mor efficient since it only happens when reawding line one
			{
				for (int i = 1; i < lineList.length; i++) // Ignoring the first element since it's "WORDS\FEATURES"
					features.add(TTRRecordType.parse(lineList[i]));
				lineNumber++;
				continue;
			}
			// Otherwise we have a <WORD,PROBS> line.
			String word = lineList[0];
			HashMap<TTRRecordType, Double> row = new HashMap<TTRRecordType, Double>();
			ArrayList<Double> probs = new ArrayList<Double>();
			for(int i=1; i<lineList.length; i++){
				Double prob = Double.parseDouble(lineList[i]);
				probs.add(prob);
			}
			for(int i=0; i<features.size(); i++)
				row.put(features.get(i), probs.get(i));
			table.put(word, row);
			lineNumber++;
		}
		return table;
	}
	
	/*
	 * Will apply all possible single computational and lexical actions at the current DAG node, without
	 * any traversal of the DAG
	 */
	protected void applyActionsInBeam()
	{
		
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		System.out.println(loadModelFromFile());
	}
}
