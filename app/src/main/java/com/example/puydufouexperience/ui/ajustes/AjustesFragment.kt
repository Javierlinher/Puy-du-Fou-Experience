package com.example.puydufouexperience.ui.ajustes

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.UserRepository
import com.example.puydufouexperience.databinding.FragmentAjustesBinding
import com.example.puydufouexperience.utils.SessionManager
import com.example.puydufouexperience.viewmodel.ajustes.AjustesViewModel
import com.example.puydufouexperience.viewmodel.ajustes.AjustesViewModelFactory
import kotlinx.coroutines.launch

/**
 * AjustesFragment:
 * - Idioma, tema, notificaciones
 * - Botón admin solo si SessionManager.isAdmin()
 *
 * Fix idioma SIN cuelgues:
 * - Solo hacemos recreate() si el idioma nuevo es distinto al idioma actual de la app
 * - Al cargar ajustes desde BD, inicializamos la UI (spinners/switch) SIN disparar guardados
 */
class AjustesFragment : Fragment() {

    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AjustesViewModel by viewModels {
        val db = DatabaseProvider.get(requireContext().applicationContext)
        val repo = UserRepository(db.usuarioDao(), db.ajustesUsuarioDao())
        val idUsuario = SessionManager.getIdUsuarioActual(requireContext())
        AjustesViewModelFactory(repo, idUsuario)
    }

    // Tags reales (coinciden con tus folders values-* y locales_config)
    private val idiomaTags = listOf("es", "en-GB")
    private val idiomaLabels = listOf("Español", "English (UK)")

    // Para evitar que la inicialización de UI dispare guardarDesdeUI()
    private var inicializandoUI = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAjustesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Botón admin visible solo si eres admin
        binding.btnAdministracion.visibility =
            if (SessionManager.isAdmin(requireContext())) View.VISIBLE else View.GONE

        // Spinner idioma
        binding.spIdioma.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            idiomaLabels
        )

        // Spinner tema (labels traducibles)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.settings_theme_labels,
            android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spTema.adapter = adapter
        }

        // Listeners (solo actúan cuando inicializandoUI = false)
        binding.spIdioma.setOnItemSelectedListener(SimpleItemSelectedListener {
            if (!inicializandoUI) guardarDesdeUI()
        })
        binding.spTema.setOnItemSelectedListener(SimpleItemSelectedListener {
            if (!inicializandoUI) guardarDesdeUI()
        })
        binding.swNotificaciones.setOnCheckedChangeListener { _, _ ->
            if (!inicializandoUI) guardarDesdeUI()
        }

        // Navegar a admin
        binding.btnAdministracion.setOnClickListener {
            findNavController().navigate(R.id.action_ajustesFragment_to_adminUsuariosFragment)
        }

        // Cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            SessionManager.clearSession(requireContext())
            Toast.makeText(requireContext(), getString(R.string.toast_session_closed), Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), com.example.puydufouexperience.ui.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Cargar ajustes (Room)
        viewModel.cargar()

        // 🔥 Sincronizar UI con lo que haya guardado en BD (y luego activar listeners)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ajustes.collect { ajustes ->
                    if (ajustes == null) return@collect

                    inicializandoUI = true

                    // Idioma: seleccion según tag guardado
                    val idxIdioma = idiomaTags.indexOf(ajustes.idioma).let { if (it >= 0) it else 0 }
                    if (binding.spIdioma.selectedItemPosition != idxIdioma) {
                        binding.spIdioma.setSelection(idxIdioma, false)
                    }

                    // Tema: seleccion según valor lógico guardado
                    val idxTema = when (ajustes.tema) {
                        "LIGHT" -> 1
                        "DARK" -> 2
                        else -> 0 // SYSTEM
                    }
                    if (binding.spTema.selectedItemPosition != idxTema) {
                        binding.spTema.setSelection(idxTema, false)
                    }

                    // Notificaciones
                    if (binding.swNotificaciones.isChecked != ajustes.notificaciones) {
                        binding.swNotificaciones.isChecked = ajustes.notificaciones
                    }

                    inicializandoUI = false
                }
            }
        }
    }

    /**
     * Lee UI y guarda en BD valores lógicos:
     * - idioma: "es" o "en-GB"
     * - tema: "SYSTEM/LIGHT/DARK"
     */
    private fun guardarDesdeUI() {
        val idioma = idiomaTags.getOrNull(binding.spIdioma.selectedItemPosition) ?: "es"

        val tema = when (binding.spTema.selectedItemPosition) {
            1 -> "LIGHT"
            2 -> "DARK"
            else -> "SYSTEM"
        }

        val notificaciones = binding.swNotificaciones.isChecked

        // 1) Persistir en Room
        viewModel.guardar(idioma, tema, notificaciones)

        // 2) Aplicar idioma SOLO si cambia realmente (evita bucle de recreate)
        val idiomaActual = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (idiomaActual != idioma) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(idioma))
            // Forzar refresco total (una sola vez, porque al recrear ya idiomaActual == idioma)
            requireActivity().recreate()
            return
        }

        // 3) Aplicar tema (esto no requiere recreate)
        when (tema) {
            "LIGHT" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "DARK" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class SimpleItemSelectedListener(
    val onSelected: () -> Unit
) : android.widget.AdapterView.OnItemSelectedListener {

    override fun onItemSelected(
        parent: android.widget.AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) = onSelected()

    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
}
