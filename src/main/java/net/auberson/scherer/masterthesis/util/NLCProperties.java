package net.auberson.scherer.masterthesis.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class NLCProperties extends Properties {

	private static final long serialVersionUID = 1L;
	private static final String NLC_PROPERTIES_FILENAME = "nlc.properties";

	public NLCProperties() {
		InputStream in = getClass().getClassLoader().getResourceAsStream(NLC_PROPERTIES_FILENAME);

		if (in == null) {
			throw new IllegalArgumentException("NLC Properties file not found: " + NLC_PROPERTIES_FILENAME);
		}

		try {
			load(in);
		} catch (IOException e) {
			throw new IllegalArgumentException("NLC Properties file could not be read: " + NLC_PROPERTIES_FILENAME, e);
		}
	}

	public String getUsername() {
		return getProperty("username");
	}

	public String getPassword() {
		return getProperty("password");
	}
}
