/*
 * Created on 01.10.2004 by Stefan Haustein
 */
package org.kxml3.stax;

import java.io.IOException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.kxml3.XmlReaderEngine;


/**
 * A StAX subset implementation based on a XmlReaderEngine.
 * For the documentation of the supported methods, please
 * refer to the StAX documentation.
 *  
 * @author Stefan Haustein
 */

public class KXmlStreamReader  implements XMLStreamReader, Location, NamespaceContext {
    
    XmlReaderEngine reader;

    
    /** storage layout: 0=prefx 1=name 2= namespace 3 =value */
    
    int attributeCount;
    int[] attributeMap = new int[4];
    
    private String[] nspStack = new String[8];
    private int[] nspCounts = new int[4];

    public KXmlStreamReader(XmlReaderEngine reader){
        this.reader = reader;
    }
    
    private static String[] ensureCapacity(String[] orig, int size){
    	
    		if(orig.length >= size) return orig;
    		String[] na = new String[size * 3 / 2];
    		
    		System.arraycopy(orig, 0, na, 0, orig.length);
    		
    		return na;
    }
    
    
    private int adjustNamespaces(int type){
    	
    		attributeCount = 0;
    		
    		if(type != START_ELEMENT){
    			return type;
    		}
    	
    		int depth = reader.getDepth();
    		
    		if(nspCounts.length <= depth){
    			int[] nw = new int[(depth+1)*3/2];
    			System.arraycopy(nspCounts, 0, nw, 0, nspCounts.length);
    			nspCounts = nw;
    		}
    		
    		nspCounts[depth] = nspCounts[depth-1];
    		
    		int rac = reader.getAttributeCount();
    		
    		if(attributeMap.length < rac){
    			attributeMap = new int[rac];
    		}
    	
    		// store namespaces
    		
    		for(int i = 0; i < rac; i++){
    			if("xmlns".equals(reader.getAttributePrefix(i))){
    				int j = nspCounts[depth]*2;
    				
    				nspStack = ensureCapacity(nspStack, j+2);
    				
    				nspStack = (String[]) ensureCapacity(nspStack, j+2);
    				nspStack[j] = reader.getAttributeLocalName(i);
    				nspStack[j+1] = reader.getAttributeValue(i);
    				nspCounts[depth]++;
    			}    			
    			else{
    				attributeMap[attributeCount++] = i;
    			}
    		}

    		return type;
    }
    
    
	public Object getProperty(String name) throws IllegalArgumentException {
		return reader.getProperty(name);
	}

	public int next() throws XMLStreamException {
		try{
			return adjustNamespaces(reader.next());
		}
		catch(IOException e){
			throw new XMLStreamException(e);
		}
	}

	public void require(int type, String namespaceURI, String localName)
			throws XMLStreamException {
		// Auto-generated method stub
		//throw new RuntimeException("NYI");
	}

	public String getElementText() throws XMLStreamException {
		try {
			return reader.getElementText();
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
	}

	public int nextTag() throws XMLStreamException {
		try{
            return adjustNamespaces(reader.nextTag());
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
	}

	

	public void close() throws XMLStreamException {
		try {
			reader.close();
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
	}

	public String getNamespaceURI(String prefix) {
		for(int i = nspCounts[reader.getDepth()]*2-2; i >= 0; i-=2){
			if(nspStack[i].equals(prefix)){
				return nspStack[i+1];
			}
		}
		return null;
	}
	
	public String getAttributeValue(String namespaceURI, String localName) {
		for(int i = 0; i < attributeCount; i++){
			if(getAttributeLocalName(i).equals(localName)
					&& (namespaceURI == null || namespaceURI.equals(getAttributeNamespace(i)))){
				return getAttributeValue(i);
			}
		}
		return null;
	}
	
	

	public int getAttributeCount() {
		return attributeCount;
	}
    

	public String getAttributeNamespace(int index) {
		return getNamespaceURI(reader.getAttributePrefix(attributeMap[index]));
	}

	public String getAttributeLocalName(int index) {
		return reader.getAttributeLocalName(attributeMap[index]);
	}

	public String getAttributePrefix(int index) {
		return reader.getAttributePrefix(attributeMap[index]);
	}

	public String getAttributeType(int index) {
		return null;
	}

	public String getAttributeValue(int index) {
		return reader.getAttributeValue(attributeMap[index]);
	}

	public boolean isAttributeSpecified(int index) {
		return true;
	}

	public int getNamespaceCount() {
        return nspCounts[reader.getDepth()];
	}

	public String getNamespacePrefix(int index) {
		return nspStack[index*2];
	}

	public String getNamespaceURI(int index) {
        return nspStack[index*2+1];
	}

	public NamespaceContext getNamespaceContext() {
		return this;
	}

	public int getEventType() {
		return reader.getEventType();
	}

	public String getText() {
		return reader.getText();
	}

	

	public int getTextCharacters(int sourceStart, char[] target,
			int targetStart, int length) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public int getTextStart() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public int getTextLength() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public String getEncoding() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public Location getLocation() {
		return this;
	}

	public String getLocalName() {
        return reader.getLocalName();
	}

	public String getNamespaceURI() {
		return getNamespaceURI(reader.getPrefix());
	}

	public String getPrefix() {
		return reader.getPrefix();
	}

	public String getVersion() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public boolean isStandalone() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public boolean standaloneSet() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public String getCharacterEncodingScheme() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public String getPITarget() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public String getPIData() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

    public int getLineNumber() {
		return reader.getLineNumber();
	}

    public int getColumnNumber() {
    		return reader.getColumnNumber();
	}

    public int getCharacterOffset() {
    		return -1;
	}

    public String getPublicId() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

    public String getSystemId() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}
}
