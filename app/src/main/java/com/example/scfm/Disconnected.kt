package com.example.scfm

import SCFM.R
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class Disconnected : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disconnect)
    }

    fun sair(view: View) {
        finish()
    }
}
