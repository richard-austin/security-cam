package org.onvif.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetTestDevice {

	static String PROPERTY_NAME = "ONVIF_HOST";
	private static final Logger LOG = LoggerFactory.getLogger(GetTestDevice.class);

	// Get a camera host, user name, and password for tests.
	// Add an environment variable or java Property called "TEST_CAM" and set to
	// host,user,password,profile
	// or modify resource/onvif.properties

	public static OnvifCredentials getOnvifCredentials(String[] args) {

		OnvifCredentials creds = getFromArgs(args);
		if (creds != null)
			return creds;

		creds = getFromProperties();
		if (creds != null)
			return creds;
		try {
			creds = getFirstFromResource("/onvif.properties");
			if (creds != null)
				return creds;
		} catch (IOException ioe) {
			LOG.error("Error", ioe);
		}
		try {
			creds = getFromStandardInput();
			if (creds != null)
				return creds;
		} catch (IOException ioe) {
			LOG.error("Error", ioe);
		}

		LOG.info("Unable to get default test onvif credentials");
		return new OnvifCredentials("", "", "", "");
	}

	// arguments to any test app can be host user password profilename
	// if no arguments passed, returns null.
	// All arguments optional.
	public static OnvifCredentials getFromArgs(String[] args) {
		if (args == null || args.length == 0)
			return null;
		String host = "", user = "", password = "", profile = "";
		host = args[0];
		if (args.length > 1)
			user = args[1];
		if (args.length > 2)
			password = args[2];
		if (args.length > 3)
			profile = args[3];
		return new OnvifCredentials(host, user, password, profile);
	}

	public static OnvifCredentials getFromProperties() {
		String test = null;
		if (test == null)
			test = System.getProperty(PROPERTY_NAME);
		if (test == null)
			test = System.getenv(PROPERTY_NAME);

		if (test != null) {
			return parse(test);
		}
		return null;
	}

	private static OnvifCredentials getFromStandardInput() throws IOException {

		System.out.println("Getting camera credentials from standard input");
		InputStreamReader inputStream = new InputStreamReader(System.in, StandardCharsets.UTF_8);
		BufferedReader keyboardInput = new BufferedReader(inputStream);
		System.out.println("Please enter camera IP (with port if not 80):");
		String cameraAddress = keyboardInput.readLine();
		System.out.println("Please enter camera username:");
		String user = keyboardInput.readLine();
		System.out.println("Please enter camera password:");
		String password = keyboardInput.readLine();
		System.out.println("Please enter camera profile [or enter to use first]:");
		String profile = keyboardInput.readLine();
		OnvifCredentials creds = new OnvifCredentials(cameraAddress, user, password, profile);
		return creds;
	}

	private static OnvifCredentials getFirstFromResource(String resource) throws IOException {
		InputStream res = GetTestDevice.class.getResourceAsStream(resource);
		if (res != null) {
			try (Scanner s = new Scanner(res, StandardCharsets.UTF_8.name());) {
				s.useDelimiter("\\A");
				while (s.hasNextLine()) {
					String line = s.nextLine();
					if (!line.isEmpty() && !line.startsWith("#"))
						return parse(line.substring(line.indexOf("=") + 1));
				}
			}
		}
		return null;
	}

	// warning, this breaks if password contains a comma.
	public static OnvifCredentials parse(String i) {
		String host = "", user = "", password = "", profile = "";
		if (i != null) {
			if (i.contains(",")) {
				String[] sp = i.split(",");
				if (sp.length > 0)
					host = sp[0];
				if (sp.length > 1)
					user = sp[1];
				if (sp.length > 2)
					password = sp[2];
				if (sp.length > 3)
					profile = sp[3];

			} else
				host = i;
		}
		return new OnvifCredentials(host, user, password, profile);
	}
}
