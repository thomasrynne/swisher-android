package thomas.swisher.ui.view;

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
                size(dip(200), dip(200));
                textSize(40);
                layoutGravity(CENTER);
                backgroundResource(buttonTouched ? R.drawable.big_round_button_touched : R.drawable.big_round_button_plain);
                setupBigButton();
                margin(dip(20), dip(20), dip(20), dip(20));
            });

            checkBox(() -> {
                size(dip(50), dip(50));
                text("Play next");
                checked(controls.isPlayNext());
                onCheckedChange(keepPlayingCheckboxListener);
                layoutGravity(LEFT);
            });

            imageButton(() -> {
                centerHorizontal();
                size(dip(80), dip(80));
                margin(0, dip(10), 0, 0);
                alignParentRight();
                alignParentTop();
                layoutGravity(RIGHT);
                textSize(12);
                imageResource(R.drawable.lines);
                onClick((v) -> controls.toggleMenu());
            });
        });
    }

    private void setupBigButton() {
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
