/*
 * Copyright 2018 Olivér Falvai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ofalvai.bpinfo.ui.alertlist.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.alertlist.AlertListType
import com.ofalvai.bpinfo.util.*
import com.wefika.flowlayout.FlowLayout
import timber.log.Timber

class AlertHolder(itemView: View, private val alertListType: AlertListType) : RecyclerView.ViewHolder(itemView) {

    private val titleTextView: TextView by bindView(R.id.list_item_alert_description)

    private val dateTextView: TextView by bindView(R.id.list_item_alert_date)

    private val routeIconsWrapper: FlowLayout by bindView(R.id.list_item_alert_route_icons_wrapper)

    private val recentTextView: TextView by bindView(R.id.list_item_alert_recent)

    /**
     * List of currently displayed route icons. This list is needed in order to find visually
     * duplicate route data, and not to display them twice.
     */
    private val displayedRoutes = mutableListOf<Route>()

    fun bindAlert(alert: Alert) {
        titleTextView.text = alert.header

        dateTextView.text = alert.formatDate(itemView.context)

        // Route icons
        // First, removing any previously added icons
        routeIconsWrapper.removeAllViews()
        displayedRoutes.clear()

        // There are alerts without affected routes, eg. announcements
        for (route in alert.affectedRoutes) {
            // Some affected routes are visually identical to others in the list, no need
            // to diplay them again.
            if (!isRouteVisuallyDuplicate(route, displayedRoutes)) {
                displayedRoutes.add(route)
                addRouteIcon(itemView.context, routeIconsWrapper, route)

                if (route.type == RouteType.OTHER) {
                    Timber.d("Unknown route type: %s (%s)", route.shortName, route.id)
                }
            }
        }

        if (alertListType == AlertListType.Today) {
            recentTextView.visibility = if (alert.isRecent()) View.VISIBLE else View.GONE
        } else {
            recentTextView.visibility = View.GONE
        }
    }
}
