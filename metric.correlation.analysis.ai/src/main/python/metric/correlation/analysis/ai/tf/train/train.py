import json

import matplotlib.pyplot as plt

import os
from os import listdir

import re
import shutil
import string

import paths

import tensorflow as tf
import numpy as np

from tensorflow.keras import layers
from tensorflow.keras import losses

SHOW_STATISTICS = False


def custom_standardization(input_data):
    return tf.strings.lower(input_data)


def vectorize_text(text, label):
    text = tf.expand_dims(text, -1)
    return vectorize_layer(text), label


batch_size = 32
seed = 42

raw_train_ds = tf.keras.utils.text_dataset_from_directory(
    paths.training_folder,
    batch_size=batch_size,
    validation_split=0.2,
    subset='training',
    seed=seed)

raw_val_ds = tf.keras.utils.text_dataset_from_directory(
    paths.training_folder,
    batch_size=batch_size,
    validation_split=0.2,
    subset='validation',
    seed=seed)

raw_test_ds = tf.keras.utils.text_dataset_from_directory(
    paths.training_folder,
    batch_size=batch_size)

max_features = 10000
sequence_length = 250

vectorize_layer = layers.TextVectorization(
    standardize=custom_standardization,
    max_tokens=max_features,
    output_mode='int',
    output_sequence_length=sequence_length)

train_text = raw_train_ds.map(lambda x, y: x)
vectorize_layer.adapt(train_text)

train_ds = raw_train_ds.map(vectorize_text)
val_ds = raw_val_ds.map(vectorize_text)
test_ds = raw_test_ds.map(vectorize_text)

AUTOTUNE = tf.data.AUTOTUNE

train_ds = train_ds.cache().prefetch(buffer_size=AUTOTUNE)
val_ds = val_ds.cache().prefetch(buffer_size=AUTOTUNE)
test_ds = test_ds.cache().prefetch(buffer_size=AUTOTUNE)

embedding_dim = 16

model = tf.keras.Sequential([
  layers.Embedding(max_features, embedding_dim),
  layers.Dropout(0.2),
  layers.GlobalAveragePooling1D(),
  layers.Dropout(0.2),
  layers.Dense(1, activation='sigmoid')])

model.summary()

model.compile(loss=losses.BinaryCrossentropy(),
              optimizer='adam',
              metrics=[tf.metrics.BinaryAccuracy(threshold=0.5)])

epochs = 10
history = model.fit(
    train_ds,
    validation_data=val_ds,
    epochs=epochs)

loss, accuracy = model.evaluate(test_ds)

print("Loss: ", loss)
print("Accuracy: ", accuracy)

history_dict = history.history
history_dict.keys()

acc = history_dict['binary_accuracy']
val_acc = history_dict['val_binary_accuracy']
loss = history_dict['loss']
val_loss = history_dict['val_loss']

epochs = range(1, len(acc) + 1)

if SHOW_STATISTICS:
    plt.plot(epochs, loss, 'bo', label='Training loss')
    plt.plot(epochs, val_loss, 'b', label='Validation loss')
    plt.title('Training and validation loss')
    plt.xlabel('Epochs')
    plt.ylabel('Loss')
    plt.legend()

    plt.show()

    plt.plot(epochs, acc, 'bo', label='Training acc')
    plt.plot(epochs, val_acc, 'b', label='Validation acc')
    plt.title('Training and validation accuracy')
    plt.xlabel('Epochs')
    plt.ylabel('Accuracy')
    plt.legend(loc='lower right')

    plt.show()

export_model = tf.keras.Sequential([
  vectorize_layer,
  model,
  layers.Activation('sigmoid')
])

export_model.compile(
    loss=losses.BinaryCrossentropy(from_logits=False), optimizer="adam", metrics=['accuracy']
)

# Test it with `raw_test_ds`, which yields raw strings
metrics = export_model.evaluate(raw_test_ds, return_dict=True)
print(metrics)

print(raw_train_ds.class_names)
export_model.save(paths.model_path)

for file in os.listdir(paths.feat_req_pot_security_folder):
    security = paths.manual_folder + 'security'
    other = paths.manual_folder + 'other'
    os.makedirs(security, exist_ok=True)
    os.makedirs(other, exist_ok=True)

    with open(paths.feat_req_pot_security_folder + file) as f:
        json_content = json.load(f)
        value = json_content.get('title')+'\n\n'+json_content.get('description')
        print(value)

    prediction = export_model(tf.constant([value]))

    predicted_label = tf.argmax(prediction, axis=1).numpy()[0]

    print('Prediction for '+file)
    print(prediction)
    print(predicted_label)
    p = prediction[0][0].numpy()
    print(p)
    if p >= .5:
        label = 1
        print('Labeled security: '+file)
    else:
        label = 0
    print(label)

    while True:
        decision = input("Is this a security request? (y/n), abort (q): ")
        if decision.lower() == 'y':
            shutil.move(paths.feat_req_pot_security_folder+file, security+'/'+file) 
            break
        elif decision.lower() == 'n':
            shutil.move(paths.feat_req_pot_security_folder+file, other+'/'+file)
            break
        elif decision.lower() == 'q':   
            exit(0)
        else:
            print("Invalid input, please enter 'y', 'n', or 'q'.")
