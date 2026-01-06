package com.example.puydufouexperience.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.puydufouexperience.databinding.ItemParadaRutaBinding
import com.example.puydufouexperience.model.entity.ParadaRuta

/**
 * Adapter simple para mostrar paradas de una ruta (ordenadas).
 */
class ParadasRutaAdapter : RecyclerView.Adapter<ParadasRutaAdapter.VH>() {

    private val items = mutableListOf<ParadaRuta>()

    fun submitList(nueva: List<ParadaRuta>) {
        items.clear()
        items.addAll(nueva)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemParadaRutaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemParadaRutaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ParadaRuta) {
            binding.tvOrden.text = item.orden.toString()
            binding.tvEtiqueta.text = item.etiqueta
            binding.tvTipo.text = item.tipoElemento
        }
    }
}
