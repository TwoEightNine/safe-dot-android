/*
 * Copyright (C) 2020.  Aravind Chowdary (@kamaravichow)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aravi.dot.activities.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.aravi.dot.BuildConfig;
import com.aravi.dot.R;
import com.aravi.dot.Utils;
import com.aravi.dot.activities.log.LogsActivity;
import com.aravi.dot.databinding.ActivityMainBinding;
import com.aravi.dot.manager.SharedPreferenceManager;
import com.aravi.dot.service.DotService;

public class MainActivity extends AppCompatActivity {

    private SharedPreferenceManager sharedPreferenceManager;
    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
        init();
        checkAutoStartRequirement();
    }

    private void init() {
        mBinding.vibrationSwitch.setChecked(sharedPreferenceManager.isVibrationEnabled());

        mBinding.vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> sharedPreferenceManager.setVibrationEnabled(isChecked));
        mBinding.align.setOnCheckedChangeListener((group, i) -> {
            // fixed: Resource IDs will be non-final in Android Gradle Plugin version 5.0, avoid using them in switch case statements
            // fix source : http://tools.android.com/tips/non-constant-fields
            if (i == R.id.topLeft) {
                sharedPreferenceManager.setPosition(0);
            } else if (i == R.id.topRight) {
                sharedPreferenceManager.setPosition(1);
            }
        });
        mBinding.logsOption.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, LogsActivity.class);
            startActivity(intent);
        });

        mBinding.versionText.setText("Version - " + BuildConfig.VERSION_NAME);
    }

    private void checkForAccessibilityAndStart() {
        if (!accessibilityPermission(getApplicationContext(), DotService.class)) {
            startActivity(new Intent("android.settings.ACCESSIBILITY_SETTINGS"));
        } else {
            Intent serviceIntent = new Intent(MainActivity.this, DotService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForAccessibilityAndStart();
    }

    public static boolean accessibilityPermission(Context context, Class<?> cls) {
        ComponentName componentName = new ComponentName(context, cls);
        String string = Settings.Secure.getString(context.getContentResolver(), "enabled_accessibility_services");
        if (string == null) {
            return false;
        }
        TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(':');
        simpleStringSplitter.setString(string);
        while (simpleStringSplitter.hasNext()) {
            ComponentName unflattenFromString = ComponentName.unflattenFromString(simpleStringSplitter.next());
            if (unflattenFromString != null && unflattenFromString.equals(componentName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Chinese ROM's kill the app services frequently so AutoStart Permission is required
     */
    private void checkAutoStartRequirement() {
        String manufacturer = android.os.Build.MANUFACTURER;
        if (sharedPreferenceManager.isFirstLaunch()) {
            if ("xiaomi".equalsIgnoreCase(manufacturer)
                    || ("oppo".equalsIgnoreCase(manufacturer))
                    || ("vivo".equalsIgnoreCase(manufacturer))
                    || ("Honor".equalsIgnoreCase(manufacturer))) {
                Utils.showAutoStartDialog(MainActivity.this);
                sharedPreferenceManager.setFirstLaunch();
            }
        }
    }
}