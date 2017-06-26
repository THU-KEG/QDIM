package edu.thu.keg.query.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

import edu.thu.keg.query.Querier;


public class JenaQuerier implements Querier<Model,String,Model> {
	Model _model = null;
	public JenaQuerier(){
		
	}
	
	public JenaQuerier(Model model){
		_model = model;
	}
	
	public Model Query(Model dataSource, String parameter) {
		return null;
	}
	
	public Model Query(String strQuery){//str必须是construct的语句
		if(_model == null)
			return null;
		Query query = QueryFactory.create(strQuery);
		QueryExecution queryExecution = QueryExecutionFactory.create(query,_model);
		Model resultModel = null;
		try{
			resultModel = queryExecution.execConstruct();
		}finally{
			queryExecution.close();
		};
		return resultModel;
	}
	
	public static ResultSet SelectQuery(Model model, String strQuery){//执行一个select
		Query query = QueryFactory.create(strQuery);
		QueryExecution queryExecution = QueryExecutionFactory.create(query,model);
		ResultSet resultSet = null;
		try{
			resultSet = queryExecution.execSelect();
		}finally{
			//queryExecution.close();
		}	
		return resultSet;
	}
}
