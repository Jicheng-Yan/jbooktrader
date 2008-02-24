package com.jbooktrader.platform.chart;


import com.jbooktrader.platform.marketdepth.*;
import org.jfree.chart.axis.SegmentedTimeline;

import java.util.*;


public class MarketTimeLine {
    /**
     * Gaps less than MAX_GAP will be ignored, gaps greater than MAX_GAP will be removed
     */
    private static final long MAX_GAP = 12 * 60 * 60 * 1000;// 12 hours
    private static final long SEGMENT_SIZE = SegmentedTimeline.FIFTEEN_MINUTE_SEGMENT_SIZE;
    private static final long GAP_BUFFER = SEGMENT_SIZE;
    private final MarketBook marketBook;

    public MarketTimeLine(MarketBook marketBook) {
        this.marketBook = marketBook;
    }


    public SegmentedTimeline getNormalHours() {
        SegmentedTimeline timeline = new SegmentedTimeline(SEGMENT_SIZE, 1, 0);
        long previousTime = marketBook.getFirstMarketDepth().getTime();

        // make a defensive copy to prevent concurrent modification
        List<MarketDepth> marketDepths = new ArrayList<MarketDepth>();
        marketDepths.addAll(marketBook.getAll());

        for (MarketDepth marketDepth : marketDepths) {
            long time = marketDepth.getTime();
            long difference = time - previousTime;
            if (difference > MAX_GAP) {
                timeline.addException(previousTime + GAP_BUFFER, time - GAP_BUFFER);
            }
            previousTime = time;
        }

        return timeline;
    }


    public SegmentedTimeline getAllHours() {
        return new SegmentedTimeline(SegmentedTimeline.DAY_SEGMENT_SIZE, 7, 0);
    }
}