package ca.utoronto.msrg.padres.tools.webclient.services;

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
import ca.utoronto.msrg.padres.client.CommandResult;
import ca.utoronto.msrg.padres.tools.webclient.WebClient;

/*
 * Will look for a Demo client to run
 */
public class ClientService extends Service {

	private static final String COMMAND_TAG = "command";

	private static WebClient commandHandler;

	public ClientService(Context context) {
		super(context);
	}

	public void prepare(WebClient client) {
		commandHandler = client;
	}

	public void process(Request req, Response resp) throws Exception {
		// covert the request XML into Java Properties
		Properties props = requestToProperties(req);

		// send the command to the client to process
		String command = props.getProperty(COMMAND_TAG);
		CommandResult results = commandHandler.handleCommand(command);

		// populate valid response
		PrintStream out = resp.getPrintStream();
		String respString = propsToXml(results);
		out.println(respString);
		resp.set("content-type", "text/xml");
		out.close();
	}

	private Properties requestToProperties(Request request) throws IOException,
			ParserConfigurationException, SAXException {
		// get request content (expect an xml string)
		int reqLength = request.getContentLength();
		InputStream reqInStream = request.getInputStream();
		byte[] bytes = new byte[reqLength];
		for (int i = 0; i < reqLength; i++)
			bytes[i] = (byte) reqInStream.read();
		reqInStream.close();
		return loadProperties(new String(bytes));
	}

	private Properties loadProperties(String xmlString) throws ParserConfigurationException,
			SAXException, IOException {
		Properties props = new Properties();
		Document xmlDocument = xmlStringToDoc(xmlString);

		NodeList nodes = xmlDocument.getChildNodes().item(0).getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			String key = nodes.item(i).getNodeName();
			String val = nodes.item(i).getTextContent();
			props.setProperty(key, val);
		}
		return props;
	}

	private Document xmlStringToDoc(String xmlString) throws ParserConfigurationException,
			SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xmlDocument = builder.parse(new InputSource(new StringReader(xmlString)));
		return xmlDocument;
	}

	private String propsToXml(CommandResult results) {
		Properties respProps = results.toProperties();
		String xmlResp = "<?xml version=\"1.0\"?>" + "<response>";
		for (Object iterKey : respProps.keySet()) {
			String key = (String) iterKey;
			String val = (String) respProps.get(key);
			val = val.replace("&", "&amp;");
			val = val.replace("<", "&lt;");
			val = val.replace(">", "&gt;");
			xmlResp += "<" + key + ">" + val + "</" + key + ">";

		}
		xmlResp += "</response>";
		return xmlResp;
	}
}
