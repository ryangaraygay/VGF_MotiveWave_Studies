package study_examples;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import com.motivewave.platform.sdk.common.*;

public class DeltaCalculator implements TickOperation {

    private final int intervalMinutes;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMddyyyyHHmm");
    ConcurrentHashMap<String, BarInfo> _minuteBarInfo = new ConcurrentHashMap<>();

    public DeltaCalculator(int intervalMinutes) {
      if (intervalMinutes <= 0) {
          throw new IllegalArgumentException("Interval minutes must be greater than zero.");
      }
      this.intervalMinutes = intervalMinutes;
    }

    private String keyFormat(long t) {
      LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneOffset.systemDefault());
      int minute = ldt.getMinute();
      int startMinute = minute - (minute % intervalMinutes);
      LocalDateTime fiveMinuteStart = ldt.withMinute(startMinute).withSecond(0).withNano(0);
      return FORMATTER.format(fiveMinuteStart);
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
