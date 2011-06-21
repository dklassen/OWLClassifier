
package org.semanticscience.dumontierlab.lib;

public class Token {
	public static final int INVALID_TYPE = 0;
    public static final int EOF_TYPE = -1;

    public String text;
    public int type;
    public int line;

    public Token(int type, String text,Integer line) {
        this.type = type;
        this.text = text;
        this.line = line;
    }

    public String toString() {
        return "["+text+":"+type+"]";
    }
}