package com.wire.android.feature.auth.registration.personal

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.wire.android.feature.auth.registration.personal.email.CreatePersonalAccountEmailFragment

class CreatePersonalAccountViewPagerAdapter(fragment:Fragment): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment =
        //TODO add phone fragment
        CreatePersonalAccountEmailFragment.newInstance()

    companion object {
        private const val NUM_PAGES = 2
    }
}
