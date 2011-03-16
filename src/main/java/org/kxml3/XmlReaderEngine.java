package org.kxml3;
import java.io.IOException;
/*
 * Created on 17.07.2004 by Stefan Haustein
 */
/**
 * @author Stefan Haustein
 */
public interface XmlReaderEngine {
    
	public static final int START_ELEMENT = 1;
	public static final int END_ELEMENT = 2;
	public static final int PROCESSING_INSTRUCTION = 3;
	public static final int CHARACTERS = 4;
	public static final int COMMENT = 5;
	public static final int SPACE = 6;
	public static final int START_DOCUMENT = 7;
	public static final int END_DOCUMENT = 8;
	public static final int ENTITY_REFERENCE = 9;
	public static final int ATTRIBUTE = 10;
	public static final int DTD = 11;
	public static final int CDATA = 12;
	public static final int NAMESPACE = 13;
	public static final int NOTATION_DECLARATION = 14;
	public static final int ENTITY_DECLARATION = 15;
    
    public void setProperty(String name, Object value);
    
	public Object getProperty(java.lang.String name)
			throws java.lang.IllegalArgumentException;

    public int next() throws IOException;

    public String getElementText() throws IOException;
	
    public int nextTag() throws IOException;
	
    public void close() throws IOException;

    /**
	 * Convenience method for using the engine alone. Please note that this
	 * method works with the prefix, NOT the namespace
	 */
	public String getAttributeValue(String prefix, String localName);
	
    public int getAttributeCount();

    public String getAttributeLocalName(int index);
	
    public String getAttributePrefix(int index);
	
    public String getAttributeValue(int index);
	
    public int getEventType();
	
    public String getText();
	
    public String getEncoding();
	
    public String getPrefix();
	
    public String getLocalName();
	// what is the difference to getEncoding?!?
	//public String getCharacterEncodingScheme();

	/**
	 * returns the current nesting depth. 0 outside the root element. increased on an element start tag,
	 * decreased after an end element tag
	 */
	public int getDepth();
	
	
	public int getLineNumber();
	public int getColumnNumber();
	
}