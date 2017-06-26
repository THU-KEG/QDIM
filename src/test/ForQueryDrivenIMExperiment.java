package test;

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
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.thu.keg.query.Querier;
import edu.thu.keg.query.impl.JenaQuerier;
import edu.thu.keg.util.StringPair;

public class ForQueryDrivenIMExperiment {

	private static String strBase = "";
	private static String strRefBase = strBase + "ref/";
	
	private static void constructCandidateQueryResult() throws IOException{
		BufferedReader queryFile = new BufferedReader(new FileReader("query.txt"));
		FileWriter[] infoFiles = new FileWriter[3];
		infoFiles[0] = new FileWriter("information-eprints.txt");
		infoFiles[1] = new FileWriter("information-rexa.txt");
		infoFiles[2] = new FileWriter("information-dblp.txt");
		
		String strQuery = null;
		Model[] models = new Model[3];
		String[] fileNames = {"eprints","rexa","dblp"};
		
		int iModelNum = 3;
		if(iModelNum >= 1){
			models[0] = initModel("eprints.rdf","UTF-8");
			if(iModelNum >= 2){
				models[1] = initModel("rexa.rdf","UTF-8");
				if(iModelNum >= 3)
					models[2] = initModel("swetodblp_april_2008.rdf","ISO-8859-1");
			}
		}
		
		int n = 0; long lTime;
		while((strQuery = queryFile.readLine()) != null){
			System.out.println("query:" + ++n);
			for(int i = 0;i < iModelNum; ++i){
				if(models[i] != null){
					JenaQuerier querier = new JenaQuerier(models[i]);
					lTime =System.currentTimeMillis();
					Model result = (Model)querier.Query(strQuery);
					lTime = System.currentTimeMillis() - lTime;
					String strNewResultName = fileNames[i] + n + ".n3";
					FileOutputStream queryResultFile = new FileOutputStream("query_result/" + strNewResultName);
					result.write(queryResultFile, "N3");
					queryResultFile.close();
					File newResultFile = new File("query_result/" + strNewResultName);
					infoFiles[i].write(strNewResultName + "\t");
					infoFiles[i].write("" + newResultFile.length() + "\t");
					infoFiles[i].write("" + lTime + "\t");
					infoFiles[i].write(strQuery + "\n");
				}
			}
		}
		
		if(iModelNum >= 1){
			models[0].close();
			if(iModelNum >= 2){
				models[1].close();
				if(iModelNum >= 3)
					models[2].close();
			}
		}
		queryFile.close();
		infoFiles[0].close();infoFiles[1].close();infoFiles[2].close();
	}
	
	private static void constructCandidateQuery() throws IOException{
		String[] queries = {
			//co-author	
			"PREFIX dc: <http://purl.org/dc/elements/1.1/> " + 
			"PREFIX opus: <http://lsdis.cs.uga.edu/projects/semdis/opus#> " + 
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
			"PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> " +
			"PREFIX apf: <http://jena.hpl.hp.com/ARQ/property#> " + 
			"CONSTRUCT {?document opus:author ?author1. ?document opus:author ?author2. ?document rdf:type ?type. ?document rdfs:label ?label." +
			" ?author1 rdf:type ?type1. ?author1 foaf:name ?name1. " + "?author2 rdf:type ?type2. ?author2 foaf:name ?name2.} " +
			" WHERE" +
			"{?document opus:author ?authors. ?document rdf:type ?type. ?document rdfs:label ?label." +
			" ?authors rdfs:member ?author1. ?author1 rdf:type ?type1. ?author1 foaf:name ?name1. " +
			" ?authors rdfs:member ?author2. ?author2 rdf:type ?type2. ?author2 foaf:name ?name2. " +
			" FILTER (XXXXXX ?author1 != ?author2).}",
			
			//author-document
			"PREFIX dc: <http://purl.org/dc/elements/1.1/> " + 
			"PREFIX opus: <http://lsdis.cs.uga.edu/projects/semdis/opus#> " + 
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
			"PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> " +
			"PREFIX apf: <http://jena.hpl.hp.com/ARQ/property#> " + 
			"CONSTRUCT {?document opus:author ?author. ?document rdf:type ?type. ?document rdfs:label ?label. " +
			" ?author rdf:type ?type1. ?author foaf:name ?name.} " + 
			" WHERE" +
			"{?document opus:author ?authors. ?document rdf:type ?type. ?document rdfs:label ?label. " +
			" ?authors  rdfs:member ?author. ?author rdf:type ?type1. ?author foaf:name ?name." +
			" FILTER( XXXXXX ).}",
			
			//document-author
			"PREFIX dc: <http://purl.org/dc/elements/1.1/> " + 
			"PREFIX opus: <http://lsdis.cs.uga.edu/projects/semdis/opus#> " + 
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
			"PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> " +
			"PREFIX apf: <http://jena.hpl.hp.com/ARQ/property#> " + 
			"CONSTRUCT {?document opus:author ?author. ?document rdf:type ?type. ?document rdfs:label ?label. " +
			" ?author rdf:type ?type1. ?author foaf:name ?name.} " + 
			"WHERE" +
			"{?document opus:author ?authors. ?document rdf:type ?type. ?document rdfs:label ?label. " +
			" ?authors  rdfs:member ?author. ?author rdf:type ?type1. ?author foaf:name ?name." +
			" FILTER( regex(?label, \"XXXXXX\")).}",
		};
		
		FileWriter queryFile = new FileWriter("query.txt");
		BufferedReader personCandidateFile = new BufferedReader(new FileReader("personCandidate.txt"));
		BufferedReader documentCandidateFile = new BufferedReader(new FileReader("documentCandidate.txt"));
		String strPersonName = null;
		String strDocumentLabel = null;
		int n = 0; int m = 0;
		String strQuery = null;
		while((strPersonName = personCandidateFile.readLine()) != null){
			String[] strNames = strPersonName.split(" ");
			if(strNames.length > 1){
				++n;
				String strFilter = "";
				for(int i = 0;i < strNames.length;++i){
					strFilter += "regex(?name1,\\\"" + strNames[i].trim() + "\\\") && ";
				}
				strQuery = queries[0].replaceAll("XXXXXX", strFilter);
				queryFile.write(strQuery + "\n");
				
				strFilter = "";
				strFilter += "regex(?name,\\\"" + strNames[0].trim() + "\\\")";
				for(int i = 1;i < strNames.length;++i){
					strFilter += " && regex(?name,\\\"" + strNames[i].trim() + "\\\")";
				}
				strQuery = queries[1].replaceAll("XXXXXX", strFilter);
				queryFile.write(strQuery + "\n");
			}
		}
		
		while((strDocumentLabel = documentCandidateFile.readLine()) != null){
			strQuery = queries[2].replaceAll("XXXXXX", strDocumentLabel);
			queryFile.write(strQuery + "\n");++m;
		}
	
		System.out.println(n);
		System.out.println(m);
		queryFile.close();
		personCandidateFile.close();
		documentCandidateFile.close();
		
	}
	
	private static InputStream FileInputStream(String string) {
		// TODO Auto-generated method stub
		return null;
	}

	private static void selectCandidateConcept() throws IOException{
		Map<String,String> ep_re_Map = initMapWithMatchingFile(strRefBase + "eprints_rexa_goldstandard.xml");
		Map<String,String> re_dp_Map = initMapWithMatchingFile(strRefBase + "rexa-dblp-goldstandard.xml");
		Map<String,String> ep_dp_Map = initMapWithMatchingFile(strRefBase + "eprints_dblp_goldstandard.xml");
		Set<StringPair> ep_dp_Set = new HashSet<StringPair>();
		Set<StringPair> ep_re_Set = new HashSet<StringPair>();
		Set<StringPair> re_dp_Set = new HashSet<StringPair>();
		
		transformToPairs(ep_dp_Set,ep_dp_Map);
		transformToPairs(ep_re_Set,ep_re_Map);
		transformToPairs(re_dp_Set,re_dp_Map);
		
		DataOutputStream personFile = new DataOutputStream(new FileOutputStream("person.dat"));
		DataOutputStream docFile = new DataOutputStream(new FileOutputStream("document.dat"));
		DataOutputStream numFile = new DataOutputStream(new FileOutputStream("number.dat"));
		
		Iterator<Map.Entry<String,String>> it = ep_re_Map.entrySet().iterator();
		
		int i = 0;int iPerson = 0;int iDoc = 0;
		while(it.hasNext()){
			Map.Entry<String,String> ep_re_entry = it.next();
			String strEprints = ep_re_entry.getKey();
			String strRexa = ep_re_entry.getValue();
			String strDblp = re_dp_Map.get(strRexa);
			if(strDblp != null){
				//System.out.println(strEprints + "   " + strRexa + "   " + strDblp);
				if(strEprints.indexOf("person") >= 0){
					personFile.writeUTF(strEprints);
					personFile.writeUTF(strRexa);
					personFile.writeUTF(strDblp);
					++iPerson;
				}else{
					docFile.writeUTF(strEprints);
					docFile.writeUTF(strRexa);
					docFile.writeUTF(strDblp);
					++iDoc;
				}
				++i;
			}
		}
		System.out.println(ep_re_Map.size() + "  :->  " + re_dp_Map.size() + "  :->  " + ep_dp_Map.size());
		System.out.println(ep_re_Set.size() + "  :->  " + re_dp_Set.size() + "  :->  " + ep_dp_Set.size());
		System.out.println(i + ":->" + iPerson + ":->" + iDoc);
		numFile.writeInt(iPerson);
		numFile.writeInt(iDoc);
		numFile.close();
		personFile.close();
		docFile.close();
	}
	
	private static void transformToPairs(Set<StringPair> ep_dp_Set,
			Map<String, String> ep_dp_Map) {
		if(ep_dp_Set == null || ep_dp_Map == null){
			System.out.println("parameters are null!!!");return;
		}
		Iterator<Map.Entry<String,String>> it = ep_dp_Map.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String,String> entry = it.next();
			ep_dp_Set.add(new StringPair(entry.getKey(),entry.getValue()));
		}
	}

	private static Map<String, String> initMapWithMatchingFile(String strFileName) {
		if(strFileName == null)return null;
		HashMap<String,String> result = new HashMap<String,String>();
		
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(strFileName);
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		}
		
		NodeList cells = doc.getElementsByTagName("Cell");
		int iCellsLen = cells.getLength();
		for(int i = 0; i < iCellsLen; ++i){
			Element cell = (Element)cells.item(i);
			NodeList entitys1 = cell.getElementsByTagName("entity1");
			NodeList entitys2 = cell.getElementsByTagName("entity2");
			if(entitys1.getLength() == 0 || entitys2.getLength() == 0){
				System.out.println("entity1 and entity2 are not match!!!");
				System.exit(-1);
			}
			Element entity1 = (Element)entitys1.item(0);
			Element entity2 = (Element)entitys2.item(0);
			String strRes1 = entity1.getAttribute("rdf:resource");
			String strRes2 = entity2.getAttribute("rdf:resource");
			if(strRes1.isEmpty() || strRes2.isEmpty()){
				System.out.println("the entities have no valid value of rdf:resource!!!");
				System.exit(-1);
			}
			result.put(strRes1,strRes2);
		}
		return result;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		constructCandidateQuery();
		try {
			selectCandidateConcept();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			obtainCandidateString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			constructCandidateQuery();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		test();
		try {
			constructCandidateQueryResult();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			generateNumList(404,"NumList.dat");
			DataInputStream file = new DataInputStream(new FileInputStream("NumList.dat"));
			int Num = file.readInt();
			for(int i = 0;i < Num; ++i){
				System.out.println(file.readInt());
			}
			file.close();
		} catch (IOException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void generateNumList(int Num, String strOutputFileName) throws IOException{
		DataOutputStream file = new DataOutputStream(new FileOutputStream(strOutputFileName));
		file.writeInt(Num);
		LinkedList<Integer> list = new LinkedList<Integer>();
		for(int i = 1; i <= Num ; ++i){
			list.add(i);
		}
		int i = 0;
		for(; Num > 1 ; --Num){
			Random r = new Random();
			int index = r.nextInt(Num);
			int ir = list.remove(index);
			file.writeInt(ir);
			++i;
		}
		file.writeInt(list.get(0));
		file.close();
	}
	
	private static void test() {
		//Model[] models = new Model[2];
		//models[0] = initModel("eprints.rdf","UTF-8");
		//models[1] = initModel("rexa.rdf","UTF-8");
		
		String strQuery = 
			"PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX opus: <http://lsdis.cs.uga.edu/projects/semdis/opus#> PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> PREFIX apf: <http://jena.hpl.hp.com/ARQ/property#> CONSTRUCT {?document opus:author ?author. ?document rdf:type ?type. ?document rdfs:label ?label.  ?author rdf:type ?type1. ?author foaf:name ?name.}  WHERE{?document opus:author ?authors. ?document rdf:type ?type. ?document rdfs:label ?label.  ?authors  rdfs:member ?author. ?author rdf:type ?type1. ?author foaf:name ?name. FILTER( regex(?name,\"Conklin\") && regex(?name,\"Jeff\") ).}";

		long begin,end;
		begin = System.currentTimeMillis();
		Model model = ModelFactory.createDefaultModel();
		try {
			FileInputStream ontFile = new FileInputStream(strBase + "A-R-S-benchmark/opus.owl");
			InputStreamReader ontReader = new InputStreamReader(ontFile,"UTF-8");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
			ontFile = new FileInputStream(strBase + "A-R-S-benchmark/foaf.rdf");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
			ontFile = new FileInputStream(strBase + "A-R-S-benchmark/dcelements.rdf");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
			ontFile = new FileInputStream(strBase + "A-R-S-benchmark/rexa.rdf");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		
		Querier querier = new JenaQuerier(model);
		Model result = (Model)querier.Query(strQuery);
		result.write(System.out, "N3");
		end = System.currentTimeMillis();
		System.out.println(end - begin);
		model.close();
		//models[0].close();
		//models[1].close();
		
	}

	private static void obtainCandidateObject() throws IOException {
		// TODO Auto-generated method stub
		DataInputStream numberFile = new DataInputStream(new FileInputStream("number.dat"));
		int iPerson = numberFile.readInt();
		int iDoc = numberFile.readInt();
		numberFile.close();
		
		Model[] models = new Model[3];
		models[0] = initModel("eprints.rdf","UTF-8");
		models[1] = initModel("rexa.rdf","UTF-8");
		models[2] = initModel("swetodblp_april_2008.rdf","ISO-8859-1");
		
		//person
		DataInputStream personFile = new DataInputStream(new FileInputStream("person.dat"));
		DataOutputStream personNameFile = new DataOutputStream(new FileOutputStream("personName.dat"));
		FileWriter personNameWriter = new FileWriter("personName.txt");
		String strQuery = "PREFIX dc: <http://purl.org/dc/elements/1.1/> " + 
		"PREFIX opus: <http://lsdis.cs.uga.edu/projects/semdis/opus#> " + 
		"PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
		"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
		"PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> " +
		"PREFIX apf: <http://jena.hpl.hp.com/ARQ/property#> " + 
		"SELECT ?name " +
		"WHERE" +
		"{??? foaf:name ?name.}";
		for(int i = 0;i < iPerson;++i){
			System.out.println("person:" + i + ":" + iPerson);
			String[] personURI = new String[3];
			personURI[0] = personFile.readUTF();
			personURI[1] = personFile.readUTF();
			personURI[2] = personFile.readUTF();
			for(int j = 0;j < 3;++j){
				String strQuery1 = strQuery.replace("???", "<" + personURI[j] + ">");
				ResultSet resultSet = JenaQuerier.SelectQuery(models[j], strQuery1);
				if(resultSet.hasNext()){
					QuerySolution querySolution = resultSet.nextSolution();
					Literal name = querySolution.getLiteral("name");
					if(name != null && !name.toString().trim().isEmpty()){
						personNameFile.writeUTF(name.toString());
						personNameWriter.write(name.toString() + " || ");
					}else{
						personNameFile.writeUTF("XXX");
						personNameWriter.write("XXX" + " || ");
					}
				}else{
					personNameFile.writeUTF("XXX");
					personNameWriter.write("XXX" + " || ");
				}
			}
			personNameWriter.write("\n");
		}
		
		personNameFile.close();
		personFile.close();
		personNameWriter.close();
		
		//document
		DataInputStream documentFile = new DataInputStream(new FileInputStream("document.dat"));
		DataOutputStream documentLabelFile = new DataOutputStream(new FileOutputStream("documentLabel.dat"));
		FileWriter documentLabelWriter = new FileWriter("documentLabel.txt");
		strQuery = "PREFIX dc: <http://purl.org/dc/elements/1.1/> " + 
		"PREFIX opus: <http://lsdis.cs.uga.edu/projects/semdis/opus#> " + 
		"PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
		"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
		"PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> " +
		"PREFIX apf: <http://jena.hpl.hp.com/ARQ/property#> " + 
		"SELECT ?label " +
		"WHERE" +
		"{??? rdfs:label ?label.}";
		for(int i = 0;i < iDoc;++i){
			System.out.println("document:" + i + ":" + iDoc);
			String[] documentURI = new String[3];
			documentURI[0] = documentFile.readUTF();
			documentURI[1] = documentFile.readUTF();
			documentURI[2] = documentFile.readUTF();
			for(int j = 0;j < 3;++j){
				String strQuery1 = strQuery.replace("???", "<" + documentURI[j] + ">");
				ResultSet resultSet = JenaQuerier.SelectQuery(models[j], strQuery1);
				if(resultSet.hasNext()){
					QuerySolution querySolution = resultSet.nextSolution();
					Literal label = querySolution.getLiteral("label");	
					if(label != null && !label.toString().trim().isEmpty()){
						documentLabelFile.writeUTF(label.toString());
						documentLabelWriter.write(label.toString() + " || ");
					}else{
						documentLabelFile.writeUTF("XXX");
						documentLabelWriter.write("XXX || ");
					}
				}else{
					documentLabelFile.writeUTF("XXX");
					documentLabelWriter.write("XXX || ");
				}
			}
			documentLabelWriter.write("\n");
		}
		
		documentLabelFile.close();
		documentFile.close();
		documentLabelWriter.close();
		
		models[0].close();
		models[1].close();
		models[2].close();
	}
	
	private static void obtainCandidateString() throws IOException {
		// TODO Auto-generated method stub
		DataInputStream numberFile = new DataInputStream(new FileInputStream("number.dat"));
		int iPerson = numberFile.readInt();
		int iDoc = numberFile.readInt();
		numberFile.close();
		
		//person
		DataInputStream personNameFile = new DataInputStream(new FileInputStream("personName.dat"));
		DataOutputStream personCandidateFile = new DataOutputStream(new FileOutputStream("personCandidate.dat"));
		FileWriter personCandidateWriter = new FileWriter("personCandidate.txt");
	
		for(int i = 0;i < iPerson;++i){
			System.out.println("person:" + i + ":" + iPerson);
			String[] personName = new String[3];
			personName[0] = personNameFile.readUTF();
			personName[1] = personNameFile.readUTF();
			personName[2] = personNameFile.readUTF();
			String strCandidateName = getRecommendedString(personName);
			if(strCandidateName != null && strCandidateName.length() > 3){
				personCandidateFile.writeUTF(strCandidateName);
				personCandidateWriter.write(strCandidateName + "\n");
			}else{
				System.out.println("Can find saticfied string!!!" + personName[0] + " || " + personName[1] + " || " + personName[2]);
			}
		}
		
		personNameFile.close();
		personCandidateFile.close();
		personCandidateWriter.close();
		
		//document
		//person
		DataInputStream documentLabelFile = new DataInputStream(new FileInputStream("documentLabel.dat"));
		DataOutputStream documentCandidateFile = new DataOutputStream(new FileOutputStream("documentCandidate.dat"));
		FileWriter documentCandidateWriter = new FileWriter("documentCandidate.txt");
	
		for(int i = 0;i < iDoc;++i){
			System.out.println("document:" + i + ":" + iDoc);
			String[] documentLabel = new String[3];
			documentLabel[0] = documentLabelFile.readUTF();
			documentLabel[1] = documentLabelFile.readUTF();
			documentLabel[2] = documentLabelFile.readUTF();
			String strCandidateLabel= getRecommendedLabel(documentLabel);
			if(strCandidateLabel != null || !strCandidateLabel.isEmpty()){
				documentCandidateFile.writeUTF(strCandidateLabel);
				documentCandidateWriter.write(strCandidateLabel + "\n");
			}else{
				System.out.println("Can find saticfied string!!!" + documentLabel[0] + " || " + documentLabel[1] + " || " + documentLabel[2]);
			}
		}
		
		documentLabelFile.close();
		documentCandidateFile.close();
		documentCandidateWriter.close();
	}

	private static String getRecommendedLabel(String[] documentLabel) {
		String strReturn = null;
		String strTmp = LCS(documentLabel[0],documentLabel[1]);
		if(strTmp != null && !strTmp.isEmpty()){
			strReturn = LCS(documentLabel[2],strTmp);
		}
		return strReturn;
	}
	
	private static String LCS(String s1,String s2){
		if(s1.length() > s2.length()){   
			String temp  = s1;   
			s1 = s2;   
			s2 = temp;
			}   
		int n = s1.length();   
		int index = 0;
		ok:for(;n > 0; n--){   
			for(int i = 0;i < s1.length() - n + 1; i++){   
				String s = s1.substring(i,i + n);   
				if(s2.indexOf(s) != -1){ 
					index = i;
					break ok;
				}   
			}   
		}   
		return s1.substring(index, index + n);  
	}	

	private static String getRecommendedString(String[] personName) {
		if(personName == null || personName.length == 0){
			System.out.println("personName array is null or empty!!!");
			System.exit(-1);
		}
		String strReturn = null;
		Pattern pattern = Pattern.compile("[A-Z0-9a-z']*");
		Matcher matcher = pattern.matcher(personName[0]);
		while(matcher.find()){
			String string = matcher.group();
			if(string.length() > 3){
				if(personName[1].indexOf(string) >= 0 && personName[2].indexOf(string) >= 0){
					if(strReturn == null){
						strReturn = string;
					}else{
						strReturn += (" " + string);
					}
				}
			}
		}
		return strReturn;
	}

	private static Model initModel(String strFile,String strEncode) {
		Model model = ModelFactory.createDefaultModel();
		try {
			FileInputStream ontFile = new FileInputStream(strBase + "A-R-S-benchmark/opus.owl");
			InputStreamReader ontReader = new InputStreamReader(ontFile,"UTF-8");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
			ontFile = new FileInputStream(strBase + "A-R-S-benchmark/foaf.rdf");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
			ontFile = new FileInputStream(strBase + "A-R-S-benchmark/dcelements.rdf");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
			ontFile = new FileInputStream(strBase + "A-R-S-benchmark/" + strFile);
			ontReader = new InputStreamReader(ontFile,strEncode);
			model.read(ontReader,null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return model;
		
	}

}
