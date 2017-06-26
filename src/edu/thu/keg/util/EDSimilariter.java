package edu.thu.keg.util;

import com.wcohen.ss.Levenstein;
import com.wcohen.ss.api.StringDistance;

public class EDSimilariter {
	public static StringDistance sd = new Levenstein();
	
	public static double sim(String srcName, String tarName) {
		double length = Math.max(srcName.length(), tarName.length());
		double score = (sd.score(srcName, tarName) + length) / length;
		return score;
	}
}
