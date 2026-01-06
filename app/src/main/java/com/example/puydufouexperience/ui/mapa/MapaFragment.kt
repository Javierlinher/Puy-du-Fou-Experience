package com.example.puydufouexperience.ui.mapa

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.network.RoutesApiClient
import com.example.puydufouexperience.data.repository.MapaRepository
import com.example.puydufouexperience.data.repository.RutasRepository
import com.example.puydufouexperience.data.seed.SeedData
import com.example.puydufouexperience.databinding.FragmentMapaBinding
import com.example.puydufouexperience.model.entity.ParadaRuta
import com.example.puydufouexperience.model.entity.Ruta
import com.example.puydufouexperience.utils.PolylineUtils
import com.example.puydufouexperience.utils.SessionManager
import com.example.puydufouexperience.viewmodel.mapa.MapaViewModel
import com.example.puydufouexperience.viewmodel.mapa.MapaViewModelFactory
import com.example.puydufouexperience.viewmodel.mapa.ParadaTemp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MapaFragment : Fragment() {

    companion object {
        // ✅ Desde RutasFragment: iniciar modo creación en el mapa
        const val ARG_INICIAR_CREACION = "arg_iniciar_creacion_ruta"

        // ✅ Desde RutaDetalleFragment: ver/dibujar una ruta guardada en el mapa
        const val ARG_VER_RUTA_ID = "arg_ver_ruta_id"

        // ✅ Desde Detalle (Espectáculo/Restaurante): ruta rápida hacia un destino
        const val ARG_DEST_LAT = "arg_dest_lat"
        const val ARG_DEST_LNG = "arg_dest_lng"
        const val ARG_DEST_NAME = "arg_dest_name"
    }

    private var _binding: FragmentMapaBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapaViewModel
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Para no recentrar la cámara cada vez que el flow emite
    private var cameraCentrada = false

    // Ruta actual (polyline real) para poder borrarla al recalcular
    private var rutaPolyline: Polyline? = null

    // Flechas para indicar dirección (solo cuando hay ruta dibujada)
    private val flechaMarkers: MutableList<Marker> = mutableListOf()

    // Markers temporales de paradas (modo creación)
    private val paradaMarkers: MutableList<Marker> = mutableListOf()
    private var lineaParadas: Polyline? = null

    // =========================================================
    // Seguimiento de ruta (ON/OFF)
    // =========================================================

    private var seguimientoActivo: Boolean = false

    // Guardamos “qué ruta estoy siguiendo” para recalcular sin preguntarte:
    // - destino final
    // - waypoints (si es ruta guardada)
    private var seguimientoDestLat: Double? = null
    private var seguimientoDestLng: Double? = null
    private var seguimientoWaypoints: List<Pair<Double, Double>> = emptyList()
    private var seguimientoNombre: String = "Destino"

    // Control de gasto: recalcular solo si te mueves X metros o pasa X tiempo
    private var ultimoOrigenRecalc: Location? = null
    private var ultimoRecalcMs: Long = 0L

    private val THRESHOLD_METROS = 30f        // recalcula si te mueves >= 30m
    private val THRESHOLD_TIEMPO_MS = 15_000L // o si pasan >= 15s

    private val locationCallbackSeguimiento = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            if (!seguimientoActivo) return

            val destLat = seguimientoDestLat ?: return
            val destLng = seguimientoDestLng ?: return

            val ahora = System.currentTimeMillis()
            val ultimaLoc = ultimoOrigenRecalc
            val distancia = if (ultimaLoc != null) loc.distanceTo(ultimaLoc) else Float.MAX_VALUE
            val tiempo = abs(ahora - ultimoRecalcMs)

            // ✅ Recalcular si cumple umbral (o si es la primera vez)
            if (ultimaLoc == null || distancia >= THRESHOLD_METROS || tiempo >= THRESHOLD_TIEMPO_MS) {
                ultimoOrigenRecalc = loc
                ultimoRecalcMs = ahora

                // Recalcular ruta real desde tu posición
                lifecycleScope.launch {
                    recalcularRutaDesdeUbicacionActual(
                        originLat = loc.latitude,
                        originLng = loc.longitude,
                        destLat = destLat,
                        destLng = destLng,
                        waypoints = seguimientoWaypoints,
                        destinoNombre = seguimientoNombre,
                        centrarCamara = false // no “robar” cámara cada recalculo
                    )
                }
            }
        }
    }

    // Launcher para pedir permiso de ubicación en runtime
    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val map = googleMap ?: return@registerForActivityResult
            if (granted) {
                activarMiUbicacionYCentra(map)

                // Si veníamos a dibujar ruta guardada, reintenta
                dibujarRutaGuardadaSiProcede()

                // Si veníamos desde detalle, reintenta
                dibujarRutaRapidaDesdeArgsSiProcede()
            } else {
                centrarEnParquesol(map)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // 1) ViewModel (MVVM)
        val db = DatabaseProvider.get(requireContext().applicationContext)
        val repo = MapaRepository(db.espectaculoDao(), db.restauranteDao())
        viewModel = ViewModelProvider(this, MapaViewModelFactory(repo))[MapaViewModel::class.java]

        // 2) Botón "Rutas" -> ir a la lista de rutas
        binding.btnRutas.setOnClickListener {
            findNavController().navigate(R.id.action_mapaFragment_to_rutasFragment)
        }

        // 3) Botón "Seguir / Parar" -> seguimiento de la ruta que esté dibujada
        binding.btnSeguirRuta.setOnClickListener {
            if (seguimientoActivo) pararSeguimiento() else iniciarSeguimiento()
        }
        pintarEstadoSeguimiento()

        // 4) Guardar / Cancelar (solo modo creación)
        binding.btnCancelarRuta.setOnClickListener {
            // No tiene sentido seguir ruta mientras creas rutas
            pararSeguimiento()
            viewModel.cancelarCreacion()
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_creation_cancelled),
                Toast.LENGTH_SHORT
            ).show()

        }

        binding.btnGuardarRuta.setOnClickListener {
            pedirNombreYGuardarRuta()
        }

        // 5) SupportMapFragment dentro del contenedor
        val mapFragment =
            (childFragmentManager.findFragmentByTag("map") as? SupportMapFragment)
                ?: SupportMapFragment.newInstance().also {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.mapContainer, it, "map")
                        .commit()
                }

        // 6) BottomSheet -> "Ir hasta aquí" (ruta rápida)
        childFragmentManager.setFragmentResultListener(
            PoiBottomSheetDialogFragment.REQUEST_ROUTE,
            viewLifecycleOwner
        ) { _, bundle ->
            val destLat = bundle.getDouble(PoiBottomSheetDialogFragment.KEY_LAT)
            val destLng = bundle.getDouble(PoiBottomSheetDialogFragment.KEY_LNG)
            val name = bundle.getString(PoiBottomSheetDialogFragment.KEY_NAME).orEmpty()

            // Ruta rápida: sin waypoints
            calcularYMostrarRutaRapida(destLat, destLng, name)
        }

        // 7) Cuando el mapa esté listo
        mapFragment.getMapAsync { map ->
            googleMap = map

            // Long press: añadir punto libre si estamos creando
            map.setOnMapLongClickListener { latLng ->
                if (viewModel.modoCreacion.value) {
                    val etiqueta = "Punto ${viewModel.paradasTemp.value.size + 1}"
                    viewModel.addParadaPunto(latLng.latitude, latLng.longitude, etiqueta)
                }
            }

            // Click en marker:
            // - Si modo creación y marker es parada "1. ...": borrar parada
            // - Si modo creación y marker es POI: añadir parada (sin abrir bottomsheet)
            // - Si NO modo creación: abrir bottomsheet
            map.setOnMarkerClickListener { marker ->
                if (viewModel.modoCreacion.value) {
                    val titulo = marker.title ?: ""
                    if (titulo.contains(". ")) {
                        val orden = titulo.substringBefore(".").toIntOrNull()
                        if (orden != null) {
                            viewModel.removeParadaByOrden(orden)
                            return@setOnMarkerClickListener true
                        }
                    }
                }

                val poi = marker.tag as? PoiUi ?: return@setOnMarkerClickListener false

                if (viewModel.modoCreacion.value) {
                    viewModel.addParadaPoi(poi)
                    true
                } else {
                    PoiBottomSheetDialogFragment
                        .newInstance(poi.tipo, poi.id)
                        .show(childFragmentManager, "poi_sheet")
                    true
                }
            }

            // Centrar + pintar POIs
            intentarCentrarEnMiUbicacion(map)
            observarPoisYPintar()

            // Observar modo creación + paradas
            observarModoCreacionYParadas()

            // Cargar POIs
            viewModel.cargar()

            // ✅ Si venimos desde RutasFragment: iniciar modo creación
            val iniciarCreacion = arguments?.getBoolean(ARG_INICIAR_CREACION, false) ?: false
            if (iniciarCreacion) {
                pararSeguimiento()
                clearRutaReal()

                viewModel.iniciarCreacion()
                arguments?.putBoolean(ARG_INICIAR_CREACION, false)
            }

            // ✅ Si venimos desde detalle: ruta rápida hacia destino
            dibujarRutaRapidaDesdeArgsSiProcede()

            // ✅ Si venimos desde detalle: dibujar ruta guardada
            dibujarRutaGuardadaSiProcede()
        }
    }

    // =========================================================
    // ✅ Ruta rápida desde args (detalle espectáculo/restaurante)
    // =========================================================

    private fun dibujarRutaRapidaDesdeArgsSiProcede() {
        val lat = arguments?.getDouble(ARG_DEST_LAT, Double.NaN) ?: Double.NaN
        val lng = arguments?.getDouble(ARG_DEST_LNG, Double.NaN) ?: Double.NaN
        val name = arguments?.getString(ARG_DEST_NAME).orEmpty()

        if (lat.isNaN() || lng.isNaN()) return
        if (viewModel.modoCreacion.value) return

        // Consumimos args para que no se repita al rotar
        arguments?.remove(ARG_DEST_LAT)
        arguments?.remove(ARG_DEST_LNG)
        arguments?.remove(ARG_DEST_NAME)

        calcularYMostrarRutaRapida(lat, lng, if (name.isBlank()) "Destino" else name)
    }

    // =========================================================
    // UI MODO CREACIÓN
    // =========================================================

    private fun observarModoCreacionYParadas() {
        // Mostrar/ocultar botones inferiores
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.modoCreacion.collect { modo ->
                binding.routeActions.visibility = if (modo) View.VISIBLE else View.GONE

                // Si entras en modo creación, deshabilitamos "Seguir"
                pintarEstadoSeguimiento()

                if (modo) {
                    Toast.makeText(
                        requireContext(),
                        "Modo ruta: toca POIs y mantén pulsado para puntos.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    limpiarUiCreacionRuta()
                }
            }
        }

        // Repintar paradas temporales (markers + línea simple)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.paradasTemp.collect { paradas ->
                pintarParadasTemporales(paradas)
            }
        }
    }

    private fun pintarParadasTemporales(paradas: List<ParadaTemp>) {
        val map = googleMap ?: return

        // Borrar markers temporales anteriores
        for (m in paradaMarkers) m.remove()
        paradaMarkers.clear()

        // Borrar línea anterior
        lineaParadas?.remove()
        lineaParadas = null

        if (paradas.isEmpty()) return

        val puntos = mutableListOf<LatLng>()

        // Pintar cada parada con su número
        for (p in paradas) {
            val latLng = LatLng(p.lat, p.lng)
            puntos.add(latLng)

            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("${p.orden}. ${p.etiqueta}")
            )
            if (marker != null) paradaMarkers.add(marker)
        }

        // Línea simple conectando puntos (solo visual)
        if (puntos.size >= 2) {
            lineaParadas = map.addPolyline(
                PolylineOptions()
                    .addAll(puntos)
                    .width(8f)
            )
        }
    }

    private fun limpiarUiCreacionRuta() {
        for (m in paradaMarkers) m.remove()
        paradaMarkers.clear()

        lineaParadas?.remove()
        lineaParadas = null
    }

    private fun pedirNombreYGuardarRuta() {
        val paradas = viewModel.paradasTemp.value
        if (paradas.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_add_at_least_one_stop),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val input = EditText(requireContext()).apply { hint = "Nombre de la ruta" }

        AlertDialog.Builder(requireContext())
            .setTitle("Guardar ruta")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = input.text.toString().trim().ifBlank { "Ruta ${fechaCorta()}" }
                guardarRutaEnRoom(nombre, paradas)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarRutaEnRoom(nombre: String, paradasTemp: List<ParadaTemp>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val idUsuario = SessionManager.getIdUsuarioActual(requireContext())
            if (idUsuario <= 0) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_no_active_session),
                    Toast.LENGTH_LONG
                ).show()

                return@launch
            }

            val db = DatabaseProvider.get(requireContext().applicationContext)
            val rutasRepo = RutasRepository(db.rutaDao(), db.paradaRutaDao())

            val ruta = Ruta(
                id = 0,
                idUsuario = idUsuario,
                nombre = nombre,
                fechaCreacion = System.currentTimeMillis()
            )

            val paradasEntity = paradasTemp.map { p ->
                ParadaRuta(
                    id = 0,
                    idRuta = 0, // se rellena tras insertar Ruta
                    orden = p.orden,
                    tipoElemento = p.tipo,
                    idElemento = p.idElemento,
                    latitud = p.lat,
                    longitud = p.lng,
                    etiqueta = p.etiqueta
                )
            }

            try {
                rutasRepo.crearRutaConParadas(ruta, paradasEntity)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_route_saved),
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.cancelarCreacion()
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_route_save_error),
                    Toast.LENGTH_LONG
                ).show()

            }
        }
    }

    private fun fechaCorta(): String =
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())

    // =========================================================
    // RUTA RÁPIDA (BOTTOMSHEET + DETALLE)
    // =========================================================

    private fun calcularYMostrarRutaRapida(destLat: Double, destLng: Double, destinoNombre: String) {
        if (viewModel.modoCreacion.value) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_exit_creation_mode_for_quick_route),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_location_required_for_route),
                Toast.LENGTH_SHORT
            ).show()

            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_location_fetch_error),
                    Toast.LENGTH_SHORT
                ).show()

                return@addOnSuccessListener
            }

            val originLat = loc.latitude
            val originLng = loc.longitude

            // Guardamos la “ruta actual” para seguimiento
            seguimientoDestLat = destLat
            seguimientoDestLng = destLng
            seguimientoWaypoints = emptyList()
            seguimientoNombre = destinoNombre

            viewLifecycleOwner.lifecycleScope.launch {
                recalcularRutaDesdeUbicacionActual(
                    originLat = originLat,
                    originLng = originLng,
                    destLat = destLat,
                    destLng = destLng,
                    waypoints = emptyList(),
                    destinoNombre = destinoNombre,
                    centrarCamara = true
                )
                // Tras dibujar, actualiza estado del botón “Seguir”
                pintarEstadoSeguimiento()
            }
        }
    }

    // =========================================================
    // VER RUTA GUARDADA (WAYPOINTS) — si llega ARG_VER_RUTA_ID
    // =========================================================

    private fun dibujarRutaGuardadaSiProcede() {
        val idRuta = arguments?.getInt(ARG_VER_RUTA_ID, -1) ?: -1
        if (idRuta <= 0) return
        if (viewModel.modoCreacion.value) return

        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_location_required_to_view_route),
                Toast.LENGTH_SHORT
            ).show()

            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val db = DatabaseProvider.get(requireContext().applicationContext)
            val repo = RutasRepository(db.rutaDao(), db.paradaRutaDao())

            val paradas = repo.getParadasOrdenadas(idRuta)
            if (paradas.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_route_has_no_stops),
                    Toast.LENGTH_SHORT
                ).show()

                arguments?.putInt(ARG_VER_RUTA_ID, -1)
                return@launch
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc == null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_location_fetch_error),
                        Toast.LENGTH_SHORT
                    ).show()

                    arguments?.putInt(ARG_VER_RUTA_ID, -1)
                    return@addOnSuccessListener
                }

                val originLat = loc.latitude
                val originLng = loc.longitude

                val destino = paradas.last()
                val destLat = destino.latitud
                val destLng = destino.longitud

                val waypoints = paradas.dropLast(1).map { it.latitud to it.longitud }

                // Guardamos la “ruta actual” para seguimiento
                seguimientoDestLat = destLat
                seguimientoDestLng = destLng
                seguimientoWaypoints = waypoints
                seguimientoNombre = "Ruta guardada"

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        recalcularRutaDesdeUbicacionActual(
                            originLat = originLat,
                            originLng = originLng,
                            destLat = destLat,
                            destLng = destLng,
                            waypoints = waypoints,
                            destinoNombre = "Ruta guardada",
                            centrarCamara = true
                        )

                        Toast.makeText(
                            requireContext(),
                            "Ruta cargada: ${paradas.size} paradas",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (_: Exception) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_route_display_error),
                            Toast.LENGTH_LONG
                        ).show()

                    } finally {
                        // Evita redibujar por rotación/recreación
                        arguments?.putInt(ARG_VER_RUTA_ID, -1)

                        // Tras dibujar, actualiza estado del botón “Seguir”
                        pintarEstadoSeguimiento()
                    }
                }
            }
        }
    }

    // =========================================================
    // NÚCLEO: recalcular y pintar ruta real (polyline + flechas)
    // =========================================================

    private suspend fun recalcularRutaDesdeUbicacionActual(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        waypoints: List<Pair<Double, Double>>,
        destinoNombre: String,
        centrarCamara: Boolean
    ) {
        val map = googleMap ?: return

        try {
            val result = withContext(Dispatchers.IO) {
                if (waypoints.isEmpty()) {
                    RoutesApiClient.computeRoute(
                        originLat = originLat,
                        originLng = originLng,
                        destLat = destLat,
                        destLng = destLng
                    )
                } else {
                    RoutesApiClient.computeRouteWithWaypoints(
                        originLat = originLat,
                        originLng = originLng,
                        waypoints = waypoints,
                        destLat = destLat,
                        destLng = destLng
                    )
                }
            }

            val points = PolylineUtils.decode(result.encodedPolyline)

            // Quitamos ruta anterior y flechas anteriores
            rutaPolyline?.remove()
            clearFlechas()

            // Dibujamos polyline de la ruta
            rutaPolyline = map.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(10f)
            )

            // ✅ Flechitas (densidad 5)
            drawFlechas(points, step = 5)

            if (centrarCamara) {
                centrarCamaraEnRuta(originLat, originLng, destLat, destLng)
            }

            val km = result.distanceMeters / 1000.0
            val min = result.durationSeconds / 60.0

            Toast.makeText(
                requireContext(),
                "Ruta a $destinoNombre: ${"%.2f".format(km)} km · ${"%.0f".format(min)} min",
                Toast.LENGTH_LONG
            ).show()

        } catch (_: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_route_calculation_error),
                Toast.LENGTH_LONG
            ).show()

        }
    }

    private fun clearRutaReal() {
        rutaPolyline?.remove()
        rutaPolyline = null
        clearFlechas()
        pintarEstadoSeguimiento()
    }

    // =========================================================
    // Flechas de dirección sobre la ruta
    // =========================================================

    private fun clearFlechas() {
        for (m in flechaMarkers) m.remove()
        flechaMarkers.clear()
    }

    private fun bearingBetween(a: LatLng, b: LatLng): Float {
        val lat1 = Math.toRadians(a.latitude)
        val lon1 = Math.toRadians(a.longitude)
        val lat2 = Math.toRadians(b.latitude)
        val lon2 = Math.toRadians(b.longitude)

        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        val brng = Math.toDegrees(Math.atan2(y, x))
        return ((brng + 360) % 360).toFloat()
    }

    private fun drawFlechas(points: List<LatLng>, step: Int) {
        val map = googleMap ?: return
        clearFlechas()

        if (points.size < 2) return

        // ⚠️ Necesitas un drawable ic_arrow_route
        val icon = getMarkerIcon(R.drawable.ic_arrow_route) ?: return

        var i = 0
        while (i + 1 < points.size) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val bearing = bearingBetween(p1, p2)

            val m = map.addMarker(
                MarkerOptions()
                    .position(p1)
                    .icon(icon)
                    .anchor(0.5f, 0.5f)
                    .rotation(bearing)
                    .flat(true)
            )
            if (m != null) flechaMarkers.add(m)

            i += step
        }
    }

    // =========================================================
    // Seguimiento ON/OFF
    // =========================================================

    private fun iniciarSeguimiento() {
        if (viewModel.modoCreacion.value) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_cannot_follow_route_in_creation_mode),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Solo tiene sentido si ya hay ruta dibujada
        if (seguimientoDestLat == null || seguimientoDestLng == null || rutaPolyline == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_calculate_route_first),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_location_required_for_tracking),
                Toast.LENGTH_SHORT
            ).show()

            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        // Reseteamos control de recálculo
        ultimoOrigenRecalc = null
        ultimoRecalcMs = 0L

        // Updates moderados para no gastar demasiado
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallbackSeguimiento,
            Looper.getMainLooper()
        )

        seguimientoActivo = true
        pintarEstadoSeguimiento()
        Toast.makeText(
            requireContext(),
            getString(R.string.toast_tracking_enabled),
            Toast.LENGTH_SHORT
        ).show()

    }

    private fun pararSeguimiento() {
        if (!seguimientoActivo) {
            pintarEstadoSeguimiento()
            return
        }

        fusedLocationClient.removeLocationUpdates(locationCallbackSeguimiento)
        seguimientoActivo = false
        pintarEstadoSeguimiento()
        Toast.makeText(
            requireContext(),
            getString(R.string.toast_tracking_disabled),
            Toast.LENGTH_SHORT
        ).show()

    }

    private fun pintarEstadoSeguimiento() {
        // Texto del botón según estado
        binding.btnSeguirRuta.text = if (seguimientoActivo) "Parar" else "Seguir"

        // ✅ Solo habilitado si hay ruta dibujada y NO estamos creando
        binding.btnSeguirRuta.isEnabled = (rutaPolyline != null && !viewModel.modoCreacion.value)

        // Para que no “desaparezca” visualmente cuando está deshabilitado
        binding.btnSeguirRuta.alpha = if (binding.btnSeguirRuta.isEnabled) 1f else 0.5f
    }

    // =========================================================
    // UBICACIÓN + POIs (fase 9)
    // =========================================================

    private fun centrarCamaraEnRuta(originLat: Double, originLng: Double, destLat: Double, destLng: Double) {
        val map = googleMap ?: return

        val bounds = LatLngBounds.Builder()
            .include(LatLng(originLat, originLng))
            .include(LatLng(destLat, destLng))
            .build()

        map.setOnMapLoadedCallback {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140))
        }
    }

    private fun intentarCentrarEnMiUbicacion(map: GoogleMap) {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine) {
            activarMiUbicacionYCentra(map)
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun activarMiUbicacionYCentra(map: GoogleMap) {
        try {
            map.isMyLocationEnabled = true
        } catch (_: SecurityException) {
            centrarEnParquesol(map)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val yo = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(yo, 15.5f))
                    cameraCentrada = true
                } else {
                    centrarEnParquesol(map)
                }
            }
            .addOnFailureListener {
                centrarEnParquesol(map)
            }
    }

    private fun centrarEnParquesol(map: GoogleMap) {
        val parquesol = LatLng(SeedData.PARQUESOL_LAT, SeedData.PARQUESOL_LNG)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(parquesol, 15.5f))
        cameraCentrada = true
    }

    /**
     * Observa POIs y pinta markers.
     * OJO: usamos map.clear(), así que se borran overlays (ruta/flechas).
     */
    private fun observarPoisYPintar() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pois.collect { pois ->
                val map = googleMap ?: return@collect

                map.clear()

                // Al limpiar el mapa, perdemos overlays
                rutaPolyline = null
                clearFlechas()
                pintarEstadoSeguimiento()

                val iconoEspectaculo = getMarkerIcon(R.drawable.ic_marker_espectaculo)
                val iconoRestaurante = getMarkerIcon(R.drawable.ic_marker_restaurante)

                for (poi in pois) {
                    val icono = when (poi.tipo) {
                        PoiUi.TIPO_ESPECTACULO -> iconoEspectaculo
                        PoiUi.TIPO_RESTAURANTE -> iconoRestaurante
                        else -> null
                    }

                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(LatLng(poi.lat, poi.lng))
                            .title(poi.nombre)
                            .icon(icono ?: BitmapDescriptorFactory.defaultMarker())
                    )
                    marker?.tag = poi
                }

                if (!cameraCentrada) {
                    centrarEnParquesol(map)
                }
            }
        }
    }

    /**
     * Convierte un drawable a BitmapDescriptor para markers.
     */
    private fun getMarkerIcon(drawableRes: Int): BitmapDescriptor? {
        val drawable = ContextCompat.getDrawable(requireContext(), drawableRes) ?: return null

        val w = drawable.intrinsicWidth.coerceAtLeast(96)
        val h = drawable.intrinsicHeight.coerceAtLeast(96)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Por seguridad, paramos seguimiento al salir del fragment
        pararSeguimiento()

        _binding = null
        googleMap = null
    }
}
