package study_examples;

import java.util.concurrent.ConcurrentHashMap;
import com.motivewave.platform.sdk.common.*;

public class DeltaCalculator implements TickOperation {

    ConcurrentHashMap<String, BarInfo> _minuteBarInfo = new ConcurrentHashMap<>();

    private String keyFormat(long t) {
      return Util.formatMMDDYYYYHHMM(t); // todo maybe adopt so it can be with non-1min chart, there are assumptions here about 1-min chart for efficiency
    }
    
    public void remove(long t) {
      _minuteBarInfo.remove(keyFormat(t));
    }
    
    public BarInfo getBarInfo(long t) {
      return _minuteBarInfo.get(keyFormat(t));
    }
  
    @Override
    public void onTick(Tick t) {
      String t1 = keyFormat(t.getTime());
      BarInfo bi = _minuteBarInfo.getOrDefault(t1, new BarInfo());

      int tv = t.getVolume();
      boolean isAsk = t.isAskTick();
      
      bi.deltaClose += tv * (isAsk ? 1 : -1);
      bi.deltaMin = Math.min(bi.deltaClose, bi.deltaMin);      
      bi.deltaMax = Math.max(bi.deltaClose, bi.deltaMax);

      float priceLevel = t.getPrice();
      VolumeInfo vi = bi._priceVolume.getOrDefault(priceLevel, new VolumeInfo());
      if (isAsk) {
        vi.askVolume += tv;
      } else {
        vi.bidVolume += tv;
      }

      bi._priceVolume.put(priceLevel, vi);

      _minuteBarInfo.put(t1, bi);
    }
  }
