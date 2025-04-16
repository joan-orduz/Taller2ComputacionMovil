package com.example.sesion5ejerciciopermisos.adapters

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import com.example.taller2computacionmovil.databinding.ContactrowBinding

class ContactsAdapter (context: Context?, c: Cursor?, flags: Int) :
    CursorAdapter(context, c, flags){
    private lateinit var binding : ContactrowBinding
    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        binding = ContactrowBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        binding.idContact.text = cursor?.getInt(0).toString()
        binding.nameContact.text =  cursor?.getString(1)
    }
}
