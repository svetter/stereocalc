# Copyright 2024 Simon Vetter
# 
# This file is part of Stereophonic Calculator.
# 
# Stereophonic Calculator is free software: you can redistribute it and/or modify it under the
# terms of the GNU General Public License as published by the Free Software Foundation, either
# version 3 of the License, or (at your option) any later version.
# 
# Stereophonic Calculator is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
# PURPOSE. See the GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License along with Stereophonic
# Calculator. If not, see <https://www.gnu.org/licenses/>.

import datetime
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
from keras.src.callbacks import EarlyStopping, ModelCheckpoint
from keras.src.layers import InputLayer
from matplotlib.colors import LinearSegmentedColormap
from tensorflow.keras.layers import Dense
from tensorflow.keras.models import Sequential

from raw_data import data_angular_distortion



# Train a neural network with the angular distortion data

# Prepare the data
num_samples = data_angular_distortion.shape[0]
x = data_angular_distortion[:, 0]
y = data_angular_distortion[:, 1]
z = data_angular_distortion[:, 2]

# Normalize the data
x_min = 0
x_max = 50
y_min = 0
y_max = 180
z_min = 0
z_max = 12

x = (x - x_min) / (x_max - x_min)
y = (y - y_min) / (y_max - y_min)
z = (z - z_min) / (z_max - z_min)



if __name__ == '__main__':
	# Shuffle the data
	shuffle_indices = np.random.permutation(num_samples)
	x = x[shuffle_indices]
	y = y[shuffle_indices]
	z = z[shuffle_indices]
	
	# Combine x and y into a single input array
	inputs = np.vstack((x, y)).T
	outputs = z
	
	# Define the model
	model = Sequential()
	model.add(InputLayer(input_shape=(2,)))
	model.add(Dense(10, activation='relu'))
	model.add(Dense(5, activation='relu'))
	model.add(Dense(1, activation='sigmoid'))
	
	# Compile the model
	model.compile(optimizer='adam', loss='mse')
	
	# Train the model
	early_stopping = EarlyStopping(monitor='loss', patience=500, restore_best_weights=True)
	datetime = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
	model_checkpoint = ModelCheckpoint('angular_distortion_nn_' + datetime + '.keras', monitor='loss', save_best_only=True)
	history = model.fit(inputs, outputs, epochs=10000, verbose=1, validation_split=0, batch_size=num_samples, shuffle=True, callbacks=[early_stopping, model_checkpoint])
	
	print("Minimal loss: " + str(min(history.history['loss'])))# + ", minimal validation loss: " + str(min(history.history['val_loss'])))
	
	
	
	fig_train, ax_train = plt.subplots()
	fig_original, ax_original = plt.subplots()
	fig_result1, ax_result1 = plt.subplots()
	fig_result2, ax_result2 = plt.subplots()
	fig_error, ax_error = plt.subplots()
	
	colormap_urgency = LinearSegmentedColormap.from_list('urgency', ['green', 'orange', 'red'])
	mpl.colormaps.register(cmap=colormap_urgency)
	
	
	
	# Plot training history
	ax_train.plot(history.history['loss'], label='loss')
	#ax_train.plot(history.history['val_loss'], label='val_loss')
	#total_loss = np.array(history.history['loss']) * 3 / 4 + np.array(history.history['val_loss']) / 4
	#ax_train.plot(total_loss, label='tot_loss')
	ax_train.set_xlabel('Epoch')
	ax_train.set_ylabel('Mean Squared Error')
	ax_train.legend()


def predict(model, x_vals, y_vals):
	x_vals = np.array(x_vals)
	y_vals = np.array(y_vals)
	
	x_norm = (x_vals - x_min) / (x_max - x_min)
	y_norm = (y_vals - y_min) / (y_max - y_min)
	
	input_data = np.vstack((x_norm, y_norm)).T
	z_norm = model.predict(input_data)
	
	z = z_norm * (z_max - z_min) + z_min
	return z.flatten()
	
	
	
if __name__ == '__main__':
	plot_original = ax_original.scatter(data_angular_distortion[:, 0], data_angular_distortion[:, 1], s=data_angular_distortion[:, 2]**3, c=data_angular_distortion[:, 2], cmap='urgency', vmin=3, vmax=10)
	ax_original.set_title("Original angular distortion data")
	fig_result1.colorbar(plot_original, location='right')
	fig_original.tight_layout()
	
	
	
	result1_z_points = predict(model, data_angular_distortion[:, 0], data_angular_distortion[:, 1])
	result1_z_points_sizes = [value**3 for value in result1_z_points]
	
	plot_result1 = ax_result1.scatter(data_angular_distortion[:, 0], data_angular_distortion[:, 1], s=result1_z_points_sizes, c=result1_z_points, cmap='urgency', vmin=3, vmax=10)
	ax_result1.set_title("Predicted angular distortion")
	fig_result1.colorbar(plot_result1, location='right')
	fig_result1.tight_layout()
	
	
	result2_x_points = []
	result2_y_points = []
	for x in range(0, 52, 1):
		for y in range(0, 190, 2):
			result2_x_points.append(x)
			result2_y_points.append(y)
	result2_z_points = predict(model, result2_x_points, result2_y_points)
	result2_z_points_sizes = [value**3 for value in result2_z_points]
	
	plot_result2 = ax_result2.hexbin(result2_x_points, result2_y_points, C=result2_z_points, gridsize=50, cmap='urgency', vmin=3, vmax=10)
	ax_result2.set_title("Predicted angular distortion")
	fig_result2.colorbar(plot_result2, location='right')
	fig_result2.tight_layout()
	
	
	
	error = np.abs(result1_z_points - data_angular_distortion[:, 2])
	plot_error = ax_error.scatter(data_angular_distortion[:, 0], data_angular_distortion[:, 1], s=error**2*1000, c=error, cmap='urgency')
	ax_error.set_title("Errors in angular distortion prediction")
	fig_error.colorbar(plot_error, location='right')
	fig_error.tight_layout()
	
	
	
	print("Model weights:")
	for layer in model.layers:
		print(layer.get_weights())
	
	
	
	plt.show()