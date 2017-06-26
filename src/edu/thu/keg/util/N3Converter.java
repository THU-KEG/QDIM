package edu.thu.keg.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class N3Converter {
	public static String BASE = "query_result";
	public static String NEW_BASE = "input/";
	
	public static void main(String args[]) throws Exception {
		File baseDir = new File(BASE);
		File[] files = baseDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().startsWith("dblp") || files[i].getName().startsWith("eprints") || files[i].getName().startsWith("rexa")) {
				String inputFile = files[i].getAbsolutePath();
				String outputFile = NEW_BASE + files[i].getName().substring(0, files[i].getName().indexOf('.')) + ".rdf";
				convert(inputFile, outputFile);
				System.out.println(inputFile + " finished. ReWrite to " + outputFile);
			}
		}
	}
	
	public static void convert(String inputFile, String outputFile) {
		try {
			FileInputStream fis = new FileInputStream(inputFile);
			Model inputModel = ModelFactory.createDefaultModel();
			inputModel.read(fis, "", "N3");
			FileOutputStream fos = new FileOutputStream(outputFile);
			inputModel.write(fos);
			fis.close();
			fos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
