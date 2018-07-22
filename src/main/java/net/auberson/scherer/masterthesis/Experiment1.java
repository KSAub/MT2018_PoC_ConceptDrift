package net.auberson.scherer.masterthesis;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import net.auberson.scherer.masterthesis.util.BatchClassifier;
import net.auberson.scherer.masterthesis.util.Sampler;

/**
 * Executable for first experiment
 */
public class Experiment1 extends ExperimentBase implements Runnable {

	public static final File DATA_DIR = new File("./data/processed/ex1");

	private static final int DATA_SET_SIZE = 150;
	private static final int ITERATIONS = 10;

	/**
	 * Program executable for Experiment 1
	 * 
	 * @param args
	 *            the name of the classes to use for experiment 1, as arguments
	 */
	public static void main(String[] args) {
		new Experiment1(args).run();
	}

	public Experiment1(String[] classes) {
		super(classes, DATA_SET_SIZE);
	}

	public void run() {
		System.out.println();
		System.out.println("[ Initial Iteration ]");
		File trainingSet = getEmptyFile(DATA_DIR, "Iteration", "0", "Training");
		System.out.println("Creating training set in " + trainingSet.getPath());
		Sampler.sample(DATA_SET_SIZE, classNames, sampleCount, trainingSet);
		File output = trainAndClassify(trainingSet, 0);
	}

	private File trainAndClassify(File input, Integer iter) {
		System.out.println("Training Classifier with " + DATA_SET_SIZE + " samples");
		BatchClassifier classifier = trainClassifier(input, "Ex1", "Iteration" + iter.toString());

		File testSet = getEmptyFile(DATA_DIR, "Iteration", iter.toString(), "Test");
		System.out.println("Creating test set in " + testSet.getPath());
		Sampler.sample(DATA_SET_SIZE, classNames, sampleCount, testSet);

		File output = getEmptyFile(DATA_DIR, "Iteration", iter.toString(), "Output");
		System.out.println("Classifying test set into " + output.getPath());
		classifier.classify(testSet, output);

		System.out.println("Deleting Classifier " + classifier.getName());
		classifier.delete();

		return output;
	}

	static class Result {
		final String text;
		final String expectedClass;
		final String detectedClass;
		final Double confidence;

		public Result(CSVRecord csvRecord) {
			this(csvRecord.get(0), csvRecord.get(1), csvRecord.get(2), Double.parseDouble(csvRecord.get(3)));
		}

		public Result(String text, String expectedClass, String detectedClass, Double confidence) {
			this.text = text;
			this.expectedClass = expectedClass;
			this.detectedClass = detectedClass;
			this.confidence = confidence;
		}
	}

	static class ResultComparator implements Comparator<Result> {
		public int compare(Result o1, Result o2) {
			return Double.compare(o1.confidence, o2.confidence);
		}
	}

	private void getBottomN(File results, int n) {
		CSVParser inputCsv = null;
		try {
			inputCsv = CSVFormat.DEFAULT.parse(new FileReader(results));
		} catch (IOException e) {
			System.err.println("Unable to parse the result CSV at '" + results.getAbsolutePath() + "'");
			e.printStackTrace();
			System.exit(-1);
		}

		List<Result> resultList = new ArrayList<Result>();
		for (CSVRecord csvRecord : inputCsv) {
			resultList.add(new Result(csvRecord));
		}
		resultList.sort(new ResultComparator());

	}

}
