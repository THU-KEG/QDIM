package com.interdataworking.mm.alg;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.rdf.model.Literal;
import org.w3c.rdf.model.Model;
import org.w3c.rdf.model.ModelException;
import org.w3c.rdf.model.NodeFactory;
import org.w3c.rdf.model.RDFNode;
import org.w3c.rdf.model.Resource;
import org.w3c.rdf.model.Statement;
import org.w3c.rdf.util.RDFFactory;
import org.w3c.rdf.util.RDFFactoryImpl;
import org.w3c.rdf.util.RDFUtil;

import com.interdataworking.UntypedGateway;

/**
 * This is an implementation of the Similarity Flooding algorithm described in
 * the ICDE'02 paper.
 * 
 */

public class Match implements UntypedGateway {

	public boolean DEBUG = false;

	public static final int DEBUG_MAX_ITERATIONS = 0;

	// default formula to be used: {ADD_SIGMA0_BEFORE=t, ADD_SIGMA0_AFTER=t,
	// ADD_SIGMAN_AFTER=t}
	public boolean[] formula = FORMULA_TTT;

	// default way of computing the propagation coefficients to be used
	public int FLOW_GRAPH_TYPE = FG_AVG;

	// various iteration formulas

	// MAY BE BETTER! but much worse convergence!
	// sigma^{n+1} = normalize(f(sigma^0 + sigma^n));
	public static final boolean[] FORMULA_TFF = { true, false, false };

	// SLIGHTLY WORSE, BUT BETTER CONVERGENCE!
	// sigma^{n+1} = normalize(sigma^0 + sigma^n + f(sigma^0 + sigma^n));
	public static final boolean[] FORMULA_TTT = { true, true, true };

	// USE THIS ONE FOR TESTING/DEBUGGING - PURE VERSION
	// sigma^{n+1} = normalize(sigma^n + f(sigma^n));
	public static final boolean[] FORMULA_FFT = { false, false, true };

	// sigma^{n+1} = normalize(sigma^0 + f(sigma^n));
	public static final boolean[] FORMULA_FTF = { false, true, false };

	// sigma^{n+1} = normalize(sigma^0 + f(sigma^0 + sigma^n));
	public static final boolean[] FORMULA_TTF = { true, true, false };

	// sigma^{n+1} = normalize(sigma^n + f(sigma^0 + sigma^n));
	public static final boolean[] FORMULA_TFT = { true, false, true };

	// sigma^{n+1} = normalize(sigma^0 + sigma^n + f(sigma^n));
	public static final boolean[] FORMULA_FTT = { false, true, true };

	// static final boolean ADD_SIGMAN_BEFORE = true; // ALWAYS TRUE, OTHERWISE
	// DOES NOT MAKE SENSE
	static final double MIN_NODE_SIM2 = StringMatcher.MIN_NODE_SIM;

	// ways of computing propagation coefficients

	public static final int FG_PRODUCT = 1;

	public static final int FG_AVG = 2;

	public static final int FG_EQUAL = 3;

	public static final int FG_TOTALP = 4;

	public static final int FG_TOTALS = 5;

	public static final int FG_AVG_TOTALS = 6;

	public static final int FG_STOCHASTIC = 7; // weight of OUTGOING normalized

	// to 1

	public static final int FG_INCOMING = 8; // weight of INCOMING normalized

	// to 1

	// other variables and constants

	public static final double UPDATE_GUESS_WEIGHT = 1.0; // between 0 and 1

	public double RESIDUAL_VECTOR_LENGTH = 0.05; // make this number smaller

	// to increase precision

	public int MAX_ITERATION_NUM = 10000;

	public int MIN_ITERATION_NUM = 7;

	public int TIMEOUT = 30 * 1000; // 30 sec

	public boolean TRY_ALL_ARCS = false; // consider all arcs, not only those

	// that are equal

	public boolean DIRECTED_GRAPH = true;

	public boolean TEST = true;

	static final int MIN_CHOICE_ITEMS = 50;

	public double EQUAL_PRED_COEFF = 1.0;

	public double OTHER_PRED_COEFF = 0.001;

	Model m1, m2;

	Map<PGNode, PGNode> pgnodes = new HashMap<PGNode, PGNode>();

	List<PGArc> pgarcs = new ArrayList<PGArc>();

	// cache for reusable pairs
	MapPair[] cachePairs = { new MapPair(), new MapPair() };

	static final int PASS_PAIR = 0;

	static final int GET_PAIR = 1;

	/**
	 * Transforms an ordered list of models into another list of models
	 */
	public List<Model> execute(List<Model> input) throws ModelException {

		Model mm1 = input.get(0);
		Model mm2 = input.get(1);
		List<MapPair> sigma0 = null;

		if (input.size() > 2) {
			Model initialMap = input.get(2);
			sigma0 = MapPair.toMapPairs(initialMap);
			if (sigma0.size() == 0)
				sigma0 = null;
		}

		PGNode[] finalList = getMatch(mm1, mm2, sigma0);

		Model map = MapPair.asModel(mm1.create(), finalList);
		ArrayList<Model> l = new ArrayList<Model>();
		l.add(map);
		return l;
	}

	public PGNode[] getMatch(Model mm1, Model mm2, List<MapPair> sigma0) throws ModelException {

		this.m1 = mm1;
		this.m2 = mm2;
		// this.sigma0 = rm;

		// computes sigma0 if null; must go before constructPropagationGraph
		if (sigma0 == null)
			initSigma0();
		else {
			// set initial values from sigma0
			for (Iterator<MapPair> it = sigma0.iterator(); it.hasNext();) {

				MapPair p = it.next();
				PGNode n = this.pgnodes.get(p);
				if (n == null) {
					n = new PGNode(p.getLeft(), p.getRight());
					this.pgnodes.put(n, n);
				}
				n.sim0 = (p.sim_ == MapPair.NULL_SIM ? 1 : p.sim_);
				n.inverse_ = p.inverse_; // make sure chosen direction remains
				// unchanged
			}
		}

		// must go AFTER cardMaps
		boolean ignorePredicates = this.TRY_ALL_ARCS; // || (FLOW_GRAPH_TYPE
														// ==
		// FG_STOCHASTIC);

		long pgstart = System.currentTimeMillis();
//		System.err.print("Creating propagation graph: ");
		constructPropagationGraph(ignorePredicates);
		long pgend = System.currentTimeMillis();
		System.err.println("" + (double) (pgend - pgstart) / 1000 + " sec");

		long startTime = System.currentTimeMillis();

		if (this.TEST) {
			// System.err.println("Pairwise connectivity graph contains " +
			// stmtPairs.size() + " arcs");
			System.err.println("Propagation graph contains " + this.pgarcs.size() + " bidirectional arcs and "
					+ this.pgnodes.size() + " nodes");
			if (this.DEBUG) {
				// System.err.println("============ Arcs: ==============");
				// dump(pgarcs);
				// System.err.println("============ Nodes: =============");
				// dump(pgnodes.values());
				System.err.println("EQUAL_PRED_COEFF = " + this.EQUAL_PRED_COEFF + "\n" + "OTHER_PRED_COEFF = "
						+ this.OTHER_PRED_COEFF + "\n" + "TRY_ALL_ARCS = " + this.TRY_ALL_ARCS);
			}
		}

		// create arrays for efficiency

		PGNode[] nodes;
		PGArc[] arcs;

		arcs = new PGArc[this.pgarcs.size()];
		this.pgarcs.toArray(arcs);
		this.pgarcs = null; // free memory

		nodes = new PGNode[this.pgnodes.size()];
		this.pgnodes.values().toArray(nodes);
		this.pgnodes = null; // free memory

		if (this.TEST)
			System.err.print("Iterating over (" + mm1.size() + " arcs, " + RDFUtil.getNodes(mm1).size() + " nodes) x ("
					+ mm2.size() + " arcs, " + RDFUtil.getNodes(mm2).size() + " nodes): ");

		int iterationNum = this.MAX_ITERATION_NUM;

		// initialize sigmaN1 := sigma0;
		for (int i = 0; i < nodes.length; i++) {
			// nodes[i].simN1 = rnd.nextDouble();
			// nodes[i].sim0 /= 1000;
			nodes[i].simN1 = nodes[i].sim0;
		}
		normalizeN1(nodes);

		for (int iteration = 0; iteration < iterationNum; iteration++) {

			if (this.DEBUG && iteration < DEBUG_MAX_ITERATIONS) {
				System.err.println("\nIteration: " + iteration);
				// debugMap(sigmaN);
			}

			applyFormula(arcs, nodes, iteration);

			System.err.print(".");

			normalizeN1(nodes);

			if (this.DEBUG && iteration < DEBUG_MAX_ITERATIONS) {
				System.err.println("\nAfter norm: " + iteration);
				dump(Arrays.asList(nodes));
			}

			double diff = distance(nodes);

			// double maxN1 = maxN1(nodes);
			// double diff = distanceF(nodes, maxN, maxN1);
			// maxN = maxN1;

			if (this.TEST) {
				if (this.DEBUG && iteration < DEBUG_MAX_ITERATIONS)
					System.err.println("------------------");
				System.err.print("(" + iteration + ":" + diff + ")");
			}

			if (iteration >= this.MIN_ITERATION_NUM && diff <= this.RESIDUAL_VECTOR_LENGTH)
				break; // we are done!
			if (System.currentTimeMillis() - startTime > this.TIMEOUT)
				break;

			// copy sigmaN+1 into sigmaN and repeat: done at top of loop
		}
		// RETURN

		if (this.TEST)
			System.err.println(". Time: " + ((double) (System.currentTimeMillis() - startTime) / 1000) + " sec");

		// copy result into sim
		for (int i = 0; i < nodes.length; i++)
			nodes[i].sim_ = nodes[i].simN1; // / maxN;

		return nodes;
	}

	public PGNode[] getMatch(Model mm1, Model mn2, List<MapPair> sigma0, Map<URI, URI> constraints)
			throws ModelException {

		this.m1 = mm1;
		this.m2 = mn2;
		// this.sigma0 = rm;

		// computes sigma0 if null; must go before constructPropagationGraph
		if (sigma0 == null)
			initSigma0();
		else {
			// set initial values from sigma0
			for (Iterator<MapPair> it = sigma0.iterator(); it.hasNext();) {

				MapPair p = it.next();
				PGNode n = this.pgnodes.get(p);
				if (n == null) {
					n = new PGNode(p.getLeft(), p.getRight());
					this.pgnodes.put(n, n);
				}
				n.sim0 = (p.sim_ == MapPair.NULL_SIM ? 1 : p.sim_);
				n.inverse_ = p.inverse_; // make sure chosen direction remains
				// unchanged
			}
		}

		// must go AFTER cardMaps
		boolean ignorePredicates = this.TRY_ALL_ARCS; // || (FLOW_GRAPH_TYPE
														// ==
		// FG_STOCHASTIC);

		long pgstart = System.currentTimeMillis();
//		System.err.print("Creating propagation graph: ");
		constructPropagationGraph(ignorePredicates);
		long pgend = System.currentTimeMillis();
		System.err.println("" + (double) (pgend - pgstart) / 1000 + " sec");

		long startTime = System.currentTimeMillis();

		if (this.TEST) {
			// System.err.println("Pairwise connectivity graph contains " +
			// stmtPairs.size() + " arcs");
			System.err.println("Propagation graph contains " + this.pgarcs.size() + " bidirectional arcs and "
					+ this.pgnodes.size() + " nodes");
			if (this.DEBUG) {
				// System.err.println("============ Arcs: ==============");
				// dump(pgarcs);
				// System.err.println("============ Nodes: =============");
				// dump(pgnodes.values());
				System.err.println("EQUAL_PRED_COEFF = " + this.EQUAL_PRED_COEFF + "\n" + "OTHER_PRED_COEFF = "
						+ this.OTHER_PRED_COEFF + "\n" + "TRY_ALL_ARCS = " + this.TRY_ALL_ARCS);
			}
		}

		// create arrays for efficiency

		PGNode[] nodes;
		PGArc[] arcs;

		arcs = new PGArc[this.pgarcs.size()];
		this.pgarcs.toArray(arcs);
		this.pgarcs = null; // free memory

		nodes = new PGNode[this.pgnodes.size()];
		this.pgnodes.values().toArray(nodes);
		this.pgnodes = null; // free memory

		if (this.TEST)
			System.err.print("Iterating over (" + mm1.size() + " arcs, " + RDFUtil.getNodes(mm1).size() + " nodes) x ("
					+ mn2.size() + " arcs, " + RDFUtil.getNodes(mn2).size() + " nodes): ");

		int iterationNum = this.MAX_ITERATION_NUM;

		// initialize sigmaN1 := sigma0;
		for (int i = 0; i < nodes.length; i++) {
			// nodes[i].simN1 = rnd.nextDouble();
			// nodes[i].sim0 /= 1000;
			nodes[i].simN1 = nodes[i].sim0;
//			if( nodes[i].getLeftNode().getLabel().contains("dsqdbz") )
//				System.out.println("dsqdbz<->" + nodes[i].getRightNode().getLabel() + "(" + nodes[i].simN1 + ")");
		}
		normalizeN1(nodes);

		for (int iteration = 0; iteration < iterationNum; iteration++) {

			if (this.DEBUG && iteration < DEBUG_MAX_ITERATIONS) {
				System.err.println("\nIteration: " + iteration);
				// debugMap(sigmaN);
			}

			applyFormula(arcs, nodes, iteration);

			// by liyi
			/*for (int i = 0; i < nodes.length; i++) {
				PGNode node = nodes[i];
				Iterator<Entry<URI,URI>> iterEntry = constraints.entrySet().iterator();
				while (iterEntry.hasNext()) {
					Entry<URI, URI> entry = iterEntry.next();
					if (node.getLeftNode().getLabel().equals(entry.getKey().toString())
							&& !node.getRightNode().getLabel().equals(entry.getValue().toString())) {
						node.simN1 = 0;
					}
					if (!node.getLeftNode().getLabel().equals(entry.getKey().toString())
							&& node.getRightNode().getLabel().equals(entry.getValue().toString())) {
						node.simN1 = 0;
					}
				}
			}*/

			System.err.print(".");

			normalizeN1(nodes);

			// by liyi
			/*for (int i = 0; i < nodes.length; i++) {
				PGNode node = nodes[i];
				Iterator<Entry<URI, URI>> iterEntry = constraints.entrySet().iterator();
				while (iterEntry.hasNext()) {
					Entry<URI, URI> entry = iterEntry.next();
					if (node.getLeftNode().getLabel().equals(entry.getKey().toString())
							&& node.getRightNode().getLabel().equals(entry.getValue().toString())) {
						node.simN1 = 1;
					}
				}
//				if( node.getLeftNode().getLabel().contains("dsqdbz") && node.sim > 0)
//					System.out.println("dsqdbz<->" + node.getRightNode().getLabel() + "(" + node.simN1 + ")");
			}*/

			if (this.DEBUG && iteration < DEBUG_MAX_ITERATIONS) {
				System.err.println("\nAfter norm: " + iteration);
				dump(Arrays.asList(nodes));
			}

			double diff = distance(nodes);

			// double maxN1 = maxN1(nodes);
			// double diff = distanceF(nodes, maxN, maxN1);
			// maxN = maxN1;

			if (this.TEST) {
				if (this.DEBUG && iteration < DEBUG_MAX_ITERATIONS)
					System.err.println("------------------");
				System.err.print("(" + iteration + ":" + diff + ")");
			}

			if (iteration >= this.MIN_ITERATION_NUM && diff <= this.RESIDUAL_VECTOR_LENGTH)
				break; // we are done!
			if (System.currentTimeMillis() - startTime > this.TIMEOUT)
				break;

			// copy sigmaN+1 into sigmaN and repeat: done at top of loop
		}
		// RETURN

		if (this.TEST)
			System.err.println(". Time: " + ((double) (System.currentTimeMillis() - startTime) / 1000) + " sec");

		// copy result into sim
		for (int i = 0; i < nodes.length; i++)
			nodes[i].sim_ = nodes[i].simN1; // / maxN;

		return nodes;
	}

	public void applyFormula(PGArc[] arcs, PGNode[] nodes, int iteration) {

		// special case for default formula

		if (this.formula == FORMULA_TFT) {

			for (int i = nodes.length; --i >= 0;) {

				PGNode n = nodes[i];
				n.sim_ = (n.simN = n.simN1) + n.sim0;
			}
			propagateValues(arcs);
			return;
		}

		// generic, for all formulas

		boolean add_sigma0_before = this.formula[0];
		boolean add_sigma0_after = this.formula[1];
		boolean add_sigmaN_after = this.formula[2];

		for (int i = nodes.length; --i >= 0;) {

			PGNode n = nodes[i];
			// move simN1 values in simN and take current value from previous
			// iteration
			n.sim_ = n.simN = n.simN1;

			if (add_sigma0_before)
				n.sim_ += n.sim0;

			// initialize simN1 for next iteration
			if (!add_sigmaN_after)
				n.simN1 = 0; // otherwise, n.simN1 = n.sim from above

			if (add_sigma0_after)
				n.simN1 += n.sim0;
		}

		// if(DEBUG && iteration < DEBUG_MAX_ITERATIONS) {
		// System.err.println("\nBefore propagation: " + iteration);
		// dump(nodes);
		// }

		propagateValues(arcs);

		if (this.DEBUG && iteration < DEBUG_MAX_ITERATIONS) {
			System.err.println("\nAfter propagation: " + iteration);
			dump(nodes);
		}

		/*
		 * if(add_sigma0_after || add_sigmaN_after) {
		 * 
		 * for(int i = nodes.length; --i >= 0;) {
		 * 
		 * PGNode n = nodes[i];
		 * 
		 * if(add_sigma0_after) n.simN1 += n.sim0;
		 * 
		 * if(add_sigmaN_after) n.simN1 += n.sim; } }
		 */
	}

	public static void propagateValues(PGArc[] arcs) {

		// propagate values from previous iteration over propagation graph

		for (int i = arcs.length; --i >= 0;) {

			PGArc arc = arcs[i];
			
//			try {
//				if (arc.dest.getLeftNode().getLabel().contains("dsqdbz") && arc.src.sim > 0)
//					System.out.println("dsqdbz<->" + arc.dest.getRightNode().getLabel() +"(" + arc.dest.simN1 + ")" + " get " + arc.src.sim + " from " + arc.src.getLeftNode().getLabel() + "<->"
//							+ arc.src.getRightNode().getLabel());
//				if (arc.src.getLeftNode().getLabel().contains("dsqdbz") && arc.dest.sim > 0)
//					System.out.println("dsqdbz<->" + arc.src.getRightNode().getLabel() +"(" + arc.src.simN1 + ")" + " get " + arc.dest.sim + " from " + arc.dest.getLeftNode().getLabel() + "<->"
//							+ arc.dest.getRightNode().getLabel());
//			} catch (ModelException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
			// forward
			arc.dest.simN1 += arc.src.sim_ * arc.fw;
			// backward
			arc.src.simN1 += arc.dest.sim_ * arc.bw;
			

		}
	}

	public double distance(PGNode[] nodes) {

		double diff = 0.0;

		for (int i = nodes.length; --i >= 0;) {
			double d = nodes[i].simN1 - nodes[i].simN;
			diff += d * d;
		}

		return Math.sqrt(diff);
	}

	public double distanceF(PGNode[] nodes, double maxN, double maxN1) {

		if (this.DEBUG)
			System.err.println("Calc distance with maxN=" + maxN + ", maxN1=" + maxN1);

		double diff = 0.0;

		for (int i = 0; i < nodes.length; i++) {
			double d = nodes[i].simN1 / maxN1 - nodes[i].simN / maxN;
			diff += d * d;
		}

		return Math.sqrt(diff);
	}

	static double minN(PGNode[] nodes) {

		double min = 0;
		for (int i = 0; i < nodes.length; i++)
			if (nodes[i].simN > 0)
				min = Math.min(min, nodes[i].simN);
		return min;
	}

	static double maxN1(PGNode[] nodes) {

		double max = 0;
		for (int i = 0; i < nodes.length; i++)
			max = Math.max(max, nodes[i].simN1);
		return max;
	}

	static double sumN1(PGNode[] nodes) {

		double sum = 0;
		for (int i = 0; i < nodes.length; i++)
			sum += nodes[i].simN1;
		return sum;
	}

	static double sumN(PGNode[] nodes) {

		double sum = 0;
		for (int i = 0; i < nodes.length; i++)
			sum += nodes[i].simN;
		return sum;
	}

	static void normalizeN1(PGNode[] nodes) {

		double max = maxN1(nodes);
		if (max == 0)
			return;
		for (int i = 0; i < nodes.length; i++)
			nodes[i].simN1 /= max;
	}

	public static void dump(Collection<? extends Object> c) {

		for (Iterator<? extends Object> it = c.iterator(); it.hasNext();)
			System.err.println(String.valueOf(it.next()));
	}

	public static void dump(Object[] arr) {

		dump(Arrays.asList(arr));
	}

	PGNode getNormalNode(RDFNode n1, RDFNode n2) {

		double sim = n1.equals(n2) ? 1.0 : MIN_NODE_SIM2;

		boolean isL1 = n1 instanceof Literal;
		boolean isL2 = n2 instanceof Literal;

		if (isL1 != isL2)
			sim *= StringMatcher.RESOURCE_LITERAL_MATCH_PENALTY;

		PGNode node = new PGNode(n1, n2);
		node.sim0 = sim;
		return node;
	}

	// precompute pairs of statements to consider
	// also computes sigma0 for the cross-product of all nodes and literals if
	// needed
	// computes a mapping, in principle

	@SuppressWarnings("unchecked")
	void initSigma0() throws ModelException {

		System.err.println("All nodes are considered equally similar");
		// sigma0 = new HashSet();
		Collection<RDFNode> c2 = RDFUtil.getNodes(m2).values();
		Iterator<RDFNode> it1 = RDFUtil.getNodes(m1).values().iterator();
		while (it1.hasNext()) {
			RDFNode n1 = it1.next();
			Iterator<RDFNode> it2 = c2.iterator();
			while (it2.hasNext()) {
				RDFNode n2 = it2.next();
				// MapPair p = getNormalPair(n1, n2);
				// System.err.println("Reinforce pair: " + p);
				// sigma0.add(p);
				PGNode pn = getNormalNode(n1, n2);
				// System.err.println("Init node: " + pn);
				this.pgnodes.put(pn, pn);
			}
		}
	}

	@SuppressWarnings("unchecked")
	void constructPropagationGraph(boolean ignorePredicates) throws ModelException {

		// SP -> count
		Map<MapPair, MapPair> cardMapSPLeft, cardMapOPLeft, cardMapPLeft, cardMapSPRight, cardMapOPRight, cardMapPRight;

		cardMapSPLeft = new HashMap<MapPair, MapPair>();
		cardMapOPLeft = new HashMap<MapPair, MapPair>();
		cardMapPLeft = new HashMap<MapPair, MapPair>();

		cardMapSPRight = new HashMap<MapPair, MapPair>();
		cardMapOPRight = new HashMap<MapPair, MapPair>();
		cardMapPRight = new HashMap<MapPair, MapPair>();

		computeCardMaps(this.m1, cardMapSPLeft, cardMapOPLeft, cardMapPLeft, ignorePredicates);
		computeCardMaps(this.m2, cardMapSPRight, cardMapOPRight, cardMapPRight, ignorePredicates);

		Map<MapPair, MapPair> outgoing = new HashMap<MapPair, MapPair>(); // same
		// as
		// incoming
		// Map incoming = new HashMap();

		List<StmtPair> stmtPairs = new ArrayList<StmtPair>();

		for (Enumeration<Statement> en1 = this.m1.elements(); en1.hasMoreElements();) {

			Statement st1 = en1.nextElement();

			if (st1.subject() instanceof Statement || st1.object() instanceof Statement)
				continue;

			for (Enumeration<Statement> en2 = this.m2.elements(); en2.hasMoreElements();) {

				Statement st2 = en2.nextElement();

				if (st2.subject() instanceof Statement || st2.object() instanceof Statement)
					continue;

				// if(tryAll) {
				// sigma0.add(getNormalPair(st1.subject(), st2.subject()));
				// sigma0.add(getNormalPair(st1.object(), st2.object()));
				// sigma0.add(getNormalPair(st1.subject(), st2.object()));
				// sigma0.add(getNormalPair(st1.object(), st2.subject()));
				// }

				double ps = 0.0; // predicateSim(st1.predicate(),
				// st2.predicate());
				// System.err.println("-- " + st1 + " -- " + st2);
				if (st1.predicate().equals(st2.predicate()))
					ps = this.EQUAL_PRED_COEFF;
				else if (this.TRY_ALL_ARCS)
					ps = this.OTHER_PRED_COEFF;

				if (ps > 0) {
					// System.err.println("--- ps=" + ps + " from " +
					// EQUAL_PRED_COEFF + ", " + TRY_ALL_ARCS + ", " +
					// OTHER_PRED_COEFF);
					StmtPair p = new StmtPair(st1, st2, ps, getCard(cardMapSPLeft, st1.subject(),
							ignorePredicates ? null : st1.predicate()), getCard(cardMapOPLeft, st1.object(),
							ignorePredicates ? null : st1.predicate()), getCard(cardMapPLeft, null,
							ignorePredicates ? null : st1.predicate()), getCard(cardMapSPRight, st2.subject(),
							ignorePredicates ? null : st2.predicate()), getCard(cardMapOPRight, st2.object(),
							ignorePredicates ? null : st2.predicate()), getCard(cardMapPRight, null,
							ignorePredicates ? null : st2.predicate()));

					// MapPair p = new MapPair(st1, st2, ps);

					if (this.FLOW_GRAPH_TYPE == FG_STOCHASTIC || this.FLOW_GRAPH_TYPE == FG_INCOMING) {

						// collect the numbers

						MapPair sourcePair = get(outgoing, st1.subject(), st2.subject());
						sourcePair.sim_ += 1.0;

						sourcePair = get(outgoing, st1.object(), st2.object());
						sourcePair.sim_ += 1.0;

						if (this.TRY_ALL_ARCS) {
							sourcePair = get(outgoing, st1.subject(), st2.object());
							sourcePair.sim_ += 1.0;

							sourcePair = get(outgoing, st1.object(), st2.subject());
							sourcePair.sim_ += 1.0;
						}

						// MapPair targetPair = get(incoming, st1.object(),
						// st2.object());
						// targetPair.sim += 1.0;
					}

					if (this.DEBUG)
						System.err.println("" + p);
					stmtPairs.add(p);
				}
			}
		}

		if (this.FLOW_GRAPH_TYPE == FG_STOCHASTIC) {

			Iterator<StmtPair> it = stmtPairs.iterator();
			while (it.hasNext()) {

				StmtPair p = it.next();
				p.soso = 1.0 / get(outgoing, p.stLeft.subject(), p.stRight.subject()).sim_;
				p.osos = 1.0 / get(outgoing, p.stLeft.object(), p.stRight.object()).sim_;
				if (this.TRY_ALL_ARCS) {
					p.soos = 1.0 / get(outgoing, p.stLeft.subject(), p.stRight.object()).sim_;
					p.osso = 1.0 / get(outgoing, p.stLeft.object(), p.stRight.subject()).sim_;
				}
				if (this.DEBUG)
					System.err.println("Adjusted: " + p);
			}
		} else if (this.FLOW_GRAPH_TYPE == FG_INCOMING) {

			Iterator<StmtPair> it = stmtPairs.iterator();
			while (it.hasNext()) {

				StmtPair p = it.next();
				p.osos = 1.0 / get(outgoing, p.stLeft.subject(), p.stRight.subject()).sim_;
				p.soso = 1.0 / get(outgoing, p.stLeft.object(), p.stRight.object()).sim_;
				if (this.TRY_ALL_ARCS) {
					p.osso = 1.0 / get(outgoing, p.stLeft.subject(), p.stRight.object()).sim_;
					p.soos = 1.0 / get(outgoing, p.stLeft.object(), p.stRight.subject()).sim_;
				}
				if (this.DEBUG)
					System.err.println("Adjusted: " + p);
			}
		}

		// we don't need cardMaps any more, free memory
		cardMapSPLeft = cardMapOPLeft = cardMapSPRight = cardMapOPRight = cardMapPLeft = cardMapPRight = null;

		// pgnodes = new HashMap();
		// pgarcs = new ArrayList();

		for (Iterator<StmtPair> it = stmtPairs.iterator(); it.hasNext();) {

			StmtPair p = it.next();
			Statement st1 = p.stLeft;
			Statement st2 = p.stRight;

			PGNode ss = getNode(this.pgnodes, st1.subject(), st2.subject());
			PGNode oo = getNode(this.pgnodes, st1.object(), st2.object());

			this.pgarcs.add(new PGArc(ss, oo, p.soso * UPDATE_GUESS_WEIGHT, p.osos * UPDATE_GUESS_WEIGHT));

			if (!this.DIRECTED_GRAPH) {

				PGNode so = getNode(this.pgnodes, st1.subject(), st2.object());
				PGNode os = getNode(this.pgnodes, st1.object(), st2.subject());

				pgarcs.add(new PGArc(so, os, p.soos * UPDATE_GUESS_WEIGHT, p.osso * UPDATE_GUESS_WEIGHT));
			}
		}
	}

	PGNode getNode(Map<PGNode, PGNode> table, RDFNode r1, RDFNode r2) {

		PGNode p = new PGNode(r1, r2);
		PGNode res = table.get(p);
		if (res == null) {
			table.put(p, p);
			return p;
		}
		return res;
	}

	int getCard(Map<MapPair, MapPair> cardMap, RDFNode r, Resource pred) {

		MapPair p = get(cardMap, r, pred);
		// there MUST be a pair after computeCardMaps!!!
		return (int) p.sim_;
	}

	// collects the number of nodes going out of a node

	@SuppressWarnings("unchecked")
	void computeCardMaps(Model m, Map<MapPair, MapPair> cardMapSP, Map<MapPair, MapPair> cardMapOP,
			Map<MapPair, MapPair> cardMapP, boolean ignorePredicates) throws ModelException {

		for (Enumeration<Statement> en = m.elements(); en.hasMoreElements();) {

			Statement st = en.nextElement();

			if (st.subject() instanceof Statement || st.object() instanceof Statement)
				continue;

			Resource pred = ignorePredicates ? null : st.predicate();
			MapPair p = get(cardMapSP, st.subject(), pred);
			p.sim_ += 1.0;

			p = get(cardMapOP, st.object(), pred);
			p.sim_ += 1.0;

			p = get(cardMapP, null, pred);
			p.sim_ += 1.0;
		}
	}

	MapPair get(Map<MapPair, MapPair> table, RDFNode r1, RDFNode r2) {

		MapPair p = setPair(GET_PAIR, r1, r2); // new MapPair(r1, r2); //
		// MapPair p = new MapPair(r1, r2);
		MapPair res = table.get(p);
		if (res == null) {
			res = p.duplicate();
			table.put(res, res);
		}
		return res;
	}

	// this method is used to avoid creating of new objects
	MapPair setPair(int id, RDFNode r1, RDFNode r2) {

		MapPair p = this.cachePairs[id];
		p.setLeft(r1);
		p.setRight(r2);
		// p.hash = 0;
		return p;
	}

	public int getMinInputLen() {
		return 2;
	}

	public int getMaxInputLen() {
		return 3;
	}

	public int getMinOutputLen() {
		return 1;
	}

	public int getMaxOutputLen() {
		return 1;
	}

	/**
	 * A node in the propagation graph
	 */
	class PGNode extends MapPair {

		double sim0;

		// double sim; corresponds to simN, defined in MapPair
		double simN1; // N+1

		double simN; // for comparing vectors, storage only

		public PGNode(RDFNode r1, RDFNode r2) {

			super(r1, r2);
		}

		@Override
		public String toString() {

			return "[" + getLeft() + "," + getRight() + ": sim=" + this.sim_ + ", init=" + this.sim0 + ", N="
					+ this.simN + ", N1=" + this.simN1 + (this.inverse_ ? ", inverse" : "") + "]";
		}
	}

	/**
	 * An arc of the propagation graph
	 */
	class PGArc {

		double fw, bw; // coefficients on arcs

		PGNode src, dest;

		public PGArc(PGNode n1, PGNode n2, double fw, double bw) {

			this.src = n1;
			this.dest = n2;
			this.fw = fw;
			this.bw = bw;
		}

		@Override
		public String toString() {

			return this.src + " <--" + this.bw + " " + this.fw + "--> " + this.dest;
		}
	}

	/**
	 * Instances of this class are used temporarily for creating the propagation
	 * graph
	 */
	class StmtPair {

		Statement stLeft, stRight;

		double predSim;

		// int spLeft,opLeft,spRight,opRight;
		double soso, osos, soos, osso;

		public StmtPair(Statement stLeft, Statement stRight, double predSim, int spLeft, int opLeft, int pLeft,
				int spRight, int opRight, int pRight) {

			// System.err.println("--- predSim=" + predSim);

			this.stLeft = stLeft;
			this.stRight = stRight;
			this.predSim = predSim;

			switch (Match.this.FLOW_GRAPH_TYPE) {

			case Match.FG_AVG: {

				double c = 2.0;
				this.soso = c * predSim / (spLeft + spRight);
				this.osos = c * predSim / (opLeft + opRight);
				this.soos = c * predSim / (spLeft + opRight);
				this.osso = c * predSim / (opLeft + spRight);
				// System.err.println("--- soso=" + soso + " from predSim="
				// +
				// predSim + ", spLeft="+ spLeft + ", spRight=" + spRight);
				break;
			}
			case Match.FG_PRODUCT: {

				double c = 1.0;
				this.soso = c * predSim / (spLeft * spRight);
				this.osos = c * predSim / (opLeft * opRight);
				this.soos = c * predSim / (spLeft * opRight);
				this.osso = c * predSim / (opLeft * spRight);
				break;
			}

				/*
				 * case Match.FG_CONSTANT_WEIGHT: { // ignore directionality
				 * 
				 * double c = 1.0 / ((spLeft * spRight) + (opLeft * opRight));
				 * this.soso = c * predSim; this.osos = c * predSim; this.soos =
				 * c * predSim; this.osso = c * predSim; break; }
				 */
			case Match.FG_EQUAL:
			case Match.FG_INCOMING:
			case Match.FG_STOCHASTIC: { // for constant weight, weight
				// computed
				// here does not matter...

				double c = 1.0;
				this.soso = c * predSim;
				this.osos = c * predSim;
				this.soos = c * predSim;
				this.osso = c * predSim;
				break;
			}
			case Match.FG_TOTALP: {

				double c = pLeft * pRight;
				this.soso = predSim / c;
				this.osos = predSim / c;
				this.soos = predSim / c;
				this.osso = predSim / c;
				break;
			}
			case Match.FG_TOTALS: {

				double c = 2.0 / (pLeft + pRight);
				this.soso = predSim * c;
				this.osos = predSim * c;
				this.soos = predSim * c;
				this.osso = predSim * c;
				break;
			}

			case Match.FG_AVG_TOTALS: {

				double c = 4.0 / (pLeft + pRight);
				this.soso = c * predSim / (spLeft + spRight);
				this.osos = c * predSim / (opLeft + opRight);
				this.soos = c * predSim / (spLeft + opRight);
				this.osso = c * predSim / (opLeft + spRight);
				break;
			}

			}
			// try {
			// // By liyi
			// if
			// (this.stLeft.predicate().getLabel().equals(StructureLearner.SF_HasProperty))
			// {
			// this.osos *= 0.5;
			// this.osso *= 0.5;
			// this.soos *= 0.5;
			// this.soso *= 0.5;
			// }
			// } catch (ModelException e) {
			//				
			// e.printStackTrace();
			// }
		}

		@Override
		public String toString() {

			try {
				return "StmtPair[("
						+ this.stLeft.subject()
						+ ","
						+ this.stRight.subject()
						+ ") -> ("
						+ this.stLeft.object()
						+ ","
						+ this.stRight.object()
						+ "), "
						+ this.soso
						+ "->, <-"
						+ this.osos
						+ (Match.this.TRY_ALL_ARCS ? " *** (" + this.stLeft.subject() + "," + this.stRight.object()
								+ ") -> (" + this.stLeft.object() + "," + this.stRight.subject() + "), " + this.soos
								+ "->, <-" + this.osso + ")" : "");
			} catch (ModelException any) {
				return "StmtPair[" + this.stLeft + "," + this.stRight + "," + this.predSim + "," + this.soso + ","
						+ this.osos + "," + this.soos + "," + this.osso;
			}
		}
	}

	static void ICDE02Example() throws Exception {

		System.err.println("\nThis is a simple example used in the ICDE'02 paper.");

		RDFFactory rf = new RDFFactoryImpl();
		NodeFactory nf = rf.getNodeFactory();

		// Create two tiny sample graphs used in the ICDE'02 paper.

		// create graph/model A
		Model A = rf.createModel();

		Resource a = nf.createResource("a");
		Resource a1 = nf.createResource("a1");
		Resource a2 = nf.createResource("a2");
		Resource l1 = nf.createResource("l1");
		Resource l2 = nf.createResource("l2");

		A.add(nf.createStatement(a, l1, a1));
		A.add(nf.createStatement(a, l1, a2));
		A.add(nf.createStatement(a1, l2, a2));

		// create graph/model B
		Model B = rf.createModel();

		Resource b = nf.createResource("b");
		Resource b1 = nf.createResource("b1");
		Resource b2 = nf.createResource("b2");

		B.add(nf.createStatement(b, l1, b1));
		B.add(nf.createStatement(b, l2, b2));
		B.add(nf.createStatement(b2, l2, b1));

		// create an initial mapping which is just a cross-product with 1's as
		// weights
		List<MapPair> initMap = new ArrayList<MapPair>();
		initMap.add(new MapPair(a, b, 1.0));
		initMap.add(new MapPair(a, b1, 1.0));
		initMap.add(new MapPair(a, b2, 1.0));
		initMap.add(new MapPair(a1, b, 1.0));
		initMap.add(new MapPair(a1, b1, 1.0, true)); // test inverse
		initMap.add(new MapPair(a1, b2, 1.0));
		initMap.add(new MapPair(a2, b, 1.0));
		initMap.add(new MapPair(a2, b1, 1.0));
		initMap.add(new MapPair(a2, b2, 1.0));

		Match sf = new Match();

		// Two lines below are used to get the same setting as in the example of
		// the ICDE'02 paper.
		// (In general, this formula won't converge! So better stick to the
		// default values instead)
		sf.formula = FORMULA_FFT;
		sf.FLOW_GRAPH_TYPE = FG_PRODUCT;

		MapPair[] result = sf.getMatch(A, B, initMap);
		MapPair.sort(result);
		dump(result);
		// MapPair.printMap(Arrays.asList(result), System.out);
	}

	/**
	 * Matches two lists of given length assuming that initially all nodes are
	 * equally similar. Long lists (> 200 nodes) are not matched accurately due
	 * to insufficient numerical precision.
	 */
	static void orderedNodesExample(int num) throws Exception {

		System.err.println("\nThis is an example of matching two lists. See the source code for details.");

		RDFFactory rf = new RDFFactoryImpl();
		NodeFactory nf = rf.getNodeFactory();

		// Create two tiny sample graphs

		Resource[] a = new Resource[num];
		Resource[] b = new Resource[num];
		Resource NEXT = nf.createResource("next");

		for (int i = 0; i < num; i++) {
			a[i] = nf.createResource("a" + (i + 1));
			b[i] = nf.createResource("b" + (i + 1));
		}

		// create graphs A and B
		Model A = rf.createModel();
		Model B = rf.createModel();

		// connect lists in A and B
		for (int i = 1; i < num; i++) {
			A.add(nf.createStatement(a[i - 1], NEXT, a[i]));
			B.add(nf.createStatement(b[i - 1], NEXT, b[i]));
		}

		Match sf = new Match();
		PGNode[] result = sf.getMatch(A, B, null); // initial mapping is a full
		// cross-product of nodes in
		// A and B
		MapPair.printMap(new FilterBest().getFilterBest(Arrays.asList(result), true), System.out);
	}

	static void addSequence(Model m, NodeFactory f, String str, String prefix) throws ModelException {

		Resource prev = f.createResource(prefix + "0");

		for (int i = 0; i < str.length(); i++) {

			Resource next = f.createResource(prefix + i);
			Resource arc = f.createResource(String.valueOf(str.charAt(i)));
			m.add(f.createStatement(prev, arc, next));
			prev = next;
		}
	}

	/**
	 * Matches two lists of given length assuming that initially all nodes are
	 * equally similar. Long lists (> 200 nodes) are not matched accurately due
	 * to insufficient numerical precision.
	 */
	static void sequenceExample(String seq1, String seq2) throws Exception {

		System.err
				.println("\nThis is an example of matching two gene/protein sequences. See the source code for details.");

		RDFFactory rf = new RDFFactoryImpl();
		NodeFactory nf = rf.getNodeFactory();

		System.err.println("Matching sequences " + seq1 + " and " + seq2);

		// create graphs A and B
		Model A = rf.createModel();
		Model B = rf.createModel();

		addSequence(A, nf, seq1, "x");
		addSequence(B, nf, seq2, "y");

		Match sf = new Match();
		PGNode[] result = sf.getMatch(A, B, null); // initial mapping is a full
		// cross-product of nodes in
		// A and B

		List<MapPair> pruned = new FilterBest().getFilterBest(Arrays.asList(result), true);
		MapPair[] f = new MapPair[0];
		f = pruned.toArray(f);
		MapPair.sortGroup(f, false);
		dump(f);
	}

	public static void main(String[] args) throws Exception {

		switch (args.length) {

		case 0:

			ICDE02Example();
			orderedNodesExample(10);
			sequenceExample("GATTACA", "GTAACATCAGAGATTTTGAGACAC");
			break;

		case 1:

			orderedNodesExample(Integer.parseInt(args[0]));
			break;

		case 2:
			sequenceExample(args[0], args[1]);
			break;
		}
	}
}
