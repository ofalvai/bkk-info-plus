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

package com.ofalvai.bpinfo.ui.alertdetail

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.ContentLoadingProgressBar
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.Resource
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.ui.alertlist.AlertListType
import com.ofalvai.bpinfo.util.*
import com.wefika.flowlayout.FlowLayout
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.sufficientlysecure.htmltextview.HtmlTextView
import timber.log.Timber

class AlertDetailFragment : BottomSheetDialogFragment() {

    companion object {

        const val FRAGMENT_TAG = "alert_detail"

        private const val ARG_ALERT_OBJECT = "alert_object"
        private const val ARG_LIST_TYPE = "alert_list_type"

        fun newInstance(alert: Alert, alertListType: AlertListType): AlertDetailFragment {
            val fragment = AlertDetailFragment()
            val args = bundleOf(
                ARG_ALERT_OBJECT to alert,
                ARG_LIST_TYPE to alertListType
            )
            fragment.arguments = args
            return fragment
        }
    }

    private val viewModel by viewModel<AlertDetailViewModel>()

    private var alertWithDetails: Alert? = null

    private lateinit var alertListType: AlertListType

    private val analytics: Analytics by inject()

    /**
     * List of currently displayed route icons. This list is needed in order to find visually
     * duplicate route data, and not to display them twice.
     */
    private val displayedRoutes = mutableListOf<Route>()

    private val titleTextView: TextView by bindView(R.id.alert_detail_title)

    private val dateTextView: TextView by bindView(R.id.alert_detail_date)

    private val routeIconsLayout: FlowLayout by bindView(R.id.alert_detail_route_icons_wrapper)

    private val descriptionTextView: HtmlTextView by bindView(R.id.alert_detail_description)

    private val urlTextView: TextView by bindView(R.id.alert_detail_url)

    private val progressBar: ContentLoadingProgressBar by bindView(R.id.alert_detail_progress_bar)

    private val errorLayout: LinearLayout by bindView(R.id.error_with_action)

    private val errorMessage: TextView by bindView(R.id.error_message)

    private val errorButton: Button by bindView(R.id.error_action_button)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            alertWithDetails = arguments?.getSerializable(ARG_ALERT_OBJECT) as Alert
            alertListType = arguments?.getSerializable(ARG_LIST_TYPE) as AlertListType
        }

        if (savedInstanceState != null) {
             alertWithDetails = savedInstanceState.getSerializable(ARG_ALERT_OBJECT) as Alert
        }

        analytics.logAlertContentView(alertWithDetails)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(ARG_ALERT_OBJECT, alertWithDetails)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_alert_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayAlert(alertWithDetails)

        alertWithDetails?.let {
            if (!it.isPartial) {
                progressBar.hide()
            }
        }

        errorButton.setOnClickListener {
            alertWithDetails?.id?.let {
                val alertLiveData = viewModel.reloadAlert(it, alertListType)
                observe(alertLiveData) { resource ->
                    when (resource) {
                        is Resource.Success -> updateAlert(resource.value)
                        is Resource.Loading -> {
                            errorLayout.visibility = View.GONE
                            progressBar.show()
                        }
                        is Resource.Error -> onAlertUpdateFailed()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Bottom sheets on tablets should have a smaller width than the screen width.
        val width = requireContext().resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
        val actualWidth = if (width > 0) width else ViewGroup.LayoutParams.MATCH_PARENT
        dialog?.window?.setLayout(actualWidth, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun updateAlert(alert: Alert) {
        this.alertWithDetails = alert
        displayedRoutes.clear()
        routeIconsLayout.removeAllViews()

        // Updating views
        displayAlert(alert)

        // View animations
        // For some reason, ObjectAnimator doesn't work here (skips animation states, just shows the
        // last frame), we need to use ValueAnimators.
        val animatorSet = AnimatorSet()
        animatorSet.duration = 300
        animatorSet.interpolator = FastOutSlowInInterpolator()

        // We can't measure the TextView's height before a layout happens because of the setText() call
        // Note: even though displayAlert() was called earlier, the TextView's height is still 0.
        val heightEstimate = descriptionTextView.lineHeight * descriptionTextView.lineCount + 10

        val descriptionHeight = ValueAnimator.ofInt(descriptionTextView.height, heightEstimate)
        descriptionHeight.addUpdateListener { animation ->
            descriptionTextView.layoutParams.height = animation.animatedValue as Int
            descriptionTextView.requestLayout()
        }
        descriptionHeight.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                descriptionTextView.alpha = 1.0f
                descriptionTextView.visibility = View.VISIBLE
            }
        })

        val descriptionAlpha = ValueAnimator.ofFloat(0.0f, 1.0f)
        descriptionAlpha.addUpdateListener { animation ->
            descriptionTextView.alpha = animation.animatedValue as Float
        }

        val progressHeight = ValueAnimator.ofInt(progressBar.height, 0)
        progressHeight.addUpdateListener { animation ->
            progressBar.layoutParams.height = animation.animatedValue as Int
            progressBar.requestLayout()
        }
        progressHeight.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progressBar.hide()
            }
        })

        animatorSet.playTogether(progressHeight, descriptionHeight, descriptionAlpha)
        animatorSet.start()
    }

    fun onAlertUpdateFailed() {
        progressBar.hide()
        errorMessage.setText(R.string.error_alert_detail_load)
        errorButton.setText(R.string.label_retry)
        errorLayout.visibility = View.VISIBLE
    }

    private fun displayAlert(alert: Alert?) {
        if (alert == null) return

        titleTextView.text = alert.header

        val dateString = alert.formatDate(requireContext())
        dateTextView.text = dateString

        // There are alerts without affected routes, eg. announcements
        for (route in alert.affectedRoutes) {
            // Some affected routes are visually identical to others in the list, no need
            // to display them again.
            if (!isRouteVisuallyDuplicate(route, displayedRoutes)) {
                displayedRoutes.add(route)
                addRouteIcon(requireContext(), routeIconsLayout, route)
            }
        }

        alert.description?.let {
            try {
                descriptionTextView.setHtml(it)
            } catch (t: Throwable) {
                analytics.logException(t)
                Timber.e(t, "Failed to parse alert description HTML")
            }
        }

        if (alert.url == null) {
            urlTextView.visibility = View.GONE
        } else {
            urlTextView.paintFlags = urlTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            urlTextView.setOnClickListener {
                val url = Uri.parse(alert.url)
                openCustomTab(requireActivity(), url)
                analytics.logAlertUrlClick(alert)
            }
        }
    }
}
