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

import org.junit.Test
import org.junit.Assert.*



class CalculationsUnitTest {
	private val recAngleCardioidDataPoints = listOf(
		//		RA		MD		MA
		arrayOf(180.0,	0.0,	88.0),
		arrayOf(160.0,	5.0,	87.0),
		arrayOf(140.0,	10.0,	85.0),
		arrayOf(120.0,	15.0,	86.0),
		arrayOf(100.0,	20.0,	92.0),
		arrayOf(80.0,	25.0,	109.0),
		arrayOf(60.0,	30.0,	148.0),
		arrayOf(60.0,	50.0,	72.0),
		arrayOf(80.0,	45.0,	33.0),
		arrayOf(100.0,	40.0,	19.0),
		arrayOf(120.0,	35.0,	17.0),
		arrayOf(140.0,	30.0,	19.0),
		arrayOf(160.0,	25.0,	25.0),
		arrayOf(180.0,	20.0,	33.0)
	)
	
	@Test
	fun testCardioidMicAngleCalculations() {
		recAngleCardioidDataPoints.forEach { (recAngle, micDistance, micAngle) ->
			assertEquals(micAngle, calculateCardioidMicAngle(recAngle, micDistance), 1.5)
		}
	}
	
	@Test
	fun testCardioidMicDistanceCalculations() {
		recAngleCardioidDataPoints.forEach { (recAngle, micDistance, micAngle) ->
			assertEquals(micDistance, calculateCardioidMicDistance(recAngle, micAngle), 0.5)
		}
	}
	
	@Test
	fun testCardioidRecordingAngleCalculation() {
		recAngleCardioidDataPoints.forEach { (recAngle, micDistance, micAngle) ->
			assertEquals(recAngle, calculateCardioidRecordingAngle(micDistance, micAngle), 6.0)
		}
	}
	
	
	
	private val recAngleOmniDataPoints = listOf(
		//		RA		MD
		arrayOf(100.0,	49.5),
		arrayOf(120.0,	43.0),
		arrayOf(140.0,	40.0),
		arrayOf(160.0,	38.0),
		arrayOf(180.0,	37.5)
	)
	
	@Test
	fun testOmniMicDistanceCalculations() {
		recAngleOmniDataPoints.forEach { (recAngle, micDistance) ->
			assertEquals(micDistance, calculateOmniMicDistance(recAngle), 1.0)
		}
	}
	
	@Test
	fun testOmniRecordingAngleCalculations() {
		fun delta(micDistance: Double): Double {
			val deltaX1	= 37.0
			val deltaY1	= 15.0
			val deltaX2	= 50.0
			val deltaY2	= 2.0
			val slope	= (deltaY2 - deltaY1) / (deltaX2 - deltaX1)
			return deltaY1 + (micDistance - deltaX1) * slope
		}
		recAngleOmniDataPoints.forEach { (recAngle, micDistance) ->
			assertEquals(recAngle, calculateOmniRecordingAngle(micDistance), delta(micDistance))
		}
	}
	
	
	
	private val angularDistortionTestDataPoints = listOf(
		//		MD		MA		AD
		arrayOf(1.0,	163.0,	7.0),
		arrayOf(12.0,	158.0,	6.0),
		arrayOf(24.0,	167.0,	5.0),
		arrayOf(33.0,	137.0,	4.0),
		arrayOf(49.0,	74.0,	4.0),
		arrayOf(47.0,	27.0,	6.0),
		arrayOf(48.0,	4.0,	8.0),
		arrayOf(38.0,	1.0,	9.0),
		arrayOf(34.0,	5.0,	9.0),
		arrayOf(28.0,	13.0,	8.0),
		arrayOf(24.0,	22.0,	7.0),
		arrayOf(20.5,	31.0,	6.0),
		arrayOf(14.5,	46.0,	5.0),
		arrayOf(6.5,	69.0,	5.0),
		arrayOf(1.5,	98.0,	6.0),
		
		arrayOf(18.0,	135.0,	5.0),
		arrayOf(26.0,	105.0,	4.0),
		arrayOf(36.0,	64.0,	4.0),
		arrayOf(42.5,	41.0,	5.0),
		arrayOf(37.5,	26.0,	6.0),
		arrayOf(32.0,	24.0,	6.0),
		arrayOf(27.0,	27.0,	6.0),
		arrayOf(23.0,	38.0,	5.0),
		arrayOf(11.5,	81.0,	5.0),
		arrayOf(13.0,	93.0,	5.0),
		arrayOf(15.0,	111.0,	5.0),
	)
	
	@Test
	fun testAngularDistortionCalculation() {
		angularDistortionTestDataPoints.forEach { (micDistance, micAngle, angularDistortion) ->
			assertEquals(angularDistortion, calculateAngularDistortion(micDistance, micAngle), 0.5)
		}
	}
	
	
	
	private val sidesReverbLimitDataPoints = listOf(
		//		MD		MA
		arrayOf(0.0,	76.0),
		arrayOf(5.0,	68.0),
		arrayOf(10.0,	59.0),
		arrayOf(15.0,	50.0),
		arrayOf(20.0,	39.0),
		arrayOf(25.0,	28.0),
		arrayOf(30.0,	19.0),
		arrayOf(35.0,	12.0),
		arrayOf(40.0,	5.0)
	)
	
	@Test
	fun testSidesReverbLimitCalculation() {
		sidesReverbLimitDataPoints.forEach { (micDistance, micAngle) ->
			assertEquals(micAngle, calculateSidesReverbLimit(micDistance), 1.5)
		}
	}
}