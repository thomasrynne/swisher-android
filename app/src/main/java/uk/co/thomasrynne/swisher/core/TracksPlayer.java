package uk.co.thomasrynne.swisher.core;

public interface TracksPlayer {
    void cueBeginning(); //Called when there is only one group and it finishes
    void stop();
    void pausePlay();
//    void seekTo(int millis);
//    void jumpTo(int millis);
    void clear();

    void jumpTo(int track);
    //public void progress:PlayerProgress = UnknownPlayerProgress
}
