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
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt



class AngleCalcActivity : AppCompatActivity() {
	private lateinit var mainLayout:				ConstraintLayout
	private lateinit var scrollView:				ScrollView
	private lateinit var scrollViewLayout:			ConstraintLayout
	
	private lateinit var micHeightEdit:				EditText
	private lateinit var subjectHeightEdit:			EditText
	private lateinit var subjectWidthEdit:			EditText
	private lateinit var horDistanceEdit:			EditText
	private lateinit var lengthUnitLabels:			Array<TextView>
	
	private lateinit var recAngleValueLabel:		TextView
	private lateinit var micInclinationValueLabel:	TextView
	
	private lateinit var micHeightSlider:			VerticalSeekBar
	private lateinit var subjectHeightSlider:		VerticalSeekBar
	private lateinit var subjectWidthSlider:		SeekBar
	private lateinit var horDistanceSlider: 		SeekBar
	
	private lateinit var graphicsViewLayout:		FrameLayout
	private lateinit var graphicsView:				AngleCalcGraphicsView
	
	private lateinit var backButton:				Button
	private lateinit var applyButton:				Button
	private lateinit var applyBackButton:			Button
	
	
	private lateinit var sharedPreferences:		android.content.SharedPreferences
	private lateinit var prefEditor:			android.content.SharedPreferences.Editor
	
	
	private val sliderPrecision	= 0.10		// Slider steps in m or ft
	
	private val metersFormat	= "%.2f"
	private val feetFormat		= "%.1f"
	
	private var useImperial		= false
	private var useHalfAngles	= false
	
	private var ignoreListeners = false
	private var currentRecAngle = -1.0
	
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_angle_calc)
		populateUIElementMembers()
		var systemBarsHeight = 0
		ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			systemBarsHeight = systemBars.top + systemBars.bottom
			view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		mainLayout.post {
			// Expand the graphics view to fill the remaining space, if any
			if (mainLayout.height > scrollViewLayout.height + systemBarsHeight) {
				graphicsViewLayout.layoutParams.height += mainLayout.height - scrollViewLayout.height - systemBarsHeight
				graphicsViewLayout.requestLayout()
			}
		}
		
		
		sharedPreferences = getSharedPreferences("com.gmail.simetist.stereophoniccalculator", MODE_PRIVATE)
		prefEditor = sharedPreferences.edit()
		
		
		useImperial		= intent.getBooleanExtra("useImperial",	false)
		useHalfAngles	= intent.getBooleanExtra("useHalfAngles",	false)
		
		setupUIForSettings()
		initializeListeners()
		
		// Set defaults
		if (useImperial) {
			micHeightEdit		.setText(feetFormat.format(6.0))
			subjectHeightEdit	.setText(feetFormat.format(3.0))
			subjectWidthEdit	.setText(feetFormat.format(15.0))
			horDistanceEdit		.setText(feetFormat.format(10.0))
		} else {
			micHeightEdit		.setText(metersFormat.format(2.0))
			subjectHeightEdit	.setText(metersFormat.format(1.0))
			subjectWidthEdit	.setText(metersFormat.format(5.0))
			horDistanceEdit		.setText(metersFormat.format(3.0))
		}
		
		if (savedInstanceState != null) {
			restoreStateFromBundle(savedInstanceState)
		} else if (sharedPreferences.contains("micHeightCm")) {
			restoreStateFromSharedPrefs()
		} else {
			calculateAndUpdate()
		}
	}
	
	
	
	private fun calculateAndUpdate() {
		// No conversions necessary, as the calculations are all in the same units
		val micHeight		= micHeightEdit		.text.toString().replace(',', '.').toDoubleOrNull()
		val subjectHeight	= subjectHeightEdit	.text.toString().replace(',', '.').toDoubleOrNull()
		val subjectWidth	= subjectWidthEdit	.text.toString().replace(',', '.').toDoubleOrNull()
		val horDistance		= horDistanceEdit	.text.toString().replace(',', '.').toDoubleOrNull()
		
		if (micHeight == null || subjectHeight == null || subjectWidth == null || horDistance == null) {
			recAngleValueLabel.text = ""
			micInclinationValueLabel.text = ""
			return
		}
		
		val heightDifference	= micHeight - subjectHeight
		val distanceFromMic		= sqrt(heightDifference.pow(2) + horDistance.pow(2))
		currentRecAngle = if (distanceFromMic > 0) {
			Math.toDegrees(2 * atan(subjectWidth / (2 * distanceFromMic)))
		} else 180.0
		val micInclination = Math.toDegrees(atan(heightDifference / horDistance))
		
		recAngleValueLabel.text = angleText(currentRecAngle, useHalfAngles, plusMinusSpace = true)
		
		if (micInclination.isNaN() || micInclination.roundToInt() == 0) {
			micInclinationValueLabel.text = "level"
		} else {
			val downUpString = if (micInclination > 0) " down" else " up"
			micInclinationValueLabel.text = angleText(abs(micInclination), false) + downUpString
		}
		
		val conversionFactor = if (useImperial) cmPerFoot else 100.0
		graphicsView.setParametersInCm(
			micHeight		* conversionFactor,
			subjectHeight	* conversionFactor,
			subjectWidth	* conversionFactor,
			horDistance		* conversionFactor
		)
	}
	
	private fun submitRecAngle() {
		val resultIntent = Intent()
		resultIntent.putExtra("recAngle", currentRecAngle)
		setResult(RESULT_OK, resultIntent)
	}
	
	
	
	private fun populateUIElementMembers() {
		mainLayout					= findViewById(R.id.mainLayout)
		scrollView					= findViewById(R.id.scrollView)
		scrollViewLayout			= findViewById(R.id.scrollViewLayout)
		
		micHeightEdit				= findViewById(R.id.micHeightEdit)
		subjectHeightEdit			= findViewById(R.id.subjectHeightEdit)
		subjectWidthEdit			= findViewById(R.id.subjectWidthEdit)
		horDistanceEdit				= findViewById(R.id.horDistanceEdit)
		lengthUnitLabels = arrayOf(
			findViewById(R.id.micHeightUnitLabel),
			findViewById(R.id.subjectHeightUnitLabel),
			findViewById(R.id.subjectWidthUnitLabel),
			findViewById(R.id.horDistanceUnitLabel)
		)
		
		recAngleValueLabel			= findViewById(R.id.recAngleValueLabel)
		micInclinationValueLabel	= findViewById(R.id.micInclinationValueLabel)
		
		micHeightSlider				= findViewById(R.id.micHeightSlider)
		subjectHeightSlider			= findViewById(R.id.subjectHeightSlider)
		subjectWidthSlider			= findViewById(R.id.subjectWidthSlider)
		horDistanceSlider			= findViewById(R.id.horDistanceSlider)
		
		graphicsViewLayout			= findViewById(R.id.graphicsViewLayout)
		graphicsView				= findViewById(R.id.graphicsView)
		
		backButton					= findViewById(R.id.backButton)
		applyButton					= findViewById(R.id.applyButton)
		applyBackButton				= findViewById(R.id.applyBackButton)
	}
	
	private fun setupUIForSettings() {
		for (lengthUnitLabel in lengthUnitLabels) {
			if (useImperial) {
				lengthUnitLabel.text = "ft"
			} else {
				lengthUnitLabel.text = "m"
			}
		}
		
		if (useImperial) {
			micHeightSlider		.max = (15 / sliderPrecision).roundToInt()
			subjectHeightSlider	.max = (15 / sliderPrecision).roundToInt()
			subjectWidthSlider	.max = (60 / sliderPrecision).roundToInt()
			horDistanceSlider	.max = (30 / sliderPrecision).roundToInt()
		} else {
			micHeightSlider		.max = ( 5 / sliderPrecision).roundToInt()
			subjectHeightSlider	.max = ( 5 / sliderPrecision).roundToInt()
			subjectWidthSlider	.max = (20 / sliderPrecision).roundToInt()
			horDistanceSlider	.max = (10 / sliderPrecision).roundToInt()
		}
	}
	
	private fun initializeListeners() {
		micHeightEdit.addTextChangedListener {
			handleLengthEditChange(micHeightEdit, micHeightSlider)
		}
		subjectHeightEdit.addTextChangedListener {
			handleLengthEditChange(subjectHeightEdit, subjectHeightSlider)
		}
		subjectWidthEdit.addTextChangedListener {
			handleLengthEditChange(subjectWidthEdit, subjectWidthSlider)
		}
		horDistanceEdit.addTextChangedListener {
			handleLengthEditChange(horDistanceEdit, horDistanceSlider)
		}
		
		micHeightSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				handleLengthSliderChange(micHeightSlider, micHeightEdit)
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
		subjectHeightSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				handleLengthSliderChange(subjectHeightSlider, subjectHeightEdit)
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
		subjectWidthSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				handleLengthSliderChange(subjectWidthSlider, subjectWidthEdit)
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
		horDistanceSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				handleLengthSliderChange(horDistanceSlider, horDistanceEdit)
			}
			override fun onStartTrackingTouch(p0: SeekBar?) {}
			override fun onStopTrackingTouch(p0: SeekBar?) {}
		})
		
		backButton.setOnClickListener {
			finish()
		}
		applyButton.setOnClickListener {
			submitRecAngle()
		}
		applyBackButton.setOnClickListener {
			submitRecAngle()
			finish()
		}
	}
	
	private fun handleLengthEditChange(edit: EditText, slider: SeekBar) {
		if (ignoreListeners) return
		ignoreListeners = true
		
		val value = edit.text.toString().replace(',', '.').toDoubleOrNull()
		if (value != null) {
			slider.progress = (value / sliderPrecision).roundToInt()
		}
		
		ignoreListeners = false
		calculateAndUpdate()
	}
	
	private fun handleLengthSliderChange(slider: SeekBar, edit: EditText) {
		if (ignoreListeners) return
		ignoreListeners = true
		
		val formatString = if (useImperial) feetFormat else metersFormat
		edit.setText(formatString.format(slider.progress * sliderPrecision))
		
		ignoreListeners = false
		calculateAndUpdate()
	}
	
	
	
	// SAVE / RESTORE STATE
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		
		outState.putDouble("micHeight",		micHeightEdit		.text.toString().replace(',', '.').toDoubleOrNull() ?: Double.NaN)
		outState.putDouble("subjectHeight",	subjectHeightEdit	.text.toString().replace(',', '.').toDoubleOrNull() ?: Double.NaN)
		outState.putDouble("subjectWidth",	subjectWidthEdit		.text.toString().replace(',', '.').toDoubleOrNull() ?: Double.NaN)
		outState.putDouble("horDistance",	horDistanceEdit		.text.toString().replace(',', '.').toDoubleOrNull() ?: Double.NaN)
	}
	
	private fun restoreStateFromBundle(savedInstanceState: Bundle) {
		val formatString = if (useImperial) feetFormat else metersFormat
		
		arrayOf(
			"micHeight"		to micHeightEdit,
			"subjectHeight"	to subjectHeightEdit,
			"subjectWidth"	to subjectWidthEdit,
			"horDistance"	to horDistanceEdit
		).forEach { (key, edit) ->
			val value = savedInstanceState.getDouble(key)
			if (!value.isNaN()) {
				edit.setText(formatString.format(value))
			}
		}
		
		calculateAndUpdate()
	}
	
	private fun saveStateToSharedPrefs() {
		val conversionFactor = if (useImperial) cmPerFoot else 100.0
		arrayOf(
			"micHeightCm"		to micHeightEdit		.text.toString().replace(',', '.').toDoubleOrNull(),
			"subjectHeightCm"	to subjectHeightEdit	.text.toString().replace(',', '.').toDoubleOrNull(),
			"subjectWidthCm"	to subjectWidthEdit		.text.toString().replace(',', '.').toDoubleOrNull(),
			"horDistanceCm"		to horDistanceEdit		.text.toString().replace(',', '.').toDoubleOrNull()
		).forEach { (key, value) ->
			if (value != null) {
				prefEditor.putFloat(key, (value * conversionFactor).toFloat())
			} else {
				prefEditor.remove(key)
			}
		}
		
		prefEditor.apply()
	}
	
	private fun restoreStateFromSharedPrefs() {
		val conversionFactor = if (useImperial) cmPerFoot else 100.0
		val formatString = if (useImperial) feetFormat else metersFormat
		
		arrayOf(
			"micHeightCm"		to micHeightEdit,
			"subjectHeightCm"	to subjectHeightEdit,
			"subjectWidthCm"	to subjectWidthEdit,
			"horDistanceCm"		to horDistanceEdit
		).forEach { (key, edit) ->
			val value = sharedPreferences.getFloat(key, Float.NaN).toDouble()
			if (!value.isNaN()) {
				edit.setText(formatString.format(value / conversionFactor))
			}
		}
		
		calculateAndUpdate()
	}
	
	override fun onDestroy() {
		saveStateToSharedPrefs()
		super.onDestroy()
	}
}