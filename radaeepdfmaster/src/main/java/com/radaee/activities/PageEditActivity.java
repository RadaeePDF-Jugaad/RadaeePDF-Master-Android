package com.radaee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.radaee.pdf.Document;
import com.radaee.pdfmaster.R;
import com.radaee.util.PDFPageGridAdt;
import com.radaee.util.PDFPageGridView;

public class PageEditActivity extends AppCompatActivity {
    static protected Document ms_tran_doc;
    private PDFPageGridView mView;

    public static int RESULT_CANCEL = 0;
    public static int RESULT_CONFIRM = 1;
    public static int RESULT_PAGE = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Document doc = ms_tran_doc;
        ms_tran_doc = null;
        setContentView(R.layout.activity_pdf_page_edit);

        mView = findViewById(com.radaee.viewlib.R.id.vw_pages);
        mView.PDFOpen(doc);
        mView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            PDFPageGridAdt.PDFPageGridItem item = (PDFPageGridAdt.PDFPageGridItem) mView.getItemAtPosition(position);
            Intent intent = new Intent();
            intent.putExtra("page", item.getPageNo());
            setResult(RESULT_PAGE, intent);
            finish();
        });
        setResult(RESULT_CANCEL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_reader_activity_annot, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_confirm:
                if (!mView.PDFIsModified()) {
                    finish();
                    return true;
                }
                Intent intent = new Intent();
                boolean[] removal = mView.PDFGetRemoval();
                int[] rotation = mView.PDFGetRotate();
                intent.putExtra("removal", removal);
                intent.putExtra("rotate", rotation);
                setResult(RESULT_CONFIRM, intent);
                finish();
                return true;
            case R.id.action_cancel:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
