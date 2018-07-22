package net.auberson.scherer.masterthesis;

import java.io.File;

import net.auberson.scherer.masterthesis.util.BatchClassifier;
import net.auberson.scherer.masterthesis.util.Sampler;

/**
 * Executable for first experiment
 */
public class Experiment1 extends ExperimentBase implements Runnable {

	public static final File DATA_DIR = new File("./data/processed/ex1");

	private static final int DATA_SET_SIZE = 25; //150;
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
		File trainingSet = getEmptyFile(DATA_DIR, "Ex1", "I0", "Training");
		System.out.println("Creating training set in " + trainingSet.getPath());
		Sampler.sample(DATA_SET_SIZE, classNames, sampleCount, trainingSet);

		File testSet = getEmptyFile(DATA_DIR, "Ex1", "I0", "Test");
		System.out.println("Creating test set in " + testSet.getPath());
		Sampler.sample(DATA_SET_SIZE, classNames, sampleCount, trainingSet);

		System.out.println("Training Classifier with " + DATA_SET_SIZE + " samples");
		BatchClassifier classifier = trainClassifier(trainingSet, "Ex1", "I0");

		File output = getEmptyFile(DATA_DIR, "Ex1", "I0", "Output");
		System.out.println("Classifying test set into " + output.getPath());
		classifier.classify(testSet, output);

		System.out.println("Deleting Classifier");
		classifier.delete();
	}

}
