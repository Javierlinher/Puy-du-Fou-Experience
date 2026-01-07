package com.example.puydufouexperience.ui.espectaculos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.FavoritosRepository
import com.example.puydufouexperience.databinding.FragmentEspectaculoDetalleBinding
import com.example.puydufouexperience.model.entity.Espectaculo
import com.example.puydufouexperience.ui.mapa.MapaFragment
import com.example.puydufouexperience.utils.RecordatorioScheduler
import com.example.puydufouexperience.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Detalle de espectáculo:
 * - Carga datos del espectáculo
 * - Favoritos
 * - Recordatorios
 * - ✅ Ruta rápida "Ir hasta allí" (igual que Restaurantes)
 */
class EspectaculoDetalleFragment : Fragment() {

    private var _binding: FragmentEspectaculoDetalleBinding? = null
    private val binding get() = _binding!!

    private var espectaculoId: Int = -1
    private var idUsuarioActual: Int = -1

    private lateinit var favoritosRepository: FavoritosRepository
    private var favoritoActivo = false

    private var espectaculoActual: Espectaculo? = null

    // ✅ Guardamos destino para “Ir hasta allí” (igual que en restaurante)
    private var destinoLat: Double? = null
    private var destinoLng: Double? = null
    private var destinoNombre: String = ""

    private var pendienteEsInicio: Boolean? = null

    private val requestNotificationsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val esInicio = pendienteEsInicio
            pendienteEsInicio = null

            if (granted) {
                if (esInicio != null) programarRecordatorioInterno(esInicio)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permiso de notificaciones denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        espectaculoId = requireArguments().getInt(ARG_ESPECTACULO_ID, -1)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEspectaculoDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        idUsuarioActual = SessionManager.getIdUsuarioActual(requireContext())
        if (idUsuarioActual == -1 || espectaculoId == -1) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_invalid_data),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val db = DatabaseProvider.get(requireContext().applicationContext)
        favoritosRepository = FavoritosRepository(db.favoritoDao())

        // Carga detalle + favorito + prepara destinoLat/Lng
        cargarDetalleYFavorito()

        // Favorito
        binding.btnFavorito.setOnClickListener { toggleFavorito() }

        // Recordatorios
        binding.btnNotificarInicio.setOnClickListener {
            solicitarPermisoNotificacionesYProgramar(esInicio = true)
        }

        binding.btnNotificar15.setOnClickListener {
            solicitarPermisoNotificacionesYProgramar(esInicio = false)
        }

        // ✅ Ir hasta allí -> navega al mapa con ruta rápida
        // (Necesitas que exista binding.btnIrMapa en el XML)
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

            // Igual que en restaurante: navegamos directo al mapaFragment
            findNavController().navigate(R.id.mapaFragment, args)
        }
    }

    private fun solicitarPermisoNotificacionesYProgramar(esInicio: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            programarRecordatorioInterno(esInicio)
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            programarRecordatorioInterno(esInicio)
        } else {
            pendienteEsInicio = esInicio
            requestNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Carga datos del espectáculo y estado de favorito.
     * ✅ Además, guarda lat/lng/nombre para la ruta rápida.
     */
    private fun cargarDetalleYFavorito() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (esp, esFav) = withContext(Dispatchers.IO) {
                val db = DatabaseProvider.get(requireContext().applicationContext)
                val e = db.espectaculoDao().getById(espectaculoId)
                val fav = favoritosRepository.isFavorito(
                    idUsuarioActual,
                    TIPO_ESPECTACULO,
                    espectaculoId
                )
                Pair(e, fav)
            }

            espectaculoActual = esp
            favoritoActivo = esFav

            val espectaculo = espectaculoActual ?: run {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_show_not_found),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            // ✅ Guardamos destino para “Ir hasta allí”
            destinoLat = espectaculo.latitud
            destinoLng = espectaculo.longitud
            destinoNombre = espectaculo.nombre

            requireActivity().title = espectaculo.nombre

            binding.tvNombre.text = espectaculo.nombre
            binding.tvDuracion.text = "Duración: ${espectaculo.duracionMinutos} min"
            binding.tvHorarios.text = "Horarios: ${espectaculo.horarios.replace("|", " | ")}"
            binding.tvAccesible.text = "Accesible: " + if (espectaculo.accesible) "Sí" else "No"

            pintarEstadoFavorito()
        }
    }

    private fun programarRecordatorioInterno(esInicio: Boolean) {
        val espectaculo = espectaculoActual ?: return

        viewLifecycleOwner.lifecycleScope.launch {

            val notificacionesPermitidas = withContext(Dispatchers.IO) {
                val db = DatabaseProvider.get(requireContext().applicationContext)
                db.ajustesUsuarioDao()
                    .getByUsuario(idUsuarioActual)
                    ?.notificaciones ?: true
            }

            if (!notificacionesPermitidas) {
                Toast.makeText(
                    requireContext(),
                    "Notificaciones desactivadas en ajustes",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val fechaSesion = calcularProximaSesion(espectaculo.horarios)
            if (fechaSesion == null) {
                Toast.makeText(
                    requireContext(),
                    "No hay sesiones futuras hoy",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val triggerAtMillis = if (esInicio) fechaSesion else fechaSesion - 15 * 60 * 1000

            RecordatorioScheduler.programar(
                context = requireContext(),
                espectaculoId = espectaculoId,
                tipo = if (esInicio) RecordatorioScheduler.TIPO_INICIO else RecordatorioScheduler.TIPO_15_MIN,
                triggerAtMillis = triggerAtMillis,
                titulo = espectaculo.nombre,
                texto = if (esInicio) "El espectáculo comienza ahora" else "El espectáculo empieza en 15 minutos"
            )

            Toast.makeText(
                requireContext(),
                if (esInicio) "Aviso al inicio programado" else "Aviso 15 min antes programado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun calcularProximaSesion(horarios: String): Long? {
        val ahora = System.currentTimeMillis()
        val hoy = Calendar.getInstance()

        return horarios.split("|")
            .mapNotNull { hora ->
                val partes = hora.trim().split(":")
                if (partes.size != 2) return@mapNotNull null

                Calendar.getInstance().apply {
                    set(Calendar.YEAR, hoy.get(Calendar.YEAR))
                    set(Calendar.MONTH, hoy.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, hoy.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, partes[0].toInt())
                    set(Calendar.MINUTE, partes[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            .filter { it > ahora }
            .minOrNull()
    }

    private fun toggleFavorito() {
        viewLifecycleOwner.lifecycleScope.launch {
            val nuevoEstado = withContext(Dispatchers.IO) {
                if (favoritoActivo) {
                    favoritosRepository.remove(idUsuarioActual, TIPO_ESPECTACULO, espectaculoId)
                    false
                } else {
                    favoritosRepository.add(idUsuarioActual, TIPO_ESPECTACULO, espectaculoId)
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
        binding.btnFavorito.text =
            if (favoritoActivo) "★ En favoritos" else "☆ Añadir a favoritos"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ESPECTACULO_ID = "espectaculoId"
        private const val TIPO_ESPECTACULO = "ESPECTACULO"
    }
}
