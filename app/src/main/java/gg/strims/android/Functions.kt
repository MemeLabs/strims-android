package gg.strims.android

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

fun keyRequestFocus(editText: EditText, context: Context) {
    editText.requestFocus()
    val imm: InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(
        InputMethodManager.SHOW_FORCED,
        InputMethodManager.HIDE_IMPLICIT_ONLY
    )
}

fun hideKeyboardFrom(context: Context, view: View) {
    val imm =
        context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun hideFragment(activity: FragmentActivity, fragment: Fragment) {
    activity.supportFragmentManager.beginTransaction()
        .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
        .hide(fragment)
        .commit()
}

fun showFragment(activity: FragmentActivity, fragment: Fragment) {
    activity.supportFragmentManager.beginTransaction()
        .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
        .show(fragment)
        .commit()
}

fun showHideFragment(activity: FragmentActivity, fragment: Fragment) {
    val fragmentTransaction = activity.supportFragmentManager.beginTransaction()
    fragmentTransaction.setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)

    if (fragment.isHidden) {
        fragmentTransaction.show(fragment)
    } else if (!fragment.isHidden) {
        fragmentTransaction.hide(fragment)
    }

    fragmentTransaction.commit()
}