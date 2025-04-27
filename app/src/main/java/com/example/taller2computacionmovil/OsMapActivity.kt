package com.example.taller2computacionmovil

import android.app.UiModeManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller2computacionmovil.databinding.ActivityOsMapBinding
import com.example.taller2computacionmovil.model.MyLocation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import org.json.JSONArray
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.util.Date


class OsMapActivity : AppCompatActivity() {

    val RADIUS_EARTH_METERS = 6378137;
    private lateinit var binding: ActivityOsMapBinding
    private lateinit var map: MapView
    private val bogota = GeoPoint(4.62, -74.07)
    private var longPressedMarker: Marker? = null
    private lateinit var geocoder: Geocoder
    private lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null
    lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var locationActual: GeoPoint? = null
    private var markerLocationActual: Marker? = null
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var sensorEventListener: SensorEventListener
    private val locations = JSONArray()
    private var locationBuscada: GeoPoint? = null
    private var locationLongPressed: GeoPoint? = null


    val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if (it.resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "El GPS está apagado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            if (it) {
                locationSettings()
            } else {
                Toast.makeText(this, "No hay permiso para acceder al GPS", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOsMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inicializarSensor()
        inicializarMapa()
        inicializarSuscrLocalizacion()
        inicializarListenersBotones()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    fun inicializarListenersBotones() {
        binding.address.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                val text = binding.address.text.toString()
                val latlong = findLocation(text)
                if (this@OsMapActivity::map.isInitialized) {
                    if (latlong != null) {
                        val loc = GeoPoint(latlong.latitude, latlong.longitude)
                        val address = findAddress(latlong)
                        locationBuscada = loc
                        drawRouteTresPuntos()
                        if (!address.isNullOrEmpty()) {
                            map.controller.animateTo(loc)
                            map.controller.setZoom(18.0)
                            addMarker(loc, address, false)
                        }
                    }
                }
            }
            true
        }
        binding.drawRutasJson.setOnClickListener {
            drawRoutesDeJson()
        }
    }

    fun inicializarSuscrLocalizacion() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()
    }

    fun inicializarMapa() {
        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )
        map = binding.osmMap
        map.setTileSource(
            TileSourceFactory.MAPNIK
        )
        map.setMultiTouchControls(true)
        map.overlays.add(createOverlayEvents())
        roadManager = OSRMRoadManager(this, "ANDROID")
        geocoder = Geocoder(this)
    }

    fun inicializarSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorEventListener = createSensorEventListener()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            sensorEventListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        map.onResume()
        map.controller.setZoom(18.0)
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationSettings()
        } else {
            locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val uims = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uims.nightMode == UiModeManager.MODE_NIGHT_YES) {
            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }
        locationActual?.let {
            map.controller.animateTo(it)
        } ?: run {
            map.controller.animateTo(bogota)
        }
    }

    private fun createSensorEventListener(): SensorEventListener {
        val sel = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (this@OsMapActivity::map.isInitialized) {
                    if (event != null) {
                        // El valor de 5000 es mejor calibrarlo con un dispositivo real
                        if (event.values[0] > 5000) {
                            // Modo claro - quitar filtro de color
                            map.overlayManager.tilesOverlay.setColorFilter(null)
                        } else {
                            // Modo oscuro - aplicar filtro de inversión de colores
                            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        return sel
    }


    override fun onPause() {
        super.onPause()
        map.onPause()
        stopLocationUpdates()
        sensorManager.unregisterListener(sensorEventListener)
    }

    fun createOverlayEvents(): MapEventsOverlay {
        val overlayEvents = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    longPressOnMap(p)
                    drawRouteTresPuntos()
                        ?: run {
                    }
                }
                return true
            }
        })
        return overlayEvents
    }

    fun longPressOnMap(p: GeoPoint) {
        if (longPressedMarker != null)
            map.getOverlays().remove(longPressedMarker)
        val address = findAddress(LatLng(p.latitude, p.longitude))
        locationLongPressed = p
        val snippet: String
        if (address != null) {
            snippet = address
        } else {
            snippet = ""
        }
        addMarker(p, snippet, true)
    }

    fun addMarker(p: GeoPoint, snippet: String, longPressed: Boolean) {
        if (longPressed) {
            longPressedMarker =
                createMarker(
                    p, "Ubicación long pressed", snippet, R.drawable.baseline_location_alt_24
                )
            if (longPressedMarker != null) {
                map.getOverlays().add(longPressedMarker)
            }
            locationActual?.let {
                var distancia = distance(it.latitude, it.longitude, p.latitude, p.longitude)
                Toast.makeText(baseContext,"Distancia: $distancia",Toast.LENGTH_SHORT).show()
            }
        } else {
            val searchMarker = createMarker(
                p, "Ubicación por busqueda", snippet, R.drawable.baseline_location_alt_24
            )
            map.overlays.add(searchMarker)
            locationActual?.let {
                var distancia = distance(it.latitude, it.longitude, p.latitude, p.longitude)
                Toast.makeText(baseContext,"Distancia: $distancia",Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun findAddress(location: LatLng): String? {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 2)
        if (addresses != null && !addresses.isEmpty()) {
            val addr = addresses.get(0)
            val locname = addr.getAddressLine(0)
            return locname
        }
        return null
    }

    fun findLocation(address: String): LatLng? {
        val addresses = geocoder.getFromLocationName(address, 2)
        if (addresses != null && !addresses.isEmpty()) {
            val addr = addresses.get(0)
            val location = LatLng(
                addr.latitude, addr.longitude
            )
            return location
        }
        return null
    }

    fun createMarker(p: GeoPoint, title: String, desc: String, iconID: Int): Marker? {
        var marker: Marker? = null;
        if (map != null) {
            marker = Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                val myIcon = getResources().getDrawable(iconID, this.getTheme());
                marker.setIcon(myIcon);
            }
            marker.setPosition(p);
            marker.setAnchor(
                Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM
            );
        }
        return marker
    }

    fun drawRoute() {
        if (locationActual != null && locationBuscada != null && locationLongPressed!= null) {
            val routePoints = ArrayList<GeoPoint>()
            routePoints.add(locationActual!!)
            routePoints.add(locationBuscada!!)
            routePoints.add(locationLongPressed!!)

            val road = roadManager.getRoad(routePoints)

            if (roadOverlay != null) {
                map.overlays.remove(roadOverlay)
            }

            roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay!!.getOutlinePaint().setColor(Color.BLUE)
            roadOverlay!!.getOutlinePaint().setStrokeWidth(10F)
            map.overlays.add(roadOverlay)

            map.invalidate()
        } else {
            Toast.makeText(this, "Faltan ubicaciones para crear la ruta", Toast.LENGTH_SHORT).show()
        }
    }

    fun locationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            startLocationUpdates()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val isr: IntentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettings.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "El dispositivo no tiene GPS", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun createLocationRequest(): LocationRequest {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
        return request
    }

    private fun createLocationCallback(): LocationCallback {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val loc = result.lastLocation
                if (loc != null) {
                    updateUI(loc)
                }
            }
        }
        return callback
    }

    fun updateUI(location: Location) {
        val newLocation = GeoPoint(location.latitude, location.longitude)
        val address = findAddress(LatLng(location.latitude, location.longitude))

        var moverCamara = false
        var guardarJSON = false

        if (locationActual == null) {
            moverCamara = true
            guardarJSON = true
        } else {
            val distancia = distance(
                locationActual!!.latitude,
                locationActual!!.longitude,
                newLocation.latitude,
                newLocation.longitude
            )

            if (distancia > 30.0) {
                moverCamara = true
                guardarJSON = true
            }
        }
        if (guardarJSON) {
            locationActual = newLocation
            writeJSONObject()
            drawRouteTresPuntos()
        }

        markerLocationActual?.let {
            map.overlays.remove(it)
        }

        val newMarker = createMarker(
            newLocation,
            "Tu ubicación",
            address ?: "Estás aquí",
            R.drawable.baseline_location_alt_24
        )

        map.overlays.add(newMarker)
        markerLocationActual = newMarker

        if (moverCamara) {
            map.controller.animateTo(newLocation)
        }
    }

    fun writeJSONObject() {
        locationActual?.let {
            val myLocation = MyLocation(
                Date(System.currentTimeMillis()),
                it.latitude,
                it.longitude
            )

            locations.put(myLocation.toJSON())
            val filename = "locations.json"
            val file = File(baseContext.getExternalFilesDir(null), filename)
            val output = BufferedWriter(FileWriter(file))
            output.write(locations.toString())
            output.close()

            Log.i("LOCATION", "Ubicación guardada: $myLocation")
        } ?: Log.e("LOCATION", "No se pudo guardar: currentLocation es null")
    }

    fun distance(lat1 : Double, long1: Double, lat2:Double, long2:Double) : Double{
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)+
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        val result = RADIUS_EARTH_METERS * c;
        return Math.round(result*100.0)/100.0;
    }

    fun drawRouteTresPuntos() {
        if (locationActual != null && locationBuscada != null && locationLongPressed != null) {
            val routePoints = arrayListOf(
                locationActual!!,
                locationBuscada!!,
                locationLongPressed!!
            )

            val road = roadManager.getRoad(routePoints)

            if (roadOverlay != null) {
                map.overlays.remove(roadOverlay)
            }

            roadOverlay = RoadManager.buildRoadOverlay(road).apply {
                outlinePaint.color = Color.BLUE
                outlinePaint.strokeWidth = 10f
            }

            map.overlays.add(roadOverlay)
            map.invalidate()

            Log.i("Ruta", "Ruta generada automáticamente entre los tres puntos.")
        }
    }

    fun drawRoutesDeJson() {
        val filename = "locations.json"
        val file = File(baseContext.getExternalFilesDir(null), filename)
        if (!file.exists()) {
            Toast.makeText(baseContext, "No hay ubicaciones guardadas", Toast.LENGTH_SHORT).show()
            return
        }
        val json = file.readText()
        val jsonArray = JSONArray(json)
        if (jsonArray.length() < 2) {
            Toast.makeText(baseContext, "Se necesitan al menos 2 ubicaciones para una ruta", Toast.LENGTH_SHORT).show()
            return
        }
        val geoPoints = ArrayList<GeoPoint>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val lat = obj.getDouble("latitude")
            val lon = obj.getDouble("longitude")
            geoPoints.add(GeoPoint(lat, lon))
        }
        val road = roadManager.getRoad(geoPoints)
        if (roadOverlay != null) {
            map.overlays.remove(roadOverlay)
        }
        roadOverlay = RoadManager.buildRoadOverlay(road).apply {
            outlinePaint.color = Color.MAGENTA
            outlinePaint.strokeWidth = 8f
        }
        map.overlays.add(roadOverlay)
        map.invalidate()
    }

}