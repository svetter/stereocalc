import os
import numpy as np
from scipy.optimize import curve_fit
import matplotlib.pyplot as plt

from raw_data import data, data_upper_reverb_border, data_lower_reverb_border



# Figure 9 from "The Stereophonic Zoom"
# https://www.gracedesign.com/support/StereoZoom10.pdf
# Page 10, Figure 9, cropped to the outside border of the graph area
original_data_img_path = '../../Data source/sra_cardioid_ext_cropped.png'



def sigmoid(x, out_scale, out_min, growth_rate, in_midpoint):
	return out_scale / (1 + np.exp(-growth_rate * (x - in_midpoint))) + out_min

p0 = [-250, 200, 0.07, 25]
fit_param_names = ['out_scale', 'out_min', 'growth_rate', 'in_midpoint']



fit_params = {}

for hra in range(30, 100, 10):
	data_select = data[data[:, 0] == hra][:, [2, 1]]
	
	# Use curve_fit to find the optimal parameters
	popt = curve_fit(sigmoid, data_select[:, 0], data_select[:, 1], p0=p0)[0]
	fit_params[hra] = popt
	
	#print("Optimal parameters for ±" + str(hra) + "°:")
	#print(f"\tout_scale: {popt[0]}, out_min: {popt[1]}, growth_rate: {popt[2]}, in_midpoint: {popt[3]}")

lower_reverb_border_fit_params = curve_fit(sigmoid, data_lower_reverb_border[:, 1], data_lower_reverb_border[:, 0], p0=p0)[0]



fig_data, ax_data = plt.subplots()
fig_error, ax_error = plt.subplots()
fig_fit, ax_fit = plt.subplots()
fig_reconstruct, ax_reconstruct = plt.subplots()



curves_x_steps = np.linspace(0, 50, 1000)

total_abs_error = 0

for hra in range(30, 100, 10):
	# Plot original data and fitted curve
	data_select = data[data[:, 0] == hra][:, [2, 1]]
	params = fit_params[hra]
	ax_data.scatter(data_select[:, 0], data_select[:, 1], label='± ' + str(hra) + '°')
	ax_data.plot(curves_x_steps, sigmoid(curves_x_steps, params[0], params[1], params[2], params[3]))
	
	# Plot errors
	error_data = data_select[:, 1] - sigmoid(data_select[:, 0], params[0], params[1], params[2], params[3])
	total_abs_error += np.sum(np.abs(error_data))
	ax_error.plot(data_select[:, 0], error_data, marker='o', label='± ' + str(hra) + '°')

ax_data.scatter(data_lower_reverb_border[:, 1], data_lower_reverb_border[:, 0], label='Lower reverb border', color='gray')
ax_data.plot(curves_x_steps, sigmoid(curves_x_steps, *lower_reverb_border_fit_params), color='gray')

if os.path.isfile(original_data_img_path):
	original_data_img = plt.imread(original_data_img_path)
	ax_data.imshow(original_data_img, extent=[0, 50, 0, 180])
	ax_data.set_aspect('auto')

ax_data.set_title("Original data points and fitted curves".format(total_abs_error))
ax_data.set_xlabel("Microphone distance [cm]")
ax_data.set_ylabel("Microphone angle [°]")
ax_data.legend()
ax_data.legend(loc='lower left')
ax_data.set_xlim(0, 50)
ax_data.set_ylim(0, 180)
fig_data.set_size_inches(15, 10)
fig_data.subplots_adjust(left=0.05, right=0.98, top=0.97, bottom=0.05)

ax_error.set_title("Total absolute error: {:.3f}".format(total_abs_error))
ax_error.set_xlabel("Microphone distance [cm]")
ax_error.set_ylabel("Error [°]")
ax_error.legend()
ax_error.set_xlim(0, 50)
ax_error.set_ylim(-2, 2)
fig_error.set_size_inches(8, 4)
fig_error.subplots_adjust(left=0.1, right=0.98, top=0.92, bottom=0.14)



# Approximate fitting parameter curves

def poly_function(x, coeff0, coeff1, coeff2, coeff3, coeff4, coeff5):
	if np.isscalar(x):
		x = np.array(x)
	return coeff0 + coeff1 * x + coeff2 * np.pow(x, 2) + coeff3 * np.pow(x, 3) + coeff4 * np.pow(x, 4) + coeff5 * np.pow(x, 5)

average_params = np.mean(list(fit_params.values()), axis=0)

meta_fit_params = []
fit_params_x_steps = np.linspace(0, 90, 1000)

for i in range(4):
	rel_param_values = [params[i] for params in fit_params.values()] / average_params[i]
	ax_fit.scatter(range(30, 100, 10), rel_param_values, label=fit_param_names[i])
	
	# Use curve_fit to find the optimal parameters
	popt = curve_fit(poly_function, range(30, 100, 10), rel_param_values)[0]
	meta_fit_params.append(popt)
	ax_fit.plot(fit_params_x_steps, poly_function(fit_params_x_steps, *popt))

ax_fit.set_title("Fitting parameter values relative to respective average")
ax_fit.set_xlabel("Half recording angle [°]")
ax_fit.set_ylabel("Relative parameter value")
ax_fit.legend()
ax_fit.set_xlim(18, 92)
ax_fit.set_ylim(0, 3)
fig_fit.set_size_inches(6, 4)
fig_fit.subplots_adjust(left=0.12, right=0.98, top=0.92, bottom=0.14)



# Attempt to reconstruct and interpolate the original data

def interpolate_fit_params(hra):
	params = []
	for i in range(4):
		params.append(poly_function(hra, *meta_fit_params[i]) * average_params[i])
	return params

def interpolate_data(hra, md):
	fit_params = interpolate_fit_params(hra)
	return sigmoid(md, fit_params[0], fit_params[1], fit_params[2], fit_params[3])

for hra in range(20, 95, 5):
	curves_x_steps = np.linspace(-100, 300, 1000)
	# Reconstruct the original data
	interpolated_data = interpolate_data(hra, curves_x_steps)
	
	# Plot the interpolated data
	ax_reconstruct.plot(curves_x_steps, interpolated_data, label='± ' + str(hra) + '°')

if os.path.isfile(original_data_img_path):
	ax_reconstruct.imshow(original_data_img, extent=[0, 50, 0, 180])
	ax_reconstruct.set_aspect('auto')

ax_reconstruct.set_title("Reconstructed and interpolated data".format(total_abs_error))
ax_reconstruct.set_xlabel("Microphone distance [cm]")
ax_reconstruct.set_ylabel("Microphone angle [°]")
ax_reconstruct.legend()
ax_reconstruct.legend(loc='lower left')
ax_reconstruct.set_xlim(0, 50)
ax_reconstruct.set_ylim(0, 180)
fig_reconstruct.set_size_inches(15, 10)
fig_reconstruct.subplots_adjust(left=0.05, right=0.98, top=0.97, bottom=0.05)



# Print all constants necessary for the app

print("\n\nKotlin code: Second-level approximation parameters")
print("=====================================")
print("Fifth-order polynomial coefficients (from x^0 to x^5) for each of the four first-level approximation parameters\n")
for i in range(4):
	camelCaseParamName = fit_param_names[i].replace('_', ' ').title().replace(' ', '')
	camelCaseParamName = camelCaseParamName[0].lower() + camelCaseParamName[1:]
	print("val " + camelCaseParamName + "PolyCoefficients = doubleArrayOf(\n\t" + ',\n\t'.join(str(x) for x in meta_fit_params[i]) + "\n)")
print("\nReverberation limits:")
print("val centerReverbLimitMicAngle\t= {:.1f}".format(data_upper_reverb_border))
for i in range(4):
	camelCaseParamName = fit_param_names[i].replace('_', ' ').title().replace(' ', '')
	print("val sidesReverbLimitFit" + camelCaseParamName + "\t= " + str(lower_reverb_border_fit_params[i]))



# Show the plots

plt.show()
