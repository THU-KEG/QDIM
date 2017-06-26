package edu.thu.keg.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.hp.hpl.jena.ontology.Individual;

import edu.thu.keg.Invoker;
import edu.thu.keg.control.LabelMatcherProcessUnit;
import edu.thu.keg.model.IndividualParameter;
import edu.thu.keg.model.Match;
import edu.thu.keg.model.MatchModel;
import edu.thu.keg.util.AnyPair;
import edu.thu.keg.util.NaiveNameTokenizer;
import edu.thu.keg.util.StringPair;

public class LabelMatcherWithIndex extends LabelMatcher {

	static class Item{
		ArrayList<IndividualParameter>  _srcIndiList = null;
		ArrayList<IndividualParameter>  _tarIndiList = null;
		
		Item(){
			
		}
		
		void addToSrcList(IndividualParameter pair){
			if(_srcIndiList == null)
				_srcIndiList = new ArrayList<IndividualParameter>();
			_srcIndiList.add(pair);
		}
		
		void addToTarList(IndividualParameter pair){
			if(_tarIndiList == null)
				_tarIndiList = new ArrayList<IndividualParameter>();
			_tarIndiList.add(pair);
		}
	}
	
	public LabelMatcherWithIndex(QueryTask task) {
		super(task);
	}
	
	public LabelMatcherWithIndex(QueryTask task, int type) {
		super(task,type);
	}
	
	public LabelMatcherWithIndex(QueryTask task, int type, boolean bUseCache) {
		super(task,type,bUseCache);
	}
	
	public void match(ArrayList<IndividualParameter> srcIndiList) {		
		HashMap<String, Item> wordIndiMap = new HashMap<String,Item>();	
		
		Set<StringPair> processedPairs = new HashSet<StringPair>();
		
		int iSrcSize = srcIndiList.size();
		for(int i = 0; i < iSrcSize ; ++i){
			IndividualParameter curPair = srcIndiList.get(i);
			//List<String> words = tokenizer.tokenize(curPair.get_strLabel());
			Iterator<String> it = curPair.get_tokenedLabelList().iterator();
			while(it.hasNext()){
				String word = it.next().toLowerCase();
				Item item = wordIndiMap.get(word);
				if(item == null){
					item = new Item();
					item.addToSrcList(curPair);
					wordIndiMap.put(word, item);
				}else{
					item.addToSrcList(curPair);
				}
			}
		}
		
		int iTarSize = task.tarIndiLabelList.size();
		for(int i = 0; i < iTarSize ; ++i){
			IndividualParameter curPair = task.tarIndiLabelList.get(i);
			//List<String> words = tokenizer.tokenize(curPair.get_strLabel());
			Iterator<String> it = curPair.get_tokenedLabelList().iterator();
			while(it.hasNext()){
				String word = it.next().toLowerCase();
				Item item = wordIndiMap.get(word);
				if(item != null){
					item.addToTarList(curPair);
				}
			}
		}
		
		Future<Match> future = null;
		MatchModel mm = new MatchModel();
		int iTotalNum = 0;
		int iCounter = 0;
		
		Iterator<Item> itItems = wordIndiMap.values().iterator();
		HashMap<IndividualParameter,HashSet<IndividualParameter>> tasks = 
			new HashMap<IndividualParameter,HashSet<IndividualParameter>>();
		while(itItems.hasNext()){
			Item item = itItems.next();
			ArrayList<IndividualParameter> srcList = item._srcIndiList;
			ArrayList<IndividualParameter> tarList = item._tarIndiList;
			if(tarList != null){
				Iterator<IndividualParameter> itSrcIndis = srcList.iterator();
				while(itSrcIndis.hasNext()){
					IndividualParameter curSrcIndi = itSrcIndis.next();
					HashSet<IndividualParameter> curTarList = tasks.get(curSrcIndi);
					if(curTarList == null){
						curTarList = new HashSet<IndividualParameter>();
						tasks.put(curSrcIndi, curTarList);
					}
					Iterator<IndividualParameter> itTarIndis = tarList.iterator();
					while(itTarIndis.hasNext()){
						IndividualParameter curTarIndi = itTarIndis.next();
						if(curSrcIndi.get_strType().equals(curTarIndi.get_strType())){
							if(processedPairs.add(new StringPair(curSrcIndi.get_strURI(),curTarIndi.get_strURI()))){
								curTarList.add(curTarIndi);
							}
						}
					}
				}
			}
		}
		
		wordIndiMap = null;	
		processedPairs = null;
		
		Queue<Future<Match>> futureQueue = new LinkedList<Future<Match>>();
		Iterator<Entry<IndividualParameter,HashSet<IndividualParameter>>> itTasks = 
			tasks.entrySet().iterator();
		while(itTasks.hasNext()){
			Entry<IndividualParameter,HashSet<IndividualParameter>> curEntry = itTasks.next();
			future = submitToControllor(
					new LabelMatcherProcessUnit((LabelMatcher)this, curEntry.getKey(),curEntry.getValue()));
			futureQueue.offer(future);
			++iTotalNum;
		}
		tasks = null;
		
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
			System.out.println("" + iTotalNum + ":" + (++iCounter) + "->" + match.getSrcURI());
			if (match != null){
				mm.addMatch(match);				
			}
		}
		
		task.setResult(mm);
	}
	
	public static void main(String[] args){
		HashSet<IndividualParameter> set = new HashSet<IndividualParameter>();
		IndividualParameter para1 = new IndividualParameter("aa","121","dkjf");
		set.add(para1);
		set.add( new IndividualParameter("aa1","121d","dkdsjf"));
		Iterator<IndividualParameter> it = set.iterator();
		while(it.hasNext()){
			IndividualParameter p = it.next();
			System.out.println(p.get_strURI() + p.get_strLabel() + p.get_strType());
		}
		HashMap<IndividualParameter,Double> map = new HashMap<IndividualParameter,Double>();
		map.put(para1, new Double(100));
		System.out.println(map.get(new IndividualParameter("aa","121d","dkdsjf")));
		System.out.println(set.size());
		
	}
}
