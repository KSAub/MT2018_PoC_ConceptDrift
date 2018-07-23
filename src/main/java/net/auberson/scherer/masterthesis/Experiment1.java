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

	public static final File DATA_DIR = new File("./data/processed/experiment1");

	private static final int TRAINING_SET_SIZE = 10;//150;
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
		Sampler.sample(TRAINING_SET_SIZE, classNames, sampleCount, trainingSet);

		File output = trainAndClassify(trainingSet, 0);

		// This list simulates the entries that would have been manually reviewed:
		List<Element> reviewedEntries = new ArrayList<Element>();

		for (int i = 1; i <= ITERATIONS; i++) {
			System.out.println();
			System.out.println("[ Iteration " + i + " ]");

			List<ClassifierResult> newlyReviewedEntries = getSamplesUnderThreshold(output, CONFIDENCE_THRESHOLD);
			reviewedEntries.addAll(newlyReviewedEntries);
			System.out.println(newlyReviewedEntries.size() + " samples were reviewed this iteration.");
			System.out.println(
					"This brings the total to " + reviewedEntries + " samples that will be added to the training set");

			File reviewFile = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Review");
			System.out.println("Creating review file in " + trainingSet.getPath());
			outputSamples(reviewedEntries, reviewFile); // This is just for illustration purposes

			trainingSet = getEmptyFile(DATA_DIR, "Iteration", Integer.toString(i), "Training");
			System.out.println("Creating training set in " + trainingSet.getPath());
			outputSamples(reviewedEntries, trainingSet);
			int remainingSamples = TRAINING_SET_SIZE + Math.floorDiv(-reviewedEntries.size(), classCount);
			Sampler.sample(remainingSamples, classNames, sampleCount, trainingSet);

			output = trainAndClassify(trainingSet, i);
		}

	}

	private File trainAndClassify(File input, Integer iter) {
		System.out.println("Training Classifier with " + TRAINING_SET_SIZE + " samples");
		BatchClassifier classifier = trainClassifier(input, "Ex1", "Iteration" + iter.toString());

		File testSet = getEmptyFile(DATA_DIR, "Iteration", iter.toString(), "Test");
		System.out.println("Creating test set in " + testSet.getPath());
		Sampler.sample(TEST_SET_SIZE, classNames, sampleCount, testSet);

		File output = getEmptyFile(DATA_DIR, "Iteration", iter.toString(), "Output");
		System.out.println("Classifying test set into " + output.getPath());
		classifier.classify(testSet, output);

		File confMatrix = getEmptyFile(DATA_DIR, "Iteration", iter.toString(), "ConfusionMatrix");
		System.out.println("Calculating Confusion Matrix in " + confMatrix.getPath());
		outputConfMatrix(output, confMatrix);

		System.out.println("Deleting Classifier " + classifier.getName());
		classifier.delete();

		return output;
	}

}
