package thomas.swisher.ui.view;

import android.util.Log;
import android.view.MotionEvent;
import android.widget.CompoundButton;

import thomas.swisher.R;
import thomas.swisher.ui.model.UIControls;

import static trikita.anvil.BaseDSL.*;
import static trikita.anvil.DSL.*;

public class ControlView {

    private UIControls.Core controls;

    public ControlView(UIControls.Core controls) {
        this.controls = controls;
    }

    private boolean buttonTouched = false;

    //Lambdas should replace this but there was confusion about which interface to implement
    private final CompoundButton.OnCheckedChangeListener keepPlayingCheckboxListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            controls.updateAutoPlayNext(isChecked);
        }
    };
    public void view() {
        relativeLayout(() -> {
            size(FILL, FILL);
            gravity(TOP);
            button(() -> {
                centerHorizontal();
                size(150, 150);
                textSize(40);
                gravity(CENTER);
                backgroundResource(buttonTouched ? R.drawable.big_round_button_touched : R.drawable.big_round_button_plain);
                setupBitButton();
            });

            checkBox(() -> {
                checked(controls.isPlayNext());
                onCheckedChange(keepPlayingCheckboxListener);
            });

            imageButton(() -> {
                centerHorizontal();
                size(40, 40);
                margin(0, 10, 0, 0);
                alignParentRight();
                alignParentTop();
                gravity(RIGHT);
                textSize(12);
                imageResource(R.drawable.lines);
                onClick((v) -> controls.toggleMenu());
            });
        });
    }

    private void setupBitButton() {
        switch (controls.buttonState()) {
            case PAUSE: text("||"); break;
            case PLAY: text("\u25B6"); break;
            case STOP: text("\u25FC"); break;
            case NONE: text(""); break;
        }
        onTouch((view, event) -> {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    controls.pausePlay();
                    buttonTouched = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    buttonTouched = false;
                    break;
            }
            return true;
        });
    }
}
