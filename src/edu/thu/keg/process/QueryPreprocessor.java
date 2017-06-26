package edu.thu.keg.process;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.thu.keg.model.IndividualParameter;
import edu.thu.keg.util.AnyPair;
import edu.thu.keg.util.Util;

public class QueryPreprocessor {
	QueryTask task;
	
	public QueryPreprocessor(QueryTask task) {
		this.task = task;
	}
	
	public void preprocess() {
		loadInputFile(task.getSrcModel(), task.getSrcFile());
		loadInputFile(task.getTarModel(), task.getTarFile());
		
		if(task.srcIndiLabelList == null)
			task.srcIndiLabelList = new ArrayList<IndividualParameter>();
		else
			task.srcIndiLabelList.clear();		
		initIndiLabelList(task.getSrcModel(),task.srcIndiLabelList);
		
		if(task.tarIndiLabelList == null)
			task.tarIndiLabelList = new ArrayList<IndividualParameter>();
		else
			task.tarIndiLabelList.clear();
		initIndiLabelList(task.getTarModel(),task.tarIndiLabelList);
	}
	
	public void preprocess(HashSet<String> srcs) {
		loadInputFile(task.getSrcModel(), task.getSrcFile());
		loadInputFile(task.getTarModel(), task.getTarFile());
		
		if(task.srcIndiLabelList == null)
			task.srcIndiLabelList = new ArrayList<IndividualParameter>();
		else
			task.srcIndiLabelList.clear();		
		initIndiLabelList(task.getSrcModel(),task.srcIndiLabelList,srcs);
		
		if(task.tarIndiLabelList == null)
			task.tarIndiLabelList = new ArrayList<IndividualParameter>();
		else
			task.tarIndiLabelList.clear();
		initIndiLabelList(task.getTarModel(),task.tarIndiLabelList);
	}
	
	private void initIndiLabelList(OntModel model,
			ArrayList<IndividualParameter> srcIndiLabelList,
			HashSet<String> srcs) {
		ResIterator srit = model.listSubjects();
		while (srit.hasNext()) {
			Resource res = srit.nextResource();
			if (Util.testIndi(res)) {
				if(!srcs.contains(res.getURI()))
					continue;
				Individual indi = (Individual)res.as(Individual.class);
				IndividualParameter para = Util.getIndiPara(indi);
				srcIndiLabelList.add(para);
			}
		}
		
	}

	private void initIndiLabelList(OntModel model,
			ArrayList<IndividualParameter> indiLabelList) {
		ResIterator srit = model.listSubjects();
		while (srit.hasNext()) {
			Resource res = srit.nextResource();
			if (Util.testIndi(res)) {
				Individual indi = (Individual)res.as(Individual.class);
				IndividualParameter para = Util.getIndiPara(indi);
				indiLabelList.add(para);
			}
		}
	}

	public void loadInputFile(OntModel model, String fileName) {
		try {
			FileInputStream fis = new FileInputStream(fileName);
			model.read(fis, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
