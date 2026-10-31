package com.tokenmonitor.sw2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sonyericsson.extras.liveware.aef.control.Control;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView mTvNetworkEndpoint;

    // 手机面板数据控件
    private TextView mTvPhoneModel;
    private TextView mTvPhoneTokens;
    private TextView mTvPhoneInput;
    private TextView mTvPhoneOutput;
    private TextView mTvPhoneCache;
    private TextView mTvPhoneCost;

    // 手表双模预览
    private ImageView mIvPreviewAod;
    private ImageView mIvPreviewActive;
    private TextView mTvActivePreviewLabel;

    // 主题卡片
    private LinearLayout mCardM3;
    private LinearLayout mCardCyber;
    private LinearLayout mCardSynthwave;
    private LinearLayout mCardAurora;
    private LinearLayout mCardMono;

    private TextView mTvTitleM3;
    private TextView mTvTitleCyber;
    private TextView mTvTitleSynthwave;
    private TextView mTvTitleAurora;
    private TextView mTvTitleMono;

    private Button mBtnPushWatch;
    private Button mBtnToggleConfig;
    private LinearLayout mLayoutConfig;

    private EditText mEtApiUrl;
    private EditText mEtApiKey;
    private EditText mEtModelName;
    private TextView mTvStatus;

    private SharedPreferences mPrefs;
    private int mSelectedTheme = WatchRenderer.THEME_M3;
    private boolean mIsReceiverRegistered = false;

    // 监听全局内部广播，一旦电脑推送过来，手机主界面秒级联动刷新
    private final BroadcastReceiver mDataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshPhonePanelAndPreviews();
            Toast.makeText(MainActivity.this, "收到来自电脑的最新 Token 数据！", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences("token_prefs", Context.MODE_PRIVATE);
        mSelectedTheme = mPrefs.getInt("active_theme", WatchRenderer.THEME_M3);

        initViews();
        updateThemeSelectionUI();
        refreshPhonePanelAndPreviews();

        LocalHttpServer.getInstance(this).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mIsReceiverRegistered) {
            IntentFilter filter = new IntentFilter(LocalHttpServer.ACTION_TOKEN_DATA_UPDATED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mDataUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mDataUpdateReceiver, filter);
            }
            mIsReceiverRegistered = true;
        }
        refreshPhonePanelAndPreviews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsReceiverRegistered) {
            try {
                unregisterReceiver(mDataUpdateReceiver);
            } catch (Exception ignored) {}
            mIsReceiverRegistered = false;
        }
    }

    private void initViews() {
        mTvNetworkEndpoint = findViewById(R.id.tv_network_endpoint);
        String localIp = TokenExtensionService.getLocalIpAddress(this);
        mTvNetworkEndpoint.setText("接收端点: http://" + localIp + ":8888/sync");

        mTvPhoneModel = findViewById(R.id.tv_phone_model);
        mTvPhoneTokens = findViewById(R.id.tv_phone_tokens);
        mTvPhoneInput = findViewById(R.id.tv_phone_input);
        mTvPhoneOutput = findViewById(R.id.tv_phone_output);
        mTvPhoneCache = findViewById(R.id.tv_phone_cache);
        mTvPhoneCost = findViewById(R.id.tv_phone_cost);

        mIvPreviewAod = findViewById(R.id.iv_preview_aod);
        mIvPreviewActive = findViewById(R.id.iv_preview_active);
        mTvActivePreviewLabel = findViewById(R.id.tv_active_preview_label);

        mCardCyber = findViewById(R.id.theme_card_cyber);
        mCardSynthwave = findViewById(R.id.theme_card_synthwave);
        mCardAurora = findViewById(R.id.theme_card_aurora);
        mCardMono = findViewById(R.id.theme_card_mono);

        mTvTitleCyber = findViewById(R.id.tv_title_cyber);
        mTvTitleSynthwave = findViewById(R.id.tv_title_synthwave);
        mTvTitleAurora = findViewById(R.id.tv_title_aurora);
        mTvTitleMono = findViewById(R.id.tv_title_mono);

        mBtnPushWatch = findViewById(R.id.btn_push_watch);
        mBtnToggleConfig = findViewById(R.id.btn_toggle_config);
        mLayoutConfig = findViewById(R.id.layout_config);

        mEtApiUrl = findViewById(R.id.et_api_url);
        mEtApiKey = findViewById(R.id.et_api_key);
        mEtModelName = findViewById(R.id.et_model_name);
        mTvStatus = findViewById(R.id.tv_status);

        mEtApiUrl.setText(mPrefs.getString("api_url", "https://api.openai.com/v1"));
        mEtApiKey.setText(mPrefs.getString("api_key", ""));
        mEtModelName.setText(mPrefs.getString("model_name", "gemini-3.8-flash"));

        mCardCyber.setOnClickListener(v -> selectTheme(WatchRenderer.THEME_CYBER));
        mCardSynthwave.setOnClickListener(v -> selectTheme(WatchRenderer.THEME_SYNTHWAVE));
        mCardAurora.setOnClickListener(v -> selectTheme(WatchRenderer.THEME_AURORA));
        mCardMono.setOnClickListener(v -> selectTheme(WatchRenderer.THEME_MONO));

        mBtnPushWatch.setOnClickListener(v -> pushCurrentToWatch());

        mBtnToggleConfig.setOnClickListener(v -> {
            if (mLayoutConfig.getVisibility() == View.VISIBLE) {
                mLayoutConfig.setVisibility(View.GONE);
                mBtnToggleConfig.setText("⚙️ 配置在线 API (可选，留空走局域网同步)");
            } else {
                mLayoutConfig.setVisibility(View.VISIBLE);
                mBtnToggleConfig.setText("▲ 收起配置面板");
            }
        });

        findViewById(R.id.btn_save_config).setOnClickListener(v -> {
            String apiKey = mEtApiKey.getText().toString().trim();
            mPrefs.edit()
                    .putString("api_url", mEtApiUrl.getText().toString().trim())
                    .putString("api_key", apiKey)
                    .putString("model_name", mEtModelName.getText().toString().trim())
                    .apply();

            Toast.makeText(this, "配置已保存！", Toast.LENGTH_SHORT).show();
            refreshPhonePanelAndPreviews();
            pushCurrentToWatch();
        });
    }

    private void selectTheme(int theme) {
        mSelectedTheme = theme;
        mPrefs.edit().putInt("active_theme", mSelectedTheme).apply();
        updateThemeSelectionUI();
        refreshPhonePanelAndPreviews();

        TokenControlExtension control = TokenExtensionService.getCurrentControl();
        if (control != null) {
            control.updateTheme(mSelectedTheme);
        }
    }

    private void updateThemeSelectionUI() {
        mCardCyber.setBackgroundResource(R.drawable.bg_theme_unselected);
        mCardSynthwave.setBackgroundResource(R.drawable.bg_theme_unselected);
        mCardAurora.setBackgroundResource(R.drawable.bg_theme_unselected);
        mCardMono.setBackgroundResource(R.drawable.bg_theme_unselected);

        mTvTitleCyber.setText("赛博霓虹 (Cyber Neon)");
        mTvTitleCyber.setTextColor(0xFFFFFFFF);
        mTvTitleSynthwave.setText("幻境霓裳 (Synthwave Purple)");
        mTvTitleSynthwave.setTextColor(0xFFFFFFFF);
        mTvTitleAurora.setText("极光翡翠 (Matrix Aurora)");
        mTvTitleAurora.setTextColor(0xFFFFFFFF);
        mTvTitleMono.setText("经典单色 (Classic Monochrome)");
        mTvTitleMono.setTextColor(0xFFFFFFFF);

        if (mSelectedTheme == WatchRenderer.THEME_SYNTHWAVE) {
            mCardSynthwave.setBackgroundResource(R.drawable.bg_theme_selected);
            mTvTitleSynthwave.setText("幻境霓裳 (Synthwave Purple) ✓");
            mTvTitleSynthwave.setTextColor(0xFFFF007F);
            mTvActivePreviewLabel.setText("点亮：幻境霓裳");
            mTvActivePreviewLabel.setTextColor(0xFFFF007F);
        } else if (mSelectedTheme == WatchRenderer.THEME_AURORA) {
            mCardAurora.setBackgroundResource(R.drawable.bg_theme_selected);
            mTvTitleAurora.setText("极光翡翠 (Matrix Aurora) ✓");
            mTvTitleAurora.setTextColor(0xFF10B981);
            mTvActivePreviewLabel.setText("点亮：极光翡翠");
            mTvActivePreviewLabel.setTextColor(0xFF10B981);
        } else if (mSelectedTheme == WatchRenderer.THEME_MONO) {
            mCardMono.setBackgroundResource(R.drawable.bg_theme_selected);
            mTvTitleMono.setText("经典单色 (Classic Monochrome) ✓");
            mTvTitleMono.setTextColor(0xFF00D2FF);
            mTvActivePreviewLabel.setText("点亮：经典单色");
            mTvActivePreviewLabel.setTextColor(0xFFFFFFFF);
        } else {
            mCardCyber.setBackgroundResource(R.drawable.bg_theme_selected);
            mTvTitleCyber.setText("赛博霓虹 (Cyber Neon) ✓");
            mTvTitleCyber.setTextColor(0xFF00D2FF);
            mTvActivePreviewLabel.setText("点亮：赛博霓虹");
            mTvActivePreviewLabel.setTextColor(0xFF00D2FF);
        }
    }

    private void refreshPhonePanelAndPreviews() {
        WatchRenderer.WatchData data = new WatchRenderer.WatchData();
        data.modelName = mPrefs.getString("model_name", "gemini-3.8-flash");
        data.totalTokens = mPrefs.getLong("used_tokens", 3338800);
        data.inputTokens = mPrefs.getLong("input_tokens", 3297293);
        data.outputTokens = mPrefs.getLong("output_tokens", 41532);
        data.cacheReadTokens = mPrefs.getLong("cache_tokens", 33472572);
        data.costUsd = Double.longBitsToDouble(mPrefs.getLong("cost_usd", Double.doubleToLongBits(0.00)));
        data.lastUpdateTime = mPrefs.getLong("last_update_time", System.currentTimeMillis());
        data.localIp = TokenExtensionService.getLocalIpAddress(this);

        mTvPhoneModel.setText(data.modelName);
        mTvPhoneTokens.setText(formatTokens(data.totalTokens));
        mTvPhoneInput.setText(formatCompact(data.inputTokens));
        mTvPhoneOutput.setText(formatCompact(data.outputTokens));
        mTvPhoneCache.setText(formatCompact(data.cacheReadTokens));
        mTvPhoneCost.setText(String.format(Locale.US, "$%.2f", data.costUsd));

        Bitmap bmpAod = WatchRenderer.render(mSelectedTheme, true, data);
        Bitmap bmpActive = WatchRenderer.render(mSelectedTheme, false, data);

        mIvPreviewAod.setImageBitmap(bmpAod);
        mIvPreviewActive.setImageBitmap(bmpActive);

        TokenAppWidgetProvider.updateAllWidgets(this);
    }

    private void pushCurrentToWatch() {
        TokenControlExtension control = TokenExtensionService.getCurrentControl();
        if (control != null) {
            control.renderDashboard();
            Toast.makeText(this, "已即时同步至手表！", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(Control.Intents.CONTROL_START_REQUEST_INTENT);
            intent.putExtra(Control.Intents.EXTRA_AEA_PACKAGE_NAME, getPackageName());
            intent.setPackage("com.sonymobile.smartconnect.smartwatch2");
            sendBroadcast(intent, "com.sonyericsson.extras.liveware.aef.HOSTAPP_PERMISSION");
            Toast.makeText(this, "已唤醒手表并同步！", Toast.LENGTH_SHORT).show();
        }
    }

    private static String formatTokens(long tokens) {
        if (tokens >= 1_000_000) {
            return String.format(Locale.US, "%,.1f M", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format(Locale.US, "%,.1f K", tokens / 1_000.0);
        } else {
            return String.format(Locale.US, "%d", tokens);
        }
    }

    private static String formatCompact(long tokens) {
        if (tokens >= 1_000_000) {
            return String.format(Locale.US, "%.2fM", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format(Locale.US, "%.1fK", tokens / 1_000.0);
        } else {
            return String.valueOf(tokens);
        }
    }
}
