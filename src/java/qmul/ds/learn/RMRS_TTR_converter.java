package qmul.ds.learn;

import org.apache.log4j.Logger;

import qmul.ds.formula.ttr.TTRRecordType;

public class RMRS_TTR_converter {

	private static Logger logger = Logger.getLogger(Corpus.class);
	
	
	public void validateTTR(String ttr){
		try {
			logger.info(TTRRecordType.parse(ttr));
		} catch (Exception e) {
			logger.error(ttr + " not valid TTR RT! " + e.getMessage());
		}
	}
	
	
	public RMRS_TTR_converter() {
		
		
	}
	
	/**
	 * Main method from string of rmrs returns an equivalent record type
	 * @param rmrs
	 * @return
	 */
	public String convert(String rmrs){
		String ttr = ""; //the string to be returned
		String rmrsTest = rmrs.substring(1, rmrs.length()-1);
		logger.info(rmrsTest);
		
		
		validateTTR(ttr);
		logger.info("ORIGINAL = " + rmrs + "\nFINAL = " + ttr);
		return ttr;
	}
	
	
	public static void main(String[] args){
		
		String exampleUtt = "nach rechts drehen";
		String exampleRMRS = "[ [l0:a1:e2]\n{ }\n ARG1(a1,x8),\nl6:a7:addressee(x8),\n ARG1(a18,e2), \nl17:a18:_rechts(e19),\n l0:a1:_drehen(e2)]";
		RMRS_TTR_converter rmttr = new RMRS_TTR_converter();
		rmttr.convert(exampleRMRS);
	}
	
	

}
