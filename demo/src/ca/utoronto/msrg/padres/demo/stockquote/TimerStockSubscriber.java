package ca.utoronto.msrg.padres.demo.stockquote;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogException;
import ca.utoronto.msrg.padres.common.util.LogSetup;

public class TimerStockSubscriber extends Client {

	public TimerStockSubscriber(ClientConfig newConfig, String id, String brokerConnection)
			throws ClientException, ParseException {
		super(newConfig);
		handleCommand("s [class,eq,'STOCK'],[symbol,isPresent,'STEM']");
	}

	public void processMessage(Message msg) {
		if (msg instanceof PublicationMessage) {
			Publication pub = ((PublicationMessage) msg).getPublication();
			long currTime = System.currentTimeMillis();
			long timestamp = pub.getTimeStamp().getTime();
			long delay = currTime - timestamp;
			System.out.println(pub.getPairMap().get("num") + "\t" + timestamp + "\t" + currTime
					+ "\t" + delay);
		}
	}

	public static void main(String args[]) {
		try {
			new LogSetup(null);
		} catch (LogException e) {
			e.printStackTrace();
			System.exit(1);
		}
		String id = args[0];
		String brokerURI = args[1];

		try {
			ClientConfig newConfig = new ClientConfig();
			newConfig.clientID = id;
			newConfig.connectBrokerList = new String[1];
			newConfig.connectBrokerList[0] = brokerURI;
			new TimerStockSubscriber(newConfig, id, brokerURI);
		} catch (ClientException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
