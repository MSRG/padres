package ca.utoronto.msrg.padres.demo.webclient.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import simple.http.Request;
import simple.http.Response;
import simple.http.load.Service;
import simple.http.serve.Context;
import ca.utoronto.msrg.padres.demo.webclient.client.WebClientException;

/*
 * Will look for a Demo client to run
 */
public class ClientService extends Service {

	public ClientService(Context context) {
		super(context);
	}

	private static final String PROP_CLASS_NAME = "class_name";

	private static final String PROP_METHOD_NAME = "method_name";

	public void process(Request req, Response resp) throws Exception {
		PrintStream out = resp.getPrintStream();

		System.out.println("\n*** ClientService");
		System.out.println("reqpath: " + req.getPath());
		System.out.println("req: " + req);

		// get request content (expect an xml string)
		int len = req.getContentLength();
		InputStream in = req.getInputStream();
		byte[] bytes = new byte[len];
		for (int bb = 0; bb < len; bb++)
			bytes[bb] = (byte) in.read();

		try {
			// fill properties
			Properties props = new Properties();
			loadProperties(props, new String(bytes));
			Object[] invokeArgs = { props };

			// Methods are expected to accept a Properties object as input and
			// return a Properties object This makes is easier to pass arbitrary
			// parameters back and forth as String pairs
			String className = props.getProperty(PROP_CLASS_NAME);
			String methodName = props.getProperty(PROP_METHOD_NAME);
			Class<?>[] methodArgs = { Properties.class };

			// just for debugging
			props.list(System.out);

			Properties respProps = null;
			try {
				Object obj = Class.forName(className).newInstance();
				respProps = (Properties) Class.forName(className).getMethod(methodName, methodArgs).invoke(
						obj, invokeArgs);
			} catch (Exception ex) {
				if (ex.getCause() instanceof WebClientException) {
					// Give a nice message to be returned as an alert
					System.out.println("!! Clean error: " + ex.getCause().getMessage());
					resp.setText(ex.getCause().getMessage());
				} else {
					// Some other exception has occurred Will cause an
					// "Internal Server Error" alert in UI
					throw ex;
				}
			}

			// populate valid response
			if (respProps != null) {
				System.out.println(propsToXml(respProps));
				out.println(propsToXml(respProps));

				resp.set("content-type", "text/xml");
			} else {
				resp.set("content-type", "text/plain");
			}
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	private void loadProperties(Properties props, String string) throws Exception {
		System.out.println("loadProperties: " + string);
		Document document = xmlStringToDoc(string);

		NodeList nodes = document.getChildNodes().item(0).getChildNodes();
		for (int ii = 0; ii < nodes.getLength(); ii++) {
			String key = nodes.item(ii).getNodeName();
			String val = nodes.item(ii).getTextContent();
			props.setProperty(key, val);
		}
	}

	private Document xmlStringToDoc(String string) throws ParserConfigurationException,
			SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader(string)));
		return document;
	}

	private String propsToXml(Properties respProps) {
		String resp = "<?xml version=\"1.0\"?>" + "<response>";
		for (Object iterKey : respProps.keySet()) {
			String key = (String) iterKey;
			String val = (String) respProps.get(key);
			resp += "<" + key + ">" + val + "</" + key + ">";

		}
		resp += "</response>";
		return resp;
	}
}
