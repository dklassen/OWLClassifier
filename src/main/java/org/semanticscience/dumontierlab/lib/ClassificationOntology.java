package org.semanticscience.dumontierlab.lib;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/*
 * @author: Dana Klassen
 * @contact : dklassen@connect.carleton.ca
 * @description : Interface between OWL ontology and decision tree. Should not be used independently but only through
 * ClassificationOntologyGenerator
 */
public class ClassificationOntology {
 
	OWLDataFactory factory;
	OWLOntology ontology;
	OWLOntologyManager manager;
	IRI ontologyURI;
	String delim = "#";
	
	public ClassificationOntology(String referenceUri) throws OWLOntologyCreationException{
			
			ontologyURI =  IRI.create(referenceUri);
			manager = OWLManager.createOWLOntologyManager();
			ontology = manager.createOntology(ontologyURI);
			factory = manager.getOWLDataFactory();	
		}
	
	/*
	 * @description :: Generate owl class using supplied string
	 */
	public OWLClass createOWLClass(String id){	
		return factory.getOWLClass(IRI.create(ontologyURI + delim +id));
	}
	
	/*
	 * @description :: create and apply a rdfs comment to an OWL Class.
	 */
	public void applyRDFSComment(OWLClass cls,String label){
		OWLLiteral lbl = factory.getOWLLiteral(label,"en");
		OWLAnnotationProperty lblIRI = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI());
		OWLAnnotation annotation = factory.getOWLAnnotation(lblIRI,lbl);
		OWLAxiom l = factory.getOWLAnnotationAssertionAxiom(cls.getIRI(),annotation);
		AddAxiom axiom = new AddAxiom(ontology, l);
	    manager.applyChange(axiom);
	}
	
	/*
	 * @description :: create and apply a rdfs label to an OWL class.
	 */
	public void applyRDFSLabel(OWLClass cls, String label){
	    
		OWLLiteral lbl = factory.getOWLLiteral(label,"en");
		OWLAnnotationProperty lblIRI = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
		OWLAnnotation annotation = factory.getOWLAnnotation(lblIRI,lbl);
		OWLAxiom l = factory.getOWLAnnotationAssertionAxiom(cls.getIRI(),annotation);
		AddAxiom axiom = new AddAxiom(ontology, l);
	    manager.applyChange(axiom);
	}
	
	/*
	 * @description :: Create existential restriction of the super class and a descriptor that is less than or equal to a float value.
	 */
	public void createLessThanOrEqualToClassAxiom(OWLClass superclass,OWLClass subclass,OWLClass descriptor,Float v){
	
		 OWLDataProperty hasValue = factory.getOWLDataProperty(IRI.create(ontologyURI + "#hasValue"));
		 OWLObjectProperty hasAttribute = factory.getOWLObjectProperty(IRI.create(ontologyURI + "#hasAttribute"));
		 
		 OWLDatatype floatDatatype = factory.getFloatOWLDatatype();
		 OWLLiteral value = factory.getOWLLiteral(v);
		 OWLFacet facet = OWLFacet.MIN_EXCLUSIVE;
		 
		 OWLDataRange greaterthan = factory.getOWLDatatypeRestriction(floatDatatype,facet,value);
		 
		 OWLDataSomeValuesFrom  someValuesFrom = factory.getOWLDataSomeValuesFrom(hasValue,greaterthan);
		 
		 // create axiom unions => (descriptor has (hasValue value boolean)
	     OWLObjectIntersectionOf desValueRestriction = factory.getOWLObjectIntersectionOf(descriptor,someValuesFrom);
	     
	     // => hasAttribute some (Descriptor hasAttribute (hasValue value float[<=#])
	     OWLObjectSomeValuesFrom hasAttributeSomeDescriptor = factory.getOWLObjectSomeValuesFrom(hasAttribute,desValueRestriction);
	      
	     //create intersection of superclass and descriptor
	     OWLObjectIntersectionOf branchRestriction = factory.getOWLObjectIntersectionOf(superclass,hasAttributeSomeDescriptor);
	      
	     // make equivalent class axiom
	     OWLEquivalentClassesAxiom classAxiom = factory.getOWLEquivalentClassesAxiom(subclass,branchRestriction);
	     
	     //apply change
	     manager.applyChange(new AddAxiom(ontology,classAxiom));

	}
	
	/*
	 * @description :: create an existential restriction axiom that is the intersection between a superclass, descriptor, and some boolean value.
	 * e.x superclass and hasAttribute some (descriptor that (hasvalue some boolean))
	 */
	public void createBooleanEquivalentClassAxiom(OWLClass desc,OWLClass supercls,OWLClass subcls,Boolean value){
		
		//create the data value
		OWLLiteral someBooleanValue = factory.getOWLLiteral(value);
		
		// create data property
		OWLDataProperty hasValue = factory.getOWLDataProperty(IRI.create(ontologyURI + "#hasValue"));
		
		//create object property
		OWLObjectProperty hasAttribute = factory.getOWLObjectProperty(IRI.create(ontologyURI + "#hasAttribute"));
		
		//set the value of the data property
		OWLDataHasValue hasValueSomeBoolean = factory.getOWLDataHasValue(hasValue,someBooleanValue);
		
	    //create object intersection
		OWLClassExpression descHasValueSomeBoolean = factory.getOWLObjectIntersectionOf(desc,hasValueSomeBoolean);
	      
		//some values from
		OWLObjectSomeValuesFrom hasAttributeSomeDescriptor = factory.getOWLObjectSomeValuesFrom(hasAttribute,descHasValueSomeBoolean);
		
		//object intersection with above statement and superclass
		OWLObjectIntersectionOf intersection = factory.getOWLObjectIntersectionOf(supercls,hasAttributeSomeDescriptor);
	      
		//create the equivalent class axiom
		OWLEquivalentClassesAxiom equivClassAxiom = factory.getOWLEquivalentClassesAxiom(subcls,intersection);
		
		//make the change to the ontology
		manager.applyChange(new AddAxiom(ontology,equivClassAxiom));

	}
	
	/*
	 * @description :: create an existential class axiom of the intersection between supeclass, descriptor, and some value that is less than a float value.
	 * ex. superclass and hasAttribute some descriptor and hasValue some [value<#]
	 */
	public void createLessThanClassAxiom(OWLClass descriptor,OWLClass superclass,OWLClass subclass,Float v){
		

		 OWLDataProperty hasValue = factory.getOWLDataProperty(IRI.create(ontologyURI + "#hasValue"));
		 OWLObjectProperty hasAttribute = factory.getOWLObjectProperty(IRI.create(ontologyURI + "#hasAttribute"));
		 
		 OWLDatatype floatDatatype = factory.getFloatOWLDatatype();
		 OWLLiteral value = factory.getOWLLiteral(v);
		 OWLFacet facet = OWLFacet.MAX_EXCLUSIVE;
		 
		 OWLDataRange lessthan = factory.getOWLDatatypeRestriction(floatDatatype,facet,value);
		 
		 OWLDataSomeValuesFrom  someValuesFrom = factory.getOWLDataSomeValuesFrom(hasValue,lessthan);
			 
		 // create axiom unions => (descriptor has (hasValue value boolean)
	     OWLObjectIntersectionOf desValueRestriction = factory.getOWLObjectIntersectionOf(descriptor,someValuesFrom);
	     
	     // => hasAttribute some (Descriptor hasAttribute (hasValue value float[<=#])
	     OWLObjectSomeValuesFrom hasAttributeSomeDescriptor = factory.getOWLObjectSomeValuesFrom(hasAttribute,desValueRestriction);
	      
	     //create intersection of superclass and descriptor
	     OWLObjectIntersectionOf branchRestriction = factory.getOWLObjectIntersectionOf(superclass,hasAttributeSomeDescriptor);
	      
	     // make equivalent class axiom
	     OWLEquivalentClassesAxiom classAxiom = factory.getOWLEquivalentClassesAxiom(subclass,branchRestriction);
	     
	     //apply change
	     manager.applyChange(new AddAxiom(ontology,classAxiom));


	}
	
	/*
	 * @description : create an existential class axiom of the intersection between the superclass, descriptor, and some value which is greater than a float value.
	 * ex. superclass and hasAttribute some descriptor and hasValue some [value>#]
	 */
	public void createGreaterThanClassAxiom(OWLClass descriptor,OWLClass superclass,OWLClass subclass,Float v){
		

		 OWLDataProperty hasValue = factory.getOWLDataProperty(IRI.create(ontologyURI + "#hasValue"));
		 OWLObjectProperty hasAttribute = factory.getOWLObjectProperty(IRI.create(ontologyURI + "#hasAttribute"));
		 
		 OWLDatatype floatDatatype = factory.getFloatOWLDatatype();
		 OWLLiteral value = factory.getOWLLiteral(v);
		 OWLFacet facet = OWLFacet.MAX_INCLUSIVE;
		 
		 OWLDataRange greaterthan = factory.getOWLDatatypeRestriction(floatDatatype,facet,value);
		 
		 OWLDataSomeValuesFrom  someValuesFrom = factory.getOWLDataSomeValuesFrom(hasValue,greaterthan);
		 
		 // create axiom unions => (descriptor has (hasValue value boolean)
	     OWLObjectIntersectionOf desValueRestriction = factory.getOWLObjectIntersectionOf(descriptor,someValuesFrom);
	     
	     // => hasAttribute some (Descriptor hasAttribute (hasValue value float[<=#])
	     OWLObjectSomeValuesFrom hasAttributeSomeDescriptor = factory.getOWLObjectSomeValuesFrom(hasAttribute,desValueRestriction);
	      
	     //create intersection of superclass and descriptor
	     OWLObjectIntersectionOf branchRestriction = factory.getOWLObjectIntersectionOf(superclass,hasAttributeSomeDescriptor);
	      
	     // make equivalent class axiom
	     OWLEquivalentClassesAxiom classAxiom = factory.getOWLEquivalentClassesAxiom(subclass,branchRestriction);
	     
	     //apply change
	     manager.applyChange(new AddAxiom(ontology,classAxiom));


	}
	
	/*
	 * @description :: create an equivalent class restriction that is the intersection between superclass, descriptor, and some value greater or equal to some float.
	 * ex. superclass and hasAttribute some Attribute that hasValue some [value>=#]
	 */
	public void createGreaterThanOrEqualClassAximo(OWLClass descriptor,OWLClass superclass,OWLClass subclass,Float v){
		OWLDataProperty hasValue = factory.getOWLDataProperty(IRI.create(ontologyURI + "#hasValue"));
		 OWLObjectProperty hasAttribute = factory.getOWLObjectProperty(IRI.create(ontologyURI + "#hasAttribute"));
		 
		 OWLDatatype floatDatatype = factory.getFloatOWLDatatype();
		 OWLLiteral value = factory.getOWLLiteral(v);
		 OWLFacet facet = OWLFacet.MAX_INCLUSIVE;
		 
		 OWLDataRange datarange = factory.getOWLDatatypeRestriction(floatDatatype,facet,value);
		 
		 OWLDataSomeValuesFrom  someValuesFrom = factory.getOWLDataSomeValuesFrom(hasValue,datarange);
		 
		 // create axiom unions => (descriptor has (hasValue value boolean)
	     OWLObjectIntersectionOf desValueRestriction = factory.getOWLObjectIntersectionOf(descriptor,someValuesFrom);
	     
	     // => hasAttribute some (Descriptor hasAttribute (hasValue value float[<=#])
	     OWLObjectSomeValuesFrom hasAttributeSomeDescriptor = factory.getOWLObjectSomeValuesFrom(hasAttribute,desValueRestriction);
	      
	     //create intersection of superclass and descriptor
	     OWLObjectIntersectionOf branchRestriction = factory.getOWLObjectIntersectionOf(superclass,hasAttributeSomeDescriptor);
	      
	     // make equivalent class axiom
	     OWLEquivalentClassesAxiom classAxiom = factory.getOWLEquivalentClassesAxiom(subclass,branchRestriction);
	     
	     //apply change
	     manager.applyChange(new AddAxiom(ontology,classAxiom));
	}
	
	/*
	 * @description :: create an equivalent class restriction that is the intersection between the superclass, descriptor, and value less than or equal to some float.
	 * ex. superclass and hasAttribute some attribute and hasValue some [value<=#]
	 */
	public void createLessThanOrEqualClassAximo(OWLClass descriptor,OWLClass superclass,OWLClass subclass,Float v){
		OWLDataProperty hasValue = factory.getOWLDataProperty(IRI.create(ontologyURI + "#hasValue"));
		 OWLObjectProperty hasAttribute = factory.getOWLObjectProperty(IRI.create(ontologyURI + "#hasAttribute"));
		 
		 OWLDatatype floatDatatype = factory.getFloatOWLDatatype();
		 OWLLiteral value = factory.getOWLLiteral(v);
		 OWLFacet facet = OWLFacet.MIN_INCLUSIVE;
		 
		 OWLDataRange datarange = factory.getOWLDatatypeRestriction(floatDatatype,facet,value);
		 
		 OWLDataSomeValuesFrom  someValuesFrom = factory.getOWLDataSomeValuesFrom(hasValue,datarange);
		 
		 // create axiom unions => (descriptor has (hasValue value boolean)
	     OWLObjectIntersectionOf desValueRestriction = factory.getOWLObjectIntersectionOf(descriptor,someValuesFrom);
	     
	     // => hasAttribute some (Descriptor hasAttribute (hasValue value float[<=#])
	     OWLObjectSomeValuesFrom hasAttributeSomeDescriptor = factory.getOWLObjectSomeValuesFrom(hasAttribute,desValueRestriction);
	      
	     //create intersection of superclass and descriptor
	     OWLObjectIntersectionOf branchRestriction = factory.getOWLObjectIntersectionOf(superclass,hasAttributeSomeDescriptor);
	      
	     // make equivalent class axiom
	     OWLEquivalentClassesAxiom classAxiom = factory.getOWLEquivalentClassesAxiom(subclass,branchRestriction);
	     
	     //apply change
	     manager.applyChange(new AddAxiom(ontology,classAxiom));
	}
	/*
	 * @description :: Create an OWL subclss axiom based on the two classes passed in.
	 */
	public void createOWLSubclass(OWLClass supercls,OWLClass subcls){   
		OWLSubClassOfAxiom subclsAxiom = factory.getOWLSubClassOfAxiom(subcls,supercls);
		manager.applyChange(new AddAxiom(ontology,subclsAxiom));
	}
	
	/*
	 * @description :: Save the OWL ontology to file.
	 */
	public void saveOntology(String filename){
		File file = new File(filename);
		try {
			manager.saveOntology(ontology,IRI.create(file.toURI()));
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	    
	}

}
