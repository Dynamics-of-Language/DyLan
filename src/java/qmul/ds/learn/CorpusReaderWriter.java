package qmul.ds.learn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class CorpusReaderWriter {

	private static Logger logger = Logger.getLogger(Corpus.class);
	public static List<String[]> corpusSource = new ArrayList<String[]>();//string for converting
	public static List<String[]> missed = new ArrayList<String[]>(); // strings not converted successfully
	public static List<String> omitList = new ArrayList<String>();//strings to be ommitted
	public static int maxLength = 7;//max length of utterances
	public static String sentence = "([^\\.\\?]*)(\\s+[\\.\\?])$";
	
	CorpusReaderWriter(String corpusSourceFolder){
		populateOmit();
		readInCHILDES(corpusSourceFolder);
	}
	
	public static void pause() {
		System.out.println("Press enter to continue...");
		try {
			System.in.read();
		} catch (Exception e) {}
	}
	
	
	/**
	 * Method to populate {@ corpusSource list pairs of <sentence,sem> from the 20 files in childes corpus folder
	 */
	public static void readInCHILDES(String corpusSourceFolder) {
		Pattern p = Pattern.compile(sentence);
		for (int i = 1; i <= 20; i++) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(new File(corpusSourceFolder + File.separator + "trainPairs_"
						+ i)));
			} catch (Exception e) {
				logger.error(e.getMessage());
				System.exit(0);
			}

			try {
				String line;
				String[] pair = new String[3]; // will in fact be a tuple of <sentence, formula, file number>
				while ((line = reader.readLine()) != null) {
					// line = comment(line.trim());
					if ((line == null) || (line.isEmpty())) {
						continue;
					}
					pair[2] = Integer.toString(i);
					if (line.startsWith("Sent:")) {
						pair[0] = line.substring(line.indexOf(":") + 1).trim();
						if (!p.matcher(pair[0]).matches()) {
							pair[0]= "STRING PUNCT!" + pair[0];
						} else {
							//TODO checking for repeats
							String[] morphemes = pair[0].split("\\s+");
							String newString = "";
							for (int m=0; m<morphemes.length; m++){
								//TODO for now contactenating ed's
								if (morphemes[m].equals("ed")){
									newString+=morphemes[m];
								} else {
									newString+=" "+morphemes[m];
								}
								if (m<0&&morphemes[m].equals(morphemes[m-1])){
									newString+="_REPEAT!_";
								}
							}
							pair[0] = newString.substring(0,newString.length()-1).trim();
						}
					} else if (line.startsWith("Sem:")) {
						pair[1] = line.substring(line.indexOf(":") + 1).trim();
					} else if (line.startsWith("//Sent:")){
						pair[0] = line.substring(line.indexOf(":") + 1).trim();
					} else if (line.startsWith("//Sem:")){
						pair[1] =  line.substring(line.indexOf(":") + 1).trim();
					} else if (line.startsWith("//example_end")){
						missed.add(pair);
						pair = new String[3]; 
					} else if (line.startsWith("example_end")) {
						if (pair[0] == null || pair[1] == null) {
							logger.error("Hasn't read in example!" + pair.toString());
						} else {
							if (omitList.contains(pair[1])||pair[0].startsWith("whose")||pair[0].split("\\s+").length>maxLength) {
								logger.debug("missed in " + pair[2]);
								missed.add(pair);
							} else {
								corpusSource.add(pair);
								logger.debug("Adding example" + pair.toString());
							}
						}
						pair = new String[3]; // reset
					}
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("Error reading templates from stream " + reader);
			}
	
		}
		logger.info("Read " + corpusSource.size() + " examples from " + corpusSourceFolder + " folder");
		pause();
	}
	
	public void writeToFile(String corpusSourceFolder, List<String[]> done, CorpusStatistics corpusStats){
		PrintStream out = null;
		FileOutputStream fileOpen = null;
		PrintStream out1 = null;
		FileOutputStream fileOpen1 = null;
		try {

			fileOpen = new FileOutputStream(corpusSourceFolder + File.separator + "CHILDESconversion.txt");
			out = new PrintStream(fileOpen);
			fileOpen1 = new FileOutputStream(corpusSourceFolder + File.separator + "missedCHILDES.txt");
			out1 = new PrintStream(fileOpen1);
			int m = 0;
			int c = 0;
			logger.info("missed : " + missed.size());
			for (String[] miss : missed) {
				logger.info(miss[1] + miss[2]);
				out1.print(miss[0] + "\n : " + miss[1] + "\n :" + miss[2] + "\n\n");
				m++;
			}
			out1.print(m);
			logger.info("CHILDES");
			for (String[] thepair : done) {
				System.out.println(thepair[0] + "\n : " + thepair[1] + "\n : " + thepair[2] + "\n\n");
				out.print("Sent : " + thepair[0] + "\nSem : " + thepair[1] + "\nFile : " + thepair[2] + "\n\n");
				c++;
			}
			out.print("END_OF_CORPUS\n\n");
			out.print(c);
			out.print("\n" + corpusStats.finalUttLengthDistribution());
			out.print("\n" + corpusStats.finalWordDistribution());
			//System.out.println(corpusStats.finalWordDistribution());
		} catch (Exception e) {
			logger.error("Couldn't write to file!");
		} finally {
			if (out != null)
				out.close();
		}
		
	}
	
	
	public void populateOmit(){
		omitList.add("lambda $0_{ev}.not($0,)");
		omitList.add("lambda $0_{ev}.not(and(pro|me,,$0)");
		omitList.add("lambda $0_{e}.and($0)");
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
