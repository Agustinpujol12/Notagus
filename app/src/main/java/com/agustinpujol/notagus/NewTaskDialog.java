package com.agustinpujol.notagus;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.DialogFragment;

import com.agustinpujol.notagus.ThemePalettes;
import com.agustinpujol.notagus.SettingsManager;
import com.agustinpujol.notagus.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class NewTaskDialog extends DialogFragment {

    public interface OnCreateTaskListener {
        void onCreateTask(String title, int repeatMode, @Nullable Integer dayKey);
    }

    private final OnCreateTaskListener listener;
    private final @Nullable Integer dayKeyFromHeader;

    public NewTaskDialog(OnCreateTaskListener listener, @Nullable Integer dayKeyFromHeader) {
        this.listener = listener;
        this.dayKeyFromHeader = dayKeyFromHeader;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_task, null, false);

        // Refs
        View header = view.findViewById(R.id.newTaskHeader);
        View body   = view.findViewById(R.id.newTaskBody);

        TextView tvTitulo         = view.findViewById(R.id.tvTitulo);
        ImageButton btnClose      = view.findViewById(R.id.btnClose);
        TextView tvLabel          = view.findViewById(R.id.tvLabel);
        TextView tvDuracionTitulo = view.findViewById(R.id.tvDuracionTitulo);

        TextInputLayout tilNombre  = view.findViewById(R.id.tilNombre);
        TextInputEditText etNombre = view.findViewById(R.id.etNombre);

        Chip chipSoloDia   = view.findViewById(R.id.chipSoloDia);
        Chip chipTodosDias = view.findViewById(R.id.chipTodosDias);

        FloatingActionButton fabConfirm = view.findViewById(R.id.fabConfirm);

        // Paleta
        ThemePalettes.Colors cols = ThemePalettes.forMode(
                SettingsManager.getInstance(requireContext()).getThemeMode()
        );
        int onHeader  = cols.textOnHeader;
        int onContent = cols.textOnContent;

        // Fondos
        if (header != null) header.setBackgroundColor(cols.header);
        if (body   != null) body.setBackgroundColor(cols.content);

        // Textos
        if (tvTitulo != null) tvTitulo.setTextColor(onHeader);
        if (btnClose != null) btnClose.setImageTintList(ColorStateList.valueOf(onHeader));
        if (tvLabel  != null) tvLabel.setTextColor(onContent);
        if (tvDuracionTitulo != null) tvDuracionTitulo.setTextColor(onContent);

        // Input: hint y borde por estado (incluye estado de error -> rojo)
        if (tilNombre != null) {
            int hint        = ColorUtils.setAlphaComponent(onContent, 150);
            int strokeIdle  = ColorUtils.setAlphaComponent(onContent, 120);
            int strokeFocus = cols.header;
            int strokeError = ContextCompat.getColor(requireContext(), R.color.delete_red);

            tilNombre.setHintTextColor(ColorStateList.valueOf(hint));
            tilNombre.setDefaultHintTextColor(ColorStateList.valueOf(hint));
            tilNombre.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);

            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_focused},
                    new int[]{-android.R.attr.state_focused, com.google.android.material.R.attr.state_error},
                    new int[]{}
            };
            int[] colors = new int[]{ strokeFocus, strokeError, strokeIdle };
            tilNombre.setBoxStrokeColorStateList(new ColorStateList(states, colors));
        }
        if (etNombre != null) {
            etNombre.setTextColor(onContent);
            etNombre.setHintTextColor(ColorUtils.setAlphaComponent(onContent, 150));

            // 👉 Dar foco y abrir teclado automáticamente
            etNombre.requestFocus();
            etNombre.post(() -> {
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etNombre, InputMethodManager.SHOW_IMPLICIT);
            });
        }

        // Chips: marcado = header/onHeader, desmarcado = content/onContent
        styleDurationChips(chipSoloDia, chipTodosDias, cols, onHeader, onContent);

        // Mutual exclusión (por las dudas)
        chipSoloDia.setOnClickListener(v -> { chipSoloDia.setChecked(true); chipTodosDias.setChecked(false); });
        chipTodosDias.setOnClickListener(v -> { chipTodosDias.setChecked(true); chipSoloDia.setChecked(false); });

        // FAB (respetamos el verde del XML: NO sobreescribir backgroundTint aquí)
        if (fabConfirm != null) {
            fabConfirm.setImageTintList(ColorStateList.valueOf(0xFFFFFFFF));
            fabConfirm.setOnClickListener(v -> {
                String nombre = (etNombre != null && etNombre.getText() != null)
                        ? etNombre.getText().toString().trim() : "";

                if (nombre.isEmpty()) {
                    if (tilNombre != null) tilNombre.setError(getString(R.string.name_required));
                    return;
                }
                if (tilNombre != null) tilNombre.setError(null);

                int repeatMode = chipSoloDia.isChecked() ? Task.REPEAT_ONE_DAY : Task.REPEAT_ALWAYS;
                Integer dk = (repeatMode == Task.REPEAT_ONE_DAY) ? dayKeyFromHeader : null;

                if (listener != null) listener.onCreateTask(capitalizeFirst(nombre), repeatMode, dk);
                dismiss();
            });
        }

        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        // Diálogo
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            Window w = dialog.getWindow();

            // 👉 Fuerza el estado del teclado visible al abrir
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95f);
            w.setAttributes(lp);
        }
        return dialog;
    }

    private void styleDurationChips(@NonNull Chip chipSoloDia,
                                    @NonNull Chip chipTodosDias,
                                    @NonNull ThemePalettes.Colors cols,
                                    int onHeader,
                                    int onContent) {

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };

        ColorStateList chipBg     = new ColorStateList(states, new int[]{ cols.header, cols.content });
        ColorStateList chipText   = new ColorStateList(states, new int[]{ onHeader, onContent });
        ColorStateList chipStroke = new ColorStateList(states, new int[]{ Color.TRANSPARENT, Color.TRANSPARENT });

        applyChipStyle(chipSoloDia,   chipBg, chipText, chipStroke, false);
        applyChipStyle(chipTodosDias, chipBg, chipText, chipStroke, true); // “Siempre” preseleccionado
    }

    private void applyChipStyle(@NonNull Chip chip,
                                @NonNull ColorStateList bg,
                                @NonNull ColorStateList text,
                                @NonNull ColorStateList stroke,
                                boolean preChecked) {
        chip.setChipBackgroundColor(bg);
        chip.setTextColor(text);
        chip.setChipStrokeColor(stroke);
        chip.setChipStrokeWidth(0);
        chip.setCheckedIconVisible(false);
        chip.setMinHeight(dp(40));
        chip.setChecked(preChecked);
        chip.setPadding(dp(14), chip.getPaddingTop(), dp(14), chip.getPaddingBottom());
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        int cp = s.codePointAt(0);
        String first = new String(Character.toChars(cp));
        String rest  = s.substring(first.length());
        return first.toUpperCase(new java.util.Locale("es", "AR")) + rest;
    }

    private int dp(int value) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return (int) (value * d + 0.5f);
    }
}
