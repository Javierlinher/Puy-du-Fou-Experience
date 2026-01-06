package com.example.puydufouexperience.ui.rutas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.FavoritosRepository
import com.example.puydufouexperience.data.repository.RutasRepository
import com.example.puydufouexperience.databinding.FragmentRutaDetalleBinding
import com.example.puydufouexperience.ui.adapters.ParadasRutaAdapter
import com.example.puydufouexperience.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * Detalle de ruta:
 * - nombre
 * - paradas ordenadas
 * Acciones:
 * - Ver en mapa (dibuja la ruta real con Routes API)
 * - Favorito (polimórfico)
 * - Borrar (ruta + paradas)
 */
class RutaDetalleFragment : Fragment() {

    companion object {
        const val ARG_RUTA_ID = "rutaId"

        // Argumento que entenderá el MapaFragment para "dibujar esta ruta guardada"
        const val ARG_MAPA_VER_RUTA_ID = "arg_ver_ruta_id"
    }

    private var _binding: FragmentRutaDetalleBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ParadasRutaAdapter

    private var idRutaActual: Int = -1
    private var esFavorita: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRutaDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ParadasRutaAdapter()
        binding.rvParadas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvParadas.adapter = adapter

        idRutaActual = arguments?.getInt(ARG_RUTA_ID, -1) ?: -1
        if (idRutaActual <= 0) {
            binding.tvTitulo.text = "Ruta no válida"
            return
        }

        // Ver en mapa -> navega al mapa con el idRuta
        binding.btnVerEnMapa.setOnClickListener {
            val args = Bundle().apply { putInt(ARG_MAPA_VER_RUTA_ID, idRutaActual) }
            findNavController().navigate(R.id.action_rutaDetalleFragment_to_mapaFragment, args)
        }

        // Borrar ruta
        binding.btnBorrar.setOnClickListener {
            confirmarBorrado()
        }

        // Favorito (toggle)
        binding.btnFavorito.setOnClickListener {
            toggleFavorito()
        }

        cargarDetalle(idRutaActual)
        cargarEstadoFavorito()
    }

    private fun cargarDetalle(idRuta: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = DatabaseProvider.get(requireContext().applicationContext)
            val repo = RutasRepository(db.rutaDao(), db.paradaRutaDao())

            val ruta = repo.getRutaById(idRuta)
            val paradas = repo.getParadasOrdenadas(idRuta)

            binding.tvTitulo.text = ruta?.nombre ?: "Ruta"
            adapter.submitList(paradas)
            binding.tvEmpty.visibility = if (paradas.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun cargarEstadoFavorito() {
        viewLifecycleOwner.lifecycleScope.launch {
            val idUsuario = SessionManager.getIdUsuarioActual(requireContext())
            if (idUsuario <= 0) return@launch

            val db = DatabaseProvider.get(requireContext().applicationContext)
            val favRepo = FavoritosRepository(db.favoritoDao())

            esFavorita = favRepo.isFavoritoRuta(idUsuario, idRutaActual)
            pintarBotonFavorito()
        }
    }

    private fun pintarBotonFavorito() {
        // Texto corto para que no "baile" demasiado el layout
        binding.btnFavorito.text = if (esFavorita) "Quitar fav" else "Favorito"
    }

    private fun toggleFavorito() {
        viewLifecycleOwner.lifecycleScope.launch {
            val idUsuario = SessionManager.getIdUsuarioActual(requireContext())
            if (idUsuario <= 0) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_no_session),
                    Toast.LENGTH_SHORT
                ).show()

                return@launch
            }

            val db = DatabaseProvider.get(requireContext().applicationContext)
            val favRepo = FavoritosRepository(db.favoritoDao())

            try {
                if (esFavorita) {
                    favRepo.removeFavoritoRuta(idUsuario, idRutaActual)
                    esFavorita = false
                } else {
                    favRepo.addFavoritoRuta(idUsuario, idRutaActual)
                    esFavorita = true
                }
                pintarBotonFavorito()
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_favorite_update_error),
                    Toast.LENGTH_LONG
                ).show()

            }
        }
    }

    private fun confirmarBorrado() {
        AlertDialog.Builder(requireContext())
            .setTitle("Borrar ruta")
            .setMessage("¿Seguro que quieres borrar esta ruta? Se borrarán sus paradas.")
            .setPositiveButton("Borrar") { _, _ ->
                borrarRuta()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun borrarRuta() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = DatabaseProvider.get(requireContext().applicationContext)
            val repo = RutasRepository(db.rutaDao(), db.paradaRutaDao())

            try {
                repo.borrarRuta(idRutaActual)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_route_deleted),
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().popBackStack()
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_route_delete_error),
                    Toast.LENGTH_LONG
                ).show()

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
