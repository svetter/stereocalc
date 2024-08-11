/*
 * Copyright 2024 Simon Vetter
 * 
 * This file is part of Stereophonic Calculator.
 * 
 * Stereophonic Calculator is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 * 
 * Stereophonic Calculator is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Stereophonic
 * Calculator. If not, see <https://www.gnu.org/licenses/>.
 */

package com.gmail.simetist.stereophoniccalculator

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity



class AboutActivity : AppCompatActivity() {
	private lateinit var versionLabel:		TextView
	private lateinit var licensesButton:	Button
	private lateinit var backButton:		Button
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_about)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { view, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		
		versionLabel	= findViewById(R.id.versionLabel)
		licensesButton	= findViewById(R.id.licensesButton)
		backButton		= findViewById(R.id.backButton)
		
		try {
			val pInfo: PackageInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
			val version = pInfo.versionName
			versionLabel.text = "Version $version"
		} catch (e: PackageManager.NameNotFoundException) {
			e.printStackTrace()
		}
		
		licensesButton.setOnClickListener {
			startActivity(Intent(this, OssLicensesMenuActivity::class.java))
		}
		
		backButton.setOnClickListener {
			finish()
		}
	}
}