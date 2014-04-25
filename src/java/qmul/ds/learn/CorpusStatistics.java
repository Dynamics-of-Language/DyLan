package qmul.ds.learn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CorpusStatistics {
	
	public class Distribution{
		public List<String> categories;
		public List<Integer> frequencies;
		public int tokens;
		public int types;
		public float ratio = 0;
		public String cats;
		
		public Distribution(List<String> cat, List<Integer> freq){
			categories = cat;
			frequencies = freq;
			types = categories.size();
			tokens = 0;
			
			//bubble sort first by freq
			boolean swapped = true;
			while (swapped==true){
				swapped = false;
				for (int i = 0; i<cat.size()-1; i++){
					if (freq.get(i)<freq.get(i+1)){
						freq.add(i,freq.get(i+1));
						freq.remove(i+2);
						cat.add(i,cat.get(i+1));
						cat.remove(i+2);
						swapped = true;
					}	
				}
			}
			cats = "\n";
			
			for (int a=0; a<cat.size(); a++){
				//System.out.println(categories.get(a) + ":" + frequencies.get(a));
				cats+=categories.get(a) + ":" + frequencies.get(a) + "\n";
				tokens+=frequencies.get(a);
			}
			System.out.println("types " + types);
			System.out.println("tokens " + tokens);
			ratio = (float) ((float) tokens)  / ((float) types);
			System.out.println("ratio tokens/type " + ratio);
		}
		public String getRatio() {
			return Float.toString(ratio);
		}
		
		public String getTokens() {
			return Integer.toString(tokens);
		}
		
		public String getTypes() {
			return Integer.toString(types);
		}
		public String getDistributionOrdered(){
			return cats;
		}
	}
	
	public static HashMap<String, Integer> occurences = new HashMap<String,Integer>();
	public static HashMap<Integer, Integer> uttLengths = new HashMap<Integer, Integer>();
	
	public void addWordToken(String word){
		if (occurences.containsKey(word)){
			occurences.put(word,occurences.get(word)+1);
		} else {
			occurences.put(word,1);
		}
	}
	
	public void addUtterance(String utt){
		String[] words = utt.split("\\s+");
		for (String word : words){
			addWordToken(word);
		}
		if (uttLengths.keySet().contains(words.length)){
			uttLengths.put(words.length,uttLengths.get(words.length)+1);
		} else {
			uttLengths.put(words.length, 1);
		}
	}
	
	public boolean containsWord(String word){
		if (occurences.containsKey(word)){
			return true;
		}
		return false;
	}
	
	
	public String finalWordDistribution(){
		System.out.println("final word distribrution:");
		List<String> categories = new ArrayList<String>();
		List<Integer> frequency = new ArrayList<Integer>();
		for (String cat : occurences.keySet()){
			categories.add(cat);
			frequency.add(occurences.get(cat));
		}
		Distribution d = new Distribution(categories, frequency);
		String finalReport = "WORD OCCURENCE STATS: \nTokens = " + d.getTokens() + 
				"\nTypes = " + d.getTypes() + "\nType/Token ratio = " + d.getRatio() + "\n";
				//d.getDistributionOrdered();
		return finalReport;
	}
	
	public String finalUttLengthDistribution(){
		System.out.println("final utterance length distribution:");
		List<String> categories = new ArrayList<String>();
		List<Integer> frequency = new ArrayList<Integer>();
		int minlength = 100;
		int maxlength = 0;
		float meanlength = 0;
		int totalWords = 0;
		int totalSentences = 0;
		for (int cat : uttLengths.keySet()){
			if (cat>maxlength){
				maxlength = cat;
			}
			if (cat<minlength){
				minlength = cat;
			}
			totalWords += (uttLengths.get(cat) * cat);
			totalSentences +=uttLengths.get(cat);
			//shuffle sort... 
			int placeat = 0;
			for (int c=0; c<categories.size(); c++){
				if (Integer.parseInt(categories.get(c))>cat){
					placeat = c;
					break;
				}
			}
			categories.add(placeat, Integer.toString(cat));
			frequency.add(placeat, uttLengths.get(cat));
		}
		meanlength = ((float) totalWords) / ((float) totalSentences);
		Distribution d= new Distribution(categories, frequency);
		
		String finalReport = "\nUTTERANCE LENGTH STATS : \nmin sentence length = " + minlength + "\n" + 
		"max sentence length = " + maxlength + "\n"  + 
		"mean sentence length = " + meanlength + "\n";
		return finalReport;
		//return new Distribution(categories, frequency);
		
		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
