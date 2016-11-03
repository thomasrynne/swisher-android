package thomas.swisher.ui;

import android.view.Gravity;
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
import static trikita.anvil.DSL.orientation;

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
                    padding(0,0,0,0);
                    padding(15);
                    orientation(LinearLayout.HORIZONTAL);

                    frameLayout(() -> { //---------------------------------[Tracks]
                        size(FILL, FILL);
                        weight(1);
                        gravity(Gravity.TOP);
                        tracksView.view();
                    });

                    linearLayout(() -> { //--------------------------------[Controls]
                        weight(1);
                        orientation(LinearLayout.VERTICAL);
                        height(FILL);

                        linearLayout(() -> { //-----------------------------[Buttons]
                            height(WRAP);
                            controlView.view();
                        });

                        linearLayout(() -> {
                            height(0);
                            weight(1);
                            layoutGravity(Gravity.CENTER);
                            imageView(() -> {    //----------------------------[Main Image]
                                layoutGravity(Gravity.CENTER);
                                visibility(!coreUI.showYouTube());
                                glideURI(coreUI.bigImage());
                            });
                            frameLayout(() -> {
                                layoutGravity(Gravity.CENTER);
                                visibility(coreUI.showYouTube());
                                DSL.id(YOUTUBE_FRAME_LAYOUT_ID);
                            });
                        });
                    });

                    frameLayout(() -> { //---------------------------------[Menu]
                        weight(1);
                        visibility(coreUI.showMenu());
                        treeMenuView.view();
                        size(FILL, FILL);
                    });
                });
            };
        };
    }

}
