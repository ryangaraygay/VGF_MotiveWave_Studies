package study_examples;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import com.motivewave.platform.sdk.common.*;

public class DeltaCloseCalculator implements TickOperation {
    
    private final int intervalMinutes;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMddyyyyHHmm");
    ConcurrentHashMap<String, Integer> _minuteDeltaClose = new ConcurrentHashMap<>();

    public DeltaCloseCalculator(int intervalMinutes) {
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

    public int getDeltaClose(long t) {
    return _minuteDeltaClose.getOrDefault(keyFormat(t), 0);
    }

    public void remove(long t) {
        _minuteDeltaClose.remove(keyFormat(t));
    }

    @Override
    public void onTick(Tick t) {
        String t1 = keyFormat(t.getTime());
        _minuteDeltaClose.put(t1, _minuteDeltaClose.getOrDefault(t1, 0) + (t.getVolume() * (t.isAskTick() ? 1 : -1)));
    }
}