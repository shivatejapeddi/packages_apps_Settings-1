package com.android.settings.citrus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class AmbientSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "AmbientSettings";

    private static final String KEY_DOZE = "doze";
    private static final String KEY_DOZE_PULSE_IN = "doze_pulse_in";
    private static final String KEY_DOZE_PULSE_VISIBLE = "doze_pulse_visible";
    private static final String KEY_DOZE_PULSE_OUT = "doze_pulse_out";

    private static final String SYSTEMUI_METADATA_NAME = "com.android.systemui";

    private SwitchPreference mDozePreference;
    private ListPreference mDozePulseIn;
    private ListPreference mDozePulseVisible;
    private ListPreference mDozePulseOut;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SQUASH;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.ambient_settings);

        mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
        mDozePreference.setOnPreferenceChangeListener(this);

        mDozePulseIn = (ListPreference) findPreference(KEY_DOZE_PULSE_IN);
        mDozePulseIn.setOnPreferenceChangeListener(this);

        mDozePulseVisible = (ListPreference) findPreference(KEY_DOZE_PULSE_VISIBLE);
        mDozePulseVisible.setOnPreferenceChangeListener(this);

        mDozePulseOut = (ListPreference) findPreference(KEY_DOZE_PULSE_OUT);
        mDozePulseOut.setOnPreferenceChangeListener(this);

        updateDozeOptions();

    }

    private void updateDozeOptions() {
        if (mDozePulseIn != null) {
            final int statusDozePulseIn = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_IN, 500);
            mDozePulseIn.setValue(String.valueOf(statusDozePulseIn));
            int index = mDozePulseIn.findIndexOfValue(String.valueOf(statusDozePulseIn));
            if (index != -1) {
                mDozePulseIn.setSummary(mDozePulseIn.getEntries()[index]);
            }
        }
        if (mDozePulseVisible != null) {
            final int statusDozePulseVisible = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_VISIBLE, 3000);
            mDozePulseVisible.setValue(String.valueOf(statusDozePulseVisible));
            int index = mDozePulseVisible.findIndexOfValue(String.valueOf(statusDozePulseVisible));
            if (index != -1) {
                mDozePulseVisible.setSummary(mDozePulseVisible.getEntries()[index]);
            }
        }
        if (mDozePulseOut != null) {
            final int statusDozePulseOut = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_OUT, 500);
            mDozePulseOut.setValue(String.valueOf(statusDozePulseOut));
            int index = mDozePulseOut.findIndexOfValue(String.valueOf(statusDozePulseOut));
            if (index != -1) {
               mDozePulseOut.setSummary(mDozePulseOut.getEntries()[index]);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
        updateDozeOptions();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateState() {
        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), Settings.Secure.DOZE_ENABLED,
                    getActivity().getResources().getBoolean(
                    com.android.internal.R.bool.config_doze_enabled_by_default) ? 1 : 0);
            mDozePreference.setChecked(value != 0);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mDozePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.DOZE_ENABLED, value ? 1 : 0);
        } else if (preference == mDozePulseIn) {
            int dozePulseIn = Integer.parseInt((String)objValue);
            int index = mDozePulseIn.findIndexOfValue((String) objValue);
            mDozePulseIn.setSummary(mDozePulseIn.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_IN, dozePulseIn);
        } else if (preference == mDozePulseVisible) {
            int dozePulseVisible = Integer.parseInt((String)objValue);
            int index = mDozePulseVisible.findIndexOfValue((String) objValue);
            mDozePulseVisible.setSummary(mDozePulseVisible.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_VISIBLE, dozePulseVisible);
        } else if (preference == mDozePulseOut) {
            int dozePulseOut = Integer.parseInt((String)objValue);
            int index = mDozePulseOut.findIndexOfValue((String) objValue);
            mDozePulseOut.setSummary(mDozePulseOut.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_OUT, dozePulseOut);
        }
        return true;
    }

    private static boolean isPickupSensorUsedByDefault(Context context) {
        return getConfigBoolean(context, "doze_pulse_on_pick_up");
    }

    private static Boolean getConfigBoolean(Context context, String configBooleanName) {
        int resId = -1;
        Boolean b = true;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("DozeSettings:", "can't access systemui resources",e);
            return null;
        }

        resId = systemUiResources.getIdentifier(
            SYSTEMUI_METADATA_NAME + ":bool/" + configBooleanName, null, null);
        if (resId > 0) {
            b = systemUiResources.getBoolean(resId);
        }
        return b;
    }
} 

