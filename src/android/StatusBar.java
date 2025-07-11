/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.cordova.statusbar;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

public class StatusBar extends CordovaPlugin {
    private static final String TAG = "StatusBar";

    // Action constants
    private static final String ACTION_HIDE = "hide";
    private static final String ACTION_SHOW = "show";
    private static final String ACTION_READY = "_ready";
    private static final String ACTION_BACKGROUND_COLOR_BY_HEX_STRING = "backgroundColorByHexString";
    private static final String ACTION_OVERLAYS_WEB_VIEW = "overlaysWebView";
    private static final String ACTION_STYLE_DEFAULT = "styleDefault";
    private static final String ACTION_STYLE_LIGHT_CONTENT = "styleLightContent";
    private static final String ACTION_GET_NAVIGATION_BAR_HEIGHT = "getNavigationBarHeight";
    private static final String ACTION_GET_STATUS_BAR_HEIGHT = "getStatusBarHeight";
    private static final String ACTION_GET_TOTAL_SCREEN_HEIGHT = "getTotalScreenHeight";

    // Style constants
    private static final String STYLE_DEFAULT = "default";
    private static final String STYLE_LIGHT_CONTENT = "lightcontent";

    private AppCompatActivity activity;
    private Window window;

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "StatusBar: initialization");
        super.initialize(cordova, webView);

        activity = this.cordova.getActivity();
        window = activity.getWindow();

        activity.runOnUiThread(() -> {
            // Clear flag FLAG_FORCE_NOT_FULLSCREEN which is set initially
            // by the Cordova.
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

            // Read 'StatusBarOverlaysWebView' from config.xml, default is true.
            setStatusBarTransparent(preferences.getBoolean("StatusBarOverlaysWebView", true));

            // Read 'StatusBarBackgroundColor' from config.xml, default is #000000.
            setStatusBarBackgroundColor(preferences.getString("StatusBarBackgroundColor", "#000000"));

            // Read 'StatusBarStyle' from config.xml, default is 'lightcontent'.
            setStatusBarStyle(
                preferences.getString("StatusBarStyle", STYLE_LIGHT_CONTENT).toLowerCase()
            );
        });
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) {
        LOG.v(TAG, "Executing action: " + action);

        switch (action) {
            case ACTION_READY:
                boolean statusBarVisible = (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0;
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, statusBarVisible));
                return true;

            case ACTION_SHOW:
                activity.runOnUiThread(() -> showStatusBar());
                return true;

            case ACTION_HIDE:
                activity.runOnUiThread(() -> hideStatusBar());
                return true;

            case ACTION_BACKGROUND_COLOR_BY_HEX_STRING:
                activity.runOnUiThread(() -> {
                    try {
                        setStatusBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                });
                return true;

            case ACTION_OVERLAYS_WEB_VIEW:
                activity.runOnUiThread(() -> {
                    try {
                        setStatusBarTransparent(args.getBoolean(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid boolean argument");
                    }
                });
                return true;

            case ACTION_STYLE_DEFAULT:
                activity.runOnUiThread(() -> setStatusBarStyle(STYLE_DEFAULT));
                return true;

            case ACTION_STYLE_LIGHT_CONTENT:
                activity.runOnUiThread(() -> setStatusBarStyle(STYLE_LIGHT_CONTENT));
                return true;

            case ACTION_GET_NAVIGATION_BAR_HEIGHT:
            case ACTION_GET_STATUS_BAR_HEIGHT:
            case ACTION_GET_TOTAL_SCREEN_HEIGHT:
                handleHeightRequests(action, callbackContext);
                return true;

            default:
                return false;
        }
    }

    /**
     * Handle all height-related requests
     */
    private void handleHeightRequests(String action, CallbackContext callbackContext) {
        activity.runOnUiThread(() -> {
            int height;
            String logMessage;

            switch (action) {
                case ACTION_GET_NAVIGATION_BAR_HEIGHT:
                    height = getNavigationBarHeight();
                    logMessage = "Navigation bar height";
                    break;
                case ACTION_GET_STATUS_BAR_HEIGHT:
                    height = getStatusBarHeight();
                    logMessage = "Status bar height";
                    break;
                case ACTION_GET_TOTAL_SCREEN_HEIGHT:
                    height = getTotalScreenHeight();
                    logMessage = "Total screen height";
                    break;
                default:
                    return;
            }

            LOG.d(TAG, logMessage + ": " + height);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, height));
        });
    }

    /**
     * Show the status bar
     */
    private void showStatusBar() {
        int uiOptions = window.getDecorView().getSystemUiVisibility();
        uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;

        window.getDecorView().setSystemUiVisibility(uiOptions);

        // CB-11197 We still need to update LayoutParams to force status bar
        // to be hidden when entering e.g. text fields
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Hide the status bar
     */
    private void hideStatusBar() {
        int uiOptions = window.getDecorView().getSystemUiVisibility()
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_FULLSCREEN;

        window.getDecorView().setSystemUiVisibility(uiOptions);

        // CB-11197 We still need to update LayoutParams to force status bar
        // to be hidden when entering e.g. text fields
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Set the status bar background color
     */
    private void setStatusBarBackgroundColor(final String colorPref) {
        if (colorPref.isEmpty()) return;

        int color;
        try {
            color = Color.parseColor(colorPref);
        } catch (IllegalArgumentException ignore) {
            LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
            return;
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS); // SDK 19-30
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS); // SDK 21
        window.setStatusBarColor(color);
    }

    /**
     * Set the status bar transparency
     */
    private void setStatusBarTransparent(final boolean isTransparent) {
        int visibility = isTransparent
            ? View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            : View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_VISIBLE;

        window.getDecorView().setSystemUiVisibility(visibility);

        if (isTransparent) {
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    /**
     * Set the status bar style
     */
    private void setStatusBarStyle(final String style) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !style.isEmpty()) {
            View decorView = window.getDecorView();
            WindowInsetsControllerCompat windowInsetsControllerCompat = WindowCompat.getInsetsController(window, decorView);

            if (style.equals(STYLE_DEFAULT)) {
                windowInsetsControllerCompat.setAppearanceLightStatusBars(true);
            } else if (style.equals(STYLE_LIGHT_CONTENT)) {
                windowInsetsControllerCompat.setAppearanceLightStatusBars(false);
            } else {
                LOG.e(TAG, "Invalid style, must be either 'default' or 'lightcontent'");
            }
        }
    }

    /**
     * Gets window insets for API 30+ devices
     */
    private WindowInsets getWindowInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            View decorView = window.getDecorView();
            return decorView.getRootWindowInsets();
        }
        return null;
    }

    /**
     * Gets the bottom navigation bar height
     */
    private int getNavigationBarHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets insets = getWindowInsets();
            if (insets != null) {
                return insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom;
            }
        }
        return 0;
    }

    /**
     * Gets the top status bar height
     */
    private int getStatusBarHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets insets = getWindowInsets();
            if (insets != null) {
                return insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top;
            }
        }
        return 0;
    }

    /**
     * Gets the total screen height
     */
    private int getTotalScreenHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager windowManager = activity.getSystemService(WindowManager.class);
            WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
            return metrics.getBounds().height();
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        }
    }
}
