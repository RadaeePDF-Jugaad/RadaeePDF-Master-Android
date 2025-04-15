package com.radaee.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.radaee.pdfmaster.R;
import com.radaee.utility.FileThumbnailRequestHandler;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter for file list
 */
public class DropboxFilesAdapter extends RecyclerView.Adapter<DropboxFilesAdapter.MetadataViewHolder> {
    private List<Metadata> mFiles;
    private final Picasso mPicasso;
    private final Callback mCallback;

    public void setFiles(List<Metadata> files) {
        mFiles = Collections.unmodifiableList(new ArrayList<>(files));
        notifyDataSetChanged();
    }

    public interface Callback {
        void onFolderClicked(FolderMetadata folder);

        void onFileClicked(FileMetadata file);
    }

    public DropboxFilesAdapter(Picasso picasso, Callback callback) {
        mPicasso = picasso;
        mCallback = callback;
    }

    @Override
    public MetadataViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.files_item, viewGroup, false);
        return new MetadataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MetadataViewHolder metadataViewHolder, int i) {
        metadataViewHolder.bind(mFiles.get(i));
    }

    @Override
    public long getItemId(int position) {
        return mFiles.get(position).getPathLower().hashCode();
    }

    @Override
    public int getItemCount() {
        return mFiles == null ? 0 : mFiles.size();
    }

    public class MetadataViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView mTextView;
        private final ImageView mImageView;
        private Metadata mItem;

        public MetadataViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.image);
            mTextView = (TextView) itemView.findViewById(R.id.text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            if (mItem instanceof FolderMetadata) {
                mCallback.onFolderClicked((FolderMetadata) mItem);
            } else if (mItem instanceof FileMetadata) {
                mCallback.onFileClicked((FileMetadata) mItem);
            }
        }

        public void bind(Metadata item) {
            mItem = item;
            mTextView.setText(mItem.getName());

            // Load based on file path
            // Prepending a magic scheme to get it to
            // be picked up by DropboxPicassoRequestHandler

            if (item instanceof FileMetadata) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                String ext = item.getName().substring(item.getName().indexOf(".") + 1);
                String type = mime.getMimeTypeFromExtension(ext);
                if (type != null && type.equals("application/pdf")) {
                    mPicasso.load(FileThumbnailRequestHandler.buildPicassoUri((FileMetadata) item))
                            .placeholder(R.drawable.ic_pdf_file)
                            .error(R.drawable.ic_pdf_file)
                            .into(mImageView);
                } else if (type != null && type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                    mPicasso.load(FileThumbnailRequestHandler.buildPicassoUri((FileMetadata) item))
                            .placeholder(R.drawable.ic_docx_file)
                            .error(R.drawable.ic_docx_file)
                            .into(mImageView);
                }
            } else if (item instanceof FolderMetadata) {
                if (item.getPathDisplay().equals("refresh")) {
                    mPicasso.load(R.drawable.ic_refresh)
                            .noFade()
                            .into(mImageView);
                } else if (item.getName().equals("..")) {
                    mPicasso.load(R.drawable.ic_go_up)
                            .noFade()
                            .into(mImageView);
                } else {
                    mPicasso.load(R.drawable.ic_folder)
                            .noFade()
                            .into(mImageView);
                }
            }
        }
    }
}
