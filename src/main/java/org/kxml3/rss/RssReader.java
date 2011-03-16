/*
 * Created on 08.05.2004 by Stefan Haustein
 */
package org.kxml3.rss;

import java.io.IOException;
import java.io.Reader;

import org.kxml3.io.KXmlReader;

/**
 * This class uses a KXmlReader and provides a stream of String arrays containing
 * the title, link, description and data of the RSS topics.
 * 
 * @author Stefan Haustein
 */
public class RssReader {

	/** Index of the title in the String array returned by next(). */
	
	public static final int TITLE = 0;

	/** Index of the link in the String array returned by next(). */

	public static final int LINK = 1;

	/** Index of the description in the String array returned by next(). */
	
	public static final int DESCRIPTION = 2;

	/** Index of the date in the String array returned by next(). */
	
	public static final int DATE = 3;

	/** Index of the author in the String array returned by next(). */
	
	public static final int AUTHOR = 4;
	

	KXmlReader xr;
	
	public String title;
	public String description;
	
	/** Creates a new RssReader instance for the given stream. */
	
	public RssReader(Reader reader) throws IOException{
		xr = new KXmlReader();
		xr.setInput(reader);
		xr.setProperty("http://xmlpull.org/v1/doc/features.html#relaxed", Boolean.TRUE);
		
		while(xr.next() != KXmlReader.END_DOCUMENT){
			if(xr.getEventType() == KXmlReader.START_ELEMENT){
				String n = xr.getLocalName().toLowerCase();
				if(n.equals("item") || n.endsWith(":item")){
					return;
				}
				
				if(n.equals("channel")||n.endsWith(":channel")){
					while(xr.next() != KXmlReader.END_ELEMENT) {
						if(xr.getEventType() == KXmlReader.START_ELEMENT){
							String name = xr.getLocalName().toLowerCase();
							int cut = name.indexOf(":");
							if(cut != -1) {
								name = name.substring(cut+1);
							}
							
							if (name.equals("item"))
								return;
							
							StringBuffer buf = new StringBuffer();
							readText(buf);
							String text	=buf.toString();
							if(name.equals("title"))
								title = text;
							else if (name.equals("description"))
								description = text;
						}
					}
				}
			}
		}
	}
	
	
	/** Internal method that reads text, skipping contained elements. */
	
	void readText(StringBuffer buf) throws IOException{
		String name = xr.getLocalName();
		loop:
		while(xr.next() != KXmlReader.END_DOCUMENT){
			switch(xr.getEventType()){
				case KXmlReader.CDATA:
				case KXmlReader.ENTITY_REFERENCE: 
				case KXmlReader.CHARACTERS : buf.append(xr.getText()); break;
				case KXmlReader.END_ELEMENT: 
					if(xr.getLocalName().equals(name)) break loop;
			}
		}	
	}
	

	/** 
	 * Returns the next RSS entry as a string array. The contents of the array
	 * should be accessed using the constants TITLE, DATE, DESCRIPTION and AUTHOR. 
	 * If there are no more entries, null is returned. 
	 * 
	 */

	public String[] next() throws IOException{
		
		String[] item = new String[5];
		
		do{
			if(xr.getEventType() == KXmlReader.START_ELEMENT){
				String n = xr.getLocalName().toLowerCase();
				if(n.equals("item")||n.endsWith(":item")){
					while(xr.next() != KXmlReader.END_ELEMENT) {
						if(xr.getEventType() == KXmlReader.START_ELEMENT){
							String name = xr.getLocalName().toLowerCase();
							int cut = name.indexOf(":");
							if(cut != -1) 
								name = name.substring(cut+1);
							StringBuffer buf = new StringBuffer();
							readText(buf);
							String text	=buf.toString();
							if(name.equals("title"))
								item[TITLE] = text;
							else if (name.equals("link"))
								item[LINK] = text;
							else if (name.equals("description"))
								item[DESCRIPTION] = text;
							else if (name.equals("date") || name.equals("pubdate"))
								item[DATE] = text;
							else if (name.equals("author"))
								item[AUTHOR] = text;
						}
					}
					return item;
				}
			}
		}
		while(xr.next() != KXmlReader.END_DOCUMENT);
			
		return null;
	}
}
