package com.example.puydufouexperience.ui.rutas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.RutasRepository
import com.example.puydufouexperience.databinding.FragmentRutasBinding
import com.example.puydufouexperience.ui.adapters.RutasAdapter
import com.example.puydufouexperience.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * Lista de rutas guardadas (Room).
 * Además tiene el botón "Crear ruta" que manda al mapa y activa modo creación.
 */
class RutasFragment : Fragment() {

    private var _binding: FragmentRutasBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RutasAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRutasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adapter + navegación al detalle de ruta
        adapter = RutasAdapter { rutaId ->
            val args = Bundle().apply { putInt(RutaDetalleFragment.ARG_RUTA_ID, rutaId) }
            findNavController().navigate(R.id.action_rutasFragment_to_rutaDetalleFragment, args)
        }

        binding.rvRutas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRutas.adapter = adapter

        // "Crear ruta" -> vuelve al mapa y le pasa arg_iniciar_creacion_ruta=true
        binding.btnCrearRuta.setOnClickListener {
            val args = Bundle().apply { putBoolean("arg_iniciar_creacion_ruta", true) }
            findNavController().navigate(R.id.action_rutasFragment_to_mapaFragment, args)
        }

        cargarRutas()
    }

    private fun cargarRutas() {
        viewLifecycleOwner.lifecycleScope.launch {
            val idUsuario = SessionManager.getIdUsuarioActual(requireContext())

            val db = DatabaseProvider.get(requireContext().applicationContext)
            val repo = RutasRepository(db.rutaDao(), db.paradaRutaDao())

            val rutas = if (idUsuario > 0) repo.getRutasByUsuario(idUsuario) else emptyList()

            adapter.submitList(rutas)
            binding.tvEmpty.visibility = if (rutas.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
