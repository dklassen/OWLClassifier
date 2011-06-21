package org.semanticscience.dumontierlab.lib;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *  @author:: Dana Klassen
 *  @contact :: dklassen@connect.carleton.ca
 * @description:: recursive decent parser for DOT syntax. Does not handle complete language!
 */

public class DOTParser {

	//logger
	public static final Logger logger = LoggerFactory.getLogger( ClassificationOntologyGenerator.class );
	
	DOTScanner scanner;
	public Vector<Node> nodes;
	public Vector<Edge> edges;
	public Token t;
	
	public DOTParser(DOTScanner s,Vector m_nodes,Vector m_edges){
		 scanner = s;
		 this.nodes = m_nodes;
		 this.edges = m_edges;
	}
	public void next() throws IOException{
		t = scanner.nextToken();
	}
	
	/*
	 * @description::calculate the degree of each node
	 */
	public void degree(){
		
		int[] totalEdges = new int[nodes.size()];
		
		for(int i = 0;i < edges.size();i++){
			Edge e =  edges.elementAt(i);
			totalEdges[e.src]++;
			totalEdges[e.dest]++;
		}
		
		for(int i=0;i<edges.size();i++){
			Edge e = edges.elementAt(i);
			Node head  = nodes.elementAt(e.dest);
			Node tail = nodes.elementAt(e.src);
			
			if(head.degree == -1){
				head.degree = totalEdges[e.dest];
			}
			if(tail.degree == -1){
				tail.degree = totalEdges[e.src];
			}
		}

	}
	
	/*
	 * @description :: recursively descend the syntax and process each part.
	 */
	public void graph() throws IOException{
		
		next();
		
			if(t.text.equalsIgnoreCase("digraph")){
				graphId();
				next();
				if(t.type == '{'){
					//System.out.println("Entering stmt_list expecting { got: " + t.text);
					stmt_list();
				}else if(t.type == '}'){
					System.out.println("Congrats");
				}
			}else{
				throw new IOException("Structure incorrect at token: " + t.text);
			}
		
			degree();
	}
	
	
	public void stmt_list() throws IOException{
		next();

		if(t.type == '}' || t.type == StreamTokenizer.TT_EOF){
			return;
		}
		else{
			//System.out.println("Entering statement:" + t.text);
			stmt();
			stmt_list();
		}
	}
	
	public void nodeStmt(final Integer nodeIndex) throws IOException{
		next();
		Node temp = (Node) nodes.elementAt(nodeIndex);
		
		if(t.type == ']' || t.type == StreamTokenizer.TT_EOF){
			return;
		}else if(t.type == StreamTokenizer.TT_WORD){
			if(t.text.equalsIgnoreCase("label")){
				next();
				if(t.text.equalsIgnoreCase("=")){
					next();
					if(t.type == StreamTokenizer.TT_WORD || t.type == '"'){
						//System.out.println("Adding label to node:  " + nodeIndex + t.text);
						temp.label = t.text;
					}else{
						logger.debug("There was an error unable to deal with: " + t.text + " at line:" + t.line);
						scanner.pushback();
					}
				}else{
					logger.debug("There was an error unable to deal with: " + t.text + " at line:" + t.line);
					scanner.pushback();
				}
			}else{
				logger.debug("There was an error unable to deal with: " + t.text + " at line:" + t.line);
				//scanner.pushback();
			}
		}
		nodeStmt(nodeIndex);
	}
	
	public void edgeStmt(Integer index) throws IOException{
		next();
		
		//System.out.println("Edge statement expecting: > got:" + t.text);
		Edge temp= null;
		if(t.type == ']' || t.type == StreamTokenizer.TT_EOF){
			return;
		}else if(t.type == '>'){
			next();
		
			if(t.type == StreamTokenizer.TT_WORD){
			node();
			int dest = nodes.indexOf(new Node(t.text,t.text));
			
			//System.out.println("creating edge from src:" + index + " to dest:" + dest);
			temp = new Edge(index,dest,null);
			
				if(edges !=null && !edges.contains(temp)){
					edges.add(temp);
				}
			}
			
			next();
			
			if(t.type == '[' && temp!=null){
				//System.out.println("Entering edge attribute:" + t.text);
				edgeAttr(temp);
			}else{
				logger.debug("Did not find a label for this edge");
				//scanner.pushback();
			}
		}else{
			logger.debug("There was an error at line: " + t.line);
			
			//return;
		}
		
	}
	
	public void edgeAttr(Edge e) throws IOException{
		next();
		if(t.type == ']' || t.type == StreamTokenizer.TT_EOF){
			return;
		}else{
			if(t.type == StreamTokenizer.TT_WORD){
				if(t.text.equalsIgnoreCase("label")){
					next();
					if(t.text.equalsIgnoreCase("=")){
						next();
						if(t.type == '"'){
							//System.out.println("Setting label of edge:"+e.src + "->" + e.dest + " to " + t.text);
							e.setLbl(t.text);
						}else{
							logger.debug("There was an error unable to deal with: " + t.text + " at line:" + t.line);
							scanner.pushback();
						}
					}
				}
			}else{
				logger.debug("There was an error unable to deal with: " + t.text + " at line:" + t.line);
				//scanner.pushback();
			}
		}
		edgeAttr(e);
	}
	
	public void stmt() throws IOException{
		//next();
		
			
		if(t.text.equalsIgnoreCase("graph") || t.text.equalsIgnoreCase("node") || t.text.equalsIgnoreCase("edge")){
			//do nothing
		}else{
			node();
			
			int nodeindex = nodes.indexOf(new Node(t.text,t.text));
			//System.out.println("Created node:" + t.text + " at index:" + nodeindex);
			next();
			
			//System.out.println(t.text);
			if(t.type == '['){
				//System.out.println("Entering node statement expect [ got " + t.text);
				nodeStmt(nodeindex);
			}else if(t.type == '-'){
				edgeStmt(nodeindex);
			}
			
		}
		
		
	}
	public void cluster(){
		//:	'{' ( property ';' | edge ';' | subgraph )* '}'
		//;
	}
	
	public void subgraph(){
		//:	'subgraph' ID cluster
		//;
	}
	
	public void edge(){
		//nodelabel '->' nodelabel optionlist?
		//;
	}
	
	public void node() throws IOException{
		//next();
		if(nodes !=null && !nodes.contains(new Node(t.text,t.text))){
			nodes.addElement(new Node(t.text,t.text));
		}else{
			//throw new IOException("Could not add node to vector?");
		}
	}
	
	
	public void nodelabel(){
		//:	value
		//;
	}
	
	public void edgelabel(){
		
	}
	
	public void property(){
		
	}
	
	public void value(){
		//:	ID
		//|	STRING
		//|	INT
		//;
	}
	
	public void optionlist(){
		//:	'[' property (',' property)* ']'
		//;
	}
	
	public void graphId() throws IOException{
			next();
			if(t.type == StreamTokenizer.TT_WORD){
				//System.out.println("ID:" + t.text);
			}else{
				logger.error("Exception coming your way from graphID()");
				throw new IOException("Expecting ID.");
			}
	}
}
