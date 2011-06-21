package org.semanticscience.dumontierlab.lib;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.graphviz.Graph;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.trees.J48;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.gui.beans.DataSource;
import weka.gui.graphvisualizer.DotParser;
import weka.gui.graphvisualizer.GraphEdge;
import weka.gui.graphvisualizer.GraphNode;

/*
 * @author:: Dana Klassen
 * @contact :: dklassen@connect.carleton.ca
 * @description:: Class to generate classification ontology using J48 decision tree algorithm . Input ARFF file, output owl file.
 * Currently uses the default parameters for the developing the decision tree.
 */
public class ClassificationOntologyGenerator {

	//Log any errors to
	public static final Logger logger = LoggerFactory.getLogger( ClassificationOntologyGenerator.class );
	
	// OWL ontology representation of decision tree
	private ClassificationOntology ontology;
	
	
	/*
	 * @method :: initialize
	 * @parameters :: filename is the name of the ARFF datafile, refererenceURI is the dereferenceable URI
	 * @description :: will generate using WEKA and j48 algorithm a decision tree which will be converted to OWL ontology.
	 */
	@SuppressWarnings("deprecation")
	public ClassificationOntologyGenerator(String filename,String referenceURI) throws IOException
	{
		
		// load the data set from Arff file fomat
		ArffLoader dataset = new ArffLoader();
		dataset.setFile(new File(filename));
		Instances instances = dataset.getDataSet();
		// set the class as the last column.
		instances.setClassIndex(instances.numAttributes()-1);
		
		//create the ontology model
		try {
			 ontology = new ClassificationOntology(referenceURI);
		} catch (OWLOntologyCreationException e1) {
			// TODO Auto-generated catch block
			logger.error(e1.getMessage());
		}
		// set up the classifier and build the model
		J48 decisiontree = new J48();
		try {
			decisiontree.buildClassifier(instances);
			String graph = decisiontree.graph();
						
			Vector nodes = new Vector();
			Vector edges = new Vector();
			DOTScanner scanner = new DOTScanner(new StringReader(graph));
			DOTParser parser = new DOTParser(scanner,nodes,edges);
			
			//generate the graph
			parser.graph();

			for(Object e : edges.toArray()){
				Edge e1 = (Edge) e;
				Node head = (Node) nodes.elementAt(e1.dest);
				Node tail = (Node) nodes.elementAt(e1.src);
				
				if(head.degree > 1){
					//extend branch
					extendBranch(head,tail,e1);
				}else{
					//build terminal branch
					terminateBranch(head,tail,e1);
				}
			}
			

	
		} catch (Exception e) {
			logger.error("Unable to build classifier:");
			logger.error(e.getMessage());
		}
	}
	
	/*
	 * @description :: extend the current branch of the ontology to the child attribute.
	 */
	public void extendBranch(Node h, Node t, Edge e){
			
			//create necessary classes
			OWLClass superCls = ontology.createOWLClass("Substance_"+t.ID);
			OWLClass subCls = ontology.createOWLClass("Substance_"+h.ID);
			
			//node specific descriptor
			OWLClass n_descriptor = ontology.createOWLClass(t.label + "_" + t.ID);
			//generalized descriptor
			OWLClass descriptor = ontology.createOWLClass(t.label);			
			
			//apply label annotations
			ontology.applyRDFSLabel(superCls,"Substance_" + t.ID);
			ontology.applyRDFSLabel(subCls,"Substance_"+h.ID);
			ontology.applyRDFSLabel(descriptor, t.label);
			
			//apply comment annotations
			ontology.applyRDFSComment(superCls, t.label);
			ontology.applyRDFSComment(subCls, h.label);
			
			//make tail node parent of head node
			ontology.createOWLSubclass(superCls, subCls);
			//make descriptor child of descriptor class.
			ontology.createOWLSubclass(ontology.createOWLClass("Descriptor"),n_descriptor);
			ontology.createOWLSubclass(n_descriptor,descriptor);
			
			// determine the branch rule i.e true, false, >,< etc.
			if(e.lbl.contains("true") || e.lbl.contains("TRUE")){
				logger.debug("Building boolean branch:" + e.lbl);
				ontology.createBooleanEquivalentClassAxiom(n_descriptor, superCls, subCls,true);
			}else if(e.lbl.contains("false") || e.lbl.contains("FALSE")){
				logger.debug("Building less or equal branch:" + e.lbl);
					ontology.createBooleanEquivalentClassAxiom(n_descriptor, superCls, subCls,false);
			}else if(e.lbl.contains(">=")){
				logger.debug("Building less or equal branch:" + e.lbl);

			}else if(e.lbl.contains("<=")){
				logger.debug("Building less or equal branch:" + e.lbl);
				Pattern p = Pattern.compile("(\\d+\\.?\\d+|\\d+)");
				
				Matcher m = p.matcher(e.lbl);
				
				if(m.find()){
					Float v = new Float(m.group(1));
					ontology.createLessThanOrEqualToClassAxiom(superCls, subCls, n_descriptor,v);
				}else{
					logger.error("No value found in: " + e.lbl);
				}
			}else{
				logger.debug("Unable to determine branch type:" + e.lbl);
			}
		}
		 
	/*
	 * @description:: build the terminating branch of the ontology leading to classification into a category.
	 */
	public void terminateBranch(Node h,Node t, Edge e){
			
			//need to sub out certain characters that WEKA throws in to terminal classes
			//System.out.println(h.label.replaceAll("\\s\\([,0-9\\\\/.]+\\)", ""));
			//create necessary classes
			OWLClass superCls = ontology.createOWLClass("Substance_"+t.ID);
			//OWLClass subCls = ontology.createOWLClass("Substance_"+h.ID);
			OWLClass n_descriptor = ontology.createOWLClass(t.label + "_" + t.ID);
			OWLClass descriptor = ontology.createOWLClass(t.label);
			
			OWLClass generalcategory = ontology.createOWLClass("Category");
			OWLClass category = ontology.createOWLClass(h.label.replaceAll("\\s\\([,0-9\\\\/.]+\\)", ""));
			OWLClass scategory = ontology.createOWLClass("Category_"+h.ID);
			
			//apply label annotations
			ontology.applyRDFSLabel(superCls,"Substance_" + t.ID);
			ontology.applyRDFSLabel(scategory,h.label.replaceAll("\\s\\([,0-9\\\\/.]+\\)", "") + "_" + h.ID);
			ontology.applyRDFSLabel(category,h.label.replaceAll("\\s\\([,0-9\\\\/.]+\\)", ""));
			ontology.applyRDFSLabel(n_descriptor, t.label + "_" + t.ID);
			ontology.applyRDFSLabel(descriptor,t.label);
			//apply comment annotations
			ontology.applyRDFSComment(superCls, t.label);
			//ontology.applyRDFSComment(cls, label)
			
			
			//make tail node parent of head node
			ontology.createOWLSubclass(superCls, scategory);
			//make descriptor child of descriptor class.
			ontology.createOWLSubclass(ontology.createOWLClass("Descriptor"),n_descriptor);
			ontology.createOWLSubclass(n_descriptor,descriptor);
			ontology.createOWLSubclass(generalcategory, category);
			ontology.createOWLSubclass(category, scategory);
			
			// determine the branch rule i.e true, false, >,< etc.
			if(e.lbl.contains("true") || e.lbl.contains("TRUE")){
					logger.debug("Building boolean branch:" + e.lbl);
					ontology.createBooleanEquivalentClassAxiom(n_descriptor, superCls, scategory,true);
			}else if(e.lbl.contains("false")|| e.lbl.contains("FALSE")){
				logger.debug("Building less or equal branch:" + e.lbl);
					ontology.createBooleanEquivalentClassAxiom(n_descriptor, superCls, scategory,false);
			}else if(e.lbl.contains(">=")){
				logger.debug("Building less or equal branch:" + e.lbl);
				
			}else if(e.lbl.contains(">")){
				logger.debug("Building less or equal branch:" + e.lbl);
				Pattern p = Pattern.compile("(\\d+\\.?\\d+|\\d+)");
				
				Matcher m = p.matcher(e.lbl);
				
				if(m.find()){
					Float v = new Float(m.group(1));

					ontology.createGreaterThanClassAxiom(n_descriptor, superCls, scategory,v);
				}else{
					logger.error("No value found in: " + e.lbl);
				}
			}else if(e.lbl.contains("<=")){
				logger.debug("Building less or equal branch:" + e.lbl);
				Pattern p = Pattern.compile("(\\d+\\.?\\d+|\\d+)");
				
				Matcher m = p.matcher(e.lbl);
				
				if(m.find()){
					Float v = new Float(m.group(1));

					ontology.createLessThanOrEqualToClassAxiom(superCls, scategory, n_descriptor, v);
				}else{
					logger.error("No value found in: " + e.lbl);
				}
			}else if(e.lbl.contains("<")){
				logger.debug("Building less or equal branch:" + e.lbl);
				Pattern p = Pattern.compile("(\\d+\\.?\\d+|\\d+)");
				
				Matcher m = p.matcher(e.lbl);
				
				if(m.find()){
					Float v = new Float(m.group(1));

					ontology.createLessThanOrEqualToClassAxiom(superCls, scategory, n_descriptor, v);
				}else{
					logger.error("No value found in: " + e.lbl);
				}
			}else{
				logger.debug("Unable to determine branch type:" + e.lbl);
			}
		}
	
	/*
	 * @description :: save the ontology to the location specified by the filename.
	 */
	public void saveOntology(String filename){
		ontology.saveOntology(filename);
	}
	
}
