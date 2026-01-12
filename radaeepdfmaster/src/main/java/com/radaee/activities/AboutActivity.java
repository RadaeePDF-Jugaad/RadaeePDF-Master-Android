package com.radaee.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.radaee.comm.Global;
import com.radaee.pdf.Document;
import com.radaee.pdf.Page;
import com.radaee.pdfmaster.R;
import com.radaee.reader.PDFGLLayoutView;
import com.radaee.util.RDAssetStream;
import com.radaee.view.IPDFLayoutView;

/**
 * Created by radaee on 2015/2/4.
 */
public class AboutActivity extends Activity implements IPDFLayoutView.PDFLayoutListener {
    private PDFGLLayoutView m_view;
    private Document m_doc;
    private RDAssetStream m_asset_stream = null;

    private void onFail(String msg)//treat open failed.
    {
        m_doc.Close();
        m_doc = null;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }

    private final void ProcessOpenResult(int ret) {
        switch (ret) {
            case -1://need input password
                onFail(getString(R.string.failed_invalid_password));
                break;
            case -2://unknown encryption
                onFail(getString(R.string.failed_encryption));
                break;
            case -3://damaged or invalid format
                onFail(getString(R.string.failed_invalid_format));
                break;
            case -10://access denied or invalid file path
                onFail(getString(R.string.failed_invalid_path));
                break;
            case 0://succeeded, and continue
                m_view.PDFOpen(m_doc, this);
                break;
            default://unknown error
                onFail(getString(R.string.failed_unknown));
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.Init(this);
        m_view = new PDFGLLayoutView(this);
        m_asset_stream = new RDAssetStream();
        m_asset_stream.open(getAssets(), "About(EN).pdf");
        m_doc = new Document();
        int ret = m_doc.OpenStream(m_asset_stream, null);
        ProcessOpenResult(ret);
        if (ret == 0)
            setContentView(m_view);
    }

    @Override
    protected void onDestroy() {
        if (m_view != null) m_view.PDFClose();
        if (m_doc != null) m_doc.Close();
        if (m_asset_stream != null) m_asset_stream.close();
        Global.RemoveTmp();
        super.onDestroy();
    }

    @Override
    public void OnPDFPageModified(int pageno) {

    }

    @Override
    public void OnPDFPageChanged(int pageno) {

    }

    @Override
    public void OnPDFAnnotTapped(int pageno, Page.Annotation annot) {
        m_view.PDFPerformAnnot();
    }

    @Override
    public void OnPDFBlankTapped(int pageno) {
    }

    @Override
    public void OnPDFSelectEnd(){
    }

    @Override
    public void OnPDFTextSelected(String text, float x, float y) {
    }

    @Override
    public void OnPDFOpenURI(String uri) {
        try {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse(uri);
            intent.setData(content_url);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(AboutActivity.this, R.string.todo_open_url + uri, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void OnPDFOpenJS(String js) {
    }

    @Override
    public void OnPDFOpenMovie(String path) {
    }

    @Override
    public void OnPDFOpenSound(int[] paras, String path) {
    }

    @Override
    public void OnPDFOpenAttachment(String path) {
    }

    @Override
    public void OnPDFOpenRendition(String path) {
    }

    @Override
    public void OnPDFOpen3D(String path) {
    }

    @Override
    public void OnPDFZoomStart() {

    }

    @Override
    public void OnPDFZoomEnd() {

    }

    @Override
    public boolean OnPDFDoubleTapped(float x, float y) {
        return false;
    }

    @Override
    public void OnPDFSearchFinished(boolean found) {
    }

    @Override
    public void OnPDFPageRendered(IPDFLayoutView.IVPage vpage) {
    }

    @Override
    public void OnPDFLongPressed(float x, float y) {
    }

    @Override
    public void OnPDFPageDisplayed(Canvas canvas, IPDFLayoutView.IVPage vpage) {
    }
}
