package com.example.puydufouexperience.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.puydufouexperience.databinding.ItemHorarioBinding
import com.example.puydufouexperience.ui.espectaculos.HorarioUi

class HorariosAdapter(
    private val onClick: (HorarioUi) -> Unit
) : RecyclerView.Adapter<HorariosAdapter.VH>() {

    private val items = mutableListOf<HorarioUi>()

    fun submitList(newItems: List<HorarioUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHorarioBinding.inflate(
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
        private val binding: ItemHorarioBinding,
        private val onClick: (HorarioUi) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HorarioUi) {
            // Hora grande
            binding.tvHora.text = item.hora

            // Título del espectáculo
            binding.tvTitulo.text = item.titulo

            // Extra cortito (duración/accesible)
            binding.tvExtra.text = item.extra

            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
