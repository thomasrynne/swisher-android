package uk.co.thomasrynne.swisher.tree;

import android.net.Uri;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Value;

/**
 * Defines the menu behaviour, used by the android ui, web ui and remote android ui
 */
public class MenuTree {

    public interface MenuItem {
        public String name();
        public Uri image();
        public List<MenuItem> children();

        public static MenuItem tree(String name, String path, MenuItem... children) {
            return new FixedTreeItem(name, Arrays.asList(children));
        }
    }

    public static MenuItem Null = new MenuItem() {
        @Override
        public String name() {
            return "Pending";
        }

        @Override
        public Uri image() {
            return null;
        }

        @Override
        public List<MenuItem> children() {
            return Collections.emptyList();
        }
    };


    @Value
    static class FixedTreeItem implements MenuItem {
        String name;
        List<MenuItem> children;

        @Override
        public String name() {
            return name;
        }

        @Override
        public Uri image() {
            return null;
        }

        @Override
        public List<MenuItem> children() {
            return children;
        }
    }

    @Value
    public static class PlaylistItemMenuItem implements MenuItem {
        public String name;
        public Uri image;
        public JSONObject json;

        @Override
        public String name() {
            return name;
        }

        @Override
        public Uri image() {
            return image;
        }

        @Override
        public List<MenuItem> children() {
            return Collections.emptyList();
        }
    }

    @Value
    public static class InvokeMenuItem implements MenuItem {
        public String name;
        public Runnable action;
        public JSONObject json;

        @Override
        public String name() {
            return name;
        }

        @Override
        public Uri image() {
            return null;
        }

        @Override
        public List<MenuItem> children() {
            return Collections.emptyList();
        }
    }

}
