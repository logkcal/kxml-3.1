package org.kxml3.wap;


class WbxmlCodeImpl implements WbxmlCode<WbxmlCodeImpl>{
    //-------------------------------------------------------------
    // Private variables
    //-------------------------------------------------------------

    private final int code;
    private final String name;
    private final int page;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    WbxmlCodeImpl(int code, String name, int page) {
        this.code = code;
        this.name = name;
        this.page = page;
    }

    //-------------------------------------------------------------
    // Implementation
    //-------------------------------------------------------------

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getPage() {
        return page;
    }

    @Override
    public int compareTo(WbxmlCodeImpl t) {
        return new Integer(code).compareTo(t.code);
    }

}
