package edu.thu.keg.process;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.thu.keg.Invoker;
import edu.thu.keg.control.LabelMatcherProcessUnit;
import edu.thu.keg.model.IndividualParameter;
import edu.thu.keg.model.Match;
import edu.thu.keg.model.MatchModel;
import edu.thu.keg.model.MatchScore;
import edu.thu.keg.util.AnyPair;
import edu.thu.keg.util.EDSimilariter;
import edu.thu.keg.util.StringPair;
import edu.thu.keg.util.Util;
import edu.thu.keg.util.VDSimilariter;

public class LabelMatcher {
	public static int TYPE_ED = 1;
	public static int TYPE_VD = 2;
	public static Map<StringPair,Double> __labelMatchCache = null;
	
	protected boolean _bUseCache = false;
	protected QueryTask task;
	protected int type;
	
	public LabelMatcher(QueryTask task) {
		this.task = task;
		this.type = 1;
	}
	
	public LabelMatcher(QueryTask task, int type) {
		this.task = task;
		this.type = type;
	}
	
	public LabelMatcher(QueryTask task, int type, boolean bUseCache) {
		this.task = task;
		this.type = type;
		_bUseCache = bUseCache;
	}
	
	public void match(){
		match(task.srcIndiLabelList);
	}
	
	public void match(HashSet<String> srcs) {		
		Iterator<IndividualParameter> itSrc = task.srcIndiLabelList.iterator();
		ArrayList<IndividualParameter> srcList = new ArrayList<IndividualParameter>();
		while (itSrc.hasNext()) {
			IndividualParameter srcPair = itSrc.next();
			if (! srcs.contains(srcPair.get_strURI())){
				continue;
			}
			srcList.add(srcPair);
		}
		match(srcList);
	}
	
	public void match(ArrayList<IndividualParameter> srcIndiList) {	
		Future<Match> future = null;
		MatchModel mm = new MatchModel();
		Queue<Future> futureQueue = new LinkedList<Future>();
		Iterator<IndividualParameter> itSrc = srcIndiList.iterator();
		//System.out.println("begin to add to thread pools!!!");
		while (itSrc.hasNext()) {
			future = submitToControllor(
					new LabelMatcherProcessUnit(this,itSrc.next(),task.tarIndiLabelList));
			futureQueue.offer(future);
		}
		//System.out.println("end to add to thread pools!!!");
		
		while(futureQueue.size() > 0){
			future = futureQueue.poll();
			Match match = null;
			try {
				match = future.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			//System.out.println("" + iTotalNum + ":" + (++iCounter) + "->" + match.getSrcURI());
			if (match != null){
				mm.addMatch(match);				
			}
		}
		
		task.setResult(mm);
	}
	
	protected synchronized Future<Match> submitToControllor(
			LabelMatcherProcessUnit labelMatcherProcessUnit) {
		return Invoker.__controller.submit(labelMatcherProcessUnit);
	}
}
	
