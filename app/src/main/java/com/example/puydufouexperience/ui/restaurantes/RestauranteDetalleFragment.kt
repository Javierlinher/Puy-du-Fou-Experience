package com.example.puydufouexperience.ui.restaurantes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.example.puydufouexperience.data.repository.FavoritosRepository
import com.example.puydufouexperience.databinding.DialogReservaRestauranteBinding
import com.example.puydufouexperience.databinding.FragmentRestauranteDetalleBinding
import com.example.puydufouexperience.model.entity.ReservaRestaurante
import com.example.puydufouexperience.ui.mapa.MapaFragment
import com.example.puydufouexperience.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RestauranteDetalleFragment : Fragment() {

    private var _binding: FragmentRestauranteDetalleBinding? = null
    private val binding get() = _binding!!

    private var restauranteId: Int = -1
    private var idUsuarioActual: Int = -1

    private lateinit var favoritosRepository: FavoritosRepository
    private var favoritoActivo: Boolean = false

    // ✅ Guardamos destino para “Ir hasta allí”
    private var destinoLat: Double? = null
    private var destinoLng: Double? = null
    private var destinoNombre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restauranteId = requireArguments().getInt(ARG_RESTAURANTE_ID, -1)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRestauranteDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        idUsuarioActual = SessionManager.getIdUsuarioActual(requireContext())
        if (idUsuarioActual == -1) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_invalid_session),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (restauranteId == -1) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_invalid_restaurant_id),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val db = DatabaseProvider.get(requireContext().applicationContext)
        favoritosRepository = FavoritosRepository(db.favoritoDao())

        cargarDetalleYFavorito()

        binding.btnFavorito.setOnClickListener { toggleFavorito() }

        // ✅ Ir hasta allí -> MapaFragment con ruta rápida
        binding.btnIrMapa.setOnClickListener {
            val lat = destinoLat
            val lng = destinoLng
            if (lat == null || lng == null) {

                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_location_unavailable),
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val args = bundleOf(
                MapaFragment.ARG_DEST_LAT to lat,
                MapaFragment.ARG_DEST_LNG to lng,
                MapaFragment.ARG_DEST_NAME to destinoNombre
            )

            findNavController().navigate(R.id.mapaFragment, args)
        }

        binding.btnReservar.setOnClickListener { mostrarDialogoReserva() }
    }

    private fun cargarDetalleYFavorito() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val db = DatabaseProvider.get(requireContext().applicationContext)
                val restaurante = db.restauranteDao().getById(restauranteId)
                val esFav = favoritosRepository.isFavorito(idUsuarioActual, TIPO_RESTAURANTE, restauranteId)
                Pair(restaurante, esFav)
            }

            val restaurante = result.first
            favoritoActivo = result.second

            if (restaurante == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_restaurant_not_found),
                    Toast.LENGTH_SHORT
                ).show()


                return@launch
            }

            // ✅ Guardamos destino para botón "Ir hasta allí"
            destinoLat = restaurante.latitud
            destinoLng = restaurante.longitud
            destinoNombre = restaurante.nombre

            requireActivity().title = restaurante.nombre

            binding.tvNombre.text = restaurante.nombre
            binding.tvTipo.text = "Tipo: ${restaurante.tipo}"
            binding.tvPrecio.text = "Precio: ${restaurante.rangoPrecio}"

            pintarEstadoFavorito()
        }
    }

    private fun toggleFavorito() {
        viewLifecycleOwner.lifecycleScope.launch {
            val nuevoEstado = withContext(Dispatchers.IO) {
                if (favoritoActivo) {
                    favoritosRepository.remove(idUsuarioActual, TIPO_RESTAURANTE, restauranteId)
                    false
                } else {
                    val yaEraFav = favoritosRepository.isFavorito(idUsuarioActual, TIPO_RESTAURANTE, restauranteId)
                    if (!yaEraFav) {
                        favoritosRepository.add(idUsuarioActual, TIPO_RESTAURANTE, restauranteId)
                    }
                    true
                }
            }

            favoritoActivo = nuevoEstado
            pintarEstadoFavorito()

            Toast.makeText(
                requireContext(),
                if (favoritoActivo) "Añadido a favoritos" else "Eliminado de favoritos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun pintarEstadoFavorito() {
        binding.btnFavorito.text = if (favoritoActivo) "★ En favoritos" else "☆ Añadir a favoritos"
    }

    private fun mostrarDialogoReserva() {
        val dialogBinding = DialogReservaRestauranteBinding.inflate(LayoutInflater.from(requireContext()))

        val cal = Calendar.getInstance()
        var fechaElegida = false
        var horaElegida = false

        fun actualizarTextoFechaHora() {
            if (!fechaElegida && !horaElegida) {
                dialogBinding.tvFechaHoraValue.text = "(sin seleccionar)"
                return
            }
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dialogBinding.tvFechaHoraValue.text = sdf.format(cal.time)
        }

        dialogBinding.btnElegirFecha.setOnClickListener {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                fechaElegida = true
                actualizarTextoFechaHora()
            }, y, m, d).show()
        }

        dialogBinding.btnElegirHora.setOnClickListener {
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)

            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                horaElegida = true
                actualizarTextoFechaHora()
            }, h, min, true).show()
        }

        actualizarTextoFechaHora()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva reserva")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                val personasText = dialogBinding.etPersonas.text?.toString()?.trim().orEmpty()
                val personas = personasText.toIntOrNull()

                if (personas == null || personas <= 0) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_invalid_people_number),
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                if (!fechaElegida || !horaElegida) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_select_date_time),
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val fechaHoraMillis = cal.timeInMillis
                if (fechaHoraMillis < System.currentTimeMillis()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_date_time_cannot_be_past),
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                guardarReserva(fechaHoraMillis, personas)
            }
            .show()
    }

    private fun guardarReserva(fechaHoraMillis: Long, numPersonas: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val db = DatabaseProvider.get(requireContext().applicationContext)
                    db.reservaRestauranteDao().insert(
                        ReservaRestaurante(
                            idUsuario = idUsuarioActual,
                            idRestaurante = restauranteId,
                            fechaHora = fechaHoraMillis,
                            numPersonas = numPersonas
                        )
                    )
                    true
                } catch (_: Exception) {
                    false
                }
            }

            Toast.makeText(
                requireContext(),
                if (ok) "Reserva guardada" else "No se pudo guardar la reserva",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_RESTAURANTE_ID = "restauranteId"
        private const val TIPO_RESTAURANTE = "RESTAURANTE"
    }
}
