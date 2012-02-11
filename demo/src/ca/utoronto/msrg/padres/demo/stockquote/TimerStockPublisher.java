package ca.utoronto.msrg.padres.demo.stockquote;

import java.util.Timer;
import java.util.TimerTask;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogException;
import ca.utoronto.msrg.padres.common.util.LogSetup;

public class TimerStockPublisher extends Client {

	public TimerStockPublisher(ClientConfig newConfig, String id, String brokerConnection,
			long period) throws ClientException, ParseException {
		super(newConfig);

		// handleCommand("a [class,eq,'temp'],[area,eq,'tor'],[value,<,100]");
		handleCommand("a [class,eq,'STOCK'],[symbol,isPresent,'STEM'],[open,isPresent,1.2],[high,isPresent,1.2],[low,isPresent,1.2],[close,isPresent,1.2],[volume,isPresent,1],[date,isPresent,'A'],[num,isPresent,1]");

		try {
			/*
			 * synchronized (this) { wait(); }
			 */
			Thread.sleep(10000);
		} catch (Exception e) {
		}

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			int pubcount = 0;

			public void run() {
				try {
					// handleCommand("p [class,'temp'],[area,'tor'],[value,-10]");
					handleCommand("p [class,'STOCK'],[symbol,'STEM'],[open,3.26],[high,3.26],[low,3.15],[close,3.18],[volume,1630900],[date,'21-Dec-04'],[num,"
							+ (pubcount++) + "]");
				} catch (Exception e) {
					System.out.println("Could not send publication");
					e.printStackTrace();
					System.exit(1);
				}
			}
		}, 0, period);

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
		int rate = Integer.parseInt(args[2]); // Messages/minute

		int delay = (int) ((1.0 / rate) * 60000);

		try {
			ClientConfig newConfig = new ClientConfig();
			newConfig.clientID = id;
			newConfig.connectBrokerList = new String[1];
			newConfig.connectBrokerList[0] = brokerURI;
			new TimerStockPublisher(newConfig, id, brokerURI, delay);
		} catch (ClientException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
