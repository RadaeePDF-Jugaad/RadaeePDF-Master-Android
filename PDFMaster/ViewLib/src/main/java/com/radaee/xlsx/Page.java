package com.radaee.xlsx;

import android.graphics.Bitmap;
import android.util.Log;

import com.radaee.comm.DIB;

public class Page {
    static private native boolean render(long page, long dib, float scale, int orgx, int orgy);

    protected long hand;
    protected Document m_doc;

    protected Page(Document doc, long handle) {
        m_doc = doc;
        hand = handle;
    }

    public boolean Render(DIB dib, float scale, int orgx, int orgy) {
        if (dib == null || scale < 0.01f) return false;
        return render(hand, dib.get_hand(), scale, orgx, orgy);
    }

    /**
     * Close page object and free memory.
     */
    final public void Close() {
        long hand_page = hand;
        hand = 0;
        if (m_doc != null) {
            m_doc = null;
        }
    }
}
