package com.radaee.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.radaee.comm.Global;
import com.radaee.modules.fts.FTS;
import com.radaee.modules.fts.FTSGenerationTask;
import com.radaee.modules.fts.FTSResultManager;
import com.radaee.modules.fts.RadaeeFTSManager;
import com.radaee.pdf.Document;
import com.radaee.pdf.Page;
import com.radaee.pdfmaster.R;
import com.radaee.reader.PDFEditLayoutView;
import com.radaee.util.BookmarkHandler;
import com.radaee.util.CommonUtil;
import com.radaee.util.FileBrowserAdt;
import com.radaee.util.FileBrowserView;
import com.radaee.util.PDFAttListView;
import com.radaee.util.PDFThumbView;
import com.radaee.util.RDAssetStream;
import com.radaee.util.RDHttpStream;
import com.radaee.utility.DropboxUtility;
import com.radaee.utility.UriContentUtility;
import com.radaee.view.IPDFLayoutView;
import com.radaee.view.PDFViewThumb;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFReaderActivity extends AppCompatActivity implements IPDFLayoutView.PDFLayoutListener, PDFViewThumb.PDFThumbListener, DropboxUtility.DropBoxCallback {

    public enum Status {
        none,
        search,
        ink,
        line,
        rect,
        ellipse,
        note,
        text_box,
        edit_text,
        sign_field,
        radio_field,
        check_box_field,
        edit_box_field,
        polygon,
        polyline,
        stamp
    }

    static public Document ms_tran_doc;
    private RDHttpStream m_http_stream = null;
    private RDAssetStream m_asset_stream = null;
    private boolean m_modified = false;
    private boolean need_save_doc = false;
    private Document mPDFDoc = null;
    private String mDataSource = "external";

    private ImageView mFindNext;
    private ImageView mFindPrev;
    private EditText mSearchField;
    private PDFEditLayoutView mPDFView;
    private PDFThumbView mThumbView;
    private LinearLayout mSearchBar;

    private FloatingActionButton mEditFAB;
    private FloatingActionButton mEditTextFAB;
    private FloatingActionButton mAddAnnotFAB;
    private FloatingActionButton mBookmarkFAB;
    private FloatingActionButton mAddFieldFAB;

    private LinearLayout mBottomControlLayout;

    private boolean hideCancelItem = false;

    //private int mMenuID = 0;
    private String str_find;
    private ActionBar mActionBar;
    private Status mStatus = Status.none;
    private String mFilePath;
    private String mDocPass = "";

    private void onFail(String msg)//treat open failed.
    {
        mPDFDoc.Close();
        mPDFDoc = null;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void ProcessOpenResult(int ret) {
        switch (ret) {
            case -1://need input password
                inputPswd();
                break;
            case -2://unknown encryption
                onFail(getString(R.string.message_open_err_encryption));
                break;
            case -3://damaged or invalid format
                onFail(getString(R.string.message_open_err_file, "PDF"));
                break;
            case -10://access denied or invalid file path
                onFail(getString(R.string.message_open_err_path));
                break;
            case 0://succeeded, and continue
                OpenTask task = new OpenTask(false);
                new FTSGenerationTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, mFilePath, mDocPass);
                task.execute();

                //m_view.PDFOpen(m_doc, this);
                //m_controller = new PDFViewController(m_layout, m_view);
                break;
            default://unknown error
                onFail(getString(R.string.message_open_err_unknown));
                break;
        }
    }

    private void inputPswd() {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(com.radaee.viewlib.R.layout.dlg_pswd, null);
        final EditText passwordInput = layout.findViewById(com.radaee.viewlib.R.id.txt_password);

        final CheckBox showPasswordCB = layout.findViewById(com.radaee.viewlib.R.id.chk_show);
        showPasswordCB.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                passwordInput.setTransformationMethod(null);
            } else {
                passwordInput.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> {
            mDocPass = passwordInput.getText().toString();
            int ret = mPDFDoc.Open(mFilePath, mDocPass);
            if (ret == -1) {
                inputPswd();
            } else {
                ProcessOpenResult(ret);
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.button_cancel_label, (dialog, which) -> {
            dialog.dismiss();
            mPDFDoc.Close();
            finish();
        });
        builder.setTitle(com.radaee.viewlib.R.string.input_password);
        builder.setCancelable(false);
        builder.setView(layout);

        AlertDialog dlg = builder.create();
        dlg.show();
    }

    private void initFloatingActionButtons() {
        mEditFAB = findViewById(R.id.fab_edit);
        mAddAnnotFAB = findViewById(R.id.fab_add_annot);
        mBookmarkFAB = findViewById(R.id.fab_add_bookmark);
        mAddFieldFAB = findViewById(R.id.fab_add_field);
        mEditTextFAB = findViewById(R.id.item_edit_text);

        mAddAnnotFAB.hide();
        mBookmarkFAB.hide();
        mAddFieldFAB.hide();
        mEditTextFAB.hide();

        //if (mDataSource.endsWith("external")) {
        //    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
        //        mEditFAB.hide();
        //    }
        //} else
        {
            mEditFAB.setOnClickListener(FABClickListener);
            mAddAnnotFAB.setOnClickListener(FABClickListener);
            mBookmarkFAB.setOnClickListener(FABClickListener);
            mAddFieldFAB.setOnClickListener(FABClickListener);
            mEditTextFAB.setOnClickListener(FABClickListener);
        }
    }

    private boolean mIsFABOpen = false;
    /*private boolean mIsFABAnnotOpen = false;
    private boolean mIsFABFieldOpen = false;*/

    private int dp2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale);
    }

    private final View.OnClickListener FABClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.fab_edit:
                    if (!mIsFABOpen) {
                        mAddAnnotFAB.show();
                        mBookmarkFAB.show();
                        mAddFieldFAB.show();
                        mEditTextFAB.show();

                        mEditFAB.animate().rotation(180);
                        mEditTextFAB.animate().translationY(-180);
                        mAddAnnotFAB.animate().translationY(-360);
                        mAddFieldFAB.animate().translationY(-540);
                        mBookmarkFAB.animate().translationY(-720);
                        mIsFABOpen = true;
                    } else {
                        closeFABButtons();
                    }
                    break;
                case R.id.item_edit_text:
                    closeFABButtons();
                    showBottomBar(R.layout.bottom_bar_page_edit);
                    break;
                case R.id.fab_add_annot:
                    closeFABButtons();
                    showBottomBar(R.layout.bottom_bar_add_annot);
                    break;
                case R.id.fab_add_bookmark:
                    int page = m_cur_page;
                    BookmarkHandler.BookmarkStatus status = BookmarkHandler.addToBookmarks(mFilePath, page, getString(R.string.bookmark_label, m_cur_page + 1));
                    AlertDialog.Builder builder = new AlertDialog.Builder(PDFReaderActivity.this);
                    builder.setTitle(R.string.notification_label);
                    if (status == BookmarkHandler.BookmarkStatus.SUCCESS) {
                        builder.setMessage(R.string.message_add_bookmark_success_label);
                    } else if (status == BookmarkHandler.BookmarkStatus.ALREADY_ADDED) {
                        builder.setMessage(R.string.message_bookmark_exist_label);
                    } else {
                        builder.setMessage(R.string.message_bookmark_error_label);
                    }
                    builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
                    builder.create().show();
                    break;
                case R.id.fab_add_field:
                    showBottomBar(R.layout.bottom_bar_add_field);
                    closeFABButtons();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 10000)
            return;
        if (resultCode == PageEditActivity.RESULT_PAGE) {
            int page = data.getIntExtra("page", 0);
            mPDFView.PDFGotoPage(page);
        } else if (resultCode == PageEditActivity.RESULT_CONFIRM) {
            boolean removal[] = data.getBooleanArrayExtra("removal");
            int rotate[] = data.getIntArrayExtra("rotate");
            if (removal == null || rotate == null) return;

            mPDFView.PDFSaveView();
            mThumbView.thumbSave();

            Document doc = mPDFView.PDFGetDoc();

            int pcnt = removal.length;
            int pcur = pcnt;
            while (pcur > 0) {
                pcur--;
                if (removal[pcur])
                    doc.RemovePage(pcur);
                else if ((rotate[pcur] >> 16) != (rotate[pcur] & 0xFFFF)) {
                    Page page = doc.GetPage(pcur);
                    page.UpdateWithPGEditor();//required if you are using edit mode.
                    page.Close();
                    doc.SetPageRotate(pcur, rotate[pcur] & 0xFFFF);
                }
            }
            mThumbView.thumbRestore();
            mPDFView.PDFRestoreView();
            m_modified = true;
        }
    }

    private void closeFABButtons() {

        mEditFAB.animate().rotationBy(-180);

        mAddAnnotFAB.animate().translationY(0);
        mBookmarkFAB.animate().translationY(0);
        mAddFieldFAB.animate().translationY(0);
        mEditTextFAB.animate().translationY(0);

        mAddAnnotFAB.hide();
        mBookmarkFAB.hide();
        mAddAnnotFAB.hide();
        mAddFieldFAB.hide();
        mEditTextFAB.hide();
        mIsFABOpen = false;
    }

    /**
     * call when page changed.
     *
     * @param pageno
     */
    @Override
    public void OnPDFPageModified(int pageno) {
        m_modified = true;
        if (mThumbView != null) mThumbView.thumbUpdatePage(pageno);//render thumbnail again.
    }

    private int m_cur_page = 0;

    /**
     * call when page scrolling.
     *
     * @param pageno
     */
    @Override
    public void OnPDFPageChanged(int pageno) {
        m_cur_page = pageno;
        if (mThumbView != null) mThumbView.thumbGotoPage(pageno);
    }

    /**
     * call when annotation tapped.
     *
     * @param pno
     * @param annot
     */
    @Override
    public void OnPDFAnnotTapped(int pno, Page.Annotation annot) {
        if (pno < 0 && annot == null)//restore to initailize UI.
        {
            mStatus = Status.none;
            /*mMenuID = R.menu.menu_pdf_reader_activity_main;
            invalidateOptionsMenu();*/
            mEditFAB.show();
        }
    }

    /**
     * call when blank tapped on page, this mean not annotation tapped.
     */
    @Override
    public void OnPDFBlankTapped(int pageno) {
        if (mStatus != Status.none)
            return;
        if (mIsFABOpen) {
            closeFABButtons();
            return;
        }
        invalidateOptionsMenu();
        if (mActionBar.isShowing()) {
            mActionBar.hide();
            mThumbView.setVisibility(View.GONE);
        } else {
            mActionBar.show();
            mThumbView.setVisibility(View.VISIBLE);
        }
        if (mBottomControlLayout.getVisibility() == View.VISIBLE && mIsBottomBarCanHide) {
            hideBottomBar();
        }
    }

    @Override
    public void OnPDFSelectEnd(){
        LinearLayout layout = findViewById(R.id.text_select_menu);
        if(layout.getVisibility() == View.VISIBLE)
            layout.setVisibility(View.GONE);
    }

    /**
     * call select status end.
     *
     * @param text selected text string
     */
    @Override
    public void OnPDFTextSelected(String text, float x, float y) {
        LinearLayout layout = findViewById(R.id.text_select_menu);
        //final RadioGroup rad_group = layout.findViewById(com.radaee.viewlib.R.id.rad_group);

        View.OnClickListener TextSelectionListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean ret = true;
                switch (v.getId()){
                    case R.id.icon_copy:
                        Toast.makeText(PDFReaderActivity.this, getString(R.string.text_copy_hint) + text, Toast.LENGTH_SHORT).show();
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Radaee", text);
                        clipboard.setPrimaryClip(clip);
                        break;
                    case R.id.icon_highlight:
                        ret = mPDFView.PDFSetSelMarkup(0);
                        break;
                    case R.id.icon_underline:
                        ret = mPDFView.PDFSetSelMarkup(1);
                        break;
                    case R.id.icon_strikeout:
                        ret = mPDFView.PDFSetSelMarkup(2);
                        break;
                    case R.id.icon_squiggly:
                        ret = mPDFView.PDFSetSelMarkup(4);
                        break;
                    case R.id.icon_eraser:
                        ret = mPDFView.PDFEraseSel();
                        break;
                }
                if (!ret) {
                    Toast.makeText(PDFReaderActivity.this, getString(R.string.text_add_annot_failed_hint), Toast.LENGTH_SHORT).show();
                }
            }
        };

        layout.findViewById(R.id.icon_copy).setOnClickListener(TextSelectionListener);
        if (!mPDFDoc.CanSave()) {
            layout.findViewById(R.id.icon_highlight).setVisibility(View.GONE);
            layout.findViewById(R.id.icon_underline).setVisibility(View.GONE);
            layout.findViewById(R.id.icon_strikeout).setVisibility(View.GONE);
            layout.findViewById(R.id.icon_squiggly).setVisibility(View.GONE);
            layout.findViewById(R.id.icon_eraser).setVisibility(View.GONE);
        } else {
            layout.findViewById(R.id.icon_highlight).setOnClickListener(TextSelectionListener);
            layout.findViewById(R.id.icon_underline).setOnClickListener(TextSelectionListener);
            layout.findViewById(R.id.icon_strikeout).setOnClickListener(TextSelectionListener);
            layout.findViewById(R.id.icon_squiggly).setOnClickListener(TextSelectionListener);
            layout.findViewById(R.id.icon_eraser).setOnClickListener(TextSelectionListener);
        }

        layout.setVisibility(View.VISIBLE);
        DisplayMetrics displayMetrics =  getResources().getDisplayMetrics();
        int height = layout.getHeight();
        int width = layout.getWidth();
        if(x + width > displayMetrics.widthPixels)
            x = displayMetrics.widthPixels - width - dp2px(10);
        layout.setX(x);
        float yPosition = y + dp2px(30);
        if (yPosition + height > displayMetrics.heightPixels)
            yPosition = displayMetrics.heightPixels - height - dp2px(30);
        layout.setY(yPosition);
    }

    @Override
    public void OnPDFOpenURI(String uri) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.notification_label);
        builder.setMessage(getString(R.string.message_url_jump_label, uri));
        Pattern EMAIL_PATTERN = Pattern.compile(
                "^mailto:([a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)");
        Matcher matcher = EMAIL_PATTERN.matcher(uri);
        if (!uri.startsWith("http://") && !uri.startsWith("https://") && !matcher.matches())
            uri = "http://" + uri;
        String finalUri = uri;
        builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> {
            try {
                if (matcher.matches()) {
                    String address = matcher.group(1);
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
                    try {
                        startActivity(Intent.createChooser(i, null));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUri));
                    startActivity(browserIntent);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open url:" + finalUri, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        builder.setNeutralButton(R.string.copy_url, (dialog, view) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip;
            if (matcher.matches()) {
                String address = matcher.group(1);
                clip = ClipData.newPlainText("", address);
            } else {
                clip = ClipData.newPlainText("", finalUri);
            }
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.success_copy_url, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.button_cancel_label, (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
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

    /**
     * call when zoom start.
     */
    @Override
    public void OnPDFZoomStart() {

    }

    /**
     * call when zoom end
     */
    @Override
    public void OnPDFZoomEnd() {

    }

    @Override
    public boolean OnPDFDoubleTapped(float x, float y) {
        return false;
    }

    @Override
    public void OnPDFLongPressed(float x, float y) {

    }

    /**
     * call when search finished. each search shall call back each time.
     *
     * @param found
     */
    @Override
    public void OnPDFSearchFinished(boolean found) {

    }

    /**
     * call when page displayed on screen.
     *
     * @param canvas
     * @param vpage
     */
    @Override
    public void OnPDFPageDisplayed(Canvas canvas, IPDFLayoutView.IVPage vpage) {

    }

    /**
     * call when page is rendered by backing thread.
     *
     * @param vpage
     */
    @Override
    public void OnPDFPageRendered(IPDFLayoutView.IVPage vpage) {

    }

    @Override
    public void OnPageClicked(int pageno) {
        mPDFView.PDFGotoPage(pageno);
    }

    @SuppressLint("StaticFieldLeak")
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
            mPDFDoc.GetPagesMaxSize();//it may spend much time for first invoking this method.
            return null;
        }

        @Override
        protected void onPreExecute() {
            handler = new Handler();
            runable = () -> dlg = ProgressDialog.show(PDFReaderActivity.this, getString(R.string.message_wait_label), getString(R.string.message_loading_label, "PDF"), true);
            handler.postDelayed(runable, 1000);//delay 1 second to display progress dialog.
        }

        @Override
        protected void onPostExecute(Integer integer) {
            mPDFView.PDFOpen(mPDFDoc, PDFReaderActivity.this);
            mThumbView.thumbOpen(mPDFDoc, PDFReaderActivity.this, false);
            //m_controller = new PDFViewController(m_layout, mPDFView);
            need_save_doc = need_save;
            if (dlg != null)
                dlg.dismiss();
            else
                handler.removeCallbacks(runable);
        }
    }

    private static final int HIDE_ACTION_BAR = 0;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_ACTION_BAR:
                    if (mActionBar != null)
                        mActionBar.hide();
                    mThumbView.setVisibility(View.GONE);
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

        setContentView(R.layout.activity_pdf_reader);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN /*| WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE*/);

        mActionBar = getSupportActionBar();
        mHandler.sendEmptyMessageDelayed(HIDE_ACTION_BAR, 3000);

        mPDFView = findViewById(R.id.pdf_view);
        mThumbView = findViewById(R.id.thumbs);
        mSearchBar = findViewById(R.id.layout_search);

        mSearchField = findViewById(R.id.edit_search_input);
        mSearchField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                activateSearch(1, true);
                return true;
            }
            return false;
        });
        mSearchField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        ImageView mSearchBtn = findViewById(R.id.btn_search);
        mSearchBtn.setOnClickListener(buttonClickListener);
        mFindPrev = findViewById(R.id.search_prev);
        mFindPrev.setOnClickListener(buttonClickListener);
        mFindNext = findViewById(R.id.search_next);
        mFindNext.setOnClickListener(buttonClickListener);
        mBottomControlLayout = findViewById(R.id.bottom_control_layout);

        Intent intent = getIntent();
        if (intent.hasExtra("data_source"))
            mDataSource = intent.getStringExtra("data_source");
        initFloatingActionButtons();
        if (ms_tran_doc != null) {
            mPDFDoc = ms_tran_doc;
            ms_tran_doc = null;

            OpenTask task = new OpenTask(true);
            task.execute();
            mFilePath = mPDFDoc.getDocPath();

            //List<BookmarkHandler.Bookmark> mBookmarkList = BookmarkHandler.getBookmarks(mFilePath);
        } else {
            String pdf_asset = intent.getStringExtra("PDFAsset");
            String pdf_path = intent.getStringExtra("PDFPath");
            mDocPass = intent.getStringExtra("PDFPswd");
            String pdf_http = intent.getStringExtra("PDFHttp");
            if (pdf_http != null && !pdf_http.equals("")) {
                m_http_stream = new RDHttpStream();
                m_http_stream.open(pdf_http);
                mPDFDoc = new Document();
                int ret = mPDFDoc.OpenStream(m_http_stream, mDocPass);

                ProcessOpenResult(ret);
            } else if (pdf_asset != null && !pdf_asset.equals("")) {
                m_asset_stream = new RDAssetStream();
                m_asset_stream.open(getAssets(), pdf_asset);
                mPDFDoc = new Document();
                int ret = mPDFDoc.OpenStream(m_asset_stream, mDocPass);
                ProcessOpenResult(ret);
            } else if (pdf_path != null && !pdf_path.equals("")) {
                mPDFDoc = new Document();
                int ret = mPDFDoc.Open(pdf_path, mDocPass);
                mFilePath = pdf_path;
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
                    mPDFDoc = new Document();
                    int ret = mPDFDoc.Open(mFilePath, mDocPass);
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

    private boolean mIsBottomBarCanHide = true;

    private void showBottomBar(int viewID) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(viewID, null);
        if (mBottomControlLayout.getVisibility() == View.VISIBLE)
            hideBottomBar();

        mBottomControlLayout.removeAllViewsInLayout();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        if (viewID == R.layout.bottom_bar_annot && hideCancelItem && view.findViewById(R.id.action_cancel) != null) {
            view.findViewById(R.id.action_cancel).setVisibility(View.GONE);
        }
        mIsBottomBarCanHide = !(viewID == R.layout.bottom_bar_annot);
        mBottomControlLayout.addView(view, params);
        mBottomControlLayout.setVisibility(View.VISIBLE);

        /*ObjectAnimator animation = ObjectAnimator.ofFloat(mBottomControlLayout, "translationY", getResources().getDisplayMetrics().heightPixels - 0);
        animation.setDuration(2000);
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBottomControlLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animation.start();*/
    }

    private void hideBottomBar() {
        mBottomControlLayout.setVisibility(View.GONE);
    }

    public void action_confirm(View v) {
        switch (mStatus) {
            case ink:
                mPDFView.PDFSetInk(1);
                break;
            case line:
                mPDFView.PDFSetLine(1);
                break;
            case rect:
                mPDFView.PDFSetRect(1);
                break;
            case ellipse:
                mPDFView.PDFSetEllipse(1);
                break;
            case note:
                mPDFView.PDFSetNote(1);
                break;
            case edit_text:
                mPDFView.PDFSetEdit(1);
                break;
            case text_box:
                mPDFView.PDFSetEditbox(1);
                break;
            case sign_field:
                mPDFView.PDFSetFieldSign(1);
                break;
            case radio_field:
                mPDFView.PDFSetFieldRadio(1);
                break;
            case check_box_field:
                mPDFView.PDFSetFieldCheckbox(1);
                break;
            case edit_box_field:
                mPDFView.PDFSetFieldEditbox(1);
                break;
            case polygon:
                mPDFView.PDFSetPolygon(1);
                break;
            case polyline:
                mPDFView.PDFSetPolyline(1);
                break;
            case stamp:
                mPDFView.PDFSetStamp(1);
                break;
            default:
                break;
        }
        mStatus = Status.none;
        m_modified = true;
        mEditFAB.show();
        hideBottomBar();
    }

    public void action_cancel(View v) {
        switch (mStatus) {
            case ink:
                mPDFView.PDFSetInk(2);
                break;
            case line:
                mPDFView.PDFSetLine(2);
                break;
            case rect:
                mPDFView.PDFSetRect(2);
                break;
            case ellipse:
                mPDFView.PDFSetEllipse(2);
                break;
            case note:
                mPDFView.PDFSetNote(2);
                break;
            case edit_text:
                mPDFView.PDFSetEdit(2);
                break;
            case text_box:
                mPDFView.PDFSetEditbox(2);
                break;
            case sign_field:
                mPDFView.PDFSetFieldSign(2);
                break;
            case radio_field:
                mPDFView.PDFSetFieldRadio(2);
                break;
            case check_box_field:
                mPDFView.PDFSetFieldCheckbox(2);
                break;
            case edit_box_field:
                mPDFView.PDFSetFieldEditbox(2);
                break;
            case polygon:
                mPDFView.PDFSetPolygon(2);
                break;
            case polyline:
                mPDFView.PDFSetPolyline(2);
                break;
            case stamp:
                mPDFView.PDFSetStamp(2);
                break;
            default:
                break;
        }
        mStatus = Status.none;
        mEditFAB.show();
        hideBottomBar();
    }

    public void item_edit_text(View v) {
        mPDFView.PDFSetEdit(0);
        mStatus = Status.edit_text;
        showBottomBar(R.layout.bottom_bar_annot);
    }

    public void item_edit_page(View v) {
        Intent intent = new Intent();
        intent.setClass(PDFReaderActivity.this, PageEditActivity.class);
        PageEditActivity.ms_tran_doc = mPDFDoc;
        startActivityForResult(intent, 10000);
    }

    public void image_annot_text_box(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = true;
        mEditFAB.hide();
        mPDFView.PDFSetEditbox(0);
        mStatus = Status.text_box;
    }

    public void image_annot_ink(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetInk(0);
        mStatus = Status.ink;
    }

    public void image_annot_note(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetNote(0);
        mStatus = Status.note;
    }

    public void image_annot_ellipse(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetEllipse(0);
        mStatus = Status.ellipse;
    }

    public void image_annot_rect(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetRect(0);
        mStatus = Status.rect;
    }

    public void image_annot_line(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetLine(0);
        mStatus = Status.line;
    }

    public void image_annot_polyline(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetPolyline(0);
        mStatus = Status.polyline;
    }

    public void image_annot_polygon(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetPolygon(0);
        mStatus = Status.polygon;
    }

    public void item_field_sign(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetFieldSign(0);
        mStatus = Status.sign_field;
    }

    public void item_field_checkbox(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetFieldCheckbox(0);
        mStatus = Status.check_box_field;
    }

    public void item_field_radio(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetFieldRadio(0);
        mStatus = Status.radio_field;
    }

    public void item_field_edit_box(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = true;
        mEditFAB.hide();
        mPDFView.PDFSetFieldEditbox(0);
        mStatus = Status.edit_box_field;
    }

    public void item_annot_stamp(View v) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        showBottomBar(R.layout.bottom_bar_annot);
        hideCancelItem = false;
        mEditFAB.hide();
        mPDFView.PDFSetStamp(0);
        mStatus = Status.stamp;
    }

    private void showMeta() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        RelativeLayout lay_meta = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.dlg_meta, null);
        EditText editbox;
        editbox = lay_meta.findViewById(R.id.edit_title);
        editbox.setText(mPDFDoc.GetMeta("Title"));
        editbox = lay_meta.findViewById(R.id.edit_author);
        editbox.setText(mPDFDoc.GetMeta("Author"));
        editbox = lay_meta.findViewById(R.id.edit_subject);
        editbox.setText(mPDFDoc.GetMeta("Subject"));
        editbox = lay_meta.findViewById(R.id.edit_keywords);
        editbox.setText(mPDFDoc.GetMeta("Keywords"));
        editbox = lay_meta.findViewById(R.id.edit_creator);
        editbox.setText(mPDFDoc.GetMeta("Creator"));
        editbox = lay_meta.findViewById(R.id.edit_producer);
        editbox.setText(mPDFDoc.GetMeta("Producer"));
        TextView txtv;
        txtv = lay_meta.findViewById(com.radaee.viewlib.R.id.txt_pdfa);
        String spdfa = mPDFDoc.GetMeta("pdf/a");
        if (spdfa == null || spdfa.isEmpty())
            txtv.setText("None");
        else
            txtv.setText(spdfa);
        txtv = lay_meta.findViewById(com.radaee.viewlib.R.id.txt_create);
        spdfa = mPDFDoc.GetMeta("CreationDate");
        txtv.setText(mPDFDoc.GetMeta("CreationDate"));
        txtv = lay_meta.findViewById(com.radaee.viewlib.R.id.txt_modify);
        txtv.setText(mPDFDoc.GetMeta("ModDate"));

        builder.setView(lay_meta);
        builder.setTitle(R.string.text_meta_label);
        builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> {
            EditText editbox1;
            editbox1 = lay_meta.findViewById(R.id.edit_title);
            mPDFDoc.SetMeta("Title", editbox1.getText().toString());
            editbox1 = lay_meta.findViewById(R.id.edit_author);
            mPDFDoc.SetMeta("Author", editbox1.getText().toString());
            editbox1 = lay_meta.findViewById(R.id.edit_subject);
            mPDFDoc.SetMeta("Subject", editbox1.getText().toString());
            editbox1 = lay_meta.findViewById(R.id.edit_keywords);
            mPDFDoc.SetMeta("Keywords", editbox1.getText().toString());
            editbox1 = lay_meta.findViewById(R.id.edit_creator);
            mPDFDoc.SetMeta("Creator", editbox1.getText().toString());
            editbox1 = lay_meta.findViewById(R.id.edit_producer);
            mPDFDoc.SetMeta("Producer", editbox1.getText().toString());
            m_modified = true;
        });
        builder.setNegativeButton(R.string.button_cancel_label, (dialog, which) -> {
        });
        builder.create().show();
    }

    private void showAttachmentList() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(com.radaee.viewlib.R.layout.dlg_browser, null);
        RelativeLayout lay_dlg = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.dlg_attachment, null);
        PDFAttListView lst_attach = lay_dlg.findViewById(R.id.lst_attach);
        lst_attach.init(mPDFDoc, item -> m_modified = true);
        ImageView btn_add = lay_dlg.findViewById(R.id.btn_add);
        btn_add.setOnClickListener(v -> {
            //Toast.makeText(PDFReaderActivity.this, R.string.hint_not_support, Toast.LENGTH_SHORT).show();
            //open browser dialog.
            AlertDialog.Builder builder = new AlertDialog.Builder(PDFReaderActivity.this);
            builder.setTitle(R.string.text_title_label);
            builder.setView(content);//.setView(com.radaee.viewlib.R.layout.dlg_browser);
            builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
            AlertDialog dlg = builder.create();
            dlg.setOnShowListener(dialog -> {
                FileBrowserView fb_view = dlg.findViewById(com.radaee.viewlib.R.id.fb_view);
                TextView txt_filter = dlg.findViewById(com.radaee.viewlib.R.id.txt_filter);
                txt_filter.setText("*.*");
                fb_view.FileInit("/storage/emulated/0", null);
                fb_view.setOnItemClickListener((parent, view, position, id) -> {
                    FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                    if (item.m_item.is_dir())
                        fb_view.FileGotoSubdir(item.m_item.get_name());
                    else {
                        String spath = item.m_item.get_path();
                        mPDFDoc.NewEmbedFile(spath);
                        lst_attach.updateList();
                        dlg.dismiss();
                        m_modified = true;
                    }
                });
            });

            dlg.show();
        });

        lay_dlg.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, mPDFView.getHeight() * 3 / 5);
            v.setLayoutParams(lp);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(lay_dlg);
        builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
        AlertDialog dlg = builder.create();
        dlg.show();
    }

    private final View.OnClickListener buttonClickListener = v -> {
        EditText input = findViewById(R.id.edit_search_input);
        String query = input.getText().toString();
        if (v.getId() == R.id.btn_search) {
            if (query.length() <= 0)
                return;
            activateSearch(1, true);
        } else if (v.getId() == R.id.search_prev)
            activateSearch(-1, false);
        else if (v.getId() == R.id.search_next)
            activateSearch(1, false);
    };

    @Override
    public void onResume() {
        super.onResume();
        if (mPDFDoc == null)
            mPDFDoc = mPDFView.PDFGetDoc();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        mPDFView.BundleSavePos(savedInstanceState);
        if (need_save_doc && mPDFDoc != null) {
            Document.BundleSave(savedInstanceState, mPDFDoc);//save Document object
            mPDFDoc = null;
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mPDFDoc == null) {
            mPDFDoc = Document.BundleRestore(savedInstanceState);//restore Document object
            mPDFView.PDFOpen(mPDFDoc, this);
            //m_controller = new PDFViewController(m_layout, m_view);
            need_save_doc = true;
        }
        mPDFView.BundleRestorePos(savedInstanceState);
    }

    private void activateSearch(int direction, boolean actionSearch) {
        String val = mSearchField.getText().toString();
        val = CommonUtil.bidiFormatCheck(val);
        if (!TextUtils.isEmpty(val)) {
            if (RadaeeFTSManager.getSearchType() == 1 && val.trim().length() < RadaeeFTSManager.FTS_QUERY_MIN_LENGTH) {
                //FTS search type, word to search too short
                Toast.makeText(this, "Enter at least " + RadaeeFTSManager.FTS_QUERY_MIN_LENGTH +
                        " characters to start searching", Toast.LENGTH_SHORT).show();
                return;
            }

            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(mSearchField.getWindowToken(), 0);
            if (val.equals(str_find)) {
                if (RadaeeFTSManager.getSearchType() == 1 && RadaeeFTSManager.sFTSValid) {
                    if (actionSearch)
                        FTSResultManager.reShowResults();
                    else
                        FTSResultManager.gotoResult(this, direction);
                } else
                    mPDFView.PDFFind(direction);
            } else {
                str_find = val;
                if (RadaeeFTSManager.getSearchType() == 1) {
                    ProgressDialog progressDialog = ProgressDialog.show(this, "Searching",
                            getString(com.radaee.viewlib.R.string.loading), true, false);
                    try {
                        List<FTS> ftsResult = RadaeeFTSManager.search(this, val, mPDFView.PDFGetDoc());
                        if (ftsResult != null && ftsResult.size() > 0)
                            mThumbView.setVisibility(View.GONE);
                        FTSResultManager.showSearchResult(this, ftsResult, val, (clickedItem, position) -> {
                            mFindPrev.setColorFilter(ContextCompat.getColor(this, R.color.actionButtonBG), PorterDuff.Mode.SRC_IN);
                            mFindNext.setColorFilter(ContextCompat.getColor(this, R.color.actionButtonBG), PorterDuff.Mode.SRC_IN);
                            if (position == 0) //disable prev button
                                mFindPrev.setColorFilter(ContextCompat.getColor(this, R.color.actionBarTextColor), PorterDuff.Mode.SRC_IN);
                            if (position == ftsResult.size() - 1) //disable next button
                                mFindNext.setColorFilter(ContextCompat.getColor(this, R.color.actionBarTextColor), PorterDuff.Mode.SRC_IN);
                            mPDFView.PDFGotoPage(clickedItem.getPageIndex());
                            new Handler().postDelayed(() -> {
                                mPDFView.PDFSetFTSRect(new float[]{(float) clickedItem.getRectLeft(), (float) clickedItem.getRectTop(),
                                        (float) clickedItem.getRectRight(), (float) clickedItem.getRectBottom()}, clickedItem.getPageIndex());
                            }, 100);
                        });
                        progressDialog.dismiss();
                        return;
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                }
                mPDFView.PDFFindStart(val, false, false);
                mPDFView.PDFFind(direction);
            }
        }
    }

    /*private void onFindPrev(String key) {
        String str = key;
        if (str_find != null) {
            if (str != null && str.compareTo(str_find) == 0) {
                mPDFView.PDFFind(-1);
                return;
            }
        }
        if (str != null && str.length() > 0) {
            mPDFView.PDFFindStart(str, false, false);
            mPDFView.PDFFind(1);
            str_find = str;
        }
    }

    private void onFindNext(String key) {
        String str = key;
        if (str_find != null) {
            if (str != null && str.compareTo(str_find) == 0) {
                mPDFView.PDFFind(1);
                return;
            }
        }
        if (str != null && str.length() > 0) {
            mPDFView.PDFFindStart(str, false, false);
            mPDFView.PDFFind(1);
            str_find = str;
        }
    }*/

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.
     *
     * <p>This is only called once, the first time the options menu is
     * displayed.  To update the menu every time it is displayed, see
     * {@link #onPrepareOptionsMenu}.
     *
     * <p>The default implementation populates the menu with standard system
     * menu items.  These are placed in the {@link Menu#CATEGORY_SYSTEM} group so that
     * they will be correctly ordered with application-defined menu items.
     * Deriving classes should always call through to the base implementation.
     *
     * <p>You can safely hold on to <var>menu</var> (and any items created
     * from it), making modifications to it as desired, until the next
     * time onCreateOptionsMenu() is called.
     *
     * <p>When you add items to the menu, you can implement the Activity's
     * {@link #onOptionsItemSelected} method to handle them there.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*if (mMenuID == 0)
            mMenuID = R.menu.menu_pdf_reader_activity_main;*/

        getMenuInflater().inflate(R.menu.menu_pdf_reader_activity_main, menu);

        //if (mMenuID == R.menu.menu_pdf_reader_activity_main) {
        hideCancelItem = false;
            /*
            if (mDataSource.endsWith("external") && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                if (menu.findItem(R.id.action_share) != null)
                    menu.findItem(R.id.action_share).setEnabled(false);
            }
             */
        if (mPDFView.PDFCanRedo())
            menu.findItem(R.id.action_redo).setIcon(R.drawable.ic_redo);
        else
            menu.findItem(R.id.action_redo).setIcon(R.drawable.ic_redo_disabled);

        if (mPDFView.PDFCanUndo())
            menu.findItem(R.id.action_undo).setIcon(R.drawable.ic_undo);
        else
            menu.findItem(R.id.action_undo).setIcon(R.drawable.ic_undo_disabled);

        if (mPDFDoc == null) {
            menu.findItem(R.id.action_menu).setIcon(R.drawable.ic_menu_disabled);
            menu.findItem(R.id.action_menu).setEnabled(false);
        } else if (mPDFDoc.GetOutlines() == null) {
            menu.findItem(R.id.action_menu).setIcon(R.drawable.ic_menu_disabled);
            menu.findItem(R.id.action_menu).setEnabled(false);
        }
        /*} else if (mMenuID == R.menu.menu_pdf_reader_activity_annot && hideCancelItem) {
            menu.findItem(R.id.action_cancel).setVisible(false);
        }*/
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.</p>
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mHandler.removeMessages(HIDE_ACTION_BAR);
        switch (item.getItemId()) {
            case R.id.action_undo:
                if (mPDFView.PDFCanUndo())
                    mPDFView.PDFUndo();
                invalidateOptionsMenu();
                break;
            case R.id.action_redo:
                if (mPDFView.PDFCanRedo())
                    mPDFView.PDFRedo();
                invalidateOptionsMenu();
                break;
            case R.id.action_view_vert:
                Global.g_view_mode = 0;
                mPDFView.PDFSetView(0);
                break;
            case R.id.action_view_horz:
                Global.g_view_mode = 1;
                mPDFView.PDFSetView(1);
                break;
            case R.id.action_view_page:
                Global.g_view_mode = 3;
                mPDFView.PDFSetView(3);
                break;
            /*case R.id.action_view_reflow:
                break;*/
            case R.id.action_view_dual:
                Global.g_view_mode = 6;
                mPDFView.PDFSetView(6);
                break;
            case R.id.action_help:
                Intent helpIntent = new Intent(this, HelpActivity.class);
                startActivity(helpIntent);
                break;
            case R.id.action_book_mark:
                BookmarkHandler.showBookmarks(this, mFilePath, pageno -> {
                    mPDFView.PDFGotoPage(pageno);
                });
                break;
            case R.id.action_share:
                if (mFilePath == null || mFilePath.length() <= 0)
                    break;
                if (mPDFView.PDFCanSave())
                    mPDFView.PDFSave();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mFilePath)));
                shareIntent.setType("*/pdf");
                startActivity(Intent.createChooser(shareIntent, "Share " + mFilePath.substring(mFilePath.lastIndexOf('/'))));
                break;
            case R.id.action_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                break;
            case R.id.action_menu:
                CommonUtil.showPDFOutlines(mPDFView, this);
                break;
            case R.id.action_search:
                if (mStatus == Status.search) {
                    mStatus = Status.none;
                    item.setIcon(R.drawable.ic_search);
                    mSearchBar.animate().translationY(0);
                    mSearchBar.setVisibility(View.GONE);
                    str_find = null;
                    if (RadaeeFTSManager.getSearchType() == 1) {
                        mPDFView.PDFSetFTSRect(null, -1);
                    } else
                        mPDFView.PDFFindEnd();
                    mThumbView.setVisibility(View.VISIBLE);
                } else {
                    mStatus = Status.search;
                    item.setIcon(R.drawable.ic_cancel);
                    mSearchBar.animate().translationY(mActionBar.getHeight());
                    mSearchBar.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.action_attachment:
                showAttachmentList();
                break;
            case R.id.action_meta:
                showMeta();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the activity has detected the user's press of the back
     * key. The {@link #getOnBackPressedDispatcher() OnBackPressedDispatcher} will be given a
     * chance to handle the back button before the default behavior of
     * {@link Activity#onBackPressed()} is invoked.
     *
     * @see #getOnBackPressedDispatcher()
     */
    @Override
    public void onBackPressed() {
        if (mDataSource.equals("dropbox") && m_modified && mPDFDoc.CanSave()) {
            //save & upload
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.upload_to_dropbox_label);
            builder.setTitle(R.string.notification_label);
            builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> {
                if (mPDFView.PDFCanSave())
                    mPDFView.PDFSave();
                dialog.dismiss();
                Uri uri = Uri.fromFile(new File((mFilePath)));
                DropboxUtility.uploadFile(uri.toString(), this, mFilePath, this);
            });
            builder.setNegativeButton(R.string.button_negative_label, (dialog, which) -> {
                dialog.dismiss();
                finish();
            });
            builder.setNeutralButton(R.string.button_cancel_label, ((dialog, which) -> dialog.dismiss()));
            builder.create().show();
        } else if (m_modified && mPDFDoc.CanSave()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.notification_label);
            builder.setMessage(R.string.notification_save_label);
            builder.setPositiveButton(R.string.button_positive_label, (dialog, which) -> {
                if (mPDFView.PDFCanSave())
                    mPDFView.PDFSave();
                dialog.dismiss();
                finish();
            });
            builder.setNegativeButton(R.string.button_negative_label, ((dialog, which) -> {
                dialog.dismiss();
                finish();
            }));
            builder.setNeutralButton(R.string.button_cancel_label, ((dialog, which) -> dialog.dismiss()));
            builder.create().show();
        } else
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mPDFDoc != null) {
            mPDFView.PDFClose();
            mPDFDoc.Close();
            mPDFDoc = null;
        }
        if (m_asset_stream != null) {
            m_asset_stream.close();
            m_asset_stream = null;
        }
        if (m_http_stream != null) {
            m_http_stream.close();
            m_http_stream = null;
        }
        Global.RemoveTmp();
        super.onDestroy();
    }

    @Override
    public void OnDownloadDone(File result) {

    }

    @Override
    public void OnDownloadError(Exception e) {

    }

    @Override
    public void OnUploadDone() {
        finish();
    }

    @Override
    public void OnUploadError(Exception e) {
        Toast.makeText(this,
                        "An error has occurred",
                        Toast.LENGTH_SHORT)
                .show();
    }
}
