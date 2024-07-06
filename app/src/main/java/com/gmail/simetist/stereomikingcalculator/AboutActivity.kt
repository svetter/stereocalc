package com.gmail.simetist.stereomikingcalculator

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat



class AboutActivity : AppCompatActivity() {
	private lateinit var backButton:	Button
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_about)
		
		
		backButton = findViewById(R.id.backButton)
		
		
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		
		
		
		backButton.setOnClickListener {
			onBackPressedDispatcher.onBackPressed()
		}
	}
}