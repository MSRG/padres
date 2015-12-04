package ca.utoronto.msrg.padres.broker.management.web.services;

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
import ca.utoronto.msrg.padres.broker.management.CommandHandler;
import ca.utoronto.msrg.padres.broker.management.web.ManagementServer;

/*
 * Will run a Broker management command
 */
public class BrokerService extends Service {
	
	private static final String PROP_COMMAND = "command";

	private static final String PROP_ARGS = "args";

	private static final String DEFAULT_COMMAND = "?";
	
	private CommandHandler cmdHandler;

	public BrokerService(Context context) {
		super(context);
	}
	
	public void prepare(ManagementServer ms) throws Exception {
		cmdHandler = ms.getCmdHandler();
	}

	public void process(Request req, Response resp) throws Exception {
		PrintStream out = resp.getPrintStream();

		// get request content (expect an xml string)
		int len = req.getContentLength();
		InputStream in = req.getInputStream();
		byte[] bytes = new byte[len];
		for (int bb = 0; bb < len; bb++) {
			bytes[bb] = (byte) in.read();
		}

		try {
			// fill properties
			Properties props = new Properties();
			loadProperties(props, new String(bytes));

			String command = props.getProperty(PROP_COMMAND, DEFAULT_COMMAND);
			String[] args = (props.getProperty(PROP_ARGS) == null ? null : props.getProperty(
					PROP_ARGS).split("\\s+"));

			Properties respProps = cmdHandler.runCommand(command, args);

			// populate valid response
			if (respProps != null) {
				out.println(propsToXml(respProps));
				resp.set("content-type", "text/xml");
			} else {
				resp.set("content-type", "text/plain");
			}
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
			throw ex;
		}
	}

	private void loadProperties(Properties props, String string) throws Exception {
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
		for (Object obj : respProps.keySet()) {
			String key = (String) obj;
			String val = (String) respProps.get(key);
			val = val.replaceAll("&", "&amp;");
			val = val.replaceAll("<", "&lt;");
			val = val.replaceAll(">", "&gt;");
			resp += "<" + key + ">" + val + "</" + key + ">";

		}
		resp += "</response>";
		return resp;
	}
}
