package net.auberson.scherer.masterthesis.model;

public class Statistics {

	// True Positive
	final int tp;
	// False Positive
	final int fp;
	// False Negative
	final int fn;
	// True Negative
	final int tn;

	// Recall
	final double tpr;
	// Precision
	final double ppv;
	// True Negative Rate
	final double tnr;
	// Informedness
	final double bm;
	// Accuracy
	final double acc;
	// Error Rate
	final double err;
	// F1 Score
	final double f1;

	public Statistics(int tp, int fp, int fn, int tn) {
		super();
		this.tp = tp;
		this.fp = fp;
		this.fn = fn;
		this.tn = tn;

		double TP = tp;
		double FP = fp;
		double FN = fn;
		double TN = tn;

		this.tpr = TP / (TP + FN);
		this.ppv = TP / (TP + FP);
		this.tnr = TN / (TN + FP);
		this.bm = tpr + tnr - 1;
		this.acc = (TP + TN) / (TP + TN + FP + FN);
		this.err = (FP + FN) / (TP + TN + FP + FN);
		this.f1 = 2 * ((ppv * tpr) / (ppv + tpr));
	}

	@Override
	public String toString() {
		return "Statistics [tp=" + tp + ", fp=" + fp + ", fn=" + fn + ", tn=" + tn + ", tpr=" + tpr + ", ppv=" + ppv
				+ ", tnr=" + tnr + ", bm=" + bm + ", acc=" + acc + ", err=" + err + ", f1=" + f1 + "]";
	}

	
}
