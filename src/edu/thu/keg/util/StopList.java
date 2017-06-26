package edu.thu.keg.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class StopList {
	private HashSet<String> wordSet;

	/*
	 * Constructs a stoplist with very few stopwords.
	 */

	public StopList() {
		this.wordSet = new HashSet<String>();
		addGenericWords();
	}

	/**
	 * Constructs a new stoplist from the contents of a file. It is assumed that
	 * the file contains stopwords, one on a line. The stopwords need not be in
	 * any order.
	 */

	public StopList(String stopFile) {
		this.wordSet = new HashSet<String>();

		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(stopFile));

			while (reader.ready())
				this.wordSet.add(new String(reader.readLine()));

		} catch (IOException e) {
			e.printStackTrace(System.err);
			addGenericWords();
		}
	}

	/**
	 * Adds some extremely common words to the stoplist.
	 */
	private void addGenericWords() {
		String[] genericWords = { "a", "an", "the", "and", "or", "but", "nor" };
		for (int i = 1; i < 7; i++)
			this.wordSet.add(genericWords[i]);
	}

	/**
	 * Returns true if the word is in the stoplist.
	 */
	public boolean contains(String word) {
		return this.wordSet.contains(word);
	}

}
