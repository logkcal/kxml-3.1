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


package org.kxml3.wap;

import java.io.*;
import java.util.*;

import org.kxml3.XmlWriterEngine;


// TODO: make some of the "direct" WBXML token writing methods public??

/** 
 * A class for writing WBXML. 
 *  
 */



public class WbxmlWriter implements XmlWriterEngine {

    Hashtable stringTable = new Hashtable();

    OutputStream out;

    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    ByteArrayOutputStream stringTableBuf = new ByteArrayOutputStream();

    String encoding="UTF8";
    int encodingValue;

    String pending;
    int depth;
    String name;
    String namespace;
    List<String> attributes = new ArrayList<String>();

    private final WbxmlCodeLookup codeLookup;

    private int currentAttrPage = 0;
    private int currentTagPage = 0;
    private boolean startWritten = false;

    public WbxmlWriter(WbxmlCodeLookup codeLookup) {
        this.codeLookup = codeLookup;
    }


    @Override
    public void writeAttribute(String prefix, String name, String value) {
        attributes.add(prefix == null ? name : prefix+':'+name);
        attributes.add(value);
    }


    public void cdsect (String cdsect) throws IOException{
        writeCharacters (cdsect);
    }
    
    public int getDepth() {
    	return depth;
    }


    public boolean getFeature (String name) {
        return false;
    }
    
    public String getNamespace() {
            throw new RuntimeException("NYI");
    }

    public String getName() {
            throw new RuntimeException("NYI");
    }

    public String getPrefix(String nsp, boolean create) {
        throw new RuntimeException ("NYI");
    }
    
    
    public Object getProperty (String name) {
        return null;
    }

    public void ignorableWhitespace (String sp) {
    }
    

    public void endDocument() throws IOException {
        while ( depth > 0) {
            writeEndElement(null, null);
        }

        startDocument(null, false);

        writeInt(out, stringTableBuf.size());

        // write StringTable

        out.write(stringTableBuf.toByteArray());

        out.write(buf.toByteArray());

        // ready!

        out.flush();
    }


    /** ATTENTION: flush cannot work since Wbxml documents require
    need buffering. Thus, this call does nothing. */

    public void flush() {
    }


    public void checkPending(boolean degenerated) throws IOException {
        if (pending == null)
            return;

        int len = attributes.size();

        WbxmlCode code =  codeLookup.lookupTagByName(pending);

        // if no entry in known table, then add as literal
        if (code == null) {
            buf.write(
                len == 0
                    ? (degenerated ? Wbxml.LITERAL : Wbxml.LITERAL_C)
                    : (degenerated ? Wbxml.LITERAL_A : Wbxml.LITERAL_AC));

            writeStrT(pending);
        }
        else {
            if(code.getPage() != currentTagPage){
                    currentTagPage= code.getPage();
                    buf.write(Wbxml.SWITCH_PAGE);
                    buf.write(currentTagPage);
            }
        	
            buf.write(
                len == 0
                    ? (degenerated ? code.getCode() : code.getCode() | 64)
                    : (degenerated
                        ? code.getCode() | 128
                        : code.getCode() | 192));

        }

        for (int i = 0; i < len;i++) {
            WbxmlCode attrCode = codeLookup.lookupAttributeByName( attributes.get(i));
            
            if (attrCode == null) {
                buf.write(Wbxml.LITERAL);
                writeStrT((String) attributes.get(i));
            }
            else {
                if(attrCode.getPage() != currentAttrPage){
                        currentAttrPage = attrCode.getPage();
                        buf.write(0);
                        buf.write(currentAttrPage);
                }
                buf.write(attrCode.getCode());
            }

            WbxmlCode valueCode = codeLookup.lookupAttributeValueByName(attributes.get(++i));
            if (valueCode == null) {
                buf.write(Wbxml.STR_I);
                writeStrI(buf, (String) attributes.get(i));
            }
            else {
                if(valueCode.getPage() != currentAttrPage){
                        currentAttrPage = valueCode.getPage();
                        buf.write(0);
                        buf.write(currentAttrPage);
                }
                buf.write(valueCode.getCode());
            }
        }

        if (len > 0)
            buf.write(Wbxml.END);

        pending = null;
        attributes.clear();
    }


    public void processingInstruction(String pi) {
        throw new RuntimeException ("PI NYI");
    }




    public void setOutput (Writer writer) {
        throw new RuntimeException ("Wbxml requires an OutputStream!");
    }

    public void setOutput (OutputStream out, String encoding) throws IOException {

        if ( encoding == null ) {
            encoding = "UTF8";
        }

        this.encoding = encoding;

        if ( encoding.equals("UTF8")) {
            encodingValue = Wbxml.ENCODING_UTF8;
        } else if ( encoding.equals("US-ASCII")) {
            encodingValue = Wbxml.ENCODING_US_ASCII;
        } else if ( encoding.equals("ISO-8859-1")) {
            encodingValue = Wbxml.ENCODING_ISO_8859_1;
        } else {
            throw new IllegalArgumentException(encoding + " is not a supported encoding, only UTF8, US-ASCII, and ISO-8859-1 are allowed");
        }
        
        this.out = out;

        buf = new ByteArrayOutputStream();
        stringTableBuf = new ByteArrayOutputStream();

        // ok, write header 
    }


    public void setProperty(String property, Object value) {
        throw new IllegalArgumentException ("unknown property "+property);
    }

    
    public void startDocument(String s, Boolean b) throws IOException{
        if ( !startWritten ) {
            out.write(0x03); // version
            out.write(0x01); // unknown or missing public identifier
            out.write(encodingValue);
            startWritten = true;
        }
    }


    public void writeStartElement(String prefix, String name) throws IOException {
        checkPending(false);
        if (prefix != null && prefix.length() != 0)
            pending = prefix+":"+name;
        else
        	pending = name;

        depth++;
		
    }

    @Override
    public void writeCharacters(String text) throws IOException {
        checkPending(false);

        //check for entities in characters and print those


        buf.write(Wbxml.STR_I);
        writeStrI(buf, text);
    }
    
    

    public void writeEndElement(String prefix, String name) throws IOException {

//        current = current.prev;

        if (pending != null)
            checkPending(true);
        else
            buf.write(Wbxml.END);

		depth--;

    }

    /** currently ignored! */

    public void writeLegacy(int type, String data) {
    }

    // ------------- internal methods --------------------------

    static void writeInt(OutputStream out, int i) throws IOException {
        byte[] buf = new byte[5];
        int idx = 0;

        do {
            buf[idx++] = (byte) (i & 0x7f);
            i = i >> 7;
        }
        while (i != 0);

        while (idx > 1) {
            out.write(buf[--idx] | 0x80);
        }
        out.write(buf[0]);
    }

    void writeStrI(OutputStream out, String s) throws IOException {
        out.write(s.getBytes(encoding));
        out.write(0x00);
    }

    void writeStrT(String s) throws IOException {

        Integer idx = (Integer) stringTable.get(s);

        if (idx == null) {
            idx = new Integer(stringTableBuf.size());
            stringTable.put(s, idx);
            writeStrI(stringTableBuf, s);
            stringTableBuf.flush();
        }

        writeInt(buf, idx.intValue());
    }

}
