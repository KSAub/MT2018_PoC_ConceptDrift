package net.auberson.scherer.masterthesis.model;

public class StatisticsCounter {

	final String classLabel;

	// True Positive
	int tp = 0;
	// False Positive
	int fp = 0;
	// False Negative
	int fn = 0;
	// True Negative
	int tn = 0;

	public StatisticsCounter(String classLabel) {
		this.classLabel = classLabel;
	}

	public String getClassLabel() {
		return classLabel;
	}

	public void update(String actualClass, String detectedClass) {
		if (actualClass.equals(detectedClass)) {
			if (classLabel.equals(detectedClass)) {
				tp++;
			} else {
				fp++;
			}
		} else {
			if (classLabel.equals(detectedClass)) {
				tn++;
			} else {
				fn++;
			}
		}
	}

}