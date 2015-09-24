package ca.utoronto.msrg.padres.common.message.parser;

import java.io.StringReader;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.parser.MessageParser.ParserType;

public class MessageFactory {
	
	static Logger messageLogger = Logger.getLogger("Message");

	static Logger exceptionLogger = Logger.getLogger("Exception");
	
	private MessageFactory() {}
	
	private static final MessageParser _mp;
	static{
		MessageParser tmpMP = null;
		try{
			tmpMP = new MessageParser(ParserType.UNINITIALIZED, "");
		}catch(ParseException e) {
			messageLogger.fatal("Could not instantiate message parser... ", e);
			exceptionLogger.fatal("Could not instantiate message parser... ", e);
			System.exit(-1);
		}
		_mp = tmpMP;
	}

	public static Publication createEmptyPublication() {
		return new Publication();
	}
	
	public static Publication createPublicationFromString(String stringRep) throws ParseException {
		synchronized (_mp) {
			_mp.ReInit(new StringReader(stringRep + ";"));
			_mp._parsertype = ParserType.PUB_PARSER;
			Object obj = _mp.Input();
			if (obj == null)
				return createEmptyPublication();
			else
				return (Publication)obj;
		}
	}
	
	public static CompositeSubscription createEmptyCompositeSubscription () {
		return new CompositeSubscription();
	}
	
	public static CompositeSubscription createCompositeSubscriptionFromString(String stringRep) throws ParseException {
		synchronized (_mp) {
			_mp._parsertype = ParserType.COMP_SUB_PARSER;
			_mp.ReInit(new StringReader(stringRep + ";"));
		  	CompositeSubscription cs = (CompositeSubscription) _mp.Input();
			if(cs==null)
				return createEmptyCompositeSubscription();
			else
				return cs;
		}
	}
	
	public static Subscription createEmptySubscription() {
		return new  Subscription();
	}
	
	public static Subscription createSubscriptionFromString(String stringRep) throws ParseException {
		synchronized (_mp) {
			_mp._parsertype = ParserType.SUB_PARSER;
			_mp.ReInit(new StringReader(stringRep + ";"));
			Object obj = _mp.Input();
			if (obj == null )
				return createEmptySubscription();
			else
				return (Subscription)obj;
		}
	}

	public static Advertisement createEmptyAdvertisement() {
		return new Advertisement();
	}
	
	public static Advertisement createAdvertisementFromString(String stringRep) throws ParseException {
		synchronized (_mp) {
			_mp.ReInit(new StringReader(stringRep + ";"));
			_mp._parsertype = ParserType.ADV_PARSER;
			Object obj = _mp.Input();
			if (obj == null )
				return createEmptyAdvertisement();
			else
				return (Advertisement)obj;
		}
	}	
}
