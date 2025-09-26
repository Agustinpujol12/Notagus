package com.agustinpujol.notagus;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialContainerTransform;

public class NotaEditorFragment extends Fragment {

    public static final String ARG_TRANSITION_NAME = "arg_transition_name";
    public static final String ARG_TITLE           = "arg_title";
    public static final String ARG_BODY            = "arg_body";
    public static final String ARG_POSITION        = "arg_position";
    public static final String ARG_NOTE_ID         = "arg_note_id";

    // Vistas detrás (lista y target de basura)
    private View rvNotasBehind;
    private View trashTargetBehind;

    public static NotaEditorFragment newInstance(String transitionName,
                                                 String title,
                                                 String body,
                                                 int position,
                                                 long noteId) {
        NotaEditorFragment f = new NotaEditorFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TRANSITION_NAME, transitionName);
        b.putString(ARG_TITLE, title);
        b.putString(ARG_BODY, body);
        b.putInt(ARG_POSITION, position);
        b.putLong(ARG_NOTE_ID, noteId);
        f.setArguments(b);
        return f;
    }

    public NotaEditorFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nota_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        final SettingsManager sm = SettingsManager.getInstance(requireContext());
        final ThemePalettes.Colors cols = ThemePalettes.forMode(sm.getThemeMode());

        // Buscar vistas del fragment de atrás (si no existen, quedan en null y no pasa nada)
        rvNotasBehind     = requireActivity().findViewById(R.id.rvNotas);
        trashTargetBehind = requireActivity().findViewById(R.id.trashTarget);

        final MaterialCardView cardRoot = v.findViewById(R.id.editorRootCard);
        final View editorRoot = v.findViewById(R.id.editorRoot);
        cardRoot.setCardBackgroundColor(cols.content);
        final int textOnContent = ColorUtils.calculateLuminance(cols.content) < 0.5 ? 0xFFFFFFFF : 0xFF000000;

        // Inputs: colores y estilos
        final int hintColor     = ColorUtils.setAlphaComponent(textOnContent, 150);
        final int strokeIdle    = ColorUtils.setAlphaComponent(textOnContent, 120);
        final int strokeFocused = cols.header;

        final TextInputLayout tilTitle = v.findViewById(R.id.tilTitle);
        final TextInputLayout tilBody  = v.findViewById(R.id.tilBody);
        final EditText etTitle = v.findViewById(R.id.etTitle);
        final EditText etBody  = v.findViewById(R.id.etBody);

        etTitle.setTextColor(textOnContent);
        etBody.setTextColor(textOnContent);

        tilTitle.setHintTextColor(ColorStateList.valueOf(hintColor));
        tilTitle.setDefaultHintTextColor(ColorStateList.valueOf(hintColor));
        tilBody.setHintTextColor(ColorStateList.valueOf(hintColor));
        tilBody.setDefaultHintTextColor(ColorStateList.valueOf(hintColor));

        tilTitle.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tilBody.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_focused},
                new int[]{}
        };
        int[] colors = new int[]{ strokeFocused, strokeIdle };
        ColorStateList strokeList = new ColorStateList(states, colors);
        tilTitle.setBoxStrokeColorStateList(strokeList);
        tilBody.setBoxStrokeColorStateList(strokeList);

        // Borde sutil de la card
        cardRoot.setStrokeColor(ColorUtils.setAlphaComponent(textOnContent, 60));
        cardRoot.setStrokeWidth((int) (v.getResources().getDisplayMetrics().density * 1)); // 1dp

        // Transición compartida sin scrim, color consistente
        final String transitionName = getArguments() != null ? getArguments().getString(ARG_TRANSITION_NAME) : null;
        if (transitionName != null) {
            cardRoot.setTransitionName(transitionName);

            final MaterialContainerTransform enter = new MaterialContainerTransform();
            enter.setDuration(280);
            enter.setScrimColor(0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                enter.setAllContainerColors(cols.content);
            }
            enter.setDrawingViewId(R.id.notasRoot);
            setSharedElementEnterTransition(enter);

            final MaterialContainerTransform ret = new MaterialContainerTransform();
            ret.setDuration(240);
            ret.setScrimColor(0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ret.setAllContainerColors(cols.content);
            }
            ret.setDrawingViewId(R.id.notasRoot);
            setSharedElementReturnTransition(ret);
        }

        final ImageButton btnClose = v.findViewById(R.id.btnClose);
        final ImageButton btnSave  = v.findViewById(R.id.btnSave);
        btnClose.setImageTintList(ColorStateList.valueOf(textOnContent));
        btnSave.setImageTintList(ColorStateList.valueOf(textOnContent));

        // Args
        final String initialTitle = getArguments() != null ? getArguments().getString(ARG_TITLE, "") : "";
        final String initialBody  = getArguments() != null ? getArguments().getString(ARG_BODY,  "") : "";
        final int position        = getArguments() != null ? getArguments().getInt(ARG_POSITION, -1) : -1;
        final long noteId         = getArguments() != null ? getArguments().getLong(ARG_NOTE_ID, 0L) : 0L;

        etTitle.setText(initialTitle);
        etBody.setText(initialBody);

        // Validación simple
        final TextWatcher validator = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validate(); }
            @Override public void afterTextChanged(Editable s) { validate(); }
            private void validate() {
                final String t = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                final String b = etBody.getText()  != null ? etBody.getText().toString().trim()  : "";
                tilTitle.setError(null); tilBody.setError(null);
                final boolean ok = !t.isEmpty() && !b.isEmpty();
                btnSave.setEnabled(ok);
                btnSave.setAlpha(ok ? 1f : 0.45f);
            }
        };
        etTitle.addTextChangedListener(validator);
        etBody.addTextChangedListener(validator);
        btnSave.setEnabled(false);
        btnSave.setAlpha(0.45f);

        // Mayúscula inicial
        etTitle.addTextChangedListener(new CapitalizeWatcher(etTitle));
        etBody.addTextChangedListener(new CapitalizeWatcher(etBody));

        // Mostrar/ocultar teclado según nueva/existente
        final boolean creatingNew = (noteId == 0L);
        if (creatingNew) {
            etTitle.requestFocus();
            etTitle.setSelection(etTitle.getText() != null ? etTitle.getText().length() : 0);
            v.postDelayed(() -> showKeyboard(etTitle), 120);
        } else {
            etTitle.clearFocus();
            etBody.clearFocus();
            editorRoot.setFocusable(true);
            editorRoot.setFocusableInTouchMode(true);
            editorRoot.requestFocus();
            v.post(this::hideKeyboard);
        }

        // Cerrar
        final View.OnClickListener closeEditor = click -> {
            hideKeyboard(v);
            getParentFragmentManager().popBackStack();
        };
        btnClose.setOnClickListener(closeEditor);

        // Guardar
        btnSave.setOnClickListener(click -> {
            final String t = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            final String b = etBody.getText()  != null ? etBody.getText().toString().trim()  : "";

            boolean valid = true;
            if (t.isEmpty()) { tilTitle.setError(getString(R.string.campo_obligatorio)); valid = false; }
            if (b.isEmpty()) { tilBody.setError(getString(R.string.campo_obligatorio));  valid = false; }
            if (!valid) return;

            final Bundle result = new Bundle();
            result.putInt("position", position);
            result.putLong("noteId", noteId);
            result.putString("title", t);
            result.putString("body", b);
            getParentFragmentManager().setFragmentResult("nota_edit", result);

            hideKeyboard(v);
            getParentFragmentManager().popBackStack();
        });

        // Ajuste dinámico con IME (teclado)
        final View bottomBar = v.findViewById(R.id.bottomBar);

        final int baseRootBottomPad = editorRoot.getPaddingBottom();
        final int baseBarPadTop     = bottomBar.getPaddingTop();
        final int baseBarPadBottom  = bottomBar.getPaddingBottom();
        final int btnBig            = dp(v, 56);
        final int btnSmall          = dp(v, 40);
        final int marginBig         = dp(v, 40);
        final int marginSmall       = dp(v, 12);
        final int padSmallTop       = dp(v, 2);
        final int padSmallBottom    = dp(v, 0);

        final ImageButton btnCloseRef = btnClose;
        final ImageButton btnSaveRef  = btnSave;

        ViewCompat.setOnApplyWindowInsetsListener(v, (vv, insets) -> {
            final int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

            if (ime > 0) {
                editorRoot.setPadding(
                        editorRoot.getPaddingLeft(),
                        editorRoot.getPaddingTop(),
                        editorRoot.getPaddingRight(),
                        baseRootBottomPad + ime
                );
                compactBar(bottomBar, btnCloseRef, btnSaveRef, btnSmall, marginSmall, padSmallTop, padSmallBottom);
            } else {
                editorRoot.setPadding(
                        editorRoot.getPaddingLeft(),
                        editorRoot.getPaddingTop(),
                        editorRoot.getPaddingRight(),
                        baseRootBottomPad
                );
                restoreBar(bottomBar, btnCloseRef, btnSaveRef, btnBig, marginBig, baseBarPadTop, baseBarPadBottom);
            }
            return insets;
        });
    }

    // --- Ocultar la lista solo mientras el editor está visible ---
    @Override
    public void onStart() {
        super.onStart();
        hideBehindDuringEditor();
    }

    @Override
    public void onResume() {
        super.onResume();
        hideBehindDuringEditor(); // por si se vuelve desde background
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        restoreBehindAfterEditor(); // al cerrar, que se vea la lista
    }

    private void hideBehindDuringEditor() {
        if (rvNotasBehind != null) {
            rvNotasBehind.animate().cancel();
            rvNotasBehind.setAlpha(0f);
            rvNotasBehind.setVisibility(View.INVISIBLE);
        }
        if (trashTargetBehind != null) {
            trashTargetBehind.animate().cancel();
            trashTargetBehind.setAlpha(0f);
            trashTargetBehind.setVisibility(View.GONE); // el editor nunca lo muestra
        }
    }

    private void restoreBehindAfterEditor() {
        if (rvNotasBehind != null) {
            rvNotasBehind.animate().cancel();
            rvNotasBehind.setVisibility(View.VISIBLE);
            rvNotasBehind.setAlpha(1f);
        }
        if (trashTargetBehind != null) {
            // No lo mostramos; lo controla NotasFragment al iniciar el drag.
            // Solo aseguramos alpha 1 para evitar el bug de quedar oculto.
            trashTargetBehind.animate().cancel();
            trashTargetBehind.setAlpha(1f);
        }
    }
    // --------------------------------------

    private void compactBar(View bar, ImageButton close, ImageButton save,
                            int btnSize, int sideMargin, int padTop, int padBottom) {
        LinearLayout.LayoutParams lpClose = (LinearLayout.LayoutParams) close.getLayoutParams();
        LinearLayout.LayoutParams lpSave  = (LinearLayout.LayoutParams) save.getLayoutParams();

        lpClose.width = lpClose.height = btnSize;
        lpSave.width  = lpSave.height  = btnSize;

        lpClose.setMarginEnd(sideMargin);
        lpSave.setMarginStart(sideMargin);

        close.setLayoutParams(lpClose);
        save.setLayoutParams(lpSave);

        bar.setPadding(bar.getPaddingLeft(), padTop, bar.getPaddingRight(), padBottom);
    }

    private void restoreBar(View bar, ImageButton close, ImageButton save,
                            int btnSize, int sideMargin, int padTop, int padBottom) {
        LinearLayout.LayoutParams lpClose = (LinearLayout.LayoutParams) close.getLayoutParams();
        LinearLayout.LayoutParams lpSave  = (LinearLayout.LayoutParams) save.getLayoutParams();

        lpClose.width = lpClose.height = btnSize;
        lpSave.width  = lpSave.height  = btnSize;

        lpClose.setMarginEnd(sideMargin);
        lpSave.setMarginStart(sideMargin);

        close.setLayoutParams(lpClose);
        save.setLayoutParams(lpSave);

        bar.setPadding(bar.getPaddingLeft(), padTop, bar.getPaddingRight(), padBottom);
    }

    private static class CapitalizeWatcher implements TextWatcher {
        private final EditText target;
        private boolean editing = false;

        CapitalizeWatcher(EditText target) { this.target = target; }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            if (editing) return;
            if (s.length() > 0 && Character.isLowerCase(s.charAt(0))) {
                editing = true;
                s.replace(0, 1, String.valueOf(Character.toUpperCase(s.charAt(0))));
                target.setSelection(s.length());
                editing = false;
            }
        }
    }

    private int dp(View v, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, v.getResources().getDisplayMetrics());
    }

    private void showKeyboard(View target) {
        if (target == null) return;
        target.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        View current = requireActivity().getCurrentFocus();
        if (current != null) current.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View root = getView();
        if (imm != null && root != null) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }

    private void hideKeyboard(View root) {
        View current = requireActivity().getCurrentFocus();
        if (current != null) current.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }
}
