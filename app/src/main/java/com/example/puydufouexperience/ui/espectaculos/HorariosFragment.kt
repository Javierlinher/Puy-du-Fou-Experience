package com.example.puydufouexperience.ui.espectaculos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.databinding.FragmentHorariosBinding
import com.example.puydufouexperience.ui.adapters.HorariosAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla simple de "Horarios" (HOY).
 *
 * Implementación mínima:
 * - Lee todos los espectáculos
 * - Aplana los horarios (un item por hora)
 * - Ordena por hora ascendente
 * - Click -> navega al detalle del espectáculo
 */
class HorariosFragment : Fragment() {

    private var _binding: FragmentHorariosBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HorariosAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHorariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = "Horarios"

        adapter = HorariosAdapter { item ->
            val args = bundleOf(EspectaculoDetalleFragment.ARG_ESPECTACULO_ID to item.espectaculoId)
            findNavController().navigate(
                R.id.action_horariosFragment_to_espectaculoDetalleFragment,
                args
            )
        }

        binding.rvHorarios.adapter = adapter

        cargarHorarios()
    }

    private fun cargarHorarios() {
        viewLifecycleOwner.lifecycleScope.launch {
            val lista = withContext(Dispatchers.IO) {
                val db = DatabaseProvider.get(requireContext().applicationContext)
                val espectaculos = db.espectaculoDao().getAll()

                val items = mutableListOf<HorarioUi>()

                for (e in espectaculos) {
                    val horas = e.horarios.split("|").map { it.trim() }.filter { it.contains(":") }
                    for (h in horas) {
                        val extra = "${e.duracionMinutos} min · Accesible: ${if (e.accesible) "Sí" else "No"}"
                        items.add(
                            HorarioUi(
                                espectaculoId = e.id,
                                hora = h,
                                titulo = e.nombre,
                                extra = extra
                            )
                        )
                    }
                }

                // Orden simple por string "HH:mm" (funciona si siempre viene con 2 dígitos)
                items.sortedBy { it.hora }
            }

            adapter.submitList(lista)

            if (lista.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_no_schedules),
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
