package com.example.taller2computacionmovil

import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sesion5ejerciciopermisos.adapters.ContactsAdapter
import com.example.taller2computacionmovil.databinding.ActivityContactosBinding

class ContactosActivity : AppCompatActivity() {
    val getContactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            updateUI(it)
        }
    )

    val projection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
    lateinit var adapter : ContactsAdapter

    private lateinit var binding: ActivityContactosBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ContactsAdapter(this, null, 0)
        binding.listaContactos.adapter = adapter

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_DENIED){
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)){
                Toast.makeText(this, "El permiso es requerido para mostrar los contactos", Toast.LENGTH_SHORT).show()
            }
            getContactsPermission.launch(android.Manifest.permission.READ_CONTACTS)
        } else {
            updateUI(true)
        }
    }

    private fun updateUI(permission : Boolean){
        if(permission){
            val cursor =
                contentResolver.query(ContactsContract.Contacts.
                CONTENT_URI,
                    projection, null, null, null)

            adapter.changeCursor(cursor)

        }
    }
}