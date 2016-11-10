package thomas.swisher.ui;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import thomas.swisher.ui.model.UIModel;
import thomas.swisher.ui.view.ControlView;
import thomas.swisher.ui.view.TracksView;
import thomas.swisher.ui.view.TreeMenuView;
import trikita.anvil.Anvil;
import trikita.anvil.DSL;

import static thomas.swisher.ui.view.AnvilExtras.ForGlide.glideURI;
import static trikita.anvil.BaseDSL.FILL;
import static trikita.anvil.BaseDSL.MATCH;
import static trikita.anvil.BaseDSL.WRAP;
import static trikita.anvil.BaseDSL.layoutGravity;
import static trikita.anvil.BaseDSL.padding;
import static trikita.anvil.BaseDSL.size;
import static trikita.anvil.BaseDSL.visibility;
import static trikita.anvil.BaseDSL.weight;
import static trikita.anvil.DSL.frameLayout;
import static trikita.anvil.DSL.gravity;
import static trikita.anvil.DSL.height;
import static trikita.anvil.DSL.imageView;
import static trikita.anvil.DSL.linearLayout;
import static trikita.anvil.DSL.onClick;
import static trikita.anvil.DSL.onSystemUiVisibilityChange;
import static trikita.anvil.DSL.orientation;
import static trikita.anvil.DSL.space;
import static trikita.anvil.DSL.systemUiVisibility;

/**
 *
 */

public class MainActivityLayout {

    public static final int YOUTUBE_FRAME_LAYOUT_ID = 998;

    private UIModel.CoreModel coreUI;
    private TreeMenuView treeMenuView;
    private ControlView controlView;
    private TracksView tracksView;

    public MainActivityLayout(UIModel.CoreModel coreUI) {
        this.coreUI = coreUI;
        this.treeMenuView = new TreeMenuView(coreUI.menu());
        this.controlView = new ControlView(coreUI.controls());
        this.tracksView = new TracksView(coreUI.tracks());
    }

    Anvil.Renderable view() {
        return new Anvil.Renderable() {
            @Override
            public void view() {
                linearLayout(() -> {
                    size(MATCH, MATCH);
                    padding(coreUI.isFullScreen() ? 0 : 15);
                    orientation(LinearLayout.HORIZONTAL);

                    frameLayout(() -> { //---------------------------------[Tracks]
                        size(FILL, FILL);
                        visibility(!coreUI.isFullScreen());
                        weight(1);
                        gravity(Gravity.TOP);
                        tracksView.view();
                    });

                    linearLayout(() -> { //--------------------------------[Controls]
                        weight(1);
                        orientation(LinearLayout.VERTICAL);
                        size(FILL, FILL);
                        linearLayout(() -> { //-----------------------------[Buttons]
                            height(WRAP);
                            visibility(!coreUI.isFullScreen());
                            controlView.view();
                        });

                        linearLayout(() -> {
                            height(0);
                            weight(1);
                            orientation(LinearLayout.HORIZONTAL);
                            layoutGravity(Gravity.CENTER);

                            imageView(() -> {    //----------------------------[Main Image]
                                layoutGravity(Gravity.CENTER);
                                visibility(!coreUI.showYouTube());
                                glideURI(coreUI.bigImage());
                                onClick( view -> coreUI.toggleFullScreen());
                            });

                            linearLayout(() -> {
                                layoutGravity(Gravity.CENTER);
                                visibility(coreUI.showYouTube());
                                size(FILL, FILL);
                                frameLayout(() -> {
                                    layoutGravity(Gravity.CENTER);
                                    size(FILL, FILL);
                                    DSL.id(YOUTUBE_FRAME_LAYOUT_ID);
                                });
                            });
                        });
                    });

                    frameLayout(() -> { //---------------------------------[Menu]
                        weight(1);
                        visibility(coreUI.showMenu() && !coreUI.isFullScreen());
                        treeMenuView.view();
                        size(FILL, FILL);
                    });
                });

                systemUiVisibility(coreUI.isFullScreen() ?
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION : View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

                onSystemUiVisibilityChange((view) -> {
                    //If Android wants to show controls (because there was a screen touch when in full screen)
                    //we drop out of full screen mode to show the swisher controls too.
                    //without this if youtube is off line you can't get back from full screen mode because
                    //the youtube fullscreen toggle is not shown when offline
                    if ((view & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                        coreUI.updateFullScreen(false);
                    }
                });

            };
        };
    }

}