package net.auberson.scherer.masterthesis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;

import net.auberson.scherer.masterthesis.util.BatchClassifier;
import net.auberson.scherer.masterthesis.util.NLCProperties;
import net.auberson.scherer.masterthesis.util.Project;
import net.auberson.scherer.masterthesis.util.Sampler;

/**
 * 'Compute Learning Curve' executable: Computes a learning curve, generates a
 * chart. <br>
 * Expects dataset CSVs in ./data/intermediate (one CSV per class, and a CSV
 * containing the dataset sizes).
 */
public class ComputeLearningCurve {

	private static final int TEST_SET_SIZE = 200;
	public static final File LEARNING_CURVE_REPORTS_DIR = new File("./reports/learning-curve");
	
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Please specify the classes for which to generate the learning curve.");
			System.exit(-1);
		}

		NLCProperties nlcProps = new NLCProperties();
		NaturalLanguageClassifier service = new NaturalLanguageClassifier();
		service.setUsernameAndPassword(nlcProps.getUsername(), nlcProps.getPassword());

		int classCount = args.length;
		Collection<String> classNames = Arrays.asList(args);
		Map<String, Integer> sampleCount = Sampler.getSampleCount(classNames);
		int minSampleCount = getMin(sampleCount);

		if (minSampleCount * classCount > Project.MAX_SAMPLES_PER_TRAINING) {
			// Respect the Watson NLC's limit:
			minSampleCount = Project.MAX_SAMPLES_PER_TRAINING / classCount;
		}
		int stepSize = 5;//computeStepSize(minSampleCount); TODO

		for (int sampleSize = stepSize; sampleSize <= minSampleCount; sampleSize = sampleSize + stepSize) {
			System.out.println("\nCalculating accuracy using sample size " + sampleSize);

			File trainingSet = File.createTempFile("trainingset" + sampleSize + "-", ".csv");
			File testSet = File.createTempFile("testset" + sampleSize + "-", ".csv");
			File results = new File(LEARNING_CURVE_REPORTS_DIR, getResultsFileName(classNames));
			System.out.println("Temporary directory is " + trainingSet.getParent());
			System.out.println("Results will be appended to " + results.getPath());

			Sampler.sample(sampleSize, classNames, sampleCount, trainingSet);
			Sampler.sample(TEST_SET_SIZE, classNames, sampleCount, testSet);

			System.out.println("Training Classifier for Sample Size " + sampleSize);
			BatchClassifier classifier = new BatchClassifier(service, "LearningCurveTestClassifier", "en", trainingSet);
			System.out.println("Trained " + classifier);

			try {
				System.out.println("Processing training set");
				classifier.evaluateAccuracy(trainingSet, sampleSize * classCount, "training", results);
				System.out.println("Processing test set");
				classifier.evaluateAccuracy(testSet, sampleSize * classCount, "validation", results);
			} finally {
				classifier.delete();
				System.out.println("Deleted Classifier " + classifier);
			}

			trainingSet.delete();
			testSet.delete();
		}
	}

	private static String getResultsFileName(Collection<String> classNames) {
		StringBuilder builder = new StringBuilder("learning-curve");
		for (String className : classNames) {
			builder.append('-').append(className);
		}
		return builder.append(".csv").toString();
	}

	/**
	 * @return return the smallest count from the map passed
	 */
	private static int getMin(Map<String, Integer> sampleCount) {
		int minimum = Integer.MAX_VALUE;
		for (Integer count : sampleCount.values()) {
			minimum = Integer.min(minimum, count.intValue());
		}
		return minimum;
	}

	/**
	 * Compute a reasonable step size for the measurements, i.e. one that is a round
	 * number and yields a number of measurements that is manageable.
	 */
	private static int computeStepSize(int minSampleCount) {
		return ((int) (minSampleCount / 1300)) * 100;
	}

}
