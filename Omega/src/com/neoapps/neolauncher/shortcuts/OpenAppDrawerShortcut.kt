/*
 * This file is part of Neo Launcher
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

package com.neoapps.neolauncher.shortcuts

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import com.android.launcher3.LauncherSettings
import com.android.launcher3.R
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.neoapps.neolauncher.NeoLauncher

/**
 * Home screen shortcut that opens the app drawer.
 *
 * Unlike the old "Actions" shortcut of Neo 0.9.x, a tap on it is handled inside the launcher
 * (see [com.android.launcher3.touch.ItemClickHandler]): no intermediate activity or task is
 * started, so Android never has a chance to bring another app back to the front.
 */
object OpenAppDrawerShortcut {
    const val SHORTCUT_ID = "neo_open_app_drawer"
    const val ACTION = "com.neoapps.neolauncher.action.OPEN_APP_DRAWER"

    /** Returns true if [info] is the "open app drawer" shortcut pinned by this launcher. */
    @JvmStatic
    fun matches(context: Context, info: ItemInfo?): Boolean =
        info is WorkspaceItemInfo &&
                info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT &&
                info.targetPackage == context.packageName &&
                info.deepShortcutId == SHORTCUT_ID
}

/**
 * Shortcut configuration activity listed in the widget picker ("Open app drawer").
 * It shows no UI: it immediately returns a pinned shortcut to the launcher.
 */
class OpenAppDrawerShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != Intent.ACTION_CREATE_SHORTCUT) {
            finish()
            return
        }

        val launcherComponent = ComponentName(this, NeoLauncher::class.java)
        // Only used if the shortcut is ever launched from outside Neo: it just brings
        // the launcher to the front. Inside Neo the tap is intercepted.
        val launchIntent = Intent(OpenAppDrawerShortcut.ACTION)
            .setComponent(launcherComponent)

        val result = try {
            val info = ShortcutInfo.Builder(this, OpenAppDrawerShortcut.SHORTCUT_ID)
                .setActivity(launcherComponent)
                .setShortLabel(getString(R.string.action_open_drawer))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_open_app_drawer))
                .setIntent(launchIntent)
                .build()
            getSystemService(ShortcutManager::class.java)?.createShortcutResultIntent(info)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to create the open app drawer shortcut", e)
            null
        }

        setResult(if (result != null) RESULT_OK else RESULT_CANCELED, result)
        finish()
    }

    private companion object {
        const val TAG = "OpenAppDrawerShortcut"
    }
}
