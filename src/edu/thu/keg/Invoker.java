package edu.thu.keg;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.thu.keg.util.ExpData;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.thu.keg.control.LabelMatcherProcessUnit;
import edu.thu.keg.model.GlobalMatchModel;
import edu.thu.keg.model.Match;
import edu.thu.keg.process.GlobalTask;
import edu.thu.keg.process.QueryPostprocessor;
import edu.thu.keg.process.QueryTask;
import edu.thu.keg.util.StringPair;
import edu.thu.keg.util.Util;

public class Invoker {
	public static String BASE = "input/";
	public static String DBLP = "dblp";
	public static String EPRINTS = "eprints";
	public static String REXA = "rexa";
	public static int QueryNum = 404;
	
	public static String OutputFile1 = "temp/result_phase1.txt";
	public static String OutputFile2 = "temp/result_phase2.txt";
	public static String OutputFile3 = "temp/result_phase3.txt";
	public static String TIME_EPRINTS_REXA = "temp/times_eprints_rexa.txt";
	public static String TIME_EPRINTS_DBLP = "temp/times_eprints_dblp.txt";
	public static String TIME_REXA_DBLP = "temp/times_rexa_dblp.txt";
	public static String RESULTPATH_EPRINTS_REXA = "temp/eprints_rexa/";
	public static String RESULTPATH_EPRINTS_DBLP = "temp/eprints_dblp/";
	public static String RESULTPATH_REXA_DBLP = "temp/rexa_dblp/";
	
	public static String ORIGIN_PATH = "ontology/";
	public static String EPRINTS_ORIGIN = ORIGIN_PATH +  "eprints.rdf"; 
	public static String REXA_ORIGIN = ORIGIN_PATH +  "rexa.rdf";
	public static String DBLP_ORIGIN = ORIGIN_PATH +  "swetodblp_april_2008.rdf";
	public static String RESULT_EPRINTS_REXA = "temp/globalResult_eprints_rexa.dat";
	public static String RESULT_EPRINTS_DBLP = "temp/globalResult_eprints_dblp.dat";
	public static String RESULT_REXA_DBLP = "temp/globalResult_rexa_dblp.dat";
	
	public static double MATCHING_THRESHOLD = 0.6;
	public static double GLOBAL_THRESHOLD = 0.89;
	public static double GLOBAL_MATCHING_THRESHOLD = 0.1;
	
	private static Logger __logger = LoggerFactory.getLogger(Invoker.class);
	
	public static ExecutorService __controller = null;
	private static boolean __bControllerIsRunning = false;
	
	public static boolean __bUserFeedback = false;
	public static Map<StringPair,Boolean> __feedbackedResults = null;
	public static boolean __bNeedQueryMatching = true;
	public static Set<StringPair> __globalRefResults = null;
	public static int __exactRecommend = 0;
	public static int __errorRecommend = 0;
	
	public static Map<String,String> __srcPagePropMap = null;
	public static Map<String,String> __tarPagePropMap = null;
	
	public static boolean __bUseNecessaryProperty = false;
	public static boolean __bUseSFMatcher = false;
	
	public static int NUM = 0;
	public static int THREADPOOLLEN = 1;

	public static void initThreshold(double threshold){
		MATCHING_THRESHOLD = threshold;
		GLOBAL_MATCHING_THRESHOLD = threshold;
		GLOBAL_THRESHOLD = threshold;

	}
	public static void main(String args[]) throws Exception {
		List<Double> th1 = new ArrayList<>(),th2 = new ArrayList<>();
		int num = 1;//
		for(int i =0;i<=num;i++) {
			th1.add(Double.valueOf(String.format("%.2f",i * 1.0/num)));
		}
		ExpData data = new ExpData();
		for (Double i:th1){
			System.out.println("************begin doing exp... threshold: "+ i +" *************");
			initThreshold(i);
			excuteQueryDrivenExp(data);
		}
		data.export("temp/final_result.txt");
	}

	public static void excuteQueryDrivenExp(ExpData data) throws Exception{
		THREADPOOLLEN = Runtime.getRuntime().availableProcessors();
		__controller = Executors.newFixedThreadPool(THREADPOOLLEN);
		System.err.println("processor..."+THREADPOOLLEN);
		__bControllerIsRunning = true;

		int totalNum = 404;
		String tempres = null;
		GlobalTask task = new GlobalTask();


//		String strSrcPath = BASE + EPRINTS;//BASE = input
//		String strTarPath = BASE + REXA;
//		String strRefFile = "ref/eprints_rexa_goldstandard.xml";
//		String strResultPath = RESULTPATH_EPRINTS_REXA;
//		String strTimeFile = TIME_EPRINTS_REXA;
//		String strGlobalSource = EPRINTS_ORIGIN;
//		String strGlobalTarget = REXA_ORIGIN;
//		String strGlobalResults = RESULT_EPRINTS_REXA;
//		String strSrcPagePropFile = "temp/eprints_pageproperty.txt";
//		String strTarPagePropFile = "temp/rexa_pageproperty.txt";


		String strSrcPath = BASE + EPRINTS;
		String strTarPath = BASE + DBLP;
		String strRefFile = "ref/eprints_dblp_goldstandard.xml";
		String strResultPath = RESULTPATH_EPRINTS_DBLP;
		String strTimeFile = TIME_EPRINTS_DBLP;
		String strGlobalSource = EPRINTS_ORIGIN;
		String strGlobalTarget = DBLP_ORIGIN;
		String strGlobalResults = RESULT_EPRINTS_DBLP;
		String strSrcPagePropFile = "temp/eprints_pageproperty.txt";
		String strTarPagePropFile = "temp/dblp_pageproperty.txt";


//		String strSrcPath = BASE + REXA;
//		String strTarPath = BASE + DBLP;
//		String strRefFile = "ref/rexa_dblp_goldstandard.xml";
//		String strResultPath = RESULTPATH_REXA_DBLP;
//		String strTimeFile = TIME_REXA_DBLP;
//		String strGlobalSource = REXA_ORIGIN;
//		String strGlobalTarget = DBLP_ORIGIN;
//		String strGlobalResults = RESULT_REXA_DBLP;
//		String strSrcPagePropFile = "temp/rexa_pageproperty.txt";
//		String strTarPagePropFile = "temp/dblp_pageproperty.txt";

		__bNeedQueryMatching = true;
		boolean bNeedGlobalMatching = true;
		__bUserFeedback = false;
		__bUseSFMatcher = false;
		__bUseNecessaryProperty = false;
		if(__bUseSFMatcher){//SF�ή�����ƶ�
//			MATCHING_THRESHOLD = 0.6;
			GLOBAL_THRESHOLD = 0.1;
		}
		if(__bUserFeedback){
			initFeedbackedResults(strRefFile);
		}

		if(__bUseNecessaryProperty){
			__srcPagePropMap = initPagePropMap(strSrcPagePropFile);
			__tarPagePropMap = initPagePropMap(strTarPagePropFile);
		}


		long lTotalTime = 0;
		double dThreshold = MATCHING_THRESHOLD;
		__logger.info("task = " + strRefFile);
		__logger.info("threshold1 = " + MATCHING_THRESHOLD);
		__logger.info("threshold2 = " + GLOBAL_THRESHOLD);
		__logger.info("threshold3 = " + GLOBAL_MATCHING_THRESHOLD);
		__logger.info("Use user feedback = " + __bUserFeedback);
		__logger.info("Use necessary property = " + __bUseNecessaryProperty);
		__logger.info("Use need query matching = " + __bNeedQueryMatching);
		__logger.info("Use similarity flooding = " + __bUseSFMatcher);

		ArrayList<Integer> numList = new ArrayList<Integer>();
		try {
			DataInputStream file = new DataInputStream(new FileInputStream("NumList.dat"));
			int Num = file.readInt();
			for(int i = 0;i < Num; ++i){
				numList.add(file.readInt());
			}
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		FileWriter out = null;
		if(__bNeedQueryMatching){
			out = new FileWriter(strTimeFile);
		}
		int iBegin1 = 0;
		int iBegin2 = totalNum / 3;
		int iBegin3 = 2 * totalNum / 3;
		for (int i = iBegin1; i < iBegin2; i++) {
			NUM = numList.get(i);
			QueryTask qTask = new QueryTask();
			qTask.setSrcFile(strSrcPath  + NUM + ".rdf");
			qTask.setTarFile(strTarPath  + NUM + ".rdf");
			if(__bNeedQueryMatching){
				qTask.match(QueryPostprocessor.TYPE_TF, dThreshold);
				Util.__exportMMToFile(qTask.getResult(), strResultPath + NUM + ".dat");
				Util.__exportMMToFile_Txt(qTask.getResult(), strResultPath + NUM + ".txt");
				out.write("" + NUM + "\t" + qTask.get_lRunningTime()+ "\n");
				lTotalTime += qTask.get_lRunningTime();
			}else{
				qTask.initModel();
				qTask.setResult(Util.__importMMFromFile_Txt(strResultPath + NUM + ".txt", dThreshold));
			}
			//System.err.println("Query Task Result Size: " + qTask.getResult().size());
			((GlobalMatchModel)task.getResult()).addQuery(qTask, true);
			//System.err.println("Phase 1 Iteration " + i + " finished: " + "MatchModel Size " + task.getResult().size());
			if(qTask.getSrcModel() != null){
				qTask.getSrcModel().close();
				qTask.setSrcModel(null);
			}
			if(qTask.getTarModel() != null){
				qTask.getTarModel().close();
				qTask.setTarModel(null);
			}

		}

//		task.postprocess(QueryPostprocessor.TYPE_TF, GLOBAL_THRESHOLD);
		tempres = task.evaluate(strRefFile);
		data.addData("P1"+"\t"+MATCHING_THRESHOLD+"\t"+tempres);
		System.out.println(tempres);
		task.getResult().output(OutputFile1);
		System.out.println("Phase 1 GlobalMatchModel Size: " + task.getResult().size());
		System.out.println("**************");
		//System.exit(-1);

		for (int i = iBegin2; i < iBegin3; i++) {
			NUM = numList.get(i);//Util.randomNum(404);
			//System.err.println("Task No. " + num);
			QueryTask qTask = new QueryTask();
			qTask.setSrcFile(strSrcPath  + NUM + ".rdf");
			qTask.setTarFile(strTarPath  + NUM + ".rdf");
			if(__bNeedQueryMatching){
				qTask.match(QueryPostprocessor.TYPE_TF, dThreshold);
				Util.__exportMMToFile(qTask.getResult(), strResultPath + NUM + ".dat");
				Util.__exportMMToFile_Txt(qTask.getResult(), strResultPath + NUM + ".txt");
				out.write("" + NUM + "\t" + qTask.get_lRunningTime()+ "\n");
				lTotalTime += qTask.get_lRunningTime();
			}else{
				qTask.initModel();
				qTask.setResult(Util.__importMMFromFile_Txt(strResultPath + NUM + ".txt", dThreshold));
			}
			//System.err.println("Query Task Result Size: " + qTask.getResult().size());
			((GlobalMatchModel)task.getResult()).addQuery(qTask, false);
			//System.err.println("Phase 2 Iteration " + i + " finished: " + "MatchModel Size " + task.getResult().size());
			if(qTask.getSrcModel() != null){
				qTask.getSrcModel().close();
				qTask.setSrcModel(null);
			}
			if(qTask.getTarModel() != null){
				qTask.getTarModel().close();
				qTask.setTarModel(null);
			}
		}
//		task.postprocess(QueryPostprocessor.TYPE_TF, GLOBAL_THRESHOLD);
		tempres = task.evaluate(strRefFile);
		data.addData("P2" + "\t" + MATCHING_THRESHOLD + "\t" + tempres);
		task.getResult().output(OutputFile2);
		System.out.println("Phase 2 GlobalMatchModel Size: " + task.getResult().size());
		System.out.println("**************");

		for (int i = iBegin3; i < totalNum ; i++) {
			NUM = numList.get(i);//Util.randomNum(404);
			//System.out.println("Task No. " + num);
			QueryTask qTask = new QueryTask();
			qTask.setSrcFile(strSrcPath  + NUM + ".rdf");
			qTask.setTarFile(strTarPath  + NUM + ".rdf");
			if(__bNeedQueryMatching){
				qTask.match(QueryPostprocessor.TYPE_TF, dThreshold);
				Util.__exportMMToFile(qTask.getResult(), strResultPath + NUM + ".dat");
				Util.__exportMMToFile_Txt(qTask.getResult(), strResultPath + NUM + ".txt");
				out.write("" + NUM + "\t" + qTask.get_lRunningTime()+ "\n");
				lTotalTime += qTask.get_lRunningTime();
			}else{
				qTask.initModel();
				qTask.setResult(Util.__importMMFromFile_Txt(strResultPath + NUM + ".txt", dThreshold));
			}
			//System.out.println("Query Task Result Size: " + qTask.getResult().size());
			((GlobalMatchModel)task.getResult()).addQuery(qTask, false);
			//System.out.println("Phase 3 Iteration " + i + " finished: " + "MatchModel Size " + task.getResult().size());
			if(qTask.getSrcModel() != null){
				qTask.getSrcModel().close();
				qTask.setSrcModel(null);
			}
			if(qTask.getTarModel() != null){
				qTask.getTarModel().close();
				qTask.setTarModel(null);
			}
		}

		//task.postprocess(QueryPostprocessor.TYPE_TF_OOF, GLOBAL_THRESHOLD);
		tempres = task.evaluate(strRefFile);
		data.addData("P3"+"\t"+MATCHING_THRESHOLD+"\t"+tempres);
		task.getResult().output(OutputFile3);
		System.out.println("Phase 3 GlobalMatchModel Size: " + task.getResult().size());
		System.out.println("**************");
		System.out.println("Total time:" + lTotalTime + "\n");
		if(out != null)
			out.write("total time:\t" + lTotalTime+ "\n");

		if(Invoker.__bUserFeedback && __bNeedQueryMatching){
			__logger.info("" + (double)Invoker.__exactRecommend/(__exactRecommend + __errorRecommend));
			save_result("temp/rexa_dblp_f_m.txt");
		}
		System.out.println("@@@@@@@@@@@@@@");
		if(bNeedGlobalMatching){
			LabelMatcherProcessUnit.SCORE_THRESHOLD = GLOBAL_MATCHING_THRESHOLD;
			tempres = Util.globalEvaluate(strGlobalSource, strGlobalTarget,strGlobalResults,strRefFile, QueryPostprocessor.TYPE_TF, GLOBAL_MATCHING_THRESHOLD, task);
			data.addData("Global"+"\t"+MATCHING_THRESHOLD+"\t"+tempres);
//			if(out != null)
//				out.write("global time:\t" + lGlobalTime+ "\n");
		}else{
			tempres = Util.globalEvaluate(strGlobalResults,strRefFile, QueryPostprocessor.TYPE_TF, dThreshold, task);
			data.addData("Global"+"\t"+MATCHING_THRESHOLD+"\t"+tempres);
		}
		System.out.println("@@@@@@@@@@@@@@");
		if(__bControllerIsRunning){
			__bControllerIsRunning = false;
			__controller.shutdown();
		}

		if(out != null)
			out.close();
	}

	private static Map<String, String> initPagePropMap(String strSrcPagePropFile) throws IOException {
		Map<String, String> pagePropMap = new HashMap<String,String>();

		BufferedReader reader = new BufferedReader(new FileReader(strSrcPagePropFile));
		String strLine = null;
		while((strLine = reader.readLine()) != null){
			String[] strings = strLine.split("\t");
			pagePropMap.put(strings[0], strings[1]);
		}

		return pagePropMap;
	}

	private static void save_result(String string) throws IOException {
		FileWriter writer = new FileWriter(string);
		Iterator<Entry<StringPair,Boolean>> itPair = __feedbackedResults.entrySet().iterator();
		while(itPair.hasNext()){
			Entry<StringPair,Boolean> pair = itPair.next();
			if(pair.getValue())
				writer.write(pair.getKey().getStr_P1() + "\t" +pair.getKey().getStr_P2() + "\n");
		}
		writer.close();
	}

	private static void initFeedbackedResults(String strRefFile) {
		__feedbackedResults = new HashMap<StringPair,Boolean>();
		__globalRefResults = new HashSet<StringPair>();
		
		try {
			File file = new File(strRefFile);
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
				if(entity1 != null && entity2 != null){
					String srcURI = entity1.attributeValue("resource");
					String tarURI = entity2.attributeValue("resource");
					__globalRefResults.add(new StringPair(srcURI,tarURI));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
