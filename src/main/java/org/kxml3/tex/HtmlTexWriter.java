/*
 * Created on Dec 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.kxml3.tex;
import java.io.*;


import org.kxml3.XmlWriterEngine;

import java.util.Hashtable;

/**
 * A class that is able to write XHTML to an XML writer 
 * and a latex file simeloutaniously. Both targets are optional.
 */

public class HtmlTexWriter implements XmlWriterEngine {

	static Hashtable elements = new Hashtable();

	static void register(String html, String texStart, String texEnd) {
		elements.put(html, new String[] { texStart, texEnd });
	}

	{
		register("dl", "\n\\begin{description}", "\n\\end{description}");
		register("tex-br", " \\\\ \n", "");
		register("br", " \\\\ \n", "");
		register("dt", "\n\n\\item[", "] ~\n");
		register("em", "{\\tt ", "}");
		/*		register("h1",	"\n\\begin{chapter}", "\n\\end{chapter}");
				register("h2", "\n\n\n\\section{",  "}\n\n");
				register("h3",  "\n\n\n\\subsection{",  "}\n\n");
				register("h4",  "\n\n\n\\subsubsection{",  "}\n\n");
				register("h5",  "\n\n\n\\paragraph{", "}\n\n");*/
		register("p", "\n", "\n");
	};

	Writer tex;
	XmlWriterEngine html;
	int depth;
	int headingLevelOffset;

	public HtmlTexWriter(XmlWriterEngine html, Writer tex) {
		this.tex = tex;
		this.html = html;
	}

	public void setFeature(String arg0, boolean arg1) {
		throw new RuntimeException("Unsupported Method!");
	}

	public boolean getFeature(String arg0) {
		return false;
	}

	public void setProperty(String arg0, Object arg1) {
		throw new RuntimeException("Unsupported Method!");
	}

	public Object getProperty(String arg0) {
		return null;
	}

	public void setHeadingLevelOffset(int i) {
		headingLevelOffset = i;
	}

	public void setOutput(OutputStream arg0, String arg1) {
		throw new RuntimeException("Unsupported Method!");
	}

	public void setOutput(Writer arg0) {
		throw new RuntimeException("Unsupported Method!");
	}

	public void startDocument(String arg0, Boolean arg1) throws IOException {
	}

	public void endDocument() {
	}

	public void setPrefix(String arg0, String arg1)
		throws IOException, IllegalArgumentException, IllegalStateException {
	}

	public String getPrefix(String arg0, boolean arg1)
		throws IllegalArgumentException {
		return null;
	}

	public int getDepth() {
		return depth;
	}

	public String getNamespace() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.xmlpull.v1.XmlSerializer#getName()
	 */
	public String getName() {
		throw new RuntimeException("NYI");
	}

	/* (non-Javadoc)
	 * @see org.xmlpull.v1.XmlSerializer#startTag(java.lang.String, java.lang.String)
	 */
	
	public void writeStartElement(String prefix, String name) 
		throws IOException, IllegalArgumentException, IllegalStateException {

		depth++;

		if (html != null && !name.startsWith("tex-"))
			html.writeStartElement(prefix, name);

		if (tex != null) {
			name = name.toLowerCase();

			String[] trans = (String[]) elements.get(name);
			if (trans != null)
				tex.write(trans[0]);
			else if (name.startsWith("h") && name.length() == 2) {
				tex.write("\n\n\n\\");
				switch (name.charAt(1) - 48 + headingLevelOffset) {
					case 1 :
						tex.write("chapter{");
						break;
					case 2 :
						tex.write("section{");
						break;
					case 3 :
						tex.write("subsection{");
						break;
					case 4 :
						tex.write("subsubsection{");
						break;
					default :
						tex.write("paragraph{");
				}
			}
		}
	}

	public void writeAttribute(String arg0, String arg1, String arg2)
		throws IOException, IllegalArgumentException, IllegalStateException {

		//	if(arg1.equals("latex")){
		//		
		//	}

		if (html != null)
			html.writeAttribute(arg0, arg1, arg2);

	}

	public void writeEndElement(String prefix, String name) 
		throws IOException, IllegalArgumentException, IllegalStateException {

		depth--;

		if (html != null && !name.startsWith("tex-"))
			html.writeEndElement(prefix, name);

		if (tex != null) {
			String[] trans = (String[]) elements.get(name.toLowerCase());
			if (trans != null)
				tex.write(trans[1]);
			else if (name.startsWith("h") && name.length() == 2) {
				tex.write("}\n\n");
			}
		}
	}

	public void writeCharacters(String text) 
		throws IOException, IllegalArgumentException, IllegalStateException {

		if (html != null)
			html.writeCharacters(text);

		if (tex != null) {
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				switch (c) {
					case '%': tex.write("\\%");	break;	
					case '\n':
					case '\r': tex.write(" "); break;
					case ']': tex.write("{]}"); break;
					case '|': tex.write("$|$");break;
					case '<' :tex.write("$<$"); break;
					case '>' : tex.write("$>$"); break;
					default :
						tex.write(c);
				}
			}
		}
	}

	

	public void entityRef(String arg0) {

		throw new UnsupportedOperationException();
	}

	public void processingInstruction(String arg0) {
		throw new UnsupportedOperationException();
	}

	public void comment(String arg0)
		throws IOException, IllegalArgumentException, IllegalStateException {

		if (html != null)
			comment(arg0);

		if (tex != null)
			tex.write("%" + arg0 + "\n");
	}

	

	public void flush() throws IOException {
		if (tex != null)
			tex.flush();
/*		if (html != null)
			html.flush(); */

	}

		


}
