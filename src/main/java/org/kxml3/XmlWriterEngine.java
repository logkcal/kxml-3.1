/*
 * Created on 18.07.2004 by Stefan Haustein
 */
package org.kxml3;

import java.io.IOException;

/**
 * @TODO Add StAX methods as needed
 * @author Stefan Haustein
 */
public interface XmlWriterEngine {
    
    void writeCharacters(String s) throws IOException;
    void writeStartElement(String prefix, String name) throws IOException;
    void writeEndElement(String prefix, String name) throws IOException;
    void writeAttribute(String prefix, String name, String value) throws IOException;
}
