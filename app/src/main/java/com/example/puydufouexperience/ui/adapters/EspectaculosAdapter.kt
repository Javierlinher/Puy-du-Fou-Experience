package com.example.puydufouexperience.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.puydufouexperience.R
import com.example.puydufouexperience.databinding.ItemEspectaculoBinding
import com.example.puydufouexperience.model.entity.Espectaculo

class EspectaculosAdapter(
    private val onClick: (Espectaculo) -> Unit
) : RecyclerView.Adapter<EspectaculosAdapter.VH>() {

    private val items = mutableListOf<Espectaculo>()

    fun submitList(newItems: List<Espectaculo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEspectaculoBinding.inflate(
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
        private val binding: ItemEspectaculoBinding,
        private val onClick: (Espectaculo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Espectaculo) {
            binding.tvNombre.text = item.nombre

            val horariosFormateados = item.horarios.replace("|", " | ")
            binding.tvInfo.text = "${item.duracionMinutos} min · $horariosFormateados"

            binding.tvAccesible.text =
                if (item.accesible) "♿ Accesible" else "No accesible"

            binding.img.setImageResource(R.drawable.puce)

            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
