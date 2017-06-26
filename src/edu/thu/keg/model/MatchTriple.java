package edu.thu.keg.model;

public class MatchTriple implements Comparable<MatchTriple> {
	String srcURI;
	String tarURI;
	double score;
	
	public MatchTriple(String srcURI, String tarURI, double score) {
		this.srcURI = srcURI;
		this.tarURI = tarURI;
		this.score = score;
	}

	public String getSrcURI() {
		return srcURI;
	}

	public void setSrcURI(String srcURI) {
		this.srcURI = srcURI;
	}

	public String getTarURI() {
		return tarURI;
	}

	public void setTarURI(String tarURI) {
		this.tarURI = tarURI;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tarURI == null) ? 0 : tarURI.hashCode());
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((srcURI == null) ? 0 : srcURI.hashCode());
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
		MatchTriple other = (MatchTriple) obj;
		if (tarURI == null) {
			if (other.tarURI != null)
				return false;
		} else if (!tarURI.equals(other.tarURI))
			return false;
		if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
			return false;
		if (srcURI == null) {
			if (other.srcURI != null)
				return false;
		} else if (!srcURI.equals(other.srcURI))
			return false;
		return true;
	}
	
	@Override
	public int compareTo(MatchTriple triple) {
		if (this.equals(triple))
			return 0;
		else if (this.score > triple.getScore())
			return -1;
		else if (this.score < triple.getScore())
			return 1;
		else return 0;
	}
	
	@Override
	public String toString(){
		return srcURI + "\t" + tarURI + "\t" + score;
	}
}
