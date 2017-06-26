package edu.thu.keg.model;

import java.util.List;

import edu.thu.keg.util.NaiveNameTokenizer;

public class IndividualParameter {
	String _strLabel = null;
	String _strType = null;
	String _strURI = null;
	String _strPage = null;
	List<String> _tokenedLabelList = null;
	
	public IndividualParameter(	String strURI, String strLabel,String strType){
		_strLabel = strLabel;
		_strType= strType;
		_strURI = strURI;
		NaiveNameTokenizer tokenizer = new NaiveNameTokenizer();
		_tokenedLabelList = tokenizer.tokenize(_strLabel);
	}
	
	public String get_strLabel() {
		return _strLabel;
	}
	public void set_strLabel(String label) {
		_strLabel = label;
		NaiveNameTokenizer tokenizer = new NaiveNameTokenizer();
		_tokenedLabelList = tokenizer.tokenize(_strLabel);
	}
	public String get_strType() {
		return _strType;
	}
	public void set_strType(String type) {
		_strType = type;
	}
	public String get_strURI() {
		return _strURI;
	}
	public void set_strURI(String _struri) {
		_strURI = _struri;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_strURI == null) ? 0 : _strURI.hashCode());
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
		final IndividualParameter other = (IndividualParameter) obj;
		if (_strURI == null) {
			if (other._strURI != null)
				return false;
		} else if (!_strURI.equals(other._strURI))
			return false;
		return true;
	}

	public List<String> get_tokenedLabelList() {
		return _tokenedLabelList;
	}

	public String get_strPage() {
		return _strPage;
	}

	public void set_strPage(String page) {
		_strPage = page;
	}
}
