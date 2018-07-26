package net.auberson.scherer.masterthesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import net.auberson.scherer.masterthesis.model.ClassifierResult;
import net.auberson.scherer.masterthesis.model.Element;
import net.auberson.scherer.masterthesis.model.IncrementableInt;
import net.auberson.scherer.masterthesis.model.StatisticsCounter;
import net.auberson.scherer.masterthesis.model.StatisticsResults;
import net.auberson.scherer.masterthesis.util.BatchClassifier;
import net.auberson.scherer.masterthesis.util.IOUtil;
import net.auberson.scherer.masterthesis.util.Sampler;

/**
 * Initialization code common to all experiments
 *
 */
public class ExperimentBase {

	protected final int classCount;
	protected final List<String> classNames;
	protected final Map<String, Integer> sampleCount;

	protected ExperimentBase(String[] classes, int minSampleCount) {
		// Programmatically suppress the HTTP logging
		Logger.getLogger("com.ibm.watson.developer_cloud.util.HttpLogging").setLevel(Level.WARNING);

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
	 * Returns a file in the specified directory consisting of the prefix provided,
	 * followed by the class names, all in kebap-case. If a file of that name
	 * exists, it is deleted. A copy of the file passed as source is made as the
	 * starting point for this file.
	 */
	protected File getFileDuplicate(File directory, File source, String... prefixes) {
		File file = new File(directory, getFileName(prefixes));
		try {
			file.delete();
			if (source != null) {
				FileUtils.copyFile(source, file);
			} else {
				file.createNewFile();
			}
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
			return new BatchClassifier(name.toString(), "en", trainingSet);
		} catch (FileNotFoundException e) {
			System.err.println("An unexpected error occured trying to train the classifier '" + name + "'");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	protected void mergeDataset(File outputFile, int dataSetSize, File... sources) {
		final Map<String, IncrementableInt> counters = new HashMap<String, IncrementableInt>();

		PrintWriter out = IOUtil.getWriter(outputFile);

		// Initialize counters
		for (String className : classNames) {
			counters.put(className, new IncrementableInt());
		}

		// Copy entry for entry until the dataSetSize for each entry is reached
		for (File source : sources) {
			CSVParser inputCsv = IOUtil.openCSV(source);
			for (CSVRecord csvRecord : inputCsv) {
				final String classLabel = csvRecord.get(1).trim();
				if (counters.get(classLabel).lessThan(dataSetSize)) {
					out.print("\"" + csvRecord.get(0) + "\", ");
					out.println(classLabel);
					counters.get(classLabel).inc();
				}
			}
			IOUtil.close(inputCsv);
		}

		IOUtil.close(out);
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
		CSVParser inputCsv = IOUtil.openCSV(results);

		List<ClassifierResult> resultList = new ArrayList<ClassifierResult>();
		for (CSVRecord csvRecord : inputCsv) {
			resultList.add(new ClassifierResult(csvRecord));
		}

		resultList.sort(ClassifierResult.COMPARATOR);

		IOUtil.close(inputCsv);
		return resultList.subList(0, Math.min(resultList.size(), n));
	}

	/**
	 * From a Results CSV file, retrieve the entries with a confidence below a
	 * certain threshold
	 * 
	 * @param results
	 *            File object pointing to the results file
	 * @param n
	 *            number of entries to return
	 * @return the N results with the lowest confidence
	 */
	protected List<ClassifierResult> getSamplesUnderThreshold(File results, double threshold) {
		CSVParser inputCsv = IOUtil.openCSV(results);

		List<ClassifierResult> resultList = new ArrayList<ClassifierResult>();
		for (CSVRecord csvRecord : inputCsv) {
			ClassifierResult result = new ClassifierResult(csvRecord);
			if (result.getConfidence().doubleValue() < threshold) {
				resultList.add(result);
			}
		}

		// If we need a list sorted by confidence, do this:
		// resultList.sort(ClassifierResult.COMPARATOR);

		IOUtil.close(inputCsv);
		return resultList;
	}

	/**
	 * Outputs the samples to the given file as CSV: First column is the text,
	 * second is the class.
	 * 
	 * @param samples
	 * @param outputFile
	 */
	protected void outputSamples(List<? extends Element> samples, File outputFile) {
		PrintWriter out = IOUtil.getWriter(outputFile);
		for (Element sample : samples) {
			out.print("\"" + sample.getText() + "\", ");
			out.println(sample.getClassLabel());
		}
		IOUtil.close(out);
	}

	/**
	 * Outputs the results to the given file as CSV: First column is the text,
	 * second is the actual class, third the detected class, and fourth the
	 * confidence.
	 * 
	 * @param samples
	 * @param outputFile
	 */
	protected void outputClassifierResult(List<? extends ClassifierResult> results, File outputFile) {
		PrintWriter out = IOUtil.getAppendingWriter(outputFile);
		for (ClassifierResult result : results) {
			out.print("\"" + result.getText() + "\", ");
			out.print(result.getClassLabel() + ", ");
			out.print(result.getDetectedClassLabel() + ", ");
			out.println(result.getConfidence());
		}
		IOUtil.close(out);
	}

	/**
	 * Calculate a confusion matrix for the specified results CSV
	 * 
	 * @param results
	 * @param confMatrix
	 */
	protected void outputConfMatrix(File results, File outputFile) {
		CSVParser inputCsv = IOUtil.openCSV(results);
		PrintWriter out = IOUtil.getWriter(outputFile);

		HashMap<String, Integer> headers = new HashMap<String, Integer>();
		for (int i = 0; i < classCount; i++) {
			final String classLabel = classNames.get(i);
			headers.put(classLabel, i);
			out.print(", " + classLabel);
		}
		out.println();

		int[][] matrix = new int[classCount][classCount];
		for (CSVRecord csvRecord : inputCsv) {
			final int actualClass = headers.get(csvRecord.get(1).trim()).intValue();
			final int detectedClass = headers.get(csvRecord.get(2).trim()).intValue();

			matrix[detectedClass][actualClass]++;
		}

		for (int i = 0; i < matrix.length; i++) {
			out.print(classNames.get(i));
			for (int j = 0; j < matrix[i].length; j++) {
				out.print(", ");
				out.print(matrix[i][j]);
			}
			out.println();
		}

		IOUtil.close(out);
		IOUtil.close(inputCsv);
	}

	/**
	 * Updates global and class-specific statistics files
	 * 
	 * @param trainingSetSize
	 * @param iter
	 * @param reviewedItemsCount
	 * @param testSetSize
	 */
	protected void updateStats(File results, File outputDir, Object iter, int reviewedItemsCount) {
		outputDir.getParentFile().mkdirs();
		CSVParser inputCsv = IOUtil.openCSV(results);

		// Initialize empty structure to count tn, tp, fn, fp
		List<StatisticsCounter> counters = new ArrayList<StatisticsCounter>(classCount);
		for (String className : classNames) {
			counters.add(new StatisticsCounter(className));
		}

		// Iterate through all result records, and update the counters
		for (CSVRecord csvRecord : inputCsv) {
			final String actualClass = csvRecord.get(1).trim();
			final String detectedClass = csvRecord.get(2).trim();

			// Update each statistics object
			for (StatisticsCounter counter : counters) {
				counter.update(actualClass, detectedClass);
			}
		}
		IOUtil.close(inputCsv);

		// Calculate statistics and update the stats files
		for (StatisticsCounter counter : counters) {
			File file = new File(outputDir, getFileName("Stats", capitalize(counter.getClassLabel())));
			PrintWriter out = IOUtil.getAppendingWriter(file);
			StatisticsResults stats = new StatisticsResults(counter);
			stats.output(out, iter, reviewedItemsCount);
			IOUtil.close(out);
		}

		// Update global statistics file
		File file = new File(outputDir, getFileName("Stats"));
		PrintWriter out = IOUtil.getAppendingWriter(file);
		StatisticsResults stats = StatisticsResults.aggregate(counters);
		stats.output(out, iter, reviewedItemsCount);
		IOUtil.close(out);
	}

	/**
	 * Empties global and class-specific statistics files, ensures the files exist
	 * for appending
	 */
	protected void clearStats(File outputDir) {
		for (String className : classNames) {
			File file = getEmptyFile(outputDir, "Stats", capitalize(className));
			PrintWriter out = IOUtil.getAppendingWriter(file);
			StatisticsResults.outputHeader(out);
			IOUtil.close(out);
		}
		File file = getEmptyFile(outputDir, "Stats");
		PrintWriter out = IOUtil.getAppendingWriter(file);
		StatisticsResults.outputHeader(out);
		IOUtil.close(out);
	}

	private String capitalize(String input) {
		return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
	}

	/**
	 * Updates global and class-specific statistics files
	 * 
	 * @param trainingSetSize
	 * @param iter
	 * @param reviewedItemsCount
	 * @param testSetSize
	 */
	protected void updateReviewStats(File reviewFile, File outputDir, Object iter) {
		final Map<String, IncrementableInt> counters = new HashMap<String, IncrementableInt>();
		outputDir.getParentFile().mkdirs();

		// Initialize counters
		for (String className : classNames) {
			counters.put(className, new IncrementableInt());
		}

		CSVParser inputCsv = IOUtil.openCSV(reviewFile);
		for (CSVRecord csvRecord : inputCsv) {
			counters.get(csvRecord.get(1).trim()).inc();
		}
		IOUtil.close(inputCsv);

		// Update global statistics file
		File file = new File(outputDir, getFileName("Review"));
		PrintWriter out = IOUtil.getAppendingWriter(file);
		out.print(iter);
		for (String className : classNames) {
			out.print(",");
			out.print(counters.get(className));
		}
		out.println();

		IOUtil.close(out);
	}

	/**
	 * Empties the review statistics file, ensuring the files exist for appending
	 */
	protected void clearReviewStats(File outputDir) {
		File file = getEmptyFile(outputDir, "Review");
		PrintWriter out = IOUtil.getAppendingWriter(file);
		out.print("iter");
		for (String className : classNames) {
			out.print(",");
			out.print(className);
		}
		out.println();
		IOUtil.close(out);
	}

}
