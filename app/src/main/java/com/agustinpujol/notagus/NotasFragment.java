package com.agustinpujol.notagus;

import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotasFragment extends Fragment {

    private final Calendar headerDate = Calendar.getInstance();
    private TextView tvFecha;
    private AppCompatImageButton btnAyer, btnManana, btnSettings;
    private RecyclerView rvNotas;

    // DB
    private AppDatabase db;
    private ExecutorService dbExecutor;

    // Datos UI
    private final ArrayList<Note> notas = new ArrayList<>();
    private NotasAdapter adapter;

    // Drag & drop / delete
    private View trashTarget;
    private ImageView ivTrash;
    private ItemTouchHelper itemTouchHelper;
    private WeakReference<View> liftedCardRef;

    // Estado drag
    private boolean overTrashNow = false;
    private boolean hapticFired = false;
    @Nullable private RecyclerView.ViewHolder draggingVH = null;
    @Nullable private Note draggingNoteRef = null;

    public NotasFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Insets header
        View header = v.findViewById(R.id.headerContainer);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (view, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                view.setPadding(view.getPaddingLeft(), topInset, view.getPaddingRight(), view.getPaddingBottom());
                return insets;
            });
        }

        // Header refs
        tvFecha     = v.findViewById(R.id.tvFecha);
        btnSettings = v.findViewById(R.id.btnSettings);
        btnAyer     = v.findViewById(R.id.btnAyer);
        btnManana   = v.findViewById(R.id.btnManana);

        updateHeaderDate();
        if (btnSettings != null) {
            btnSettings.setOnClickListener(x ->
                    new SettingsDialog().show(getParentFragmentManager(), "SettingsDialog"));
        }
        if (btnAyer != null)  btnAyer.setOnClickListener(x -> { headerDate.add(Calendar.DAY_OF_MONTH, -1); updateHeaderDate(); });
        if (btnManana != null)btnManana.setOnClickListener(x -> { headerDate.add(Calendar.DAY_OF_MONTH,  1); updateHeaderDate(); });

        // Recycler
        rvNotas = v.findViewById(R.id.rvNotas);
        rvNotas.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        RecyclerView.ItemAnimator ia = rvNotas.getItemAnimator();
        if (ia instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) ia).setSupportsChangeAnimations(false);
        }
        rvNotas.setItemAnimator(null);
        rvNotas.setItemViewCacheSize(0);
        rvNotas.getRecycledViewPool().clear();

        // Target de eliminación
        trashTarget = v.findViewById(R.id.trashTarget);
        ivTrash     = v.findViewById(R.id.ivTrash);
        if (trashTarget != null) {
            trashTarget.setVisibility(View.GONE);
            ViewCompat.setOnApplyWindowInsetsListener(trashTarget, (view, insets) -> {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                int base = dp(6);
                lp.bottomMargin = base + bottomInset;
                view.setLayoutParams(lp);
                return insets;
            });

            SettingsManager sm = SettingsManager.getInstance(requireContext());
            ThemePalettes.Colors cols = ThemePalettes.forMode(sm.getThemeMode());
            int onContent = ColorUtils.calculateLuminance(cols.content) < 0.5 ? 0xFFFFFFFF : 0xFF000000;

            int fill   = ColorUtils.setAlphaComponent(onContent, 22);
            int stroke = ColorUtils.setAlphaComponent(onContent, 170);

            android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable();
            ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            ring.setColor(fill);
            ring.setStroke(dp(2), stroke);
            trashTarget.setBackground(ring);

            if (ivTrash != null) ivTrash.setImageTintList(android.content.res.ColorStateList.valueOf(stroke));
        }

        // Adapter
        adapter = new NotasAdapter(
                requireActivity(),
                notas,
                viewHolder -> itemTouchHelper.startDrag(viewHolder)
        );
        rvNotas.setAdapter(adapter);

        // ItemTouchHelper (mover y persistir al soltar)
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return 0;
                Note n = notas.get(pos);
                if (n.placeholder || pos == notas.size() - 1) return 0; // bloquear placeholder
                int drag = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(drag, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder source,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = source.getBindingAdapterPosition();
                int to   = target.getBindingAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false;
                if (to == notas.size() - 1 || notas.get(to).placeholder) return false;
                Collections.swap(notas, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    draggingVH = viewHolder;

                    int pos = viewHolder.getBindingAdapterPosition();
                    draggingNoteRef = (pos != RecyclerView.NO_POSITION && pos < notas.size()) ? notas.get(pos) : null;

                    View card = viewHolder.itemView;
                    liftedCardRef = new WeakReference<>(card);
                    card.setPressed(false);
                    card.setActivated(false);
                    card.animate().cancel();
                    card.animate()
                            .scaleX(1.04f).scaleY(1.04f)
                            .translationZ(dp(8))
                            .alpha(1f)
                            .setDuration(120)
                            .start();

                    if (trashTarget != null) {
                        trashTarget.setVisibility(View.VISIBLE);
                        trashTarget.setAlpha(1f);   // ← asegura que sea visible aunque viniera con alpha 0
                        trashTarget.setScaleX(1f);
                        trashTarget.setScaleY(1f);
                    }
                    overTrashNow = false;
                    hapticFired = false;
                    animateOverTrash(false);

                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    // Soltó tarjeta
                    if (overTrashNow && draggingNoteRef != null && !draggingNoteRef.placeholder && draggingNoteRef.id != 0) {
                        promptDeleteNote(draggingNoteRef);
                    }

                    animateOverTrash(false);
                    resetLiftAnimation();
                    if (trashTarget != null) trashTarget.setVisibility(View.GONE);

                    // Persistir el orden actual mostrado (ignorando placeholder final)
                    persistOrderToDb();

                    draggingVH = null;
                    draggingNoteRef = null;
                    overTrashNow = false;
                    hapticFired = false;
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                animateOverTrash(false);
                resetLiftAnimation();
                if (trashTarget != null) trashTarget.setVisibility(View.GONE);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive && trashTarget != null) {
                    int[] itemLoc   = new int[2];
                    int[] circleLoc = new int[2];
                    viewHolder.itemView.getLocationOnScreen(itemLoc);
                    trashTarget.getLocationOnScreen(circleLoc);

                    float itemCx = itemLoc[0] + viewHolder.itemView.getWidth()  / 2f;
                    float itemCy = itemLoc[1] + viewHolder.itemView.getHeight() / 2f;
                    float circleCx = circleLoc[0] + trashTarget.getWidth()  / 2f;
                    float circleCy = circleLoc[1] + trashTarget.getHeight() / 2f;

                    float dx = itemCx - circleCx;
                    float dy = itemCy - circleCy;
                    float dist = (float) Math.hypot(dx, dy);

                    float baseR = Math.min(trashTarget.getWidth(), trashTarget.getHeight()) / 2f * 0.95f;
                    float rEnter = baseR;
                    float rLeave = baseR * 1.15f;

                    boolean hit = overTrashNow ? (dist <= rLeave) : (dist <= rEnter);

                    if (hit && !overTrashNow) {
                        overTrashNow = true;
                        highlightTrash(true);
                        animateOverTrash(true);
                        if (!hapticFired) {
                            trashTarget.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                            hapticFired = true;
                        }
                    } else if (!hit && overTrashNow) {
                        overTrashNow = false;
                        highlightTrash(false);
                        animateOverTrash(false);
                        hapticFired = false;
                    }
                }
            }

            @Override public boolean isLongPressDragEnabled() { return false; }
            @Override public boolean isItemViewSwipeEnabled()  { return false; }
        });
        itemTouchHelper.attachToRecyclerView(rvNotas);

        // DB
        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
        dbExecutor = Executors.newSingleThreadExecutor();

        // Cargar notas (orden persistente)
        dbExecutor.execute(() -> {
            List<Note> persisted = db.noteDao().getAllOrdered();
            requireActivity().runOnUiThread(() -> {
                notas.clear();
                notas.addAll(persisted);
                ensurePlaceholderAtEnd();
                adapter.notifyDataSetChanged();
            });
        });

        // Resultado editor
        getParentFragmentManager().setFragmentResultListener(
                "nota_edit",
                getViewLifecycleOwner(),
                (key, result) -> {
                    int position  = result.getInt("position", -1);
                    long noteId   = result.getLong("noteId", 0L);
                    String title  = result.getString("title", "");
                    String body   = result.getString("body", "");

                    if (position < 0 || position >= notas.size()) return;

                    if (noteId == 0L) {
                        // INSERT con sortIndex = max+1
                        dbExecutor.execute(() -> {
                            int max = db.noteDao().getMaxSortIndex();
                            Note toInsert = new Note();
                            toInsert.title = title;
                            toInsert.body  = body;
                            long now = System.currentTimeMillis();
                            toInsert.createdAt = now;
                            toInsert.updatedAt = now;
                            toInsert.sortIndex = max + 1;

                            long newId = db.noteDao().insert(toInsert);
                            toInsert.id = newId;

                            requireActivity().runOnUiThread(() -> {
                                notas.set(position, toInsert);
                                ensurePlaceholderAtEnd();
                                adapter.notifyDataSetChanged();
                                rvNotas.scrollToPosition(Math.min(position + 1, notas.size() - 1));
                            });
                        });

                    } else {
                        // UPDATE normal
                        Note existing = notas.get(position);
                        if (!existing.placeholder) {
                            existing.title = title;
                            existing.body  = body;
                            existing.updatedAt = System.currentTimeMillis();
                        }
                        dbExecutor.execute(() -> {
                            db.noteDao().update(existing);
                            requireActivity().runOnUiThread(adapter::notifyDataSetChanged);
                        });
                    }
                }
        );

        ensureBottomSpaceForFooter(v);
        applyPalette(v);
    }

    /** Persistir el orden actual mostrado en el Recycler (ignora el placeholder final) */
    private void persistOrderToDb() {
        if (notas.isEmpty()) return;
        int limit = notas.size();
        if (limit > 0 && notas.get(limit - 1).placeholder) limit--;

        final ArrayList<Note> snapshot = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) snapshot.add(notas.get(i));

        dbExecutor.execute(() -> {
            db.runInTransaction(() -> {
                for (int i = 0; i < snapshot.size(); i++) {
                    Note n = snapshot.get(i);
                    if (n.id != 0 && n.sortIndex != i) {
                        db.noteDao().updateSortIndex(n.id, i);
                        n.sortIndex = i;
                    }
                }
            });
        });
    }

    /** Diálogo de confirmación para eliminar una NOTA */
    private void promptDeleteNote(@NonNull Note n) {
        new ConfirmDeleteDialog(
                "Confirmar eliminación",
                "¿Estás seguro de que deseas eliminar esta nota? Esta acción no se puede deshacer.",
                () -> {
                    int idx = notas.indexOf(n);
                    if (idx >= 0) {
                        notas.remove(idx);
                        adapter.notifyItemRemoved(idx);
                        ensurePlaceholderAtEnd();
                        adapter.notifyDataSetChanged();
                        deleteNote(n);
                    }
                }
        ).show(getParentFragmentManager(), "ConfirmDeleteNote");
    }

    private void highlightTrash(boolean on) {
        if (trashTarget == null) return;
        trashTarget.animate().scaleX(on ? 1.12f : 1f).scaleY(on ? 1.12f : 1f).setDuration(100).start();
        if (ivTrash != null) ivTrash.animate().scaleX(on ? 1.12f : 1f).scaleY(on ? 1.12f : 1f).setDuration(100).start();
    }

    private void animateOverTrash(boolean over) {
        if (draggingVH == null) return;
        View card = draggingVH.itemView;
        card.setPressed(false);
        card.setActivated(false);
        card.animate().cancel();
        if (over) {
            card.animate().scaleX(0.90f).scaleY(0.90f).alpha(0.35f).setDuration(120).start();
        } else {
            card.animate().scaleX(1.04f).scaleY(1.04f).alpha(1f).setDuration(120).start();
        }
    }

    /** Borra en DB y refresca UI */
    private void deleteNote(@NonNull Note n) {
        dbExecutor.execute(() -> {
            db.noteDao().delete(n);
            List<Note> refreshed = db.noteDao().getAllOrdered();
            requireActivity().runOnUiThread(() -> {
                notas.clear();
                notas.addAll(refreshed);
                ensurePlaceholderAtEnd();
                adapter.notifyDataSetChanged();
                Toast.makeText(requireContext(), "Nota eliminada", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void ensurePlaceholderAtEnd() {
        if (notas.isEmpty() || !notas.get(notas.size() - 1).placeholder) {
            notas.add(Note.placeholder());
        }
    }

    private void resetLiftAnimation() {
        View card = liftedCardRef != null ? liftedCardRef.get() : null;
        if (card != null) {
            card.animate().cancel();
            card.animate().scaleX(1f).scaleY(1f).translationZ(0f).alpha(1f).setDuration(120).start();
        }
        liftedCardRef = null;
    }

    // ===== Helpers de UI =====

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) applyPalette(v);
    }

    private void updateHeaderDate() {
        if (tvFecha != null) tvFecha.setText(getHeaderDateText(headerDate.getTime()));
    }

    private void applyPalette(@NonNull View v) {
        View header       = v.findViewById(R.id.headerContainer);
        View contentRoot  = v.findViewById(R.id.notasRoot);
        View contentArea  = v.findViewById(R.id.notasContent);

        SettingsManager sm = SettingsManager.getInstance(requireContext());
        ThemePalettes.Colors cols = ThemePalettes.forMode(sm.getThemeMode());

        if (header != null)      header.setBackgroundColor(cols.header);
        if (contentRoot != null) contentRoot.setBackgroundColor(cols.content);
        if (contentArea != null) contentArea.setBackgroundColor(cols.content);

        int textOnHeader = ColorUtils.calculateLuminance(cols.header) < 0.5 ? 0xFFFFFFFF : 0xFF000000;
        if (tvFecha != null) tvFecha.setTextColor(textOnHeader);
        if (btnAyer != null)     btnAyer.setImageTintList(android.content.res.ColorStateList.valueOf(textOnHeader));
        if (btnManana != null)   btnManana.setImageTintList(android.content.res.ColorStateList.valueOf(textOnHeader));
        if (btnSettings != null) btnSettings.setImageTintList(android.content.res.ColorStateList.valueOf(textOnHeader));
    }

    private String getHeaderDateText(Date d) {
        Locale esAR = new Locale("es", "AR");
        SimpleDateFormat sdfDia   = new SimpleDateFormat("EEEE", esAR);
        SimpleDateFormat sdfFecha = new SimpleDateFormat("d 'de' MMMM", esAR);
        String dia   = capitalizeFirst(sdfDia.format(d));
        String fecha = sdfFecha.format(d);
        return dia + "\n" + fecha;
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        int cp = s.codePointAt(0);
        String first = new String(Character.toChars(cp));
        String rest  = s.substring(first.length());
        return first.toUpperCase(new Locale("es", "AR")) + rest;
    }

    private int dp(int v) {
        return (int) (v * requireContext().getResources().getDisplayMetrics().density);
    }

    // ===== Footer spacing =====
    private void ensureBottomSpaceForFooter(@NonNull View root) {
        final View contentArea = root.findViewById(R.id.notasContent);
        if (contentArea == null) return;

        int tmpPad = 0;
        final int dimenId = getDimenIdSafely("bottom_nav_height", -1);
        if (dimenId != -1) tmpPad = getResources().getDimensionPixelSize(dimenId);
        if (tmpPad <= 0) tmpPad = (int) (72 * requireContext().getResources().getDisplayMetrics().density);

        final int baseBottomPad = tmpPad;

        contentArea.setPadding(
                contentArea.getPaddingLeft(),
                contentArea.getPaddingTop(),
                contentArea.getPaddingRight(),
                baseBottomPad
        );

        ViewCompat.setOnApplyWindowInsetsListener(contentArea, (view, insets) -> {
            int sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    baseBottomPad + sysBottom
            );
            return insets;
        });
    }

    private int getDimenIdSafely(String dimenName, int fallbackId) {
        int id = getResources().getIdentifier(dimenName, "dimen", requireContext().getPackageName());
        return id != 0 ? id : fallbackId;
    }
}
