package com.forexpilot.aipro;

public final class AppConfig {

    private AppConfig() {
        // Prevent creating this class
    }

    public static String getOandaAccessToken() {
        return BuildConfig.OANDA_ACCESS_TOKEN;
    }
}
