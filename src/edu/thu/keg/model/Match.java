package edu.thu.keg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

public class Match {
	String srcURI;//源资源URI
	Hashtable<String, MatchScore> msList;//《目标资源，得分（得分里也包含目标资源）》
	
	public Match(String srcURI) {
		this.srcURI = srcURI;
		this.msList = new Hashtable<String, MatchScore>();
	}
	
	public void addScore (MatchScore ms) {
		if (msList.get(ms.getTarURI()) != null) {
			msList.get(ms.getTarURI()).setScore(ms.getScore());
		} else {
			msList.put(ms.getTarURI(), ms);
		}
	}
	
	public MatchScore getScore (String tarURI) {
		if (tarURI == null)
			return null;
		return msList.get(tarURI);
	}
	
	public ArrayList<MatchScore> orderedScoreList() {
		ArrayList<MatchScore> scoreList = new ArrayList<MatchScore>();
		Iterator<MatchScore> msit = msList.values().iterator();
		MatchScore score = null;
		for (; msit.hasNext(); score = msit.next()) {
			scoreList.add(score);
		}
		Collections.sort(scoreList);
		return scoreList;
	}

	public String getSrcURI() {
		return srcURI;
	}

	public void setSrcURI(String srcURI) {
		this.srcURI = srcURI;
	}

	public Hashtable<String, MatchScore> getMsList() {
		return msList;
	}

	public void setMsList(Hashtable<String, MatchScore> msList) {
		this.msList = msList;
	}
	
	public void clear() {
		msList.clear();
	}
	
	public int size() {
		return this.msList.size();
	}
}
