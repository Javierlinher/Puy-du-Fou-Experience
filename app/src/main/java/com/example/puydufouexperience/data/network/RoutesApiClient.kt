package com.example.puydufouexperience.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class RouteResult(
    val encodedPolyline: String,
    val distanceMeters: Int,
    val durationSeconds: Int
)

/**
 * Cliente simple para Google Routes API (REST).
 * - Usa ROUTES_API_KEY (BuildConfig)
 * - HttpURLConnection (sin Retrofit)
 * - Soporta ruta simple y con waypoints (intermediates)
 */
object RoutesApiClient {

    private const val TAG = "RoutesApiClient"

    /**
     * Ruta simple: origen -> destino
     */
    suspend fun computeRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): RouteResult {
        return computeRouteWithWaypoints(
            originLat = originLat,
            originLng = originLng,
            waypoints = emptyList(),
            destLat = destLat,
            destLng = destLng
        )
    }

    /**
     * Ruta con waypoints:
     * origin -> (waypoints...) -> destination
     *
     * En Routes API v2, los waypoints van en "intermediates".
     */
    suspend fun computeRouteWithWaypoints(
        originLat: Double,
        originLng: Double,
        waypoints: List<Pair<Double, Double>>, // (lat, lng)
        destLat: Double,
        destLng: Double
    ): RouteResult = withContext(Dispatchers.IO) {

        val apiKey = com.example.puydufouexperience.BuildConfig.ROUTES_API_KEY
        if (apiKey.isBlank()) {
            Log.e(TAG, "ROUTES_API_KEY vacía. Revisa local.properties y Sync/Rebuild.")
            throw IllegalStateException("ROUTES_API_KEY vacía")
        }

        val url = URL("https://routes.googleapis.com/directions/v2:computeRoutes")

        // Construimos intermediates si hay waypoints
        val intermediatesJson = if (waypoints.isNotEmpty()) {
            waypoints.joinToString(prefix = "[", postfix = "]") { (lat, lng) ->
                """
                {
                  "location": {
                    "latLng": {
                      "latitude": $lat,
                      "longitude": $lng
                    }
                  }
                }
                """.trimIndent()
            }
        } else {
            "[]"
        }

        val body = """
            {
              "origin": {
                "location": {
                  "latLng": {
                    "latitude": $originLat,
                    "longitude": $originLng
                  }
                }
              },
              "destination": {
                "location": {
                  "latLng": {
                    "latitude": $destLat,
                    "longitude": $destLng
                  }
                }
              },
              "intermediates": $intermediatesJson,
              "travelMode": "WALK"
            }
        """.trimIndent()

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000

            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("X-Goog-Api-Key", apiKey)

            // Pedimos solo lo necesario
            setRequestProperty(
                "X-Goog-FieldMask",
                "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline"
            )
        }

        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val success = code in 200..299

            val stream = if (success) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            if (!success) {
                Log.e(TAG, "HTTP $code ERROR\n$response")
                throw RuntimeException("Routes API error $code")
            }

            Log.d(TAG, "HTTP $code OK\n$response")

            val json = JSONObject(response)
            val route = json.getJSONArray("routes").getJSONObject(0)

            val polyline = route.getJSONObject("polyline").getString("encodedPolyline")
            val distance = route.getInt("distanceMeters")

            // duration viene como "123s"
            val duration = route.getString("duration").replace("s", "").toInt()

            RouteResult(
                encodedPolyline = polyline,
                distanceMeters = distance,
                durationSeconds = duration
            )
        } finally {
            connection.disconnect()
        }
    }
}
