/*
 * Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.kxml3.io;
import java.io.*;
import java.util.*;
import org.kxml3.XmlReaderEngine;
/**
 * A simple, pull based XML parser. This classe replaces the XmlParser class and
 * the corresponding event classes.
 */
public class KXmlReader implements XmlReaderEngine {
	
	String[] TYPES = {"???", "START_ELEMENT", "END_ELEMENT", "PROCESSING_INSTRUCTION",
					"CHARACTERS","COMMENT","SPACE","START_DOCUMENT",
					"END_DOCUMENT", "ENTITY_REFERENCE", "ATTRIBUTE", "DTD",
					"CDATA", "NAMESPACE","NOTATION_DECLARATION","ENTITY_DECLARATION"};
	
	
	
	private Object location;
	static final private String UNEXPECTED_EOF = "Unexpected EOF";
	static final private String ILLEGAL_TYPE = "Wrong event type";
	static final private int LEGACY = 999;
	static final private int XML_DECL = 998;
	// general
	private String version;
	private Boolean standalone;
	private boolean relaxed;
	private Hashtable entityMap;
	private int depth;
	private String[] elementStack = new String[16];
	// source
	private Reader reader;
	private String encoding;
	private char[] srcBuf;
	private int srcPos;
	private int srcCount;
	private int line;
	private int column;
	// txtbuffer
	private char[] txtBuf = new char[128];
	private int txtPos;
	// Event-related
	private int type;
	//private String text;
	private boolean isWhitespace;

	private String prefix;
	private String name;
	private boolean degenerated;
	private int attributeCount;
	private String[] attributes = new String[16];
	private int stackMismatch = 0;
	private String error;
	/**
	 * A separate peek buffer seems simpler than managing wrap around in the
	 * first level read buffer
	 */
	private int[] peek = new int[2];
	private int peekCount;
	private boolean wasCR;
	private boolean unresolved;
	private boolean token;
	public KXmlReader() {
		srcBuf = new char[Runtime.getRuntime().freeMemory() >= 1048576
				? 8192
				: 128];
	}
	private final boolean isProp(String n1, boolean prop, String n2) {
		if (!n1.startsWith("http://xmlpull.org/v1/doc/"))
			return false;
		if (prop)
			return n1.substring(42).equals(n2);
		else
			return n1.substring(40).equals(n2);
	}
	private final String[] ensureCapacity(String[] arr, int required) {
		if (arr.length >= required)
			return arr;
		String[] bigger = new String[required + 16];
		System.arraycopy(arr, 0, bigger, 0, arr.length);
		return bigger;
	}
	private final void error(String desc) throws IOException {
		if (relaxed) {
			if (error == null)
				error = "ERR: " + desc;
		} else
			exception(desc);
	}
	private final void exception(String desc) throws IOException {
		throw new RuntimeException(desc.length() < 100 ? desc : desc.substring(
				0, 100)
				+ "\n");
	}
	/**
	 * common base for next and nextToken. Clears the state, except from txtPos
	 * and whitespace. Does not set the type variable
	 */
	private final void nextImpl() throws IOException, IOException {
		if (reader == null)
			exception("No Input specified");
		if (type == END_ELEMENT)
			depth--;
		while (true) {
			attributeCount = -1;
			// degenerated needs to be handled before error because of possible
			// processor expectations(!)
			if (degenerated) {
				degenerated = false;
				type = END_ELEMENT;
				return;
			}
			if (error != null) {
				for (int i = 0; i < error.length(); i++)
					push(error.charAt(i));
				//				text = error;
				error = null;
				type = COMMENT;
				return;
			}
			if (relaxed && (stackMismatch > 0 || (peek(0) == -1 && depth > 0))) {
				int sp = (depth - 1) << 2;
				type = END_ELEMENT;

				prefix = elementStack[sp + 1];
				name = elementStack[sp + 2];
				if (stackMismatch != 1)
					error = "missing end tag /" + name + " inserted";
				if (stackMismatch > 0)
					stackMismatch--;
				return;
			}
			prefix = null;
			name = null;

			//            text = null;
			type = peekType();
			switch (type) {
				case ENTITY_REFERENCE :
					pushEntity();
					return;
				case START_ELEMENT :
					parseStartTag(false);
					return;
				case END_ELEMENT :
					parseEndTag();
					return;
				case END_DOCUMENT :
					return;
				case CHARACTERS :
					pushText('<', !token);
					if (depth == 0) {
						if (isWhitespace)
							type = SPACE;
						// make exception switchable for instances.chg... !!!!
						//	else
						//    exception ("text '"+getText ()+"' not allowed outside
						// root element");
					}
					return;
				default :
					type = parseLegacy(token);
					if (type != XML_DECL)
						return;
			}
		}
	}
	private final int parseLegacy(boolean push) throws IOException, IOException {
		String req = "";
		int term;
		int result;
		int prev = 0;
		read(); // <
		int c = read();
		if (c == '?') {
			if ((peek(0) == 'x' || peek(0) == 'X')
					&& (peek(1) == 'm' || peek(1) == 'M')) {
				if (push) {
					push(peek(0));
					push(peek(1));
				}
				read();
				read();
				if ((peek(0) == 'l' || peek(0) == 'L') && peek(1) <= ' ') {
					if (line != 1 || column > 4)
						error("PI must not start with xml");
					parseStartTag(true);
					if (attributeCount < 1 || !"version".equals(attributes[2]))
						error("version expected");
					version = attributes[3];
					int pos = 1;
					if (pos < attributeCount
							&& "encoding".equals(attributes[2 + 4])) {
						encoding = attributes[3 + 4];
						pos++;
					}
					if (pos < attributeCount
							&& "standalone".equals(attributes[4 * pos + 2])) {
						String st = attributes[3 + 4 * pos];
						if ("yes".equals(st))
							standalone = new Boolean(true);
						else if ("no".equals(st))
							standalone = new Boolean(false);
						else
							error("illegal standalone value: " + st);
						pos++;
					}
					if (pos != attributeCount)
						error("illegal xmldecl");
					isWhitespace = true;
					txtPos = 0;
					return XML_DECL;
				}
			}
			/*
			 * int c0 = read (); int c1 = read (); int
			 */
			term = '?';
			result = PROCESSING_INSTRUCTION;
		} else if (c == '!') {
			if (peek(0) == '-') {
				result = COMMENT;
				req = "--";
				term = '-';
			} else if (peek(0) == '[') {
				result = CDATA;
				req = "[CDATA[";
				term = ']';
				push = true;
			} else {
				result = DTD;
				req = "DOCTYPE";
				term = -1;
			}
		} else {
			error("illegal: <" + c);
			return COMMENT;
		}
		for (int i = 0; i < req.length(); i++)
			read(req.charAt(i));
		if (result == DTD)
			parseDoctype(push);
		else {
			while (true) {
				c = read();
				if (c == -1) {
					error(UNEXPECTED_EOF);
					return COMMENT;
				}
				if (push)
					push(c);
				if ((term == '?' || c == term) && peek(0) == term
						&& peek(1) == '>')
					break;
				prev = c;
			}
			if (term == '-' && prev == '-')
				error("illegal comment delimiter: --->");
			read();
			read();
			if (push && term != '?')
				txtPos--;
		}
		return result;
	}
	/** precondition: &lt! consumed */
	private final void parseDoctype(boolean push) throws IOException,
			IOException {
		int nesting = 1;
		boolean quoted = false;
		// read();
		while (true) {
			int i = read();
			switch (i) {
				case -1 :
					error(UNEXPECTED_EOF);
					return;
				case '\'' :
					quoted = !quoted;
					break;
				case '<' :
					if (!quoted)
						nesting++;
					break;
				case '>' :
					if (!quoted) {
						if ((--nesting) == 0)
							return;
					}
					break;
			}
			if (push)
				push(i);
		}
	}
	/* precondition: &lt;/ consumed */
	private final void parseEndTag() throws IOException, IOException {
		read(); // '<'
		read(); // '/'
		name = readName();
		skip();
		read('>');
		int sp = (depth - 1) << 2;
		if (depth == 0) {
			error("element stack empty");
			type = COMMENT;
			return;
		}
		if (!name.equals(elementStack[sp + 3])) {
			error("expected: /" + elementStack[sp + 3] + " read: " + name);
			// become case insensitive in relaxed mode
			int probe = sp;
			while (probe >= 0
					&& !name.toLowerCase().equals(
							elementStack[probe + 3].toLowerCase())) {
				stackMismatch++;
				probe -= 4;
			}
			if (probe < 0) {
				stackMismatch = 0;
				//			text = "unexpected end tag ignored";
				type = COMMENT;
				return;
			}
		}

		prefix = elementStack[sp + 1];
		name = elementStack[sp + 2];
	}
	private final int peekType() throws IOException {
		switch (peek(0)) {
			case -1 :
				return END_DOCUMENT;
			case '&' :
				return ENTITY_REFERENCE;
			case '<' :
				switch (peek(1)) {
					case '/' :
						return END_ELEMENT;
					case '?' :
					case '!' :
						return LEGACY;
					default :
						return START_ELEMENT;
				}
			default :
				return CHARACTERS;
		}
	}
	private final String get(int pos) {
		return new String(txtBuf, pos, txtPos - pos);
	}
	/*
	 * private final String pop (int pos) { String result = new String (txtBuf,
	 * pos, txtPos - pos); txtPos = pos; return result; }
	 */
	private final void push(int c) {
		isWhitespace &= c <= ' ';
		if (txtPos == txtBuf.length) {
			char[] bigger = new char[txtPos * 4 / 3 + 4];
			System.arraycopy(txtBuf, 0, bigger, 0, txtPos);
			txtBuf = bigger;
		}
		txtBuf[txtPos++] = (char) c;
	}
	/** Sets name and attributes */
	private final void parseStartTag(boolean xmldecl) throws IOException,
			IOException {
		if (!xmldecl)
			read();
		name = readName();
		attributeCount = 0;
		while (true) {
			skip();
			int c = peek(0);
			if (xmldecl) {
				if (c == '?') {
					read();
					read('>');
					return;
				}
			} 
            else {
				if (c == '/') {
					degenerated = true;
					read();
					skip();
					read('>');
					break;
				}
				if (c == '>' && !xmldecl) {
					read();
					break;
				}
			}
			if (c == -1) {
				error(UNEXPECTED_EOF);
				//type = COMMENT;
				return;
			}
			String attrName = readName();
			if (attrName.length() == 0) {
				error("attr name expected");
				//type = COMMENT;
				break;
			}
			int i = (attributeCount++) << 2;
			attributes = ensureCapacity(attributes, i + 4);
			attributes[i++] = "";
			attributes[i++] = null;
			attributes[i++] = attrName;
			skip();
			if (peek(0) != '=') {
				error("Attr.value missing f. " + attrName);
				attributes[i] = "1";
			} else {
				read('=');
				skip();
				int delimiter = peek(0);
				if (delimiter != '\'' && delimiter != '"') {
					error("attr value delimiter missing!");
					delimiter = ' ';
				} else
					read();
				int p = txtPos;
				pushText(delimiter, true);
				attributes[i] = get(p);
				txtPos = p;
				if (delimiter != ' ')
					read(); // skip endquote
			}
		}
		int sp = depth++ << 2;
		elementStack = ensureCapacity(elementStack, sp + 4);
		elementStack[sp + 3] = name;
		
		int cut = name.indexOf(':');
        if(cut != -1){
        	prefix = name.substring(0, cut);
            name = name.substring(cut+1);
        }
        else{
        	prefix = null;
        }
		elementStack[sp + 1] = prefix;
		elementStack[sp + 2] = name;
	}
	/**
	 * result: isWhitespace; if the setName parameter is set, the name of the
	 * entity is stored in "name"
	 */
	private final void pushEntity() throws IOException, IOException {
		read(); // &
		int pos = txtPos;
		while (true) {
			int c = read();
			if (c == ';')
				break;
			if (c < 128 && (c < '0' || c > '9') && (c < 'a' || c > 'z')
					&& (c < 'A' || c > 'Z') && c != '_' && c != '-' && c != '#') {
				error("unterminated entity ref");
				//; ends with:"+(char)c);
				if (c != -1)
					push(c);
				return;
			}
			push(c);
		}
		String code = get(pos);
		txtPos = pos;
		if (token && type == ENTITY_REFERENCE)
			name = code;
		if (code.charAt(0) == '#') {
			int c = (code.charAt(1) == 'x' ? Integer.parseInt(
					code.substring(2), 16) : Integer
					.parseInt(code.substring(1)));
			push(c);
			return;
		}
		String result = (String) entityMap.get(code);
		unresolved = result == null;
		if (unresolved) {
			if (!token)
				error("unresolved: &" + code + ";");
		} else {
			for (int i = 0; i < result.length(); i++)
				push(result.charAt(i));
		}
	}
	/**
	 * types: ' <': parse to any token (for nextToken ()) '"': parse to quote ' ':
	 * parse to whitespace or '>'
	 */
	private final void pushText(int delimiter, boolean resolveEntities)
			throws IOException, IOException {
		int next = peek(0);
		int cbrCount = 0;
		while (next != -1 && next != delimiter) { // covers eof, '<', '"'
			if (delimiter == ' ')
				if (next <= ' ' || next == '>')
					break;
			if (next == '&') {
				if (!resolveEntities)
					break;
				pushEntity();
			} else if (next == '\n' && type == START_ELEMENT) {
				read();
				push(' ');
			} else
				push(read());
			if (next == '>' && cbrCount >= 2 && delimiter != ']')
				error("Illegal: ]]>");
			if (next == ']')
				cbrCount++;
			else
				cbrCount = 0;
			next = peek(0);
		}
	}
	private final void read(char c) throws IOException, IOException {
		int a = read();
		if (a != c)
			error("expected: '" + c + "' actual: '" + ((char) a) + "'");
	}
	private final int read() throws IOException {
		int result;
		if (peekCount == 0)
			result = peek(0);
		else {
			result = peek[0];
			peek[0] = peek[1];
		}
		//		else {
		//			result = peek[0];
		//			System.arraycopy (peek, 1, peek, 0, peekCount-1);
		//		}
		peekCount--;
		column++;
		if (result == '\n') {
			line++;
			column = 1;
		}
		return result;
	}
	/** Does never read more than needed */
	private final int peek(int pos) throws IOException {
		while (pos >= peekCount) {
			int nw;
			if (srcBuf.length <= 1)
				nw = reader.read();
			else if (srcPos < srcCount)
				nw = srcBuf[srcPos++];
			else {
				srcCount = reader.read(srcBuf, 0, srcBuf.length);
				if (srcCount <= 0)
					nw = -1;
				else
					nw = srcBuf[0];
				srcPos = 1;
			}
			if (nw == '\r') {
				wasCR = true;
				peek[peekCount++] = '\n';
			} else {
				if (nw == '\n') {
					if (!wasCR)
						peek[peekCount++] = '\n';
				} else
					peek[peekCount++] = nw;
				wasCR = false;
			}
		}
		return peek[pos];
	}
	private final String readName() throws IOException, IOException {
		int pos = txtPos;
		int c = peek(0);
		if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && c != '_'
				&& c != ':' && c < 0x0c0 && !relaxed)
			error("name expected");
		do {
			push(read());
			c = peek(0);
		} while ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
				|| (c >= '0' && c <= '9') || c == '_' || c == '-' || c == ':'
				|| c == '.' || c >= 0x0b7);
		String result = get(pos);
		txtPos = pos;
		return result;
	}
	private final void skip() throws IOException {
		while (true) {
			int c = peek(0);
			if (c > ' ' || c == -1)
				break;
			read();
		}
	}
	//--------------- public part starts here... ---------------
	public void setInput(Reader reader) throws IOException {
		this.reader = reader;
		line = 1;
		column = 0;
		type = START_DOCUMENT;
		name = null;

		degenerated = false;
		attributeCount = -1;
		encoding = null;
		version = null;
		standalone = null;
		if (reader == null)
			return;
		srcPos = 0;
		srcCount = 0;
		peekCount = 0;
		depth = 0;
		entityMap = new Hashtable();
		entityMap.put("amp", "&");
		entityMap.put("apos", "'");
		entityMap.put("gt", ">");
		entityMap.put("lt", "<");
		entityMap.put("quot", "\"");
	}
	
	
	public void setInput(InputStream is, String _enc) throws IOException {
		srcPos = 0;
		srcCount = 0;
		String enc = _enc;
		if (is == null)
			throw new IllegalArgumentException();
		try {
			if (enc == null) {
				// read four bytes
				int chk = 0;
				while (srcCount < 4) {
					int i = is.read();
					if (i == -1)
						break;
					chk = (chk << 8) | i;
					srcBuf[srcCount++] = (char) i;
				}
				if (srcCount == 4) {
					switch (chk) {
						case 0x00000FEFF :
							enc = "UTF-32BE";
							srcCount = 0;
							break;
						case 0x0FFFE0000 :
							enc = "UTF-32LE";
							srcCount = 0;
							break;
						case 0x03c :
							enc = "UTF-32BE";
							srcBuf[0] = '<';
							srcCount = 1;
							break;
						case 0x03c000000 :
							enc = "UTF-32LE";
							srcBuf[0] = '<';
							srcCount = 1;
							break;
						case 0x0003c003f :
							enc = "UTF-16BE";
							srcBuf[0] = '<';
							srcBuf[1] = '?';
							srcCount = 2;
							break;
						case 0x03c003f00 :
							enc = "UTF-16LE";
							srcBuf[0] = '<';
							srcBuf[1] = '?';
							srcCount = 2;
							break;
						case 0x03c3f786d :
							while (true) {
								int i = is.read();
								if (i == -1)
									break;
								srcBuf[srcCount++] = (char) i;
								if (i == '>') {
									String s = new String(srcBuf, 0, srcCount);
									int i0 = s.indexOf("encoding");
									if (i0 != -1) {
										while (s.charAt(i0) != '"'
												&& s.charAt(i0) != '\'')
											i0++;
										char deli = s.charAt(i0++);
										int i1 = s.indexOf(deli, i0);
										enc = s.substring(i0, i1);
									}
									break;
								}
							}
						default :
							if ((chk & 0x0ffff0000) == 0x0FEFF0000) {
								enc = "UTF-16BE";
								srcBuf[0] = (char) ((srcBuf[2] << 8) | srcBuf[3]);
								srcCount = 1;
							} else if ((chk & 0x0ffff0000) == 0x0fffe0000) {
								enc = "UTF-16LE";
								srcBuf[0] = (char) ((srcBuf[3] << 8) | srcBuf[2]);
								srcCount = 1;
							} else if ((chk & 0x0ffffff00) == 0x0EFBBBF00) {
								enc = "UTF-8";
								srcBuf[0] = srcBuf[3];
								srcCount = 1;
							}
					}
				}
			}
			if (enc == null)
				enc = "UTF-8";
			int sc = srcCount;
			setInput(new InputStreamReader(is, enc));
			encoding = _enc;
			srcCount = sc;
		} catch (Exception e) {
			throw new IOException(
					"Invalid stream or encoding: " + e.toString());
		}
	}
	

	public String getInputEncoding() {
		return encoding;
	}
	
	public void defineEntityReplacementText(String entity, String value)
			throws IOException {
		if (entityMap == null)
			throw new RuntimeException(
					"entity replacement text must be defined after setInput!");
		entityMap.put(entity, value);
	}
	
	public Object getProperty(String property) {
		if (isProp(property, true, "xmldecl-version"))
			return version;
		if (isProp(property, true, "xmldecl-standalone"))
			return standalone;
		if (isProp(property, true, "location"))
			return location != null ? location : reader.toString();
		return null;
	}
	
	

	public int getDepth() {
		return depth;
	}
	
	public String getPositionDescription() {
		StringBuffer buf = new StringBuffer(type < TYPES.length
				? TYPES[type]
				: "unknown");
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
					buf.append("{" + attributes[i] + "}" + attributes[i + 1]
							+ ":");
				buf.append(attributes[i + 2] + "='" + attributes[i + 3] + "'");
			}
			buf.append('>');
		} else if (type == SPACE)
			;
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
		buf.append("@" + line + ":" + column);
		buf.append(" in ");
		buf.append(location == null ? reader.toString() : location);
		return buf.toString();
	}
	public int getLineNumber() {
		return line;
	}
	public int getColumnNumber() {
		return column;
	}


	public String getText() {
		switch(type){
			case CHARACTERS:
			case CDATA: 
			case SPACE:
			case ENTITY_REFERENCE:
				return get(0);
		}
		return null;
	}
	
	public char[] getTextCharacters(int[] poslen) {
		if (type >= CHARACTERS) {
			if (type == ENTITY_REFERENCE) {
				poslen[0] = 0;
				poslen[1] = name.length();
				return name.toCharArray();
			}
			poslen[0] = 0;
			poslen[1] = txtPos;
			return txtBuf;
		}
		poslen[0] = -1;
		poslen[1] = -1;
		return null;
	}


	public String getLocalName() {
		return name;
	}
	public String getPrefix() {
		return prefix;
	}
	public boolean isEmptyElementTag() throws IOException {
		if (type != START_ELEMENT)
			exception(ILLEGAL_TYPE);
		return degenerated;
	}
	public int getAttributeCount() {
		return attributeCount;
	}
	public String getAttributeType(int index) {
		return "CDATA";
	}
	public boolean isAttributeDefault(int index) {
		return false;
	}
	public String getAttributeNamespace(int index) {
		if (index >= attributeCount)
			throw new IndexOutOfBoundsException();
		return attributes[index << 2];
	}
	public String getAttributeLocalName(int index) {
		if (index >= attributeCount)
			throw new IndexOutOfBoundsException();
		return attributes[(index << 2) + 2];
	}
	public String getAttributePrefix(int index) {
		if (index >= attributeCount)
			throw new IndexOutOfBoundsException();
		return attributes[(index << 2) + 1];
	}
	public String getAttributeValue(int index) {
		if (index >= attributeCount)
			throw new IndexOutOfBoundsException();
		return attributes[(index << 2) + 3];
	}
	
	
	/** TODO: currently ignores prefix! */
	
	public String getAttributeValue(String prefix, String name) {
		for (int i = (attributeCount << 2) - 4; i >= 0; i -= 4) {
			if (attributes[i + 2].equals(name)
					)
				return attributes[i + 3];
		}
		return null;
	}
	
	public int getEventType()  {
		return type;
	}
	
	/*
	public int next() throws IOException {
		txtPos = 0;
		isWhitespace = true;
		int minType = 9999;
		token = false;
		do {
			nextImpl();
			if (type < minType)
				minType = type;
			//	    if (curr <= TEXT) type = curr;
		} while (minType > ENTITY_REF // ignorable
				|| (minType >= TEXT && peekType() >= TEXT));
		type = minType;
		if (type > TEXT)
			type = TEXT;
		return type;
	}
	*/
	
	public int next() throws IOException {
		isWhitespace = true;
		txtPos = 0;
		token = true;
		nextImpl();
		return type;
	}
	
	
	//----------------------------------------------------------------------
	// utility methods to make XML parsing easier ...
	public int nextTag() throws IOException {

		do{
			next();
		}
		while(((type == CHARACTERS || type == CDATA || type == ENTITY_REFERENCE) && isWhitespace) 
				|| type == SPACE 
				|| type == COMMENT 
				|| type == PROCESSING_INSTRUCTION);

		if (type != END_ELEMENT && type != START_ELEMENT)
			exception("unexpected type");

		return type;
	}
	
	public void require(int type, String prefix, String name)
			throws IOException, IOException {
		if (type != this.type
				|| (name != null && !name.equals(getLocalName())))
			exception("expected: " + TYPES[type] + name);
	}
	
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
	
	
	
	
	public void setProperty(String property, Object value) {
        if(property.equals("http://xmlpull.org/v1/doc/features.html#relaxed")){
        	relaxed = ((Boolean) value).booleanValue();
        }
		else if (isProp(property, true, "location"))
			location = value;
		else
			throw new RuntimeException("unsupported property: " + property);
	}
	/**
	 * Skip sub tree that is currently porser positioned on. <br>
	 * NOTE: parser must be on START_ELEMENT and when funtion returns parser will be
	 * positioned on corresponding END_ELEMENT.
	 */
	//	Implementation copied from Alek's mail...
	public void skipSubTree() throws IOException {
		require(START_ELEMENT,null, null);
		int level = 1;
		while (level > 0) {
			int eventType = next();
			if (eventType == END_ELEMENT) {
				--level;
			} else if (eventType == START_ELEMENT) {
				++level;
			}
		}
	}

	public void close() throws IOException {
		reader.close();
	}


	public String getEncoding() {
		return encoding;
	}
}