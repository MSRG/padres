package ca.utoronto.msrg.padres.demo.stockquote;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.common.comm.QueueHandler;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;

public class StockSubMsgHandler extends QueueHandler {

	private StockSubscriber client;

	public StockSubMsgHandler(Client client) {
		super(client.getClientDest());
		this.client = (StockSubscriber) client;
	}

	@Override
	public void processMessage(Message msg) {
		Publication pub = ((PublicationMessage) msg).getPublication();
	}

}
