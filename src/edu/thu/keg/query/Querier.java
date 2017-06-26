package edu.thu.keg.query;

public interface Querier<DsType,ParaType,ResultType>{
	public ResultType Query(DsType dataSource, ParaType parameter);
	public ResultType Query(ParaType parameter);
}
