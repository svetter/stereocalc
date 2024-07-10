package com.gmail.simetist.stereophoniccalculator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
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
		// Show notification bar in the same color as the app's background
		enableEdgeToEdge()
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		
		populateUIElementMembers()
		
		useImperial		= intent.getBooleanExtra("useImperial",	false)
		useHalfAngles	= intent.getBooleanExtra("useHalfAngles",	false)
		
		mainLayout.post {
			//graphicsViewLayout.layoutParams.height += mainLayout.height - scrollViewLayout.height
			scrollViewLayout.requestLayout()
			mainLayout.requestLayout()
		}
		
		for (lengthUnitLabel in lengthUnitLabels) {
			if (useImperial) {
				lengthUnitLabel.text = "ft"
			} else {
				lengthUnitLabel.text = "m"
			}
		}
		
		initializeListeners()
		
		if (useImperial) {
			micHeightEdit		.setText(feetFormat.format(6.0))
			subjectHeightEdit	.setText(feetFormat.format(3.0))
			subjectWidthEdit	.setText(feetFormat.format(15.0))
			horDistanceEdit		.setText(feetFormat.format(12.0))
			
			micHeightSlider		.max = (15 / sliderPrecision).roundToInt()
			subjectHeightSlider	.max = (15 / sliderPrecision).roundToInt()
			subjectWidthSlider	.max = (60 / sliderPrecision).roundToInt()
			horDistanceSlider	.max = (30 / sliderPrecision).roundToInt()
		} else {
			micHeightEdit		.setText(metersFormat.format(2.0))
			subjectHeightEdit	.setText(metersFormat.format(1.0))
			subjectWidthEdit	.setText(metersFormat.format(5.0))
			horDistanceEdit		.setText(metersFormat.format(4.0))
			
			micHeightSlider		.max = ( 5 / sliderPrecision).roundToInt()
			subjectHeightSlider	.max = ( 5 / sliderPrecision).roundToInt()
			subjectWidthSlider	.max = (20 / sliderPrecision).roundToInt()
			horDistanceSlider	.max = (10 / sliderPrecision).roundToInt()
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
		currentRecAngle			= Math.toDegrees(2 * atan(subjectWidth / (2 * distanceFromMic)))
		val micInclination		= Math.toDegrees(atan(heightDifference / horDistance))
		
		recAngleValueLabel.text = if (useHalfAngles) {
			"± %.1f°".format(currentRecAngle.roundToInt() / 2.0)
		} else {
			"%.0f°".format(currentRecAngle)
		}
		
		if (micInclination.roundToInt() == 0) {
			micInclinationValueLabel.text = "level"
		} else {
			val downUpString = if (micInclination > 0) " down" else " up"
			micInclinationValueLabel.text = "%.0f°".format(abs(micInclination)) + downUpString
		}
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
}