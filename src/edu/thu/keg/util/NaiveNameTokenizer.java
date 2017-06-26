package edu.thu.keg.util;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaiveNameTokenizer {
	protected Pattern pattern;

	static StoplistFilter filter = new StoplistFilter("etc/english.stop.txt");

	public NaiveNameTokenizer() {
		this.pattern = Pattern.compile("[A-Z]*[0-9a-zé]*");
	}

	/**
	 * 
	 * @param string
	 * @return
	 */
	public ArrayList<String> tokenize(String string) {
		Matcher matcher = this.pattern.matcher(string);
		ArrayList<String> words = new ArrayList<String>();
		try {
			while (matcher.find()) {
				if (matcher.start() == matcher.end())
					continue;
				String s = matcher.group();
				words.add(s);			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return words;
	}
}
