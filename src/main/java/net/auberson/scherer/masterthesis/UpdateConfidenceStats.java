package net.auberson.scherer.masterthesis;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executable that manually updates the statistics without re-running all
 * experiments.
 */
public class UpdateConfidenceStats extends ExperimentBase implements FileFilter, Comparator<File> {

	private static final Pattern ITER = Pattern.compile("\\d+a?");

	/**
	 * Program executable for Experiment 1
	 * 
	 * @param args
	 *            the name of the classes to use for experiment 1, as arguments
	 */
	public static void main(String[] args) {
		UpdateConfidenceStats updateStats = new UpdateConfidenceStats();
		updateStats.run(new File("./data/processed/experiment1"), new File("./reports/experiment1"));
		updateStats.run(new File("./data/processed/experiment2"), new File("./reports/experiment2"));
	}

	public UpdateConfidenceStats() {
		super();
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

	public void run(File dataDir, File reportsDir) {
		System.out.println();
		System.out.println("Data Directory " + dataDir.getAbsolutePath());
		clearConfidenceStats(reportsDir);

		File[] listOfFiles = dataDir.listFiles(this);
		Arrays.sort(listOfFiles, this);

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println("File " + listOfFiles[i].getName());
				Matcher m = ITER.matcher(listOfFiles[i].getName());
				m.find();
				updateConfidenceStats(listOfFiles[i], reportsDir, m.group(0));
			}
		}
	}

	@Override
	public boolean accept(File pathname) {
		return pathname.getName().matches("Iteration.+Output-.*\\.csv");
	}

	@Override
	public int compare(File o1, File o2) {
		final String pat = "Output-.*\\.csv";
		final String s1 = o1.getName().replaceAll(pat, "");
		final String s2 = o2.getName().replaceAll(pat, "");
		if (s1.endsWith("a")) {
			return s2.endsWith("0") ? 1 : -1;
		}
		if (s2.endsWith("a")) {
			return s1.endsWith("0") ? -1 : 1;
		}
		if (s1.length() == s2.length()) {
			return s1.compareTo(s2);
		}
		return s1.length() - s2.length();
	}

}
