package com.wire.android.feature.auth.login

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.wire.android.feature.auth.login.email.LoginWithEmailFragment

class LoginViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment =
        //TODO add phone fragment
        LoginWithEmailFragment.newInstance()

    companion object {
        private const val NUM_PAGES = 2
    }
}
