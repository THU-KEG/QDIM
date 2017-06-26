package edu.thu.keg.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.rdf.model.Model;
import org.w3c.rdf.model.ModelException;
import org.w3c.rdf.model.NodeFactory;
import org.w3c.rdf.util.RDFFactory;
import org.w3c.rdf.util.RDFFactoryImpl;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.interdataworking.mm.alg.MapPair;

import edu.thu.keg.Invoker;
import edu.thu.keg.model.IndividualParameter;
import edu.thu.keg.model.Match;
import edu.thu.keg.model.MatchModel;
import edu.thu.keg.model.MatchScore;
import edu.thu.keg.model.MatchTriple;
import edu.thu.keg.model.Successor;

import edu.thu.keg.util.StringPair;
import edu.thu.keg.util.Util;

public class SFMatcher {
	
	static class MySelector implements Selector{

		@Override
		public com.hp.hpl.jena.rdf.model.RDFNode getObject() {
			// TODO Auto-generated method stub
			return null;
		}
  
		@Override
		public Property getPredicate() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public com.hp.hpl.jena.rdf.model.Resource getSubject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isSimple() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean test(Statement arg0) {
			if(arg0.getPredicate().toString().indexOf("author") >= 0){
				return true;
			}
			return false;
		}
		
	}
	
	QueryTask task;
	
	public SFMatcher(QueryTask task) {
		this.task = task;
	}
	
	public void flooding(){
		com.interdataworking.mm.alg.Match sf = new com.interdataworking.mm.alg.Match();
		sf.TEST = false;
		sf.DEBUG = false;
		sf.formula = com.interdataworking.mm.alg.Match.FORMULA_TTT;
		sf.TIMEOUT = 50000;
		sf.RESIDUAL_VECTOR_LENGTH = 0.0001;
		
		//sf.FLOW_GRAPH_TYPE = com.interdataworking.mm.alg.Match.FG_EQUAL;
		//sf.FLOW_GRAPH_TYPE = com.interdataworking.mm.alg.Match.FG_AVG;
		sf.FLOW_GRAPH_TYPE = com.interdataworking.mm.alg.Match.FG_TOTALS;
		
		RDFFactory rf = new RDFFactoryImpl();
		NodeFactory nf = rf.getNodeFactory();
		
		//init source model
		Model srcModel = rf.createModel();
		Selector selector = new MySelector();
		StmtIterator stit = task.getSrcModel().listStatements(selector);
		Map<String,org.w3c.rdf.model.Resource> srcS2RMap = new HashMap<String,org.w3c.rdf.model.Resource>();
		Map<String,org.w3c.rdf.model.Resource> tarS2RMap = new HashMap<String,org.w3c.rdf.model.Resource>();
		org.w3c.rdf.model.Resource author = null;
		try {
			author = (org.w3c.rdf.model.Resource) nf.createResource("http://author");
		} catch (ModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (stit.hasNext()) {
			Statement stat = stit.next();
			try {
				org.w3c.rdf.model.Resource sub = srcS2RMap.get(stat.getSubject().getURI());
				if(sub == null){
					sub = (org.w3c.rdf.model.Resource) nf.createResource(stat.getSubject().getURI());
					srcS2RMap.put(stat.getSubject().getURI(), sub);
				}
				
				org.w3c.rdf.model.Resource obj = srcS2RMap.get(((Resource)stat.getObject()).getURI());
				if(obj == null){
					obj = (org.w3c.rdf.model.Resource) nf.createResource(((Resource)stat.getObject()).getURI());
					srcS2RMap.put(((Resource)stat.getObject()).getURI(), obj);
				}
				srcModel.add(nf.createStatement(sub,author,obj));
			} catch (ModelException e) {
				System.out.println(stat);
				e.printStackTrace();
			}
		}
		
		//init target model
		Model tarModel = rf.createModel();
		stit = task.getTarModel().listStatements(selector);
		while (stit.hasNext()) {
			Statement stat = stit.next();
			try {
				org.w3c.rdf.model.Resource sub = tarS2RMap.get(stat.getSubject().getURI());
				if(sub == null){
					sub = (org.w3c.rdf.model.Resource) nf.createResource(stat.getSubject().getURI());
					tarS2RMap.put(stat.getSubject().getURI(), sub);
				}
				
				org.w3c.rdf.model.Resource obj = tarS2RMap.get(((Resource)stat.getObject()).getURI());
				if(obj == null){
					obj = (org.w3c.rdf.model.Resource) nf.createResource(((Resource)stat.getObject()).getURI());
					tarS2RMap.put(((Resource)stat.getObject()).getURI(), obj);
				}
				tarModel.add(nf.createStatement(sub,author,obj));
			} catch (ModelException e) {
				System.out.println(stat);
				e.printStackTrace();
			}
		}
		
		List<MatchTriple> tripleList = task.getResult().renderToTripleList();;
		Iterator<MatchTriple> iterTriple = tripleList.iterator();
		List<MapPair> initMapList = new ArrayList<MapPair>(tripleList.size());
		while (iterTriple.hasNext()) {
			MatchTriple triple = iterTriple.next();
			org.w3c.rdf.model.Resource sub = srcS2RMap.get(triple.getSrcURI());
			org.w3c.rdf.model.Resource obj = tarS2RMap.get(triple.getTarURI());
			if(sub == null){
				System.out.println("sub is null");
			}
			if(obj == null){
				System.out.println("obj is null");
			}
			initMapList.add(new MapPair(sub,obj,triple.getScore()));
		}
		
		MapPair[] results = null;
		try {
			results = sf.getMatch(srcModel, tarModel, initMapList, null);
		} catch (ModelException e) {
			e.printStackTrace();
		}
		MapPair.sort(results);
		// ThresholdMatch.MAX_RETAIN = 1;
		// ThresholdMatch.THRESHOLD_PERCENTAGE = 1.00;
		// List selected = new ThresholdMatch().getThresholdMatch(results);
		// com.interdataworking.mm.alg.Match.dump(results);

		tripleList.clear();
		for (int i = 0; i < results.length; i++) {
			MapPair result = results[i];
			org.w3c.rdf.model.Resource left = (org.w3c.rdf.model.Resource) result.getLeftNode();
			org.w3c.rdf.model.Resource right = (org.w3c.rdf.model.Resource) result.getRightNode();
			
			try {
				tripleList.add(new MatchTriple(left.getURI(), right.getURI(), result.sim_));
			} catch (ModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		MatchModel newMm = new MatchModel(tripleList);
		MatchModel mm = task.result;
		merge(mm,newMm);
		//task.result = newMm;
		/*System.out.println("*****************************************");
		for (Match m : mm.getMatches().values())
			for (MatchScore ms : m.getMsList().values()) {
				double prevScore = ms.getScore();
				double curScore = newMm.getMatches().get(m.getSrcURI()).getScore(ms.getTarURI()).getScore();
				System.out.println(m.getSrcURI() + "\t" + ms.getTarURI() + "\t" + prevScore + "\t" + curScore);
			}
		System.out.println("*****************************************");*/	
	}
	
	private void merge(MatchModel mm, MatchModel newMm) {
		// TODO Auto-generated method stub
		Hashtable<String,Match> matches = mm.getMatches();
		Hashtable<String,Match> newMatches = newMm.getMatches();
		Iterator<Entry<String,Match>> itNewMatches = newMatches.entrySet().iterator();
		while(itNewMatches.hasNext()){
			Entry<String,Match> curMatchEntry = itNewMatches.next();
			Match m = matches.get(curMatchEntry.getKey());
			if(m != null){
				Match newM = curMatchEntry.getValue();
				Iterator<Entry<String,MatchScore>> itNewMatchScores = newM.getMsList().entrySet().iterator();
				while(itNewMatchScores.hasNext()){
					Entry<String,MatchScore> curScoreEntry = itNewMatchScores.next();
					MatchScore ms = m.getScore(curScoreEntry.getKey());
					if(ms != null){
						if(Invoker.__bUserFeedback){
							Boolean bValue = Invoker.__feedbackedResults.get(new StringPair(m.getSrcURI(),ms.getTarURI()));
							if(bValue != null){
								if(bValue.booleanValue()){
									ms.setScore(1);
								}
							}else{
								ms.setScore((ms.getScore() + curScoreEntry.getValue().getScore()) / 2);
							}	
						}else
							ms.setScore((ms.getScore() + curScoreEntry.getValue().getScore()) / 2);
					}else{
						//System.out.println("Find/Add New MatchScore!!!");
						if(Invoker.__bUserFeedback){
							Boolean bValue = Invoker.__feedbackedResults.get(new StringPair(newM.getSrcURI(),curScoreEntry.getValue().getTarURI()));
							if(bValue != null){
								if(bValue.booleanValue()){
									curScoreEntry.getValue().setScore(1);
									m.addScore(curScoreEntry.getValue());
								}
							}//m.addScore(curScoreEntry.getValue());
						}
					}
				}
			}else{
				//System.out.println("Find New Match!!!");
				//mm.addMatch(curMatchEntry.getValue());
				Match newM = curMatchEntry.getValue();
				Match nm = null;
				if(Invoker.__bUserFeedback){
					Iterator<Entry<String,MatchScore>> itNewMatchScores = newM.getMsList().entrySet().iterator();
					while(itNewMatchScores.hasNext()){
						Entry<String,MatchScore> curScoreEntry = itNewMatchScores.next();			
						Boolean bValue = Invoker.__feedbackedResults.get(new StringPair(newM.getSrcURI(),curScoreEntry.getValue().getTarURI()));
						if(bValue != null){
							if(bValue.booleanValue()){
								if(nm == null){
									nm = new Match(newM.getSrcURI());
								}
								curScoreEntry.getValue().setScore(1);
								nm.addScore(curScoreEntry.getValue());
							}
						}
					}
				}
				mm.addMatch(nm);
			}
		}
	}

	public void flooding(int times) {
		Hashtable<String, Hashtable<String, Successor>> srcFloodingModel = genFloodingModel(task.getSrcModel());
		Hashtable<String, Hashtable<String, Successor>> tarFloodingModel = genFloodingModel(task.getTarModel());
		for (int i = 0; i < times; i++) {
			MatchModel newMm = flooding(task.getResult(), srcFloodingModel, tarFloodingModel);
			task.getResult().clear();
			task.setResult(newMm);
		}
	}
	
	public MatchModel flooding(MatchModel mm, Hashtable<String, Hashtable<String, Successor>> srcModel, 
			Hashtable<String, Hashtable<String, Successor>> tarModel) {
		MatchModel newMm = new MatchModel();
		for (Match m : task.getResult().getMatches().values()) {
			String srcURI = m.getSrcURI();
			Hashtable<String, Successor> srcNbrs = srcModel.get(srcURI);
			Match newM = new Match(srcURI);
			for (MatchScore ms : m.getMsList().values()) {
				String tarURI = ms.getTarURI();
				double score = ms.getScore();
				Hashtable<String, Successor> tarNbrs = tarModel.get(tarURI);
				if (srcNbrs.size() == 0 || tarNbrs.size() == 0) {
					newM.addScore(new MatchScore(tarURI, score));
				} else {
					for (String srcProp : srcNbrs.keySet()) {
						if (tarNbrs.containsKey(srcProp)) {
							int srcPropSize = srcNbrs.get(srcProp).size();
							int tarPropSize = tarNbrs.get(srcProp).size();
							double fWeight = 1.0 / (srcPropSize * tarPropSize); 
							for (String srcNbr : srcNbrs.get(srcProp).getNeighbors()) {
								Match nbrMatch = null;
								if ((nbrMatch = mm.getMatch(srcNbr)) == null)
									continue;
								for (String tarNbr : tarNbrs.get(srcProp).getNeighbors()) {
									if (nbrMatch.getScore(tarNbr) == null)
										continue;
									else
										score += fWeight * nbrMatch.getScore(tarNbr).getScore();
								}
							}
						}
					}
					newM.addScore(new MatchScore(tarURI, score));
				}
			}
			newMm.addMatch(newM);
		}
		newMm.normalize();
		
		/*System.out.println("*****************************************");
		for (Match m : mm.getMatches().values())
			for (MatchScore ms : m.getMsList().values()) {
				double prevScore = ms.getScore();
				double curScore = newMm.getMatches().get(m.getSrcURI()).getScore(ms.getTarURI()).getScore();
				System.out.println(m.getSrcURI() + "\t" + ms.getTarURI() + "\t" + prevScore + "\t" + curScore);
			}
		System.out.println("*****************************************");*/
		
		return newMm;
	}
	
	public Hashtable<String, Hashtable<String, Successor>> genFloodingModel(OntModel model){
		Hashtable<String, Hashtable<String, Successor>> fModel = new Hashtable<String, Hashtable<String, Successor>>();
		ResIterator rit = model.listSubjects();
		while (rit.hasNext()) {
			Resource res = rit.nextResource();
			if (Util.testIndi(res)) {
				Individual indi = (Individual)(res.as(Individual.class));
				String indiURI = indi.getURI();
				if (fModel.get(indiURI) == null)
					fModel.put(indiURI, new Hashtable<String, Successor>());
				StmtIterator stit = indi.listProperties();
				while (stit.hasNext()) {
					Statement st = stit.nextStatement();
					//System.out.println(st.getSubject() + "\t" + st.getPredicate() + "\t" + st.getObject());
					if (st.getPredicate().getURI().equals(Util.RDF_TYPE))
						continue;
					if (st.getObject().canAs(Individual.class)) {
						String propURI = st.getPredicate().getURI();
						String rvsPropURI = propURI + "/reverse";
						String nbrURI = st.getObject().as(Individual.class).getURI();
						
						// add the neighbor information to the adjacent data structure of the individual;
						if (fModel.get(indiURI).get(propURI) == null) {
							Successor succ = new Successor(propURI);
							succ.addNeighbor(nbrURI);
							fModel.get(indiURI).put(propURI, succ);
						} else {
							fModel.get(indiURI).get(propURI).addNeighbor(nbrURI);
						}
						
						// add the reverse neighbor information to the adjacent data structure of the neighbor;
						if (fModel.get(nbrURI) == null)
							fModel.put(nbrURI, new Hashtable<String, Successor>());
						if (fModel.get(nbrURI).get(rvsPropURI) == null) {
							Successor succ = new Successor(rvsPropURI);
							succ.addNeighbor(indiURI);
							fModel.get(nbrURI).put(rvsPropURI, succ);
						} else {
							fModel.get(nbrURI).get(rvsPropURI).addNeighbor(indiURI);
						}
					}
				}
			}
		}
		
		System.out.println("*******************Flooding Model Output Begins********************");
		for (String srcURI : fModel.keySet()) {
			for (String propURI : fModel.get(srcURI).keySet()) {
				for (String tarURI : fModel.get(srcURI).get(propURI).getNeighbors())
					System.out.println(srcURI + "\t" + propURI + "\t" + tarURI); 
			}
		}
		System.out.println("*******************Flooding Model Output Ends********************");
		
		return fModel;
	}
}
