import tensorflow as tf
import os
import paths

export_model = tf.keras.models.load_model(paths.model_path)

security = 0
for file in os.listdir(paths.training_folder+'other/'):
    with open(paths.training_folder+'other/'+file) as stream:
        value = stream.read()
        prediction = export_model(tf.constant([value]))

        predicted_label = tf.argmax(prediction, axis=1).numpy()[0]
  
        print('Prediction for '+file)
        print(prediction)
        print(predicted_label)
        p = prediction[0][0].numpy()
        print(p)
        if p >= .5:
            label = 1
            security += 1
            print('Labeled security: '+file)
        else:
            label = 0
        print(label)
print("Security requests in other: "+str(security))
