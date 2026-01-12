package com.radaee.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.radaee.pdfmaster.R;

public class HelpActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView mHelpImage;
    private Button mNextBtn;

    private int[] mResources = new int[]{
            R.drawable.img_help_1,
            R.drawable.img_help_2,
            R.drawable.img_help_3,
            R.drawable.img_help_4,
            R.drawable.img_help_5,
            R.drawable.img_help_6,
            R.drawable.img_help_7
    };

    private int mPosition = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        mHelpImage = findViewById(R.id.img_help);

        mNextBtn = findViewById(R.id.btn_next);
        mNextBtn.setOnClickListener(this);

        init();
    }

    private void init() {
        mHelpImage.setImageResource(mResources[mPosition]);

    }

    private void moveToNextFocus() {
        mPosition++;
        int mCount = 6;
        if (mPosition == mCount) {
            mNextBtn.setText(R.string.text_done_label);
            mHelpImage.setImageResource(mResources[mPosition]);
        } else if (mPosition >= mCount) {
            finish();
        } else {
            mHelpImage.setImageResource(mResources[mPosition]);
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_next) {
            moveToNextFocus();
        }
    }
}
