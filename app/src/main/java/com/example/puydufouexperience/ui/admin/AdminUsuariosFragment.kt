package com.example.puydufouexperience.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.UserRepository
import com.example.puydufouexperience.databinding.FragmentAdminUsuariosBinding
import com.example.puydufouexperience.ui.adapters.AdminUsuariosAdapter
import com.example.puydufouexperience.viewmodel.admin.AdminUsuariosViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Panel de administración de usuarios (solo accesible desde Ajustes si eres admin).
 *
 * - Lista usuarios
 * - Permite borrar usuarios
 */
class AdminUsuariosFragment : Fragment() {

    private var _binding: FragmentAdminUsuariosBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AdminUsuariosViewModel
    private lateinit var adapter: AdminUsuariosAdapter

    companion object {
        private const val ADMIN_USER = "admin"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ViewModel manual (sin DI)
        val db = DatabaseProvider.get(requireContext().applicationContext)
        val repo = UserRepository(db.usuarioDao(), db.ajustesUsuarioDao())
        viewModel = AdminUsuariosViewModel(repo)

        adapter = AdminUsuariosAdapter { usuario ->
            // Seguridad: no permitir borrar al admin
            if (usuario.nombre == ADMIN_USER) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_cannot_delete_admin),
                    Toast.LENGTH_SHORT
                ).show()

                return@AdminUsuariosAdapter
            }

            // Borrado + refresh
            viewModel.borrarUsuario(usuario.id) { ok ->
                if (ok) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_user_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_user_delete_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }

        binding.recyclerUsuarios.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUsuarios.adapter = adapter

        // Observa la lista de usuarios
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.usuarios.collectLatest { lista ->
                val vacio = lista.isEmpty()
                binding.tvVacio.visibility = if (vacio) View.VISIBLE else View.GONE
                binding.recyclerUsuarios.visibility = if (vacio) View.GONE else View.VISIBLE
                adapter.submitList(lista)
            }
        }

        // Cargar usuarios al entrar
        viewModel.cargarUsuarios()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
