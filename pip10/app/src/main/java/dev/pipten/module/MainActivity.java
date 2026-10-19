package dev.pipten.module;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 44, 36, 36);
        TextView info = new TextView(this);
        info.setTextSize(17);
        info.setText("PiP 10s Controls v0.3.0-alpha\n\n" +
            "Required LSPosed scope: YouTube or Morphe. Force stop and reopen " +
            "YouTube after enabling.\n\n" +
            "For larger 32dp icons, optionally enable com.android.systemui " +
            "in LSPosed scope and reboot. This modifies only detected PiP10 icons. " +
            "It is experimental: if SystemUI becomes unstable, disable its scope.\n\n" +
            "No Notification Access permission is required.\n\n" +
            "If buttons do not work, collect: adb logcat -d -s PiP10:I");
        layout.addView(info);
        setContentView(layout);
    }
}
