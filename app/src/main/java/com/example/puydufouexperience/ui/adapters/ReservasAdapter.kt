package com.example.puydufouexperience.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.puydufouexperience.databinding.ItemReservaBinding
import com.example.puydufouexperience.model.entity.ReservaConRestauranteNombre
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReservasAdapter(
    private val onClick: (ReservaConRestauranteNombre) -> Unit
) : RecyclerView.Adapter<ReservasAdapter.VH>() {

    private val items = mutableListOf<ReservaConRestauranteNombre>()

    fun submitList(newItems: List<ReservaConRestauranteNombre>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReservaBinding.inflate(
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
        private val binding: ItemReservaBinding,
        private val onClick: (ReservaConRestauranteNombre) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReservaConRestauranteNombre) {
            // Mostramos el nombre real del restaurante (mucho más limpio)
            binding.tvRestaurante.text = item.restauranteNombre

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.tvFecha.text = "Fecha: ${sdf.format(Date(item.reserva.fechaHora))}"

            binding.tvPersonas.text = "Personas: ${item.reserva.numPersonas}"

            // Click para opciones (editar / eliminar)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
