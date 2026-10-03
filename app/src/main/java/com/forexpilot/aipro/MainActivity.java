package com.forexpilot.aipro;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SignalRepository signalRepository;

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildInterface();
        connectToMarketData();
    }

    private void buildInterface() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setGravity(
                Gravity.CENTER
        );

        layout.setPadding(
                40,
                40,
                40,
                40
        );

        layout.setBackgroundColor(
                Color.rgb(10, 15, 25)
        );

        TextView title =
                new TextView(this);

        title.setText(
                "ForexPilot AI Pro"
        );

        title.setTextColor(
                Color.WHITE
        );

        title.setTextSize(28);

        title.setGravity(
                Gravity.CENTER
        );

        statusText =
                new TextView(this);

        statusText.setText(
                "LIVE MARKET ENGINE\nConnecting..."
        );

        statusText.setTextColor(
                Color.LTGRAY
        );

        statusText.setTextSize(16);

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                0,
                30,
                0,
                0
        );

        layout.addView(title);
        layout.addView(statusText);

        setContentView(layout);
    }

    private void connectToMarketData() {

        String apiKey =
                AppConfig.getTwelveDataApiKey();

        MarketDataProvider provider =
                new TwelveDataProvider(
                        apiKey
                );

        signalRepository =
                new SignalRepository(
                        provider,
                        new SignalRepository.SignalCallback() {

                            @Override
                            public void onSignal(
                                    Signal signal
                            ) {

                                runOnUiThread(() -> {

                                    statusText.setText(
                                            "TWELVE DATA CONNECTED\n"
                                                    + signal.getSymbol()
                                                    + " • "
                                                    + signal.getTimeframe()
                                                    + "\n"
                                                    + signal.getDirection()
                                    );
                                });
                            }

                            @Override
                            public void onError(
                                    String message
                            ) {

                                runOnUiThread(() -> {

                                    statusText.setText(
                                            "MARKET DATA STATUS\n"
                                                    + message
                                    );
                                });
                            }
                        }
                );

        signalRepository.requestSignal(
                "EUR/USD",
                "15M"
        );
    }

    @Override
    protected void onDestroy() {

        if (signalRepository != null) {
            signalRepository.stop();
        }

        super.onDestroy();
    }
}