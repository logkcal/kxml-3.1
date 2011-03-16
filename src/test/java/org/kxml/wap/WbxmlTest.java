package org.kxml.wap;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kxml3.wap.WbxmlCodeArrayLookup;
import org.kxml3.wap.WbxmlCodeArrayLookup;
import org.kxml3.wap.WbxmlReader;
import org.kxml3.wap.WbxmlUtil;
import org.kxml3.wap.WbxmlWriter;


public class WbxmlTest {

    //-------------------------------------------------------------
    // Methods - Set-Up / Tear-Down
    //-------------------------------------------------------------

    @Before
    public void setUp() {
    }


    @After
    public void tearDown() {
    }


    //-------------------------------------------------------------
    // Methods - Test Cases
    //-------------------------------------------------------------

    static byte[] testCase2 = {
        03,01,0x6a,0x12,'a','b','c',0x00,' ','E','n','t','e','r',' ','n',
        'a','m','e',':',' ',0x0,0x0,0x1,(byte)0x47,(byte)0xc5,0x0,0x1,0x09,(byte)0x83,0x00,0x05,0x01,
        (byte)0x88,0x06,(byte)0x86,0x08,0x03,'x','y','z',00,(byte)0x85,0x03,'/','s',00,
        01,(byte)0x83,0x04,(byte)0x86,0x07,0x0a,0x03,'N',00,01,01,01
    };

    static String[] tagCode2 = {
        "CARD","INPUT","XYZ","DO"
    };

    static String[] attrCode2 = {
        "STYLE=LIST","TYPE","TYPE=TEXT","URL=http://","NAME","KEY"
    };
    
    static String[] valueCode2 = {
        ".org","ACCEPT"
    };

    static byte[] testCase1 = {
        03,01,03,00,00,01,0x47,0x46,03,' ','X',' ','&','Y',00,05,03,'\n','X',
        ' ','=',' ','1',' ',00,01,01
    };

    static String[] tagCode1 = {
        "BR","CARD","XYZ"
    };

    String testCase1Xml = "<XYZ><CARD> X &Y<BR></BR>\nX = 1 </CARD></XYZ>";
    String testCase2Xml = "<XYZ><CARD NAME=\"abc\" STYLE=\"LIST\"><DO TYPE=\"ACCEPT\" " +
            "URL=\"http://xyz.org/s\"></DO> Enter name: <INPUT TYPE=\"TEXT\" KEY=\"N\"></INPUT></CARD></XYZ>";

    @Test
    public void testXml2() throws IOException {
        
        WbxmlCodeArrayLookup lookup = new WbxmlCodeArrayLookup();
        lookup.addTagPage(0x01, tagCode2);
        lookup.addAttributePage(0x01, attrCode2);
        lookup.addAttributeValuePage(0x01, valueCode2);

        WbxmlReader reader = new WbxmlReader(lookup);
        reader.setInput(new ByteArrayInputStream(testCase2),"US-ASCII");

        Assert.assertEquals(testCase2Xml,WbxmlUtil.getXmlString(reader));

        WbxmlWriter writer = new WbxmlWriter(lookup);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        writer.setOutput(os, "UTF8");

        writer.startDocument(null, true);
        writer.writeStartElement(null, "XYZ");
        writer.writeStartElement(null, "CARD");
        writer.writeAttribute(null, "NAME", "abc");
        writer.writeAttribute(null, "STYLE", "LIST");
        
        writer.writeStartElement(null, "DO");
        writer.writeAttribute(null, "TYPE", "ACCEPT");
        writer.writeAttribute(null, "URL", "http://xyz.org/s");
        writer.writeEndElement(null, "DO");

        writer.writeCharacters("Enter name: ");

        writer.writeStartElement(null, "INPUT");
        writer.writeAttribute(null, "TYPE", "TEXT");
        writer.writeAttribute(null, "KEY", "N");
        writer.writeEndElement(null, "INPUT");

        writer.endDocument();

        byte[] bytes = os.toByteArray();
        System.out.println(WbxmlUtil.getReadableHexString(bytes));
         System.out.println(WbxmlUtil.getReadableHexString(testCase2));

        reader = new WbxmlReader(lookup);
        reader.setInput(new ByteArrayInputStream(testCase2),"US-ASCII");

        Assert.assertEquals(testCase2Xml,WbxmlUtil.getXmlString(reader));
    }

    @Test
    public void testReadsXml1() throws IOException {
        WbxmlCodeArrayLookup lookup = new WbxmlCodeArrayLookup();
        lookup.addTagPage(0x01, tagCode1);

        WbxmlReader reader = new WbxmlReader(lookup);
        reader.setInput(new ByteArrayInputStream(testCase1),"US-ASCII");

        Assert.assertEquals(testCase1Xml, WbxmlUtil.getXmlString(reader));

        WbxmlWriter writer = new WbxmlWriter(lookup);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        writer.setOutput(os, "US-ASCII");

        writer.startDocument(null, true);
        writer.writeStartElement(null, "XYZ");
        writer.writeStartElement(null, "CARD");
        writer.writeCharacters(" X &Y");
        writer.writeStartElement(null, "BR");
        writer.writeEndElement(null, "BR");
        writer.writeCharacters("\nX = 1 ");
        writer.writeEndElement(null, "CARD");
        writer.writeEndElement(null, "XYZ");
        writer.endDocument();

        byte[] bytes = os.toByteArray();

        assertBytesEqual(bytes,testCase1);
    }

    private void assertBytesEqual(byte[] a, byte[]b) {
        for ( int i =0; i < a.length && i < b.length; i++ ) {
            //System.out.println(Integer.toString(a[i],16) + " <=> " + Integer.toString(b[i],16));
            Assert.assertEquals("Bytes not equal at index " + i,a[i],b[i]);
        }
        Assert.assertEquals("Lengths not equal",(int)a.length,(int)b.length);
    }

}