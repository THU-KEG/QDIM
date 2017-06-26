package edu.thu.keg.model;

public class GlobalMatchScore extends MatchScore {
	int concurrence;
	int matched;
	
	public GlobalMatchScore(String tarURI, double score) {
		super(tarURI, score);
		concurrence = 0;
		matched = 0;
	}
	
	public GlobalMatchScore(String tarURI, double score, int concurrence, int matched) {
		super(tarURI, score);
		this.concurrence = concurrence;
		this.matched = matched;
	}
	
	public void updateScore(int srcCount, int tarCount) {
		//System.out.println("Concurrence : Matched : SrcCount : TarCount  " + this.concurrence +  " : "
		//		+ this.matched + " : " + srcCount + " : " + tarCount);
		
		/* first idea
		 this.score = concurrence * matched * 1.0 / (srcCount * tarCount);
		*/
		 
		//google distance based
		if(concurrence != 0){
			double dGoogleDistance = (Math.log(Math.min(srcCount, tarCount))-Math.log(concurrence))/
			(Math.log(GlobalMatchModel.QUERY_NUM)-Math.log(Math.max(srcCount, tarCount) ) ) ;
			double dGoogleSimilarity = 1/(1 + dGoogleDistance);
			this.score = ((double)matched/concurrence) * dGoogleSimilarity;
		}else{
			this.score = 0;
		}
		
		
		//this.score = ((double)matched/concurrence)*((double)concurrence/Math.min(srcCount, tarCount));
	}

	public int getConcurrence() {
		return concurrence;
	}

	public void setConcurrence(int concurrence) {
		this.concurrence = concurrence;
	}

	public int getMatched() {
		return matched;
	}

	public void setMatched(int matched) {
		this.matched = matched;
	}
}
