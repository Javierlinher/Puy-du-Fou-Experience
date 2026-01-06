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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.FavoritosRepository
import com.example.puydufouexperience.databinding.FragmentEspectaculoDetalleBinding
import com.example.puydufouexperience.model.entity.Espectaculo
import com.example.puydufouexperience.utils.RecordatorioScheduler
import com.example.puydufouexperience.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Fragment de detalle de un espectáculo.
 *
 * Funcionalidad:
 * - Mostrar información completa del espectáculo
 * - Marcar / desmarcar como favorito
 * - Programar recordatorios (inicio / 15 min antes)
 *
 * NOTA DE DISEÑO:
 * - Los recordatorios NO se guardan en Room (decisión de diseño del proyecto)
 * - Se usan AlarmManager + BroadcastReceiver
 *
 * Android 13+:
 * - Hay que pedir permiso POST_NOTIFICATIONS en runtime.
 * - Lo pedimos JUSTO cuando el usuario pulsa "Notificar..." (UX correcta).
 */
class EspectaculoDetalleFragment : Fragment() {

    private var _binding: FragmentEspectaculoDetalleBinding? = null
    private val binding get() = _binding!!

    // ID del espectáculo recibido por argumentos
    private var espectaculoId: Int = -1

    // Usuario logueado actualmente
    private var idUsuarioActual: Int = -1

    // Repositorio de favoritos (tabla polimórfica)
    private lateinit var favoritosRepository: FavoritosRepository

    // Estado del favorito en UI
    private var favoritoActivo = false

    // Guardamos el espectáculo cargado para reutilizar datos
    private var espectaculoActual: Espectaculo? = null

    // Guardamos la intención de recordatorio mientras pedimos permiso (inicio o 15 min)
    private var pendienteEsInicio: Boolean? = null

    // Launcher para pedir permiso de notificaciones (Android 13+)
    private val requestNotificationsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val esInicio = pendienteEsInicio
            pendienteEsInicio = null

            if (granted) {
                // Si conceden permiso, continuamos con lo que el usuario quería hacer
                if (esInicio != null) {
                    programarRecordatorioInterno(esInicio)
                }
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

        // Recuperamos el ID del espectáculo desde el NavController
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

        // Comprobamos sesión
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

        // Carga inicial del detalle y estado de favorito
        cargarDetalleYFavorito()

        // Botón favorito
        binding.btnFavorito.setOnClickListener {
            toggleFavorito()
        }

        // Botón: notificar al inicio del espectáculo
        binding.btnNotificarInicio.setOnClickListener {
            // Pedimos permiso si hace falta, y luego programamos
            solicitarPermisoNotificacionesYProgramar(esInicio = true)
        }

        // Botón: notificar 15 minutos antes
        binding.btnNotificar15.setOnClickListener {
            solicitarPermisoNotificacionesYProgramar(esInicio = false)
        }
    }

    /**
     * Si Android 13+ y no tenemos permiso, lo pedimos.
     * Si ya tenemos permiso (o Android < 13), programamos directamente.
     */
    private fun solicitarPermisoNotificacionesYProgramar(esInicio: Boolean) {
        // Android < 13: no existe permiso runtime -> programamos directo
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
            // Guardamos lo que el usuario quería hacer, y pedimos permiso
            pendienteEsInicio = esInicio
            requestNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Carga:
     * - Datos del espectáculo desde Room
     * - Estado de favorito para el usuario actual
     */
    private fun cargarDetalleYFavorito() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val db = DatabaseProvider.get(requireContext().applicationContext)
                val esp = db.espectaculoDao().getById(espectaculoId)
                val esFav = favoritosRepository.isFavorito(
                    idUsuarioActual,
                    TIPO_ESPECTACULO,
                    espectaculoId
                )
                Pair(esp, esFav)
            }

            espectaculoActual = result.first
            favoritoActivo = result.second

            val espectaculo = espectaculoActual ?: run {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_show_not_found),
                    Toast.LENGTH_SHORT
                ).show()

                return@launch
            }

            // Título en la toolbar
            requireActivity().title = espectaculo.nombre

            // Pintado de datos
            binding.tvNombre.text = espectaculo.nombre
            binding.tvDuracion.text = "Duración: ${espectaculo.duracionMinutos} min"
            binding.tvHorarios.text = "Horarios: ${espectaculo.horarios.replace("|", " | ")}"
            binding.tvAccesible.text =
                "Accesible: " + if (espectaculo.accesible) "Sí" else "No"

            pintarEstadoFavorito()
        }
    }

    /**
     * Programa un recordatorio usando AlarmManager.
     * (Aquí ya damos por hecho que, si hacía falta, tenemos permiso de notificaciones.)
     *
     * @param esInicio true = al inicio, false = 15 minutos antes
     */
    private fun programarRecordatorioInterno(esInicio: Boolean) {
        val espectaculo = espectaculoActual ?: return

        viewLifecycleOwner.lifecycleScope.launch {

            // ✅ Respeta el switch "Notificaciones activadas" (AjustesUsuario.notificaciones)
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

            // Calculamos la próxima sesión futura del espectáculo
            val fechaSesion = calcularProximaSesion(espectaculo.horarios)
            if (fechaSesion == null) {
                Toast.makeText(
                    requireContext(),
                    "No hay sesiones futuras hoy",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            // Momento exacto del disparo
            val triggerAtMillis =
                if (esInicio) fechaSesion else fechaSesion - 15 * 60 * 1000

            // Programamos el recordatorio
            RecordatorioScheduler.programar(
                context = requireContext(),
                espectaculoId = espectaculoId,
                tipo = if (esInicio)
                    RecordatorioScheduler.TIPO_INICIO
                else
                    RecordatorioScheduler.TIPO_15_MIN,
                triggerAtMillis = triggerAtMillis,
                titulo = espectaculo.nombre,
                texto = if (esInicio)
                    "El espectáculo comienza ahora"
                else
                    "El espectáculo empieza en 15 minutos"
            )

            Toast.makeText(
                requireContext(),
                if (esInicio)
                    "Aviso al inicio programado"
                else
                    "Aviso 15 min antes programado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * A partir del string de horarios ("11:00|13:30|17:00")
     * devuelve el timestamp de la próxima sesión futura hoy.
     */
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

    /**
     * Añade o elimina el espectáculo de favoritos.
     */
    private fun toggleFavorito() {
        viewLifecycleOwner.lifecycleScope.launch {
            val nuevoEstado = withContext(Dispatchers.IO) {
                if (favoritoActivo) {
                    favoritosRepository.remove(
                        idUsuarioActual,
                        TIPO_ESPECTACULO,
                        espectaculoId
                    )
                    false
                } else {
                    favoritosRepository.add(
                        idUsuarioActual,
                        TIPO_ESPECTACULO,
                        espectaculoId
                    )
                    true
                }
            }

            favoritoActivo = nuevoEstado
            pintarEstadoFavorito()

            Toast.makeText(
                requireContext(),
                if (favoritoActivo)
                    "Añadido a favoritos"
                else
                    "Eliminado de favoritos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Actualiza el texto del botón de favorito.
     */
    private fun pintarEstadoFavorito() {
        binding.btnFavorito.text =
            if (favoritoActivo) "★ En favoritos"
            else "☆ Añadir a favoritos"
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
