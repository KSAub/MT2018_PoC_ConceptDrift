package net.auberson.scherer.masterthesis.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * This utility helps in creating test sets and training sets
 *
 */
public class Sampler {
	private static Random rng = new Random();

	/**
	 * Creates a file with data sampled from the intermediary data sets
	 * 
	 * @param sampleSize
	 *            the size of the data set file
	 * @param classNames
	 *            the names of the classes from which to get samples. The
	 *            corresponding CSVs must exist under
	 *            <code>./data/intermediate</code>
	 * @param classSampleCounts
	 *            a map containing the number of samples available for each class
	 *            specified (you can get this using
	 *            <code>Sampler.getSampleCount(classNames);</code>)
	 * @param targets
	 *            the file in which to write the samples
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void sample(int sampleSize, Collection<String> classNames, Map<String, Integer> classSampleCounts,
			File target) {
		sample(new int[] {sampleSize}, classNames, classSampleCounts, target);
	}
	
	/**
	 * Creates one or more files with data sampled from the intermediary data sets
	 * 
	 * @param sampleSizes
	 *            the sizes of each data set file
	 * @param classNames
	 *            the names of the classes from which to get samples. The
	 *            corresponding CSVs must exist under
	 *            <code>./data/intermediate</code>
	 * @param classSampleCounts
	 *            a map containing the number of samples available for each class
	 *            specified (you can get this using
	 *            <code>Sampler.getSampleCount(classNames);</code>)
	 * @param targets
	 *            the file or files in which to write the samples
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void sample(int[] sampleSizes, Collection<String> classNames, Map<String, Integer> classSampleCounts,
			File... targets) {
		int targetCount = targets.length;

		// Open output files
		try {
			PrintWriter out[] = new PrintWriter[targets.length];
			for (int i = 0; i < targets.length; i++) {
				out[i] = new PrintWriter(targets[i]);
			}

			int totalSampleCount = 0;
			for (int sampleSize : sampleSizes) {
				totalSampleCount += sampleSize;
			}
			
			for (String className : classNames) {
				int classSampleCount = classSampleCounts.get(className);
				if (classSampleCount < totalSampleCount) {
					throw new IllegalArgumentException("Only " + classSampleCount + " exist for class '" + className
							+ "', not enough to pick " + totalSampleCount + " samples.");
				}
				List<Integer> sampledLines = sampleRandomNumbersWithoutRepetition(0, classSampleCount - 1,
						totalSampleCount);

				// Open input file
				FileInputStream in = new FileInputStream(Project.getDataFile(className));
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

				int lineNumber = 0;
				int lineCount = 0;
				while (!sampledLines.isEmpty()) {
					while (sampledLines.get(0).intValue() > lineNumber++) {
						reader.readLine();
					}
					sampledLines.remove(0);
					String readLine = reader.readLine();
					assert (!readLine.isEmpty());
					out[lineCount++ % targetCount].println(readLine);
				}
				// Close the input file
				reader.close();
				in.close();
			}

			// Close the output files
			for (int i = 0; i < targets.length; i++) {
				out[i].close();
			}
		} catch (IOException e) {
			System.err.println("A disk error occured trying to generate the data set file.");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Find out what the sample count of the class with the smallest sample count
	 * is. We will use the same sample count for all classes, so this is the maximal
	 * number of samples we can use per class.
	 * 
	 * @param classes
	 * @return
	 */
	public static Map<String, Integer> getSampleCount(Collection<String> classes) {
		HashSet<String> missingClasses = new HashSet<String>(classes);
		HashMap<String, Integer> sampleCount = new HashMap<String, Integer>();

		try {
			CSVParser entries = CSVFormat.DEFAULT.parse(new FileReader(Project.DATAFILE_COUNT));
			for (CSVRecord entry : entries) {
				for (String nameOfClass : classes) {
					if (entry.get(0).trim().equals(nameOfClass.trim())) {
						missingClasses.remove(nameOfClass);
						int size = Integer.valueOf(entry.get(1).trim());
						sampleCount.put(nameOfClass, size);
						System.out.println(size + " samples in dataset for class '" + nameOfClass + "'");
					}
				}
			}
			entries.close();

			if (!missingClasses.isEmpty()) {
				System.err.println("Dataset not found for class: " + missingClasses.toString());
				System.exit(-1);
			}

		} catch (FileNotFoundException e) {
			System.err.println(
					Project.DATAFILE_COUNT.getName() + " missing. Please run the Extract step first to fix this.");
			System.exit(-1);
		} catch (IOException e) {
			System.err.println(Project.DATAFILE_COUNT.getName()
					+ " could not be read. Please re-run the Extract step first to fix this.");
			System.exit(-1);
		}

		return sampleCount;
	}

	/**
	 * Returns Non-repeating random numbers in the range provided (taken from
	 * https://stackoverflow.com/questions/16000196/java-generating-non-repeating-random-numbers)
	 */
	private static List<Integer> sampleRandomNumbersWithoutRepetition(int start, int end, int count) {
		List<Integer> result = new ArrayList<Integer>(count);
		int remaining = end - start;
		for (int i = start; i < end && count > 0; i++) {
			double probability = rng.nextDouble();
			if (probability < ((double) count) / (double) remaining) {
				count--;
				result.add(Integer.valueOf(i));
			}
			remaining--;
		}
		return result;
	}
}
