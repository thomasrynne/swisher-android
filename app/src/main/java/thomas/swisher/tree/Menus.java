package thomas.swisher.tree;

import java.util.List;

import lombok.Value;
import thomas.swisher.shared.Core;

/**
 * The api for adding new items to the menu
 */
public class Menus {

    public interface Menu {
        public String name();
        public List<MenuEntry> items();
    }

    public interface MenuEntry {
        public Core.UIMenuItem toUI();
    }

    @Value
    public static class SubMenuMenuEntry implements MenuEntry {
        private final Menu menu;

        @Override
        public Core.UIMenuItem toUI() {
            return new Core.SubMenuUIMenuItem(menu.name(), menu.name());
        }

        public String name() {
            return menu.name();
        }
    }
    @Value
    public static class FixedItemMenuEntry implements MenuEntry {
        private final Core.UIMenuItem item;

        @Override
        public Core.UIMenuItem toUI() {
            return item;
        }
    }


}
