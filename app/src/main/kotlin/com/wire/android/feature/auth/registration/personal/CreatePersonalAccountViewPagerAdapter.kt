package com.wire.android.feature.auth.registration.personal

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.wire.android.feature.auth.registration.personal.email.CreatePersonalAccountEmailFragment
import com.wire.android.feature.auth.registration.personal.phone.CreatePersonalAccountPhoneFragment
import java.util.Locale

class CreatePersonalAccountViewPagerAdapter(
    fragmentManager: FragmentManager,
    private val titles: List<String>
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int = titles.size

    override fun getItem(position: Int): Fragment =
        if (position == PHONE_TAB_POSITION) CreatePersonalAccountPhoneFragment.newInstance()
        else CreatePersonalAccountEmailFragment.newInstance()

    override fun getPageTitle(position: Int): CharSequence =
        titles[position].toUpperCase(Locale.getDefault())

    companion object {
        private const val PHONE_TAB_POSITION = 1
    }
}
