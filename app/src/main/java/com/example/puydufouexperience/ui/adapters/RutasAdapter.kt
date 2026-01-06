package com.example.puydufouexperience.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.puydufouexperience.databinding.ItemRutaBinding
import com.example.puydufouexperience.model.entity.Ruta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter simple para listar rutas:
 * - nombre
 * - fecha de creación
 * - click -> abre detalle
 */
class RutasAdapter(
    private val onClick: (rutaId: Int) -> Unit
) : RecyclerView.Adapter<RutasAdapter.VH>() {

    private val items = mutableListOf<Ruta>()
    private val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun submitList(nueva: List<Ruta>) {
        items.clear()
        items.addAll(nueva)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRutaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemRutaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Ruta) {
            binding.tvNombre.text = item.nombre
            binding.tvFecha.text = df.format(Date(item.fechaCreacion))

            binding.root.setOnClickListener {
                onClick(item.id)
            }
        }
    }
}
