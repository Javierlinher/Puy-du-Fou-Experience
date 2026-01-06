package com.example.puydufouexperience.ui.mapa

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.EspectaculosRepository
import com.example.puydufouexperience.data.repository.RestaurantesRepository
import com.example.puydufouexperience.databinding.BottomSheetPoiBinding
import com.example.puydufouexperience.ui.espectaculos.EspectaculoDetalleFragment
import com.example.puydufouexperience.ui.restaurantes.RestauranteDetalleFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * BottomSheet para mostrar información rápida de un POI del mapa.
 *
 * Botones:
 * - Ir hasta aquí -> le manda al MapaFragment el destino (lat/lng) para calcular ruta (Routes API).
 * - Ver detalle -> navega al detalle.
 */
class PoiBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPoiBinding? = null
    private val binding get() = _binding!!

    private val tipo: String by lazy { requireArguments().getString(ARG_TIPO) ?: "" }

    // ⚠️ NO usar "id" como propiedad en un Fragment: choca con Fragment.getId()
    private val elementoId: Int by lazy { requireArguments().getInt(ARG_ID, -1) }

    // Guardamos el destino cuando cargamos info (para el botón "Ir hasta aquí")
    private var destinoLat: Double? = null
    private var destinoLng: Double? = null
    private var destinoNombre: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPoiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pintamos un mínimo de info inmediata
        binding.tvTipo.text = when (tipo) {
            PoiUi.TIPO_ESPECTACULO -> "Espectáculo"
            PoiUi.TIPO_RESTAURANTE -> "Restaurante"
            else -> "Punto"
        }

        // Botón "Ver detalle"
        binding.btnVerDetalle.setOnClickListener {
            navegarADetalle()
            dismiss()
        }

        // Botón "Ir hasta aquí" (se habilita cuando tengamos lat/lng)
        binding.btnIrHastaAqui.isEnabled = false
        binding.btnIrHastaAqui.setOnClickListener {
            enviarDestinoAlMapa()
            dismiss()
        }

        // Cargar detalles desde Room (consulta rápida por ID) y extraer lat/lng
        cargarInfoExtra()
    }

    private fun cargarInfoExtra() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = DatabaseProvider.get(requireContext().applicationContext)

            when (tipo) {
                PoiUi.TIPO_ESPECTACULO -> {
                    val repo = EspectaculosRepository(db.espectaculoDao())
                    val e = repo.getById(elementoId)

                    if (e != null) {
                        destinoLat = e.latitud
                        destinoLng = e.longitud
                        destinoNombre = e.nombre

                        binding.tvNombre.text = e.nombre
                        binding.tvExtra.text = "Duración: ${e.duracionMinutos} min · Accesible: ${if (e.accesible) "Sí" else "No"}"
                        binding.btnIrHastaAqui.isEnabled = true
                    } else {
                        binding.tvNombre.text = "No encontrado"
                        binding.tvExtra.text = ""
                        binding.btnVerDetalle.isEnabled = false
                    }
                }

                PoiUi.TIPO_RESTAURANTE -> {
                    val repo = RestaurantesRepository(db.restauranteDao(), db.reservaRestauranteDao())
                    val r = repo.getById(elementoId)

                    if (r != null) {
                        destinoLat = r.latitud
                        destinoLng = r.longitud
                        destinoNombre = r.nombre

                        binding.tvNombre.text = r.nombre
                        binding.tvExtra.text = "Tipo: ${r.tipo} · Precio: ${r.rangoPrecio}"
                        binding.btnIrHastaAqui.isEnabled = true
                    } else {
                        binding.tvNombre.text = "No encontrado"
                        binding.tvExtra.text = ""
                        binding.btnVerDetalle.isEnabled = false
                    }
                }

                else -> {
                    binding.tvNombre.text = "Tipo desconocido"
                    binding.tvExtra.text = ""
                    binding.btnVerDetalle.isEnabled = false
                }
            }
        }
    }

    /**
     * Envía el destino al MapaFragment usando FragmentResult API.
     * El MapaFragment escuchará y dibujará la ruta con Routes API.
     */
    private fun enviarDestinoAlMapa() {
        val lat = destinoLat ?: return
        val lng = destinoLng ?: return
        val nombre = destinoNombre ?: ""

        parentFragmentManager.setFragmentResult(
            REQUEST_ROUTE,
            bundleOf(
                KEY_LAT to lat,
                KEY_LNG to lng,
                KEY_NAME to nombre
            )
        )
    }

    private fun navegarADetalle() {
        // Usamos el NavController del fragment padre (MapaFragment).
        val nav = (parentFragment as? Fragment)?.findNavController() ?: return

        when (tipo) {
            PoiUi.TIPO_ESPECTACULO -> {
                val args = bundleOf(EspectaculoDetalleFragment.ARG_ESPECTACULO_ID to elementoId)
                nav.navigate(R.id.action_mapaFragment_to_espectaculoDetalleFragment, args)
            }

            PoiUi.TIPO_RESTAURANTE -> {
                val args = bundleOf(RestauranteDetalleFragment.ARG_RESTAURANTE_ID to elementoId)
                nav.navigate(R.id.action_mapaFragment_to_restauranteDetalleFragment, args)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TIPO = "arg_tipo"
        private const val ARG_ID = "arg_id"

        // FragmentResult keys
        const val REQUEST_ROUTE = "request_route"
        const val KEY_LAT = "key_lat"
        const val KEY_LNG = "key_lng"
        const val KEY_NAME = "key_name"

        fun newInstance(tipo: String, elementoId: Int): PoiBottomSheetDialogFragment {
            return PoiBottomSheetDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TIPO to tipo,
                    ARG_ID to elementoId
                )
            }
        }
    }
}
