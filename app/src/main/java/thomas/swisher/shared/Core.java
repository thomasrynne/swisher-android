package thomas.swisher.shared;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;

import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import lombok.Value;
import lombok.val;
import thomas.swisher.ui.model.UIMenuModel;
import thomas.swisher.utils.Utils;

/**
 * Holds types shared between the GUI and service
 */
public class Core {

    @Value
    public static class PlaylistEntry {
        public final long id;
        public final String name;
        public final Optional<Uri> thumbnail;
        public final List<Track> tracks;
    }

    @Value
    public static class Track {
        public final String name;
        public final Optional<Uri> image;
    }

    @Value
    public static class PlayerProgress {
        public static PlayerProgress Null = new PlayerProgress(0, 0, false);
        public final int totalMillis;
        public final int progressMillis;
        public boolean enabled;
        public PlayerProgress withProgressMillis(int progressMillis) {
            return new PlayerProgress(totalMillis, progressMillis, enabled);
        }
    }



    @Value
    public static class MenuPath {
        public static MenuPath Root = new MenuPath(new String[]{});
        private final String[] path;
        public MenuPath append(String pathName) {
            String[] newPath = Arrays.copyOf(path, path.length + 1);
            newPath[newPath.length - 1] = pathName;
            return new MenuPath(newPath);
        }
        public Optional<MenuPath> parent() {
            switch (path.length) {
                case 0:
                    return Optional.absent();
                case 1:
                    return Optional.of(Root);
                default:
                    String[] newPath = Arrays.copyOf(path, path.length - 1);
                    return Optional.of(new MenuPath(newPath));
            }
        }
        public String text() {
            return TextUtils.join(" / ", path);
        }
        public boolean isRoot() {
            return path.length == 0;
        }

        public List<MenuPath> paths() {
            val list = new LinkedList<MenuPath>();
            list.add(MenuPath.Root);
            for (int i = 0; i < path.length; i++) {
                list.add(new MenuPath(Arrays.copyOf(path, i+1)));
            }
            return list;
        }
        public Optional<String> name() {
            if (isRoot()) {
                return Optional.absent();
            } else {
                return Optional.of(path[path.length-1]);
            }
        }
    }

    @Value
    public static class MenuItemList {
        private final List<UIMenuItem> items;
        public UIMenuItem get(int index) {
            return items.get(index);
        }
        public int size() {
            return items.size();
        }
    }

    public interface UIMenuRender {
        public void renderDoItItem(String name, Runnable runnable);
        public void renderDoItItem(String name, ActivityAction action);
        public void renderSubMenu(String name, String pathName);
        public void renderPlaylistItem(String name, Utils.FlatJson json, Optional<Uri> thumbnail);
        public void renderCardActionItem(String name, Utils.FlatJson json);
        public void renderMessage(String message);
    }
    public interface UIMenuItem {
        public void render(UIMenuRender render);
    }

    @Value
    public static class DoItBackendUIMenuItem implements UIMenuItem {
        private final String label;
        private final Runnable doIt;
        @Override
        public void render(UIMenuRender renderer) {
            renderer.renderDoItItem(label, doIt);
        }
    }

    public interface ActivityAction {
        public void go(UIMenuModel.Core core, Activity activity);
    }

    @Value
    public static class DoItActivityUIMenuItem implements UIMenuItem {
        private final String label;
        private final ActivityAction doIt;
        @Override
        public void render(UIMenuRender renderer) {
            renderer.renderDoItItem(label, doIt);
        }
    }

    @Value
    public static class ErrorMenuItem implements UIMenuItem {
        private final String message;
        @Override
        public void render(UIMenuRender renderer) {
            renderer.renderMessage(message);
        }
    }

    @Value
    public static class SubMenuUIMenuItem implements UIMenuItem {
        private final String label;
        private final String pathComponent;

        @Override
        public void render(UIMenuRender renderer) {
            renderer.renderSubMenu(label, pathComponent);
        }
    }

    @Value
    public static class PlaylistItemUIMenuItem implements UIMenuItem {
        private final String name;
        private final Utils.FlatJson json;
        private final Optional<Uri> thumbnail;

        @Override
        public void render(UIMenuRender renderer) {
            renderer.renderPlaylistItem(name, json, thumbnail);
        }
    }

    @Value
    public static class CardActionUIMenuITem implements UIMenuItem {
        private final String name;
        private final Utils.FlatJson json;

        @Override
        public void render(UIMenuRender renderer) {
            renderer.renderCardActionItem(name, json);
        }
    }


}
