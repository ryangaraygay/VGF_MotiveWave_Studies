package study_examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.motivewave.platform.sdk.common.*;

public class DeltaCalculator implements TickOperation {

    long lowQuantileVolume = 0;
    long highQuantileVolume = 0;
    int deltaMin = 0;
    int deltaMax = 0;      
    int deltaClose = 0;
    int bidsAtHigh = 0;
    int bidsAtPenultimateLow = 0;
    // int bidsAtPenultimateHigh = 0;
    int bidsAtLow = 0;
    int asksAtHigh = 0;
    int asksAtLow = 0;
    int asksAtPenultimateHigh = 0;
    // int asksAtPenultimateLow = 0;

    private float _highQuantilePrice = 0;
    private float _lowQuantilePrice = 0;
    private float _high = 0;
    private float _low = 0;
    private double _minTickSize = 0;

    public DeltaCalculator(float hqPrice, float lqPrice, float high, float low, double minTickSize) {
      _highQuantilePrice = hqPrice;
      _lowQuantilePrice = lqPrice;
      _high = high;
      _low = low;
      _minTickSize = minTickSize;
      _priceVolume = new ConcurrentHashMap<>();
    }

    public Map<Float, VolumeInfo> _priceVolume;

    private class VolumeInfo {
      public int askVolume = 0;
      public int bidVolume = 0;
    }

    enum VolumeSequence {
      Bullish,
      Bearish,
      None
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
            return Integer.compare((a.getValue().askVolume + a.getValue().bidVolume), (b.getValue().askVolume + b.getValue().bidVolume));
          }
        }).getKey();
      } 

      return 0;
    }

    @Override
    public void onTick(Tick t) {
      int tv = t.getVolume();
      boolean isAsk = t.isAskTick();
      deltaClose += tv * (isAsk ? 1 : -1);
      highQuantileVolume += t.getPrice() > _highQuantilePrice ? tv : 0;
      lowQuantileVolume += t.getPrice() < _lowQuantilePrice ? tv : 0;
      deltaMin = Math.min(deltaClose, deltaMin);      
      deltaMax = Math.max(deltaClose, deltaMax);
      bidsAtHigh += t.getPrice() == _high && !isAsk ? tv : 0;
      bidsAtLow += t.getPrice() == _low && !isAsk ? tv : 0;
      bidsAtPenultimateLow += t.getPrice() == (_low + _minTickSize) && !isAsk ? tv : 0;
      // bidsAtPenultimateHigh += t.getPrice() == (_high - _minTickSize) && !isAsk ? tv : 0;
      asksAtHigh += t.getPrice() == _high && isAsk ? tv : 0;
      asksAtPenultimateHigh += t.getPrice() == (_high - _minTickSize) && isAsk ? tv : 0; 
      // asksAtPenultimateLow += t.getPrice() == (_low + _minTickSize) && isAsk ? tv : 0; 
      asksAtLow += t.getPrice() == _low && isAsk ? tv : 0; 

      float priceLevel = t.getPrice();
      VolumeInfo vi = _priceVolume.getOrDefault(priceLevel, new VolumeInfo());
      if (isAsk) {
        vi.askVolume += tv;
      } else {
        vi.bidVolume += tv;
      }

      _priceVolume.put(priceLevel, vi);
    }
  }
