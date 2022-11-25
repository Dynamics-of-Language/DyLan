package qmul.ds.learn;

import java.util.HashMap;
import java.util.Properties;

import com.github.andrewoma.dexx.collection.Set;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import qmul.ds.formula.TTRRecordType;

public class BestFirstGenerator // extends what?
{

	public static HashMap<String, HashMap<TTRRecordType, Double>> loadModelFromFile() throws IOException, ClassNotFoundException
	{
        FileInputStream file = new FileInputStream("model.txt");
        ObjectInputStream ois = new ObjectInputStream(file);
        HashMap<String, HashMap<TTRRecordType, Double>> table = (HashMap<String, HashMap<TTRRecordType, Double>>) ois.readObject();
        ois.close();
		
		return table;
	}
	
	/*
	 * Will apply all possible single computational and lexical actions at the current DAG node, without
	 * any traversal of the DAG
	 */
	protected void applyActionsInBeam()
	{
		
	}
	
	
	
	
	public static void main(String[] args)
	{
		try {
			HashMap<String, HashMap<TTRRecordType, Double>> model = loadModelFromFile();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// generate :(
			

	}

}
