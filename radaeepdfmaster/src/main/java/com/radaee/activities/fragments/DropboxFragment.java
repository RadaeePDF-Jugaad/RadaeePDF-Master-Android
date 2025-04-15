package com.radaee.activities.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxHost;
import com.dropbox.core.DbxPKCEManager;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxRequestUtil;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.auth.AuthError;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.radaee.adapters.DropboxFilesAdapter;
import com.radaee.interfaces.IOpenTaskHandler;
import com.radaee.pdfmaster.R;
import com.radaee.tasks.ListDropboxFolderTask;
import com.radaee.tasks.OpenTask;
import com.radaee.utility.DropboxUtility;
import com.radaee.utility.PicassoClient;
import com.radaee.utility.SharedPreferenceUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class DropboxFragment extends Fragment implements IOpenTaskHandler {//, DropboxUtility.DropBoxCallback {
    private boolean mIsInited = false;
    public static String mPath = "";
    private final Context mContext;
    private boolean m_pending = false;

    private DropboxFilesAdapter mDropboxAdapter;
    private RecyclerView mRecyclerView;
    private TextView mPathView;
    private FileMetadata mSelectedFile;

    //private final String mAuthorizationURL = "https://www.dropbox.com/oauth2/authorize?client_id=MY_CLIENT_ID&redirect_uri=MY_REDIRECT_URI&response_type=code";

    public DropboxFragment(Context context) {
        super();
        mContext = context;
    }

    private static final int START_OAUTH = 0;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        final String mAppKey = "rxnyl3xwkincvw5";
        private final DbxPKCEManager mPKCEManager = new DbxPKCEManager();
        private final String mScope = "account_info.read files.content.write files.content.read";

        private String createPKCEStateNonce() {
            String state = String.format(Locale.US, "oauth2code:%s:%s:%s",
                    mPKCEManager.getCodeChallenge(),
                    DbxPKCEManager.CODE_CHALLENGE_METHODS,
                    "offline"
            );
            state += ":" + mScope;
            return state;
        }

        private String createExtraQueryParams() {
            String param = String.format(Locale.US,
                    "%s=%s&%s=%s&%s=%s&%s=%s",
                    "code_challenge", mPKCEManager.getCodeChallenge(),
                    "code_challenge_method", DbxPKCEManager.CODE_CHALLENGE_METHODS,
                    "token_access_type", "offline",
                    "response_type", "code"
            );
            param += String.format(Locale.US, "&%s=%s", "scope", mScope);
            return param;
        }

        class TokenRequestAsyncTask extends AsyncTask<Void, Void, DbxAuthFinish> {
            private final String code;

            private TokenRequestAsyncTask(String code) {
                this.code = code;
            }

            @Override
            protected DbxAuthFinish doInBackground(Void... p) {
                try {
                    DbxRequestConfig rcfg = DbxRequestConfig.newBuilder("Radaee PDF Master")
                            .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                            .build();
                    return mPKCEManager.makeTokenRequest(rcfg, code, mAppKey, null, DbxHost.DEFAULT);
                } catch (DbxException e) {
                    return null;
                }
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        public void handleMessage(Message msg) {
            if (msg.what == START_OAUTH) {
                // following codes extracted from com.dropbox.core.android.AuthActivity class.
                // the auth process mainly is: spell URL from key and other parameters, then pass url to Intent.ACTION_VIEW
                // i believe it can be replaced on webview after url spelled.
                // when using Intent.ACTION_VIEW, it listen onResume callback function of this activity
                // when using Dialog, you shall listen dismiss callback of Dialog.

                String mApiType = "1";
                //"oauth2code:VPHmcFMBKwB58pSetvAXCDhJYtz8tnKQPhHbuLKJN7w:S256:offline:account_info.read files.content.write files.content.read";
                String state = createPKCEStateNonce();
                String path = "1/connect";
                Locale locale = Locale.getDefault();
                locale = new Locale(locale.getLanguage(), locale.getCountry());
                String alreadyAuthedUid = "0";
                List<String> params = new ArrayList<>(Arrays.asList(
                        "k", mAppKey,
                        "n", alreadyAuthedUid,
                        "api", mApiType,
                        "state", state
                ));
                params.add("extra_query_params");
                params.add(createExtraQueryParams());
                String url = DbxRequestUtil.buildUrlWithParams(locale.toString(), "www.dropbox.com", path, params.toArray(new String[0]));
                //mLoginWeb.destroy();
                mLoginWeb.setVisibility(View.VISIBLE);
                WebSettings ws = mLoginWeb.getSettings();
                ws.setDomStorageEnabled(true);
                ws.setDatabaseEnabled(true);
                ws.setJavaScriptEnabled(true);
                mLoginWeb.setWebChromeClient(new WebChromeClient());
                mLoginWeb.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                        super.onReceivedError(view, request, error);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        mLoadingProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (url.contains("/connect"))//success
                        {
                            mLoginWeb.setVisibility(View.GONE);
                            mLoginWeb.destroy();

                            //todo: parser token and invoke loadData(accessToken);
                            Uri uri = Uri.parse(url);
                            String secret = uri.getQueryParameter("oauth_token_secret");

                            TokenRequestAsyncTask tokenRequest = new TokenRequestAsyncTask(secret);
                            try {
                                DbxAuthFinish dbxAuthFinish = tokenRequest.execute().get();
                                String accessToken = dbxAuthFinish.getAccessToken();
                                if (accessToken != null && accessToken.length() > 0) {
                                    SharedPreferences preferences = mContext.getSharedPreferences(SharedPreferenceUtility.PREFERENCE_NAME, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString(SharedPreferenceUtility.DROPBOX_TOKEN_KEY, accessToken);
                                    editor.apply();
                                    loadData(accessToken);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else if (url.contains("/cancel"))//failed
                        {
                            mLoginWeb.setVisibility(View.GONE);
                            mLoginWeb.destroy();
                        } else {
                            mLoadingProgressBar.setVisibility(View.VISIBLE);
                            view.loadUrl(url);
                        }
                        return true;
                    }
                });
                mLoadingProgressBar.setVisibility(View.VISIBLE);
                mLoginWeb.loadUrl(url);
            }
        }
    };
    private WebView mLoginWeb;
    private ProgressBar mLoadingProgressBar;

    public void init() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RelativeLayout m_layout = (RelativeLayout) LayoutInflater.from(getContext()).inflate(R.layout.dropbox_files, null);
        mLoginWeb = m_layout.findViewById(R.id.login_web);
        mLoadingProgressBar = m_layout.findViewById(R.id.loading_progress_bar);
        mRecyclerView = m_layout.findViewById(R.id.files_list);
        mPathView = m_layout.findViewById(R.id.txt_path);
        mPathView.setText(getString(R.string.text_dropbox_path_label, ""));

        if (!mIsInited) {
            SharedPreferences preferences = mContext.getSharedPreferences(SharedPreferenceUtility.PREFERENCE_NAME, Context.MODE_PRIVATE);
            String accessToken = preferences.getString(SharedPreferenceUtility.DROPBOX_TOKEN_KEY, null);
            if (accessToken == null) {
                mHandler.sendEmptyMessage(START_OAUTH);
            } else {
                loadData(accessToken);
            }
        }

        return m_layout;
    }

    protected void loadData(String accessToken) {
        final ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage(mContext.getString(R.string.text_loading_label));
        dialog.show();

        DbxClientV2 sDbxClient = DropboxUtility.getClient(accessToken);
        new ListDropboxFolderTask(sDbxClient, new ListDropboxFolderTask.Callback() {
            @Override
            public void onDataLoaded(ListFolderResult result) {
                //init picaso client
                mIsInited = true;
                DropboxUtility.setAccessToken(accessToken);
                DbxClientV2 sDbxClient = DropboxUtility.getClient(accessToken);
                PicassoClient.init(mContext, sDbxClient);
                mDropboxAdapter = new DropboxFilesAdapter(PicassoClient.getPicasso(), new DropboxFilesAdapter.Callback() {
                    @Override
                    public void onFolderClicked(FolderMetadata folder) {
                        mPath = folder.getPathLower();
                        mPathView.setText(getString(R.string.text_dropbox_path_label, mPath));
                        loadData(accessToken);
                    }

                    @Override
                    public void onFileClicked(final FileMetadata file) {
                        mSelectedFile = file;
                        //todo: display downloading progress dialog
                        performWithPermissions(FileAction.DOWNLOAD);
                    }
                });
                mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
                mRecyclerView.setAdapter(mDropboxAdapter);
                List<Metadata> results = Collections.unmodifiableList(new ArrayList<>(result.getEntries()));
                List<Metadata> files = new ArrayList<>();
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                if (!mPath.equals("")) {
                    FolderMetadata goUpData = new FolderMetadata("..", "..", mPath.substring(0, mPath.lastIndexOf("/")), "..", null, null, null, null);
                    files.add(goUpData);
                }
                FolderMetadata refreshData = new FolderMetadata(getString(R.string.text_refresh_label), "..", mPath, "refresh", null, null, null, null);
                files.add(refreshData);
                for (Metadata data : results) {
                    if (data instanceof FileMetadata) {
                        String ext = data.getName().substring(data.getName().indexOf(".") + 1);
                        String type = mime.getMimeTypeFromExtension(ext);
                        if (type != null && (type.equals("application/pdf") || type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
                            files.add(data);
                        }
                    } else if (data instanceof FolderMetadata) {
                        files.add(data);
                    }
                }
                mDropboxAdapter.setFiles(files);
                dialog.dismiss();
                //mFilesAdapter.setFiles(result.getEntries());
            }

            @Override
            public void onError(Exception e) {
                dialog.dismiss();
                if (e.getClass().equals(InvalidAccessTokenException.class)) {
                    AuthError error = ((InvalidAccessTokenException) e).getAuthError();
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setPositiveButton(mContext.getString(R.string.button_ok_label), (dialog1, which) -> {
                        mHandler.sendEmptyMessage(START_OAUTH);
                        dialog1.dismiss();
                    });
                    if (error.isExpiredAccessToken() || error.isInvalidAccessToken())
                        builder.setMessage(R.string.text_token_expired_hint);
                    else
                        builder.setMessage(R.string.text_cannot_get_token_hint);
                    builder.create().show();
                }
            }
        }).execute(mPath);
    }


    private void openDocument(File result) {
        if (m_pending)
            return;
        OpenTask task = new OpenTask(getContext(), this, result, null);
        task.execute();
    }

    private void performWithPermissions(final FileAction action) {
        if (hasPermissionsForAction(action)) {
            performAction(action);
            return;
        }

        if (shouldDisplayRationaleForAction(action)) {
            new androidx.appcompat.app.AlertDialog.Builder(mContext)
                    .setMessage(mContext.getString(R.string.text_storage_permission_label))
                    .setPositiveButton(R.string.button_ok_label, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissionsForAction(action);
                        }
                    })
                    .setNegativeButton(R.string.button_cancel_label, null)
                    .create()
                    .show();
        } else {
            requestPermissionsForAction(action);
        }
    }

    private void requestPermissionsForAction(FileAction action) {
        ActivityCompat.requestPermissions(
                requireActivity(),
                action.getPermissions(),
                action.getCode()
        );
    }

    private boolean shouldDisplayRationaleForAction(FileAction action) {
        for (String permission : action.getPermissions()) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {
                return true;
            }
        }
        return false;
    }

    private void performAction(FileAction action) {
        if (action == FileAction.DOWNLOAD) {
            if (mSelectedFile != null) {
                DropboxUtility.downloadFile(mSelectedFile, mContext, new DropboxUtility.DropBoxCallback() {
                    @Override
                    public void OnDownloadDone(File result) {
                        //todo: dismiss downloading dialog
                        openDocument(result);
                    }

                    @Override
                    public void OnDownloadError(Exception e) {
                        //todo: dismiss downloading dialog
                        Toast.makeText(mContext,
                                R.string.text_download_error_label,
                                Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void OnUploadDone() {
                    }

                    @Override
                    public void OnUploadError(Exception e) {
                    }
                });
            } else {
                Log.e("tagd", "No file selected to download.");
            }
        } else {
            Log.e("tagd", "Can't perform unhandled file action: " + action);
        }
    }

    private boolean hasPermissionsForAction(FileAction action) {
        for (String permission : action.getPermissions()) {
            int result = ContextCompat.checkSelfPermission(mContext, permission);
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void OnTaskBegin() {
        m_pending = true;
    }

    @Override
    public void OnTaskFinished() {
        m_pending = false;
    }

    /*@Override
    public void OnDownloadDone(File result) {
        openDocument(result);
    }

    @Override
    public void OnDownloadError(Exception e) {
        Toast.makeText(mContext,
                R.string.text_download_error_label,
                Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void OnUploadDone() {

    }

    @Override
    public void OnUploadError(Exception e) {

    }*/

    private enum FileAction {
        DOWNLOAD(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        UPLOAD(Manifest.permission.READ_EXTERNAL_STORAGE);

        private static final FileAction[] values = values();

        private final String[] permissions;

        FileAction(String... permissions) {
            this.permissions = permissions;
        }

        public int getCode() {
            return ordinal();
        }

        public String[] getPermissions() {
            return permissions;
        }

        public static FileAction fromCode(int code) {
            if (code < 0 || code >= values.length) {
                throw new IllegalArgumentException("Invalid FileAction code: " + code);
            }
            return values[code];
        }
    }
}
