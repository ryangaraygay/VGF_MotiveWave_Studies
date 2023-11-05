package study_examples;

import java.util.Collections;
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
    int bidsAtLow = 0;
    int asksAtHigh = 0;
    int asksAtLow = 0;
    int asksAtPenultimateHigh = 0;

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

    Map<Float, Integer> _priceVolume;

    public float getPOC() {
      if (_priceVolume.size() > 0) {
        return Collections.max(_priceVolume.entrySet(), Map.Entry.comparingByValue()).getKey();
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
      asksAtHigh += t.getPrice() == _high && isAsk ? tv : 0;
      asksAtPenultimateHigh += t.getPrice() == (_high - _minTickSize) && isAsk ? tv : 0; 
      asksAtLow += t.getPrice() == _low && isAsk ? tv : 0; 

      float priceLevel = t.getPrice();
      _priceVolume.put(priceLevel, _priceVolume.getOrDefault(priceLevel, 0) + tv);
    }
  }
