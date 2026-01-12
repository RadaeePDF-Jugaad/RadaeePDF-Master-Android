package com.radaee.activities.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.radaee.interfaces.IOpenTaskHandler;
import com.radaee.pdfmaster.R;
import com.radaee.tasks.OpenTask;
import com.radaee.util.PDFGridItem;
import com.radaee.util.PDFGridView;

public class DocNavFragment extends Fragment implements AdapterView.OnItemClickListener, IOpenTaskHandler {

    public DocNavFragment(String engine) {
        m_engine = engine;
    }

    private PDFGridView m_grid;
    private TextView m_path;
    private String m_engine;
    private boolean m_pending = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout m_layout = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.pdf_nav, null);
        m_grid = m_layout.findViewById(R.id.pdf_nav);
        m_path = m_layout.findViewById(R.id.txt_path);
        m_grid.setViewMode(PDFGridView.MODE_BROWSE);
        m_grid.PDFSetRootPath(Environment.getExternalStorageDirectory().getPath());
        m_path.setText(m_grid.getPath());
        m_grid.setOnItemClickListener(this);
        return m_layout;
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (m_pending) return;
        PDFGridItem item = (PDFGridItem) view;
        if (item.is_dir()) {
            m_grid.PDFGotoSubdir(item.get_name());
            m_path.setText(m_grid.getPath());
        } else {
            OpenTask task = new OpenTask(getContext(), this, item, null);
            task.execute();
        }
    }

    @Override
    public void OnTaskBegin() {
        m_pending = true;
    }

    @Override
    public void OnTaskFinished() {
        m_pending = false;
    }
}
