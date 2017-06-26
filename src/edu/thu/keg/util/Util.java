package edu.thu.keg.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Random;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.thu.keg.Invoker;
import edu.thu.keg.model.IndividualParameter;
import edu.thu.keg.model.Match;
import edu.thu.keg.model.MatchModel;
import edu.thu.keg.model.MatchScore;
import edu.thu.keg.process.GlobalTask;
import edu.thu.keg.process.QueryTask;

public class Util {
	public static String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	public static String FOAF = "http://xmlns.com/foaf/0.1/";
	public static String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	
	public static int randomNum(int seed) {
		Random r = new Random(); 
		return r.nextInt(seed) + 1;
	}
	
	public static IndividualParameter getIndiPara(Individual indi) {
		IndividualParameter para = null;
		StmtIterator it = indi.listProperties();
		String strPage = null;
		while (it.hasNext()) {
			Statement st = it.nextStatement();
			if (st.getPredicate().toString().equals(RDFS + "label")) {
				String retStr = refineStr(st.getObject().toString()); 
				//System.out.println(retStr);
				para = new IndividualParameter(indi.getURI(),retStr,"other");
			}
			else if (st.getPredicate().toString().equals(FOAF + "name")) {
				String retStr = refineStr(st.getObject().toString());
				para = new IndividualParameter(indi.getURI(),retStr,"person");
			}else if(st.getPredicate().toString().equals("http://lsdis.cs.uga.edu/projects/semdis/opus#pages")){
				//System.out.println("zhongqian");
				if(Invoker.__bUseNecessaryProperty)
					strPage = refinePage(st.getObject().toString());
			}
			//System.out.println(st.getPredicate().toString());
		}
		if(Invoker.__bUseNecessaryProperty && strPage != null)
			para.set_strPage(strPage);
		//System.out.println("No Legal Name");
		return para;
	}
	
	public static String refinePage(String string) {
		int ibegin = 0;
		int iend = 0;
		int len = string.length();
		while(ibegin < len){
			if(string.charAt(ibegin) > '9' || string.charAt(ibegin) < '1')
				ibegin++;
			else
				break;
		}
		if(ibegin < len){
			iend = ibegin + 1;
			while(iend < len){
				if(string.charAt(iend) > '9' || string.charAt(iend) < '1')
					break;
				++iend;
			}
			return string.substring(ibegin,iend);
		}
		return null;
	}
	
	public static String refineStr(String str) {
		str = str.toLowerCase();
		if (str.indexOf("^^") < 0 && str.indexOf("@") < 0)
			return str;
		else if (str.indexOf("@") >= 0)
			return str.substring(0, str.indexOf("@"));
		else
			return str.substring(0, str.indexOf("^^"));
	}
	
	public static boolean testIndi(Resource r) {
		if (r == null)
			return false;
		if (r.canAs(Individual.class)) {
			if (r.canAs(OntClass.class))
				return false;
			else if (r.asNode().isBlank())
				return false;
			else return true;
		}
		return false;
	}
	
	public static String globalEvaluate(String srcFileName, String tarFileName, String strResultFile, String EvalFileName,
			int type, double threshold, GlobalTask gTask) throws IOException {
		QueryTask task = new QueryTask();
		task.setSrcFile(srcFileName);
		task.setTarFile(tarFileName);
		
		HashSet<String> srcs = new HashSet<String>();
		for (String srcURI : gTask.getSrcCount().keySet())
			srcs.add(srcURI);
		System.out.println("Source URI Set Size: " + srcs.size());
		
		task.match(type, threshold, srcs);
		HashSet<String> results = new HashSet<String>(); 
		for (Match m : task.getResult().getMatches().values()) {
			if (gTask.getSrcCount().containsKey(m.getSrcURI())) {
				for (MatchScore ms : m.getMsList().values()) {
					results.add(m.getSrcURI() + "\t" + ms.getTarURI());
				}
			}
		}
		__exportMMToFile(task.getResult(), strResultFile);
		__exportMMToFile_Txt(task.getResult(),strResultFile + ".txt");
		
		HashSet<String> reference = gTask.genReferenceSet(EvalFileName);
		return gTask.evaluate(reference, results);
//		return task.get_lRunningTime();
	}
	
	public static String globalEvaluate(String resultFile, String EvalFileName,
			int type, double threshold, GlobalTask gTask) throws IOException {
		
		MatchModel mm = __importMMFromFile_Txt(resultFile + ".txt",threshold);
		
		HashSet<String> srcs = new HashSet<String>();
		for (String srcURI : gTask.getSrcCount().keySet())
			srcs.add(srcURI);
		System.out.println("Source URI Set Size: " + srcs.size());
		
		HashSet<String> results = new HashSet<String>(); 
		for (Match m : mm.getMatches().values()) {
			if (gTask.getSrcCount().containsKey(m.getSrcURI())) {
				for (MatchScore ms : m.getMsList().values()) {
					if (ms.getScore()>=threshold) {
						results.add(m.getSrcURI() + "\t" + ms.getTarURI());
					}
				}
			}
		}
		
		HashSet<String> reference = gTask.genReferenceSet(EvalFileName);
		return gTask.evaluate(reference, results);
	}
	
	public static void __exportMMToFile(MatchModel mm, String file) throws IOException{
		if(mm == null || file == null || file.isEmpty() ){
			System.err.println("parameter is invalid when export MM to file!!!");
			return;
		}
		//System.out.println("!!!!");
		File f = new File(file);
		if(!f.exists())f.createNewFile();
		DataOutputStream out = new DataOutputStream(new FileOutputStream(f));
		out.writeInt(mm.getMatches().values().size());
		
		for (Match m : mm.getMatches().values()) {
			out.writeUTF(m.getSrcURI());
			out.writeInt(m.getMsList().values().size());
			for (MatchScore ms : m.getMsList().values()) {
				out.writeUTF(ms.getTarURI());
				out.writeDouble(ms.getScore());
			}
		}
		
		out.close();
	}
	
	public static MatchModel __importMMFromFile(String file,double dThreshold) throws IOException{
		if(file == null || file.isEmpty() ){
			System.err.println("parameter is invalid when export MM to file!!!");
			return null;
		}
		
		MatchModel mm = new MatchModel();
		DataInputStream in = new DataInputStream(new FileInputStream(file));
		int iMSize = in.readInt();
		System.out.println(iMSize);
		
		for(int i = 0;i < iMSize;++i){
			String strSrcURI = in.readUTF();
			//System.out.println(strSrcURI);
			int iMsSize = in.readInt();
			//System.out.println(iMsSize);
			Match m = null;
			for(int j = 0;j < iMsSize;++j){
				String strTarURI = in.readUTF();
				double dScore = in.readDouble();
				if(dScore > dThreshold){
					MatchScore ms = new MatchScore(strTarURI,dScore);
					if(m == null){
						m = new Match(strSrcURI);
					}
					m.addScore(ms);
				}
			}
			if(m != null)
				mm.addMatch(m);
		}
		
		in.close();
		return mm;
	}
	
	public static void __exportMMToFile_UTF(MatchModel mm, String file) throws IOException{
		if(mm == null || file == null || file.isEmpty() ){
			System.err.println("parameter is invalid when export MM to file!!!");
			return;
		}
		DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
		out.writeUTF("" + mm.getMatches().values().size());
		
		for (Match m : mm.getMatches().values()) {
			out.writeUTF(m.getSrcURI());
			out.writeUTF("" + m.getMsList().values().size());
			for (MatchScore ms : m.getMsList().values()) {
				out.writeUTF(ms.getTarURI());
				out.writeUTF("" + ms.getScore());
			}
		}
		
		out.close();
	}
	
	public static MatchModel __importMMFromFile_UTF(String file,double dThreshold) throws IOException{
		if(file == null || file.isEmpty() ){
			System.err.println("parameter is invalid when export MM to file!!!");
			return null;
		}
		
		MatchModel mm = new MatchModel();
		DataInputStream in = new DataInputStream(new FileInputStream(file));
		int iMSize = Integer.parseInt(in.readUTF());
		
		for(int i = 0;i < iMSize;++i){
			String strSrcURI = in.readUTF();
			int iMsSize = Integer.parseInt(in.readUTF());
			Match m = null;
			for(int j = 0;j < iMsSize;++j){
				String strTarURI = in.readUTF();
				double dScore = Double.parseDouble(in.readUTF());
				if(dScore > dThreshold){
					MatchScore ms = new MatchScore(strTarURI,dScore);
					if(m == null){
						m = new Match(strSrcURI);
					}
					m.addScore(ms);
				}
			}
			if(m != null)
				mm.addMatch(m);
		}
		
		in.close();
		return mm;
	}
	
	public static void __exportMMToFile_Txt(MatchModel mm, String file) throws IOException{
		if(mm == null || file == null || file.isEmpty() ){
			System.err.println("parameter is invalid when export MM to file!!!");
			return;
		}
		FileWriter out = new FileWriter(file);
		out.write("" + mm.getMatches().values().size() + "\n");
		
		for (Match m : mm.getMatches().values()) {
			out.write(m.getSrcURI() + "\n");
			out.write("" + m.getMsList().values().size() + "\n");
			for (MatchScore ms : m.getMsList().values()) {
				out.write(ms.getTarURI() + "\n");
				out.write("" + ms.getScore() + "\n");
			}
		}
		
		out.close();
	}
	
	public static MatchModel __importMMFromFile_Txt(String file,double dThreshold) throws IOException{
		if(file == null || file.isEmpty() ){
			System.err.println("parameter is invalid when export MM to file!!!");
			return null;
		}
		
		MatchModel mm = new MatchModel();
		BufferedReader in = new BufferedReader(new FileReader(file));
		int iMSize = Integer.parseInt(in.readLine());
		
		for(int i = 0;i < iMSize;++i){
			String strSrcURI = in.readLine();
			int iMsSize = Integer.parseInt(in.readLine());
			Match m = null;
			for(int j = 0;j < iMsSize;++j){
				String strTarURI = in.readLine();
				double dScore = Double.parseDouble(in.readLine());
				if(dScore > dThreshold){
					MatchScore ms = new MatchScore(strTarURI,dScore);
					if(m == null){
						m = new Match(strSrcURI);
					}
					m.addScore(ms);
				}
			}
			if(m != null)
				mm.addMatch(m);
		}
		
		in.close();
		return mm;
	}
}
