package net.auberson.scherer.masterthesis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.auberson.scherer.masterthesis.model.ClassifierResult;
import net.auberson.scherer.masterthesis.model.Element;
import net.auberson.scherer.masterthesis.util.BatchClassifier;
import net.auberson.scherer.masterthesis.util.Sampler;

/**
 * Executable for first experiment
 */
public class Experiment1 extends ExperimentBase implements Runnable {

	public static final File DATA_DIR = new File("./data/processed/ex1");

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

	public void run() {
		System.out.println();
		System.out.println("[ Initial Iteration ]");

		File trainingSet = getEmptyFile(DATA_DIR, "Iteration", "0", "Training");
		System.out.println("Creating training set in " + trainingSet.getPath());

		File testSet = getEmptyFile(DATA_DIR, "Iteration", "0", "Test");
		System.out.println("Creating test set in " + testSet.getPath());

		Sampler.sample(new int[] { TRAINING_SET_SIZE, TEST_SET_SIZE }, classNames, sampleCount, trainingSet, testSet);
		File output = trainAndClassify(trainingSet, testSet, TRAINING_SET_SIZE, 0);

		// This list simulates the entries that would have been manually reviewed:
		List<Element> reviewedEntries = new ArrayList<Element>();

		for (int i = 1; i <= ITERATIONS; i++) {
			System.out.println();
			System.out.println("[ Iteration " + i + " ]");

			List<ClassifierResult> newlyReviewedEntries = getSamplesUnderThreshold(output, CONFIDENCE_THRESHOLD);
			reviewedEntries.addAll(newlyReviewedEntries);
			System.out.println(newlyReviewedEntries.size() + " samples were reviewed this iteration.");
			System.out.println("This brings the total to " + reviewedEntries.size()
					+ " samples that will be added to the training set");

			File reviewFile = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Review");
			System.out.println("Creating review file in " + reviewFile.getPath());
			outputSamples(reviewedEntries, reviewFile); // This is just for illustration purposes

			trainingSet = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Training");
			System.out.println("Creating training set in " + trainingSet.getPath());
			outputSamples(reviewedEntries, trainingSet);
			int missingTrainingSetSample = TRAINING_SET_SIZE + Math.floorDiv(-reviewedEntries.size(), classCount);
			missingTrainingSetSample = Math.max(missingTrainingSetSample, 0);

			testSet = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Test");
			System.out.println("Creating test set in " + testSet.getPath());

			Sampler.sample(new int[] { missingTrainingSetSample, TEST_SET_SIZE }, classNames, sampleCount, trainingSet,
					testSet);
			output = trainAndClassify(trainingSet, testSet, reviewedEntries.size() + missingTrainingSetSample, i);
		}

	}

	/**
	 * Trains a classifier, evaluates the test set, updates statistics files, and
	 * returns the files containing the results from the test set evaluation.
	 */
	private File trainAndClassify(File trainingSet, File testSet, int trainingSetSize, Integer iter) {
		System.out.println("Training Classifier with " + trainingSetSize + " samples");
		BatchClassifier classifier = trainClassifier(trainingSet, "Ex1", "Iteration" + iter.toString());

		File output = getEmptyFile(DATA_DIR, "Iteration", iter.toString(), "Output");
		System.out.println("Classifying test set into " + output.getPath());
		classifier.classify(testSet, output);

		File confMatrix = getEmptyFile(DATA_DIR, "Iteration", iter.toString(), "ConfusionMatrix");
		System.out.println("Calculating Confusion Matrix in " + confMatrix.getPath());
		outputConfMatrix(output, confMatrix);

		System.out.println("Updating statistics files");
		updateStats(output);
		
		System.out.println("Deleting Classifier " + classifier.getName());
		classifier.delete();

		return output;
	}

}
