package thomas.swisher.ui.view;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import lombok.val;
import trikita.anvil.Anvil;
import trikita.anvil.DSL;
import trikita.anvil.RenderableAdapter;
import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.Iterator;

import thomas.swisher.utils.Utils;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.model.UIMenuModel;

import static android.R.attr.enabled;
import static android.R.attr.path;
import static thomas.swisher.ui.view.AnvilExtras.ForGlide.*;

import static trikita.anvil.DSL.*;

/**
 */
public class TreeMenuView {

    private AlwaysRenderedRenderableAdapter adapter = new AlwaysRenderedRenderableAdapter() {
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

            if (index >= core.items().size()) {
                return; //This is a spare recycled view which is not in use
            }
            Core.UIMenuItem item = core.items().get(index);
            item.render(new Core.UIMenuRender() {
                @Override
                public void renderDoItItem(String name, Runnable runnable) {
                    renderMenuItem(name, 30, Optional.absent(), Optional.absent(), () -> onClick(view -> runnable.run()));
                }

                private Activity activityFromView(View view) {
                    Context context = view.getContext();
                    while (context instanceof ContextWrapper) {
                        if (context instanceof Activity) {
                            return (Activity)context;
                        }
                        context = ((ContextWrapper)context).getBaseContext();
                    }
                    return null;
                }

                @Override
                public void renderDoItItem(String name, Core.ActivityAction action) {
                    renderMenuItem(name, 30, Optional.absent(), Optional.absent(),
                        () -> onClick(view -> {
                            Activity activity = activityFromView(view);
                            if (activity != null) {
                                action.go(core, activity);
                            } else {
                                //TODO error
                            }
                        }));
                }

                @Override
                public void renderSubMenu(String name, String pathName) {
                    val path = core.currentPath().append(name);
                    val loading = core.isLoading(path) ? "**" : "";
                    renderMenuItem(name + "..." + loading, 30, Optional.absent(), Optional.absent(),
                            () -> onClick(view -> core.goToMenu(path)));
                }

                @Override
                public void renderPlaylistItem(String name, Utils.FlatJson json, Optional<Uri> thumbnail) {
                    renderMenuItem(name, 20, thumbnail, Optional.of(json), () -> onClick(view -> {}));
                }

                @Override
                public void renderCardActionItem(String name, Utils.FlatJson json) {
                    renderMenuItem(name, 20, Optional.absent(), Optional.of(json), () -> {});
                }

                @Override
                public void renderMessage(String message) {
                    renderMenuItem(message, 10, Optional.absent(), Optional.absent(), () -> {});
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
                gravity(Gravity.RIGHT);
                for (Core.MenuPath path: core.currentPath().paths()) {
                    String name = path.name().or("Home");
                    if (path.equals(core.currentPath())) {
                        textView(() -> {
                            text(name);
                            textSize(12);
                        });
                    } else {
                        button(() -> {
                            text(name + " >");
                            textSize(12);
                            onClick(view -> core.goToMenu(path));
                        });
                    }
                }
                space(() -> {
                    size(0, 0);
                    weight(1);
                });
                textView(() -> {
                    visibility(core.isBusy() ? View.VISIBLE : View.INVISIBLE);
                    gravity(Gravity.RIGHT);
                    text("Busy");
                });
                button(() -> {
                    DSL.enabled(core.isBusy());
                    gravity(Gravity.RIGHT);
                    text("Stop");
                    onClick(view -> core.stopMenuTransition());
                });
                MenuToggle.button(core.uiRoot(), () -> {
                    gravity(Gravity.RIGHT);
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
