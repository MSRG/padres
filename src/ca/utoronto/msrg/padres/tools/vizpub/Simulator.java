package ca.utoronto.msrg.padres.tools.vizpub;

import no.uio.ifi.vizpub.reporter.ReporterService;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.tools.guiclient.GUIClient;

public class Simulator {

	public static void main(String args[]) {
		//Reporter.initialize();
		ReporterService.initialize();
		try {
			Thread.sleep(1000);
			BrokerCore.main(new String[] {
					"-uri", "socket://localhost:1101/BrokerB"});
			Thread.sleep(1000);
			BrokerCore.main(new String[] {
					"-uri", "socket://localhost:1100/BrokerA", "-n", "socket://localhost:1101/BrokerB"});
			Thread.sleep(1000);
			BrokerCore.main(new String[] {
					"-uri", "socket://localhost:1102/BrokerC", "-n", "socket://localhost:1101/BrokerB"});
			Thread.sleep(1000);
			GUIClient.main(new String[] {
					"-i", "ClientX", "-b", "socket://localhost:1100/BrokerA"});
			Thread.sleep(1000);
			GUIClient.main(new String[] {
					"-i", "ClientY", "-b", "socket://localhost:1102/BrokerC"});
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
