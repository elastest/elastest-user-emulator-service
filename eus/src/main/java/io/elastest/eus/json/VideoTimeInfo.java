package io.elastest.eus.json;

public class VideoTimeInfo {
    private long startTime;
    private long videoDuration;

    public VideoTimeInfo() {
    }

    public VideoTimeInfo(long startTime, long videoDuration) {
        this.startTime = startTime;
        this.videoDuration = videoDuration;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(long videoDuration) {
        this.videoDuration = videoDuration;
    }

}