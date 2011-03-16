/* Copyright (c) 2002,2003,2004 Stefan Haustein, Oberhausen, Rhld., Germany
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

// Contributors: Bjorn Aadland, Chris Bartley, Nicola Fankhauser, 
//               Victor Havin,  Christian Kurzke, Bogdan Onoiu,
//               Jain Sanjay, David Santoro.

package org.kxml3.wap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Stack;

import org.kxml3.XmlReaderEngine;




public class WbxmlReader implements XmlReaderEngine {

    String[] TYPES = {"???", "START_ELEMENT", "END_ELEMENT", "PROCESSING_INSTRUCTION",
            "CHARACTERS","COMMENT","SPACE","START_DOCUMENT",
            "END_DOCUMENT", "ENTITY_REFERENCE", "ATTRIBUTE", "DTD",
            "CDATA", "NAMESPACE","NOTATION_DECLARATION","ENTITY_DECLARATION"};
    
    public static final int WAP_EXTENSION = 64;

    static final private String ILLEGAL_TYPE =
        "Wrong event type";

    //-------------------------------------------------------------
    // Private variables
    //-------------------------------------------------------------

    //input & lookup objects
    private InputStream in;
    private final WbxmlCodeLookup codeLookup;

    //document information
    int version;
    int publicIdentifierId;
    String encoding;
    private String stringTable;

    /** Parser state variables **/
    private String prefix;
    private String name;
    private String text;
    private int currentTagPage;
    private int currentAttributePage;
    private int depth;
    private Stack<WbxmlTag> elementStack = new Stack<WbxmlTag>();
    private int attributeCount;
    private String[] attributes = new String[16];
    private int nextId = -2;

    private Object wapExtensionData;
    private int wapExtensionCode;

    private int type;

    private boolean degenerated;
    private boolean isWhitespace;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------
    public WbxmlReader(WbxmlCodeLookup codeLookup) {
        this.codeLookup = codeLookup;
    }

    //-------------------------------------------------------------
    // Getters
    //-------------------------------------------------------------
   
    public String getInputEncoding() {
        return encoding;
    }

    public Object getProperty(String property) {
        return null;
    }

    public int getDepth() {
        return depth;
    }
    
    public String getPositionDescription() {

        StringBuffer buf =
            new StringBuffer(
                type < TYPES.length ? TYPES[type] : "unknown");
        buf.append(' ');

        if (type == START_ELEMENT || type == END_ELEMENT) {
            if (degenerated)
                buf.append("(empty) ");
            buf.append('<');
            if (type == END_ELEMENT)
                buf.append('/');

            if (prefix != null)
                buf.append(prefix + ":");
            buf.append(name);

            int cnt = attributeCount << 2;
            for (int i = 0; i < cnt; i += 4) {
                buf.append(' ');
                if (attributes[i + 1] != null)
                    buf.append(
                        "{"
                            + attributes[i]
                            + "}"
                            + attributes[i
                            + 1]
                            + ":");
                buf.append(
                    attributes[i
                        + 2]
                        + "='"
                        + attributes[i
                        + 3]
                        + "'");
            }

            buf.append('>');
        }
        else if (type == SPACE);
        else if (type != CHARACTERS)
            buf.append(getText());
        else if (isWhitespace)
            buf.append("(whitespace)");
        else {
            String text = getText();
            if (text.length() > 16)
                text = text.substring(0, 16) + "...";
            buf.append(text);
        }

        return buf.toString();
    }

    public int getLineNumber() {
        return -1;
    }

    @Override
    public int getColumnNumber() {
        return -1;
    }

    //-------------------------------------------------------------
    // Implementation
    //-------------------------------------------------------------

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getLocalName() {
        return name;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    public boolean isEmptyElementTag() {
        if (type != START_ELEMENT)
            exception(ILLEGAL_TYPE);
        return degenerated;
    }

    @Override
    public int getAttributeCount() {
        return attributeCount;
    }

    @Override
    public String getAttributeLocalName(int index) {
        if (index >= attributeCount)
            throw new IndexOutOfBoundsException();
        return attributes[(index << 2) + 2];
    }

    @Override
    public String getAttributePrefix(int index) {
        if (index >= attributeCount)
            throw new IndexOutOfBoundsException();
        return attributes[(index << 2) + 1];
    }

    @Override
    public String getAttributeValue(int index) {
        if (index >= attributeCount)
            throw new IndexOutOfBoundsException();
        return attributes[(index << 2) + 3];
    }

    @Override
    public String getAttributeValue(
        String namespace,
        String name) {

        for (int i = (attributeCount << 2) - 4;
            i >= 0;
            i -= 4) {
            if (attributes[i + 2].equals(name)
                && (namespace == null
                    || attributes[i].equals(namespace)))
                return attributes[i + 3];
        }

        return null;
    }

    @Override
    public int getEventType() {
        return type;
    }

    @Override
    public int nextTag() throws IOException {

        do {
            next();
        } while ( type == SPACE);

        if (type != END_ELEMENT && type != START_ELEMENT)
            exception("unexpected type");

        return type;
    }


    @Override
    public String getElementText() throws IOException {
        if (type != START_ELEMENT)
            exception("precondition: START_ELEMENT");
        StringBuffer buf = new StringBuffer();
        while(next() != END_ELEMENT) {
            if(type == CHARACTERS || type == ENTITY_REFERENCE){
                buf.append(getText());
            }
        }

        return buf.toString();
    }



    public void require(int type, String prefix, String name)
        throws IOException {

        if (type != this.type
            || (prefix != null && !prefix.equals(getPrefix()))
            || (name != null && !name.equals(getLocalName())))
            exception(
                "expected: " + TYPES[type] + prefix + ":"+ name);
    }


    public void setInput(Reader reader)  {
        exception("InputStream required");
    }

    public void setInput(InputStream in, String enc) throws IOException {
        this.in = in;
        readPreamble();
    }

    public void close() throws IOException{
        in.close();
    }

    public String getEncoding() {
        return encoding;
    }

  
    public void setProperty(String property, Object value){
        exception("unsupported property: " + property);
    }

    @Override
    public int next()
        throws IOException {

        isWhitespace = true;

        String s;

        if (type == END_ELEMENT) {
            elementStack.pop();
            depth--;
        }

        if (degenerated) {
            type = END_ELEMENT;
            degenerated = false;
            return type;
        }

        text = null;
        prefix = null;
        name = null;

        int id = peekId ();
        while(id == Wbxml.SWITCH_PAGE){
        	nextId = -2;
		selectTagPage(readByte());
		id = peekId();
        }
        nextId = -2;

        switch (id) {
            case -1 :
                type = END_DOCUMENT;
                break;

            case Wbxml.END :
                {
                    type = END_ELEMENT;

                    prefix = elementStack.peek().prefix;
                    name = elementStack.peek().name;
                }
                break;

            case Wbxml.ENTITY :
                {
                    type = ENTITY_REFERENCE;
                    char c = (char) readInt();
                    text = "&" + c;
                    name = "&#" + ((int) c) + ";";
                }

                break;

            case Wbxml.STR_I :
                type = CHARACTERS;
                text = readStrI();
                break;

            case Wbxml.EXT_I_0 :
            case Wbxml.EXT_I_1 :
            case Wbxml.EXT_I_2 :
            case Wbxml.EXT_T_0 :
            case Wbxml.EXT_T_1 :
            case Wbxml.EXT_T_2 :
            case Wbxml.EXT_0 :
            case Wbxml.EXT_1 :
            case Wbxml.EXT_2 :
            case Wbxml.OPAQUE :
                parseWapExtension(id);
                break;

            case Wbxml.PI :
                throw new RuntimeException("PI curr. not supp.");
                // readPI;
                // break;

            case Wbxml.STR_T :
                {
                    type = CHARACTERS;
                    int pos = readInt();
                    int end = stringTable.indexOf('\0', pos);
                    text = stringTable.substring(pos, end);
                }
                break;

            default :
                parseElement(id);
        }

        return type;
    }

    //-------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------

    private final void exception(String desc){
        throw new RuntimeException(desc + " Position: "+getPositionDescription());
    }


    private void selectTagPage(int pageNum) {
        if ( ! codeLookup.verifyTagPage(pageNum) ) {
            exception("Code Page "+pageNum+" undefined!");
        }
        currentTagPage = pageNum;
    }

    private void selectAttributePage(int pageNum) {
        if ( ! codeLookup.verifyAttributePage(pageNum) ) {
            exception("Attribute Code Page: " + pageNum + " undefined!");
        }
        currentAttributePage = pageNum;
    }

    private void parseWapExtension(int id)
        throws IOException {

        type = WAP_EXTENSION;
        wapExtensionCode = id;

        switch (id) {
            case Wbxml.EXT_I_0 :
            case Wbxml.EXT_I_1 :
            case Wbxml.EXT_I_2 :
                wapExtensionData = readStrI();
                break;

            case Wbxml.EXT_T_0 :
            case Wbxml.EXT_T_1 :
            case Wbxml.EXT_T_2 :
                wapExtensionData = new Integer(readInt());
                break;

            case Wbxml.EXT_0 :
            case Wbxml.EXT_1 :
            case Wbxml.EXT_2 :
                break;

            case Wbxml.OPAQUE :
                {
                    int len = readInt();
                    byte[] buf = new byte[len];

                    for (int i = 0;
                        i < len;
                        i++) // enhance with blockread!
                        buf[i] = (byte) readByte();

                    wapExtensionData = buf;
                } // case OPAQUE
            	break;
            	
            default:
                exception("illegal id: "+id);
        } // SWITCH
    }

    private void readAttr() throws IOException {

        int id = readByte();
        int i = 0;

        while (id != 1) {

            while (id == Wbxml.SWITCH_PAGE) {
                selectAttributePage(readByte());
                id = readByte();
            } 
        	
            String name = resolveAttributeId(id);
            StringBuffer value;

            int cut = name.indexOf('=');

            //start table item could look like:
            //ITEM (we add = value())
            //ITEM=prefix (we prepend the prefix)
            if (cut == -1)
                value = new StringBuffer();
            else {
                value =
                    new StringBuffer(name.substring(cut + 1));
                name = name.substring(0, cut);
            }

            id = readByte();
            while (id > 128
            	|| id == Wbxml.SWITCH_PAGE
                || id == Wbxml.ENTITY
                || id == Wbxml.STR_I
                || id == Wbxml.STR_T
                || (id >= Wbxml.EXT_I_0 && id <= Wbxml.EXT_I_2)
                || (id >= Wbxml.EXT_T_0 && id <= Wbxml.EXT_T_2)) {

                switch (id) {
                    case Wbxml.SWITCH_PAGE :
                        selectAttributePage(readByte());
                        break;
                	
                    case Wbxml.ENTITY :
                        value.append((char) readInt());
                        break;

                    case Wbxml.STR_I :
                        value.append(readStrI());
                        break;

                    case Wbxml.EXT_I_0 :
                    case Wbxml.EXT_I_1 :
                    case Wbxml.EXT_I_2 :
                    case Wbxml.EXT_T_0 :
                    case Wbxml.EXT_T_1 :
                    case Wbxml.EXT_T_2 :
                    case Wbxml.EXT_0 :
                    case Wbxml.EXT_1 :
                    case Wbxml.EXT_2 :
                    case Wbxml.OPAQUE :

                        throw new RuntimeException("wap extension in attr not supported yet");

                        /*
                                                ParseEvent e = parseWapExtension(id);
                                                if (!(e.getType() != Xml.TEXT
                                                    && e.getType() != Xml.WHITESPACE))
                                                    throw new RuntimeException("parse WapExtension must return Text Event in order to work inside Attributes!");
                        
                                                value.append(e.getText());
                        
                                                //value.append (handleExtension (id)); // skip EXT in ATTR
                                                //break;
                        */

                    case Wbxml.STR_T :
                        value.append(readStrT());
                        break;

                    default :
                        value.append(
                            resolveAttributeValueId(id));
                }

                id = readByte();
            }

            attributes = ensureCapacity(attributes, i + 4);

            attributes[i++] = "";
            attributes[i++] = null;
            attributes[i++] = name;
            attributes[i++] = value.toString();
            
            attributeCount++;
        }
    }

    private int peekId () throws IOException {
        if (nextId == -2) {
            nextId = in.read();
        }
        return nextId;
    }

    private String resolveAttributeId(int id) throws IOException {
        int idx = (id & 0x07f) - 5;
        if (idx == -1) {
            return readStrT();
        }

        return codeLookup.lookupAttributeCode(currentAttributePage, id & 0x07f);
    }

    private String resolveAttributeValueId(int id) throws IOException {
        int idx = (id & 0x07f) - 5;
        if (idx == -1) {
            return readStrT();
        }

        return codeLookup.lookupAttributeValueCode(currentAttributePage, id & 0x07f);
    }

    private String resolveTagId(int id) throws IOException {
        int idx = (id & 0x07f) - 5;
        if (idx == -1) {
            return readStrT();
        }

        return codeLookup.lookupTagCode(currentTagPage, id & 0x07f);
    }

    private void parseElement(int id)
        throws IOException {

	type = START_ELEMENT;
        name = resolveTagId( id & 0x03f);

	attributeCount = 0;
        if ((id & 128) != 0) {
            readAttr();
        }

        degenerated = (id & 64) == 0;

        depth++;
        elementStack.push( new WbxmlTag(prefix,name));
    }

    private final String[] ensureCapacity( String[] arr, int required) {
        
        if (arr.length >= required)
            return arr;
        String[] bigger = new String[required + 16];
        System.arraycopy(arr, 0, bigger, 0, arr.length);
        return bigger;
    }

    private int readByte() throws IOException {
        int i = in.read();
        if (i == -1)
            throw new IOException("Unexpected EOF");
        return i;
    }

    private int readInt() throws IOException {
        int result = 0;
        int i;

        do {
            i = readByte();
            result = (result << 7) | (i & 0x7f);
        }
        while ((i & 0x80) != 0);

        return result;
    }

    private String readStrI() throws IOException {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        boolean wsp = true;
        while (true) {
            int i = in.read();
            if (i == -1)
                throw new IOException("Unexpected EOF");
            if (i == 0)
                break;
            if (i > 32)
                wsp = false;
            outBytes.write(i);
        }
        isWhitespace = wsp;
        return new String(outBytes.toByteArray(),encoding);
    }

    private String readStrT() throws IOException {
        int pos = readInt();
        int end = stringTable.indexOf('\0', pos);

        return stringTable.substring(pos, end);
    }

    private void readPreamble() throws IOException {
        version = readByte();
        publicIdentifierId = readInt();

        if (publicIdentifierId == 0)
            readInt();

        int charSet = readInt();

        switch (charSet) {
            case Wbxml.ENCODING_ISO_8859_1:
                encoding = "ISO-8859-1";
                break;
            case Wbxml.ENCODING_US_ASCII:
                encoding = "US-ASCII";
                break;
            case Wbxml.ENCODING_UTF8:
                encoding = "UTF8";
                break;
            default:
                throw new IllegalArgumentException("The encoding discovered is not supported=> encoding code:" + charSet);
        }

        int strTabSize = readByte();

        byte[] stringTableBytes = new byte[strTabSize];
        in.read(stringTableBytes);


        stringTable = new String(stringTableBytes,encoding);
    }
}
