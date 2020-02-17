/* Copyright 2017 Andrew Dawson
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>. */

package tech.bigfig.roma;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import timber.log.Timber;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import tech.bigfig.roma.adapter.AccountSelectionAdapter;
import tech.bigfig.roma.db.AccountEntity;
import tech.bigfig.roma.db.AccountManager;
import tech.bigfig.roma.di.Injectable;
import tech.bigfig.roma.interfaces.AccountSelectionListener;
import tech.bigfig.roma.interfaces.PermissionRequester;
import tech.bigfig.roma.util.ThemeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public abstract class BaseActivity extends AppCompatActivity implements Injectable {

    protected List<Call> callList;

    @Inject
    public ThemeUtils themeUtils;
    @Inject
    public AccountManager accountManager;

    private static final int REQUESTER_NONE = Integer.MAX_VALUE;
    private HashMap<Integer, PermissionRequester> requesters;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        /* There isn't presently a way to globally change the theme of a whole application at
         * runtime, just individual activities. So, each activity has to set its theme before any
         * views are created. */
        String theme = preferences.getString("appTheme", ThemeUtils.APP_THEME_DEFAULT);
        Timber.d("activeTheme", theme);
        if (theme.equals("black")) {
            setTheme(R.style.RomaBlackTheme);
        }

        themeUtils.setAppNightMode(theme, this);

        /* set the taskdescription programmatically, the theme would turn it blue */
        String appName = getString(R.string.app_name);
        Bitmap appIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.launcher_icon);
        int recentsBackgroundColor = ThemeUtils.getColor(this, R.attr.recents_background_color);

        setTaskDescription(new ActivityManager.TaskDescription(appName, appIcon, recentsBackgroundColor));

        int style = textStyle(preferences.getString("statusTextSize", "medium"));
        getTheme().applyStyle(style, false);

        if(requiresLogin()) {
            redirectIfNotLoggedIn();
        }

        callList = new ArrayList<>();
        requesters = new HashMap<>();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(RomaApplication.localeManager.setLocale(base));
    }

    protected boolean requiresLogin() {
        return true;
    }

    private static int textStyle(String name) {
        int style;
        switch (name) {
            case "smallest":
                style = R.style.TextSizeSmallest;
                break;
            case "small":
                style = R.style.TextSizeSmall;
                break;
            case "medium":
            default:
                style = R.style.TextSizeMedium;
                break;
            case "large":
                style = R.style.TextSizeLarge;
                break;
            case "largest":
                style = R.style.TextSizeLargest;
                break;
        }
        return style;
    }

    public void startActivityWithSlideInAnimation(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
    }

    public void finishWithoutSlideOutAnimation() {
        super.finish();
    }

    protected void redirectIfNotLoggedIn() {
        AccountEntity account = accountManager.getActiveAccount();
        if (account == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityWithSlideInAnimation(intent);
            finish();
        }
    }

    protected void showErrorDialog(View anyView, @StringRes int descriptionId, @StringRes int actionId, View.OnClickListener listener) {
        if (anyView != null) {
            Snackbar bar = Snackbar.make(anyView, getString(descriptionId), Snackbar.LENGTH_SHORT);
            bar.setAction(actionId, listener);
            bar.show();
        }
    }

    @Override
    protected void onDestroy() {
        for (Call call : callList) {
            call.cancel();
        }
        super.onDestroy();
    }

    public void showAccountChooserDialog(CharSequence dialogTitle, boolean showActiveAccount, AccountSelectionListener listener) {
        List<AccountEntity> accounts = accountManager.getAllAccountsOrderedByActive();
        AccountEntity activeAccount = accountManager.getActiveAccount();

        switch(accounts.size()) {
            case 1:
                listener.onAccountSelected(activeAccount);
                return;
            case 2:
                if (!showActiveAccount) {
                    for (AccountEntity account : accounts) {
                        if (activeAccount != account) {
                            listener.onAccountSelected(account);
                            return;
                        }
                    }
                }
                break;
        }

        if (!showActiveAccount && activeAccount != null) {
            accounts.remove(activeAccount);
        }
        AccountSelectionAdapter adapter = new AccountSelectionAdapter(this);
        adapter.addAll(accounts);

        new AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setAdapter(adapter, (dialogInterface, index) -> listener.onAccountSelected(accounts.get(index)))
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requesters.containsKey(requestCode)) {
            PermissionRequester requester = requesters.remove(requestCode);
            requester.onRequestPermissionsResult(permissions, grantResults);
        }
    }

    public void requestPermissions(String[] permissions, PermissionRequester requester) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for(String permission: permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.isEmpty()) {
            int[] permissionsAlreadyGranted = new int[permissions.length];
            for (int i = 0; i < permissionsAlreadyGranted.length; ++i)
                permissionsAlreadyGranted[i] = PackageManager.PERMISSION_GRANTED;
            requester.onRequestPermissionsResult(permissions, permissionsAlreadyGranted);
            return;
        }

        int newKey = requester == null ? REQUESTER_NONE : requesters.size();
        if (newKey != REQUESTER_NONE) {
            requesters.put(newKey, requester);
        }
        String[] permissionsCopy = new String[permissionsToRequest.size()];
        permissionsToRequest.toArray(permissionsCopy);
        ActivityCompat.requestPermissions(this, permissionsCopy, newKey);

    }
}
