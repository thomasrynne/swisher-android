package thomas.swisher.ui.view;

import thomas.swisher.R;
import thomas.swisher.ui.model.UIControls;
import thomas.swisher.ui.model.UIModel;
import trikita.anvil.Anvil;

import static trikita.anvil.BaseDSL.RIGHT;
import static trikita.anvil.BaseDSL.alignParentRight;
import static trikita.anvil.BaseDSL.alignParentTop;
import static trikita.anvil.BaseDSL.centerHorizontal;
import static trikita.anvil.BaseDSL.dip;
import static trikita.anvil.BaseDSL.layoutGravity;
import static trikita.anvil.BaseDSL.margin;
import static trikita.anvil.BaseDSL.size;
import static trikita.anvil.BaseDSL.textSize;
import static trikita.anvil.DSL.imageButton;
import static trikita.anvil.DSL.imageResource;
import static trikita.anvil.DSL.onClick;

/**
 * The menu toggle used in the menu area and the control area
 * so that it is always in the top right of the screen
 */
public class MenuToggle {
    public static void button(UIModel.CoreModel uiRoot, Anvil.Renderable renderable) {
        imageButton(() -> {
            centerHorizontal();
            size(dip(80), dip(80));
            margin(0, dip(10), 0, 0);
            alignParentRight();
            alignParentTop();
            layoutGravity(RIGHT);
            textSize(12);
            imageResource(R.drawable.lines);
            onClick((v) -> uiRoot.toggleShowMenu());
            renderable.view();
        });

    }
}
