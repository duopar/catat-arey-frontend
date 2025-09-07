package com.enigma.catat_arey.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import com.enigma.catat_arey.databinding.DialogErrorBinding

class ErrorPopupDialog private constructor(context: Context) : Dialog(context) {
    private lateinit var binding: DialogErrorBinding

    init {
        binding = DialogErrorBinding.inflate(layoutInflater)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setContentView(binding.root)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    companion object {
        private var instance: ErrorPopupDialog? = null

        @JvmStatic
        fun getInstance(context: Context): ErrorPopupDialog {
            if (instance == null || instance?.context != context) {
                instance = ErrorPopupDialog(context)
            }
            return instance!!
        }

        fun showError(
            context: Context,
            title: String,
            message: String,
            buttonText: String = "Close",
            cancelable: Boolean = false,
            onButtonClick: () -> Unit = {}
        ) {
            if (cancelable) {
                this.instance?.setCancelable(cancelable)
            }
            try {
                val dialog = getInstance(context)
                with(dialog.binding) {
                    tvTitle.text = title
                    tvMessage.text = message
                    btnAction.text = buttonText
                    btnAction.setOnClickListener {
                        onButtonClick()
                        dialog.dismiss()
                    }
                }
                if (!dialog.isShowing) dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun hideError() {
            try {
                instance?.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}