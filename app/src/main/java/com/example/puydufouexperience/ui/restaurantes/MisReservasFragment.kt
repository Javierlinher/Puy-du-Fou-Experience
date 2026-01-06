package com.example.puydufouexperience.ui.restaurantes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.databinding.DialogReservaRestauranteBinding
import com.example.puydufouexperience.databinding.FragmentMisReservasBinding
import com.example.puydufouexperience.model.entity.ReservaConRestauranteNombre
import com.example.puydufouexperience.model.entity.ReservaRestaurante
import com.example.puydufouexperience.ui.adapters.ReservasAdapter
import com.example.puydufouexperience.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MisReservasFragment : Fragment() {

    private var _binding: FragmentMisReservasBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReservasAdapter
    private var idUsuarioActual: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisReservasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recuperamos el usuario actual (sesión)
        idUsuarioActual = SessionManager.getIdUsuarioActual(requireContext())
        if (idUsuarioActual == -1) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_invalid_session),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Adapter con click -> opciones (modificar / eliminar)
        adapter = ReservasAdapter { item ->
            // En acciones (editar/eliminar) operamos con la entidad real de reserva
            mostrarOpcionesReserva(item)
        }

        binding.rvReservas.adapter = adapter

        // Primera carga
        cargarReservas()
    }

    private fun cargarReservas() {
        // Carga de Room en IO y actualización en UI
        viewLifecycleOwner.lifecycleScope.launch {
            val reservas = withContext(Dispatchers.IO) {
                val db = DatabaseProvider.get(requireContext().applicationContext)
                db.reservaRestauranteDao().getByUsuarioConNombre(idUsuarioActual)
            }

            adapter.submitList(reservas)

            val hay = reservas.isNotEmpty()
            binding.rvReservas.visibility = if (hay) View.VISIBLE else View.GONE
            binding.tvEmpty.visibility = if (hay) View.GONE else View.VISIBLE
        }
    }

    private fun mostrarOpcionesReserva(item: ReservaConRestauranteNombre) {
        val reserva = item.reserva

        // Seguridad básica: solo permitir operar con reservas del usuario actual
        if (reserva.idUsuario != idUsuarioActual) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_cannot_modify_reservation),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.restauranteNombre)
            .setItems(arrayOf("Modificar", "Eliminar", "Cancelar")) { dialog, which ->
                when (which) {
                    0 -> mostrarDialogoEditarReserva(reserva)
                    1 -> confirmarEliminarReserva(reserva)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun confirmarEliminarReserva(reserva: ReservaRestaurante) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar reserva")
            .setMessage("¿Seguro que quieres eliminar esta reserva?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarReserva(reserva)
            }
            .show()
    }

    private fun eliminarReserva(reserva: ReservaRestaurante) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val db = DatabaseProvider.get(requireContext().applicationContext)
                    db.reservaRestauranteDao().delete(reserva)
                    true
                } catch (e: Exception) {
                    false
                }
            }

            Toast.makeText(
                requireContext(),
                if (ok) "Reserva eliminada" else "No se pudo eliminar",
                Toast.LENGTH_SHORT
            ).show()

            if (ok) cargarReservas()
        }
    }

    private fun mostrarDialogoEditarReserva(reserva: ReservaRestaurante) {
        val dialogBinding = DialogReservaRestauranteBinding.inflate(LayoutInflater.from(requireContext()))

        // Partimos de los valores actuales de la reserva
        val cal = Calendar.getInstance().apply { timeInMillis = reserva.fechaHora }

        var fechaElegida = true
        var horaElegida = true

        fun actualizarTextoFechaHora() {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dialogBinding.tvFechaHoraValue.text = sdf.format(cal.time)
        }

        // Prefill personas
        dialogBinding.etPersonas.setText(reserva.numPersonas.toString())

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
            .setTitle("Modificar reserva")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
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

                guardarCambiosReserva(reserva, fechaHoraMillis, personas)
            }
            .show()
    }

    private fun guardarCambiosReserva(reservaOriginal: ReservaRestaurante, nuevaFechaHora: Long, nuevasPersonas: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val db = DatabaseProvider.get(requireContext().applicationContext)

                    // Copia con nuevos valores (mantenemos id para que UPDATE funcione)
                    val actualizada = reservaOriginal.copy(
                        fechaHora = nuevaFechaHora,
                        numPersonas = nuevasPersonas
                    )

                    db.reservaRestauranteDao().update(actualizada)
                    true
                } catch (e: Exception) {
                    false
                }
            }

            Toast.makeText(
                requireContext(),
                if (ok) "Reserva modificada" else "No se pudo modificar",
                Toast.LENGTH_SHORT
            ).show()

            if (ok) cargarReservas()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
