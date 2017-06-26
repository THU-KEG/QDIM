package edu.thu.keg.model;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class MatchModel {
	Hashtable<String, Match> matches;
	
	public MatchModel() {
		this.matches = new Hashtable<String, Match>();
	}

	public MatchModel(Hashtable<String, Match> matches) {
		super();
		this.matches = matches;
	}
	
	public MatchModel(List<MatchTriple> tripleList) {
		super();
		this.matches = new Hashtable<String, Match>();
		for (int i = 0; i < tripleList.size(); i++) {
			//System.out.println("Lalalaal");
			MatchTriple triple = tripleList.get(i);
			Match m = this.matches.get(triple.getSrcURI());
			if (m == null) {
				m = new Match(triple.getSrcURI());
				addMatch(m);
			}
			MatchScore ms = new MatchScore(triple.getTarURI(), triple.getScore());
			m.addScore(ms);
		}
	}
	
	public void addMatch(Match m) {
		matches.put(m.getSrcURI(), m);
	}
	
	public Match getMatch(String srcURI) {
		return matches.get(srcURI);
	}
	
	public Hashtable<String, Match> getMatches() {
		return matches;
	}
	
	public void output(String fileName) throws Exception {
		FileOutputStream fos = new FileOutputStream(fileName);
		PrintWriter pw = new PrintWriter(fos);
		Iterator<String> suit = matches.keySet().iterator();
		String srcURI = null;
		for (; suit.hasNext(); srcURI = suit.next()) {
			if (srcURI == null)
				continue;
			Match m = matches.get(srcURI);
			Iterator<String> tuit = m.getMsList().keySet().iterator();
			String tarURI = null;
			for (; tuit.hasNext(); tarURI = tuit.next()) {
				if (tarURI == null)
					continue;
				MatchScore ms = m.getScore(tarURI);
				if (ms.getScore() > 0)
					if (this instanceof GlobalMatchModel) {
						GlobalMatchModel model = (GlobalMatchModel)this;
						GlobalMatchScore gms = (GlobalMatchScore)ms;
						pw.println(srcURI + "\t" + tarURI + "\t" + model.getSrcCount().get(srcURI) + "\t" + model.getTarCount().get(tarURI) + "\t" + gms.getConcurrence() + "\t" + gms.getMatched() + "\t" + gms.getScore());
					}
					else 
						pw.println(srcURI + "\t" + tarURI + "\t" + ms.getScore());
			}
		}
		pw.close();
	}
	
	public ArrayList<MatchTriple> renderToTripleList() {
		ArrayList<MatchTriple> tripleList = new ArrayList<MatchTriple>();
		for (String srcURI : matches.keySet()) {
			Match m = matches.get(srcURI);
			for (MatchScore ms : m.getMsList().values()) {
				MatchTriple triple = new MatchTriple(srcURI, ms.getTarURI(), ms.getScore());
				tripleList.add(triple);
			}
		}
		return tripleList;
	}
	
	public HashSet<String> renderToStringSet(double threshold) {
		HashSet<String> result = new HashSet<String>();
		for (String srcURI : matches.keySet()) {
			Match m = matches.get(srcURI);
			for (MatchScore ms : m.getMsList().values()) {
				if (ms.getScore()>= threshold) {
					String resPair = srcURI + "\t" + ms.getTarURI();
					result.add(resPair);
				}
			}
		}
		return result;
	}
	
	public double getBestScore() {
		double bestScore = 0;
		for (Match m : this.matches.values())
			for (MatchScore ms : m.getMsList().values()) {
				if (ms.getScore() > bestScore)
					bestScore = ms.getScore();
			}
		//if (bestScore > 1.0)
		//	System.err.println("Alert : BestScore > 1.0 !! : BestScore = " + bestScore);
		return bestScore;
	}
	
	public void normalize() {
		double bestScore = getBestScore();
		for (Match m : this.matches.values())
			for (MatchScore ms : m.getMsList().values()) {
				ms.setScore(ms.getScore() / bestScore);
			}
	}
	
	public void clear(){
		for (Match m : matches.values())
			m.clear();
		matches.clear();
	}
	
	public int size() {
		int size = 0;
		for (Match m : matches.values()) 
			size += m.size();
		return size;
	}
}
