package edu.thu.keg.process;

import java.io.File;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.keg.Invoker;
import edu.thu.keg.model.GlobalMatchModel;

public class GlobalTask extends QueryTask {
	
	int queryTime;
	Hashtable<String, Integer> srcCount;
	Hashtable<String, Integer> tarCount;
	
	static Logger __logger = LoggerFactory.getLogger(GlobalTask.class);
	public GlobalTask() {
		super();
		this.result = new GlobalMatchModel();
		this.queryTime = ((GlobalMatchModel)this.result).getQueryTime();
		this.srcCount = ((GlobalMatchModel)this.result).getSrcCount();
		this.tarCount = ((GlobalMatchModel)this.result).getTarCount();
	}
	
	public Hashtable<String, Integer> getSrcCount() {
		return this.srcCount;
	}
	
	public String evaluate(String EvalFileName) {
		HashSet<String> result = this.result.renderToStringSet(Invoker.GLOBAL_THRESHOLD);
		HashSet<String> reference = genReferenceSet(EvalFileName);
		return evaluate(reference, result);
	}
	
	public HashSet<String> genReferenceSet(String EvalFileName) {
		HashSet<String> reference = new HashSet<String>();
		try {
			File file = new File(EvalFileName);
			SAXReader sr = new SAXReader();
			Document doc = sr.read(file);
			Element root = doc.getRootElement();
			Element alignment = root.element("Alignment");
			Iterator<Element> mapIt = alignment.elementIterator("map");
			while (mapIt.hasNext()) {
				Element map = mapIt.next();
				Element cell = map.element("Cell");
				Element entity1 = cell.element("entity1");
				Element entity2 = cell.element("entity2");
				String srcURI = entity1.attributeValue("resource");
				String tarURI = entity2.attributeValue("resource");
				//System.out.println(srcURI + "\t" + tarURI);
				// if (srcCount.containsKey(srcURI) && tarCount.containsKey(tarURI))
				//	reference.add(srcURI + "\t" + tarURI);
				if (srcCount.containsKey(srcURI))
					reference.add(srcURI + "\t" + tarURI);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reference;
	}
	
	public String evaluate(HashSet<String> reference, HashSet<String> result) {//src \t tar ���ַ���ƥ��
		int refSize = reference.size();
		int resSize = result.size();
		int common = 0;
		
		for (String res : result) {
			if (reference.contains(res))
				common++;
		}
		
		double precision = common * 1.0 / resSize;
		double recall = common * 1.0 / refSize;
		double fMeasure = 2 * precision * recall / (precision + recall);
	
		__logger.info("Matching Result:");
		__logger.info("Reference Size = " + refSize + "\t" + "Result size = " + resSize);
		__logger.info("Precision = " + precision);
		__logger.info("recall = " + recall);
		__logger.info("F1-Measure = " + fMeasure);
		__logger.info("******************************");
		
		refSize = 0;
		resSize = 0;
		common = 0;
		for(String refs : reference){
			if(refs.indexOf("person") < 0){
				++refSize;
			}
		}
		
		for (String res : result) {
			if(res.indexOf("person") < 0){
				++resSize;
				if (reference.contains(res))
					common++;
			}
		}
		
		precision = common * 1.0 / resSize;
		recall = common * 1.0 / refSize;
		fMeasure = 2 * precision * recall / (precision + recall);
	
		__logger.info("Matching Result:");
		__logger.info("Reference Size = " + refSize + "\t" + "Result size = " + resSize);
		__logger.info("publication Precision = " + precision);
		__logger.info("publication recall = " + recall);
		__logger.info("publication F1-Measure = " + fMeasure);
		__logger.info("******************************");

		return String.format("%.4f\t%.4f\t%.4f",precision,recall,fMeasure);
	}
}
