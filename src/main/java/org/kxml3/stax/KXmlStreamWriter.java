/*
 * Created on 19.12.2004 by Stefan Haustein
 */
package org.kxml3.stax;

import java.io.IOException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.kxml3.io.KXmlWriter;

/**
 * A StAX subset implementation based on a XmlReaderEngine.
 * For the documentation of the supported methods, please
 * refer to the StAX documentation.
 * 
 * @author Stefan Haustein
 */
public class KXmlStreamWriter implements XMLStreamWriter {
    
    KXmlWriter writer;
    
    public KXmlStreamWriter(KXmlWriter writer){
    	this.writer = writer;
    }

	public void writeStartElement(String localName) throws XMLStreamException {
        try{
            writer.writeStartElement(null, localName);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
	}

	public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		
        if(namespaceURI != null && namespaceURI.length() > 0){
        	System.out.println("ignoring nsp: "+namespaceURI);
        }
        
        try {
			writer.writeStartElement(null, localName);
		} catch (IOException e) {
			
			throw new XMLStreamException(e);
		}
            
	}

	public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeEmptyElement(String localName) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeEndElement() throws XMLStreamException {
		try {
			writer.writeEndElement();
		} catch (IOException e) {
			
			throw new XMLStreamException(e);
		}
	}

	public void writeEndDocument() throws XMLStreamException {
		try {
			writer.endDocument();
		} catch (IOException e) {
			
			throw new XMLStreamException(e);
		}
	}

	public void close() throws XMLStreamException {
		try {
			writer.close();
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
	}

	public void flush() throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeAttribute(String localName, String value) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		if(namespaceURI != null && namespaceURI.length() > 0) {
            System.out.println("Ignoring nsp: "+namespaceURI);
        }
        try{
		writer.writeAttribute(null, localName, value);
        }
        catch(IOException e){
        	throw new XMLStreamException(e);
        }
	}

	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeComment(String data) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeProcessingInstruction(String target) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeCData(String data) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeDTD(String dtd) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeEntityRef(String name) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeStartDocument() throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeStartDocument(String version) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void writeCharacters(String text) throws XMLStreamException {
		try{
			writer.writeCharacters(text);
        }
        catch(IOException e){
        	throw new XMLStreamException(e);
        }
	}

	public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public String getPrefix(String uri) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public NamespaceContext getNamespaceContext() {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		// Auto-generated method stub
		throw new RuntimeException("NYI");
	}
	
}
