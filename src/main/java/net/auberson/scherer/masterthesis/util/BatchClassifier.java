package net.auberson.scherer.masterthesis.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassificationCollection;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifiedClass;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.Classifier;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifierList;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifyCollectionOptions;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifyInput;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.CollectionItem;

import okhttp3.internal.http2.ConnectionShutdownException;

/**
 * A utility that allows the classifier to be called on large files. <br>
 * This class will group together samples in Batches, and call the classifier
 * once per batch, then merge the results.
 *
 */
public class BatchClassifier {
	private static final int NUM_RETRIES = 5;
	private NaturalLanguageClassifier svc;
	private Classifier classifier;

	/**
	 * Creates a BatchClassifier by retrieving it using the given ClassifierId
	 * 
	 * @param classifierId
	 */
	public BatchClassifier(String classifierId) {
		int retries = NUM_RETRIES;
		while (true) {
			try {
				this.classifier = getService().getClassifier(classifierId).execute();
				break;
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ConnectionShutdownException) {
					svc = null;
				}
				System.err.println(e.toString());
				if (--retries == 0) {
					throw e;
				}
			}
		}
	}

	/**
	 * Creates a BatchClassifier by creating a new classifier from scratch and
	 * training it.
	 * 
	 * @param trainingSet
	 * @throws FileNotFoundException
	 */
	public BatchClassifier(String name, String language, File trainingSet) throws FileNotFoundException {
		String classifierId = getClassifierId(name);
		if (classifierId != null) {
			System.out.println("Deleting previously existing Classifier " + classifierId);
			delete(classifierId);
		}

		long timerStart = System.currentTimeMillis();
		Classifier newClassifier = createClassifier(name, language, trainingSet);
		while (newClassifier.getStatus().equals("Training")) {
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			newClassifier = updateClassifier(newClassifier);
		}
		System.out.println("Classifier trained in " + ((System.currentTimeMillis() - timerStart) / 60000) + " Minutes");
		this.classifier = newClassifier;
	}

	private NaturalLanguageClassifier getService() {
		// Not thread-safe!
		if (svc != null) {
			return svc;
		}

		// Programmatically suppress the HTTP logging
		Logger.getLogger("com.ibm.watson.developer_cloud.util.HttpLogging").setLevel(Level.WARNING);
		
		// Initialize Watson NLC
		NLCProperties nlcProps = new NLCProperties();
		svc = new NaturalLanguageClassifier();
		svc.setUsernameAndPassword(nlcProps.getUsername(), nlcProps.getPassword());

		return svc;
	}

	/**
	 * Classify the contents of a data set, write the results to a CSV file. <br>
	 * <br>
	 * The results written to the file are in the following format: <br>
	 * sample text, expected class, detected class 1, confidence 1, detected class
	 * 2, confidence 2, etc... <br>
	 * 
	 * @param input
	 *            a File pointing to a CSV with at least 2 columns: Text and Class
	 * @param sampleSize
	 *            the number of samples in the File passed
	 * @param output
	 *            a file to which the results will be appended
	 */
	public void classify(File input, File output) {

		// Parse the CSV file
		Iterator<CSVRecord> inputCsv = null;
		try {
			inputCsv = CSVFormat.DEFAULT.parse(new FileReader(input)).iterator();
		} catch (IOException e) {
			System.err.println("A error occured trying to read CSV file at " + input.getAbsolutePath());
			e.printStackTrace();
			System.exit(-1);
		}

		// Open the output file for appending ("true")
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(output, true));
		} catch (IOException e) {
			System.err.println("Unable to open the output file at " + input.getAbsolutePath());
			e.printStackTrace();
			System.exit(-1);
		}

		// Call the classifier in batches, append the result to output file
		Batch batch = new Batch(classifier);

		while (inputCsv.hasNext()) {
			CSVRecord csvRecord = inputCsv.next();
			batch.add(csvRecord.get(0), csvRecord.get(1).trim());

			if (batch.isFull() || !inputCsv.hasNext()) {

				ClassificationCollection results = classifyBatch(batch);

				for (CollectionItem result : results.getCollection()) {
					// Output the original text (in quotes)...
					out.print("\"" + result.getText() + "\", ");
					// ...the expected value...
					out.print(batch.expectedValues.get(result.getText()));

					for (ClassifiedClass classification : result.getClasses()) {
						// ...the detected class (most likely first)...
						out.print(", " + classification.getClassName());
						// ...and the confidence
						out.print(", " + classification.getConfidence());
					}

					out.println();
				}

				batch = new Batch(classifier);
			}
		}

		// Close the output file, writing everything to disk
		out.close();
	}

	/**
	 * Classify the contents of a data set, calculate the accuracy of the
	 * classifier, and append the result to a file.<br>
	 * <br>
	 * The results written to the file are in the following format: <br>
	 * sample size, label, # processed, # correct, # incorrect, accuracy % <br>
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
			ClassificationCollection results = classifyBatch(batch);
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

	private String getClassifierId(String classifierName) {
		int retries = NUM_RETRIES;
		ClassifierList classifiers = null;
		while (true) {
			try {
				classifiers = getService().listClassifiers().execute();
				break;
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ConnectionShutdownException) {
					svc = null;
				}
				System.err.println(e.toString());
				if (--retries == 0) {
					throw e;
				}
			}
		}

		for (Classifier classifier : classifiers.getClassifiers()) {
			if (classifier.getName().equals(classifierName)) {
				return classifier.getClassifierId();
			}
		}
		return null;
	}

	private Classifier updateClassifier(Classifier newClassifier) {
		int retries = NUM_RETRIES;
		while (true) {
			try {
				return getService().getClassifier(newClassifier.getClassifierId()).execute();
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ConnectionShutdownException) {
					svc = null;
				}
				System.err.println(e.toString());
				if (--retries == 0) {
					throw e;
				}
			}
		}
	}

	private Classifier createClassifier(String name, String language, File trainingSet) throws FileNotFoundException {
		int retries = NUM_RETRIES;
		while (true) {
			try {
				return getService().createClassifier(name, language, trainingSet).execute();
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ConnectionShutdownException) {
					svc = null;
				}
				System.err.println(e.toString());
				if (--retries == 0) {
					throw e;
				}
			}
		}
	}

	private ClassificationCollection classifyBatch(Batch batch) {
		int retries = NUM_RETRIES;
		while (true) {
			try {
				return getService().classifyCollection(batch.getClassifyCollectionOptions()).execute();
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ConnectionShutdownException) {
					svc = null;
				}
				System.err.println(e.toString());
				if (--retries == 0) {
					throw e;
				}
			}
		}
	}

	/**
	 * Causes the underlying classifier to be deleted from the IBM Cloud. This
	 * object cannot be used afterwards.
	 */
	private void delete(String classifierId) {
		int retries = NUM_RETRIES;
		while (true) {
			try {
				getService().deleteClassifier(classifierId).execute();
				break;
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ConnectionShutdownException) {
					svc = null;
				}
				System.err.println(e.toString());
				if (--retries == 0) {
					throw e;
				}
			}
		}
	}

	/**
	 * Causes the underlying classifier to be deleted from the IBM Cloud. This
	 * object cannot be used afterwards.
	 */
	public void delete() {
		delete(classifier.getClassifierId());
	}

	public String getName() {
		return classifier.getName();
	}

	@Override
	public String toString() {
		return classifier.toString();
	}

}
