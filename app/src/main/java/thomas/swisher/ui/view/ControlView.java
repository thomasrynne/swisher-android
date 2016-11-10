package thomas.swisher.ui.view;

import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import thomas.swisher.R;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.model.UIControls;

import static trikita.anvil.BaseDSL.*;
import static trikita.anvil.DSL.*;

public class ControlView {

    private UIControls.Core controls;
    public ControlView(UIControls.Core controls) {
        this.controls = controls;
    }

    private boolean buttonTouched = false;

    private static final int BUTTON = 1;
    private static final int BAR = 2;
    private static final int DURATION = 3;


    //Lambdas should replace this but there was confusion about which interface to implement
    private final CompoundButton.OnCheckedChangeListener keepPlayingCheckboxListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            controls.sendAutoPlayNext(isChecked);
        }
    };

    private final SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int to, boolean user) {
            if (user) {
                controls.seekTo(to);
            }
        }
        public void onStartTrackingTouch(SeekBar seekBar) { } // see what happens when slowly updating
        public void onStopTrackingTouch(SeekBar seekBar) { }  // seek. does incremental update make position jump?
    };

    public void view() {
        relativeLayout(() -> {
            size(FILL, FILL);
            gravity(TOP);
            button(() -> {
                id(BUTTON);
                centerHorizontal();
                size(dip(200), dip(200));
                textSize(40);
                layoutGravity(CENTER);
                backgroundResource(buttonTouched ? R.drawable.big_round_button_touched : R.drawable.big_round_button_plain);
                setupBigButton();
                margin(dip(20), dip(20), dip(20), dip(20));
            });

            checkBox(() -> {
                size(WRAP, WRAP);
                textSize(20);
                text("Play next");
                checked(controls.isPlayNext());
                onCheckedChange(keepPlayingCheckboxListener);
                layoutGravity(LEFT);
            });

            MenuToggle.button(controls.uiRoot(), () -> visibility(!controls.uiRoot().showMenu()));

            imageButton(() -> {
                centerHorizontal();
                size(dip(50), dip(50));
                margin(dip(10), 0, dip(10), 0);
                alignParentRight();
                textSize(12);
                toRightOf(BUTTON);
                above(BAR);
                imageResource(R.drawable.fullscreen);
                onClick((v) -> controls.toFullScreen());
            });

            Core.PlayerProgress progress = controls.progress();

            textView(() -> {
                id(DURATION);
                size(WRAP, WRAP);
                below(BUTTON);
                textSize(20);
                margin(dip(10));
                visibility(progress.enabled ? View.VISIBLE : View.INVISIBLE);
                text(controls.duration());
            });

            seekBar(() -> {
                id(BAR);
                size(FILL, WRAP);
                below(BUTTON);
                toRightOf(DURATION);
                margin(0, dip(10), 0, dip(10));
                visibility(progress.enabled ? View.VISIBLE : View.INVISIBLE);
                max(progress.totalMillis);
                progress(progress.progressMillis);
                onSeekBarChange(seekListener);
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
