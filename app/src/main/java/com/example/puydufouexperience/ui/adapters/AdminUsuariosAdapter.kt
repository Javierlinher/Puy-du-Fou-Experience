package com.example.puydufouexperience.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.puydufouexperience.databinding.ItemAdminUsuarioBinding
import com.example.puydufouexperience.model.entity.Usuario

/**
 * Adapter simple para listar usuarios en el panel de administración.
 * - Muestra nombre
 * - Botón "Borrar" con callback
 */
class AdminUsuariosAdapter(
    private val onDeleteClick: (Usuario) -> Unit
) : RecyclerView.Adapter<AdminUsuariosAdapter.VH>() {

    private val items = mutableListOf<Usuario>()

    fun submitList(nueva: List<Usuario>) {
        items.clear()
        items.addAll(nueva)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAdminUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemAdminUsuarioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(usuario: Usuario) {
            binding.tvNombre.text = usuario.nombre
            binding.btnBorrar.setOnClickListener { onDeleteClick(usuario) }
        }
    }
}
