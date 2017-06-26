package edu.thu.keg.model;

public class MatchScore implements Comparable<MatchScore>{
	String tarURI;
	double score;
	
	public MatchScore(String tarURI, double score) {
		super();
		this.tarURI = tarURI;
		this.score = score;
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
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((tarURI == null) ? 0 : tarURI.hashCode());
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
		MatchScore other = (MatchScore) obj;
		if (Double.doubleToLongBits(score) != Double
				.doubleToLongBits(other.score))
			return false;
		if (tarURI == null) {
			if (other.tarURI != null)
				return false;
		} else if (!tarURI.equals(other.tarURI))
			return false;
		return true;
	}
	
	@Override
	public int compareTo(MatchScore ms) {
		if (this.equals(ms))
			return 0;
		else if (this.score > ms.getScore())
			return 1;
		else if (this.score < ms.getScore())
			return -1;
		else return 0; 
	}
}
