package net.auberson.scherer.masterthesis.model;

public class StatisticsCounter {

	final String classLabel;
	final String group;
		
	// True Positive
	int tp = 0;
	// False Positive
	int fp = 0;
	// False Negative
	int fn = 0;
	// True Negative
	int tn = 0;

	public StatisticsCounter(String classLabel, String group) {
		this.classLabel = classLabel;
		this.group = group;
	}

	public String getClassLabel() {
		return classLabel;
	}

	public String getGroupName() {
		return group;
	}

	public void update(String actualClass, String detectedClass) {
		if (classLabel.equals(detectedClass)) {
			if (classLabel.equals(actualClass)) {
				tp++;
			} else {
				fp++;
			}
		} else {
			if (classLabel.equals(actualClass)) {
				fn++;
			} else {
				tn++;
			}
		}
	}

}