package thomas.swisher.ui.model;

import android.util.Log;

import com.annimon.stream.function.Consumer;
import com.google.common.base.Optional;

import trikita.anvil.Anvil;
import thomas.swisher.utils.Utils;
import thomas.swisher.shared.Core.*;

import java.util.Collections;

public class UIMenuModel {

    public static class Core {
        private MenuItemList menuItemList = new MenuItemList(Collections.emptyList());
        private Consumer<MenuPath> listener;
        private Optional<MenuPath> pendingMenu = Optional.absent();
        private UIModel.CoreModel core;
        private MenuPath currentPath = MenuPath.Root;

        public Core(UIModel.CoreModel core) {
            this.core = core;
        }

        public MenuPath currentPath() {
            return currentPath;
        }

        public MenuItemList items() {
            return menuItemList;
        }

        public boolean isBusy() {
            return pendingMenu.isPresent();
        }

        public void stopMenuTransition() {
            if (pendingMenu.isPresent()) {
                core.backend().cancelMenu(pendingMenu.get());
                pendingMenu = Optional.absent();
            }
        }

        public void add(Utils.FlatJson json) {
            core.addToPlaylist(json);
        }

        public void play(Utils.FlatJson json) {
            core.play(json);
        }

        public void record(String text, Utils.FlatJson json) {
            core.record(text, json);
        }

        public void onMenuChange(Consumer<MenuPath> listener) {
            this.listener = listener;
        }

        public void goToMenu(MenuPath path) {
            pendingMenu = Optional.of(path);
            core.backend().menuFor(path);
        }

        public void onMenuResponse(MenuPath menuPath, Optional<MenuItemList> menuItemList) {
            Log.i("X", "menu response" + menuPath);
            if (pendingMenu.isPresent() && pendingMenu.get().equals(menuPath)) {
                if (menuItemList.isPresent()) {
                    this.currentPath = menuPath;
                    this.menuItemList = menuItemList.get();
                    this.listener.accept(menuPath);
                } else {
                    //TODO toast
                    Log.e("X", "Menu failed " + menuPath);
                }
                this.pendingMenu = Optional.absent();
                Anvil.render();
            }
        }
    }

}
