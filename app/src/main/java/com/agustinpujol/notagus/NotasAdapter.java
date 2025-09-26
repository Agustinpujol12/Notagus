package com.agustinpujol.notagus;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotasAdapter extends RecyclerView.Adapter<NotasAdapter.NotaViewHolder> {

    public interface DragStartListener { void onStartDrag(RecyclerView.ViewHolder viewHolder); }

    // ID estable reservado para el placeholder
    private static final long PLACEHOLDER_STABLE_ID = Long.MIN_VALUE + 1L;

    private static final int TYPE_PLACEHOLDER = 1;
    private static final int TYPE_NOTE       = 2;

    private final Context context;
    private final List<Note> notas;
    private final DragStartListener dragStartListener;

    public NotasAdapter(Context context, List<Note> notas, DragStartListener dragStartListener) {
        this.context = context;
        this.notas = notas;
        this.dragStartListener = dragStartListener;
        setHasStableIds(true);
    }

    // ---------- IDs/TIPOS ----------
    @Override public long getItemId(int position) {
        Note n = notas.get(position);
        if (n.placeholder) return PLACEHOLDER_STABLE_ID;
        if (n.id == 0L) { // respaldo estable si aún no tiene id
            long h = 1125899906842597L;
            if (n.title != null)  h = 31*h + n.title.hashCode();
            h = 31*h + n.createdAt;
            return h;
        }
        return n.id;
    }
    @Override public int getItemViewType(int position) {
        return notas.get(position).placeholder ? TYPE_PLACEHOLDER : TYPE_NOTE;
    }

    // ---------- Helpers ----------
    public void replaceAll(List<Note> list) {
        notas.clear();
        notas.addAll(list);
        notifyDataSetChanged();
    }

    public void updateTitle(int position, String nuevoTitulo) {
        if (position >= 0 && position < notas.size()) {
            Note n = notas.get(position);
            if (!n.placeholder) {
                n.title = nuevoTitulo;
                notifyItemChanged(position);
            }
        }
    }

    public void replaceAt(int position, Note n) {
        if (position >= 0 && position < notas.size()) {
            notas.set(position, n);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public NotaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nota, parent, false);
        return new NotaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotaViewHolder holder, int position) {
        Note note = notas.get(position);

        ThemePalettes.Colors cols = ThemePalettes.forMode(
                SettingsManager.getInstance(holder.itemView.getContext()).getThemeMode()
        );
        holder.card.setCardBackgroundColor(cols.content);
        int textOnContent = ColorUtils.calculateLuminance(cols.content) < 0.5 ? 0xFFFFFFFF : 0xFF000000;

        // Título SIEMPRE seteado aquí (evita residuos)
        String visibleTitle;
        if (note.placeholder) visibleTitle = context.getString(R.string.agregar_nueva_nota);
        else if (note.title == null || note.title.isEmpty()) visibleTitle = context.getString(R.string.titulo);
        else visibleTitle = note.title;
        holder.tvTitulo.setText(visibleTitle);

        if (note.placeholder) {
            holder.tvTitulo.setTextColor(ColorUtils.setAlphaComponent(textOnContent, 160));
            holder.btnAdd.setImageTintList(
                    android.content.res.ColorStateList.valueOf(ColorUtils.setAlphaComponent(textOnContent, 160))
            );
            holder.btnAdd.setAlpha(0.9f);
            holder.card.setCardElevation(2f);

            View inner = holder.itemView.findViewById(R.id.cardInner);
            if (inner != null) {
                int strokeColor = ColorUtils.setAlphaComponent(textOnContent, 120);
                GradientDrawable dashed = new GradientDrawable();
                dashed.setColor(0x00000000);
                dashed.setCornerRadius(dp(inner, 12));
                dashed.setStroke(dp(inner, 2), strokeColor, dp(inner, 8), dp(inner, 6));
                inner.setBackground(dashed);
            }

            holder.card.setOnClickListener(v -> holder.btnAdd.performClick());
            holder.card.setOnLongClickListener(v -> true); // sin drag

        } else {
            holder.tvTitulo.setTextColor(textOnContent);
            holder.btnAdd.setImageTintList(
                    android.content.res.ColorStateList.valueOf(ColorUtils.setAlphaComponent(textOnContent, 200))
            );
            holder.btnAdd.setAlpha(1f);
            holder.card.setCardElevation(4f);

            View inner = holder.itemView.findViewById(R.id.cardInner);
            if (inner != null) inner.setBackground(null); // limpiar dashed de reciclado

            holder.card.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                if (dragStartListener != null) dragStartListener.onStartDrag(holder);
                return true;
            });
            holder.card.setOnClickListener(v -> holder.btnAdd.performClick());
        }

        String transitionName = "nota_card_" + holder.getBindingAdapterPosition();
        holder.card.setTransitionName(transitionName);

        holder.btnAdd.setOnClickListener(x -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            final String titleArg = note.title == null ? "" : note.title;
            final String bodyArg  = note.body  == null ? "" : note.body;
            final long   idArg    = note.id;

            NotaEditorFragment editor =
                    NotaEditorFragment.newInstance(
                            transitionName,
                            titleArg,
                            bodyArg,
                            adapterPos,
                            idArg
                    );

            FragmentActivity act = (FragmentActivity) holder.itemView.getContext();
            act.getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .addSharedElement(holder.card, transitionName)
                    .replace(R.id.notasContent, editor, "NotaEditor")
                    .addToBackStack("NotaEditor")
                    .commit();
        });
    }

    // LIMPIEZA DURA del ViewHolder reciclado
    @Override
    public void onViewRecycled(@NonNull NotaViewHolder holder) {
        super.onViewRecycled(holder);
        holder.tvTitulo.setText("");
        holder.card.setOnClickListener(null);
        holder.card.setOnLongClickListener(null);
        holder.btnAdd.setOnClickListener(null);
        View inner = holder.itemView.findViewById(R.id.cardInner);
        if (inner != null) inner.setBackground(null);
        holder.card.setCardElevation(4f);
        holder.card.setScaleX(1f);
        holder.card.setScaleY(1f);
        holder.card.setAlpha(1f);
        holder.card.setTranslationZ(0f);
    }

    @Override public int getItemCount() { return notas.size(); }

    private static int dp(View v, int dps) {
        float density = v.getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }

    static class NotaViewHolder extends RecyclerView.ViewHolder {
        final CardView card;
        final TextView tvNotaTitulo, tvTitulo;
        final ImageButton btnAdd;

        NotaViewHolder(@NonNull View itemView) {
            super(itemView);
            this.card     = (CardView) itemView; // root es CardView
            this.tvTitulo = itemView.findViewById(R.id.tvNotaTitulo);
            this.tvNotaTitulo = this.tvTitulo;
            this.btnAdd   = itemView.findViewById(R.id.btnNotaAdd);
        }
    }
}
