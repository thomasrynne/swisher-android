package thomas.swisher.ui.view;

import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import lombok.val;
import thomas.swisher.R;
import thomas.swisher.ui.model.UITracks;

import com.google.common.base.Optional;
import com.nhaarman.listviewanimations.util.Swappable;

import static trikita.anvil.BaseDSL.*;
import static trikita.anvil.DSL.*;
import static trikita.anvil.DSL.visibility;
import static thomas.swisher.ui.view.AnvilExtras.ForGlide.*;
import static thomas.swisher.ui.view.AnvilExtras.ForDynamicListView.*;

/**
 * Defines the UI layout and UI interactions for the tracks area
 */
public class TracksView {

    private static final int MainTextId = 1;
    private static final int ImageId  = 2;
    private static final int TrackId = 3;

    private UITracks.Model model;

    public TracksView(UITracks.Model model) {
        this.model = model;
        model.onTracksChange(() -> {
            adapter.notifyDataSetChanged();
        });
    }

    private AlwaysRenderedRenderableAdapter adapter = new TracksAdapter();
    private class TracksAdapter extends AlwaysRenderedRenderableAdapter implements Swappable {

        private Optional<Integer> touchedTrack = Optional.absent();

        @Override
        public void swapItems(int a, int b) {
            model.swap(a, b);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void view(int index) {
            Optional<UITracks.Model.PlaylistEntry> maybeItem = model.trackAt(index);
            if (maybeItem.isPresent()) {
                val item = maybeItem.get();
                relativeLayout(() -> {
                    size(MATCH, WRAP);
                    imageView(() -> {
                        id(ImageId);
                        size(dip(100), WRAP);
                        padding(0, 0, dip(1), 0);
                        glideURI(item.getThumbnail());
                    });

                    textView(() -> {
                        id(MainTextId);
                        size(FILL, WRAP);
                        toRightOf(ImageId);
                        alignParentTop();
                        textSize(18);
                        layoutGravity(TOP);
                        visibility(item.getTopText().isPresent());
                        if (item.getTopText().isPresent()) {
                            text(item.getTopText().get());
                        }
                    });

                    textView(() -> {
                        id(TrackId);
                        size(FILL, WRAP);
                        toRightOf(ImageId);
                        alignParentBottom();
                        textSize(22);
                        layoutGravity(BOTTOM);
                        visibility(item.getTrackName().isPresent());
                        if (item.getTrackName().isPresent()) {
                            text(item.getTrackName().get());
                        }
                        if (item.isCurrentTrack()) {
                            backgroundResource(R.drawable.track_playing);
                        } else if (index == touchedTrack.or(-1)) {
                            backgroundResource(R.drawable.track_touch);
                        } else {
                            backgroundResource(R.drawable.track_plain);
                        }
                        tag(index);
                        onTouch((view, event) -> {
                            int touchedIndex = ((Integer) view.getTag()).intValue();
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    touchedTrack = Optional.of(touchedIndex);
                                    break;
                                case MotionEvent.ACTION_UP:
                                    touchedTrack = Optional.absent();
                                    model.playTrackAt(touchedIndex);
                                    break;
                                case MotionEvent.ACTION_CANCEL:
                                    touchedTrack = Optional.absent();
                                    break;
                            }
                            return true;
                        });
                    });

                });
            }
        }

        @Override
        public int getCount() {
            return model.trackCount();
        }

        @Override
        public Object getItem(int position) {
            return model.trackAt(position);
        }

        @Override
        public long getItemId(int position) {
            val track = model.trackAt(position);
            if (track.isPresent()) {
                return track.get().itemID;
            } else {
                return -1L;
            }
        }
    };

    public void view() {
        dynamicListView(() -> {
            size(FILL, FILL);
            adapter(adapter);
            if (model.enableDragAndDrop()) {
                enableDragAndDropOn(Optional.of(MainTextId));
            } else {
                enableDragAndDropOn(Optional.absent());
            }
            enableSwipeToDismiss( (view, reverseSortedPositions) -> {
                for (int position : reverseSortedPositions) {
                    model.remove(position);
                }
            });
        });
    }
}