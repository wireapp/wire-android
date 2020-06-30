package com.wire.android.feature.auth.registration.personal

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.accessibility.headingForAccessibility
import kotlinx.android.synthetic.main.fragment_create_personal_account.*


class CreatePersonalAccountFragment : Fragment(R.layout.fragment_create_personal_account) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCreateAnAccountTitle()
        initViewPager()
    }

    private fun initCreateAnAccountTitle() {
        createPersonalAccountTitleTextView.headingForAccessibility(true)
    }

    private fun initViewPager() {
        val titles = listOf(
            getString(R.string.authentication_tab_layout_title_email),
            getString(R.string.authentication_tab_layout_title_phone)
        )
        createPersonalAccountViewPager.adapter = CreatePersonalAccountViewPagerAdapter(childFragmentManager, titles)
        //TODO, do we want the view to be this smart? Even though it is only view logic.
        for (i in 0..titles.size) {
            val tab = createPersonalAccountTabLayout.getTabAt(i)
            tab?.contentDescription = getString(R.string.create_an_account_tab_content_description, titles[i], i + 1, titles.size)
        }
    }


    companion object {
        fun newInstance() = CreatePersonalAccountFragment()
    }
}
