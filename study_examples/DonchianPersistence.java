package study_examples;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.study.*;
import java.awt.*;

@StudyHeader(
    namespace="com.motivewave",
    id="DONCHIAN_PERSISTENCE",
    rb="com.motivewave.platform.study.nls.strings",
    name="Donchian Persistence",
    desc="Equivalent study of the TradingView Donchian Persistence indicator",
    menu="VGF Studies",
    overlay=false,
    requiresVolume=false,
    studyOverlay=false
)
public class DonchianPersistence extends Study
{
    enum Names { DCPERS, MAXBARS }
    enum Values { HIGHEST, LOWEST, PERCENTAGE, LAST_SIGNAL, DCPERS }

    final static String LENGTH = "length";
    final static String UP_COLOR = "upColor";
    final static String DOWN_COLOR = "downColor";

    @Override
    public void initialize(Defaults defaults)
    {
        var sd = createSD();
        var tab = sd.addTab(get("TAB_GENERAL"));

        var inputs = tab.addGroup(get("INPUTS"));
        inputs.addRow(new IntegerDescriptor(Names.MAXBARS.toString(), "Limit to Last N Bars", 60, 1, 10000, 1));
        inputs.addRow(new IntegerDescriptor(LENGTH, get("Length"), 34, 1, 9999, 1));
        inputs.addRow(new ColorDescriptor(UP_COLOR, get("Up Color"), defaults.getGreen()));
        inputs.addRow(new ColorDescriptor(DOWN_COLOR, get("Down Color"), defaults.getRed()));

        sd.addQuickSettings(LENGTH, UP_COLOR, DOWN_COLOR, Names.MAXBARS.toString());

        var desc = createRD();
        desc.setLabelSettings(LENGTH);
        desc.declareBars(Values.DCPERS, Names.DCPERS.toString());
        desc.setFixedTopValue(100);
        desc.setFixedBottomValue(-100);
    }

    @Override
    protected void calculate(int index, DataContext ctx)
    {
        DataSeries series = ctx.getDataSeries();
        int seriesSize = series.size();
        if (seriesSize < 2) return;
        if (!series.isBarComplete(index)) return;
    
        int barLimit = getSettings().getInteger(Names.MAXBARS.toString());
        int startingIndex = seriesSize - barLimit - 1;
        if (index < startingIndex) return;
    
        int length = getSettings().getInteger(LENGTH, 34);
        Color upColor = getSettings().getColor(UP_COLOR);
        Color downColor = getSettings().getColor(DOWN_COLOR);

        double currentHigh = series.getHigh(index);
        double currentLow = series.getLow(index);
        double close = series.getClose(index);
        double prevHighest = series.highest(index-1, length, Enums.BarInput.HIGH);
        double prevLowest = series.lowest(index-1, length, Enums.BarInput.LOW);
        double highest = Math.max(currentHigh, prevHighest);
        double lowest = Math.min(currentLow, prevLowest);

        double range = highest - lowest;
        double relativePosition = (close - lowest) / range;
        double percentage = relativePosition * 100;

        int lastSignalType = series.getInt(index - 1, Values.DCPERS);
        if (highest > prevHighest) {
            lastSignalType = 50;
        } else if (lowest < prevLowest) {
            lastSignalType = -50;
        }

        Color indColor = (lastSignalType > 0) ? blendColors(Color.WHITE, upColor, percentage / 100.0) : blendColors(downColor, Color.WHITE, percentage / 100.0);

        series.setInt(index, Values.DCPERS, lastSignalType);
        series.setBarColor(index, Values.DCPERS, indColor);
        series.setComplete(index);
    }

    private Color blendColors(Color c1, Color c2, double ratio) {
        int red = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int green = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int blue = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(red, green, blue);
    }
        
    private void debug(long millisTime, Object... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(Util.formatMMDDYYYYHHMMSS(millisTime));//, TimeZone.getTimeZone("US/Pacific")));
        sb.append(" ");
        for (int i = 0; i < args.length; i++) {
        sb.append(args[i]);
        sb.append(" ");
        }

        debug(sb.toString());
    }
}
