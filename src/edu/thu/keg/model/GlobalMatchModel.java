package edu.thu.keg.model;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.thu.keg.Invoker;
import edu.thu.keg.process.QueryTask;
import edu.thu.keg.util.StringPair;
import edu.thu.keg.util.Util;


public class GlobalMatchModel extends MatchModel {
	public static int QUERY_NUM = 0;
	
	int queryTime;
	Hashtable<String, Integer> srcCount;
	Hashtable<String, Integer> tarCount;
	Hashtable<String, Integer> extraSrcCount;
	Hashtable<String, Integer> extraTarCount;
	
	public GlobalMatchModel() {
		super();
		queryTime = 0;
		srcCount = new Hashtable<String, Integer>();          //Count Table for Evaluation Source Individuals;
		tarCount = new Hashtable<String, Integer>();          //Count Table for Evaluation Target Individuals;
		extraSrcCount = new Hashtable<String, Integer>();     //Count Table for Non-Evaluation Source Individuals;
		extraTarCount = new Hashtable<String, Integer>();     //Count Table for Non-Evaluation Target Individuals;
	}
	
	public int getQueryTime() {
		return queryTime;
	}
	
	public Hashtable<String, Integer> getSrcCount() {
		return srcCount;
	}

	public Hashtable<String, Integer> getTarCount() {
		return tarCount;
	}

	/**
	 * 
	 * @param task              	the QueryTask 
	 * @param evalFlag           	the evaluation flag, for the purpose of the experiment, 
	 *                              just matches belonging to the first section of the queries
	 *                              should be evaluated. Thus the new matches from the following 
	 *                              queries should not be added to the match model. 
	 */
	public void addQuery(QueryTask task, boolean evalFlag) {
		this.queryTime++;
		++QUERY_NUM;
		updateModel(task.getSrcModel(), srcCount, extraSrcCount, evalFlag);
		updateModel(task.getTarModel(), tarCount, extraTarCount, evalFlag);
		updateConcurrence(task.getSrcModel(), task.getTarModel());
		updateMatched(task.getResult());
		updateMatchScore();
		if(Invoker.__bUserFeedback && Invoker.__bNeedQueryMatching){
			StringPair m = recommendCandidateMatch(task);
			if(m != null){
				if(Invoker.__globalRefResults.contains(m)){
					Invoker.__feedbackedResults.put(m, true);
					++Invoker.__exactRecommend;
				}else{
					Invoker.__feedbackedResults.put(m, false);
					++Invoker.__errorRecommend;
				}
			}
		}
	}
	
	private StringPair recommendCandidateMatch(QueryTask task) {
		StringPair m = null;
		List<MatchTriple> tripleList = task.getResult().renderToTripleList();
		Collections.sort(tripleList);
		//System.out.println(tripleList.size());
		Iterator<MatchTriple> itTriple = tripleList.iterator();
		while(itTriple.hasNext()){
			MatchTriple triple = itTriple.next();
			m = new StringPair(triple.srcURI,triple.tarURI);
			if(!Invoker.__feedbackedResults.containsKey(m)){
				return m;
			}
		}
		return null;
	}

	/**
	 * @param model 		The Query Model used to update the Global Model;
	 * @param indiCount		The Global Model for evaluation with URIs and Occurrence Counts.
	 * @param extraCount 	The Global Model for extra Queries.
	 * @param evalFlag		The flag of queries that whether the query will be evaluated or not
	 */
	public void updateModel(OntModel model, Hashtable<String, Integer> indiCount, 
			Hashtable<String, Integer> extraIndiCount, boolean evalFlag) {
		ResIterator rit = model.listSubjects();
		while (rit.hasNext()) {
			Resource r = rit.nextResource();
			if (Util.testIndi(r)) {
				Individual indi = (Individual)r.as(Individual.class);
				String uri = indi.getURI();
				if (indiCount.containsKey(uri)) {
					indiCount.put(uri, indiCount.get(uri) + 1);
				} else if (evalFlag == true) {
					indiCount.put(uri, 1);
				} else if (extraIndiCount.containsKey(uri)) {
					extraIndiCount.put(uri, extraIndiCount.get(uri) + 1);
				} else {
					extraIndiCount.put(uri, 1);
				}
			}
		}
	}
	
	public void updateConcurrence(OntModel srcModel, OntModel tarModel) {
		ResIterator srit = srcModel.listSubjects();
		
		while (srit.hasNext()) {
			Resource srcRes = srit.nextResource();
			if (Util.testIndi(srcRes)) {
				Individual srcIndi = (Individual)srcRes.as(Individual.class);
				String srcURI = srcIndi.getURI();
				Match m = this.matches.get(srcURI);
				// For Source Individuals in Source Evaluation Count, just increase the concurrence with every target individuals.
				if (srcCount.containsKey(srcURI)) {
					if (m == null) {
						m = new Match(srcURI);
						this.addMatch(m);
					}
					ResIterator trit = tarModel.listSubjects();
					while (trit.hasNext()) {
						Resource tarRes = trit.nextResource();
						if (Util.testIndi(tarRes)) {
							Individual tarIndi = (Individual)tarRes.as(Individual.class);
							String tarURI = tarIndi.getURI();
							MatchScore ms = m.getScore(tarURI);
							if (ms == null) {
								ms = new GlobalMatchScore(tarURI, 0, 1, 0);
								m.addScore(ms);
								//System.out.println(srcURI + "\t" + tarURI);
							} else {
								((GlobalMatchScore)ms).setConcurrence(((GlobalMatchScore)ms).getConcurrence() + 1);
							}
						}
					}
				} else { // For Source Individuals not in Evaluation Count, just increase their 
					     // concurrence with individuals in Target Evaluation Count;
					ResIterator trit = tarModel.listSubjects();
					while (trit.hasNext()) {
						Resource tarRes = trit.nextResource();
						if (Util.testIndi(tarRes)) {
							Individual tarIndi = (Individual)tarRes.as(Individual.class);
							String tarURI = tarIndi.getURI();
							if (tarCount.containsKey(tarURI)) {
								if (m == null) {
									m = new Match(srcURI);
									this.addMatch(m);
								}				
								MatchScore ms = m.getScore(tarURI);
								if (ms == null) {
									ms = new GlobalMatchScore(tarURI, 0, 1, 0);
									m.addScore(ms);
								} else {
									((GlobalMatchScore)ms).setConcurrence(((GlobalMatchScore)ms).getConcurrence() + 1);
								}
							}
						}
					}
				}
			}
		}
	}

	public void updateMatched(MatchModel mm) {
		for (Match queryMatch : mm.getMatches().values()) {
			String srcURI = queryMatch.getSrcURI();
			Match globalMatch = this.matches.get(srcURI);
			if (globalMatch == null)
				continue;
			for (MatchScore ms : queryMatch.getMsList().values()) {
				String tarURI = ms.getTarURI();
				try {
					GlobalMatchScore gms = (GlobalMatchScore)(globalMatch.getScore(tarURI));
					if (gms == null)
						continue;
					gms.setMatched(gms.getMatched() + 1);
				} catch (NullPointerException npe) {
					npe.printStackTrace();
					System.out.println(srcURI);
					System.out.println(tarURI);
					if (this.srcCount.containsKey(srcURI))
						System.out.println(srcURI + " : " + srcCount.get(srcURI));
					if (this.tarCount.containsKey(tarURI))
						System.out.println(tarURI + " : " + tarCount.get(tarURI));
					System.exit(-1);
				}
			}
		}
	}
	
	public void updateMatchScore() {
		Iterator<Match> mit = this.matches.values().iterator();
		Match m = null;
		for (; mit.hasNext(); m = mit.next()) {
			if (m == null)
				continue;
			String srcURI = m.getSrcURI();
			Iterator<MatchScore> msit = m.getMsList().values().iterator();
			MatchScore ms = null;
			for (; msit.hasNext(); ms = msit.next()) {
				if (ms == null)
					continue;
				String tarURI = ms.getTarURI();
				//System.err.println("Going to Update Global Match Score");
				if (srcCount.containsKey(srcURI) && tarCount.containsKey(tarURI))
					((GlobalMatchScore)ms).updateScore(srcCount.get(srcURI), tarCount.get(tarURI));
				else if (srcCount.containsKey(srcURI) && extraTarCount.containsKey(tarURI))
					((GlobalMatchScore)ms).updateScore(srcCount.get(srcURI), extraTarCount.get(tarURI));
				else if (extraSrcCount.containsKey(srcURI) && tarCount.containsKey(tarURI))
					((GlobalMatchScore)ms).updateScore(extraSrcCount.get(srcURI), tarCount.get(tarURI));
				else continue;
			}
		}
	}
}

