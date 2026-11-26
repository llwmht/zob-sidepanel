package io.github.llwmht.sidepanel

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.view.WindowManager
import org.json.JSONArray
import org.json.JSONObject

/** Bridge methods backed by the device. Table: docs/bridge.md. Called on the UI thread. */
fun deviceHandlers(a: PanelActivity): Map<String, (JSONObject) -> Any?> = mapOf(
    "info" to { _ ->
        JSONObject()
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("webviewMajor", a.webviewMajor)
            .put("sdk", Build.VERSION.SDK_INT)
    },
    "battery" to { _ ->
        val b = a.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        JSONObject()
            .put("level", if (level < 0 || scale <= 0) 0 else level * 100 / scale)
            .put("charging", status == BatteryManager.BATTERY_STATUS_CHARGING)
    },
    "setBrightness" to { p ->
        val lp = a.window.attributes
        lp.screenBrightness = clampBrightness(p.optInt("level", -1))
        a.window.attributes = lp
        null
    },
    "setKeepAwake" to { p ->
        if (p.optBoolean("on")) a.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else a.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        null
    },
    "listApps" to { _ ->
        JSONArray().apply {
            launchableApps(a).forEach { put(JSONObject().put("label", it.label).put("package", it.pkg)) }
        }
    },
    "launchApp" to { p ->
        val intent = a.packageManager.getLaunchIntentForPackage(p.optString("package"))
        if (intent == null) false else { a.startActivity(intent); true }
    },
    "openDrawer" to { _ -> a.openDrawer(); null },
    "getLayout" to { _ -> (a.application as App).editorApi.currentLayout() },
    "setNight" to { p ->
        (a.application as App).night.set(p.optBoolean("enabled"), p.optString("from"), p.optString("to"), p.optString("mode"))
        null
    },
    "nightScreenOff" to { _ -> (a.application as App).night.screenOffIfNight(); null },
)
