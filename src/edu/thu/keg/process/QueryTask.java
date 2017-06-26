package edu.thu.keg.process;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.thu.keg.Invoker;
import edu.thu.keg.model.IndividualParameter;
import edu.thu.keg.model.MatchModel;
import edu.thu.keg.util.AnyPair;

public class QueryTask {
	
	String srcFile;
	String tarFile;
	
	OntModel srcModel;
	OntModel tarModel;
	
	ArrayList<IndividualParameter> srcIndiLabelList = null; 
	ArrayList<IndividualParameter> tarIndiLabelList = null; 
	
	private long _lRunningTime = 0;
	
	MatchModel result;
	
	public QueryTask() {
		this.srcModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		this.tarModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		this.result = new MatchModel();
	}
	
	public void initModel(){
		try {
			FileInputStream fis = new FileInputStream(srcFile);
			srcModel.read(fis, "");
			fis = new FileInputStream(tarFile);
			tarModel.read(fis, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void preprocess() {
		(new QueryPreprocessor(this)).preprocess();
	}
	
	private void preprocess(HashSet<String> srcs) {
		(new QueryPreprocessor(this)).preprocess(srcs);
	}
	
	public void calcSim() {
		//(new LabelMatcherWithIndex(this, LabelMatcher.TYPE_VD)).match();
		(new LabelMatcher(this, LabelMatcher.TYPE_VD,true)).match();
		if(Invoker.__bUseSFMatcher){
			new SFMatcher(this).flooding();
		}
	}
	
	public void calcSim(HashSet<String> srcs) {
		//(new LabelMatcherWithIndex(this, LabelMatcher.TYPE_VD)).match();
		if(srcFile.toLowerCase().indexOf("rexa") >= 0){
			System.out.println("Source is rexa!!!");
			(new LabelMatcher(this, LabelMatcher.TYPE_VD)).match();
		}else{
			(new LabelMatcherWithIndex(this, LabelMatcher.TYPE_VD)).match();
		}
		//(new LabelMatcher(this, LabelMatcher.TYPE_VD)).match(srcs);
		//(new SFMatcher(this)).c(3);
	}
	
	public void postprocess(int type, double threshold) {
		(new QueryPostprocessor(this, type, threshold)).postprocess();
	}
	
	public void match(int type, double threshold) {
		preprocess();
		//System.out.println(this.getSrcModel().size());
		//System.out.println(this.getTarModel().size());
		long lBeginTime = System.currentTimeMillis();
		calcSim();
		_lRunningTime = System.currentTimeMillis() - lBeginTime;
		postprocess(type, threshold);
	}
	
	public void match(int type, double threshold, HashSet<String> srcs) {
		preprocess(srcs);
		System.out.println(this.getSrcModel().size());
		System.out.println(this.getTarModel().size());
		if(this.srcModel != null){
			srcModel.close();
			srcModel = null;
		}
		if(this.tarModel != null){
			tarModel.close();
			tarModel = null;
		}
		
		long lBeginTime = System.currentTimeMillis();
		calcSim(srcs);
		_lRunningTime = System.currentTimeMillis() - lBeginTime;
		System.out.println("Post Process Begins");
		
		postprocess(type, threshold);
	}

	public String getSrcFile() {
		return srcFile;
	}

	public void setSrcFile(String srcFile) {
		this.srcFile = srcFile;
	}

	public String getTarFile() {
		return tarFile;
	}

	public void setTarFile(String tarFile) {
		this.tarFile = tarFile;
	}

	public OntModel getSrcModel() {
		return srcModel;
	}

	public void setSrcModel(OntModel srcModel) {
		this.srcModel = srcModel;
	}

	public OntModel getTarModel() {
		return tarModel;
	}

	public void setTarModel(OntModel tarModel) {
		this.tarModel = tarModel;
	}

	public MatchModel getResult() {
		return result;
	}

	public void setResult(MatchModel result) {
		this.result = result;
	}

	public long get_lRunningTime() {
		return _lRunningTime;
	}
}
