package thomas.swisher.shared;

import android.net.Uri;
import android.text.TextUtils;

import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.List;

import lombok.Value;
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
        public final boolean isCurrentTrack;
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
        public void renderSubMenu(String name, String pathName);
        public void renderPlaylistItem(String name, Utils.FlatJson json, Optional<Uri> thumbnail);
        public void renderCardActionItem(String name, Utils.FlatJson json);
    }
    public interface UIMenuItem {
        public void render(UIMenuRender render);
    }

    @Value
    public static class DoItUIMenuItem implements UIMenuItem {
        private final String label;
        private final Runnable doIt;
        @Override
        public void render(UIMenuRender renderer) {
            renderer.renderDoItItem(label, doIt);
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
