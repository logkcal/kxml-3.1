package org.kxml3.wap;

public interface WbxmlCodeLookup {
    public String lookupAttributeCode(int pageNumber, int code);
    public String lookupAttributeValueCode(int pageNumber, int code);
    public String lookupTagCode(int pageNumber, int code);

    public boolean verifyTagPage(int code);
    public boolean verifyAttributePage(int code);
    public boolean verifyAttributeValuePage(int code);

    //writer methods
    public WbxmlCode lookupTagByName(String name);
    public WbxmlCode lookupAttributeByName(String name);
    public WbxmlCode lookupAttributeValueByName(String name);
}
