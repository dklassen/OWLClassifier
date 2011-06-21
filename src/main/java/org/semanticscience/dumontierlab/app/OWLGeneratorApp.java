package org.semanticscience.dumontierlab.app;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.semanticscience.dumontierlab.lib.ClassificationOntologyGenerator;

/**
 * Hello world!
 *
 */
public class OWLGeneratorApp 
{
	
    public static void main( String[] args )
    {	
	
       // create the default options
       Options options = new Options();
       // add options
       
       Option uri = OptionBuilder.withArgName("ONTOLOGY URI")
       									.hasArg()
       									.isRequired(true)
       									.withDescription("create ontology using provided base uri")
       									.create("uri");
       									
       
       Option input = OptionBuilder.withArgName( "FILE" )
       									.hasArg()
       									.isRequired(true)
       									.withDescription("Read the given file.")
       									.create("input");
       											
       Option output = OptionBuilder.withArgName("FILE")
       								.hasArg()
       								.isRequired(true)
       								.withDescription("Output the results.")
       								.create("output");
       

       
       
       options.addOption(uri);
       options.addOption(input);
       options.addOption(output);
       
   	CommandLineParser parser = new GnuParser();
   	
   	// comment out when using on the command line.
   	//String[] testargs = new String[]{"-input","/Users/dana/Desktop/test.nt","-output","/Users/dana/Desktop/testsum.nt"};
   	
   	// parse the command line
	try {
		CommandLine cmd = parser.parse(options,args);
		// run the program with the specified options 
		ClassificationOntologyGenerator gen;
		try {
			
	
			gen = new ClassificationOntologyGenerator(cmd.getOptionValue("input"),cmd.getOptionValue("uri"));
			gen.saveOntology(cmd.getOptionValue("output"));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		
	} catch (ParseException e) {
		//System.out.println(e.getMessage());
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("OWL Descision Tree Generator", options);
	}
	
  }
}
