package com.sysu.example.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment

// [DialogFragment使用小结](https://www.jianshu.com/p/0861ee5b9028)

abstract class BaseDialogFragment(
    @LayoutRes open val layoutResId: Int,
    open var fullScreen: Boolean = true
) : DialogFragment() {
    // init {
    //     if (fullScreen) {
    //         setStyle(STYLE_NORMAL, R.style.Dialog_FullScreen)
    //     }
    // }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (fullScreen) {
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                decorView.setPadding(0)
                val wlp: WindowManager.LayoutParams = attributes
                wlp.width = WindowManager.LayoutParams.MATCH_PARENT
                wlp.height = WindowManager.LayoutParams.MATCH_PARENT
                attributes = wlp
            }
        }
        dialog.setOnShowListener(showListener)
        // dialog.setOnDismissListener(dismissListener)  // 无效，dialogFragment中只能走onDismiss
        dialog.setOnCancelListener(cancelListener)
        dialog.setOnKeyListener(keyListener)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }

    override fun onResume() {
        if (fullScreen) {
            val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            dialog!!.window!!.attributes = params as WindowManager.LayoutParams
        }
        super.onResume()
    }

    open var showListener: DialogInterface.OnShowListener? = null
    open var dismissListener: DialogInterface.OnDismissListener? = null
    open var cancelListener: DialogInterface.OnCancelListener? = null
    open var keyListener: DialogInterface.OnKeyListener? = null
    // open var multiChoiceClickListener: DialogInterface.OnMultiChoiceClickListener? = null
    // open var clickListener: DialogInterface.OnClickListener? = null

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissListener?.onDismiss(dialog)
    }
}
