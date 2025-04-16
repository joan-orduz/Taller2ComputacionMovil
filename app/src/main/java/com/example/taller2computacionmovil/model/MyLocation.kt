package com.example.taller2computacionmovil.model

import org.json.JSONObject
import java.util.Date

class MyLocation(val date : Date, val latitude: Double,
                 val longitude: Double){
    fun toJSON() : JSONObject {
        val obj = JSONObject();
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        obj.put("date", date.toString())
        return obj
    }
}