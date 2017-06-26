package edu.thu.keg.util;

import java.util.ArrayList;
import java.util.List;

public class VDSimilariter {
//	public static NaiveNameTokenizer tokenizer = new NaiveNameTokenizer();
	
	public static double sim(List<String> labelList1, List<String> labelList2) {
		double sim = 0;
		
//		ArrayList<String> labelList1 = tokenizer.tokenize(label1);
//		ArrayList<String> labelList2 = tokenizer.tokenize(label2);
		
		double sim1 = simBNSL(labelList1, labelList2);
		double sim2 = simBNSL(labelList2, labelList1);
		
		sim = (sim1 + sim2) / 2;
		return sim;
	}
	
	public static double simBNSL(List<String> list1, List<String> list2) {
		double score = 0;
		
		for (int i = 0; i < list1.size(); i++) {
			String str1 = list1.get(i);
			double maxScore = 0;
			for (int j = 0; j < list2.size(); j++) {
				double strScore = simBNS(str1, list2.get(j));
				if (strScore > maxScore) 
					maxScore = strScore;
			}
			score += maxScore;
		}
		score = score / list1.size();
		return score;
	}
	
	public static double simBNS(String str1, String str2) {		
		if (str1 == null || str2 == null) {
			return 0;
		}
		
		if (isAbbr(str1) || (isAbbr(str2))) {
			if (isAbbr(str1) && isAbbr(str2)) {
				if (str1.equals(str2))
					return 1.0;
				else return 0;
			} else if (isAbbr(str1)) {
				if (str1.charAt(0) == str2.charAt(0) || 
						str1.charAt(0) == str2.charAt(0) - 32 ||
						str1.charAt(0) == str2.charAt(0) + 32)
					return 0.75;
				else return 0;
			} else return 0;
		} else 
			return EDSimilariter.sim(str1.toLowerCase(), str2.toLowerCase());
	}
	
	public static boolean isAbbr(String str) {
		if (str == null)
			return false;
		
		if (str.endsWith("."))
			str = str.substring(0, str.length() - 1);
		
		if (str.length() == 1 && Character.isUpperCase(str.charAt(0)))
			return true;
		
		return false;
	}
	
	public static void main(String args[]) {
		/*System.out.println('a' + 0);
		System.out.println('A' + 0);
		System.out.println('A' - 26);
		ArrayList<String> list = tokenizer.tokenize("D. Lee");
		for (int i = 0; i < list.size(); i++) {
			System.out.println(list.get(i));
		}
		System.out.println(sim("D. Lee", "david lee"));*/
	}
}
