
package org.semanticscience.dumontierlab.lib;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Vector;

public class DOTScanner implements TokenStream {

	protected StreamTokenizer tk;
		public DOTScanner(Reader input){
			 tk = new StreamTokenizer(input);
			 setSyntax(tk);
		}
		
	public static void main(String[] args) throws FileNotFoundException{
		DOTScanner scanner = new DOTScanner(new FileReader("/Users/dana/Desktop/druglikeHMDB.dot"));
		Token t;
		
		try {
			t = scanner.nextToken();
			while ( t.type != Token.EOF_TYPE ) {
				System.out.println(t.text);
				t = scanner.nextToken();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
				
		protected void setSyntax(StreamTokenizer tk){
			tk.resetSyntax();
            tk.eolIsSignificant(false);
            tk.slashStarComments(true);
            tk.slashSlashComments(true);
            tk.whitespaceChars(0, ' ');
            tk.wordChars(35, 255);
            tk.ordinaryChar('[');
            tk.ordinaryChar(']');
            tk.ordinaryChar('{');
            tk.ordinaryChar('}');
            tk.ordinaryChar('-');
            tk.ordinaryChar('>');
            tk.ordinaryChar('/');
            tk.ordinaryChar('*');
            tk.quoteChar('"');
             // tk.ordinaryChar('\\');
           // tk.whitespaceChars(';', ';');
            tk.ordinaryChar('=');
		}
		
		public void pushback(){
			tk.pushBack();
		}
		
		public Token nextToken() throws IOException {
			
			tk.nextToken();
			
			
			if(tk.ttype == tk.TT_WORD || tk.ttype == '"'){
				return new Token(tk.ttype,tk.sval,tk.lineno());
			}else if(tk.ttype == tk.TT_NUMBER){
				return  new Token(tk.ttype,tk.toString(),tk.lineno());
			}else if(tk.ttype == tk.TT_EOF || tk.ttype == tk.TT_EOL){
				return new Token(tk.ttype,"",tk.lineno());
			}else{
				return new Token(tk.ttype,Character.toString((char)tk.ttype),tk.lineno());
			}
			
			

		}
}
