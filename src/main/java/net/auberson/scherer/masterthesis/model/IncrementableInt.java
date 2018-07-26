package net.auberson.scherer.masterthesis.model;

/**
 * A simple mutable int object that can be incremented. Useful for counters.
 *
 */
public class IncrementableInt {

	private int value;

	public IncrementableInt() {
		super();
		this.value = 0;
	}

	public IncrementableInt(int value) {
		super();
		this.value = value;
	}

	public int get() {
		return value;
	}

	public void set(int value) {
		this.value = value;
	}

	public int inc() {
		return ++value;
	}

	public int dec() {
		return --value;
	}

	public boolean lessThan(int otherValue) {
		return value < otherValue;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
