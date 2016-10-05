package thomas.swisher.tree;

import com.google.common.collect.FluentIterable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lombok.val;
import thomas.swisher.shared.Core;

/**
 * Shared helper menu methods
 */
public class MenuUtils {

    public static List<Menus.MenuEntry> toMenuEntries(List<? extends Core.UIMenuItem> entries) {
        return FluentIterable.from(entries).transform((entry) -> (Menus.MenuEntry)new Menus.FixedItemMenuEntry(entry)).toList();
    }

    /**
     * Replaces a list of items with groups for each letter if there are too many
     */
    public static List<Menus.MenuEntry> alphabet(List<Core.PlaylistItemUIMenuItem> items) {
        if (items.size() < 50) {
            return toMenuEntries(items);
        } else {
            Map<String,List<Core.PlaylistItemUIMenuItem>> alphabet = new TreeMap<>();
            for (Core.PlaylistItemUIMenuItem entry: items) {
                String firstLetter = entry.getName().substring(0, 1);
                if (!alphabet.containsKey(firstLetter)) {
                    alphabet.put(firstLetter, new LinkedList<>());
                }
                alphabet.get(firstLetter).add(entry);
            }
            List<Menus.MenuEntry> menu = new LinkedList<>();
            for (Map.Entry<String,List<Core.PlaylistItemUIMenuItem>> entry: alphabet.entrySet()) {
                val m = new Menus.Menu() {
                    @Override
                    public String name() {
                        return entry.getKey();
                    }

                    @Override
                    public List<Menus.MenuEntry> items() {
                        return toMenuEntries(entry.getValue());
                    }
                };
                menu.add(new Menus.SubMenuMenuEntry(m));
            }
            return menu;
        }
    }
}
