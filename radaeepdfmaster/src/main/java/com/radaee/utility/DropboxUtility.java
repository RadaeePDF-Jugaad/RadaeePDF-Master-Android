package com.radaee.utility;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.radaee.pdfmaster.R;
import com.radaee.tasks.DownloadDropboxFileTask;
import com.radaee.tasks.UploadDropboxFileTask;

import java.io.File;
import java.text.DateFormat;
import java.util.Arrays;

public class DropboxUtility {
    private static DbxRequestConfig mDbxRequestConfig;
    private static final String app_key = "rxnyl3xwkincvw5";
    private static String AccessToken;

    public static void setAccessToken(String accessToken) {
        AccessToken = accessToken;
    }

    public static void downloadFile(FileMetadata file, Context context, DropBoxCallback callback) {
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage(context.getString(R.string.text_downloading_hint));
        dialog.show();

        new DownloadDropboxFileTask(context, getClient(AccessToken), new DownloadDropboxFileTask.Callback() {
            @Override
            public void onDownloadComplete(File result) {
                dialog.dismiss();

                if (result != null) {
                    callback.OnDownloadDone(result);
                }
            }

            @Override
            public void onError(Exception e) {
                dialog.dismiss();
                callback.OnDownloadError(e);
                Log.e("tagd", "Failed to download file.", e);
            }
        }).execute(file);

    }

    public static void uploadFile(String fileUri, Context context, String path, DropBoxCallback callback) {
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage("Uploading");
        dialog.show();

        new UploadDropboxFileTask(context, getClient(AccessToken), new UploadDropboxFileTask.Callback() {
            @Override
            public void onUploadComplete(FileMetadata result) {
                dialog.dismiss();

                String message = result.getName() + " size " + result.getSize() + " modified " +
                        DateFormat.getDateTimeInstance().format(result.getClientModified());
                Toast.makeText(context, message, Toast.LENGTH_SHORT)
                        .show();

                // Reload the folder
                callback.OnUploadDone();
            }

            @Override
            public void onError(Exception e) {
                dialog.dismiss();
                callback.OnUploadError(e);
            }
        }).execute(fileUri, path);
    }

    public static void StartAuth(Context context) {
        Auth.startOAuth2PKCE(context, app_key, getRequestConfig(), Arrays.asList("account_info.read", "files.content.write", "files.content.read"));
    }

    public static DbxClientV2 getClient(String accessToken) {
        if (mDbxRequestConfig == null) {
            mDbxRequestConfig = getRequestConfig();
        }
        return new DbxClientV2(mDbxRequestConfig, accessToken);
    }

    private static DbxRequestConfig getRequestConfig() {
        mDbxRequestConfig = DbxRequestConfig.newBuilder("Radaee PDF Master")
                .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                .build();
        return mDbxRequestConfig;
    }

    public interface DropBoxCallback {
        void OnDownloadDone(File result);

        void OnDownloadError(Exception e);

        void OnUploadDone();

        void OnUploadError(Exception e);
    }
}
