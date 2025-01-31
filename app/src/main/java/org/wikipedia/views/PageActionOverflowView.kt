package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.core.widget.PopupWindowCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.textview.MaterialTextView
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.ContributionsDashboardEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ItemCustomizeToolbarMenuBinding
import org.wikipedia.databinding.ViewPageActionOverflowBinding
import org.wikipedia.page.PageViewModel
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.page.customize.CustomizeToolbarActivity
import org.wikipedia.page.tabs.Tab
import org.wikipedia.settings.Prefs
import org.wikipedia.usercontrib.ContributionsDashboardHelper
import org.wikipedia.util.ResourceUtil

class PageActionOverflowView(context: Context) : FrameLayout(context), DonorBadgeView.Callback {

    private var binding = ViewPageActionOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var popupWindowHost: PopupWindow? = null
    lateinit var callback: PageActionItem.Callback

    init {
        binding.overflowForward.setOnClickListener {
            dismissPopupWindowHost()
            callback.forwardClick()
        }
        loadDonorInformation()
        Prefs.customizeToolbarMenuOrder.forEach {
            val view = ItemCustomizeToolbarMenuBinding.inflate(LayoutInflater.from(context)).root
            val item = PageActionItem.find(it)
            view.id = item.viewId
            view.text = context.getString(item.titleResId)
            view.setCompoundDrawablesRelativeWithIntrinsicBounds(item.iconResId, 0, 0, 0)
            view.setOnClickListener {
                dismissPopupWindowHost()
                item.select(callback)
            }
            binding.overflowList.addView(view)
        }
        binding.customizeToolbar.setOnClickListener {
            dismissPopupWindowHost()
            context.startActivity(CustomizeToolbarActivity.newIntent(context))
        }
    }

    fun show(anchorView: View, callback: PageActionItem.Callback, currentTab: Tab, model: PageViewModel) {
        this.callback = callback
        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            it.showAsDropDown(anchorView, 0, 0, Gravity.END)
        }
        binding.overflowForward.visibility = if (currentTab.canGoForward()) VISIBLE else GONE

        for (i in 1 until binding.overflowList.childCount) {
            val view = binding.overflowList.getChildAt(i) as MaterialTextView
            val pageActionItem = PageActionItem.find(view.id)
            val enabled = model.page != null && (!model.shouldLoadAsMobileWeb || (model.shouldLoadAsMobileWeb && pageActionItem.isAvailableOnMobileWeb))
            when (pageActionItem) {
                PageActionItem.ADD_TO_WATCHLIST -> {
                    view.setText(if (model.isWatched) R.string.menu_page_unwatch else R.string.menu_page_watch)
                    view.setCompoundDrawablesRelativeWithIntrinsicBounds(PageActionItem.watchlistIcon(model.isWatched, model.hasWatchlistExpiry), 0, 0, 0)
                    view.visibility = if (enabled && AccountUtil.isLoggedIn) VISIBLE else GONE
                }
                PageActionItem.SAVE -> {
                    view.setCompoundDrawablesRelativeWithIntrinsicBounds(PageActionItem.readingListIcon(model.isInReadingList), 0, 0, 0)
                    view.visibility = if (enabled) VISIBLE else GONE
                }
                PageActionItem.EDIT_ARTICLE -> {
                    view.setCompoundDrawablesRelativeWithIntrinsicBounds(PageActionItem.editArticleIcon(model.page?.pageProperties?.canEdit != true), 0, 0, 0)
                }
                PageActionItem.VIEW_ON_MAP -> {
                    val geoAvailable = model.page?.pageProperties?.geo != null
                    val tintColor = ResourceUtil.getThemedColorStateList(context, if (geoAvailable) R.attr.primary_color else R.attr.inactive_color)
                    view.setTextColor(tintColor)
                    TextViewCompat.setCompoundDrawableTintList(view, tintColor)
                }
                else -> {
                    view.visibility = if (enabled) VISIBLE else GONE
                }
            }
        }

        anchorView.doOnDetach {
            dismissPopupWindowHost()
        }
    }

    private fun loadDonorInformation() {
        if (ContributionsDashboardHelper.contributionsDashboardEnabled && AccountUtil.isLoggedIn) {
            binding.donorContainer.isVisible = true
            binding.donorUsername.text = AccountUtil.userName
            binding.donorBadgeView.setup(this)
        }
    }

    private fun dismissPopupWindowHost() {
        popupWindowHost?.let {
            it.dismiss()
            popupWindowHost = null
        }
    }

    override fun onDonorBadgeClick() {
        // take user to Contribution Screen
        callback.onDonorSelected()
        dismissPopupWindowHost()
    }

    override fun onBecomeDonorClick() {
        // take user to the donation flow (the Donation bottom sheet).
        ContributionsDashboardEvent.logAction("donate_start_click", "contrib_overflow", campaignId = ContributionsDashboardHelper.CAMPAIGN_ID)
        callback.onBecomeDonorSelected()
        dismissPopupWindowHost()
    }

    override fun onUpdateDonorStatusClick() {
        // take user to the donor history screen
        ContributionsDashboardEvent.logAction("update_click", "contrib_overflow")
        callback.onUpdateDonorStatusSelected()
        dismissPopupWindowHost()
    }
}
