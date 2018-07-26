package net.auberson.scherer.masterthesis;

import java.io.File;
import java.util.List;

import net.auberson.scherer.masterthesis.model.ClassifierResult;
import net.auberson.scherer.masterthesis.util.BatchClassifier;
import net.auberson.scherer.masterthesis.util.Sampler;

/**
 * Executable for second experiment
 */
public class Experiment2 extends ExperimentBase implements Runnable {

	public static final File DATA_DIR = new File("./data/processed/experiment2");
	public static final File REPORTS_DIR = new File("./reports/experiment2");

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
		new Experiment2(args).run();
	}

	public Experiment2(String[] classes) {
		super(classes, Math.max(TRAINING_SET_SIZE, TEST_SET_SIZE));
	}

	public void run() {
		clearStats(REPORTS_DIR);
		clearReviewStats(REPORTS_DIR);
		
		// Initial Training
		// Remove the last class before training, train and test using all but the last
		// class
		System.out.println();
		System.out.println("[ Iteration 0: Initial Training ]");

		int classCountExceptLast = classCount - 1;
		List<String> classNamesExceptLast = classNames.subList(0, classCountExceptLast);

		File trainingSet = getEmptyFile(DATA_DIR, "Iteration", "0", "Training");
		System.out.println("Creating training set (missing a class) in " + trainingSet.getPath());

		File testSet = getEmptyFile(DATA_DIR, "Iteration", "0", "Test");
		System.out.println("Creating test set (missing a class) in " + testSet.getPath());

		Sampler.sample(new int[] { TRAINING_SET_SIZE, TEST_SET_SIZE }, classNamesExceptLast, sampleCount, trainingSet,
				testSet);

		System.out.println("Training Classifier with " + TRAINING_SET_SIZE + " samples per class");
		BatchClassifier classifier = trainClassifier(trainingSet, "Ex2", "Iteration0");

		File output = getEmptyFile(DATA_DIR, "Iteration0Output");
		System.out.println("Classifying test set into " + output.getPath());
		classifier.classify(testSet, output);

		File confMatrix = getEmptyFile(DATA_DIR, "Iteration0ConfusionMatrix");
		System.out.println("Calculating Confusion Matrix in " + confMatrix.getPath());
		outputConfMatrix(output, confMatrix);

		System.out.println("Updating statistics files");
		updateStats(output, REPORTS_DIR, 0, 0);

		// Initial iteration:
		// No retraining, test using all classes
		System.out.println();
		System.out.println("[ Iteration 0a: Test with all classes ]");

		testSet = getEmptyFile(DATA_DIR, "Iteration0aTest");
		System.out.println("Creating test set in " + testSet.getPath());
		Sampler.sample(TEST_SET_SIZE, classNames, sampleCount, testSet);

		output = getEmptyFile(DATA_DIR, "Iteration0aOutput");
		System.out.println("Classifying test set into " + output.getPath());
		classifier.classify(testSet, output);

		System.out.println("Deleting Classifier " + classifier.getName());
		classifier.delete();

		confMatrix = getEmptyFile(DATA_DIR, "Iteration0aConfusionMatrix");
		System.out.println("Calculating Confusion Matrix in " + confMatrix.getPath());
		outputConfMatrix(output, confMatrix);

		System.out.println("Updating statistics files");
		updateStats(output, REPORTS_DIR, "0a", 0);
		
		File reviewFile = null;
		
		// Retrain a number of times, using the classes with the least confidence
		for (int i = 1; i <= ITERATIONS; i++) {
			System.out.println();
			System.out.println("[ Iteration " + i + " ]");

			// This list simulates the entries that would have been manually reviewed:
			List<ClassifierResult> reviewedEntries = getSamplesUnderThreshold(output, CONFIDENCE_THRESHOLD);
			System.out.println(reviewedEntries.size() + " samples were reviewed this iteration.");

			reviewFile = getFileDuplicate(DATA_DIR, reviewFile, "Iteration", Integer.toString(i), "Review");
			System.out.println("Creating review file in " + reviewFile.getPath());
			outputClassifierResult(reviewedEntries, reviewFile);
			updateReviewStats(reviewFile, REPORTS_DIR, i);
			
			// Create training set missing last class
			trainingSet = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Training");
			System.out.println("Creating training set (missing a class) in " + trainingSet.getPath());
			Sampler.sample(TRAINING_SET_SIZE, classNamesExceptLast, sampleCount, trainingSet);

			// Create test set including last class
			testSet = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Test");
			System.out.println("Creating test set in " + testSet.getPath());
			Sampler.sample(TEST_SET_SIZE, classNames, sampleCount, testSet);

			// The only examples of the last class known for training are the ones discovered
			// through review:
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

		File confMatrix = getEmptyFile(DATA_DIR, "Iteration", iter.toString(), "ConfusionMatrix");
		System.out.println("Calculating Confusion Matrix in " + confMatrix.getPath());
		outputConfMatrix(output, confMatrix);

		System.out.println("Updating statistics files");
		updateStats(output, REPORTS_DIR, iter, reviewedItemsCount);

		System.out.println("Deleting Classifier " + classifier.getName());
		classifier.delete();

		return output;
	}

}
