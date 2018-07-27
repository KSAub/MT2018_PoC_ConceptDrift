package net.auberson.scherer.masterthesis;

import java.io.File;
import java.util.List;

import net.auberson.scherer.masterthesis.model.ClassifierResult;
import net.auberson.scherer.masterthesis.util.BatchClassifier;
import net.auberson.scherer.masterthesis.util.IOUtil;
import net.auberson.scherer.masterthesis.util.Sampler;

/**
 * Executable for first experiment. This looks at whether selecting the test set
 * results with the lowest confidence, and adding them to the training set,
 * improves classification quality.
 */
public class Experiment1 extends ExperimentBase implements Runnable {

	public static final File DATA_DIR = new File("./data/processed/experiment1");
	public static final File REPORTS_DIR = new File("./reports/experiment1");

	private static final int TRAINING_SET_SIZE = 150;
	private static final int TEST_SET_SIZE = 600;
	private static final int ITERATIONS = 10;
	private static final double CONFIDENCE_THRESHOLD = 0.8d;

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
		super(classes, Math.max(TRAINING_SET_SIZE, TEST_SET_SIZE));
	}
	//
	// public void test() {
	// File reviewFile = new File(
	// "./data/processed/experiment1/Iteration1Review-electronics-gaming-security-travel-cooking.csv");
	// File trainingSet = new File(
	// "./data/processed/experiment1/Iteration1Training-electronics-gaming-security-travel-cooking.csv");
	//
	// File trainingSetMerged = getEmptyFile(DATA_DIR, "Iteration", "10",
	// "TrainingMerged");
	// mergeDataset(trainingSetMerged, TRAINING_SET_SIZE, reviewFile, trainingSet);
	// }

	public void run() {
		System.out.println();
		System.out.println("[ Initial Iteration ]");
		clearStats(REPORTS_DIR);
		clearReviewStats(REPORTS_DIR);

		File trainingSet = getEmptyFile(DATA_DIR, "Iteration", "0", "Training");
		System.out.println("Creating training set in " + trainingSet.getPath());

		File testSet = getEmptyFile(DATA_DIR, "Iteration", "0", "Test");
		System.out.println("Creating test set in " + testSet.getPath());

		Sampler.sample(new int[] { TRAINING_SET_SIZE, TEST_SET_SIZE }, classNames, sampleCount, trainingSet, testSet);

		File output = trainAndClassify(trainingSet, testSet, 0, 0);

		File previousReviewFile = null;

		for (int i = 1; i <= ITERATIONS; i++) {
			System.out.println();
			System.out.println("[ Iteration " + i + " ]");

			// This list simulates the entries that would have been manually reviewed:
			List<ClassifierResult> reviewedEntries = getSamplesUnderThreshold(output, CONFIDENCE_THRESHOLD);
			System.out.println(reviewedEntries.size() + " samples were reviewed this iteration.");

			File reviewFile = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Review");
			System.out.println("Creating review file in " + reviewFile.getPath());
			outputClassifierResult(reviewedEntries, reviewFile);
			IOUtil.copyFile(previousReviewFile, reviewFile, true);
			previousReviewFile = reviewFile;
			
			updateReviewStats(reviewFile, REPORTS_DIR, i);

			trainingSet = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Training");
			System.out.println("Creating training set in " + trainingSet.getPath());

			testSet = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Test");
			System.out.println("Creating test set in " + testSet.getPath());

			Sampler.sample(new int[] { TRAINING_SET_SIZE, TEST_SET_SIZE }, classNames, sampleCount, trainingSet,
					testSet);

			File trainingSetMerged = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "TrainingMerged");
			System.out.println("Merging Review file and training set in " + trainingSet.getPath());
			mergeDataset(trainingSetMerged, TRAINING_SET_SIZE, reviewFile, trainingSet);

			output = trainAndClassify(trainingSetMerged, testSet, reviewedEntries.size(), i);
		}

	}

	/**
	 * Trains a classifier, evaluates the test set, updates statistics files, and
	 * returns the files containing the results from the test set evaluation.
	 */
	private File trainAndClassify(File trainingSet, File testSet, int reviewedItemsCount, Integer iter) {
		System.out.println("Training Classifier with " + TRAINING_SET_SIZE + " samples per class");
		BatchClassifier classifier = trainClassifier(trainingSet, "Ex1", "Iteration" + iter.toString());

		File output = getEmptyFile(DATA_DIR, "Iteration", iter.toString(), "Output");
		System.out.println("Classifying test set into " + output.getPath());
		classifier.classify(testSet, output);

		System.out.println("Calculating Confusion Matrices");
		outputConfMatrix(DATA_DIR, output, CONFIDENCE_THRESHOLD, iter);

		System.out.println("Updating statistics files");
		updateStats(REPORTS_DIR, output, CONFIDENCE_THRESHOLD, iter, reviewedItemsCount);

		System.out.println("Deleting Classifier " + classifier.getName());
		classifier.delete();

		return output;
	}

}
