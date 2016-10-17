package thomas.swisher.ui.view;

import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableAdapter;
import com.google.common.base.Optional;
import thomas.swisher.utils.Utils;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.model.UIMenuModel;
import static thomas.swisher.ui.view.AnvilExtras.ForGlide.*;

import static trikita.anvil.DSL.*;

/**
 */
public class TreeMenuView {

    private RenderableAdapter adapter = new RenderableAdapter() {
        private static final int ThumbnailId    = 1;
        private static final int PlayButtonId   = 2;
        private static final int AddButtonId    = 3;
        private static final int RecordButtonId = 4;
        private static final int ItemTextId     = 5;

        private void createButton(int id, int rightOf, String text, boolean visible, Anvil.Renderable more) {
            button(() -> {
                id(id);
                size(50, 50);
                text(text);
                toRightOf(rightOf);
                visibility(visible);
                more.view();
            });
        }
        @Override
        public void view(int index) {

            Core.UIMenuItem item = core.items().get(index);
            Log.i("X", "Rendering view for " + index + " " + item);
            item.render(new Core.UIMenuRender() {
                @Override
                public void renderDoItItem(String name, Runnable runnable) {
                    renderMenuItem(name, 30, Optional.absent(), Optional.absent(), () -> onClick(view -> runnable.run()));
                }

                @Override
                public void renderSubMenu(String name, String pathName) {
                    renderMenuItem(name + "...", 30, Optional.absent(), Optional.absent(),
                            () -> onClick(view -> core.goToMenu(core.currentPath().append(name))));
                }

                @Override
                public void renderPlaylistItem(String name, Utils.FlatJson json, Optional<Uri> thumbnail) {
                    renderMenuItem(name, 20, thumbnail, Optional.of(json), () -> onClick(view -> {}));
                }

                @Override
                public void renderCardActionItem(String name, Utils.FlatJson json) {
                    renderMenuItem(name, 20, Optional.absent(), Optional.of(json), () -> {});
                }
            });
        }

        private void renderMenuItem(String text, int textSize, Optional<Uri> thumbnail,
                                    Optional<Utils.FlatJson> json, Anvil.Renderable textMore) {
            relativeLayout(() -> {
                size(FILL, WRAP);
                orientation(RelativeLayout.CENTER_HORIZONTAL);
                padding(5);

                imageView(() -> {
                    visibility(json.isPresent());
                    id(ThumbnailId);
                    glideURI(thumbnail);
                    size(80, 80);
                });

                createButton(PlayButtonId, ThumbnailId, "\u25b6", json.isPresent(),
                        () -> onClick((v) -> core.play(json.get())));
                createButton(AddButtonId, PlayButtonId, "+", json.isPresent(),
                        () -> onClick((a) -> core.add(json.get())));
                createButton(RecordButtonId, AddButtonId, "\u21af", json.isPresent(),
                        () -> onClick((a) -> core.record(text, json.get())));

                textView(() -> {
                    id(ItemTextId);
                    size(FILL, WRAP);
                    below(ThumbnailId);
                    padding(50, 0, 0, 0);
                    textSize(textSize);
                    text(text);
                    textMore.view();
                });
            });
        }

        @Override
        public int getCount() {
            return core.items().size();
        }

        @Override
        public Object getItem(int position) {
            return core.items().get(position);
        }
    };

    private final UIMenuModel.Core core;

    public void view() {
        linearLayout(() -> {
            orientation(LinearLayout.VERTICAL);

            linearLayout(() -> {
                button(() -> {
                    text("<-");
                    onClick((v) -> core.goToMenu(core.currentPath().parent().get()));
                    enabled(!core.currentPath().isRoot());
                });
                textView(() -> {
                    textSize(12);
                    text(core.currentPath().text());
                });
                textView(() -> {
                    text(core.isBusy() ? " Busy " : "");
                });
            });
            listView(() -> {
                size(FILL, FILL);
                itemsCanFocus(false);
                adapter(adapter);
            });
        });
    }
    public TreeMenuView(UIMenuModel.Core core) {
        this.core = core;
        core.onMenuChange((path) -> {
            Log.i("W", "menu change");
            adapter.notifyDataSetChanged();
        });
    }

}
