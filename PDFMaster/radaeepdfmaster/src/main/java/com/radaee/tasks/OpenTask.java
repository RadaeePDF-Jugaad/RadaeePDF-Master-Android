package com.radaee.tasks;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.radaee.activities.DocxReaderActivity;
import com.radaee.activities.OFDReaderActivity;
import com.radaee.activities.PDFReaderActivity;
import com.radaee.interfaces.IOpenTaskHandler;
import com.radaee.modules.fts.FTSGenerationTask;
import com.radaee.util.PDFGridItem;
import com.radaee.util.RDRecentFiles;

import java.io.File;


public class OpenTask extends AsyncTask<Void, Integer, Integer> {
    private ProgressDialog dlg;
    private Handler handler;
    private Runnable runable;
    private PDFGridItem item;
    private File file;
    private String pswd;
    private int ret;
    com.radaee.pdf.Document m_pdf;
    com.radaee.docx.Document m_docx;
    com.radaee.ofd.Package m_pkg;
    private Context mContext;
    IOpenTaskHandler mHandler;

    public OpenTask(Context context, IOpenTaskHandler handler, PDFGridItem item, String pswd) {
        this.item = item;
        this.pswd = pswd;
        mContext = context;
        mHandler = handler;
    }

    public OpenTask(Context context, IOpenTaskHandler handler, File file, String pswd) {
        this.file = file;
        this.pswd = pswd;
        mContext = context;
        mHandler = handler;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        m_pdf = new com.radaee.pdf.Document();
        int err = -256;
        if (item != null) {
            err = item.open_pdf(m_pdf, pswd);
            if (err != 0) {
                m_pdf.Close();
                m_pdf = null;
            }
            if (err == -3 || err == -10) {
                m_docx = new com.radaee.docx.Document();
                err = item.open_docx(m_docx, pswd);
                if (err != 0) {
                    m_docx.Close();
                    m_docx = null;
                }
                if (err == -3 || err == -10) {
                    m_pkg = new com.radaee.ofd.Package();
                    err = item.open_ofd(m_pkg, pswd);
                    if (err != 0) {
                        m_pkg.Close();
                        m_pkg = null;
                    }
                }
            }
        } else if (file != null) {
            err = m_pdf.Open(file.getPath(), pswd);
            if (err != 0) {
                m_pdf.Close();
                m_pdf = null;
            }
            if (err == -3 || err == -10) {
                m_docx = new com.radaee.docx.Document();
                err = m_docx.Open(file.getPath(), pswd);
                if (err != 0) {
                    m_docx.Close();
                    m_docx = null;
                }
                if (err == -3 || err == -10) {
                    m_pkg = new com.radaee.ofd.Package();
                    err = m_pkg.Open(file.getPath(), pswd);
                    if (err != 0) {
                        m_pkg.Close();
                        m_pkg = null;
                    }
                }
            }
        }

        ret = err;
        return null;
    }

    @Override
    protected void onPreExecute() {
        mHandler.OnTaskBegin();
        handler = new Handler();
        runable = new Runnable() {
            public void run() {
                dlg = ProgressDialog.show(mContext, mContext.getString(com.radaee.viewlib.R.string.please_wait), mContext.getString(com.radaee.viewlib.R.string.thumbnail_creation_running), true);
            }
        };
        handler.postDelayed(runable, 1000);//delay 1 second to display progress dialog.

    }

    @Override
    protected void onPostExecute(Integer integer) {
        mHandler.OnTaskFinished();
        switch (ret) {
            case -1://need input password
                InputPswd(item);
                break;
            case -2://unknown encryption
                onFail(mContext.getString(com.radaee.viewlib.R.string.failed_encryption));
                break;
            case -3://damaged or invalid format
                onFail(mContext.getString(com.radaee.viewlib.R.string.failed_invalid_format));
                break;
            case -10://access denied or invalid file path
                onFail(mContext.getString(com.radaee.viewlib.R.string.failed_invalid_path));
                break;
            case 0://succeeded, and continue
                InitView(m_pdf, m_docx, m_pkg);
                break;
            default://unknown error
                onFail(mContext.getString(com.radaee.viewlib.R.string.failed_unknown));
                break;
        }
        if (dlg != null)
            dlg.dismiss();
        else
            handler.removeCallbacks(runable);
    }

    private void InitView(com.radaee.pdf.Document pdf, com.radaee.docx.Document docx, com.radaee.ofd.Package pkg)//process to view PDF file
    {
        boolean ret = false;
        if (item != null)
            ret = RDRecentFiles.insertDocument(mContext, item.get_name(), item.get_path());
        else if (file != null)
            ret = RDRecentFiles.insertDocument(mContext, file.getName(), file.getPath());
        if (ret) {
            //to do: resort/relayout recent file list on UI.
        }
        if (pdf != null) {
            //start a task for FTS generation
            String path = item == null ? pdf.getDocPath() : item.get_path();
            //new FTSGenerationTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mContext, item.get_path(), pswd);
            new FTSGenerationTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mContext, path, pswd);

            PDFReaderActivity.ms_tran_doc = pdf;
            Intent intent = new Intent(mContext, PDFReaderActivity.class);
            if (item != null) {
                intent.putExtra("data_source", "app");
            } else if (file != null) {
                intent.putExtra("data_source", "dropbox");
            }
            mContext.startActivity(intent);
        }
        else if (docx != null){
            DocxReaderActivity.ms_tran_doc = docx;
            Intent intent = new Intent(mContext, DocxReaderActivity.class);
            String path = null;
            if (item != null) {
                path = item.get_path();
                intent.putExtra("data_source", "app");
            } else if (file != null) {
                path = file.getPath();
                intent.putExtra("data_source", "dropbox");
            }
            intent.putExtra("doc_path", path);
            mContext.startActivity(intent);
        }
        else
        {
            OFDReaderActivity.ms_tran_pkg = pkg;
            Intent intent = new Intent(mContext, OFDReaderActivity.class);
            String path = null;
            if (item != null) {
                path = item.get_path();
                intent.putExtra("data_source", "app");
            } else if (file != null) {
                path = file.getPath();
                intent.putExtra("data_source", "dropbox");
            }
            intent.putExtra("doc_path", path);
            mContext.startActivity(intent);
        }
    }

    private void onFail(String msg)//treat open failed.
    {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    private void InputPswd(PDFGridItem item)//treat password
    {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(mContext).inflate(com.radaee.viewlib.R.layout.dlg_pswd, null);
        final EditText tpassword = (EditText) layout.findViewById(com.radaee.viewlib.R.id.txt_password);
        final PDFGridItem gitem = item;

        final CheckBox showPasswordCB = layout.findViewById(com.radaee.viewlib.R.id.chk_show);
        showPasswordCB.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tpassword.setTransformationMethod(null);
            } else {
                tpassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String password = tpassword.getText().toString();
                OpenTask task = new OpenTask(mContext, mHandler, gitem, password);
                task.execute();
            }
        });
        builder.setNegativeButton(com.radaee.viewlib.R.string.text_cancel_label, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setTitle(com.radaee.viewlib.R.string.input_password);
        builder.setCancelable(false);
        builder.setView(layout);

        AlertDialog dlg = builder.create();
        dlg.show();
    }
}
