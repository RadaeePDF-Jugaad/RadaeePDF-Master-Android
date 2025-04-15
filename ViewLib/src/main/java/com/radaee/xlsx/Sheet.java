package com.radaee.xlsx;

public class Sheet {
    static private native String getCell(long hand, int row, int col);
    protected long hand;
    protected Document m_doc;

    protected Sheet(Document doc, long handle) {
        m_doc = doc;
        hand = handle;
    }
    public String GetCellValue(int row, int col)
    {
        return getCell(hand, row, col);
    }
}
