package test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.thu.keg.Invoker;
import edu.thu.keg.model.IndividualParameter;
import edu.thu.keg.util.StringPair;
import edu.thu.keg.util.Util;

public class QueryDrivenTest {
	
	public static enum ModelType{
		RDF_MEM,OWL_MEM,RDF_RDB,RDF_NDB;
	}
	
	public static class Ontology{
		public Model _model = null;
		public ModelType _type = ModelType.OWL_MEM;
		
		public Ontology(Model model,ModelType type){
			_model = model;
			_type = type;
		}
	}
	
	public static class PropertyImportancePair implements Comparable{
		String _strURI = null;
		double _dImportance = 0;
		Set<String> _valueSet = new HashSet<String>(); 
		
		public PropertyImportancePair(String strURI){
			if(strURI == null && strURI.isEmpty()){
				__logger.error("URI is null or empty!!!");
				System.exit(-1);
			}
			_strURI = strURI;
		}
		
		@Deprecated
		public PropertyImportancePair(String strURI,double dImportance){
			if(strURI == null && strURI.isEmpty()){
				__logger.error("URI is null or empty!!!");
				System.exit(-1);
			}
			_strURI = strURI;
			_dImportance = dImportance;
		}

		public PropertyImportancePair(PropertyImportancePair other) {
			if(other == null){
				__logger.error("Other propertyImportancePair object is null!!!");
				System.exit(-1);
			}
			if(other._strURI == null && other._strURI.isEmpty()){
				__logger.error("Other propertyImportancePair's URI is null or empty!!!");
				System.exit(-1);
			}
			_strURI = other._strURI;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((_strURI == null) ? 0 : _strURI.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final PropertyImportancePair other = (PropertyImportancePair) obj;
			if (_strURI == null) {
				if (other._strURI != null)
					return false;
			} else if (!_strURI.equals(other._strURI))
				return false;
			return true;
		}

		@Override
		public int compareTo(Object obj) {
			if (this == obj)
				return 0;
			if (obj == null)
				return -1;
			if (getClass() != obj.getClass())
				return -1;
			final PropertyImportancePair other = (PropertyImportancePair) obj;
			return _dImportance == other._dImportance? 0:(_dImportance > other._dImportance ? -1: 1); 
		}
		
		public void addToValueSet(String strValue){
			if(_valueSet.add(strValue))
				++_dImportance;
		}
	}
	
	private final static Logger __logger = LoggerFactory.getLogger(QueryDrivenTest.class);
	
	private String _strMatchResultFile = null;
	
	private Ontology _sourceOntology = null;
	private Ontology _targetOntology = null;
	
	public QueryDrivenTest(String strMatchResultFile,Ontology sourceOntology,Ontology targetOntology){
		_strMatchResultFile = strMatchResultFile;
		_sourceOntology = sourceOntology;
		_targetOntology = targetOntology;
	}
	
	public static void sufficientPropertyRanking(Ontology onto){
		String RDFLABEL = "http://www.w3.org/2000/01/rdf-schema#label";
		if(onto == null){
			__logger.error("onto is null!!!");return;
		}
		if(onto._type.equals(ModelType.OWL_MEM)){
			HashMap<String,HashSet<PropertyImportancePair>> clsPropMap = new HashMap<String,HashSet<PropertyImportancePair>>();
			OntModel owlOnto = (OntModel)onto._model;
			ExtendedIterator<DatatypeProperty> dataTypePropertys= owlOnto.listDatatypeProperties();
			while(dataTypePropertys.hasNext()){
				DatatypeProperty dtProperty = dataTypePropertys.next();
				OntResource ontRes = dtProperty.getDomain();
				if(ontRes != null && ontRes.isClass()){
					OntClass cls = ontRes.asClass();
					HashSet<PropertyImportancePair> pair = clsPropMap.get(cls.getURI());
					if(pair == null){
						pair = new HashSet<PropertyImportancePair>();
						pair.add(new PropertyImportancePair(dtProperty.getURI()));
						clsPropMap.put(cls.getURI(), pair);
					}else{
						pair.add(new PropertyImportancePair(dtProperty.getURI()));
					}
					pair.add(new PropertyImportancePair(RDFLABEL));
				}
			}
			OntClass thing = owlOnto.getOntClass("http://www.w3.org/2002/07/owl#Thing");
			ExtendedIterator<OntClass> itClasses = owlOnto.listClasses();
			while(itClasses.hasNext()){
				OntClass curOnto = itClasses.next();
				if(curOnto != thing && !curOnto.hasSuperClass()){
					thing.addSubClass(curOnto);
				}
			}
			if(thing != null){
				LinkedList<OntClass> queue = new LinkedList<OntClass>();
				HashSet<String> finishedURI = new HashSet<String>();
				queue.addLast(thing);
				while(queue.size() > 0){
					OntClass curClass = queue.pollLast();
					finishedURI.add(curClass.getURI());
					HashSet<PropertyImportancePair> pair = clsPropMap.get(curClass.getURI());
					ExtendedIterator<OntClass> itSubClasses = curClass.listSubClasses();
					if(pair != null){
						while(itSubClasses.hasNext()){
							OntClass subClass = itSubClasses.next();
							//System.out.println("Supper:" + curClass.getURI() + ">>Sub:" + subClass.getURI());
							HashSet<PropertyImportancePair> subPair = clsPropMap.get(subClass.getURI());
							if(subPair == null){
								subPair = new HashSet<PropertyImportancePair>();
								clsPropMap.put(subClass.getURI(), subPair);
							}
							Iterator<PropertyImportancePair> itPairs = pair.iterator();
							while(itPairs.hasNext()){
								subPair.add(new PropertyImportancePair(itPairs.next()));
							}
							if(!finishedURI.contains(subClass.getURI()))
								queue.addLast(subClass);
						}
					}else{
						while(itSubClasses.hasNext()){
							OntClass subClass = itSubClasses.next();
							//System.out.println("Supper:" + curClass.getURI() + ">>Sub:" + subClass.getURI());
							if(!finishedURI.contains(subClass.getURI()))
								queue.addLast(itSubClasses.next());
						}
					}
				}
			}
			
			Iterator<Entry<String,HashSet<PropertyImportancePair>>> itPairs = clsPropMap.entrySet().iterator();
			while(itPairs.hasNext()){
				Entry<String,HashSet<PropertyImportancePair>> curPair = itPairs.next();
				OntClass curClass = owlOnto.getOntClass(curPair.getKey());
				HashSet<PropertyImportancePair> curPropSet = curPair.getValue();
				if(curClass == null){
					__logger.error("The onto class in the map can not be found in the ontology!!! It is imposible!!!");
					System.exit(-1);
				}
				ExtendedIterator<? extends OntResource> individuals = curClass.listInstances();
				int iTotalNum = 0;
				while(individuals.hasNext()){
					Individual individual = individuals.next().asIndividual();
					++iTotalNum;
					Iterator<PropertyImportancePair> itPIPairs = curPropSet.iterator();
					while(itPIPairs.hasNext()){
						PropertyImportancePair PIPair = itPIPairs.next();
						if(!PIPair._strURI.equals(RDFLABEL)){
							Property prop = owlOnto.getOntProperty(PIPair._strURI);
							if(prop == null){
								__logger.error("The prop in the map can not be found in the ontology!!! It is imposible!!!");
								//System.exit(-1);
							}else{
								RDFNode value = individual.getPropertyValue(prop);
								if(value != null){
									PIPair.addToValueSet(value.toString());
								}
							}
						}else{
							
							String strLabel = individual.getLabel(null);
							if(strLabel != null){
								PIPair.addToValueSet(strLabel);
							}
							
						}
					}
				}
				Iterator<PropertyImportancePair> itPIPairs = curPropSet.iterator();
				if(iTotalNum != 0){
					while(itPIPairs.hasNext()){
						PropertyImportancePair PIPair = itPIPairs.next();
						PIPair._dImportance = (PIPair._dImportance) / iTotalNum;
						PIPair._valueSet = null;
					}
				}
			}
			
			displayResultMap(clsPropMap);
		}
	}
	
	private static void displayResultMap(
			HashMap<String, HashSet<PropertyImportancePair>> clsPropMap) {
		Iterator<Entry<String,HashSet<PropertyImportancePair>>> it = clsPropMap.entrySet().iterator();	
		while(it.hasNext()){
			Entry<String,HashSet<PropertyImportancePair>> entry = it.next();
			__logger.info(entry.getKey());
			PropertyImportancePair[] pairs = new PropertyImportancePair[entry.getValue().size()];
			entry.getValue().toArray(pairs);
			Arrays.sort(pairs);
			for(int i = 0; i < entry.getValue().size(); ++i){
				PropertyImportancePair piPair = pairs[i];
				__logger.info("\t::" + piPair._strURI + "::" + piPair._dImportance);
			}
		}
	}

	static class MyInt{
		int i = 0;
		int j = 0;
	}
	
	static class ClassInfo{
		Map<String,MyInt> propEqMap = new HashMap<String,MyInt>();
		int iIndiNum = 0;
	}
	
	public static void necessaryPropertyRanking(OntModel surModel, OntModel tarModel, String strFileName) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(strFileName));
		//ArrayList<StringPair> pairs = new ArrayList<StringPair>();
		Map<String,ClassInfo> map = new HashMap<String,ClassInfo>();
		
		String strTemp = null;
		while((strTemp = reader.readLine()) != null){
			String[] strings = strTemp.split("\t");
			//pairs.add(new StringPair(strings[0],strings[1]));
			Individual srcIndi = (Individual)surModel.getResource(strings[0]).as(Individual.class);
			Individual tarIndi = (Individual)tarModel.getResource(strings[1]).as(Individual.class);
			if(srcIndi == null || tarIndi == null){
				System.out.println("Something is error!!!");
				continue;
			}
			if(srcIndi.getURI().indexOf("person") >= 0)
				continue;
			HashMap<String,String> srcPropValMap = getPropValues(srcIndi);
			HashMap<String,String> tarPropValMap = getPropValues(tarIndi);
			String strType = srcPropValMap.get("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
			if(strType == null)
				continue;
			ClassInfo classInfo = map.get(strType);
			if(classInfo == null){
				classInfo = new ClassInfo();
				++classInfo.iIndiNum;
				map.put(strType, classInfo);
			}else
				++classInfo.iIndiNum;
			Iterator<Entry<String,String>> itTarPropValMap = tarPropValMap.entrySet().iterator();
			while(itTarPropValMap.hasNext()){
				Entry<String,String> entry = itTarPropValMap.next();
				String strSrcValue = srcPropValMap.get(entry.getKey());
				if(strSrcValue != null && entry.getValue() != null){
					if(strSrcValue.toLowerCase().equals(entry.getValue().toLowerCase())){
						MyInt eqNum = classInfo.propEqMap.get(entry.getKey());
						if(eqNum == null){
							MyInt i = new MyInt();
							i.i = 1;
							classInfo.propEqMap.put(entry.getKey(), i);
						}else{
							++eqNum.i;
						}
					}else{
						//System.out.println(strSrcValue + ":"  + entry.getValue());
						MyInt eqNum = classInfo.propEqMap.get(entry.getKey());
						if(eqNum == null){
							MyInt i = new MyInt();
							i.j = 1;
							classInfo.propEqMap.put(entry.getKey(), i);
						}else{
							++eqNum.j;
						}
					}
				}
			}
		}
		
		Iterator<Entry<String,ClassInfo>> it = map.entrySet().iterator();
		while(it.hasNext()){
			Entry<String,ClassInfo> e = it.next();
			__logger.info("###########" + e.getKey());
			Iterator<Entry<String,MyInt>> it2 = e.getValue().propEqMap.entrySet().iterator();
			while(it2.hasNext()){
				Entry<String,MyInt> e2 = it2.next();
				__logger.info("**" + e2.getKey() + " : " + (double)e2.getValue().i/(e2.getValue().i + e2.getValue().j));
			}
		}
	}
	
	private static HashMap<String, String> getPropValues(Individual indi) {
		HashMap<String,String> propValMap = new HashMap<String,String>();
		StmtIterator it = indi.listProperties();
		while (it.hasNext()) {
			Statement st = it.nextStatement();
			if (st.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#label")) {
				propValMap.put("http://www.w3.org/2000/01/rdf-schema#label", st.getObject().toString());
			}else if (st.getPredicate().toString().equals("http://lsdis.cs.uga.edu/projects/semdis/opus#book_title")) {
				propValMap.put("http://lsdis.cs.uga.edu/projects/semdis/opus#book_title", st.getObject().toString());	
			}else if (st.getPredicate().toString().equals("http://lsdis.cs.uga.edu/projects/semdis/opus#pages")) {
				String strPage = refinePage(st.getObject().toString());
				propValMap.put("http://lsdis.cs.uga.edu/projects/semdis/opus#pages",strPage);	
			}else if (st.getPredicate().toString().equals("http://lsdis.cs.uga.edu/projects/semdis/opus#year")) {
				propValMap.put("http://lsdis.cs.uga.edu/projects/semdis/opus#year", st.getObject().toString());	
			}else if (st.getPredicate().toString().equals("http://lsdis.cs.uga.edu/projects/semdis/opus#volume")) {
				propValMap.put("http://lsdis.cs.uga.edu/projects/semdis/opus#volume", st.getObject().toString());	
			}else if (st.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
				propValMap.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", st.getObject().toString());	
			}
		}
		return propValMap;
	}
	
	private static String refinePage(String string) {
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
	public static void testSufficientProperty(){
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		String strOntoFile = "swetodblp_april_2008.rdf";
		//String strOntoFile = "rexa.rdf";
		try {
			FileInputStream ontFile = new FileInputStream(Invoker.ORIGIN_PATH + "opus.owl");
			InputStreamReader ontReader = new InputStreamReader(ontFile,"UTF-8");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
			ontFile = new FileInputStream(Invoker.ORIGIN_PATH + "foaf.rdf");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
			ontFile = new FileInputStream(Invoker.ORIGIN_PATH + "dcelements.rdf");
			ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
			ontFile = new FileInputStream(Invoker.ORIGIN_PATH + strOntoFile);
			ontReader = new InputStreamReader(ontFile,"ISO-8859-1");
			//ontReader = new InputStreamReader(ontFile,"UTF-8");
			model.read(ontReader,null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		__logger.info("**********************" + strOntoFile + "*************************");
		sufficientPropertyRanking(new Ontology(model,ModelType.OWL_MEM));
	}
	
	public static void testNecessaryProperty(String strSourceFile,String strSourceType,
			String strTargetFile, String strTargetType, String strRefFile) throws IOException{
		OntModel srcModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		OntModel tarModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		try {
			FileInputStream srcFile = new FileInputStream(strSourceFile);
			InputStreamReader ontReader = new InputStreamReader(srcFile,strSourceType);
			srcModel.read(ontReader,null);
			
			FileInputStream tarFile = new FileInputStream(strTargetFile);
			ontReader = new InputStreamReader(tarFile,strTargetType);
			tarModel.read(ontReader,null);
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}

		necessaryPropertyRanking(srcModel,tarModel,strRefFile);
	}
	

	
	static class PageSelector implements Selector{

		@Override
		public RDFNode getObject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Property getPredicate() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Resource getSubject() {
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
			// TODO Auto-generated method stub
			if(arg0.getPredicate().getURI().equals("http://lsdis.cs.uga.edu/projects/semdis/opus#pages"))
				return true;
			return false;
		}
		
	}
	
	public static void testPagesProperty(String strSourceFile,String strSourceType,
			String strResFile) throws IOException{
		OntModel srcModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		OntModel tarModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		try {
			FileInputStream srcFile = new FileInputStream(strSourceFile);
			InputStreamReader ontReader = new InputStreamReader(srcFile,strSourceType);
			srcModel.read(ontReader,null);			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}

		getPagesPropertyValue(srcModel,strResFile);
	}
	
	public static void getPagesPropertyValue(OntModel model,String strFileName) throws IOException{
		FileWriter writer = new FileWriter(strFileName);
		StmtIterator itStmt = model.listStatements(new PageSelector());
		while(itStmt.hasNext()){
			Statement stmt = itStmt.next();
			//System.out.println(stmt.getPredicate().getURI());
			String strPage = stmt.getObject().toString();
			if(!strPage.trim().isEmpty())
				writer.write(stmt.getSubject().getURI() + "\t" + stmt.getObject().toString() + "\n");
		}
		writer.close();
	}

	public static int testTarget(String strSourceFile) throws IOException{
		OntModel srcModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		FileInputStream srcFile = null;
		int iNum = 0;
		try {
			srcFile = new FileInputStream(strSourceFile);
			InputStreamReader ontReader = new InputStreamReader(srcFile,"UTF-8");
			srcModel.read(ontReader,null);			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return 0;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return 0;
		}
		iNum = testTarget(srcModel);
		srcFile.close();
		return iNum;
	}
	
	public static int testTarget(OntModel srcModel) throws IOException{
		
		ResIterator srit = srcModel.listSubjects();
		int iNum = 0;
		while (srit.hasNext()) {
			Resource res = srit.nextResource();
			if (Util.testIndi(res)) {
				++iNum;
			}
		}
		return iNum;
	}
	
	
	
	static void testInputFile(String strBaseName) throws IOException{
		int iMax = 0;
		int iMin = 999999999;
		int iTotal = 0;
		for(int i = 1; i <= 404; ++i){
			int iCur = testTarget("input/" + strBaseName + i + ".rdf");
			iTotal += iCur;
			if(iCur > iMax){
				iMax = iCur;
			}
			if(iCur < iMin)
				iMin = iCur;
		}
		System.out.println("avg:" + (double)iTotal/404);
		System.out.println("max:" + iMax);
		System.out.println("min:" + iMin);
	}
	
	static void testTotalInstancesNum(String strBase){
		HashSet<String> instances = new HashSet<String>();
		OntModel srcModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		FileInputStream srcFile = null;
		try {
			for(int i = 1; i < 404; ++i){
				srcFile = new FileInputStream("input/" + strBase + i + ".rdf");
				InputStreamReader ontReader = new InputStreamReader(srcFile,"UTF-8");
				srcModel.read(ontReader,null);	
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		ResIterator srit = srcModel.listSubjects();
		while (srit.hasNext()) {
			Resource res = srit.nextResource();
			if (Util.testIndi(res)) {
				instances.add(res.getURI());
			}
		}
		System.out.println(instances.size());
		return ;

	}
	
	private static void sortTest() {
		HashSet<PropertyImportancePair> pair = new HashSet<PropertyImportancePair>();
		pair.add(new PropertyImportancePair("http://1",0.123));
		pair.add(new PropertyImportancePair("http://2",12.334));
		pair.add(new PropertyImportancePair("http://3",8.8123));
		pair.add(new PropertyImportancePair("http://4",1937.1234));
		pair.add(new PropertyImportancePair("http://5",-0.123));
		PropertyImportancePair[] a = new PropertyImportancePair[pair.size()];
		pair.toArray(a);
		Arrays.sort(a);
		for(int i = 0;i < a.length; ++i){
			System.out.println(a[i]._strURI + ":" + a[i]._dImportance);
		}
	}
	public static void main(String[] args) throws IOException{
		String strFileName = "temp/dblp_pageproperty.txt";
		__logger.info(strFileName);
		//Éú³ÉÊôÐÔÒ³
		//testPagesProperty(Invoker.ORIGIN_PATH + "swetodblp_april_2008.rdf","ISO-8859-1","temp/dblp_pageproperty.txt");
		//testPagesProperty(Invoker.ORIGIN_PATH + "rexa.rdf","UTF-8","temp/rexa_pageproperty.txt");
		//testPagesProperty(Invoker.ORIGIN_PATH + "eprints.rdf","UTF-8","temp/eprints_pageproperty.txt");
		
		testNecessaryProperty(Invoker.ORIGIN_PATH + "rexa.rdf","UTF-8",Invoker.ORIGIN_PATH + "swetodblp_april_2008.rdf", "ISO-8859-1", "temp/dblp_pageproperty.txt");
		
		//testTarget(Invoker.ORIGIN_PATH + "swetodblp_april_2008.rdf");
		//testInputFile("dblp");
		//testTotalInstancesNum("dblp");
	}
}

