package com.example.taller2computacionmovil

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller2computacionmovil.databinding.ActivityGaleriaBinding
import java.io.File

class GaleriaActivity : AppCompatActivity() {
    val getContentGallery = registerForActivityResult(ActivityResultContracts.GetContent(),
        ActivityResultCallback {
            loadImage(it!!)
        })
    val getContentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),ActivityResultCallback {
            if(it){
                loadImage(uriCamera)
            }
        })
    lateinit var uriCamera : Uri

    private lateinit var binding: ActivityGaleriaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGaleriaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val file = File(filesDir, "picFromCamera")

        binding.gallery.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        binding.camara.setOnClickListener {
            uriCamera = FileProvider.getUriForFile(
                baseContext,
                baseContext.packageName + ".fileprovider", file
            )
            getContentCamera.launch(uriCamera);
        }

    }

    fun loadImage(uri: Uri) {
        val imageStream = getContentResolver().openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.imagen.setImageBitmap(bitmap)
    }
}