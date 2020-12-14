package gg.strims.android.utils

import androidx.fragment.app.Fragment
import gg.strims.android.R

fun hideChildFragment(fragment: Fragment, fragmentToHide: Fragment) {
    fragment.childFragmentManager.beginTransaction()
        .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
        .hide(fragmentToHide)
        .commit()
}

fun showChildFragment(fragment: Fragment, fragmentToShow: Fragment) {
    fragment.childFragmentManager.beginTransaction()
        .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
        .show(fragmentToShow)
        .commit()
}