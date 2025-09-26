package com.agustinpujol.notagus;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ConfirmDeleteDialog extends DialogFragment {

    public interface OnConfirmListener {
        void onConfirm();
    }

    private final OnConfirmListener listener;

    // Texto personalizable
    @Nullable private final String title;
    @Nullable private final String message;

    // Constructor original (compatibilidad): texto por defecto para tareas
    public ConfirmDeleteDialog(OnConfirmListener listener) {
        this(null, null, listener);
    }

    // Nuevo constructor: permite pasar título y mensaje (por ej. “nota”)
    public ConfirmDeleteDialog(@Nullable String title,
                               @Nullable String message,
                               OnConfirmListener listener) {
        this.title = title;
        this.message = message;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String defaultTitle = "Confirmar eliminación";
        String defaultMsg   = "¿Estás seguro de que deseas eliminar esta tarea? Esta acción no se puede deshacer.";

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title != null ? title : defaultTitle)
                .setMessage(message != null ? message : defaultMsg)
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    if (listener != null) listener.onConfirm();
                })
                .create();
    }
}
