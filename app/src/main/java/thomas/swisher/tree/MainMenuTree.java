package thomas.swisher.tree;


import com.annimon.stream.Stream;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import lombok.Value;
import lombok.val;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.UIBackendEvents;
import uk.co.thomasrynne.swisher.Events;
import uk.co.thomasrynne.swisher.Utils;

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
            return Utils.list(Stream.of(Arrays.asList("stop", "pause", "next", "previous")).
                    map(MainMenuTree::createActionItem));
        }
    };

    //Todo: either make the doit menu item always post
    //or pass the main menu tree a more explicit control class
    private final Core.DoItUIMenuItem clearPlaylist = new Core.DoItUIMenuItem(
            "Clear Playlist",
            () -> eventBus.post(new UIBackendEvents.ClearPlayListEvent())
    );

    private final Core.DoItUIMenuItem recordPlaylist = new Core.DoItUIMenuItem(
            "Record Playlist",
            () -> eventBus.post(new Events.RecordPlayListEvent())
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
            val subMenus = Stream.of(menu.items()).select(Menus.SubMenuMenuEntry.class);
            menu = subMenus.filter( (sub) -> sub.name().equals(pathEntry) ).findSingle().get().getMenu();
        }
        return new Core.MenuItemList(Utils.list(Stream.of(menu.items()).map((entry) -> entry.toUI())));
    }

    private static Menus.FixedItemMenuEntry createActionItem(String action) {
        return new Menus.FixedItemMenuEntry(new Core.CardActionUIMenuITem(
            action,
            Utils.json().add("action", action).build()
        ));
    }

}