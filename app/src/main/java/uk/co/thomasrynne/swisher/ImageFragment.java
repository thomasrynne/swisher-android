package uk.co.thomasrynne.swisher;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 */
public class ImageFragment extends Fragment {

    private ImageView imageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        imageView = new ImageView(getActivity());
        return imageView;
    }

    public void setImageURI(Uri image) {
        if (imageView != null) {
            this.imageView.setImageURI(image);
        } else {
            if (image != null) {
                Log.e("SWISHER", "not setting image");
            }
        }
    }
}
