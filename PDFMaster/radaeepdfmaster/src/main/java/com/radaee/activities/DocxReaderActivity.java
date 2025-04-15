package com.radaee.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.radaee.comm.Global;
import com.radaee.docx.Document;
import com.radaee.pdfmaster.R;
import com.radaee.reader.DOCXGLLayoutView;
import com.radaee.util.RDAssetStream;
import com.radaee.util.RDHttpStream;
import com.radaee.utility.UriContentUtility;
import com.radaee.view.IDOCXLayoutView;

import java.io.File;

public class DocxReaderActivity extends AppCompatActivity implements IDOCXLayoutView.DOCXLayoutListener/*, DropboxUtility.DropBoxCallback*/ {
    class OpenTask extends AsyncTask<Void, Integer, Integer> {
        private boolean need_save;
        private ProgressDialog dlg;
        private Handler handler;
        private Runnable runable;

        OpenTask(boolean need_save) {
            this.need_save = need_save;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            mDocxDoc.GetPagesMaxSize();//it may spend much time for first invoking this method.
            return null;
        }

        @Override
        protected void onPreExecute() {
            handler = new Handler();
            runable = new Runnable() {
                public void run() {
                    dlg = ProgressDialog.show(DocxReaderActivity.this, getString(R.string.message_wait_label), getString(R.string.message_loading_label, "DOCX"), true);
                }
            };
            handler.postDelayed(runable, 1000);//delay 1 second to display progress dialog.
        }

        @Override
        protected void onPostExecute(Integer integer) {
            mDocxView.DOCXOpen(mDocxDoc, DocxReaderActivity.this);
            need_save_doc = need_save;
            if (dlg != null)
                dlg.dismiss();
            else
                handler.removeCallbacks(runable);
        }
    }

    static public Document ms_tran_doc;
    private Document mDocxDoc = null;
    private boolean need_save_doc = false;
    //private String mDataSource = "external";

    private String mFilePath;

    private DOCXGLLayoutView mDocxView;
    private ActionBar mActionBar;
    private LinearLayout mSearchBar;
    private boolean mIsInSearchMode = false;

    private static final int HIDE_ACTION_BAR = 0;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_ACTION_BAR:
                    if (mActionBar != null)
                        mActionBar.hide();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Global.Init(this);
        setContentView(R.layout.activity_docx_reader);

        mActionBar = getSupportActionBar();
        mHandler.sendEmptyMessageDelayed(HIDE_ACTION_BAR, 3000);

        mDocxView = findViewById(R.id.docx_glview);
        mSearchBar = findViewById(R.id.layout_search);
        ImageView mSearchBtn = findViewById(R.id.btn_search);
        mSearchBtn.setOnClickListener(buttonClickListener);

        Intent intent = getIntent();
        /*if (intent.hasExtra("data_source"))
            mDataSource = intent.getStringExtra("data_source");*/
        if (ms_tran_doc != null) {
            mDocxDoc = ms_tran_doc;
            ms_tran_doc = null;
            mFilePath = intent.getStringExtra("doc_path");
            OpenTask task = new OpenTask(true);
            task.execute();
        } else {
            String docx_asset = intent.getStringExtra("DOCXAsset");
            String docx_path = intent.getStringExtra("DOCXPath");
            String docx_pswd = intent.getStringExtra("DOCXPswd");
            String docx_http = intent.getStringExtra("DOCXHttp");
            if (docx_http != null && !docx_http.equals("")) {
                RDHttpStream m_http_stream = new RDHttpStream();
                m_http_stream.open(docx_http);
                mDocxDoc = new Document();
                mFilePath = docx_http;
                int ret = mDocxDoc.OpenStream(m_http_stream, docx_pswd);

                ProcessOpenResult(ret);
            } else if (docx_asset != null && !docx_asset.equals("")) {
                RDAssetStream m_asset_stream = new RDAssetStream();
                m_asset_stream.open(getAssets(), docx_asset);
                mDocxDoc = new Document();
                int ret = mDocxDoc.OpenStream(m_asset_stream, docx_pswd);
                ProcessOpenResult(ret);
            } else if (docx_path != null && !docx_path.equals("")) {
                mDocxDoc = new Document();
                int ret = mDocxDoc.Open(docx_path, docx_pswd);
                mFilePath = docx_path;
                ProcessOpenResult(ret);
            } else if (intent.getScheme() != null && intent.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                String path;
                if (intent.getExtras() != null)
                    path = intent.getExtras().getString("AbsolutePath");
                else
                    path = UriContentUtility.getRealPathFromURI(intent.getData(), this);
                if (path != null) {
                    if (path.startsWith("/storage/sdcard0")) {
                        mFilePath = path
                                .replace("/storage/sdcard0", "/storage/emulated/0");
                    } else if (path.startsWith("/storage/extSdCard")) {
                        mFilePath = path.replace("/storage/extSdCard",
                                "/storage/emulated/0");
                    } else if (!path.startsWith("/storage/emulated/0")) {
                        //invalid path, deal with stream
                        mFilePath = UriContentUtility.writeStreamToLocal(intent, this);
                    } else
                        mFilePath = path;

                } else {
                    String dataString = intent.getDataString();
                    if (dataString.startsWith("content://")) {
                        //data stream from external application
                        mFilePath = UriContentUtility.writeStreamToLocal(intent, this);
                    }
                }

                if (mFilePath.length() > 0) {
                    mDocxDoc = new Document();
                    int ret = mDocxDoc.Open(mFilePath, docx_pswd);
                    ProcessOpenResult(ret);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.notification_label);
                    builder.setMessage(R.string.message_cannot_open_document_label);
                    builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    });
                }
            }
        }
    }

    private final View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.btn_search) {
                CheckBox searchPrevCheckBox = findViewById(R.id.check_search_prev);
                EditText input = findViewById(R.id.edit_search_input);
                String query = input.getText().toString();

                if (query.length() > 0) {
                    if (!query.equals(mSearchKey)) {
                        mSearchKey = query;
                        mDocxView.DOCXFindStart(mSearchKey, false, true);
                    }
                    if (searchPrevCheckBox.isChecked())
                        mDocxView.DOCXFind(-1);
                    else
                        mDocxView.DOCXFind(1);
                }
            }
        }
    };

    private void onFail(String msg)//treat open failed.
    {
        mDocxDoc.Close();
        mDocxDoc = null;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void ProcessOpenResult(int ret) {
        switch (ret) {
            case -1://need input password
                onFail(getString(R.string.message_open_err_password));
                break;
            case -2://unknown encryption
                onFail(getString(R.string.message_open_err_encryption));
                break;
            case -3://damaged or invalid format
                onFail(getString(R.string.message_open_err_file, "DOCX"));
                break;
            case -10://access denied or invalid file path
                onFail(getString(R.string.message_open_err_path));
                break;
            case 0://succeeded, and continue
                OpenTask task = new OpenTask(false);
                task.execute();
                break;
            default://unknown error
                onFail(getString(R.string.message_open_err_unknown));
                break;
        }
    }

    String mSearchKey = "";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_docx_reader_activity, menu);

        /*SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
        ImageView v = searchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_search);
        searchView.setSubmitButtonEnabled(true);
        LinearLayout searchViewLayout = (LinearLayout) searchView.getChildAt(0);
        CheckBox searchPrevCheckBox = new CheckBox(getApplicationContext());
        searchPrevCheckBox.setText(R.string.text_search_prev_label);
        searchPrevCheckBox.setVisibility(View.GONE);
        searchViewLayout.addView(searchPrevCheckBox);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.length() > 0) {
                    if (!query.equals(mSearchKey)) {
                        mSearchKey = query;
                        mDocxView.DOCXFindStart(mSearchKey, false, true);
                    }
                    if (searchPrevCheckBox.isChecked())
                        mDocxView.DOCXFind(-1);
                    else
                        mDocxView.DOCXFind(1);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //mSearchKey = newText;
                return false;
            }
        });
        searchView.setOnSearchClickListener(v1 -> searchPrevCheckBox.setVisibility(View.VISIBLE));
        searchView.setOnCloseListener(() -> {
            searchPrevCheckBox.setVisibility(View.GONE);
            return false;
        });*/
        if (mFilePath == null || mFilePath.length() <= 0) {
            MenuItem item = menu.findItem(R.id.action_convert_pdf);
            item.setEnabled(false);
            item.setIcon(R.drawable.ic_convert_pdf_disabled);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_vert:
                mDocxView.DOCXSetView(0);
                break;
            case R.id.action_view_horz:
                mDocxView.DOCXSetView(1);
                break;
            case R.id.action_view_page:
                mDocxView.DOCXSetView(3);
                break;
            case R.id.action_view_dual:
                mDocxView.DOCXSetView(6);
                break;
            case R.id.action_convert_pdf:
                com.radaee.pdf.Document pdfDoc = new com.radaee.pdf.Document();
                String path = mFilePath.substring(0, mFilePath.lastIndexOf("/"));
                String name = mFilePath.substring(mFilePath.lastIndexOf("/") + 1, mFilePath.lastIndexOf("."));
                name = name + ".pdf";
                path = path + File.separatorChar + name;
                pdfDoc.Create(path);
                boolean result = mDocxDoc.ExportPDF(pdfDoc);
                if (result) {
                    pdfDoc.Save();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(getString(R.string.text_convert_pdf_success_hint, path));
                    builder.setTitle(R.string.notification_label);
                    builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> {
                        dialog.dismiss();
                    });
                    builder.create().show();
                } else {
                    Toast.makeText(this, R.string.text_convert_pdf_error_hint, Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.action_share:
                if (mFilePath == null || mFilePath.length() <= 0)
                    break;
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mFilePath)));
                shareIntent.setType("*/docx");
                startActivity(Intent.createChooser(shareIntent, "Share " + mFilePath.substring(mFilePath.lastIndexOf('/'))));
                break;
            case R.id.action_help:
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                break;
            case R.id.action_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                break;
            case R.id.action_search:
                mHandler.removeMessages(HIDE_ACTION_BAR);
                if (mIsInSearchMode) {
                    item.setIcon(R.drawable.ic_search);
                    mSearchBar.animate().translationY(0);
                    mSearchBar.setVisibility(View.GONE);
                } else {
                    item.setIcon(R.drawable.ic_cancel);
                    mSearchBar.animate().translationY(mActionBar.getHeight());
                    mSearchBar.setVisibility(View.VISIBLE);
                }
                mIsInSearchMode = !mIsInSearchMode;
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * call when page scrolling.
     *
     * @param pageno
     */
    @Override
    public void OnDOCXPageChanged(int pageno) {

    }

    /**
     * call when blank tapped on page, this mean not annotation tapped.
     */
    @Override
    public void OnDOCXBlankTapped() {
        if (mIsInSearchMode)
            return;
        invalidateOptionsMenu();
        if (mActionBar.isShowing()) {
            mActionBar.hide();
        } else {
            mActionBar.show();
        }
    }

    private int dp2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale);
    }

    @Override
    public void OnDOCXSelectEnd() {
        LinearLayout layout = findViewById(R.id.text_select_menu);
        if (layout.getVisibility() == View.VISIBLE)
            layout.setVisibility(View.GONE);
    }

    /**
     * call select status end.
     *
     * @param text selected text string
     */
    @Override
    public void OnDOCXTextSelected(String text, float x, float y) {
        LinearLayout layout = findViewById(R.id.text_select_menu);

        View.OnClickListener TextSelectionListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.icon_copy) {
                    Toast.makeText(DocxReaderActivity.this, getString(R.string.text_copy_hint) + text, Toast.LENGTH_SHORT).show();
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Radaee", text);
                    clipboard.setPrimaryClip(clip);
                }
            }
        };

        layout.findViewById(R.id.icon_copy).setOnClickListener(TextSelectionListener);

        layout.setVisibility(View.VISIBLE);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int height = layout.getHeight();
        int width = layout.getWidth();
        if (x + width > displayMetrics.widthPixels)
            x = displayMetrics.widthPixels - width - dp2px(10);
        layout.setX(x);
        float yPosition = y + dp2px(30);
        if (yPosition + height > displayMetrics.heightPixels)
            yPosition = displayMetrics.heightPixels - height - dp2px(30);
        layout.setY(yPosition);
    }

    @Override
    public void OnDOCXOpenURI(String uri) {
        if (!uri.startsWith("http://") && !uri.startsWith("https://"))
            uri = "http://" + uri;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.notification_label);
        builder.setMessage(getString(R.string.message_url_jump_label, uri));
        String finalUri = uri;
        builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUri));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "todo: open url:" + finalUri, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.button_cancel_label, (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }

    /**
     * call when zoom start.
     */
    @Override
    public void OnDOCXZoomStart() {

    }

    /**
     * call when zoom end
     */
    @Override
    public void OnDOCXZoomEnd() {

    }

    @Override
    public boolean OnDOCXDoubleTapped(float x, float y) {
        return false;
    }

    @Override
    public void OnDOCXLongPressed(float x, float y) {

    }

    /**
     * call when search finished. each search shall call back each time.
     *
     * @param found
     */
    @Override
    public void OnDOCXSearchFinished(boolean found) {

    }

    /**
     * call when page displayed on screen.
     *
     * @param canvas
     * @param vpage
     */
    @Override
    public void OnDOCXPageDisplayed(Canvas canvas, IDOCXLayoutView.IVPage vpage) {

    }

    /**
     * call when page is rendered by backing thread.
     *
     * @param vpage
     */
    @Override
    public void OnDOCXPageRendered(IDOCXLayoutView.IVPage vpage) {

    }

    /*@Override
    public void OnDownloadDone(File result) {

    }

    @Override
    public void OnDownloadError(Exception e) {

    }

    @Override
    public void OnUploadDone() {

    }

    @Override
    public void OnUploadError(Exception e) {

    }*/
}
