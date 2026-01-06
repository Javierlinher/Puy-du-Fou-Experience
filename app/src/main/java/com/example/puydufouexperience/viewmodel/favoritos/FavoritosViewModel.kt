package com.example.puydufouexperience.viewmodel.favoritos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puydufouexperience.data.repository.FavoritosRepository
import com.example.puydufouexperience.ui.favoritos.FavoritoUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FavoritosViewModel(
    private val favoritosRepository: FavoritosRepository,
    private val idUsuarioActual: Int,
    private val resolverNombres: suspend (List<Int>, List<Int>, List<Int>) -> Triple<List<FavoritoUi>, List<FavoritoUi>, List<FavoritoUi>>
) : ViewModel() {

    // Estado UI: 3 listas por categoría
    private val _favoritosEsp = MutableStateFlow<List<FavoritoUi>>(emptyList())
    val favoritosEsp: StateFlow<List<FavoritoUi>> = _favoritosEsp

    private val _favoritosRes = MutableStateFlow<List<FavoritoUi>>(emptyList())
    val favoritosRes: StateFlow<List<FavoritoUi>> = _favoritosRes

    private val _favoritosRut = MutableStateFlow<List<FavoritoUi>>(emptyList())
    val favoritosRut: StateFlow<List<FavoritoUi>> = _favoritosRut

    fun cargar() {
        // Carga de favoritos y cálculo de listas UI
        viewModelScope.launch {
            val favoritos = favoritosRepository.getByUsuario(idUsuarioActual)

            // Separamos ids por tipo (polimorfismo)
            val idsEsp = favoritos.filter { it.tipoElemento == TIPO_ESPECTACULO }.map { it.idElemento }.distinct()
            val idsRes = favoritos.filter { it.tipoElemento == TIPO_RESTAURANTE }.map { it.idElemento }.distinct()
            val idsRut = favoritos.filter { it.tipoElemento == TIPO_RUTA }.map { it.idElemento }.distinct()

            // Resolver nombres (se hace fuera para no acoplar ViewModel a Room directamente)
            val resolved = resolverNombres(idsEsp, idsRes, idsRut)

            _favoritosEsp.value = resolved.first
            _favoritosRes.value = resolved.second
            _favoritosRut.value = resolved.third
        }
    }

    companion object {
        private const val TIPO_ESPECTACULO = "ESPECTACULO"
        private const val TIPO_RESTAURANTE = "RESTAURANTE"
        private const val TIPO_RUTA = "RUTA"
    }
}

class FavoritosViewModelFactory(
    private val favoritosRepository: FavoritosRepository,
    private val idUsuarioActual: Int,
    private val resolverNombres: suspend (List<Int>, List<Int>, List<Int>) -> Triple<List<FavoritoUi>, List<FavoritoUi>, List<FavoritoUi>>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FavoritosViewModel(favoritosRepository, idUsuarioActual, resolverNombres) as T
    }
}
