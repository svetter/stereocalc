import numpy as np
from scipy.optimize import curve_fit
import matplotlib.pyplot as plt



approximator_to_use = 'sigmoid'

def sigmoid(x, out_scale, out_min, growth_rate, in_midpoint):
	return out_scale / (1 + np.exp(-growth_rate * (x - in_midpoint))) + out_min

def sinoid(x, out_scale, out_center, rate, in_zeropoint):
	return out_scale * np.sin(rate * x - in_zeropoint) + out_center

if approximator_to_use == 'sigmoid':
	approximator = sigmoid
	p0 = [-250, 200, 0.07, 25]
	fit_param_names = ['out_scale', 'out_min', 'growth_rate', 'in_midpoint']
elif approximator_to_use == 'sinoid':
	approximator = sinoid
	p0 = [50, 80, 0.04, 25]
	fit_param_names = ['out_scale', 'out_center', 'rate', 'in_zeropoint']
else:
	exit("Invalid approximator selected")



# Data points
# Half recording angle, microphone angle, microphone distance
data = np.array([
#	 HRA	MA		MD
	[30,	71.3,	50.0],
	[30,	80,		47.7],
	[30,	90,		45.2],
	[30,	100,	42.6],
	[30,	110,	40.1],
	[30,	120,	37.6],
	[30,	130,	35.0],
	[30,	140,	32.2],
	[30,	150,	29.4],
	[30,	160,	26.4],
	[30,	170,	23.2],
	[30,	180,	19.9],
	
	[40,	20,		49.7],
	[40,	30,		45.9],
	[40,	40,		42.7],
	[40,	50,		39.8],
	[40,	60,		37.1],
	[40,	70,		34.6],
	[40,	80,		32.3],
	[40,	90,		29.8],
	[40,	100,	27.3],
	[40,	110,	24.9],
	[40,	120,	22.4],
	[40,	130,	19.7],
	[40,	140,	17.0],
	[40,	150,	14.0],
	[40,	160,	11.0],
	[40,	170,	7.8],
	[40,	180,	4.6],
	
	[50,	0,		49.8],
	[50,	10,		44.2],
	[50,	20,		39.9],
	[50,	30,		36.2],
	[50,	40,		33.1],
	[50,	50,		30.4],
	[50,	60,		27.8],
	[50,	70,		25.4],
	[50,	80,		22.9],
	[50,	90,		20.5],
	[50,	100,	18.0],
	[50,	110,	15.4],
	[50,	120,	12.8],
	[50,	130,	10.1],
	[50,	140,	7.2],
	[50,	150,	4.3],
	[50,	160,	1.4],
	[50,	164.7,	0.0],
	
	[60,	0,		43.9],
	[60,	10,		38.1],
	[60,	20,		33.8],
	[60,	30,		30.1],
	[60,	40,		27.1],
	[60,	50,		24.3],
	[60,	60,		21.7],
	[60,	70,		19.1],
	[60,	80,		16.6],
	[60,	90,		13.9],
	[60,	100,	11.3],
	[60,	110,	8.6],
	[60,	120,	5.8],
	[60,	130,	3.0],
	[60,	140,	0.3],
	
	[70,	0,		40.4],
	[70,	10,		34.2],
	[70,	20,		29.6],
	[70,	30,		25.9],
	[70,	40,		22.8],
	[70,	50,		20.1],
	[70,	60,		17.2],
	[70,	70,		14.4],
	[70,	80,		11.6],
	[70,	90,		8.8],
	[70,	100,	5.9],
	[70,	110,	3.1],
	[70,	120,	0.3],
	
	[80,	0,		38.7],
	[80,	10,		31.7],
	[80,	20,		26.8],
	[80,	30,		23.0],
	[80,	40,		19.7],
	[80,	50,		16.5],
	[80,	60,		13.5],
	[80,	70,		10.3],
	[80,	80,		7.2],
	[80,	90,		4.1],
	[80,	100,	1.0],
	[80,	103.6,	0.0],
	
	[90,	0,		38.2],
	[90,	10,		30.1],
	[90,	20,		24.9],
	[90,	30,		20.8],
	[90,	40,		17.1],
	[90,	50,		13.5],
	[90,	60,		9.9],
	[90,	70,		6.4],
	[90,	80,		2.9],
	[90,	88.7,	0.0]
])

data_upper_reverb_border = np.array(120)
data_lower_reverb_border = np.array([
	[0,		44.3],
	[10,	35.9],
	[20,	29.4],
	[30,	24.3],
	[40,	19.6],
	[50,	14.9],
	[60,	9.7],
	[70,	4.2],
	[77.3,	0.0]
])



fit_params = {}

for hra in range(30, 100, 10):
	data_select = data[data[:, 0] == hra][:, [2, 1]]
	
	# Use curve_fit to find the optimal parameters
	popt = curve_fit(approximator, data_select[:, 0], data_select[:, 1], p0=p0)[0]
	fit_params[hra] = popt
	
	#print("Optimal parameters for ±" + str(hra) + "°:")
	#print(f"\tout_scale: {popt[0]}, out_min: {popt[1]}, growth_rate: {popt[2]}, in_midpoint: {popt[3]}")

lower_reverb_border_fit_params = curve_fit(approximator, data_lower_reverb_border[:, 1], data_lower_reverb_border[:, 0], p0=p0)[0]



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
	ax_data.plot(curves_x_steps, approximator(curves_x_steps, params[0], params[1], params[2], params[3]))
	
	# Plot errors
	error_data = data_select[:, 1] - approximator(data_select[:, 0], params[0], params[1], params[2], params[3])
	total_abs_error += np.sum(np.abs(error_data))
	ax_error.plot(data_select[:, 0], error_data, marker='o', label='± ' + str(hra) + '°')

ax_data.scatter(data_lower_reverb_border[:, 1], data_lower_reverb_border[:, 0], label='Lower reverb border', color='gray')
ax_data.plot(curves_x_steps, approximator(curves_x_steps, *lower_reverb_border_fit_params), color='gray')

original_data_img = plt.imread('../../Data source/Cropped.png')
ax_data.imshow(original_data_img, extent=[0, 50, 0, 180])
ax_data.set_aspect('auto')

ax_data.set_xlabel("Microphone Distance [cm]")
ax_data.set_ylabel("Microphone Angle [°]")
ax_data.legend()
ax_data.set_xlim(0, 50)
ax_data.set_ylim(0, 180)
fig_data.set_size_inches(15, 10)
fig_data.subplots_adjust(left=0.05, right=0.98, top=0.98, bottom=0.05)

ax_error.set_title("Total absolute error: {:.3f}".format(total_abs_error))
ax_error.set_xlabel("Microphone Distance [cm]")
ax_error.set_ylabel("Error [°]")
ax_error.legend()
ax_error.set_xlim(0, 50)
ax_error.set_ylim(-2, 2)
fig_error.set_size_inches(8, 4)
fig_error.subplots_adjust(left=0.1, right=0.98, top=0.92, bottom=0.14)



# Approximate fitting parameter curves

def poly_function(x, coeff5, coeff4, coeff3, coeff2, coeff1, coeff0):
	if np.isscalar(x):
		x = np.array(x)
	return coeff5 * np.pow(x, 5) + coeff4 * np.pow(x, 4) + coeff3 * np.pow(x, 3) + coeff2 * np.pow(x, 2) + coeff1 * x + coeff0

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
ax_fit.set_xlabel("Half Recording Angle [°]")
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
	return approximator(md, fit_params[0], fit_params[1], fit_params[2], fit_params[3])

for hra in range(20, 95, 5):
	curves_x_steps = np.linspace(-100, 300, 1000)
	# Reconstruct the original data
	interpolated_data = interpolate_data(hra, curves_x_steps)
	
	# Plot the interpolated data
	ax_reconstruct.plot(curves_x_steps, interpolated_data, label='± ' + str(hra) + '°')

ax_reconstruct.imshow(original_data_img, extent=[0, 50, 0, 180])
ax_reconstruct.set_aspect('auto')

ax_reconstruct.set_xlabel("Microphone Distance [cm]")
ax_reconstruct.set_ylabel("Microphone Angle [°]")
ax_reconstruct.legend()
ax_reconstruct.set_xlim(0, 50)
ax_reconstruct.set_ylim(0, 180)
fig_reconstruct.set_size_inches(15, 10)
fig_reconstruct.subplots_adjust(left=0.05, right=0.98, top=0.98, bottom=0.05)



plt.show()
