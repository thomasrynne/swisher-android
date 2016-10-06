package thomas.swisher.service.player;

public interface TracksPlayer {
    void cueBeginning(); //Called when there is only one group and it finishes
    void stop();
    void pausePlay();
    void clear();
    void playNext(boolean play);
    void jumpTo(int track);
}
