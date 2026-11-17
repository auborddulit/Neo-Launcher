package com.neoapps.neolauncher.allapps.search

import android.content.Context
import com.android.launcher3.Launcher
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem
import com.android.launcher3.allapps.search.DefaultAppSearchAlgorithm
import com.android.launcher3.model.AllAppsList
import com.android.launcher3.model.BgDataModel
import com.android.launcher3.model.ModelTaskController
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.search.SearchCallback
import com.android.launcher3.search.StringMatcherUtility
import com.neoapps.neolauncher.launcher
import com.neoapps.neolauncher.util.prefs
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.algorithms.WeightedRatio
import java.util.Locale

/**
 * App-only search for the app drawer (no web suggestions).
 */
class NeoAppSearchAlgorithm(val context: Context, addNoResultsMessage: Boolean) :
    DefaultAppSearchAlgorithm(context, addNoResultsMessage) {

    private val prefs = context.prefs

    override fun doSearch(query: String, callback: SearchCallback<AdapterItem>?) {
        Launcher.getLauncher(context).model.enqueueModelUpdateTask { _: ModelTaskController?, _: BgDataModel?, apps: AllAppsList? ->
            // Work on a copy: the model's list must never be modified from here.
            val candidates: List<AppInfo> =
                if (prefs.searchHiddenApps.getValue()) ArrayList(context.launcher.allApps)
                else ArrayList(apps?.data ?: emptyList())
            val result = getSearchResult(candidates, query)
            if (mAddNoResultsMessage && result.isEmpty()) {
                result.add(getEmptyMessageAdapterItem(query))
            }
            mResultHandler.post { callback?.onSearchResult(query, result) }
        }
    }

    private fun getSearchResult(apps: List<AppInfo>, query: String): ArrayList<AdapterItem> {
        return if (prefs.searchFuzzy.getValue()) {
            matchFuzzy(apps, query)
        } else {
            matchTitles(apps, query)
        }
    }

    private fun matchFuzzy(apps: List<AppInfo>, query: String): ArrayList<AdapterItem> {
        val matches = FuzzySearch.extractSorted(
            query.lowercase(Locale.getDefault()), apps,
            { it.title.toString() }, WeightedRatio(), 65
        )
        val result = ArrayList<AdapterItem>()
        for (match in matches) {
            if (result.size >= MAX_RESULTS_COUNT) break
            result.add(AdapterItem.asApp(match.referent))
        }
        return result
    }

    private fun matchTitles(apps: List<AppInfo>, query: String): ArrayList<AdapterItem> {
        val queryTextLower = query.lowercase(Locale.getDefault())
        val matcher = StringMatcherUtility.StringMatcher.getInstance()
        val result = ArrayList<AdapterItem>()
        for (info in apps) {
            if (result.size >= MAX_RESULTS_COUNT) break
            if (StringMatcherUtility.matches(queryTextLower, info.title.toString(), matcher)) {
                result.add(AdapterItem.asApp(info))
            }
        }
        return result
    }
}
