package edu.thu.keg.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import com.hp.hpl.jena.ontology.OntModel;

import edu.thu.keg.Invoker;
import edu.thu.keg.model.MatchModel;
import edu.thu.keg.model.MatchTriple;
import edu.thu.keg.util.Util;

public class QueryPostprocessor {
	public static int TYPE_TF = 1;
	public static int TYPE_OOF = 2;
	public static int TYPE_TF_OOF = 3;
	
	int type;
	double threshold = 0.0;
	QueryTask task;
	
	public QueryPostprocessor(QueryTask task, int type) {
		this.task = task;
		this.type = type;
	}
	
	public QueryPostprocessor(QueryTask task, int type, double threshold) {
		this.task = task;
		this.type = type;
		this.threshold = threshold;
	}
	
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	
	public void postprocess() {
		//System.out.println("Before Preprocess: " + task.getResult().size());
		if (this.type == TYPE_TF)
			thresholdFilter(threshold);
		else if (this.type == TYPE_OOF)
			oneToOneFilter();
		else if (this.type == TYPE_TF_OOF) {
			thresholdFilter(threshold);
			oneToOneFilter();
		}
		//System.out.println("After Postprocess: " + task.getResult().size());
	}
	
	public void thresholdFilter(double threshold) {
		ArrayList<MatchTriple> tripleList = task.getResult().renderToTripleList();
		ArrayList<MatchTriple> newTripleList = new ArrayList<MatchTriple>();
		for (int i = 0; i < tripleList.size(); i++) {
			//System.out.println(tripleList.get(i));
			if (tripleList.get(i).getScore() >= threshold){
				if(Invoker.__bUseNecessaryProperty){
					String strSrcPage = Invoker.__srcPagePropMap.get(tripleList.get(i).getSrcURI());
					String strTarPage = Invoker.__tarPagePropMap.get(tripleList.get(i).getTarURI());
					if(strSrcPage != null && strTarPage != null){
						strSrcPage = Util.refinePage(strSrcPage);
						strTarPage = Util.refinePage(strTarPage);
						if(!strSrcPage.equals(strTarPage)){
							//System.out.println("Skip");
							continue;
						}
					}
				}
				newTripleList.add(tripleList.get(i));
			}
		}
		task.getResult().clear();
		//System.err.println("New Triple List size: " + newTripleList.size());
		//System.out.println(newTripleList.get(0));
		MatchModel newMm = new MatchModel(newTripleList);
		//System.err.println("New Match Model size: " +  newMm.size());
		task.setResult(newMm);
	}
	
	public void oneToOneFilter() {
		ArrayList<MatchTriple> tripleList = task.getResult().renderToTripleList();
		ArrayList<MatchTriple> newTripleList = new ArrayList<MatchTriple>();
		HashSet<String> srcSet = new HashSet<String>();
		HashSet<String> tarSet = new HashSet<String>();
		Collections.sort(tripleList);
		for (int i = 0 ; i < tripleList.size(); i++) {
			MatchTriple triple = tripleList.get(i);
			if (srcSet.contains(triple.getSrcURI()))
				continue;
			else if (tarSet.contains(triple.getTarURI()))
				continue;
			else {
				newTripleList.add(triple);
				srcSet.add(triple.getSrcURI());
				tarSet.add(triple.getTarURI());
			}
		}
		MatchModel newMm = new MatchModel(newTripleList);
		task.getResult().clear();
		task.setResult(newMm);
	}	
}
