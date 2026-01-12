package com.radaee.utility;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class UriContentUtility {

    public static String writeStreamToLocal(Intent intent, Context context) {
        try {
            InputStream attachment = context.getContentResolver().openInputStream(Objects.requireNonNull(intent.getData()));
            File tempFolder = new File(context.getFilesDir().getPath() + "/temp/");
            if (!tempFolder.exists()) {
                tempFolder.mkdir();
            }
            byte[] buf = new byte[4096];
            File tempFile;
            String fileName;
            if (intent.getExtras() != null)
                fileName = intent.getExtras().getString("filename");
            else
                fileName = getRealPathFromURI(intent.getData(), context);
            if (fileName != null && fileName.length() > 0) {
                if (fileName.contains(File.separator))
                    fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
                String filePath = context.getFilesDir().getPath() + "/temp/" + File.separator + fileName;
                tempFile = new File(filePath);
            } else
                tempFile = new File(context.getFilesDir().getPath() + "/temp/temp.pdf");
            if (!tempFile.exists()) {
                tempFile.createNewFile();
            }
            FileOutputStream dst = new FileOutputStream(tempFile);
            int read;
            while ((read = attachment.read(buf)) > 0)
                dst.write(buf, 0, read);
            attachment.close();
            dst.close();
            return tempFile.getAbsolutePath();
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getRealPathFromURI(Uri contentUri, Context context) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            try {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                res = cursor.getString(column_index);
            } catch (IllegalArgumentException ignored) {
            }
        }
        cursor.close();
        return res;
    }
}
