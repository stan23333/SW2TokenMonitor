package com.tokenmonitor.sw2;

import android.content.ContentValues;
import android.content.Context;

import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

public class TokenRegistrationInformation extends RegistrationInformation {

    final Context mContext;

    protected TokenRegistrationInformation(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        mContext = context;
    }

    @Override
    public int getRequiredControlApiVersion() {
        return 2;
    }

    @Override
    public int getTargetControlApiVersion() {
        return 2;
    }

    @Override
    public int getRequiredSensorApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredNotificationApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredWidgetApiVersion() {
        return 0;
    }

    @Override
    public boolean supportsLowPowerMode() {
        // 关键声明：向 SmartWatch 2 声明本插件支持黑白反显低功耗模式
        return true;
    }

    @Override
    public ContentValues getExtensionRegistrationConfiguration() {
        String iconHostapp = ExtensionUtils.getUriString(mContext, R.mipmap.ic_launcher);
        String iconExtension = ExtensionUtils.getUriString(mContext, R.drawable.icon_extension);
        String iconExtension48 = ExtensionUtils.getUriString(mContext, R.drawable.icon_extension48);

        ContentValues values = new ContentValues();
        values.put(Registration.ExtensionColumns.CONFIGURATION_ACTIVITY, MainActivity.class.getName());
        values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT, mContext.getString(R.string.configuration_text));
        values.put(Registration.ExtensionColumns.NAME, mContext.getString(R.string.extension_name));
        values.put(Registration.ExtensionColumns.EXTENSION_KEY, TokenExtensionService.EXTENSION_KEY);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, iconHostapp);
        // 一代手表专用 36x36 图标
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, iconExtension);
        // 二代 SW2 手表专用 48x48 菜单高清图标 (核心关键字段！)
        values.put(Registration.ExtensionColumns.EXTENSION_48PX_ICON_URI, iconExtension48);
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION, getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, mContext.getPackageName());

        return values;
    }

    @Override
    public boolean isDisplaySizeSupported(int width, int height) {
        return (width == 220 && height == 176);
    }
}
