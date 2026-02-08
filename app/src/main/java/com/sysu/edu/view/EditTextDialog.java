package com.sysu.edu.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sysu.edu.databinding.DialogEditTextBinding;

import java.util.Objects;

public class EditTextDialog {

    private final AlertDialog dialog;
    private final @NonNull DialogEditTextBinding binding;

    String mValue = "";
    ValueChangeListener listener;

    public EditTextDialog(Context context) {
        binding = DialogEditTextBinding.inflate(LayoutInflater.from(context));
        dialog = new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (_, _) -> setValue(binding.edit.getText() == null ? "" : binding.edit.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        if (!Objects.equals(mValue, value)) {
            mValue = value;
            binding.edit.setText(value);
            if (listener != null) {
                listener.onValueChange(value);
            }
        }
    }

    public void show() {
        dialog.show();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(72, 32, 72, 0);
        binding.getRoot().setLayoutParams(params);
    }

    public void setTitle(int title) {
        dialog.setTitle(title);
    }

    public void setTitle(String title) {
        dialog.setTitle(title);
    }

    public void setHint(int hint) {
        binding.getRoot().setHint(hint);
    }

    public void setHint(String hint) {
        binding.getRoot().setHint(hint);
        binding.edit.setContentDescription(hint);
    }

    public void setValueChangeListener(ValueChangeListener listener) {
        this.listener = listener;
    }

    public String getText() {
        return binding.edit.getText() == null ? "" : binding.edit.getText().toString();
    }

    public AlertDialog getDialog() {
        return dialog;
    }

    public interface ValueChangeListener {
        void onValueChange(String value);
    }
}
