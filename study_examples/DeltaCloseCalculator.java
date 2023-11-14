package study_examples;

import java.util.concurrent.ConcurrentHashMap;
import com.motivewave.platform.sdk.common.*;

public class DeltaCloseCalculator implements TickOperation {
    ConcurrentHashMap<String, Integer> _minuteDeltaClose = new ConcurrentHashMap<>();

    private String keyFormat(long t) {
    return Util.formatMMDDYYYYHHMM(t); // todo maybe adopt so it can be with non-1min chart, there are assumptions here about 1-min chart for efficiency
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