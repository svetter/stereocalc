package com.gmail.simetist.stereophoniccalculator

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat



class AboutActivity : AppCompatActivity() {
	private lateinit var versionLabel:	TextView
	private lateinit var backButton:	Button
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_about)
		// Show notification bar in the same color as the app's background
		enableEdgeToEdge()
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { view, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		
		versionLabel	= findViewById(R.id.versionLabel)
		backButton		= findViewById(R.id.backButton)
		
		try {
			val pInfo: PackageInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
			val version = pInfo.versionName
			versionLabel.text = "Version $version"
		} catch (e: PackageManager.NameNotFoundException) {
			e.printStackTrace()
		}
		
		backButton.setOnClickListener {
			finish()
		}
	}
}