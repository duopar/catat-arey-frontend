package com.enigma.catat_arey.util

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.enigma.catat_arey.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun showCustomDialog(
    context: Context,
    title: String,
    layoutId: Int,
    positiveButtonText: String = "Simpan",
    negativeButtonText: String = "Batal",
    onPositiveAction: (dialogView: View, dialog: androidx.appcompat.app.AlertDialog) -> Unit,
    onDialogViewCreated: ((View) -> Unit)? = null
) {
    val dialogView = LayoutInflater.from(context).inflate(layoutId, null)
    onDialogViewCreated?.invoke(dialogView)

    val dialogLayoutBuilder = MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogTheme)
        .setView(dialogView)
        .setTitle(title)
        .setPositiveButton(positiveButtonText, null)
        .setNegativeButton(negativeButtonText) { dialog, _ ->
            dialog.dismiss()
        }

    val dialog = dialogLayoutBuilder.show()

    // prevent auto-dismiss
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        onPositiveAction(dialogView, dialog)
    }
}