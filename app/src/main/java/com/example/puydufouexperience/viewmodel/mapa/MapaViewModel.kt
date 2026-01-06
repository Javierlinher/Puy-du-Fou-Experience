package com.example.puydufouexperience.viewmodel.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puydufouexperience.data.repository.MapaRepository
import com.example.puydufouexperience.ui.mapa.PoiUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Parada temporal (solo en memoria) mientras creas una ruta.
 * Luego se transforma a ParadaRuta (entity) para guardarla en Room.
 */
data class ParadaTemp(
    val orden: Int,
    val tipo: String,      // "ESPECTACULO" | "RESTAURANTE" | "PUNTO"
    val idElemento: Int?,  // null si tipo == PUNTO
    val lat: Double,
    val lng: Double,
    val etiqueta: String
)

class MapaViewModel(
    private val repo: MapaRepository
) : ViewModel() {

    private val _pois = MutableStateFlow<List<PoiUi>>(emptyList())
    val pois: StateFlow<List<PoiUi>> = _pois

    private val _modoCreacion = MutableStateFlow(false)
    val modoCreacion: StateFlow<Boolean> = _modoCreacion

    private val _paradasTemp = MutableStateFlow<List<ParadaTemp>>(emptyList())
    val paradasTemp: StateFlow<List<ParadaTemp>> = _paradasTemp

    fun cargar() {
        viewModelScope.launch {
            _pois.value = repo.getPois()
        }
    }

    fun iniciarCreacion() {
        _modoCreacion.value = true
        _paradasTemp.value = emptyList()
    }

    fun cancelarCreacion() {
        _modoCreacion.value = false
        _paradasTemp.value = emptyList()
    }

    /**
     * Añade una parada POI (espectáculo/restaurante).
     * Regla: no permitir duplicados del mismo elemento en una misma ruta.
     */
    fun addParadaPoi(poi: PoiUi) {
        if (!_modoCreacion.value) return

        val tipoElemento = when (poi.tipo) {
            PoiUi.TIPO_ESPECTACULO -> "ESPECTACULO"
            PoiUi.TIPO_RESTAURANTE -> "RESTAURANTE"
            else -> "PUNTO"
        }

        // ✅ evitar duplicados por (tipo + idElemento)
        val existe = _paradasTemp.value.any { it.tipo == tipoElemento && it.idElemento == poi.id }
        if (existe) return

        val orden = _paradasTemp.value.size + 1
        val parada = ParadaTemp(
            orden = orden,
            tipo = tipoElemento,
            idElemento = poi.id,
            lat = poi.lat,
            lng = poi.lng,
            etiqueta = poi.nombre
        )

        _paradasTemp.value = _paradasTemp.value + parada
    }

    /**
     * Punto libre (long press).
     */
    fun addParadaPunto(lat: Double, lng: Double, etiqueta: String) {
        if (!_modoCreacion.value) return

        val orden = _paradasTemp.value.size + 1
        val parada = ParadaTemp(
            orden = orden,
            tipo = "PUNTO",
            idElemento = null,
            lat = lat,
            lng = lng,
            etiqueta = etiqueta
        )
        _paradasTemp.value = _paradasTemp.value + parada
    }

    /**
     * Eliminar parada tocando su marker "1. ...".
     * Luego reordenamos 1..N.
     */
    fun removeParadaByOrden(orden: Int) {
        val nuevas = _paradasTemp.value
            .filterNot { it.orden == orden }
            .mapIndexed { index, p -> p.copy(orden = index + 1) }

        _paradasTemp.value = nuevas
    }
}

class MapaViewModelFactory(
    private val repo: MapaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MapaViewModel(repo) as T
    }
}
