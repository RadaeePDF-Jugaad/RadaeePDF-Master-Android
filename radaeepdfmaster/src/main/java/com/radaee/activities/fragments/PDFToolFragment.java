package com.radaee.activities.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.radaee.activities.PDFReaderActivity;
import com.radaee.pdf.Document;
import com.radaee.pdfmaster.R;
import com.radaee.util.FileBrowserAdt;
import com.radaee.util.FileBrowserView;
import com.radaee.utility.PDFUtilities;

import java.io.File;

public class PDFToolFragment extends Fragment implements PDFUtilities.OnOperationListener {//, View.OnClickListener {
    private Document mDestDoc = null;
    private Document mSrcDoc = null;
    private ProgressDialog mProgressDialog;
    private final int SHOW_PROGRESS_DIALOG = 0;
    private final int DISMISS_PROGRESS_DIALOG = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = LayoutInflater.from(getContext()).inflate(R.layout.fragment_pdf_tool, null);
        LinearLayout mergePDFLayout = layout.findViewById(R.id.layout_merge_pdf);
        mergePDFLayout.setOnClickListener(mOnMergePDFClickListener);

        LinearLayout convertPDFLayout = layout.findViewById(R.id.layout_convert_pdf);
        convertPDFLayout.setOnClickListener(mConvertPDFClickListener);

        LinearLayout encryptPDFLayout = layout.findViewById(R.id.layout_encrypt_pdf);
        encryptPDFLayout.setOnClickListener(mEncryptPDFClickListener);

        LinearLayout decryptPDFLayout = layout.findViewById(R.id.layout_decrypt_pdf);
        decryptPDFLayout.setOnClickListener(mDecryptPDFClickListener);

        LinearLayout compressPDFLayout = layout.findViewById(R.id.layout_compress_pdf);
        compressPDFLayout.setOnClickListener(mCompressPDFClickListener);

        LinearLayout convertPDFALayout = layout.findViewById(R.id.layout_convert_pdfa);
        convertPDFALayout.setOnClickListener(mConvertPDFAClickListener);

        return layout;
    }

    @Override
    public void onDone(Object result, int requestCode) {
        mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);
        getActivity().runOnUiThread(() -> {
            if (requestCode == PDFUtilities.REQUEST_CODE_MERGE_PDF || requestCode == PDFUtilities.REQUEST_CODE_CONVERT_PDF) {
                PDFReaderActivity.ms_tran_doc = (Document) result;
                Intent intent = new Intent(getActivity(), PDFReaderActivity.class);
                intent.putExtra("data_source", "app");
                startActivity(intent);
            } else if (requestCode == PDFUtilities.REQUEST_CODE_ENCRYPT_PDF) {
                String path = (String) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.encrypt_success_hint, path));
                builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
                builder.create().show();
            } else if (requestCode == PDFUtilities.REQUEST_CODE_DECRYPT_PDF) {
                String path = (String) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.decrypt_success_hint, path));
                builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
                builder.create().show();
            } else if (requestCode == PDFUtilities.REQUEST_CODE_COMPRESS_PDF) {
                String path = (String) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.compress_success_hint, path));
                builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
                builder.setCancelable(false);
                builder.create().show();
            } else if (requestCode == PDFUtilities.REQUEST_CODE_CONVERT_PDFA) {
                String path = (String) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.convert_success_hint, path));
                builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
                builder.create().show();
            }
        });
    }

    @Override
    public void onError(String error, int requestCode) {
        mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);

        getActivity().runOnUiThread(() -> {
            if (requestCode == PDFUtilities.REQUEST_CODE_ENCRYPT_PDF)
                Toast.makeText(getActivity(), R.string.encrypt_failed_hint, Toast.LENGTH_LONG).show();
            else if (requestCode == PDFUtilities.REQUEST_CODE_COMPRESS_PDF)
                Toast.makeText(getActivity(), R.string.compress_failed_hint, Toast.LENGTH_LONG).show();
            else if (requestCode == PDFUtilities.REQUEST_CODE_CONVERT_PDFA)
                Toast.makeText(getActivity(), R.string.convert_failed_hint, Toast.LENGTH_LONG).show();
        });
    }

    private Handler mHandler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS_DIALOG:
                    Context context = getActivity();
                    mProgressDialog = new ProgressDialog(context);
                    mProgressDialog.setMessage(context.getResources().getString(R.string.message_wait_label));
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();
                    break;
                case DISMISS_PROGRESS_DIALOG:
                    removeMessages(SHOW_PROGRESS_DIALOG);
                    if (mProgressDialog != null && mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
                    mProgressDialog = null;
                    break;
                default:
                    break;
            }
        }
    };

    private final View.OnClickListener mEncryptPDFClickListener = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.pdf");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    com.radaee.pdf.Document pdfDoc = new com.radaee.pdf.Document();
                    String fullPath = item.m_item.get_path();
                    int ret = pdfDoc.Open(fullPath, "");
                    if (ret == 0) {
                        InputPswd(fullPath, pdfDoc, null, PDFUtilities.REQUEST_CODE_ENCRYPT_PDF);
                    } else if (ret == -1) {
                        InputPswd(fullPath, pdfDoc, null, PDFUtilities.REQUEST_CODE_ENCRYPT_PDF);
                    }
                    dlg.dismiss();
                }
            });
        });
        dlg.show();
    };

    private final View.OnClickListener mDecryptPDFClickListener = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.pdf");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    com.radaee.pdf.Document pdfDoc = new com.radaee.pdf.Document();
                    String fullPath = item.m_item.get_path();
                    int ret = pdfDoc.Open(fullPath, "");
                    if (ret == 0) {
                        Toast.makeText(getActivity(), R.string.not_encrypted_pdf_hint, Toast.LENGTH_LONG).show();
                    } else if (ret == -1) {
                        InputPswd(fullPath, pdfDoc, null, PDFUtilities.REQUEST_CODE_DECRYPT_PDF);
                    }
                    dlg.dismiss();
                }
            });
        });
        dlg.show();
    };

    private final View.OnClickListener mCompressPDFClickListener = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.pdf");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    com.radaee.pdf.Document pdfDoc = new com.radaee.pdf.Document();
                    String fullPath = item.m_item.get_path();
                    int ret = pdfDoc.Open(fullPath, "");
                    if (ret == 0) {
                        String path = fullPath.substring(0, fullPath.lastIndexOf("/"));
                        String name = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("."));
                        name = name + "_compressed.pdf";
                        path = path + File.separatorChar + name;
                        mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
                        String finalPath = path;
                        Thread thread = new Thread(() -> PDFUtilities.CompressPDF(finalPath, PDFToolFragment.this, pdfDoc));
                        thread.start();
                    } else if (ret == -1) {
                        InputPswd(item.m_item.get_path(), mDestDoc, null, PDFUtilities.REQUEST_CODE_COMPRESS_PDF);
                    } else if (ret != 0) {
                        mDestDoc.Close();
                        mDestDoc = null;
                    }
                    dlg.dismiss();
                }
            });
        });
        dlg.show();
    };

    private final View.OnClickListener mConvertPDFAClickListener = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.pdf");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    com.radaee.pdf.Document pdfDoc = new com.radaee.pdf.Document();
                    String fullPath = item.m_item.get_path();
                    int ret = pdfDoc.Open(fullPath, "");
                    if (ret == 0) {
                        String path = fullPath.substring(0, fullPath.lastIndexOf("/"));
                        String name = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("."));
                        name = name + "_PDFA.pdf";
                        path = path + File.separatorChar + name;
                        mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
                        String finalPath = path;
                        Thread thread = new Thread(() -> PDFUtilities.ConvertPDFA(finalPath, this, pdfDoc));
                        thread.start();
                    } else if (ret == -1) {
                        InputPswd(item.m_item.get_path(), mDestDoc, null, PDFUtilities.REQUEST_CODE_COMPRESS_PDF);
                    } else if (ret != 0) {
                        mDestDoc.Close();
                        mDestDoc = null;
                    }
                    dlg.dismiss();
                }
            });
        });
        dlg.show();
    };

    private final View.OnClickListener mConvertPDFClickListener = v -> {
        PDFUtilities.ConvertDocxToPDF(getActivity(), this);
    };

    private final View.OnClickListener mOnMergePDFClickListener = v -> {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_merge_pdf, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        Button destFileBtn = view.findViewById(R.id.btn_dest_file);
        destFileBtn.setOnClickListener(button -> {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(view.getContext());
            builder1.setView(inflater.inflate(R.layout.dialog_pick_file, null));
            AlertDialog dlg = builder1.create();
            dlg.setOnShowListener(dialog -> {
                FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
                TextView txt_filter = dlg.findViewById(R.id.extension_filter);
                txt_filter.setText("*.pdf");
                fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
                fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                    FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                    if (item.m_item.is_dir())
                        fb_view.FileGotoSubdir(item.m_item.get_name());
                    else {
                        if (mDestDoc != null) {
                            mDestDoc.Close();
                            mDestDoc = null;
                        }
                        mDestDoc = new Document();
                        int ret = mDestDoc.Open(item.m_item.get_path(), "");
                        if (ret == 0)
                            ((Button) button).setText(item.m_path);
                        else if (ret == -1) {
                            InputPswd(item.m_item.get_path(), mDestDoc, (Button) button, PDFUtilities.REQUEST_CODE_MERGE_PDF);
                        } else if (ret != 0) {
                            mDestDoc.Close();
                            mDestDoc = null;
                        }
                        dlg.dismiss();
                    }
                });
            });
            dlg.show();
        });
        Button sourceFileBtn = view.findViewById(R.id.btn_source_file);
        sourceFileBtn.setOnClickListener(button -> {
            AlertDialog.Builder builder2 = new AlertDialog.Builder(view.getContext());
            builder2.setView(inflater.inflate(R.layout.dialog_pick_file, null));
            AlertDialog dlg = builder2.create();
            dlg.setOnShowListener(dialog -> {
                FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
                TextView txt_filter = dlg.findViewById(R.id.extension_filter);
                txt_filter.setText("*.pdf");
                fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
                fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                    FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                    if (item.m_item.is_dir())
                        fb_view.FileGotoSubdir(item.m_item.get_name());
                    else {
                        if (mSrcDoc != null) {
                            mSrcDoc.Close();
                            mSrcDoc = null;
                        }
                        mSrcDoc = new Document();
                        int ret = mSrcDoc.Open(item.m_item.get_path(), "");
                        if (ret == 0)
                            ((Button) button).setText(item.m_path);
                        else if (ret == -1) {
                            InputPswd(item.m_item.get_path(), mSrcDoc, ((Button) button), PDFUtilities.REQUEST_CODE_MERGE_PDF);
                        } else {
                            mSrcDoc.Close();
                            mSrcDoc = null;
                        }
                        dlg.dismiss();
                    }
                });
            });
            dlg.show();
        });
        builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> {
            if (mDestDoc == null || mSrcDoc == null)
                return;
            PDFUtilities.MergePDF(mDestDoc, mSrcDoc, this);
        });
        builder.setNegativeButton(R.string.button_cancel_label, (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    };

    private void InputPswd(String itemPath, Document document, Button button, int operationCode) {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String password = passwordInput.getText().toString();
            if (operationCode == PDFUtilities.REQUEST_CODE_ENCRYPT_PDF) {
                int ret = document.Open(itemPath, password);
                if (ret == -1)
                    InputPswd(itemPath, document, button, operationCode);
                else if (ret == 0) {
                    String path = itemPath.substring(0, itemPath.lastIndexOf("/"));
                    String name = itemPath.substring(itemPath.lastIndexOf("/") + 1, itemPath.lastIndexOf("."));
                    name = name + "_encrypted.pdf";
                    path = path + File.separatorChar + name;
                    PDFUtilities.EncryptPDF(path, password, this, document);
                }
            } else if (operationCode == PDFUtilities.REQUEST_CODE_DECRYPT_PDF) {
                int ret = document.Open(itemPath, password);
                if (ret == -1)
                    InputPswd(itemPath, document, button, operationCode);
                else if (ret == 0) {
                    if (button != null && operationCode == PDFUtilities.REQUEST_CODE_MERGE_PDF)
                        button.setText(itemPath);
                    else if (operationCode == PDFUtilities.REQUEST_CODE_DECRYPT_PDF) {
                        String path = itemPath.substring(0, itemPath.lastIndexOf("/"));
                        String name = itemPath.substring(itemPath.lastIndexOf("/") + 1, itemPath.lastIndexOf("."));
                        name = name + "_decrypted.pdf";
                        path = path + File.separatorChar + name;
                        PDFUtilities.DecryptPDF(path, this, document);
                    }
                }
            } else if (operationCode == PDFUtilities.REQUEST_CODE_COMPRESS_PDF) {
                int ret = document.Open(itemPath, password);
                if (ret == -1)
                    InputPswd(itemPath, document, button, operationCode);
                else if (ret == 0) {
                    String path = itemPath.substring(0, itemPath.lastIndexOf("/"));
                    String name = itemPath.substring(itemPath.lastIndexOf("/") + 1, itemPath.lastIndexOf("."));
                    name = name + "_compressed.pdf";
                    path = path + File.separatorChar + name;
                    mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
                    String finalPath = path;
                    Thread thread = new Thread(() -> PDFUtilities.CompressPDF(finalPath, PDFToolFragment.this, document));
                    thread.start();
                }
            } else if (operationCode == PDFUtilities.REQUEST_CODE_CONVERT_PDFA) {
                int ret = document.Open(itemPath, password);
                if (ret == -1)
                    InputPswd(itemPath, document, button, operationCode);
                else if (ret == 0) {
                    String path = itemPath.substring(0, itemPath.lastIndexOf("/"));
                    String name = itemPath.substring(itemPath.lastIndexOf("/") + 1, itemPath.lastIndexOf("."));
                    name = name + "_PDFA.pdf";
                    path = path + File.separatorChar + name;
                    mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
                    String finalPath = path;
                    Thread thread = new Thread(() -> PDFUtilities.ConvertPDFA(finalPath, this, document));
                    thread.start();
                }
            }
            dialog.dismiss();
            return;
        });
        builder.setNegativeButton(com.radaee.viewlib.R.string.text_cancel_label, (dialog, which) -> dialog.dismiss());
        builder.setTitle(com.radaee.viewlib.R.string.input_password);
        builder.setCancelable(false);
        builder.setView(layout);

        AlertDialog dlg = builder.create();
        dlg.show();
    }
}
