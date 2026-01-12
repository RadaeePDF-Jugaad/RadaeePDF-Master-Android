package com.radaee.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.radaee.activities.fragments.DocNavFragment;
import com.radaee.activities.fragments.DocRecentFragment;
import com.radaee.activities.fragments.DropboxFragment;
import com.radaee.activities.fragments.PDFToolFragment;
import com.radaee.comm.Global;
import com.radaee.pdfmaster.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int CODE_RECENT_FILE_FRAGMENT = 0;
    private final int CODE_BROWSE_FILE_FRAGMENT = 1;
    private final int CODE_DROPBOX_FRAGMENT = 2;
    private final int CODE_PDF_TOOL_FRAGMENT = 3;

    private final int OPEN_FILE_REQUEST_CODE = 0;

    private DocRecentFragment mDocRecentFragment;
    private DocNavFragment mDocNavFragment;
    private DropboxFragment mDropboxFragment;
    private PDFToolFragment mPDFToolFragment;
    private ImageView mBrowseFileButton;
    private ImageView mRecentFileButton;
    private ImageView mDropboxButton;
    private ImageView mPDFToolButton;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.Init(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Environment.isExternalStorageManager()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(R.string.permission_dialog_content)
                    .setPositiveButton(R.string.button_ok_label, ((dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 1000);

                        dialog.dismiss();
                    }))
                    .setNegativeButton(R.string.text_cancel_label, ((dialog, which) -> {
                        dialog.dismiss();
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.permission_dialog_title)
                                .setMessage(R.string.permission_denied_content)
                                .setPositiveButton(R.string.button_ok_label, ((d, w) -> d.dismiss()))
                                .show();
                    }))
                    .show();
        }
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences("pref", 0);
        boolean firstRun = settings.getBoolean("first_run", true);
        if (firstRun) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("first_run", false);
            editor.apply();
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        }
        mRecentFileButton = findViewById(R.id.btn_recent_file);
        mRecentFileButton.setOnClickListener(this);
        mBrowseFileButton = findViewById(R.id.btn_brows_file);
        mBrowseFileButton.setOnClickListener(this);
        mDropboxButton = findViewById(R.id.btn_dropbox);
        mDropboxButton.setOnClickListener(this);

        mPDFToolButton = findViewById(R.id.btn_pdf_tool);
        mPDFToolButton.setOnClickListener(this);

        mDocRecentFragment = new DocRecentFragment();
        mDocNavFragment = new DocNavFragment(getIntent().getStringExtra("ENGINE"));
        mDropboxFragment = new DropboxFragment(this);

        mPDFToolFragment = new PDFToolFragment();

        switchFragment(CODE_RECENT_FILE_FRAGMENT);
    }

    private void switchFragment(int fragmentCode) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        switch (fragmentCode) {
            case CODE_RECENT_FILE_FRAGMENT:
                mRecentFileButton.setImageResource(R.drawable.ic_recent_file_selected);
                mBrowseFileButton.setImageResource(R.drawable.ic_browse_file);
                mDropboxButton.setImageResource(R.drawable.ic_dropbox);
                mPDFToolButton.setImageResource(R.drawable.ic_pdf_tool);
                fragmentTransaction.replace(R.id.layout_fragment_container, mDocRecentFragment);
                break;
            case CODE_BROWSE_FILE_FRAGMENT:
                mRecentFileButton.setImageResource(R.drawable.ic_recent_file);
                mBrowseFileButton.setImageResource(R.drawable.ic_browse_file_selected);
                mDropboxButton.setImageResource(R.drawable.ic_dropbox);
                mPDFToolButton.setImageResource(R.drawable.ic_pdf_tool);
                fragmentTransaction.replace(R.id.layout_fragment_container, mDocNavFragment);
                break;
            case CODE_DROPBOX_FRAGMENT:
                mRecentFileButton.setImageResource(R.drawable.ic_recent_file);
                mBrowseFileButton.setImageResource(R.drawable.ic_browse_file);
                mDropboxButton.setImageResource(R.drawable.ic_dropbox_selected);
                mPDFToolButton.setImageResource(R.drawable.ic_pdf_tool);
                mDropboxFragment.init();
                fragmentTransaction.replace(R.id.layout_fragment_container, mDropboxFragment);
                break;
            case CODE_PDF_TOOL_FRAGMENT:
                mRecentFileButton.setImageResource(R.drawable.ic_recent_file);
                mBrowseFileButton.setImageResource(R.drawable.ic_browse_file);
                mDropboxButton.setImageResource(R.drawable.ic_dropbox);
                mPDFToolButton.setImageResource(R.drawable.ic_pdf_tool_selected);
                fragmentTransaction.replace(R.id.layout_fragment_container, mPDFToolFragment);
                break;
            default:
                return;
        }
        fragmentTransaction.commit();
    }

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
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
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
        if (item.getItemId() == R.id.action_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        Global.RemoveTmp();
        super.onDestroy();
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_recent_file:
                switchFragment(CODE_RECENT_FILE_FRAGMENT);
                break;
            case R.id.btn_brows_file:
                //if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
                //{
                //Android 10 or higher, select file by platform
                //    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                //    intent.addCategory(Intent.CATEGORY_OPENABLE);
                //    intent.setType("*/*");
                //    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                //            "application/pdf", // .pdf
                //            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx
                //    });
                //    startActivityForResult(intent, OPEN_FILE_REQUEST_CODE);
                //} else
                switchFragment(CODE_BROWSE_FILE_FRAGMENT);
                break;
            case R.id.btn_dropbox:
                switchFragment(CODE_DROPBOX_FRAGMENT);
                break;
            case R.id.btn_pdf_tool:
                switchFragment(CODE_PDF_TOOL_FRAGMENT);
                break;
            default:
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_FILE_REQUEST_CODE) {
            if (data != null) {
                String path = this.getContentResolver().getType(data.getData());
                //String filePath = UriContentUtility.writeStreamToLocal(data, this);
                if (path.toLowerCase().equals("application/pdf")) {
                    //Document document = new Document();
                    Intent intent = new Intent(this, PDFReaderActivity.class);
                    intent.setData(data.getData());
                    startActivity(intent);
                } else if (path.toLowerCase().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                    Intent intent = new Intent(this, DocxReaderActivity.class);
                    intent.setData(data.getData());
                    startActivity(intent);
                }
            }
        } else if (requestCode == 1000) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_dialog_title)
                        .setMessage(R.string.permission_denied_content)
                        .setPositiveButton(R.string.button_ok_label, ((dialog, which) -> dialog.dismiss()))
                        .show();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        /*for (int index = 0; index < permissions.length; index++) {
            if (permissions[index].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED)
                    m_vFiles.PDFGotoSubdir(".");
            }
        }*/
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}