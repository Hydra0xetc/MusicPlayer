package com.music.player;

import java.util.List;

public interface ScanResultHandler {
    public void onScanComplete(List<MusicFile> files);
    public void onScanError(String error);
}
