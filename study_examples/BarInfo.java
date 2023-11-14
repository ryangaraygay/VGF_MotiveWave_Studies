package study_examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BarInfo {
    
    int deltaMin = 0;
    int deltaMax = 0;      
    int deltaClose = 0;

    public Map<Float, VolumeInfo> _priceVolume;

    public BarInfo() {
      _priceVolume = new ConcurrentHashMap<>();
    }

    public float getDeltaRange() {
      return deltaMax - deltaMin;
    }

    public int getCloseRangePerc() {
      return (int)(deltaClose * 100 / (getDeltaRange()));
    }

    public VolumeSequence hasVolumeSequence(int requiredIncreasingAsk, int requiredDecreasingBid) {
      int maxIncreasingAskCount = 0;
      int maxDecreasingBidCount = 0;
      int increasingAskCount = 0;
      int decreasingBidCount = 0;
      int currentAskVolume = 0;
      int currentBidVolume = 0;
      int index = 0;

      List<Map.Entry<Float, VolumeInfo>> list = new ArrayList<>(_priceVolume.entrySet());
      list.sort(Map.Entry.comparingByKey());

      for (Map.Entry<Float, VolumeInfo> entry : list) {
        if (index == 0) {
          index++;
        }

        else {
          if (entry.getValue().askVolume > currentAskVolume) {
            increasingAskCount++;
          } else {
            if (increasingAskCount > maxIncreasingAskCount) {
              maxIncreasingAskCount = increasingAskCount;
            }

            increasingAskCount = 0;
          }
          
          if (entry.getValue().bidVolume < currentBidVolume) {
            decreasingBidCount++;
          } else {
            if (decreasingBidCount > maxDecreasingBidCount) {
              maxDecreasingBidCount = decreasingBidCount;
            }

            decreasingBidCount = 0;
          }
        }

        currentAskVolume = entry.getValue().askVolume;
        currentBidVolume = entry.getValue().bidVolume;
      }

      maxIncreasingAskCount = Math.max(maxIncreasingAskCount, increasingAskCount);
      maxDecreasingBidCount = Math.max(maxDecreasingBidCount, decreasingBidCount);

      if (maxIncreasingAskCount >= requiredIncreasingAsk) return VolumeSequence.Bullish;
      if (maxDecreasingBidCount >= requiredDecreasingBid) return VolumeSequence.Bearish;
      return VolumeSequence.None;
    }

    public float getPOC() {
      if (_priceVolume.size() > 0) {
        return Collections.max(_priceVolume.entrySet(), new Comparator<Map.Entry<Float, VolumeInfo>>() {
          @Override
          public int compare(final Map.Entry<Float, VolumeInfo> a, final Map.Entry<Float, VolumeInfo> b) {
            return Integer.compare(a.getValue().getTotalVolume(), b.getValue().getTotalVolume());
          }
        }).getKey();
      } 

      return 0;
    }

    public int getAboveVolume(float _highQuantilePrice) {
      return _priceVolume.entrySet().stream()
      .filter((s) -> s.getKey() > _highQuantilePrice)
      .mapToInt(x -> x.getValue().getTotalVolume())
      .sum();
    }

    public int getBelowVolume(float _lowQuantilePrice) {
      return _priceVolume.entrySet().stream()
      .filter((s) -> s.getKey() < _lowQuantilePrice)
      .mapToInt(x -> x.getValue().getTotalVolume())
      .sum();
    }

    public int getBidVolume(float price) {
      VolumeInfo vi = _priceVolume.get(price);
      return vi == null ? 0 : vi.bidVolume;
    }

    public int getAskVolume(float price) {
      VolumeInfo vi = _priceVolume.get(price);
      return vi == null ? 0 : vi.askVolume;
    }
}
