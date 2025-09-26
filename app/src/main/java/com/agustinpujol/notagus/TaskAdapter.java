package com.agustinpujol.notagus;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.content.res.ColorStateList;
import androidx.core.graphics.ColorUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.recyclerview.widget.RecyclerView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Adapter del RecyclerView de tareas con soporte de SUBTAREAS inline.
 * Orden: fijadas primero, luego "Único día", luego el resto.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface OnEmptyListener { void onEmpty(boolean isEmpty); }
    public interface OnDeleteListener { void onDelete(Task task); }

    /** Proveedor del dayKey actualmente seleccionado (AAAAMMDD). */
    public interface DayKeyProvider { Integer getSelectedDayKey(); }

    private DayKeyProvider dayKeyProvider;
    public void setDayKeyProvider(DayKeyProvider provider) { this.dayKeyProvider = provider; }

    private final List<Task> data;
    private final OnEmptyListener onEmptyListener;
    private final OnDeleteListener onDeleteListener;

    private final Set<Long> expandedTaskIds = new HashSet<>();

    // DB lazy
    private AppDatabase db;
    private SubtaskDao subtaskDao;

    // ===== Estado de edición =====
    private long editingTaskId = -1L;
    private WeakReference<VH> editingVHRef = null;

    // ===== Hook al RecyclerView =====
    private RecyclerView attachedRv;
    public void attachRecyclerView(RecyclerView rv) { this.attachedRv = rv; }

    public boolean isEditing() { return editingTaskId != -1L; }
    public void commitActiveEdit() { commitEditingIfNeeded(true); }

    // Repetition constants (ajustá si usás otros)
    private static final int REPEAT_SINGLE = 0;  // Único día
    private static final int REPEAT_ALWAYS = 1;  // Siempre

    public TaskAdapter(List<Task> data, OnEmptyListener onEmptyListener, OnDeleteListener onDeleteListener) {
        this.data = data;
        this.onEmptyListener = onEmptyListener;
        this.onDeleteListener = onDeleteListener;
        setHasStableIds(false);
        resortPinnedThenSingleDay(); // orden inicial
    }

    // ---------- API pública ----------
    public void addTaskAndSort(@NonNull Task t) {
        data.add(t);
        resortPinnedThenSingleDay();
    }

    public void setTasksAndSort(@NonNull List<Task> list) {
        data.clear();
        data.addAll(list);
        resortPinnedThenSingleDay();
    }

    public void updateTaskAndSort(@NonNull Task updated) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId() == updated.getId()) {
                data.set(i, updated);
                break;
            }
        }
        resortPinnedThenSingleDay();
    }

    /**
     * Maneja la tecla BACK desde la Activity.
     * @return true si consumió el back (colapsó editor/subtareas), false si no hizo nada.
     */
    public boolean handleBack() {
        // 1) Si estoy editando el título, confirmar/cerrar
        if (editingTaskId != -1L) {
            commitEditingIfNeeded(true);
            return true;
        }

        // 2) Si hay alguna fila con "nuevo subtask" visible, cerrarla y ocultar teclado
        boolean closedNewEditors = false;
        if (attachedRv != null) {
            for (int i = 0; i < attachedRv.getChildCount(); i++) {
                View item = attachedRv.getChildAt(i);
                RecyclerView.ViewHolder vh = attachedRv.getChildViewHolder(item);
                if (vh instanceof VH) {
                    VH h = (VH) vh;
                    if (h.rowNewSubtask.getVisibility() == View.VISIBLE) {
                        h.etNewSubtask.setText("");
                        h.rowNewSubtask.setVisibility(View.GONE);
                        hideKeyboard(h.etNewSubtask);
                        closedNewEditors = true;
                    }
                }
            }
        }
        if (closedNewEditors) return true;

        // 3) Si hay tareas expandida(s), colapsarlas
        if (!expandedTaskIds.isEmpty()) {
            expandedTaskIds.clear();
            if (attachedRv != null) {
                for (int i = 0; i < attachedRv.getChildCount(); i++) {
                    View item = attachedRv.getChildAt(i);
                    RecyclerView.ViewHolder vh = attachedRv.getChildViewHolder(item);
                    if (vh instanceof VH) {
                        VH h = (VH) vh;
                        h.listSubtasks.removeAllViews();
                        h.rowNewSubtask.setVisibility(View.GONE);
                        h.containerSubtasks.setVisibility(View.GONE);
                        if (h.btnExpand.getVisibility() == View.VISIBLE) {
                            h.btnExpand.setRotation(0f);
                        }
                    }
                }
            }
            return true;
        }

        // 4) Nada para cerrar
        return false;
    }
    // ------------------------------------------

    /** Orden: fijadas primero, luego "Único día", luego el resto (estable). */
    private void resortPinnedThenSingleDay() {
        Collections.sort(data, new Comparator<Task>() {
            @Override public int compare(Task a, Task b) {
                if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                boolean aSingle = isSingleDay(a);
                boolean bSingle = isSingleDay(b);
                if (aSingle != bSingle) return aSingle ? -1 : 1;
                return 0; // TimSort estable mantiene orden relativo
            }
        });
        notifyDataSetChanged();
    }

    private boolean isSingleDay(@NonNull Task t) {
        try {
            java.lang.reflect.Method m = t.getClass().getMethod("isSingleDay");
            Object r = m.invoke(t);
            if (r instanceof Boolean) return (Boolean) r;
        } catch (Exception ignored) { }
        try {
            java.lang.reflect.Method m = t.getClass().getMethod("getRepeatMode");
            Object r = m.invoke(t);
            if (r instanceof Integer) return ((Integer) r) == REPEAT_SINGLE;
        } catch (Exception ignored) { }
        return false;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        if (db == null) {
            db = DatabaseClient.getInstance(h.itemView.getContext()).getAppDatabase();
            subtaskDao = db.subtaskDao();
        }

        final Task t = data.get(position);

        // Paleta
        SettingsManager.ThemeMode mode = SettingsManager.getInstance(h.itemView.getContext()).getThemeMode();
        ThemePalettes.Colors cols = ThemePalettes.forMode(mode);
        int onContent = cols.textOnContent;
        int accent    = cols.header;

        // Colores base
        h.tvTitle.setTextColor(onContent);
        h.etTitle.setTextColor(onContent);
        h.etTitle.setHintTextColor(ColorUtils.setAlphaComponent(onContent, 150));
        h.tvAddSubtask.setTextColor(onContent);
        h.btnMore.setImageTintList(ColorStateList.valueOf(onContent));
        h.etNewSubtask.setTextColor(onContent);
        h.etNewSubtask.setHintTextColor(ColorUtils.setAlphaComponent(onContent, 150));

        // Checkbox tint
        int unchecked = ColorUtils.setAlphaComponent(onContent, 160);
        int disabled  = ColorUtils.setAlphaComponent(onContent, 90);
        ColorStateList cbTint = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_enabled, android.R.attr.state_checked},
                        new int[]{android.R.attr.state_enabled, -android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_enabled}
                },
                new int[]{ accent, unchecked, disabled }
        );
        CompoundButtonCompat.setButtonTintList(h.cbDone, cbTint);

        // Check y estilo
        h.cbDone.setOnCheckedChangeListener(null);
        h.cbDone.setChecked(t.isDone());
        applyStrike(h.tvTitle, t.isDone());
        h.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            t.setDone(isChecked);
            applyStrike(h.tvTitle, isChecked);

            // ✅ Lógica especial para PERMANENTES:
            if (t.getRepeatMode() == REPEAT_ALWAYS) {
                Integer dk = (dayKeyProvider != null) ? dayKeyProvider.getSelectedDayKey() : null;
                Integer completed = isChecked ? dk : null;
                t.setCompletedAtDayKey(completed);
                Executors.newSingleThreadExecutor().execute(() ->
                        db.taskDao().updateDoneAndCompletion(t.getId(), isChecked, completed)
                );
            } else {
                // ÚNICO DÍA: se mantiene comportamiento previo
                Executors.newSingleThreadExecutor().execute(() ->
                        db.taskDao().updateDone(t.getId(), isChecked)
                );
            }
        });

        // Pin
        if (h.imgCornerPin != null) {
            h.imgCornerPin.setVisibility(t.isPinned() ? View.VISIBLE : View.GONE);
            if (t.isPinned()) ensurePinDrawableTint(h, onContent);
        }

        // Puntito Único día
        boolean singleDay = isSingleDay(t);
        if (h.ivOneDayDot != null) {
            h.ivOneDayDot.setVisibility(singleDay ? View.VISIBLE : View.GONE);
            h.ivOneDayDot.setImageTintList(ColorStateList.valueOf(onContent));
        }

        // Título
        h.tvTitle.setText(t.getTitle());
        h.etTitle.setText(t.getTitle());

        // Filtro sin saltos
        InputFilter noNewLines = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) if (source.charAt(i) == '\n') return "";
            return null;
        };
        h.etTitle.setFilters(new InputFilter[]{ noNewLines });

        // IME Done
        h.etTitle.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commitEditingIfNeeded(true);
                hideKeyboard(v);
                return true;
            }
            return false;
        });

        // Perder foco = guardar
        h.etTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                commitEditingIfNeeded(true);
                hideKeyboard(v);
            }
        });

        if (t.getId() == editingTaskId) showEditing(h); else showReading(h);

        // Expand subtareas
        h.btnExpand.setVisibility(View.GONE);
        h.btnExpand.setImageTintList(ColorStateList.valueOf(onContent));
        h.btnExpand.setRotation(0f);
        refreshExpandIndicator(h, t.getId(), onContent);

        // ⬇️ Pasamos accent para poder recargar subtareas al expandir
        h.btnExpand.setOnClickListener(v -> toggleExpand(h, t, onContent, accent));
        h.tvTitle.setOnClickListener(v -> toggleExpand(h, t, onContent, accent));

        // ===== Menú ⋮ =====
        h.btnMore.setOnClickListener(v -> {
            final LayoutInflater inflater = LayoutInflater.from(v.getContext());
            FrameLayout dummyRoot = new FrameLayout(v.getContext());
            View content = inflater.inflate(R.layout.bottom_sheet_blank, dummyRoot, false);

            final PopupWindow popup = new PopupWindow(
                    content,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            popup.setOutsideTouchable(true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                popup.setElevation(24f);
            }

            content.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int popupW = content.getMeasuredWidth();
            int popupH = content.getMeasuredHeight();

            android.graphics.Rect windowRect = new android.graphics.Rect();
            v.getWindowVisibleDisplayFrame(windowRect);

            int[] loc = new int[2];
            v.getLocationOnScreen(loc);
            int anchorY = loc[1];
            int anchorH = v.getHeight();

            int espacioAbajo = windowRect.bottom - (anchorY + anchorH);
            boolean mostrarArriba = espacioAbajo < popupH + dpToPx(v.getContext(), 8);
            int xoff = v.getWidth() - popupW;
            int yoff = mostrarArriba ? - (popupH + anchorH) - dpToPx(v.getContext(), 4) : dpToPx(v.getContext(), 4);

            // Editar
            View itemEdit = content.findViewById(R.id.itemEdit);
            if (itemEdit != null) {
                itemEdit.setOnClickListener(v2 -> {
                    popup.dismiss();
                    commitEditingIfNeeded(true);
                    beginEditing(h, t);
                });
            }

            // Agregar subtarea
            View itemAddSub = content.findViewById(R.id.itemAddSubtask);
            if (itemAddSub != null) {
                itemAddSub.setOnClickListener(v2 -> {
                    expandedTaskIds.add(t.getId());
                    h.containerSubtasks.setVisibility(View.VISIBLE);
                    renderSubtasks(h, t.getId(), onContent, accent);
                    h.rowNewSubtask.setVisibility(View.VISIBLE);
                    h.etNewSubtask.requestFocus();

                    h.etNewSubtask.post(() -> {
                        Rect r = new Rect(0, h.etNewSubtask.getTop(), h.etNewSubtask.getRight(), h.etNewSubtask.getBottom());
                        h.itemView.requestRectangleOnScreen(r, true);
                        showKeyboard(h.etNewSubtask);
                    });

                    if (h.btnExpand.getVisibility() == View.VISIBLE) h.btnExpand.setRotation(180f);
                    popup.dismiss();
                });
            }

            // Fijar / Desfijar
            View itemPin = content.findViewById(R.id.itemPinTask);
            TextView tvPinLabel = (itemPin != null) ? itemPin.findViewById(R.id.tvPinLabel) : null;
            if (itemPin != null) {
                if (tvPinLabel != null) tvPinLabel.setText(t.isPinned() ? "Desfijar tarea" : "Fijar tarea");
                itemPin.setOnClickListener(v2 -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (t.isPinned()) {
                            db.taskDao().unpin(t.getId());
                            ((Activity) v.getContext()).runOnUiThread(() -> {
                                t.setPinned(false);
                                resortPinnedThenSingleDay();
                            });
                        } else {
                            db.runInTransaction(() -> {
                                db.taskDao().clearAllPins();
                                db.taskDao().setPinned(t.getId());
                            });
                            ((Activity) v.getContext()).runOnUiThread(() -> {
                                for (Task it : data) it.setPinned(false);
                                t.setPinned(true);
                                resortPinnedThenSingleDay();
                            });
                        }
                    });
                    popup.dismiss();
                });
            }

            // Eliminar
            View itemDelete = content.findViewById(R.id.itemDelete);
            if (itemDelete != null) {
                itemDelete.setOnClickListener(v2 -> {
                    int pos = h.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        Task toDelete = data.get(pos);
                        new ConfirmDeleteDialog(() -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                subtaskDao().deleteByTaskId(toDelete.getId());
                                db.taskDao().delete(toDelete);
                            });
                            data.remove(pos);
                            resortPinnedThenSingleDay();
                            if (data.isEmpty() && onEmptyListener != null) onEmptyListener.onEmpty(true);
                        }).show(((MainActivity) v.getContext()).getSupportFragmentManager(), "ConfirmDelete");
                    }
                    popup.dismiss();
                });
            }

            popup.showAsDropDown(v, xoff, yoff);
        });

        // Subtareas
        boolean expanded = expandedTaskIds.contains(t.getId());
        h.containerSubtasks.setVisibility(expanded ? View.VISIBLE : View.GONE);
        h.rowNewSubtask.setVisibility(View.GONE);
        if (h.btnExpand.getVisibility() == View.VISIBLE) {
            h.btnExpand.setRotation(expanded ? 180f : 0f);
        }

        h.tvAddSubtask.setOnClickListener(v -> {
            expandedTaskIds.add(t.getId());
            h.containerSubtasks.setVisibility(View.VISIBLE);
            renderSubtasks(h, t.getId(), onContent, accent);
            h.rowNewSubtask.setVisibility(View.VISIBLE);
            h.etNewSubtask.requestFocus();

            h.etNewSubtask.post(() -> {
                Rect r = new Rect(0, h.etNewSubtask.getTop(), h.etNewSubtask.getRight(), h.etNewSubtask.getBottom());
                h.itemView.requestRectangleOnScreen(r, true);
                showKeyboard(h.etNewSubtask);
            });

            if (h.btnExpand.getVisibility() == View.VISIBLE) h.btnExpand.setRotation(180f);
        });

        h.btnSaveSubtask.setOnClickListener(v -> saveNewSubtaskFromEditor(h, t, onContent, accent));
        h.etNewSubtask.setOnEditorActionListener((tv, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveNewSubtaskFromEditor(h, t, onContent, accent);
                return true;
            }
            return false;
        });

        h.btnCancelSubtask.setOnClickListener(v -> {
            h.etNewSubtask.setText("");
            h.rowNewSubtask.setVisibility(View.GONE);
            hideKeyboard(h.etNewSubtask);
        });

        if (expanded) renderSubtasks(h, t.getId(), onContent, accent);
        else h.listSubtasks.removeAllViews();
    }

    @Override public int getItemCount() { return data.size(); }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        if ((editingVHRef != null && editingVHRef.get() == holder)) {
            commitEditingIfNeeded(true);
            hideKeyboard(holder.itemView);
        }
    }

    // ======= Helpers =======
    private void applyStrike(TextView tv, boolean strike) {
        if (strike) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(0.6f);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(1f);
        }
    }

    // ⚠️ CAMBIO: ahora recibe 'accent' y recarga subtareas al expandir
    private void toggleExpand(@NonNull VH h, @NonNull Task t, int onContent, int accent) {
        boolean expandedNow = expandedTaskIds.contains(t.getId());
        if (expandedNow) {
            expandedTaskIds.remove(t.getId());
            h.listSubtasks.removeAllViews();
            h.rowNewSubtask.setVisibility(View.GONE);
            h.containerSubtasks.setVisibility(View.GONE);
            if (h.btnExpand.getVisibility() == View.VISIBLE) h.btnExpand.setRotation(0f);
        } else {
            expandedTaskIds.add(t.getId());
            h.containerSubtasks.setVisibility(View.VISIBLE);
            // ✅ Recargar desde DB al volver a abrir
            renderSubtasks(h, t.getId(), onContent, accent);
            if (h.btnExpand.getVisibility() == View.VISIBLE) h.btnExpand.setRotation(180f);
        }
    }

    private void saveNewSubtaskFromEditor(VH h, Task task, int onContent, int accent) {
        String title = h.etNewSubtask.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(h.itemView.getContext(), "Escribí un nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            Subtask s = new Subtask(task.getId(), title, false, 0);
            long id = subtaskDao.insert(s);
            s.id = id;
            ((Activity) h.itemView.getContext()).runOnUiThread(() -> {
                h.etNewSubtask.setText("");
                h.rowNewSubtask.setVisibility(View.GONE);
                hideKeyboard(h.etNewSubtask);
                addOneSubtaskRow(h, s, onContent, accent);
                refreshExpandIndicator(h, task.getId(), onContent);
            });
        });
    }

    private void renderSubtasks(VH h, long taskId, int onContent, int accent) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Subtask> subtasks = subtaskDao.getByTaskId(taskId);
            ((Activity) h.itemView.getContext()).runOnUiThread(() -> {
                h.listSubtasks.removeAllViews();
                for (Subtask s : subtasks) addOneSubtaskRow(h, s, onContent, accent);
            });
        });
    }

    private void addOneSubtaskRow(VH h, Subtask s, int onContent, int accent) {
        LayoutInflater inflater = LayoutInflater.from(h.itemView.getContext());
        View row = inflater.inflate(R.layout.item_subtask, h.listSubtasks, false);

        CheckBox cb = row.findViewById(R.id.cbSubDone);
        TextView tv = row.findViewById(R.id.tvSubTitle);
        ImageButton btnDel = row.findViewById(R.id.btnSubDelete);

        tv.setTextColor(onContent);
        btnDel.setImageTintList(ColorStateList.valueOf(onContent));

        tv.setText(s.title);
        cb.setChecked(s.done);
        applyStrike(tv, s.done);

        int unchecked = ColorUtils.setAlphaComponent(onContent, 160);
        int disabled  = ColorUtils.setAlphaComponent(onContent, 90);
        ColorStateList subTint = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_enabled, android.R.attr.state_checked},
                        new int[]{android.R.attr.state_enabled, -android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_enabled}
                },
                new int[]{ accent, unchecked, disabled }
        );
        CompoundButtonCompat.setButtonTintList(cb, subTint);

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            s.done = isChecked;
            applyStrike(tv, isChecked);
            Executors.newSingleThreadExecutor().execute(() -> subtaskDao.updateDone(s.id, isChecked));
        });

        btnDel.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                subtaskDao.delete(s);
                ((Activity) h.itemView.getContext()).runOnUiThread(() -> {
                    h.listSubtasks.removeView(row);
                    refreshExpandIndicator(h, s.taskId, onContent);
                });
            });
        });
        h.listSubtasks.addView(row);
    }

    private void refreshExpandIndicator(VH h, long taskId, int onContent) {
        if (h.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            int subCount = subtaskDao.countByTaskId(taskId);
            ((Activity) h.itemView.getContext()).runOnUiThread(() -> {
                if (h.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;

                if (subCount > 0) {
                    h.btnExpand.setVisibility(View.VISIBLE);
                    h.btnExpand.setImageTintList(ColorStateList.valueOf(onContent));
                    boolean expanded = expandedTaskIds.contains(taskId);
                    h.btnExpand.setRotation(expanded ? 180f : 0f);
                } else {
                    h.btnExpand.setVisibility(View.GONE);
                    h.containerSubtasks.setVisibility(View.GONE);
                    h.rowNewSubtask.setVisibility(View.GONE);
                    expandedTaskIds.remove(taskId);
                }
            });
        });
    }

    // ===== Edición de título =====
    private void beginEditing(@NonNull VH h, @NonNull Task t) {
        editingTaskId = t.getId();
        editingVHRef = new WeakReference<>(h);
        h.etTitle.setText(t.getTitle());
        showEditing(h);

        int pos = h.getBindingAdapterPosition();
        if (attachedRv != null && pos != RecyclerView.NO_POSITION) {
            attachedRv.smoothScrollToPosition(pos);
        }

        h.etTitle.requestFocus();
        h.etTitle.setSelection(h.etTitle.getText().length());
        h.etTitle.post(() -> {
            Rect r = new Rect(0, h.etTitle.getTop(), h.etTitle.getRight(), h.etTitle.getBottom());
            h.itemView.requestRectangleOnScreen(r, true);

            InputMethodManager imm = (InputMethodManager) h.itemView.getContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(h.etTitle, InputMethodManager.SHOW_IMPLICIT);
                h.etTitle.postDelayed(() -> imm.showSoftInput(h.etTitle, InputMethodManager.SHOW_IMPLICIT), 40);
                h.etTitle.postDelayed(() -> imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0), 120);
            }
        });
    }

    private void showEditing(@NonNull VH h) {
        h.tvTitle.setVisibility(View.GONE);
        h.etTitle.setVisibility(View.VISIBLE);
    }

    private void showReading(@NonNull VH h) {
        h.etTitle.setVisibility(View.GONE);
        h.tvTitle.setVisibility(View.VISIBLE);
    }

    /** Guarda si hay edición activa (y opcionalmente cierra teclado). */
    private void commitEditingIfNeeded(boolean hideEditor) {
        if (editingTaskId == -1L) return;
        VH h = (editingVHRef != null) ? editingVHRef.get() : null;
        if (h == null) { editingTaskId = -1L; editingVHRef = null; return; }

        final int pos = h.getBindingAdapterPosition();
        if (pos == RecyclerView.NO_POSITION || pos >= data.size()) { editingTaskId = -1L; editingVHRef = null; return; }

        Task task = data.get(pos);
        String newTitle = h.etTitle.getText().toString().trim();

        if (TextUtils.isEmpty(newTitle)) {
            h.etTitle.setText(task.getTitle());
            if (hideEditor) {
                showReading(h);
                hideKeyboard(h.etTitle);
                editingTaskId = -1L; editingVHRef = null;
            }
            return;
        }

        if (!newTitle.equals(task.getTitle())) {
            task.setTitle(newTitle);
            h.tvTitle.setText(newTitle);
            Executors.newSingleThreadExecutor().execute(() -> db.taskDao().update(task));
            notifyItemChanged(pos);
        }

        if (hideEditor) {
            showReading(h);
            hideKeyboard(h.etTitle);
            editingTaskId = -1L;
            editingVHRef = null;
        }
    }

    private void hideKeyboard(View v) {
        WindowInsetsControllerCompat c = ViewCompat.getWindowInsetsController(v);
        if (c != null) {
            c.hide(WindowInsetsCompat.Type.ime());
        } else {
            InputMethodManager imm = (InputMethodManager) v.getContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void showKeyboard(View v) {
        WindowInsetsControllerCompat c = ViewCompat.getWindowInsetsController(v);
        if (c != null) {
            c.show(WindowInsetsCompat.Type.ime());
        } else {
            InputMethodManager imm = (InputMethodManager) v.getContext()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void ensurePinDrawableTint(@NonNull VH h, int color) {
        if (h.imgCornerPin == null) return;
        Drawable d = h.imgCornerPin.getDrawable();
        if (d == null) {
            h.imgCornerPin.setImageResource(R.drawable.tri_corner_pinned);
            d = h.imgCornerPin.getDrawable();
        }
        if (d != null) {
            Drawable wrapped = DrawableCompat.wrap(d.mutate());
            DrawableCompat.setTint(wrapped, color);
            h.imgCornerPin.setImageDrawable(wrapped);
        }
    }

    private static int dpToPx(android.content.Context ctx, int dp) {
        float d = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }

    public static class VH extends RecyclerView.ViewHolder {
        CheckBox cbDone;
        TextView tvTitle;
        EditText etTitle;
        AppCompatImageButton btnMore;
        AppCompatImageButton btnExpand;

        android.widget.ImageView imgCornerPin;
        android.widget.ImageView ivOneDayDot;

        LinearLayout containerSubtasks, listSubtasks, rowNewSubtask;
        TextView tvAddSubtask;
        EditText etNewSubtask;
        ImageButton btnSaveSubtask, btnCancelSubtask;

        public VH(@NonNull View itemView) {
            super(itemView);
            cbDone = itemView.findViewById(R.id.cbDone);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            etTitle = itemView.findViewById(R.id.etTitle);
            btnMore = itemView.findViewById(R.id.btnMore);
            btnExpand = itemView.findViewById(R.id.btnExpand);

            imgCornerPin = itemView.findViewById(R.id.imgCornerPin);
            ivOneDayDot  = itemView.findViewById(R.id.ivOneDayDot);

            containerSubtasks = itemView.findViewById(R.id.containerSubtasks);
            listSubtasks = itemView.findViewById(R.id.listSubtasks);
            rowNewSubtask = itemView.findViewById(R.id.rowNewSubtask);
            tvAddSubtask = itemView.findViewById(R.id.tvAddSubtask);
            etNewSubtask = itemView.findViewById(R.id.etNewSubtask);
            btnSaveSubtask = itemView.findViewById(R.id.btnSaveSubtask);
            btnCancelSubtask = itemView.findViewById(R.id.btnCancelSubtask);
        }
    }

    private SubtaskDao subtaskDao() { return subtaskDao; }
}
