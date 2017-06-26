package com.interdataworking.mm.alg;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.w3c.rdf.model.Literal;
import org.w3c.rdf.model.Model;
import org.w3c.rdf.model.ModelException;
import org.w3c.rdf.model.NodeFactory;
import org.w3c.rdf.model.RDFNode;
import org.w3c.rdf.model.Resource;
import org.w3c.rdf.model.Statement;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.RDF;

import com.interdataworking.mm.MapVocabulary;

public class MapPair implements Cloneable {

	public static final double PRECISION = 0.0000001;

	public static final double NULL_SIM = Double.MIN_VALUE;

	RDFNode r1; // read only! setting done only in Match.java

	RDFNode r2;

	public double sim_ = NULL_SIM;

	public boolean inverse_; // used by Merge and GUIs only

	public MapPair(RDFNode r1, RDFNode r2) {

		this(r1, r2, 0, false);
	}

	public MapPair(RDFNode r1, RDFNode r2, double sim) {

		this(r1, r2, sim, false);
	}

	public MapPair(RDFNode r1, RDFNode r2, double sim, boolean inverse) {

		this.r1 = r1;
		this.r2 = r2;
		this.sim_ = sim;
		this.inverse_ = inverse;
	}

	/**
	 * Use for containers only
	 */
	public MapPair() {
		//Empty
	}

	public RDFNode getLeftNode() {

		return r1;
	}

	public RDFNode getRightNode() {

		return r2;
	}

	public void setLeft(RDFNode obj) {

		r1 = obj;
	}

	public void setRight(RDFNode obj) {

		r2 = obj;
	}

	public RDFNode getLeft() {

		return r1;
	}

	public RDFNode getRight() {

		return r2;
	}

	// no performance gain
	@Override
	public int hashCode() {

		int h1 = r1 != null ? r1.hashCode() : 0;
		int h2 = r2 != null ? r2.hashCode() : 0;

		return h1 * 7 + h2;
	}

	@Override
	public boolean equals(Object that) {

		if (this == that)
			return true;
		if (!(that instanceof MapPair))
			return false;
		MapPair p = (MapPair) that;
		return ((r1 == null && p.r1 == null) || r1.equals(p.r1)) && ((r2 == null && p.r2 == null) || r2.equals(p.r2));
	}

	public MapPair duplicate() {

		MapPair p = new MapPair(r1, r2, sim_, inverse_);
		// p.hash = hash;
		return p;
	}

	public MapPair duplicate(double sim) {

		MapPair p = new MapPair(r1, r2, sim, inverse_);
		// p.hash = hash;
		return p;
	}

	@Override
	public Object clone() {

		MapPair r = duplicate();
		// r.hash = hash;
		return r;
	}

	@Override
	public String toString() {

		return "MapPair[" + sim_ + ": " + r1 + "; " + r2 + "]";
	}

	/**
	 * returns map l -> sorted list of pairs, or r -> sorted list of pairs
	 */
	public static Map<Object, List<MapPair>> sortedCandidates(MapPair[] arr, boolean isRight) {

		Map<Object, List<MapPair>> result = new HashMap<Object, List<MapPair>>();

		sortGroup(arr, isRight);
		// if !isRight, sorted by left elements
		// collect lists

		// pivot -> currList
		Object pivot = null;
		List<MapPair> currList = null;

		for (int i = 0; i < arr.length; i++) {

			MapPair currPair = arr[i];
			Object currLeft = isRight ? currPair.r2 : currPair.r1;

			boolean createNew = false;

			if (i == 0) {
				pivot = currLeft;
				createNew = true;

			} else {

				MapPair prevPair = arr[i - 1];

				Object prevLeft = pivot = isRight ? prevPair.r2 : prevPair.r1;

				if (!currLeft.equals(prevLeft))
					createNew = true;
			}

			if (createNew) {
				// save previous if any
				if (currList != null) {
					result.put(pivot, currList);
				}
				currList = new ArrayList<MapPair>();
			}
			if( currList == null )
				currList = new ArrayList<MapPair>();
			currList.add(currPair);
		}

		if (currList != null && currList.size() > 0) {
			result.put(pivot, currList);
		}

		return result;
	}

	/** sort an array of map pairs, first by r1, then by similarity. */
	public static void sortGroup(MapPair[] arr) {

		sortGroup(arr, false);
	}

	public static void sortGroup(MapPair[] arr, boolean isRight) {

		Arrays.sort(arr, new GroupComparator(isRight));
	}

	/** sort an array of map pairs */
	public static void sort(MapPair[] arr) {

		Arrays.sort(arr, new Comparator<MapPair>() {

			public int compare(MapPair o1, MapPair o2) {
				MapPair p1 = o1;
				MapPair p2 = o2;
				double diff = p1.sim_ - p2.sim_;
				if (Math.abs(diff) < PRECISION) { // consider equal
					// be deterministic
					int h1 = p1.hashCode();
					int h2 = p2.hashCode();
					if (h1 < h2)
						return -1;
					else if (h1 > h2)
						return 1;
					else
						return 0;
				} else if (diff > 0)
					return -1;
				else
					// if(diff < 0)
					return 1;
			}

			@Override
			public boolean equals(Object that) {
				return this == that;
			}
		});
	}

	// shifts the values randomly by X percent up or down
	public static void randomizeMap(Collection<MapPair> c, int percent) {

		Iterator<MapPair> it = c.iterator();
		// Random rnd = new Random(150775011073L); // be repeatable
		Random rnd = new Random();

		while (it.hasNext()) {

			MapPair p = it.next();
			double sim = p.sim_ * (100 + rnd.nextInt() % percent) / 100;
			if (sim > 1)
				sim = 1;
			if (sim < 0)
				sim = 0;
			// System.err.println("RND: " + p.sim + " -> " + sim);
			p.sim_ = sim;
		}
	}

	// sets every value to the average
	public static void averagizeMap(Collection<MapPair> c) {

		Iterator<MapPair> it = c.iterator();

		double sum = 0;
		int count = 0;

		while (it.hasNext()) {

			MapPair p = it.next();
			sum += p.sim_;
			count++;
		}

		if (count > 0) {

			double avg = sum / count;

			// System.err.println("AVG is " + avg);

			it = c.iterator();
			while (it.hasNext()) {

				MapPair p = it.next();
				p.sim_ = avg;
			}
		}
	}

	public static Map<RDFNode, MapPair> toJavaMap(Collection<MapPair> c) {

		return toJavaMap(c, false);
	}

	// given: collection of MapPairs, returns: MapPair -> MapPair
	// inverted: r -> (l,r), otherwise: l -> (l,r)
	public static Map<MapPair, MapPair> toSelfMap(Collection<MapPair> c) {

		Map<MapPair, MapPair> table = new HashMap<MapPair, MapPair>();
		Iterator<MapPair> it = c.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			table.put(p, p);
		}
		return table;
	}

	public static MapPair get(Map<MapPair, MapPair> table, RDFNode r1, RDFNode r2, MapPair container) {

		container.r1 = r1;
		container.r2 = r2;
		MapPair res = table.get(container);
		return res;
	}

	public static MapPair getAdd(Map<MapPair, MapPair> table, RDFNode r1, RDFNode r2, MapPair container) {

		container.r1 = r1;
		container.r2 = r2;
		MapPair res = table.get(container);
		if (res == null) {
			res = container.duplicate();
			table.put(res, res);
		}
		return res;
	}

	// given: collection of MapPairs, returns: r1 -> MapPair
	// inverted: r -> {(l,r)}, otherwise: l -> {(l,r)}
	public static Map<RDFNode, Set<MapPair>> toJavaSetMap(Collection<MapPair> c, boolean inverted) {

		Map<RDFNode, Set<MapPair>> table = new HashMap<RDFNode, Set<MapPair>>();
		Iterator<MapPair> it = c.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			RDFNode key = inverted ? p.r2 : p.r1;
			Set<MapPair> s = table.get(key);
			if (s == null)
				table.put(key, s = new HashSet<MapPair>());
			s.add(p);
		}
		return table;
	}

	// given: collection of MapPairs, returns: r1 -> MapPair
	// inverted: r -> {l}, otherwise: l -> {r}
	public static Map<RDFNode, Set<RDFNode>> toJavaSetNodeMap(Collection<MapPair> c, boolean inverted) {

		Map<RDFNode, Set<RDFNode>> table = new HashMap<RDFNode, Set<RDFNode>>();
		Iterator<MapPair> it = c.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			RDFNode key = inverted ? p.r2 : p.r1;
			Set<RDFNode> s = table.get(key);
			if (s == null)
				table.put(key, s = new HashSet<RDFNode>());
			s.add(inverted ? p.r1 : p.r2);
		}
		return table;
	}

	// given: collection of MapPairs, returns: r1 -> MapPair
	// inverted: r -> (l,r), otherwise: l -> (l,r)
	public static Map<RDFNode, MapPair> toJavaMap(Collection<MapPair> c, boolean inverted) {

		Map<RDFNode, MapPair> table = new HashMap<RDFNode, MapPair>();
		Iterator<MapPair> it = c.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			table.put(inverted ? p.r2 : p.r1, p);
		}
		return table;
	}

	// given: collection of MapPairs, returns: r1 -> r2
	public static Map<RDFNode, RDFNode> toJavaMapDropSimilarity(Collection<MapPair> c) {

		Map<RDFNode, RDFNode> table = new HashMap<RDFNode, RDFNode>();
		Iterator<MapPair> it = c.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			table.put(p.r1, p.r2);
		}
		return table;
	}

	public static List<MapPair> replacementMap(Collection<MapPair> map) {

		List<MapPair> jointMap = new ArrayList<MapPair>();

		for (Iterator<MapPair> it = map.iterator(); it.hasNext();) {

			MapPair p = it.next();
			if (p.sim_ >= 0 && !p.inverse_)
				jointMap.add(p);
			else { // inverse
				MapPair n = new MapPair(p.r2, p.r1, Math.abs(p.sim_), true);
				jointMap.add(n);
			}
		}

		return jointMap;
	}

	// given: collection of MapPairs, returns: r1 -> MapPair
	public static Map<String, MapPair> toStringMap(Collection<MapPair> c) throws ModelException {

		Map<String, MapPair> table = new HashMap<String, MapPair>();
		Iterator<MapPair> it = c.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			if (p.r1 instanceof Literal)
				table.put(p.getLeftNode().getLabel(), p);
		}
		return table;
	}

	// Map -> Map
	@SuppressWarnings("unchecked")
	private static Map<Resource, MapPair> toMap(Model map) throws ModelException {

		NodeFactory f = map.getNodeFactory();
		MapVocabulary M = new MapVocabulary(f);

		Map<Resource, MapPair> m = new HashMap<Resource, MapPair>();

		Enumeration<Statement> en = map.elements();

		while (en.hasMoreElements()) {

			Statement st = en.nextElement();
			Resource e = st.subject();
			Resource pred = st.predicate();
			MapPair p = m.get(e);

			if (p == null) {
				p = new MapPair();
			}

			boolean putIt = false;

			if (pred.equals(M.src)) {
				p.r1 = st.object();
				putIt = true;
			} else if (pred.equals(M.dest)) {
				p.r2 = st.object();
				putIt = true;
			} else if (pred.equals(M.sim)) {
				p.sim_ = Double.parseDouble(st.object().getLabel());
				putIt = true;
			} else if (pred.equals(M.inverse)) {
				p.inverse_ = true;
				putIt = true;
			}

			if (putIt)
				m.put(e, p);
		}

		return m;
	}

	public static List<MapPair> toMapPairs(Model map) throws ModelException {

		if (map == null)
			return null;

		// System.err.println("RETRIEVING: " + map.getClass());

		if (map instanceof ModelWrapper) {

			ModelWrapper mw = (ModelWrapper) map;
			if (!mw.materialized) {
				Collection<MapPair> pairs = mw.getPairs();
				return new ArrayList<MapPair>(pairs); // pairs.toArray());
			}
		}

		// NodeFactory f = map.getNodeFactory();
		// MapVocabulary M = new MapVocabulary(f);

		// return new ArrayList(toMap(map).values());

		ArrayList<MapPair> list = new ArrayList<MapPair>();

		Collection<MapPair> c = toMap(map).values();

		Iterator<MapPair> it = c.iterator();
		while (it.hasNext()) {
			MapPair p = it.next();
			if (p.r1 == null || p.r2 == null)
				System.err.println("BOGUS pair: " + p);
			list.add(p);
		}

		return list;

		/*
		 * Model rm = map.find(null, RDF.type, M.Map); Enumeration en = rm.elements();
		 * 
		 * while(en.hasMoreElements()) {
		 * 
		 * Statement st = (Statement)en.nextElement(); Resource e = (Resource)st.subject(); RDFNode src = RDFUtil.getObject(map, e, M.src); RDFNode dest = RDFUtil.getObject(map, e, M.dest); String
		 * simlit = RDFUtil.getObjectLiteral(map, e, M.sim);
		 * 
		 * double sim = (simlit != null) ? Double.parseDouble(simlit) : 0.0;
		 * 
		 * MapPair p = new MapPair(src, dest, sim); list.add(p); }
		 */
	}

	/** arr: array of MapPairs */
	public static void toModelReal(Model map, Object[] arr) throws ModelException {

		// System.err.println("============ TO MODEL REAL");

		NodeFactory f = map.getNodeFactory();
		MapVocabulary M = new MapVocabulary(f);

		for (int i = 0; i < arr.length; i++) {

			MapPair p = (MapPair) arr[i];
			// System.err.println("Adding to model " + p);

			Resource m = f.createUniqueResource();
			map.add(f.createStatement(m, RDF.type, M.Map));
			map.add(f.createStatement(m, M.src, p.getLeftNode()));
			map.add(f.createStatement(m, M.dest, p.getRightNode()));
			if (p.sim_ != NULL_SIM)
				map.add(f.createStatement(m, M.sim, f.createLiteral("" + p.sim_)));
			if (p.inverse_)
				map.add(f.createStatement(m, M.inverse, f.createLiteral("t")));
		}
	}

	public static Model asModel(Model map, MapPair[] arr) throws ModelException {

		return new ModelWrapper(Arrays.asList(arr), map.create());
	}

	public static Model asModel(Model map, Collection<MapPair> c) throws ModelException {

		if (map == null)
			return null;
		return new ModelWrapper(c, map.create());
	}

	public static void printMap(Collection<MapPair> c, OutputStream out) {

		printMap(c, new OutputStreamWriter(out));
	}

	public static void printMap(Collection<MapPair> c, Writer w) {

		List<MapPair> l = new ArrayList<MapPair>(c);
		MapPair[] arr = new MapPair[0];
		l.toArray(arr);
		MapPair.sort(arr);

		Iterator<MapPair> it = Arrays.asList(arr).iterator();
		try {
			while (it.hasNext()) {

				MapPair p = it.next();
				w.write(p.toString());
				w.write('\n');
				w.flush();
			}
		} catch (IOException any) {
			// TODO
		}
	}

	public static Object[] toArray(Collection<Object> c) {

		Object[] a = new Object[c.size()];

		Iterator<Object> it = c.iterator();

		for (int i = 0; i < c.size(); i++)
			a[i] = it.next();

		return a;
	}

	public static MapPair[] toMapPairArray(Collection<MapPair> c) {

		MapPair[] a = new MapPair[c.size()];

		Iterator<MapPair> it = c.iterator();

		for (int i = 0; i < c.size(); i++)
			a[i] = it.next();

		return a;
	}

	public static double distance(MapPair[] a1, MapPair[] a2) {

		if (a1.length != a2.length)
			return -1.0;

		double diff = 0.0;

		for (int i = 0; i < a1.length; i++) {
			MapPair p1 = a1[i];
			MapPair p2 = a2[i];
			double d = p1.sim_ - p2.sim_;
			diff += d * d;
		}

		return Math.sqrt(diff);
	}

	public static int prefixLen(Object[] a1, Object[] a2) {

		int len = Math.min(a1.length, a2.length);

		int i = 0;

		while (i < len && a1[i].equals(a2[i])) {
			i++;
		}

		// if(i < len)
		// System.err.println("<" + a1[i] + ", " + a2[i] + ">");

		return i;
	}

	public static void addTo(Map<MapPair, MapPair> table, Collection<MapPair> pairs, double updateWeight) {

		Iterator<MapPair> it = pairs.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			MapPair c = table.get(p);
			if (c == null) {
				if (p.sim_ > 0) {
					c = p.duplicate();
					c.sim_ *= updateWeight;
					table.put(c, c);
				}
			} else {
				if (p.sim_ >= 0)
					c.sim_ += p.sim_ * updateWeight;
				else
					table.remove(c); // negative values are set zeroed out
			}
		}
	}

	public static void promoteSame(Collection<MapPair> pairs) {

		Iterator<MapPair> it = pairs.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			if (p.r1.equals(p.r2)) {
				System.err.println("PROMOTING SAME: " + p.r1 + " and " + p.r2);
				p.sim_ = 1.0;
			}
		}
	}

	/*
	 * public static void addToOverwriteLiterals(Map table, Collection pairs, double updateWeight) {
	 * 
	 * Iterator it = pairs.iterator(); while(it.hasNext()) {
	 * 
	 * MapPair p = (MapPair)it.next(); MapPair c = (MapPair)table.get(p); if(c == null) { c = p.duplicate(); c.sim *= updateWeight; table.put(c, c); } else { c.sim += p.sim * updateWeight; } //
	 * if(p.r1 instanceof Literal && p.r2 instanceof Literal) // c.sim = p.sim; if(c.r1.equals(c.r2)) c.sim = 1.0; } }
	 */

	// copies similarities only!
	public static void copy(Map<MapPair, MapPair> src, Map<MapPair, MapPair> dest) {

		Iterator<MapPair> it = src.values().iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			MapPair c = dest.get(p);
			if (c == null) {
				c = p.duplicate();
				dest.put(c, c);
			} else
				c.sim_ = p.sim_;
		}

		// clean up dest: remove all nodes that are not in src

		List<MapPair> toRemove = new ArrayList<MapPair>();
		it = dest.values().iterator();
		while (it.hasNext()) {
			MapPair c = it.next();
			if (!src.containsKey(c))
				toRemove.add(c);
		}
		it = toRemove.iterator();
		while (it.hasNext())
			dest.remove(it.next());
	}

	// returns normalized sum
	public static double normalize(Collection<MapPair> c) {

		double max = 0;
		double sum = 0;

		Iterator<MapPair> it = c.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			if (Math.abs(p.sim_) > max)
				max = Math.abs(p.sim_);
		}

		it = c.iterator();

		int count = 1;
		while (it.hasNext()) {

			MapPair p = it.next();
			if (max > 0)
				p.sim_ /= max;
			sum += p.sim_;
			count++;
		}
		return sum / count * 1000;
	}

	// leave for every matching element a choice of "choiceNum" items only, truncate the rest

	public static List<MapPair> prune(Collection<MapPair> c, int choiceNum) {

		HashMap<RDFNode, Integer> src = new HashMap<RDFNode, Integer>(); // RDFNode -> Integer
		HashMap<RDFNode, Integer> dest = new HashMap<RDFNode, Integer>(); // RDFNode -> Integer

		List<MapPair> list = new ArrayList<MapPair>();

		Integer[] ints = new Integer[choiceNum + 1];
		for (int i = 1; i < ints.length; i++)
			ints[i] = new Integer(i);

		Iterator<MapPair> it = c.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			Integer n1 = src.get(p.r1);
			Integer n2 = dest.get(p.r2);

			if (n1 == null || n2 == null || n1.intValue() < choiceNum || n2.intValue() < choiceNum) {

				list.add(p);

				// update counts
				src.put(p.r1, n1 == null ? ints[1] : (n1.intValue() < choiceNum ? ints[n1.intValue() + 1] : new Integer(n1.intValue() + 1)));
				dest.put(p.r2, n2 == null ? ints[1] : (n2.intValue() < choiceNum ? ints[n2.intValue() + 1] : new Integer(n2.intValue() + 1)));
			}
		}
		return list;
	}

	public Card getCardinality(Collection<MapPair> pairs) {

		MapPair pivot = this;
		int left = 0;
		int right = 0;

		Iterator<MapPair> it = pairs.iterator();
		while (it.hasNext()) {

			MapPair p = it.next();
			if (pivot.r1.equals(p.r1))
				left++;
			if (pivot.r2.equals(p.r2))
				right++;
		}

		return new Card(left, right);
	}

	public class Card {

		public int left;

		public int right;

		public Card(int left, int right) {

			this.left = left;
			this.right = right;
		}

		@Override
		public String toString() {

			return "Card[" + left + ", " + right + "]";
		}
	}

	/**
	 * Wrapper for efficient exchange of large mappings
	 */
	public static class ModelWrapper implements Model {

		Model m;

		Collection<MapPair> pairs;

		boolean materialized = false;

		public ModelWrapper(Collection<MapPair> pairs, Model empty) {

			this.pairs = pairs == null ? new ArrayList<MapPair>() : pairs;
			// System.err.println("SET PAIRS: " + pairs.size());
			this.m = empty;
		}

		/*
		 * protected ModelWrapper(Collection pairs, Model empty, boolean materialized) {
		 * 
		 * this.pairs = pairs; // System.err.println("SET PAIRS: " + pairs.size()); this.m = empty;
		 * 
		 * this.materialized = materialized; }
		 */

		public Collection<MapPair> getPairs() {

			// System.err.println("GET PAIRS: " + pairs.size());
			return pairs;
		}

		synchronized void materialize() throws ModelException {

			// if(true)
			// throw new ModelException("Attempt to materialize");

			if (!materialized) {

				// System.err.println("--------- materializing pairs ------------");
				toModelReal(m, pairs.toArray());
				materialized = true;
			}
		}

		public void setSourceURI(String uri) throws ModelException {

			materialize();
			m.setSourceURI(uri);
		}

		public String getLocalName() throws ModelException {

			materialize();
			return m.getLocalName();
		}

		public String getURI() throws ModelException {

			materialize();
			return m.getLocalName();
		}

		public String getNamespace() throws ModelException {

			materialize();
			return m.getLocalName();
		}

		public String getLabel() throws ModelException {

			materialize();
			return m.getLocalName();
		}

		public String getSourceURI() throws ModelException {

			materialize();
			return m.getSourceURI();
		}

		public int size() throws ModelException {

			if (!materialized) {
				return getPairs().size() * 4;
			} else
				return m.size();
		}

		public boolean isEmpty() throws ModelException {

			if (!materialized)
				return getPairs().isEmpty();
			else
				return m.isEmpty();
		}

		@SuppressWarnings("unchecked")
		public Enumeration<Statement> elements() throws ModelException {

			materialize();
			return m.elements();
		}

		public boolean contains(Statement t) throws ModelException {

			materialize();
			return m.contains(t);
		}

		public void add(Statement t) throws ModelException {

			materialize();
			m.add(t);
		}

		public void remove(Statement t) throws ModelException {

			materialize();
			m.remove(t);
		}

		public boolean isMutable() throws ModelException {

			// materialize();
			return m.isMutable();
		}

		public Model find(Resource subject, Resource predicate, RDFNode object) throws ModelException {

			materialize();
			return m.find(subject, predicate, object);
		}

		public Model duplicate() throws ModelException {

			// System.err.println("DUPLICATED WRAPPER");

			if (materialized)
				return m;
			else
				return new ModelWrapper(pairs, m.create());

			// materialize();
			// return m.duplicate();
		}

		public Model create() throws ModelException {

			// materialize();
			return m.create();
		}

		public NodeFactory getNodeFactory() throws ModelException {

			// materialize();
			return m.getNodeFactory();
		}

	}

	static class GroupComparator implements Comparator<MapPair> {

		boolean isRight;

		public GroupComparator(boolean isRight) {

			this.isRight = isRight;
		}

		public int compare(MapPair o1, MapPair o2) {

			MapPair p1 = o1;
			MapPair p2 = o2;

			RDFNode r1 = isRight ? p1.r2 : p1.r1;
			RDFNode r2 = isRight ? p2.r2 : p2.r1;

			if (r1 instanceof Comparable)
				return ((Comparable<Object>) r1).compareTo(r2);

			// make sure resources go first
			if (r1 instanceof Resource && r2 instanceof Literal)
				return -1;
			else if (r1 instanceof Literal && r2 instanceof Resource)
				return 1;

			try {

				int strDiff = r1.getLabel().compareTo(r2.getLabel());

				if (strDiff != 0)
					return strDiff;

			} catch (ModelException any) {

				return r1.hashCode() - r2.hashCode(); // just be deterministic
			}

			/*
			 * int hashDiff = r1.hashCode() - r2.hashCode(); if(hashDiff < 0) return -1; else if(hashDiff > 0) return 1; // hashes are equal
			 * 
			 * if(!r1.equals(r2)) { // hash collision, use string comparison }
			 */

			// same nodes, compare similarity
			double diff = p1.sim_ - p2.sim_;
			if (diff > 0)
				return -1;
			else if (diff < 0)
				return 1;
			else
				return 0;
		}

		@Override
		public boolean equals(Object that) {
			return that instanceof GroupComparator;
		}
	}

}
