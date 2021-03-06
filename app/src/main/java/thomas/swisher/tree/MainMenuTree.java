package thomas.swisher.tree;


import com.google.common.collect.FluentIterable;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import lombok.val;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.UIBackendEvents;
import thomas.swisher.utils.Utils;

/**
 */
public class MainMenuTree {

    private EventBus eventBus;
    private List<Menus.MenuEntry> rootItems;

    public MainMenuTree(EventBus eventBus, Menus.Menu... menus) {
        this.eventBus = eventBus;
        this.rootItems = new LinkedList<>();
        for (Menus.Menu menu: menus) {
            rootItems.add(new Menus.SubMenuMenuEntry(menu));
        }
        rootItems.add(new Menus.SubMenuMenuEntry(actionMenu));
        rootItems.add(new Menus.FixedItemMenuEntry(clearPlaylist));
        rootItems.add(new Menus.FixedItemMenuEntry(recordPlaylist));
    }

    private final Menus.Menu actionMenu = new Menus.Menu() {
        @Override
        public String name() {
            return "Actions";
        }
        @Override
        public List<Menus.MenuEntry> items() {
            return FluentIterable.from(Arrays.asList("stop", "pause", "next", "previous")).
                    transform(MainMenuTree::createActionItem).toList();
        }
    };

    //Todo: either make the doit menu item always post
    //or pass the main menu tree a more explicit control class
    private final Core.DoItBackendUIMenuItem clearPlaylist = new Core.DoItBackendUIMenuItem(
            "Clear Playlist",
            () -> eventBus.post(new UIBackendEvents.ClearPlayListEvent())
    );

    private final Core.DoItBackendUIMenuItem recordPlaylist = new Core.DoItBackendUIMenuItem(
            "Record Playlist",
            () -> eventBus.post(new UIBackendEvents.RecordPlayListEvent())
    );


    private final Menus.Menu root = new Menus.Menu() {
        public String name() {
            return "Root";
        }

        @Override
        public List<Menus.MenuEntry> items() {
            return rootItems;
        }
    };

    public Core.MenuItemList menuFor(Core.MenuPath path) {
        Menus.Menu menu = root;
        for (String pathEntry : path.getPath()) {
            val subMenus = FluentIterable.from(menu.items()).filter(Menus.SubMenuMenuEntry.class);
            menu = subMenus.filter( (sub) -> sub.name().equals(pathEntry) ).first().get().getMenu();
        }
        return new Core.MenuItemList(FluentIterable.from(menu.items()).transform((entry) -> entry.toUI()).toList());
    }

    private static Menus.MenuEntry createActionItem(String action) {
        return new Menus.FixedItemMenuEntry(new Core.CardActionUIMenuITem(
            action,
            Utils.json().add("action", action).build()
        ));
    }

}