package net.auberson.scherer.masterthesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;

import net.auberson.scherer.masterthesis.model.ClassifierResult;
import net.auberson.scherer.masterthesis.model.Element;
import net.auberson.scherer.masterthesis.util.BatchClassifier;
import net.auberson.scherer.masterthesis.util.NLCProperties;
import net.auberson.scherer.masterthesis.util.Sampler;

/**
 * Initialization code common to all experiments
 *
 */
public class ExperimentBase {

	protected final int classCount;
	protected final List<String> classNames;
	protected final NaturalLanguageClassifier service;
	protected final Map<String, Integer> sampleCount;

	protected ExperimentBase(String[] classes, int minSampleCount) {
		// Ensure classes were specified
		if (classes.length < 2) {
			System.err.println("Please specify several classes with which to execute the experiment.");
			System.exit(-1);
		}

		// Keep the number of classes and the class names
		classCount = classes.length;
		classNames = Arrays.asList(classes);

		// Ensure each class has a sufficient number of samples
		sampleCount = Sampler.getSampleCount(classNames);
		for (Map.Entry<String, Integer> entry : sampleCount.entrySet()) {
			if (entry.getValue().intValue() < minSampleCount) {
				System.err.println("Class '" + entry.getKey() + "' does not have enough samples for this experiment.");
				System.err.println(
						entry.getValue() + " samples were found, but " + minSampleCount + " entries are needed.");
				System.err.println("Please choose another class.");
				System.exit(-1);
			}
		}

		// Programmatically suppress the HTTP logging
		Logger.getLogger("com.ibm.watson.developer_cloud.util.HttpLogging").setLevel(Level.WARNING);

		// Initialize Watson NLC
		NLCProperties nlcProps = new NLCProperties();
		service = new NaturalLanguageClassifier();
		service.setUsernameAndPassword(nlcProps.getUsername(), nlcProps.getPassword());
	}

	/**
	 * Returns a file in the specified directory consisting of the prefix provided,
	 * followed by the class names, all in kebap-case. If a file of that name
	 * exists, it is deleted. In any case, an empty file is returned
	 */
	protected File getEmptyFile(File directory, String... prefixes) {
		File file = new File(directory, getFileName(prefixes));
		try {
			file.delete();
			file.getParentFile().mkdirs();
			file.createNewFile();
		} catch (IOException e) {
			System.err.println("A disk error occured trying to create an empty file at " + file.getAbsolutePath());
			e.printStackTrace();
			System.exit(-1);
		}
		return file;
	}

	/**
	 * Returns a file name consisting of the prefix provided, followed by the class
	 * names, all in kebap-case.
	 */
	protected String getFileName(String... prefixes) {
		StringBuilder builder = new StringBuilder();
		for (String prefix : prefixes) {
			builder.append(prefix);
		}
		for (String className : classNames) {
			builder.append('-').append(className);
		}
		return builder.append(".csv").toString();
	}

	/**
	 * Create a new classifier, using a training set to train it from scratch
	 * 
	 * @param trainingSet
	 *            a file containing the training set CSV: A text in the first
	 *            column, the expected class in the second
	 * @param nameSuffix
	 *            a suffix (or several) to use in naming the classifier
	 * @return a trained BatchClassifier
	 */
	protected BatchClassifier trainClassifier(File trainingSet, String... nameSuffix) {
		StringBuilder name = new StringBuilder("Classifier");
		for (String string : nameSuffix) {
			name.append(string);
		}
		try {
			return new BatchClassifier(service, name.toString(), "en", trainingSet);
		} catch (FileNotFoundException e) {
			System.err.println("An unexpected error occured trying to train the classifier '" + name + "'");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	/**
	 * From a Results CSV file, retrieve the N entries with the lowest confidence
	 * 
	 * @param results
	 *            File object pointing to the results file
	 * @param n
	 *            number of entries to return
	 * @return the N results with the lowest confidence
	 */
	protected List<ClassifierResult> getBottomN(File results, int n) {
		CSVParser inputCsv = null;
		try {
			inputCsv = CSVFormat.DEFAULT.parse(new FileReader(results));
		} catch (IOException e) {
			System.err.println("Unable to parse the result CSV at '" + results.getAbsolutePath() + "'");
			e.printStackTrace();
			System.exit(-1);
		}

		List<ClassifierResult> resultList = new ArrayList<ClassifierResult>();
		for (CSVRecord csvRecord : inputCsv) {
			resultList.add(new ClassifierResult(csvRecord));
		}
		try {
			inputCsv.close();
		} catch (IOException e) {
			System.err.println("Unable to close the result CSV at '" + results.getAbsolutePath() + "'");
			e.printStackTrace();
		}

		resultList.sort(ClassifierResult.COMPARATOR);

		return resultList.subList(0, Math.min(resultList.size(), n));
	}

	/**
	 * Outputs the samples to the given file as CSV: First column is the text,
	 * second is the class.
	 * 
	 * @param samples
	 * @param outputFile
	 */
	protected void outputSamples(List<Element> samples, File outputFile) {
		PrintWriter out;
		try {
			out = new PrintWriter(outputFile);
			for (Element sample : samples) {
				out.print("\"" + sample.getText() + "\", ");
				out.println(sample.getClassLabel());
			}
			out.close();
		} catch (FileNotFoundException e) {
			System.err.println("Unable to write samples to dataset CSV at '" + outputFile.getAbsolutePath() + "'");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Calculate a confusion matrix for the specified results CSV
	 * 
	 * @param results
	 * @param confMatrix
	 */
	protected void outputConfMatrix(File results, File confMatrix) {
		CSVParser inputCsv = null;
		try {
			inputCsv = CSVFormat.DEFAULT.parse(new FileReader(results));
		} catch (IOException e) {
			System.err.println("Unable to parse the result CSV at '" + results.getAbsolutePath() + "'");
			e.printStackTrace();
			System.exit(-1);
		}

		HashMap<String, Integer> headers = new HashMap<String, Integer>();
		for (int i = 0; i < classCount; i++) {
			headers.put(classNames.get(i), i);
		}

		int[][] matrix = new int[classCount][classCount];
		for (CSVRecord csvRecord : inputCsv) {
			final int actualClass = headers.get(csvRecord.get(1)).intValue();
			final int detectedClass = headers.get(csvRecord.get(2)).intValue();

			matrix[actualClass][detectedClass]++;
		}

		try {
			inputCsv.close();
		} catch (IOException e) {
			System.err.println("Unable to close the result CSV at '" + results.getAbsolutePath() + "'");
			e.printStackTrace();
		}
	}
}
