package com.melonbar.core.postprocessing;

import com.melonbar.core.http.response.PostProcessor;
import com.melonbar.core.model.Candle;
import com.melonbar.core.model.CandlesRange;

import java.util.Arrays;

public final class Candles {

    private static final String CANDLES_DELIMITER = "(?<=],)";

    public static PostProcessor<Candle[]> getCandles() {
        return response -> Arrays.stream(parseCandles(response.content()))
                .map(Candle::from)
                .toArray(Candle[]::new);
    }

    public static PostProcessor<CandlesRange> getCandlesRange() {
        return response -> getCandles()
                .andThen(CandlesRange::new)
                .apply(response);
    }

    private static String[] parseCandles(final String body) {
        return body.substring(1, body.length()-1).split(CANDLES_DELIMITER);
    }
}
