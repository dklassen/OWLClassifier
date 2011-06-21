package org.semanticscience.dumontierlab.lib;

import java.io.IOException;

public interface TokenStream {
	public Token nextToken() throws IOException;
}
