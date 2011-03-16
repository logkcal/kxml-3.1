package org.kxml3.wap;

import java.io.IOException;

public class WbxmlUtil {

    public static String getReadableHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for( byte b : bytes ) {
            if ( b > 20  && b < 128) {
                sb.append("'"+(char)b + "'");
            } else {
                sb.append("0x"+Integer.toString(b,16));
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String getXmlString(WbxmlReader reader) throws IOException {
        StringBuffer buff = new StringBuffer();
        boolean hasContent = true;

        int eventType = reader.getEventType();
        do {
            switch ( eventType ) {
                case 0:
                    //ignored, start doc
                    break;
                case WbxmlReader.START_ELEMENT:
                    if ( ! hasContent ) {
                        buff.append(">");
                    }
                    buff.append("<").append(reader.getLocalName());
                    for( int i =0 ; i < reader.getAttributeCount(); i++ ) {
                        buff.append(" ");
                        buff.append(reader.getAttributeLocalName(i));
                        buff.append("=\"");
                        buff.append(reader.getAttributeValue(i));
                        buff.append("\"");
                    }
                    hasContent = false;
                    break;
                case WbxmlReader.CHARACTERS:
                    if ( ! hasContent ) {
                        buff.append(">");
                    }
                    hasContent = true;
                    buff.append(reader.getText());
                    break;
                case WbxmlReader.ENTITY_REFERENCE:
                    if ( ! hasContent ) {
                        buff.append(">");
                    }
                    buff.append(reader.getLocalName());
                    break;
                case WbxmlReader.END_ELEMENT:
                    if ( ! hasContent ) {
                        buff.append(">");
                    }
                    hasContent = true;
                    buff.append("</").append(reader.getLocalName()).append(">");
                    break;
                default :
                    //TODO: Throw an error?
                    //System.out.println("Not understood" + eventType);
            }

            eventType = reader.next();
        } while ( eventType != WbxmlReader.END_DOCUMENT);

        return buff.toString();
    }
}
