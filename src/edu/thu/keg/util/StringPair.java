package edu.thu.keg.util;


public class StringPair {
	private String str_P1;
	private String str_P2;
	
	public StringPair(String str_P1,String str_P2){
		this.str_P1 = str_P1;
		this.str_P2 = str_P2;
	}

	public String getStr_P1() {
		return str_P1;
	}

	public void setStr_P1(String str_P1) {
		this.str_P1 = str_P1;
	}

	public String getStr_P2() {
		return str_P2;
	}

	public void setStr_P2(String str_P2) {
		this.str_P2 = str_P2;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((str_P1 == null) ? 0 : str_P1.hashCode());
		result = PRIME * result + ((str_P2 == null) ? 0 : str_P2.hashCode());
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
		final StringPair other = (StringPair) obj;
		if (str_P1 == null) {
			if (other.str_P1 != null)
				return false;
		} else if (!str_P1.equals(other.str_P1))
			return false;
		if (str_P2 == null) {
			if (other.str_P2 != null)
				return false;
		} else if (!str_P2.equals(other.str_P2))
			return false;
		return true;
	}
	
	
}

