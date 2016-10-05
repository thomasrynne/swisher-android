package thomas.swisher.ui.view;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.common.base.Optional;

import trikita.anvil.Anvil;
import trikita.anvil.BaseDSL;

/**
 * Additional anvil methods
 */
public class AnvilExtras {

    public static void glideURI(Optional<Uri> uri) {
        BaseDSL.attr(GlideImageSrcFunction.instance, uri);
    }

    private final static class GlideImageSrcFunction implements Anvil.AttrFunc<Optional<Uri>> {
        private final static GlideImageSrcFunction instance = new GlideImageSrcFunction();

        public void apply(View view, Optional<Uri> newUri, Optional<Uri> oldUri) {
            if (view instanceof ImageView) {
                if (newUri.isPresent()) {
                    Glide.with(getActivityFor(view)).load(newUri.get()).fitCenter().into((ImageView) view);
                } else {
                    ((ImageView) view).setImageDrawable(null);
                }
            }
        }

        private Activity getActivityFor(View view) {
            Context context = view.getContext();
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity) {
                    return (Activity)context;
                }
                context = ((ContextWrapper)context).getBaseContext();
            }
            return null;
        }
    }
}
