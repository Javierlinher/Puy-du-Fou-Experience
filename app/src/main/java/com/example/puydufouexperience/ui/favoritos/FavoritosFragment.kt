package com.example.puydufouexperience.ui.favoritos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.FavoritosRepository
import com.example.puydufouexperience.databinding.FragmentFavoritosBinding
import com.example.puydufouexperience.ui.adapters.FavoritosAdapter
import com.example.puydufouexperience.ui.espectaculos.EspectaculoDetalleFragment
import com.example.puydufouexperience.ui.restaurantes.RestauranteDetalleFragment
import com.example.puydufouexperience.ui.rutas.RutaDetalleFragment
import com.example.puydufouexperience.utils.SessionManager
import com.example.puydufouexperience.viewmodel.favoritos.FavoritosViewModel
import com.example.puydufouexperience.viewmodel.favoritos.FavoritosViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritosFragment : Fragment() {

    private var _binding: FragmentFavoritosBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterEspectaculos: FavoritosAdapter
    private lateinit var adapterRestaurantes: FavoritosAdapter
    private lateinit var adapterRutas: FavoritosAdapter

    private lateinit var viewModel: FavoritosViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val idUsuarioActual = SessionManager.getIdUsuarioActual(requireContext())
        if (idUsuarioActual == -1) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_invalid_session),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val db = DatabaseProvider.get(requireContext().applicationContext)

        // Repo de favoritos (tabla polimórfica)
        val favoritosRepo = FavoritosRepository(db.favoritoDao())

        // Función que resuelve nombres por lotes usando Room (espectáculos/restaurantes/rutas)
        val resolverNombres:
                suspend (List<Int>, List<Int>, List<Int>) -> Triple<List<FavoritoUi>, List<FavoritoUi>, List<FavoritoUi>> =
            { idsEsp, idsRes, idsRut ->
                withContext(Dispatchers.IO) {
                    // Una query por tipo (evita N+1)
                    val espectaculos =
                        if (idsEsp.isEmpty()) emptyList() else db.espectaculoDao().getByIds(idsEsp)
                    val restaurantes =
                        if (idsRes.isEmpty()) emptyList() else db.restauranteDao().getByIds(idsRes)

                    // ✅ RUTAS: aquí estaba el fallo: antes ponías "Ruta #id"
                    // Cargamos por ids y resolvemos su nombre real
                    val rutas =
                        if (idsRut.isEmpty()) emptyList() else idsRut.mapNotNull { db.rutaDao().getById(it) }

                    val mapaEsp = espectaculos.associateBy({ it.id }, { it.nombre })
                    val mapaRes = restaurantes.associateBy({ it.id }, { it.nombre })
                    val mapaRut = rutas.associateBy({ it.id }, { it.nombre })

                    val listaEsp = idsEsp.mapNotNull { id ->
                        val nombre = mapaEsp[id] ?: return@mapNotNull null
                        FavoritoUi("ESPECTACULO", id, nombre, "Espectáculo")
                    }.sortedBy { it.titulo }

                    val listaRes = idsRes.mapNotNull { id ->
                        val nombre = mapaRes[id] ?: return@mapNotNull null
                        FavoritoUi("RESTAURANTE", id, nombre, "Restaurante")
                    }.sortedBy { it.titulo }

                    val listaRut = idsRut.mapNotNull { id ->
                        val nombre = mapaRut[id] ?: return@mapNotNull null
                        FavoritoUi("RUTA", id, nombre, "Ruta")
                    }.sortedBy { it.titulo }

                    Triple(listaEsp, listaRes, listaRut)
                }
            }

        viewModel = ViewModelProvider(
            this,
            FavoritosViewModelFactory(favoritosRepo, idUsuarioActual, resolverNombres)
        )[FavoritosViewModel::class.java]

        // Adapter de espectáculos
        adapterEspectaculos = FavoritosAdapter { item ->
            val args = bundleOf(EspectaculoDetalleFragment.ARG_ESPECTACULO_ID to item.idElemento)
            findNavController().navigate(R.id.action_favoritosFragment_to_espectaculoDetalleFragment, args)
        }

        // Adapter de restaurantes
        adapterRestaurantes = FavoritosAdapter { item ->
            val args = bundleOf(RestauranteDetalleFragment.ARG_RESTAURANTE_ID to item.idElemento)
            findNavController().navigate(R.id.action_favoritosFragment_to_restauranteDetalleFragment, args)
        }

        // ✅ Adapter de rutas: ahora navega al detalle ruta
        adapterRutas = FavoritosAdapter { item ->
            val args = bundleOf(RutaDetalleFragment.ARG_RUTA_ID to item.idElemento)
            findNavController().navigate(R.id.action_favoritosFragment_to_rutaDetalleFragment, args)
        }

        binding.rvFavEspectaculos.adapter = adapterEspectaculos
        binding.rvFavRestaurantes.adapter = adapterRestaurantes
        binding.rvFavRutas.adapter = adapterRutas

        // Observamos estados y pintamos UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favoritosEsp.collect { lista ->
                adapterEspectaculos.submitList(lista)
                val hay = lista.isNotEmpty()
                binding.rvFavEspectaculos.visibility = if (hay) View.VISIBLE else View.GONE
                binding.tvEmptyEspectaculos.visibility = if (hay) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favoritosRes.collect { lista ->
                adapterRestaurantes.submitList(lista)
                val hay = lista.isNotEmpty()
                binding.rvFavRestaurantes.visibility = if (hay) View.VISIBLE else View.GONE
                binding.tvEmptyRestaurantes.visibility = if (hay) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favoritosRut.collect { lista ->
                adapterRutas.submitList(lista)
                val hay = lista.isNotEmpty()
                binding.rvFavRutas.visibility = if (hay) View.VISIBLE else View.GONE
                binding.tvEmptyRutas.visibility = if (hay) View.GONE else View.VISIBLE
            }
        }

        // Lanzamos carga
        viewModel.cargar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
