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

import net.auberson.scherer.masterthesis.model.Result;
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

		System.out.println("Deleting Classifier " + classifier.getName());
		classifier.delete();

		return output;
	}

}
