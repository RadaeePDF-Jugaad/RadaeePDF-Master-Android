package com.radaee.utility;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.radaee.pdf.Document;
import com.radaee.pdfmaster.R;
import com.radaee.util.FileBrowserAdt;
import com.radaee.util.FileBrowserView;

import java.io.File;
import java.util.Random;

public class PDFUtilities {
    public interface OnOperationListener {
        void onDone(Object result, int requestCode);

        void onError(String error, int requestCode);
    }

    public static final int REQUEST_CODE_MERGE_PDF = 0;
    public static final int REQUEST_CODE_CONVERT_PDF = 1;
    public static final int REQUEST_CODE_ENCRYPT_PDF = 2;
    public static final int REQUEST_CODE_DECRYPT_PDF = 3;
    public static final int REQUEST_EXPORT_TO_HTML = 4;
    public static final int REQUEST_CODE_COMPRESS_PDF = 5;
    public static final int REQUEST_CODE_CONVERT_PDFA = 6;

    public static void MergePDF(Document destDoc, Document sourceDoc, OnOperationListener listener) {
        if (destDoc == null || sourceDoc == null) {
            listener.onError(null, REQUEST_CODE_MERGE_PDF);
        }

        Document.ImportContext context = destDoc.ImportStart(sourceDoc);
        int pageCount = sourceDoc.GetPageCount();
        int startIndex = destDoc.GetPageCount();
        for (int index = 0; index < pageCount; index++) {
            context.ImportPage(index, startIndex);
            startIndex++;
        }
        context.Destroy();
        sourceDoc.Close();
        if (destDoc.CanSave())
            destDoc.Save();
        listener.onDone(destDoc, REQUEST_CODE_MERGE_PDF);
    }

    public static void ExportToHTML(Document document, String path, OnOperationListener listener) {
        Document.HtmlExportor exportor = document.NewHtmlExportor(path);
        exportor.ExportEnd(false);
        listener.onDone(null, REQUEST_EXPORT_TO_HTML);
    }

    public static void ConvertDocxToPDF(Context context, OnOperationListener listener) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setView(LayoutInflater.from(context).inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder1.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.docx");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".docx"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    com.radaee.pdf.Document pdfDoc = new com.radaee.pdf.Document();
                    com.radaee.docx.Document docxDoc = new com.radaee.docx.Document();
                    String fullPath = item.m_item.get_path();
                    int ret = docxDoc.Open(fullPath, "");
                    if (ret == 0) {
                        String path = fullPath.substring(0, fullPath.lastIndexOf("/"));
                        String name = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("."));
                        name = name + ".pdf";
                        path = path + File.separatorChar + name;
                        pdfDoc.Create(path);
                        boolean result = docxDoc.ExportPDF(pdfDoc);
                        if (result) {
                            pdfDoc.Save();
                            listener.onDone(pdfDoc, REQUEST_CODE_CONVERT_PDF);
                        } else {
                            listener.onError(context.getString(R.string.text_convert_pdf_error_hint), REQUEST_CODE_CONVERT_PDF);
                        }
                    } else if (ret == -1) {

                    }
                    dlg.dismiss();
                }
            });
        });
        dlg.show();
    }

    public static void EncryptPDF(String path, String password, OnOperationListener listener, Document document) {
        byte[] id = new byte[32];
        new Random(0).nextBytes(id);
        boolean ret = document.EncryptAs(path, password, password, 0x10, 3, id);
        document.Close();
        document = null;
        if (ret)
            listener.onDone(path, REQUEST_CODE_ENCRYPT_PDF);
        else
            listener.onError("", REQUEST_CODE_ENCRYPT_PDF);
    }

    public static void DecryptPDF(String path, OnOperationListener listener, Document document) {
        Document destDoc = new Document();
        destDoc.Create(path);
        if (document == null) {
            listener.onError(null, REQUEST_CODE_DECRYPT_PDF);
        }

        Document.ImportContext context = destDoc.ImportStart(document);
        int pageCount = document.GetPageCount();
        int startIndex = destDoc.GetPageCount();
        for (int index = 0; index < pageCount; index++) {
            context.ImportPage(index, startIndex);
            startIndex++;
        }
        context.Destroy();
        if (destDoc.CanSave())
            destDoc.Save();
        document.Close();
        document = null;
        destDoc.Close();
        destDoc = null;
        listener.onDone(path, REQUEST_CODE_DECRYPT_PDF);
        //MergePDF(destDoc, document, listener);
    }

    public static void CompressPDF(String path, OnOperationListener listener, Document document) {
        boolean ret = document.SaveAs(path, new byte[]{1, 1, 0, 2, 1, 0, 1, 0, 1, 0}, 144);
        document.Close();
        document = null;
        if (ret)
            listener.onDone(path, REQUEST_CODE_COMPRESS_PDF);
        else
            listener.onError("", REQUEST_CODE_COMPRESS_PDF);
    }

    public static void ConvertPDFA(String path, OnOperationListener listener, Document document) {
        boolean ret = document.SaveAs(path, new byte[]{1,1,0,2,1,0,1,0,1,1}, 144);
        document.Close();
        document = null;
        if (ret)
            listener.onDone(path, REQUEST_CODE_CONVERT_PDFA);
        else
            listener.onError("", REQUEST_CODE_CONVERT_PDFA);
    }
}
