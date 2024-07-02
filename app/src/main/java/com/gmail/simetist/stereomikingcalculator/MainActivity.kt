package com.gmail.simetist.stereomikingcalculator

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat



private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
	val recAngleLowerBound = 40
	val recAngleUpperBound = 180
	val recAngleDefault = 90
	
	private lateinit var unitsSwitch:		Switch
	private lateinit var halfAnglesSwitch:	Switch
	private lateinit var micCharSwitch:		Switch
	private lateinit var recAngleEdit:		EditText
	private lateinit var recAngleSlider:	SeekBar
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_main)
		
		unitsSwitch			= findViewById(R.id.unitsSwitch)
		halfAnglesSwitch	= findViewById(R.id.halfAnglesSwitch)
		micCharSwitch		= findViewById(R.id.micTypeSwitch)
		recAngleEdit		= findViewById(R.id.recAngleEdit)
		recAngleSlider		= findViewById(R.id.recAngleSlider)
		
		// Set default recAngle values
		recAngleSlider.max = recAngleUpperBound - recAngleLowerBound
		recAngleSlider.progress = recAngleDefault - recAngleLowerBound
		recAngleEdit.setText(recAngleDefault.toString())
		
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		
		// Set up listeners
		
		unitsSwitch.setOnCheckedChangeListener { _, isChecked ->
			if (isChecked) {
				Log.i(TAG, "Switched to imperial")
			} else {
				Log.i(TAG, "Switched to metric")
			}
		}
		
		halfAnglesSwitch.setOnCheckedChangeListener { _, isChecked ->
			if (isChecked) {
				Log.i(TAG, "Switched to half angles")
			} else {
				Log.i(TAG, "Switched to full angles")
			}
		}
		
		micCharSwitch.setOnCheckedChangeListener { _, isChecked ->
			if (isChecked) {
				Log.i(TAG, "Switched to omni")
			} else {
				Log.i(TAG, "Switched to cardioid")
			}
		}
		
		recAngleEdit.setOnKeyListener(object: View.OnKeyListener {
			override fun onKey(p0: View?, p1: Int, p2: KeyEvent?): Boolean {
				if (p2?.action != KeyEvent.ACTION_UP || p2.keyCode != KeyEvent.KEYCODE_ENTER) {
					return false
				}
				
				var currentValue = recAngleEdit.text.toString().toIntOrNull() ?: return false
				
				if (currentValue < recAngleLowerBound || currentValue > recAngleUpperBound) {
					currentValue = currentValue.coerceIn(recAngleLowerBound, recAngleUpperBound)
					recAngleEdit.setText(currentValue.toString())
				}
				
				recAngleSlider.progress = currentValue - recAngleLowerBound
				return false
			}
		})
		
		recAngleSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				val currentValue = recAngleSlider.progress + recAngleLowerBound
				recAngleEdit.setText(currentValue.toString())
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
	}
}