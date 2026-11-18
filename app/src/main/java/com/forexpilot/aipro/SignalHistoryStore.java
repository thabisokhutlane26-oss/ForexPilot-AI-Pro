package com.forexpilot.aipro;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SignalHistoryStore {

    private static final String PREFS = "forexpilot_signal_history";
    private static final String KEY = "signals";
    private static final int MAX_RECORDS = 200;

    private final SharedPreferences preferences;

    public SignalHistoryStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void save(Signal signal) {
        if (signal == null) return;

        JSONArray records = readRecords();

        JSONObject item = new JSONObject();

        try {
            item.put("symbol", signal.getSymbol());
            item.put("timeframe", signal.getTimeframe());
            item.put("direction", signal.getDirection().name());
            item.put("entry", signal.getEntry());
            item.put("stopLoss", signal.getStopLoss());
            item.put("takeProfit1", signal.getTakeProfit1());
            item.put("takeProfit2", signal.getTakeProfit2());
            item.put("takeProfit3", signal.getTakeProfit3());
            item.put("trend", signal.getTrend());
            item.put("timestamp", signal.getTimestamp());

            JSONArray updated = new JSONArray();
            updated.put(item);

            for (int i = 0;
                 i < records.length() && i < MAX_RECORDS - 1; i++) {
                updated.put(records.getJSONObject(i));
            }

            preferences.edit()
                    .putString(KEY, updated.toString())
                    .apply();

        } catch (Exception ignored) {
        }
    }

    public synchronized List<JSONObject> getAll() {
        JSONArray records = readRecords();
        List<JSONObject> result = new ArrayList<>();

        for (int i = 0; i < records.length(); i++) {
            JSONObject item = records.optJSONObject(i);
            if (item != null) result.add(item);
        }

        return result;
    }

    public synchronized void clear() {
        preferences.edit().remove(KEY).apply();
    }

    public synchronized int size() {
        return readRecords().length();
    }

    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "--";

        SimpleDateFormat format = new SimpleDateFormat(
                "dd MMM yyyy • HH:mm",
                Locale.getDefault()
        );

        return format.format(new Date(timestamp));
    }

    private JSONArray readRecords() {
        String saved = preferences.getString(KEY, "[]");

        try {
            return new JSONArray(saved);
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}