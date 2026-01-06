package com.example.puydufouexperience.ui.restaurantes

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
import com.example.puydufouexperience.data.repository.RestaurantesRepository
import com.example.puydufouexperience.databinding.FragmentRestaurantesBinding
import com.example.puydufouexperience.ui.adapters.RestaurantesAdapter
import com.example.puydufouexperience.viewmodel.restaurantes.RestaurantesViewModel
import com.example.puydufouexperience.viewmodel.restaurantes.RestaurantesViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * RestaurantesFragment (MVVM light):
 * - El Fragment NO consulta Room directamente.
 * - Observa el ViewModel y pinta la lista.
 * - La navegación se mantiene igual.
 */
class RestaurantesFragment : Fragment() {

    private var _binding: FragmentRestaurantesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RestaurantesAdapter

    // ✅ ViewModel con Factory (sin DI)
    private val viewModel: RestaurantesViewModel by viewModels {
        val db = DatabaseProvider.get(requireContext().applicationContext)

        // Tu repo necesita 2 DAOs: RestauranteDao y ReservaRestauranteDao
        val repo = RestaurantesRepository(
            restauranteDao = db.restauranteDao(),
            reservaDao = db.reservaRestauranteDao()
        )

        RestaurantesViewModelFactory(repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestaurantesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón: "Mis reservas"
        binding.btnMisReservas.setOnClickListener {
            findNavController().navigate(R.id.action_restaurantesFragment_to_misReservasFragment)
        }

        // Adapter con navegación al detalle
        adapter = RestaurantesAdapter { restaurante ->
            val args = bundleOf(RestauranteDetalleFragment.ARG_RESTAURANTE_ID to restaurante.id)
            findNavController().navigate(
                R.id.action_restaurantesFragment_to_restauranteDetalleFragment,
                args
            )
        }

        binding.rvRestaurantes.adapter = adapter

        // ✅ Observadores
        observarViewModel()

        // ✅ Cargar datos
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
