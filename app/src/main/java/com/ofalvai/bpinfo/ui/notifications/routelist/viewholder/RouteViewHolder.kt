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

package com.ofalvai.bpinfo.ui.notifications.routelist.viewholder

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.util.addRouteIcon
import kotterknife.bindView

class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val iconWrapperLayout: FrameLayout by bindView(R.id.list_item_route__icon_wrapper)

    private val descriptionTextView: TextView by bindView(R.id.list_item_route__description)

    fun bind(route: Route) {
        iconWrapperLayout.removeAllViews()
        addRouteIcon(itemView.context, iconWrapperLayout, route)

        descriptionTextView.text = Html.fromHtml(route.description)
    }
}
