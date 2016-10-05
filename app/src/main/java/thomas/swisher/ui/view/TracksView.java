package thomas.swisher.ui.view;

import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import lombok.val;
import thomas.swisher.R;
import trikita.anvil.Anvil;
import trikita.anvil.BaseDSL;
import thomas.swisher.ui.model.UITracks;

import com.google.common.base.Optional;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;

import static trikita.anvil.BaseDSL.*;
import static trikita.anvil.DSL.*;
import static trikita.anvil.DSL.visibility;
import static thomas.swisher.ui.view.AnvilExtras.*;

/**
 */
public class TracksView {

    private UITracks.Model model;

    public TracksView(UITracks.Model model) {
        this.model = model;
        model.onTracksChange(() -> {
            adapter.notifyDataSetChanged();
        });
    }

    private AlwaysRenderedRenderableAdapter adapter = new AlwaysRenderedRenderableAdapter() {

        private Optional<Integer> touchedTrack = Optional.absent();

        @Override
        public void view(int index) {
            Optional<UITracks.PlaylistEntry> maybeItem = model.trackAt(index);
            if (maybeItem.isPresent()) {
                val item = maybeItem.get();
                linearLayout(() -> {
                    size(MATCH, MATCH);
                    orientation(LinearLayout.HORIZONTAL);
                    imageView(() -> {
                        size(dip(100), WRAP);
                        padding(0, 0, dip(1), 0);
                        glideURI(item.getThumbnail());
                    });
                    linearLayout(() -> {
                        orientation(LinearLayout.VERTICAL);
                        size(MATCH, WRAP);

                        textView(() -> {
                            size(MATCH, WRAP);
                            textSize(18);
                            layoutGravity(TOP);
                            visibility(item.getTopText().isPresent());
                            if (item.getTopText().isPresent()) {
                                text(item.getTopText().get());
                            }
                        });

                        textView(() -> {
                            textSize(22);
                            size(MATCH, WRAP);
                            layoutGravity(BOTTOM);
                            visibility(item.getTrackName().isPresent());
                            if (item.getTrackName().isPresent()) {
                                text(item.getTrackName().get());
                            }
                            if (item.isCurrentTrack) {
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
                                        model.playTrackAt(touchedIndex);
                                    case MotionEvent.ACTION_CANCEL:
                                        touchedTrack = Optional.absent();
                                }
                                return true;
                            });
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
    };

    public void view() {
        dynamicListView(() -> {
           size(FILL, FILL);
           adapter(adapter);
        });
    }

    private void dynamicListView(Anvil.Renderable renderable) {
        BaseDSL.v(DynamicListView.class, renderable);
    }
}
