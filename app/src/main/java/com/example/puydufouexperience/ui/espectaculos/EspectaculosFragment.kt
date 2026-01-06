package com.example.puydufouexperience.ui.espectaculos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.EspectaculosRepository
import com.example.puydufouexperience.databinding.FragmentEspectaculosBinding
import com.example.puydufouexperience.ui.adapters.EspectaculosAdapter
import com.example.puydufouexperience.viewmodel.espectaculos.EspectaculosViewModel
import com.example.puydufouexperience.viewmodel.espectaculos.EspectaculosViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * EspectaculosFragment (MVVM light):
 * - El Fragment NO consulta Room directamente.
 * - Observa el ViewModel y pinta la lista.
 * - La navegación sigue igual.
 */
class EspectaculosFragment : Fragment() {

    private var _binding: FragmentEspectaculosBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: EspectaculosAdapter

    // ✅ ViewModel inyectado con Factory (sin DI)
    private val viewModel: EspectaculosViewModel by viewModels {
        val db = DatabaseProvider.get(requireContext().applicationContext)
        val repo = EspectaculosRepository(db.espectaculoDao())
        EspectaculosViewModelFactory(repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEspectaculosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adapter con navegación al detalle
        adapter = EspectaculosAdapter { espectaculo ->
            val args = bundleOf(EspectaculoDetalleFragment.ARG_ESPECTACULO_ID to espectaculo.id)
            findNavController().navigate(
                R.id.action_espectaculosFragment_to_espectaculoDetalleFragment,
                args
            )
        }

        binding.rvEspectaculos.adapter = adapter

        // Botón "Horarios" -> pantalla agenda simple de HOY
        binding.btnHorarios.setOnClickListener {
            findNavController().navigate(R.id.action_espectaculosFragment_to_horariosFragment)
        }

        // ✅ Observadores (Fragment solo pinta UI)
        observarViewModel()

        // ✅ Lanzar carga
        viewModel.cargar()
    }

    private fun observarViewModel() {
        // Lista
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.items.collectLatest { lista ->
                adapter.submitList(lista)
            }
        }

        // Error (opcional)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { msg ->
                if (!msg.isNullOrBlank()) {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
