package edu.thu.keg.control;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import com.hp.hpl.jena.ontology.Individual;

import edu.thu.keg.Invoker;
import edu.thu.keg.util.StringPair;

import edu.thu.keg.model.IndividualParameter;
import edu.thu.keg.model.Match;
import edu.thu.keg.model.MatchScore;
import edu.thu.keg.process.LabelMatcher;
import edu.thu.keg.util.AnyPair;
import edu.thu.keg.util.EDSimilariter;
import edu.thu.keg.util.Util;
import edu.thu.keg.util.VDSimilariter;

public class LabelMatcherProcessUnit implements Callable<Match> {
	private LabelMatcher _matcher = null;
	private IndividualParameter _srcPair = null;
	private Collection<IndividualParameter> _tarPairs = null;
	public static double SCORE_THRESHOLD = 0;//can be modified by global matching
	
	public LabelMatcherProcessUnit(LabelMatcher matcher,
			IndividualParameter srcPair,
			Collection<IndividualParameter> tarPairs){
		_matcher = matcher;
		_srcPair = srcPair;
		_tarPairs = tarPairs;
	}
	
	@Override
	public Match call() throws Exception {
		// TODO Auto-generated method stub

		Match m = new Match(_srcPair.get_strURI());		
		Iterator<IndividualParameter> itTar = _tarPairs.iterator();
		while (itTar.hasNext()) {
			IndividualParameter tarPara = itTar.next();
			if(!(_srcPair.get_strType().equals(tarPara.get_strType()))){
				continue;
			}
			
			double score = 0;
			boolean isSkip = false;
			
			if(Invoker.__bUseNecessaryProperty){
				String strSrcPage = Invoker.__srcPagePropMap.get(_srcPair.get_strURI());
				String strTarPage = Invoker.__tarPagePropMap.get(tarPara.get_strURI());
				if(strSrcPage != null && strTarPage != null){
					strSrcPage = Util.refinePage(strSrcPage);
					strTarPage = Util.refinePage(strTarPage);
					if(!strSrcPage.equals(strTarPage)){
						//System.out.println("Skip");
						isSkip = true;
					}
				}
			}
			
			if(!isSkip){
				if(Invoker.__bUserFeedback){
					Boolean bMatched = Invoker.__feedbackedResults.get(new StringPair(_srcPair.get_strURI(),tarPara.get_strURI()));
					if(bMatched != null){
						if(bMatched.booleanValue()){
							score = 1;
						}
					}else{
						if (_srcPair.get_strType().equals("person")){
							score = VDSimilariter.sim(_srcPair.get_tokenedLabelList(), tarPara.get_tokenedLabelList());			
						}else{
							score = EDSimilariter.sim(_srcPair.get_strLabel(), tarPara.get_strLabel());
						}
					}
				}else{
					if (_srcPair.get_strType().equals("person")){
						score = VDSimilariter.sim(_srcPair.get_tokenedLabelList(), tarPara.get_tokenedLabelList());			
					}else{
						score = EDSimilariter.sim(_srcPair.get_strLabel(), tarPara.get_strLabel());
					}
				}
			}
			/*if (_matcher.getType() == LabelMatcher.TYPE_ED)
				score = EDSimilariter.sim(srcPara.get_strLabel(), tarPara.get_strLabel());
			else if (_matcher.getType() == LabelMatcher.TYPE_VD)
				score = VDSimilariter.sim(srcPara.get_strLabel(), tarPara.get_strLabel());*/
			
			if (score > SCORE_THRESHOLD) {
				MatchScore ms = new MatchScore(tarPara.get_strURI(), score);
				m.addScore(ms);
			}
		}
			
		return m;
	
	}
}
