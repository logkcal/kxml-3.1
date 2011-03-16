/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */
 

package org.kxml3.io;

import java.io.*;

import org.kxml3.XmlWriterEngine;
//import org.xmlpull.v1.*;

public class KXmlWriter implements XmlWriterEngine {

    //    static final String UNDEFINED = ":";

    private Writer writer;

    private boolean pending;
    private int auto;
    private int depth;

    private String[] elementStack = new String[12];
    //prefix/name
    //prefix/nsp; both empty are ""
    private boolean[] indent = new boolean[4];
    private boolean unicode;
    private String encoding;

    
    private final void check(boolean close) throws IOException {
        if (!pending)
            return;

        depth++;
        pending = false;

        if (indent.length <= depth) {
            boolean[] hlp = new boolean[depth + 4];
            System.arraycopy(indent, 0, hlp, 0, depth);
            indent = hlp;
        }
        indent[depth] = indent[depth - 1];

   
        writer.write(close ? " />" : ">");
    }

    private final void writeEscaped(String s, int quot)
        throws IOException {

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            	case '\n':
            	case '\r':
            	case '\t':
            		if(quot == -1) 
            			writer.write(c);
            		else 
            			writer.write("&#"+((int) c)+';');
            		break;
                case '&' :
                    writer.write("&amp;");
                    break;
                case '>' :
                    writer.write("&gt;");
                    break;
                case '<' :
                    writer.write("&lt;");
                    break;
                case '"' :
                case '\'' :
                    if (c == quot) {
                        writer.write(
                            c == '"' ? "&quot;" : "&apos;");
                        break;
                    }
                default :
                	//if(c < ' ')
					//	throw new IllegalArgumentException("Illegal control code:"+((int) c));

                    if (c >= ' ' && c !='@' && (c < 127 || unicode))
                        writer.write(c);
                    else
                        writer.write("&#" + ((int) c) + ";");

            }
        }
    }

    /*
    	private final void writeIndent() throws IOException {
    		writer.write("\r\n");
    		for (int i = 0; i < depth; i++)
    			writer.write(' ');
    	}*/

    public void docdecl(String dd) throws IOException {
        writer.write("<!DOCTYPE");
        writer.write(dd);
        writer.write(">");
    }

    public void endDocument() throws IOException {
        while (depth > 0) {
            writeEndElement(
                elementStack[depth * 2 - 2],
                elementStack[depth * 2 - 1]);
        }
        flush();
    }

    public void entityRef(String name) throws IOException {
        check(false);
        writer.write('&');
        writer.write(name);
        writer.write(';');
    }

    public boolean getFeature(String name) {
        //return false;
        return (
            "http://xmlpull.org/v1/doc/features.html#indent-output"
                .equals(
                name))
            ? indent[depth]
            : false;
    }


    public Object getProperty(String name) {
        throw new RuntimeException("Unsupported property");
    }

    public void ignorableWhitespace(String s)
        throws IOException {
        writeCharacters(s);
    }

    public void setFeature(String name, boolean value) {
        if ("http://xmlpull.org/v1/doc/features.html#indent-output"
            .equals(name)) {
            indent[depth] = value;
        }
        else
            throw new RuntimeException("Unsupported Feature");
    }

    public void setProperty(String name, Object value) {
        throw new RuntimeException(
            "Unsupported Property:" + value);
    }


    public void setOutput(Writer writer) {
        this.writer = writer;

        // elementStack = new String[12]; //nsp/prefix/name
        //nspCounts = new int[4];
        //nspStack = new String[8]; //prefix/nsp
        //indent = new boolean[4];

        pending = false;
        auto = 0;
        depth = 0;

        unicode = false;
    }

    public void setOutput(OutputStream os, String encoding)
        throws IOException {
        if (os == null)
            throw new IllegalArgumentException();
        setOutput(
            encoding == null
                ? new OutputStreamWriter(os)
                : new OutputStreamWriter(os, encoding));
        this.encoding = encoding;
        if (encoding != null
            && encoding.toLowerCase().startsWith("utf"))
            unicode = true;
    }

    public void startDocument(
        String encoding,
        Boolean standalone)
        throws IOException {
        writer.write("<?xml version='1.0' ");

        if (encoding != null) {
            this.encoding = encoding;
            if (encoding.toLowerCase().startsWith("utf"))
                unicode = true;
        }

        if (this.encoding != null) {
            writer.write("encoding='");
            writer.write(this.encoding);
            writer.write("' ");
        }

        if (standalone != null) {
            writer.write("standalone='");
            writer.write(
                standalone.booleanValue() ? "yes" : "no");
            writer.write("' ");
        }
        writer.write("?>");
    }

    public void writeStartElement(String prefix, String name)
        throws IOException {
        check(false);

        //        if (namespace == null)
        //            namespace = "";

        if (indent[depth]) {
            writer.write("\r\n");
            for (int i = 0; i < depth; i++)
                writer.write("  ");
        }

        int esp = depth * 2;

        if (elementStack.length < esp + 2) {
            String[] hlp = new String[elementStack.length + 12];
            System.arraycopy(elementStack, 0, hlp, 0, esp);
            elementStack = hlp;
        }


        elementStack[esp++] = prefix;
        elementStack[esp] = name;

        writer.write('<');
        if (prefix != null && !"".equals(prefix)) {
            writer.write(prefix);
            writer.write(':');
        }

        writer.write(name);

        pending = true;
    }

    public void writeAttribute(
        String prefix,
        String name,
        String value)
        throws IOException {
        if (!pending)
            throw new IllegalStateException("illegal position for attribute");


        writer.write(' ');
        if (prefix != null && !"".equals(prefix)) {
            writer.write(prefix);
            writer.write(':');
        }
        writer.write(name);
        writer.write('=');
        char q = value.indexOf('"') == -1 ? '"' : '\'';
        writer.write(q);
        writeEscaped(value, q);
        writer.write(q);
    }

    public void flush() throws IOException {
        check(false);
        writer.flush();
    }
    /*
    	public void close() throws IOException {
    		check();
    		writer.close();
    	}
    */
    
    public void writeEndElement() throws IOException{
        int d = pending ? depth : depth -1;
        writeEndElement(elementStack[d*2], elementStack[d*2+1]);
                
    }
    
    public void writeEndElement(String prefix, String name)
        throws IOException {

        if (!pending)
            depth--;
        //        if (namespace == null)
        //          namespace = "";

        if ((prefix == null
            && elementStack[depth * 2] != null)
            || (prefix != null
                && !prefix.equals(elementStack[depth * 2]))
            || !elementStack[depth * 2 + 1].equals(name))
            throw new IllegalArgumentException("</"+name+"> does not match start");

        if (pending) {
            check(true);
            depth--;
        }
        else {
            if (indent[depth + 1]) {
                writer.write("\r\n");
                for (int i = 0; i < depth; i++)
                    writer.write("  ");
            }

            writer.write("</");
            //String prefix = elementStack[depth * 3 + 1];
            if (prefix != null && !"".equals(prefix)) {
                writer.write(prefix);
                writer.write(':');
            }
            writer.write(name);
            writer.write('>');
        }
    }


    public String getName() {
        return getDepth() == 0 ? null : elementStack[getDepth() * 2 - 1];
    }

    public int getDepth() {
        return pending ? depth + 1 : depth;
    }

    public void writeCharacters(String text) throws IOException {
        check(false);
        indent[depth] = false;
        writeEscaped(text, -1);
    }

    public void writeCharacters(char[] text, int start, int len)
        throws IOException {
        writeCharacters(new String(text, start, len));
    }

    public void cdsect(String data) throws IOException {
        check(false);
        writer.write("<![CDATA[");
        writer.write(data);
        writer.write("]]>");
    }

    public void comment(String comment) throws IOException {
        check(false);
        writer.write("<!--");
        writer.write(comment);
        writer.write("-->");
    }

    public void processingInstruction(String pi)
        throws IOException {
        check(false);
        writer.write("<?");
        writer.write(pi);
        writer.write("?>");
    }

	/**
	 * 
	 */
	public void close() throws IOException {
		writer.close();
	}
}
