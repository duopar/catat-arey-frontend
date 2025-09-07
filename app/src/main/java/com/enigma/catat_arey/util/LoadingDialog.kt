package com.enigma.catat_arey.util

import android.app.Dialog
import android.content.Context
import com.enigma.catat_arey.R

class LoadingDialog private constructor(context: Context) : Dialog(context) {

    init {
        setContentView(R.layout.dialog_loading)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    companion object {
        private var instance: LoadingDialog? = null

        @JvmStatic
        fun getInstance(context: Context): LoadingDialog {
            if (instance == null || instance?.context != context) {
                instance = LoadingDialog(context)
            }
            return instance!!
        }

        fun showLoading(context: Context) {
            try {
                if (instance?.isShowing == true) return
                getInstance(context).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun hideLoading() {
            try {
                instance?.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}