/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.orlinskas.videofacefinder.tflite;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.tensorflow.lite.Interpreter;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * - https://github.com/tensorflow/models/tree/master/research/object_detection
 * where you can find the training code.
 *
 * To use pretrained models in the API or convert to TF Lite models, please see docs for details:
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md#running-our-model-on-android
 */
public class TFLiteClassifier {

    private static final int OUTPUT_SIZE = 192;

    // Only return this many results.
    private static final int NUM_DETECTIONS = 1;

    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;

    // Number of threads in the java app
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    // Config values.
    private int inputSize;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;

    private ByteBuffer imgData;

    private Interpreter tfLite;

    private TFLiteClassifier() {}

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param inputSize The size of image input
     * @param isQuantized Boolean representing model is quantized or not
     */
    public static TFLiteClassifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final int inputSize,
            final boolean isQuantized
    )
            throws IOException {

        final TFLiteClassifier d = new TFLiteClassifier();

        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        InputStream labelsInput = assetManager.open(actualFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            d.labels.add(line);
        }
        br.close();

        d.inputSize = inputSize;

        try {
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        d.tfLite.setNumThreads(NUM_THREADS);
        d.outputLocations = new float[1][NUM_DETECTIONS][4];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];
        return d;
    }

    public float[] recognize(final Bitmap bitmap) {
        try {
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            imgData.rewind();
            for (int i = 0; i < inputSize; ++i) {
                for (int j = 0; j < inputSize; ++j) {
                    int pixelValue = intValues[i * inputSize + j];
                    if (isModelQuantized) {
                        // Quantized model
                        imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                        imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                        imgData.put((byte) (pixelValue & 0xFF));
                    } else { // Float model
                        imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                        imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                        imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    }
                }
            }

            float[][] embeddings = new float[1][OUTPUT_SIZE];

            tfLite.run(imgData, embeddings);

            return embeddings[0];
        } catch (Exception e) {
            e.printStackTrace();
            return new float[]{};
        }
    }

    public Map<Integer, Object> recognizeMultiply(final List<Bitmap> bitmaps) {
        try {

            ArrayList<Object> imgsData = new ArrayList<>();

            for (Bitmap bitmap : bitmaps) {
                bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                imgData.rewind();
                for (int i = 0; i < inputSize; ++i) {
                    for (int j = 0; j < inputSize; ++j) {
                        int pixelValue = intValues[i * inputSize + j];
                        if (isModelQuantized) {
                            // Quantized model
                            imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                            imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                            imgData.put((byte) (pixelValue & 0xFF));
                        } else { // Float model
                            imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                            imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                            imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                        }
                    }
                }

                imgsData.add(imgData);
            }

            float[][] embeddings = new float[1][OUTPUT_SIZE];

            Map<Integer, Object> output = new HashMap<>();

            tfLite.runForMultipleInputsOutputs(new ArrayList[]{imgsData}, output);

            return output;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public void close() {
        tfLite.close();
    }
}
