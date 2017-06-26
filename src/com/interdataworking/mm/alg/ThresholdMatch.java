package com.interdataworking.mm.alg;

import com.interdataworking.UntypedGateway;

import org.w3c.rdf.model.*;
import java.util.*;

public class ThresholdMatch implements UntypedGateway {

	public static double THRESHOLD_PERCENTAGE = 0.0; // was 0.15;

	// static final double SENSITIVITY_DROP = 0.9; // 10%
	public static double SENSITIVITY_DROP = 1.0; // none

	public static int MAX_RETAIN = 5;

	boolean USE_REL_CONF_INSTEAD_OF_DISC = true;

	public ThresholdMatch(boolean useRelConf) {

		USE_REL_CONF_INSTEAD_OF_DISC = useRelConf;
	}

	public List<MapPair> getThresholdMatch(List<MapPair> arr) {

		List<MapPair> result = new ArrayList<MapPair>();
		getThresholdMatch(arr, result);
		return result;
	}

	// input: sorted array
	public List<MapPair> getThresholdMatch(MapPair[] a) {

		List<MapPair> result = new ArrayList<MapPair>();
		getThresholdMatch(a, result);
		return result;
	}

	public void getThresholdMatch(List<MapPair> arr, Collection<MapPair> result) {

		MapPair[] a = new MapPair[0];
		a = arr.toArray(a);
		// MapPair.sort(a);
		getThresholdMatch(a, result);
	}

	// first sortGroup left, remember trash
	// then sortGroup right, remember trash
	// then compile the result

	public void getThresholdMatch(MapPair[] a, Collection<MapPair> result) {

		getThresholdMatch(a, result, THRESHOLD_PERCENTAGE);
	}

	public void getThresholdMatch(MapPair[] a, Collection<MapPair> result, double threshold) {

		List<MapPair> tempResult = null;

		int retain = MAX_RETAIN;

		Set<MapPair> goodStuff = new HashSet<MapPair>();

		for (int i = 0; i < 1; i++) {

			tempResult = new ArrayList<MapPair>();
			int suspicious = thresholdPass(a, tempResult, threshold, retain, goodStuff);
			suspicious++;
			suspicious--;
			// System.err.println("TOTAL SUSPICIOUS AFTER " + i+ ": " +
			// suspicious);
			// By liyi
			// a = tempResult.toArray();
			// threshold *= SENSITIVITY_DROP;
		}

		// fill result
		result.addAll(tempResult);
	}

	// returns number of suspicious matches
	public int thresholdPass(MapPair[] a, Collection<MapPair> result, double threshold, int retain,
			Set<MapPair> goodStuff) {

		Set<MapPair> trash = new HashSet<MapPair>();
		int suspicious = 0;

		MapPair.sortGroup(a, false);
		suspicious += collectTrash(a, trash, goodStuff, false, threshold, retain);

		MapPair.sortGroup(a, true);
		suspicious += collectTrash(a, trash, goodStuff, true, threshold, retain);

		// compile result

		for (int i = 0; i < a.length; i++) {

			MapPair p = a[i];
			if (goodStuff.contains(p) || !trash.contains(p))
				result.add(p);
		}

		return suspicious;
	}

	// returns suspicious
	int collectTrash(Object[] a, Set<MapPair> trash, @SuppressWarnings("unused")
	Set<MapPair> goodStuff, boolean isRight, double threshold,
			int retain) {

		//RDFNode old = null;

		int i = 0;
		int suspicious = 0;

		// double threshold = thresholdBound;

		while (i < a.length) {

			MapPair pivot = (MapPair) a[i];
			// System.err.println("Checking pair 1: " + pivot);

			//int pivotIndex = i;
			RDFNode pivotNode, curr;
			//RDFNode pivotNode prev;
			curr = pivotNode = isRight ? pivot.getRightNode() : pivot.getLeftNode();

			boolean trashRest = false;
			int retainedNum = 0;

			while (true) {

				// first is guaranteed to be ok

				if (++i >= a.length)
					break;

				MapPair p = (MapPair) a[i];
				curr = isRight ? p.getRightNode() : p.getLeftNode();

				if (!curr.equals(pivotNode))
					break;

				// System.err.println("Checking pair 2: " + p);
				//RDFNode trashNode = isRight ? p.getLeftNode() : p.getRightNode();

				if (trashRest) {

					trash.add(p);
					// System.err.println("" + pivotNode + ": trashed node = " +
					// trashNode);

				} else {

					// discrimination
					double disc = (1 - p.sim_ / ((MapPair) a[i - 1]).sim_);

					// relative confidence
					double rel_conf = (1 - p.sim_ / pivot.sim_);

					double new_threshold = threshold; 
					if ((USE_REL_CONF_INSTEAD_OF_DISC && rel_conf > new_threshold)
							|| (!USE_REL_CONF_INSTEAD_OF_DISC && disc > new_threshold)) {
						// trash this one and the rest
						// System.err.println("Trashing " + trashNode + ",
						// pivot: " + pivotNode + ",rel_conf: " + rel_conf + ",
						// threshold: " + threshold);
						trash.add(p);
						trashRest = true;
					} else {
						// System.err.println("" + pivotNode + ": retained node
						// = " + trashNode + ", new sens: " +
						// threshold_percentage);
						retainedNum++;
						new_threshold *= SENSITIVITY_DROP;
					}
				}
			}

			// check whether need to trash all

			if (retainedNum > retain) {
				suspicious++;
				// System.err.println("Suspicious " + isRight + ": " +
				// pivotNode);

			} else if (trashRest) { // mark as goodStuff only if found a lower
									// bound, or single match

				/*
				 * for(int j = pivotIndex; j < pivotIndex + retainedNum + 1;
				 * j++) { goodStuff.add(a[j]); }
				 */
			}

			/*
			 * if(retainedNum > 5) { // too many similar items, trash all of
			 * them for(int j = pivotIndex; j < i; j++) { trash.add(a[j]); } }
			 */
		}

		return suspicious;
	}

	public List<Model> execute(List input) throws ModelException {

		Model src = (Model) input.get(0);

		List<MapPair> l1 = MapPair.toMapPairs(src);

		double threshold = THRESHOLD_PERCENTAGE;

		if (input.size() == 2)
			threshold = Double.parseDouble((String) input.get(1));

		List<MapPair> best = new ArrayList<MapPair>();
		MapPair[] temp = new MapPair[0];
		getThresholdMatch(l1.toArray(temp), best, threshold);

		ArrayList<Model> l = new ArrayList<Model>();
		Model dest = MapPair.asModel(src.create(), best.toArray(temp));
		l.add(dest);
		return l;
	}

	public int getMinInputLen() {
		return 1;
	}

	public int getMaxInputLen() {
		return 2;
	}

	public int getMinOutputLen() {
		return 1;
	}

	public int getMaxOutputLen() {
		return 1;
	}

}
