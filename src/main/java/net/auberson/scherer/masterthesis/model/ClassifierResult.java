package net.auberson.scherer.masterthesis.model;

import java.util.Comparator;

import org.apache.commons.csv.CSVRecord;

/**
 * Represents a dataset element and the associated result returned by a
 * classifier: THe text, its class, and the class detected by the classifier
 *
 */
public class ClassifierResult extends Element{

	final String detectedClassLabel;
	final Double confidence;

	public ClassifierResult(CSVRecord csvRecord) {
		this(csvRecord.get(0), csvRecord.get(1), csvRecord.get(2), Double.parseDouble(csvRecord.get(3)));
	}

	public ClassifierResult(String text, String classLabel, String detectedClassLabel, Double confidence) {
		super(text, classLabel);
		this.detectedClassLabel = detectedClassLabel;
		this.confidence = confidence;
	}

	public String getDetectedClassLabel() {
		return detectedClassLabel;
	}

	public Double getConfidence() {
		return confidence;
	}

	@Override
	public String toString() {
		return "text=" + truncate(text, 80) + "... , classLabel=" + classLabel
				+ ", detectedClassLabel=" + detectedClassLabel + ", confidence=" + confidence;
	}

	public static final Comparator<ClassifierResult> COMPARATOR = new Comparator<ClassifierResult>() {
		public int compare(ClassifierResult o1, ClassifierResult o2) {
			return Double.compare(o1.confidence, o2.confidence);
		}
	};

}
