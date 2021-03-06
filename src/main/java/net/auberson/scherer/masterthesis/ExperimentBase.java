package net.auberson.scherer.masterthesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

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

	protected ExperimentBase() {
		classCount = 0;
		classNames = Collections.emptyList();
		sampleCount = Collections.emptyMap();
	}

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
	 * Calculate a confusion matrix for the specified results CSV, another for
	 * 
	 * @param results
	 * @param confMatrix
	 */
	protected void outputConfMatrix(File outputDir, File results, double threshold, Object iter) {
		String threshPercent = Long.toString(Math.round(threshold * 100));
		File outputFile = getEmptyFile(outputDir, "Iteration", iter.toString(), "ConfMtx");
		File outputUnder = getEmptyFile(outputDir, "Iteration", iter.toString(), "ConfMtxUnder", threshPercent);
		File outputOver = getEmptyFile(outputDir, "Iteration", iter.toString(), "ConfMtxOver", threshPercent);

		CSVParser inputCsv = IOUtil.openCSV(results);

		HashMap<String, Integer> headers = new HashMap<String, Integer>();
		for (int i = 0; i < classCount; i++) {
			final String classLabel = classNames.get(i);
			headers.put(classLabel, i);
		}
		int[][] matrix = new int[classCount][classCount];
		int[][] matrixUnder = new int[classCount][classCount];
		int[][] matrixOver = new int[classCount][classCount];

		for (CSVRecord csvRecord : inputCsv) {
			final int actualClass = headers.get(csvRecord.get(1).trim()).intValue();
			final int detectedClass = headers.get(csvRecord.get(2).trim()).intValue();
			final double confidence = Double.parseDouble(csvRecord.get(3).trim());

			matrix[detectedClass][actualClass]++;
			if (confidence < threshold) {
				matrixUnder[detectedClass][actualClass]++;
			} else {
				matrixOver[detectedClass][actualClass]++;
			}

		}

		outputMatrix(outputFile, matrix);
		outputMatrix(outputUnder, matrixUnder);
		outputMatrix(outputOver, matrixOver);
	}

	private void outputMatrix(File outputFile, int[][] matrix) {
		PrintWriter out = IOUtil.getWriter(outputFile);
		for (int i = 0; i < classCount; i++) {
			out.print(", " + classNames.get(i));
		}
		out.println();

		for (int i = 0; i < matrix.length; i++) {
			out.print(classNames.get(i));
			for (int j = 0; j < matrix[i].length; j++) {
				out.print(", ");
				out.print(matrix[i][j]);
			}
			out.println();
		}

		IOUtil.close(out);
	}

	/**
	 * Updates global and class-specific statistics files
	 * 
	 * @param trainingSetSize
	 * @param iter
	 * @param reviewedItemsCount
	 * @param testSetSize
	 */
	protected void updateStats(File outputDir, File results, double threshold, Object iter, int reviewedItemsCount) {
		outputDir.getParentFile().mkdirs();
		CSVParser inputCsv = IOUtil.openCSV(results);

		String threshPercent = Long.toString(Math.round(threshold * 100));

		// Initialize empty structure to count tn, tp, fn, fp
		List<StatisticsCounter> counters = new ArrayList<StatisticsCounter>(classCount);
		List<StatisticsCounter> countersUnder = new ArrayList<StatisticsCounter>(classCount);
		List<StatisticsCounter> countersOver = new ArrayList<StatisticsCounter>(classCount);
		for (String className : classNames) {
			counters.add(new StatisticsCounter(className, "All"));
			countersUnder.add(new StatisticsCounter(className, "Under" + threshPercent));
			countersOver.add(new StatisticsCounter(className, "Over" + threshPercent));
		}

		// Iterate through all result records, and update the counters
		for (CSVRecord csvRecord : inputCsv) {
			final String actualClass = csvRecord.get(1).trim();
			final String detectedClass = csvRecord.get(2).trim();
			final double confidence = Double.parseDouble(csvRecord.get(3).trim());

			// Update each statistics object
			for (StatisticsCounter counter : counters) {
				counter.update(actualClass, detectedClass);
			}
			if (confidence < threshold) {
				for (StatisticsCounter counter : countersUnder) {
					counter.update(actualClass, detectedClass);
				}
			} else {
				for (StatisticsCounter counter : countersOver) {
					counter.update(actualClass, detectedClass);
				}
			}

		}
		IOUtil.close(inputCsv);

		// Calculate statistics and update the stats files
		computeResultsAndOutput(outputDir, iter, reviewedItemsCount, counters);
		computeResultsAndOutput(outputDir, iter, reviewedItemsCount, countersUnder);
		computeResultsAndOutput(outputDir, iter, reviewedItemsCount, countersOver);

		// Update global statistics file
		aggregateResultsAndOutput(outputDir, iter, reviewedItemsCount, counters);
		aggregateResultsAndOutput(outputDir, iter, reviewedItemsCount, countersUnder);
		aggregateResultsAndOutput(outputDir, iter, reviewedItemsCount, countersOver);
	}

	private void aggregateResultsAndOutput(File outputDir, Object iter, int reviewedItemsCount,
			List<StatisticsCounter> counters) {
		File file = new File(outputDir, getFileName("Stats"));
		PrintWriter out = IOUtil.getAppendingWriter(file);
		StatisticsResults stats = StatisticsResults.aggregate(counters);
		stats.output(out, iter, reviewedItemsCount, counters.get(0).getGroupName());
		IOUtil.close(out);
	}

	private void computeResultsAndOutput(File outputDir, Object iter, int reviewedItemsCount,
			List<StatisticsCounter> counters) {
		for (StatisticsCounter counter : counters) {
			File file = new File(outputDir, getFileName("Stats", capitalize(counter.getClassLabel())));
			PrintWriter out = IOUtil.getAppendingWriter(file);
			StatisticsResults stats = new StatisticsResults(counter);
			stats.output(out, iter, reviewedItemsCount, counter.getGroupName());
			IOUtil.close(out);
		}
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

	/**
	 * Updates Confidence Stats file
	 * 
	 * @param trainingSetSize
	 * @param iter
	 * @param reviewedItemsCount
	 * @param testSetSize
	 */
	protected void updateConfidenceStats(File outputFile, File outputDir, Object iter) {
		outputDir.getParentFile().mkdirs();

		// Fill an Apache Commons Math object with all confidence values for the first class
		DescriptiveStatistics stats = new DescriptiveStatistics();
		Frequency freq = new Frequency();
		CSVParser inputCsv = IOUtil.openCSV(outputFile);
		for (CSVRecord csvRecord : inputCsv) {
			double confidence = Double.parseDouble(csvRecord.get(3).trim());
			stats.addValue(confidence);
			freq.addValue(Double.valueOf(confidence));
		}
		IOUtil.close(inputCsv);

		// Update global statistics file
		File file = new File(outputDir, getFileName("Confidence"));
		PrintWriter out = IOUtil.getAppendingWriter(file);
		out.print(iter);
		out.print(",");
		out.print(stats.getN());
		out.print(",");
		out.print(stats.getMin());
		out.print(",");
		out.print(stats.getMax());
		out.print(",");
		out.print(stats.getMean());
		out.print(",");
		out.print(stats.getGeometricMean());
		out.print(",");
		out.print(stats.getPercentile(50));
		out.print(",");
		out.print(stats.getKurtosis());
		out.print(",");
		out.print(stats.getVariance());
		out.print(",");
		out.print(stats.getSkewness());
		out.print(",");
		out.print(stats.getStandardDeviation());
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(.1d)));
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(.2d)));
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(.3d)));
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(.4d)));
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(.5d)));
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(.6d)));
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(.7d)));
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(.8d)));
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(.9d)));
		out.print(",");
		out.print(freq.getCumFreq(Double.valueOf(1d)));
		out.println();
		
		IOUtil.close(out);
	}

	/**
	 * Empties the review statistics file, ensuring the files exist for appending
	 */
	protected void clearConfidenceStats(File outputDir) {
		File file = getEmptyFile(outputDir, "Confidence");
		PrintWriter out = IOUtil.getAppendingWriter(file);
		out.print("iter, ");
		out.print("count, ");
		out.print("min, ");
		out.print("max, ");
		out.print("mean, ");
		out.print("geometricMean, ");
		out.print("median, ");
		out.print("kurtosis, ");
		out.print("variance, ");
		out.print("skewness, ");
		out.print("sd, ");
		out.print("cf10, ");
		out.print("cf20, ");
		out.print("cf30, ");
		out.print("cf40, ");
		out.print("cf50, ");
		out.print("cf60, ");
		out.print("cf70, ");
		out.print("cf80, ");
		out.print("cf90");
		out.println();
		IOUtil.close(out);
	}

}
