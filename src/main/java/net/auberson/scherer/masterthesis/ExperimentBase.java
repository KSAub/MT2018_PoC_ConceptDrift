package net.auberson.scherer.masterthesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;

import net.auberson.scherer.masterthesis.util.BatchClassifier;
import net.auberson.scherer.masterthesis.util.NLCProperties;
import net.auberson.scherer.masterthesis.util.Sampler;

/**
 * Initialization code common to all experiments
 *
 */
public class ExperimentBase {

	protected final int classCount;
	protected final Collection<String> classNames;
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
}