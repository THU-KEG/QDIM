package edu.thu.keg.model;

import java.util.HashSet;


/**
 *   Data Structure to Express the neighbor of an individual
 *   The neighbors are marked with their corresponding property.
 *   The reverse property will be added a "/reverse" to mark it. 
 */

public class Successor {
	String property;
	HashSet<String> neighbors;
	
	public Successor(String property) {
		this.property = property;
		this.neighbors = new HashSet<String>();
	}
	
	public void addNeighbor(String neighbor) {
		this.neighbors.add(neighbor);
	}
	
	public int size() {
		return neighbors.size();
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public HashSet<String> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(HashSet<String> neighbors) {
		this.neighbors = neighbors;
	}
}
