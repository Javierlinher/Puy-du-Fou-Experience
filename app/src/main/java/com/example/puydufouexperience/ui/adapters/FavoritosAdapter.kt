package com.example.puydufouexperience.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.puydufouexperience.databinding.ItemFavoritoBinding
import com.example.puydufouexperience.ui.favoritos.FavoritoUi

class FavoritosAdapter(
    private val onClick: (FavoritoUi) -> Unit
) : RecyclerView.Adapter<FavoritosAdapter.VH>() {

    private val items = mutableListOf<FavoritoUi>()

    fun submitList(newItems: List<FavoritoUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFavoritoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(
        private val binding: ItemFavoritoBinding,
        private val onClick: (FavoritoUi) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FavoritoUi) {
            // Título principal (nombre del elemento)
            binding.tvTitulo.text = item.titulo

            // Subtítulo (tipo)
            binding.tvSubtitulo.text = item.subtitulo

            // Click -> navegación al detalle
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
