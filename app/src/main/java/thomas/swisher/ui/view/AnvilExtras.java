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
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.dragdrop.TouchViewDraggableManager;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;

import trikita.anvil.Anvil;
import trikita.anvil.BaseDSL;

/**
 * Additional anvil methods
 */
public class AnvilExtras {

    public static class ForGlide {

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
                        return (Activity) context;
                    }
                    context = ((ContextWrapper) context).getBaseContext();
                }
                return null;
            }
        }
    }

    public static class ForDynamicListView {

        public static void dynamicListView(Anvil.Renderable renderable) {
            BaseDSL.v(com.nhaarman.listviewanimations.itemmanipulation.DynamicListView.class, renderable);
        }

        public static void enableDragAndDropOn(Optional<Integer> touchId) {
            BaseDSL.attr(EnableDragAndDropFunction.instance, touchId);
        }

        public static void enableSwipeToDismiss(OnDismissCallback callback) {
            BaseDSL.attr(EnableSwipeToDismissFunction.instance, callback);
        }

        private final static class EnableDragAndDropFunction implements Anvil.AttrFunc<Optional<Integer>> {
            private final static EnableDragAndDropFunction instance = new EnableDragAndDropFunction();

            public void apply(View view, Optional<Integer> newValue, Optional<Integer> oldValue) {
                if (view instanceof com.nhaarman.listviewanimations.itemmanipulation.DynamicListView) {
                    if (newValue.isPresent()) {
                        ((com.nhaarman.listviewanimations.itemmanipulation.DynamicListView) view).enableDragAndDrop();
                        ((com.nhaarman.listviewanimations.itemmanipulation.DynamicListView) view).setDraggableManager(new TouchViewDraggableManager(newValue.get()));
                    } else {
                        ((com.nhaarman.listviewanimations.itemmanipulation.DynamicListView) view).disableDragAndDrop();
                    }
                }
            }
        }

        private final static class EnableSwipeToDismissFunction implements Anvil.AttrFunc<OnDismissCallback > {
            private final static EnableSwipeToDismissFunction instance = new EnableSwipeToDismissFunction();

            public void apply(View view, OnDismissCallback newCallback, OnDismissCallback  oldCalback) {
                if (view instanceof com.nhaarman.listviewanimations.itemmanipulation.DynamicListView) {
                    ((com.nhaarman.listviewanimations.itemmanipulation.DynamicListView) view).enableSwipeToDismiss(newCallback);
                }
            }
        }
    }
}
