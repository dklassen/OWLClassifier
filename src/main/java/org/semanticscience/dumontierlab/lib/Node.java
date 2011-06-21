package org.semanticscience.dumontierlab.lib;

import java.lang.reflect.Array;
import java.util.Vector;

public class Node {

	public String ID;
	public String label;
	public int degree = -1;
	
	/*
	 * @author :: Dana Klassen
	 * @contact :: dklassen@connect.carleton.ca
	 * @description :: Object to hold information from a node in a decision tree
	 */
	public Node(String id,String label){
		this.ID = id;
		this.label = label;
	}
	
	public boolean equals(Object obj){
		Boolean result = false;
		
		if(obj instanceof Node){
			Node that = (Node) obj;
			result = (this.getID().equalsIgnoreCase(that.getID())); 
		}
		
		return result;
	}
	
	public String getID(){
		return ID;
	}
}
