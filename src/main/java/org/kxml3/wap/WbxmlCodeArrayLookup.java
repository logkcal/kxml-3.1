package org.kxml3.wap;

import java.util.HashMap;
import java.util.Map;

public class WbxmlCodeArrayLookup implements WbxmlCodeLookup {

    //-------------------------------------------------------------
    // Private variables
    //-------------------------------------------------------------
    
    private Map<Integer,String[]> tagPages = new HashMap<Integer,String[]>();
    private Map<Integer,String[]> attributePages = new HashMap<Integer,String[]>();
    private Map<Integer,String[]> attributeValuePages = new HashMap<Integer,String[]>();

    private Map<String,WbxmlCode> tagNames = new HashMap<String,WbxmlCode>();
    private Map<String,WbxmlCode> attributeNames = new HashMap<String,WbxmlCode>();
    private Map<String,WbxmlCode> attributeValues = new HashMap<String,WbxmlCode>();

    //-------------------------------------------------------------
    // Methods
    //-------------------------------------------------------------

    public void addTagPage(int pageNum, String[] values) {
        tagPages.put(pageNum,values);
        int code = 5;
        for ( String value : values ) {
            tagNames.put(value,new WbxmlCodeImpl(code++,value,pageNum));
        }
    }

    public void addAttributePage(int pageNum, String[] values) {
        attributePages.put(pageNum,values);
        int code = 5;
        for ( String value : values ) {
            attributeNames.put(value,new WbxmlCodeImpl(code++,value,pageNum));
        }
    }

    public void addAttributeValuePage(int pageNum, String[] values) {
        attributeValuePages.put(pageNum,values);
        int code = 5;
        for ( String value : values ) {
            attributeValues.put(value,new WbxmlCodeImpl(code++,value,pageNum));
        }
    }

    //-------------------------------------------------------------
    // Implementation
    //-------------------------------------------------------------

    @Override
    public String lookupAttributeCode(int pageNumber, int code) {
        return attributePages.get(pageNumber)[code-5];
    }

    @Override
    public String lookupAttributeValueCode(int pageNumber, int code) {
        return attributeValuePages.get(pageNumber)[code-5];
    }

    @Override
    public String lookupTagCode(int pageNumber, int code) {
        return tagPages.get(pageNumber)[code-5];
    }

    @Override
    public boolean verifyTagPage(int code) {
        return tagPages.containsKey(code);
    }

    @Override
    public boolean verifyAttributePage(int code) {
        return attributePages.containsKey(code);
    }

    @Override
    public boolean verifyAttributeValuePage(int code) {
        return attributeValuePages.containsKey(code);
    }

    @Override
    public WbxmlCode lookupTagByName(String name) {
        return tagNames.get(name);
    }

    @Override
    public WbxmlCode lookupAttributeByName(String name) {
        return attributeNames.get(name);
    }

    @Override
    public WbxmlCode lookupAttributeValueByName(String name) {
        return attributeValues.get(name);
    }
}
