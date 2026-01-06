package com.example.puydufouexperience.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.puydufouexperience.R
import com.example.puydufouexperience.databinding.ItemRestauranteBinding
import com.example.puydufouexperience.model.entity.Restaurante

class RestaurantesAdapter(
    private val onClick: (Restaurante) -> Unit
) : RecyclerView.Adapter<RestaurantesAdapter.VH>() {

    private val items = mutableListOf<Restaurante>()

    fun submitList(newItems: List<Restaurante>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRestauranteBinding.inflate(
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
        private val binding: ItemRestauranteBinding,
        private val onClick: (Restaurante) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Restaurante) {
            binding.tvNombre.text = item.nombre
            binding.tvInfo.text = "${item.tipo} · ${item.rangoPrecio}"

            binding.img.setImageResource(R.drawable.puce_estadio)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
