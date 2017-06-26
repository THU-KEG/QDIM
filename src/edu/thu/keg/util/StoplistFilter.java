package edu.thu.keg.util;

/**
 * Filter which removes stop-listed words.
 */

public class StoplistFilter {

	private StopList stoplist;

	public String stoplistPath;

	/**
	 * Create a new StopListFilter with the stoplist given in <code>stoplistfile</code>
	 */
	public StoplistFilter(String stoplistfile) {
		this(new StopList(stoplistfile));
		this.stoplistPath = stoplistfile;
	}

	/** Create a new StoplistFilter with the given StopList. */
	public StoplistFilter(StopList stoplist) {
		this.stoplist = stoplist;
	}

	public boolean filter(String item) {
		return this.stoplist.contains(item);
	}

}
