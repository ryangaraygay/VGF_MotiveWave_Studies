package study_examples;

public class VolumeInfo {
    public int askVolume = 0;
    public int bidVolume = 0;
    public int getTotalVolume() {
    return askVolume + bidVolume;
    }
}
