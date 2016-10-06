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
            controls.updatePlayNext(isChecked);
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

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.controls_fragment, container, false);
//
//        ImageButton createCardsButton = (ImageButton)view.findViewById(R.id.buttonCreateCards);
//        createCardsButton.setOnClickListener(new View.OnClickListener() {
//			@Override public void onClick(View view) {
//                //((MainActivity)getActivity()).toggleCreatePanel();
//			}
//        });
//
//        view.findViewById(R.id.btnFullScreen).setOnTouchListener(new View.OnTouchListener() {
//            @Override public boolean onTouch(View view, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    eventBus.post(new Events.GoToFullScreenEvent());
//                }
//                return true;
//            }
//        });
//
//        progressBar = (SeekBar)view.findViewById(R.id.progressBar);
//        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            public void onProgressChanged(SeekBar seekBar, int to, boolean user) {
//                if (user) {
//                    eventBus.post(new Events.SeekToEvent(to));
//                }
//            }
//            public void onStartTrackingTouch(SeekBar seekBar) { }
//            public void onStopTrackingTouch(SeekBar seekBar) { }
//        });
//        durationText = (TextView)view.findViewById(R.id.durationText);
//        durationText.requestFocus();
//        durationText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean hasFocus) {
//                if (!hasFocus) durationText.requestFocus();
//            }
//        });
//        //progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        buttonStopStart = (Button)view.findViewById(R.id.btnStop);
//        buttonStopStart.setOnTouchListener(new View.OnTouchListener() {
//            @Override public boolean onTouch(View view, MotionEvent event) {
//                int action = event.getAction();
//                if (action == MotionEvent.ACTION_DOWN) {
//                    postAction("pause");
//                    view.setBackgroundResource(R.drawable.big_round_button_touched);
//                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
//                    view.setBackgroundResource(R.drawable.big_round_button_plain);
//                }
//                if (action == MotionEvent.ACTION_UP) {
//                }
//                return true;
//            }
//        });
//        return view;
//    }
//
//    private boolean isPlaying = false;
//    private long lastRealProgressUpdateTime;
//    private int lastRealProgress;
//    private Runnable updateProgress = new Runnable() { public void run() {
//        if (isPlaying) {
//            long timeSinceLastRealUpdate = System.currentTimeMillis() - lastRealProgressUpdateTime;
//            progressBar.setProgress(lastRealProgress + (int)timeSinceLastRealUpdate);
//            progressBar.postDelayed(this, 500);
//        }
//    } };
//
//    private void updateProgress(Events.ProgressStatus status) {
//        if (status != null) { isPlaying = status.progress.isPlaying; }
//        if (status != null && status.progress.isKnown) {
//            if (status.progress.isPlaying) {
//                lastRealProgressUpdateTime = System.currentTimeMillis();
//                lastRealProgress = status.progress.progress;
//                progressBar.postDelayed(updateProgress, 500);
//            }
//            progressBar.setVisibility(View.VISIBLE);
//            progressBar.setMax(status.progress.total);
//            progressBar.setProgress(status.progress.progress);
//            durationText.setVisibility(View.VISIBLE);
//            int totalSeconds = status.progress.total / 1000;
//            int minutes = totalSeconds / 60;
//            int seconds = totalSeconds % 60;
//            durationText.setText( minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
//        } else {
//            progressBar.setVisibility(View.INVISIBLE);
//            durationText.setVisibility(View.INVISIBLE);
//        }
//    }
//
//    private void postAction(String action) {
//        try {
//            JSONObject json = new JSONObject();
//            json.put("action", action);
//            eventBus.post(new Events.DoEvent(json));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void updateBigButton(Events.BigButtonStatus status) {
//        if (status == null) status = new Events.BigButtonStatus(Events.BigButtonStatus.Status.Off);
//        buttonStopStart.setEnabled(status.status != Events.BigButtonStatus.Status.Off);
//        if (status.status == Events.BigButtonStatus.Status.Pause) {
//            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        } else {
//            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        }
//        String text = "";
//        switch (status.status) {
//            case Pause: text = "||"; break;
//            case Play: text = "\u25B6"; break;
//            case Stop: text = "\u25FC"; break;
//        }
//        buttonStopStart.setText(text);
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        updateBigButton(eventBus.getStickyEvent(Events.BigButtonStatus.class));
//        updateProgress(eventBus.getStickyEvent(Events.ProgressStatus.class));
//        eventBus.register(listener);
//        durationText.requestFocus();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        eventBus.unregister(listener);
//    }
}
