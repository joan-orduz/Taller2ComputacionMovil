package com.example.taller2computacionmovil

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller2computacionmovil.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.galeria.setOnClickListener {
            startActivity(Intent(baseContext, GaleriaActivity::class.java))
        }

        binding.contactos.setOnClickListener {
            startActivity(Intent(baseContext, ContactosActivity::class.java))
        }

        binding.osMap.setOnClickListener {
            startActivity(Intent(baseContext, OsMapActivity::class.java))
        }
    }
}