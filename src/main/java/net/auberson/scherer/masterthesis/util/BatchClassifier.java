package net.auberson.scherer.masterthesis.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassificationCollection;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.Classifier;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifierList;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifyCollectionOptions;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifyInput;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.CollectionItem;

/**
 * A utility that allows the classifier to be called on large files. <br>
 * This class will group together samples in Batches, and call the classifier
 * once per batch, then merge the results.
 *
 */
public class BatchClassifier {
	private final NaturalLanguageClassifier service;
	private final Classifier classifier;

	/**
	 * Creates a BatchClassifier by retrieving it using the given ClassifierId
	 * 
	 * @param classifierId
	 */
	public BatchClassifier(NaturalLanguageClassifier service, String classifierId) {
		this.classifier = service.getClassifier(classifierId).execute();
		this.service = service;
	}

	/**
	 * Creates a BatchClassifier by creating a new classifier from scratch and
	 * training it.
	 * 
	 * @param trainingSet
	 * @throws FileNotFoundException
	 */
	public BatchClassifier(NaturalLanguageClassifier service, String name, String language, File trainingSet)
			throws FileNotFoundException {
		String classifierId = getClassifierId(service, name);
		if (classifierId != null) {
			System.out.println("Deleting previously existing Classifier " + classifierId);
			service.deleteClassifier(classifierId).execute();
		}

		long timerStart = System.currentTimeMillis();
		Classifier newClassifier = service.createClassifier(name, language, trainingSet).execute();
		while (newClassifier.getStatus().equals("Training")) {
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			newClassifier = service.getClassifier(newClassifier.getClassifierId()).execute();
		}
		System.out.println("Classifier trained in " + ((System.currentTimeMillis() - timerStart) / 60000) + " Minutes");
		this.classifier = newClassifier;
		this.service = service;
	}

	/**
	 * Classify the contents of a data set, calculate the accuracy of the
	 * classifier, and append the result to a file.<br>
	 * The results written to the file are in the following format: sample size,
	 * label, # processed, # correct, # incorrect, accuracy % <br>
	 * 
	 * @param input
	 *            a File pointing to a CSV with at least 2 columns: Text and Class
	 * @param sampleSize
	 *            the number of samples in the File passed
	 * @param label
	 *            a label passed to the output file
	 * @param output
	 *            a file to which the results will be appended
	 */
	public void evaluateAccuracy(File input, int sampleSize, String label, File output)
			throws FileNotFoundException, IOException {
		CSVParser inputCsv = CSVFormat.DEFAULT.parse(new FileReader(input));

		Batch currentBatch = new Batch(classifier);
		List<Batch> allBatches = new ArrayList<Batch>(sampleSize / Project.MAX_SAMPLES_PER_CLASSIFICATION_REQUEST + 1);
		allBatches.add(currentBatch);

		// Batch samples
		for (CSVRecord csvRecord : inputCsv) {
			if (currentBatch.isFull()) {
				currentBatch = new Batch(classifier);
				allBatches.add(currentBatch);
			}
			currentBatch.add(csvRecord.get(0), csvRecord.get(1).trim());
		}

		// Process each batch sequentially
		int processed = 0;
		int correct = 0;
		int incorrect = 0;

		for (Batch batch : allBatches) {
			ClassificationCollection results = service.classifyCollection(batch.getClassifyCollectionOptions())
					.execute();
			for (CollectionItem result : results.getCollection()) {
				String expected = batch.expectedValues.get(result.getText());
				String returned = result.getTopClass();
				processed++;
				if (expected.equals(returned)) {
					correct++;
				} else {
					incorrect++;
				}
			}
		}

		PrintWriter out = new PrintWriter(new FileWriter(output, true));
		out.println(sampleSize + ", " + label + ", " + processed + ", " + correct + ", " + incorrect);
		out.close();

		System.out.println("samplesize, label, processed, correct, incorrect, accuracy:");
		System.out.println(sampleSize + ", " + label + ", " + processed + ", " + correct + ", " + incorrect + ", "
				+ (100 * correct / processed) + "%");

		inputCsv.close();
	}

	private static class Batch {
		final ClassifyCollectionOptions.Builder parameters;
		final HashMap<String, String> expectedValues = new HashMap<String, String>(
				Project.MAX_SAMPLES_PER_CLASSIFICATION_REQUEST);
		int count = 0;

		Batch(Classifier classifier) {
			parameters = new ClassifyCollectionOptions.Builder().classifierId(classifier.getClassifierId());
		}

		void add(String text, String expectedClass) {
			if (isFull()) {
				throw new IllegalArgumentException("Batch is full");
			}
			ClassifyInput classifyInput = new ClassifyInput();
			classifyInput.setText(text);
			parameters.addClassifyInput(classifyInput);
			expectedValues.put(text, expectedClass);
			count++;
		}

		boolean isFull() {
			return count >= Project.MAX_SAMPLES_PER_CLASSIFICATION_REQUEST;
		}

		ClassifyCollectionOptions getClassifyCollectionOptions() {
			return parameters.build();
		}
	}

	private String getClassifierId(NaturalLanguageClassifier service, String classifierName) {
		ClassifierList classifiers = service.listClassifiers().execute();
		for (Classifier classifier : classifiers.getClassifiers()) {
			if (classifier.getName().equals(classifierName)) {
				return classifier.getClassifierId();
			}
		}
		return null;
	}

	/**
	 * Causes the underlying classifier to be deleted from the IBM Cloud. This
	 * object cannot be used afterwards.
	 */
	public void delete() {
		service.deleteClassifier(classifier.getClassifierId()).execute();
	}

	@Override
	public String toString() {
		return classifier.toString();
	}

}
