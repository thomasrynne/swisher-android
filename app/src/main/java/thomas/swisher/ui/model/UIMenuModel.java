package thomas.swisher.ui.model;

import android.util.Log;

import com.annimon.stream.function.Consumer;
import com.google.common.base.Optional;

import thomas.swisher.shared.Core;
import thomas.swisher.ui.UIBackendEvents;
import trikita.anvil.Anvil;
import thomas.swisher.utils.Utils;
import thomas.swisher.shared.Core.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.SynchronousQueue;

public class UIMenuModel {

    public static class Core {
        private MenuItemList menuItemList = new MenuItemList(Collections.emptyList());
        private Consumer<MenuPath> listener;
        private Optional<MenuPath> pendingMenu = Optional.absent();
        private UIModel.CoreModel core;
        private MenuPath currentPath = MenuPath.Root;
        private long menuRequestStart;
        private static final long IGNORED_MENU_DURATION = 300;

        public Core(UIModel.CoreModel core) {
            this.core = core;
        }

        public MenuPath currentPath() {
            return currentPath;
        }

        public boolean isLoading(MenuPath path) {
            return pendingMenu.isPresent() && pendingMenu.get().equals(path);
        }

        public void back() {
            if (currentPath().parent().isPresent()) {
                goToMenu(currentPath().parent().get());
            }
        }

        public boolean canBack() {
            return !currentPath().isRoot();
        }

        public MenuItemList items() {
            return menuItemList;
        }

        public boolean isBusy() {
            long timeSinceMenuRequest = System.currentTimeMillis()  - menuRequestStart;
            return pendingMenu.isPresent() && (timeSinceMenuRequest > IGNORED_MENU_DURATION);
        }

        public void stopMenuTransition() {
            if (pendingMenu.isPresent()) {
                //For now we forget about the requested menu. Perhaps we should cancel the request.
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
            stopMenuTransition();
            pendingMenu = Optional.of(path);
            menuRequestStart = System.currentTimeMillis();
            core.backend().menuFor(path);
            core.runLater(() -> Anvil.render(), IGNORED_MENU_DURATION + 10);
            Anvil.render();
        }

        public void onMenuResponse(MenuPath menuPath, UIBackendEvents.MenuResult result) {
            if (pendingMenu.isPresent() && pendingMenu.get().equals(menuPath)) {
                result.handle(new UIBackendEvents.MenuResultHandler() {
                    @Override
                    public void visit(UIBackendEvents.SuccessMenuResult success) {
                        menuItemList = success.menuItemList;
                    }

                    @Override
                    public void visit(UIBackendEvents.FailureMenuResult failure) {
                        menuItemList = new MenuItemList(Arrays.asList(new ErrorMenuItem(failure.message)));
                    }
                });
                currentPath = menuPath;
                listener.accept(menuPath);
                this.pendingMenu = Optional.absent();
                Anvil.render();
            }
        }

        public UIModel.CoreModel uiRoot() {
            return core;
        }
    }

}
