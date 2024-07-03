import tensorflow as tf
import matplotlib as mpl
from matplotlib import pyplot as plt
from matplotlib.colors import LinearSegmentedColormap
from tensorflow.keras.models import load_model

from angular_dist_nn import predict, x_min, x_max, y_min, y_max, z_min, z_max



# Load the model
model = load_model('angular_distortion_nns/10x5x1_loss_0.00007.keras')



# Test the model
x_points = []
y_points = []
for x in range(0, 52, 1):
	for y in range(0, 190, 2):
		x_points.append(x)
		y_points.append(y)
z_points = predict(model, x_points, y_points)
z_points_sizes = [value**3 for value in z_points]

colormap_urgency = LinearSegmentedColormap.from_list('urgency', ['green', 'orange', 'red'])
mpl.colormaps.register(cmap=colormap_urgency)

fig_result, ax_result = plt.subplots()

plot_result = plt.hexbin(x_points, y_points, C=z_points, gridsize=50, cmap='urgency', vmin=3, vmax=10)
ax_result.set_title("Predicted angular distortion")
fig_result.colorbar(plot_result, location='right')
fig_result.tight_layout()

plt.show()



def convert_name(name):
	# Convert a Python snake_case name to a camelCase name
	words = name.split('_')
	return words[0] + ''.join(word.capitalize() for word in words[1:])
	

# Function to convert a Keras model to Kotlin code
def keras_to_kotlin(model):
	kotlin_code = ""
	
	# Get model configuration
	config = model.get_config()
	
	# Start with Kotlin imports and class definition
	kotlin_code += "import kotlin.math.*\n\n"
	kotlin_code += "object AngularDistortionNN {\n"
	
	# Function to perform the dot product and add bias
	kotlin_code += "\tprivate fun denseLayer(inputs: DoubleArray, weights: Array<DoubleArray>, bias: DoubleArray): DoubleArray {\n"
	kotlin_code += "\t\tval output = DoubleArray(bias.size)\n"
	kotlin_code += "\t\tfor (i in bias.indices) {\n"
	kotlin_code += "\t\t\toutput[i] = bias[i]\n"
	kotlin_code += "\t\t\tfor (j in inputs.indices) {\n"
	kotlin_code += "\t\t\t\toutput[i] += inputs[j] * weights[j][i]\n"
	kotlin_code += "\t\t\t}\n"
	kotlin_code += "\t\t}\n"
	kotlin_code += "\t\treturn output\n"
	kotlin_code += "\t}\n\n"
	
	# Function to apply activation
	kotlin_code += "\tprivate fun applyActivation(inputs: DoubleArray, activation: String): DoubleArray {\n"
	kotlin_code += "\t\tval output = DoubleArray(inputs.size)\n"
	kotlin_code += "\t\tfor (i in inputs.indices) {\n"
	kotlin_code += "\t\t\toutput[i] = when (activation) {\n"
	kotlin_code += "\t\t\t\t\"relu\" -> max(0.0, inputs[i])\n"
	kotlin_code += "\t\t\t\t\"sigmoid\" -> (1.0 / (1.0 + exp(-inputs[i])))\n"
	kotlin_code += "\t\t\t\telse -> inputs[i]\n"
	kotlin_code += "\t\t\t}\n"
	kotlin_code += "\t\t}\n"
	kotlin_code += "\t\treturn output\n"
	kotlin_code += "\t}\n\n"
	
	# Define weights and biases for each layer
	for layer in model.layers:
		if isinstance(layer, tf.keras.layers.Dense):
			weights, biases = layer.get_weights()
			layer_name = convert_name(layer.name)
			
			kotlin_code += f"\tprivate val {layer_name}Weights = arrayOf(\n"
			for weight_row in weights:
				kotlin_code += "\t\tdoubleArrayOf(" + ", ".join([f"{w}" for w in weight_row]) + "),\n"
			kotlin_code += "\t)\n"
			
			kotlin_code += f"\tprivate val {layer_name}Biases = doubleArrayOf(" + ", ".join(
				[f"{b}" for b in biases]) + ")\n\n"
	
	# Preprocessing
	kotlin_code += "private const val X_MIN = " + str(x_min) + "\n"
	kotlin_code += "private const val X_MAX = " + str(x_max) + "\n"
	kotlin_code += "private const val Y_MIN = " + str(y_min) + "\n"
	kotlin_code += "private const val Y_MAX = " + str(y_max) + "\n"
	kotlin_code += "private const val Z_MIN = " + str(z_min) + "\n"
	kotlin_code += "private const val Z_MAX = " + str(z_max) + "\n\n"
	
	# Define the inference function
	kotlin_code += "\tfun predict(x: Double, y: Double): Double {\n"
	kotlin_code += "val xNormalized = (x - X_MIN) / (X_MAX - X_MIN)\n"
	kotlin_code += "val yNormalized = (y - Y_MIN) / (Y_MAX - Y_MIN)\n"
	kotlin_code += "val inputs = doubleArrayOf(xNormalized, yNormalized)\n"
	inputs = "inputs"
	for layer in model.layers:
		assert isinstance(layer, tf.keras.layers.Dense)
		layer_name = convert_name(layer.name)
		activation = layer.activation.__name__
		kotlin_code += f"\t\tval {layer_name}Output = denseLayer({inputs}, {layer_name}Weights, {layer_name}Biases)\n"
		kotlin_code += f"\t\tval {layer_name}Activated = applyActivation({layer_name}Output, \"{activation}\")\n"
		inputs = f"{layer_name}Activated"
	
	kotlin_code += f"val z = {inputs}[0] * (Z_MAX - Z_MIN) + Z_MIN\n"
	kotlin_code += f"\t\treturn z\n"
	kotlin_code += "\t}\n"
	kotlin_code += "}\n"
	
	return kotlin_code



# Convert the Keras model to Kotlin code
kotlin_code = keras_to_kotlin(model)
print(kotlin_code)
