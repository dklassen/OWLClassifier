
package org.semanticscience.dumontierlab.lib;

public class Edge {

	public int src;
	public int dest;
	public String lbl;
	
	/*
	 * @author :: Dana Klassen
	 * @contact :: 
	 * @description :: class to hold information extracted from a brach in a decision tree.
	 */
		public Edge(Integer tail,Integer head,String label){
			src = tail;
			dest = head;
			lbl = label;
		}
		
		public void setLbl(String str){
			lbl = str;
		}
}
