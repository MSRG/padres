package ca.utoronto.msrg.padres.test.junit;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ca.utoronto.msrg.padres.common.message.*;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.message.parser.TokenMgrError;

public class TestMessageParser extends TestCase {

	protected void setUp() throws Exception {super.setUp();	}
	protected void tearDown() throws Exception {super.tearDown();}
    public TestMessageParser(String name) {super(name);}

        public void testInsert() throws ParseException, TokenMgrError {
        	{
        		String stringRep = new String("[class,'temp'],[area,'tor'],[value,-10];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	        	assertTrue(pub.toString().contains("[value,-10]"));
        	}
        	
        	{
        		String stringRep = new String("[class,'temp'],[area,'tor'],[value,+10];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	        	assertTrue(pub.toString().contains("[value,10]"));
        	}

        	{
        		String stringRep = new String("[class,'temp'],[area,'tor'],[value,-10.9];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	        	assertTrue(pub.toString().contains("[value,-10.9]"));
        	}

        	{
        		String stringRep = new String("[class,'temp'],[area,'tor'],[value,+10.9];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	        	assertTrue(pub.toString().contains("[value,10.9]"));
        	}

        	{
        		String stringRep = new String("[class,stock],[price,100.3];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	        	assertTrue(pub.toString().contains("[price,100.3]"));
        	}
        	
        	{
        		String stringRep = new String("[class,stock],[price,\"100.4\"];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	        	assertTrue(pub.toString().contains("[price,\"100.4\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,reading];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,reading]"));
        	}
        	
        	{
        		String stringRep = new String("[class,reading];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,reading]"));
        	}
        	
        	{
        		String stringRep = new String("[class,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,abc]"));
        	}
        	
        	{
        		String stringRep = new String("[class,reading],[level,123];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,reading]"));
	            assertTrue(pub.toString().contains("[level,123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,reading],[level,'abc'];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,reading]"));
	            assertTrue(pub.toString().contains("[level,\"abc\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,reading],[shipID,123],[level,5];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,reading]"));
	            assertTrue(pub.toString().contains("[shipID,123]"));
	            assertTrue(pub.toString().contains("[level,5]"));
        	}
        	
        	{
        		String stringRep = new String("[class,audit],[firm,ACME],[trust,5];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,audit]"));
	            assertTrue(pub.toString().contains("[firm,\"ACME\"]"));
	            assertTrue(pub.toString().contains("[trust,5]"));
        	}
        }
        
        public void testInsertMultipleAttributes() throws ParseException{
        	{
        		String stringRep = new String("[class,audit],[firm,ACME],[trust,5];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,audit]"));
	            assertTrue(pub.toString().contains("[firm,\"ACME\"]"));
	            assertTrue(pub.toString().contains("[trust,5]"));
        	}
        	
        	{
        		String stringRep = new String("[class,manifest],[shipID,123],[firm,ACME],[content,dynamite];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,manifest],"));
	            assertTrue(pub.toString().contains("[shipID,123]"));
	            assertTrue(pub.toString().contains("[firm,\"ACME\"]"));
	            assertTrue(pub.toString().contains("[content,\"dynamite\"]"));
        	}
        }
        
        public void testInsertMixedTypes() throws ParseException{
        	{
        		String stringRep = new String("[class,audit],[firm,3.3],[trust,5];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,audit]"));
	            assertTrue(pub.toString().contains("[firm,3.3]"));
	            assertTrue(pub.toString().contains("[trust,5]"));
        	}

        	{
        		String stringRep = new String("[class,manifest],[shipID,123],[firm,\"ACME\"],[content,4.5];");
	        	System.out.println("Input: " + stringRep);
	        	Publication pub = MessageFactory.createPublicationFromString(stringRep);
	            assertTrue(pub.toString().contains("[class,manifest]"));
	            assertTrue(pub.toString().contains("[shipID,123]"));
	            assertTrue(pub.toString().contains("[firm,\"ACME\"]"));
	            assertTrue(pub.toString().contains("[content,4.5]"));
        	}
        }

        public void testCreateTable() throws ParseException{
        	{
        		String stringRep = "[class,eq,'stock'],[price,=,100.3];";
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	        	assertTrue(adv.toString().contains("[price,=,100.3]"));
        	}
        	
        	{
        		String stringRep = "[class,eq,'BROKER_CONTROL'],[brokerID,isPresent,''],[command,str-contains,'-'],[broker,isPresent,''],[fromID,isPresent,''],[fromURI,isPresent,''];";
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	        	assertTrue(adv.toString().contains("[brokerID,isPresent,\"\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading];");
	        	System.out.println("1Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
        	}
        	
            {
        		String stringRep = new String("[class,eq,reading],[level,=,5];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[level,=,5]"));
            }
            
            {
        		String stringRep = new String("[class,eq,reading],[level,eq,\"5\"];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[level,eq,\"5\"]"));
            }
            
            {
        		String stringRep = new String("[class,eq,reading],[level,=,5.5];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[level,=,5.5]"));
            }
        }
        
        public void testCreateTableAsteriskTypes() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,reading];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
        	}
            
        	{
        		String stringRep = new String("[class,eq,reading],[level,isPresent,123];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[level,isPresent,123]"));
        	}
            
        	{
        		String stringRep = new String("[class,eq,reading],[level,isPresent,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[level,isPresent,\"abc\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[level,isPresent,123.123];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[level,isPresent,123.123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,=,123];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,=,123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,isPresent,123];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,isPresent,123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,isPresent,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,isPresent,\"abc\"]"));
        	}
        }
       
        public void testCreateTableMultipleAttributes() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,audit],[firm,isPresent,abc],[trust,>=,0];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,audit]"));
	            assertTrue(adv.toString().contains("[firm,isPresent,\"abc\"]"));
	            assertTrue(adv.toString().contains("[trust,>=,0]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,manifest],[shipID,isPresent,123],[firm,isPresent,123.123],[content,isPresent,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,manifest]"));
	            assertTrue(adv.toString().contains("[shipID,isPresent,123]"));
	            assertTrue(adv.toString().contains("[firm,isPresent,123.123]"));
	            assertTrue(adv.toString().contains("[content,isPresent,\"abc\"]"));
        	}
        }
        
        public void testCreateTableIntWithDifferentOperators() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,>,123];");
        		System.out.println("Input: " + stringRep);
		    	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
		        assertTrue(adv.toString().contains("[class,eq,reading]"));
		        assertTrue(adv.toString().contains("[shipID,>,123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,=,123],[level,<,3];");
		    	System.out.println("Input: " + stringRep);
		    	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
		        assertTrue(adv.toString().contains("[class,eq,reading]"));
		        assertTrue(adv.toString().contains("[shipID,=,123]"));
		        assertTrue(adv.toString().contains("[level,<,3]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,<=,123],[level,>=,3];");
		    	System.out.println("Input: " + stringRep);
		    	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
		        assertTrue(adv.toString().contains("[class,eq,reading]"));
		        assertTrue(adv.toString().contains("[shipID,<=,123]"));
		        assertTrue(adv.toString().contains("[level,>=,3]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,>,123],[level,isPresent,\"123\"];");
		    	System.out.println("Input: " + stringRep);
		    	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
		        assertTrue(adv.toString().contains("[class,eq,reading]"));
		        assertTrue(adv.toString().contains("[shipID,>,123]"));
		        assertTrue(adv.toString().contains("[level,isPresent,\"123\"]"));
        	}
        }
        
        public void testCreateTableDoubleWithDifferentOperators() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,>,123.456];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,>,123.456]"));
        	}
            
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,<=,123.567],[level,>=,3.456];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,<=,123.567]"));
	            assertTrue(adv.toString().contains("[level,>=,3.456]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,=,123.456],[level,<,3.456];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,=,123.456]"));
	            assertTrue(adv.toString().contains("[level,<,3.456]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,>,123.345],[level,isPresent,123.123];");
	            Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,>,123.345]"));
	            assertTrue(adv.toString().contains("[level,isPresent,123.123]"));
        	}
        }
        
        public void testCreateTableStringWithDifferentOperators() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,str-gt,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,str-gt,\"abc\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,eq,abc],[level,str-lt,abcd];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,eq,\"abc\"]"));
	            assertTrue(adv.toString().contains("[level,str-lt,\"abcd\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,str-le,abc],[level,str-ge,abcd];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,str-le,\"abc\"]"));
	            assertTrue(adv.toString().contains("[level,str-ge,\"abcd\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,str-gt,abc],[level,isPresent,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,reading]"));
	            assertTrue(adv.toString().contains("[shipID,str-gt,\"abc\"]"));
	            assertTrue(adv.toString().contains("[level,isPresent,\"abc\"]"));
        	}
        }
        
        public void testCreateTableMixedTypes() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,audit],[firm,isPresent,abc],[trust,>=,0.0];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,audit]"));
	            assertTrue(adv.toString().contains("[firm,isPresent,\"abc\"]"));
	            assertTrue(adv.toString().contains("[trust,>=,0.0]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,manifest],[shipID,=,12345],[content,eq,radioactivestuff],[firm,>,12345.67];");
	        	System.out.println("Input: " + stringRep);
	        	Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
	            assertTrue(adv.toString().contains("[class,eq,manifest]"));
	            assertTrue(adv.toString().contains("[shipID,=,12345]"));
	            assertTrue(adv.toString().contains("[content,eq,\"radioactivestuff\"]"));
	            assertTrue(adv.toString().contains("[firm,>,12345.67]"));
        	}
        }
     
        public void testSelect() throws ParseException{
        	{
        		String stringRep = "[class,eq,'stock'],[price,=,100.3];";
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	        	assertTrue(sub.toString().contains("[price,=,100.3]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
        	}
            
        	{
        		String stringRep = new String("[class,eq,reading];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
        	}
        
        	{
        		String stringRep = new String("[class,eq,reading],[level,=,5];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[level,=,5]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[level,eq,\"5\"];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[level,eq,\"5\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[level,=,5.5];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[level,=,5.5]"));
        	}
        }
        
        public void testSelectAsteriskTypes() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,reading];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[level,isPresent,123];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[level,isPresent,123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[level,isPresent,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[level,isPresent,\"abc\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[level,isPresent,123.123];");
        		System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[level,isPresent,123.123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,=,123];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,=,123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,isPresent,123];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,isPresent,123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,isPresent,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,isPresent,\"abc\"]"));
        	}
        }
    
        public void testSelectMultipleAttributes() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,audit],[firm,isPresent,abc],[trust,>=,0];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,audit]"));
	            assertTrue(sub.toString().contains("[firm,isPresent,\"abc\"]"));
	            assertTrue(sub.toString().contains("[trust,>=,0]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,manifest],[shipID,isPresent,123],[firm,isPresent,123.123],[content,isPresent,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,manifest]"));
	            assertTrue(sub.toString().contains("[shipID,isPresent,123]"));
	            assertTrue(sub.toString().contains("[firm,isPresent,123.123]"));
	            assertTrue(sub.toString().contains("[content,isPresent,\"abc\"]"));
        	}
        }
        
        public void testSelectIntWithDifferentOperators() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,>,123];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,>,123]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,=,123],[level,<,3];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,=,123]"));
	            assertTrue(sub.toString().contains("[level,<,3]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,<=,123],[level,>=,3];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,<=,123]"));
	            assertTrue(sub.toString().contains("[level,>=,3]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,>,123],[level,isPresent,123];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,>,123]"));
	            assertTrue(sub.toString().contains("[level,isPresent,123]"));
        	}
        }
        
        public void testSelectDoubleWithDifferentOperators() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,>,123.456];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,>,123.456]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,=,123.456],[level,<,3.456];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,=,123.456]"));
	            assertTrue(sub.toString().contains("[level,<,3.456]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,<=,123.567],[level,>=,3.456];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,<=,123.567]"));
	            assertTrue(sub.toString().contains("[level,>=,3.456]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,>,123.345],[level,isPresent,\"123.123\"];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,>,123.345]"));
	            assertTrue(sub.toString().contains("[level,isPresent,\"123.123\"]"));
        	}
        }
        
        public void testSelectStringWithDifferentOperators() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,str-gt,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,str-gt,\"abc\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,eq,abc],[level,str-lt,abcd];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,eq,\"abc\"]"));
	            assertTrue(sub.toString().contains("[level,str-lt,\"abcd\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,str-le,abc],[level,str-ge,abcd];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,str-le,\"abc\"]"));
	            assertTrue(sub.toString().contains("[level,str-ge,\"abcd\"]"));
        	}
        	
        	{
        		String stringRep = new String("[class,eq,reading],[shipID,str-gt,abc],[level,isPresent,abc];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,reading]"));
	            assertTrue(sub.toString().contains("[shipID,str-gt,\"abc\"]"));
	            assertTrue(sub.toString().contains("[level,isPresent,\"abc\"]"));
        	}
        }
        
        public void testSelectMixedTypes() throws ParseException{
        	{
        		String stringRep = new String("[class,eq,audit],[firm,isPresent,abc],[trust,>=,0.0];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,audit]"));
	            assertTrue(sub.toString().contains("[firm,isPresent,\"abc\"]"));
	            assertTrue(sub.toString().contains("[trust,>=,0.0]"));
        	}
            
        	{
        		String stringRep = new String("[class,eq,manifest],[shipID,=,12345],[content,eq,radioactivestuff],[firm,>,12345.67];");
	        	System.out.println("Input: " + stringRep);
	        	Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
	            assertTrue(sub.toString().contains("[class,eq,manifest]"));
	            assertTrue(sub.toString().contains("[shipID,=,12345]"));
	            assertTrue(sub.toString().contains("[content,eq,\"radioactivestuff\"]"));
	            assertTrue(sub.toString().contains("[firm,>,12345.67]"));
        	}
        }
        
        public void testCompositeSelect() throws ParseException{
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
        	}
	            
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	    	    assertTrue(comsub.toString().contains("[class,eq,reading]"));
        	}
    	    
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,=,5]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[level,=,5]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,eq,\"5\"]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	        	assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[level,eq,\"5\"]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,=,5.5]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[level,=,5.5]"));
        	}
        }
        
        public void testCompositeSelectAsteriskTypes() throws ParseException{
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,isPresent,123]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	    	    assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[level,isPresent,123]"));
        	}
    		
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,isPresent,abc]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[level,isPresent,\"abc\"]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,isPresent,123.123]}}");
        		System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[level,isPresent,123.123]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,=,123]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,=,123]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,isPresent,123]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,isPresent,123]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,isPresent,abc]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,isPresent,\"abc\"]"));
        	}
        }

        public void testCompositeSelectMultipleAttributes() throws ParseException{
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,audit],[firm,isPresent,abc],[trust,>=,0]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[firm,isPresent,\"abc\"]"));
	            assertTrue(comsub.toString().contains("[trust,>=,0]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,manifest],[shipID,isPresent,123],[firm,isPresent,123.123],[content,isPresent,abc]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,manifest]"));
	             assertTrue(comsub.toString().contains("[shipID,isPresent,123]"));
	            assertTrue(comsub.toString().contains("[firm,isPresent,123.123]"));
	            assertTrue(comsub.toString().contains("[content,isPresent,\"abc\"]"));
        	}
        }
        
        public void testCompositeSelectIntWithDifferentOperators() throws ParseException{
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,>,123]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,>,123]"));
        	}
            
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,=,123],[level,<,3]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,=,123]"));
	            assertTrue(comsub.toString().contains("[level,<,3]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,<=,123],[level,>=,3]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,<=,123]"));
	            assertTrue(comsub.toString().contains("[level,>=,3]"));
        	}
            
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,>,123],[level,isPresent,123]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,>,123]"));
	            assertTrue(comsub.toString().contains("[level,isPresent,123]"));
        	}
        }
        
        public void testCompositeSelectDoubleWithDifferentOperators() throws ParseException{
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,>,123.456]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,>,123.456]"));
        	}
        	
            {
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,=,123.456],[level,<,3.456]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,=,123.456]"));
	            assertTrue(comsub.toString().contains(",[level,<,3.456]"));
            }
            
            {
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,<=,123.567],[level,>=,3.456]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,<=,123.567]"));
	            assertTrue(comsub.toString().contains("[level,>=,3.456]"));
            }
            
            {
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,>,123.345],[level,isPresent,123.123]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,>,123.345]"));
	            assertTrue(comsub.toString().contains("[level,isPresent,123.123]"));
        	}
        }
        
        public void testCompositeSelectStringWithDifferentOperators() throws ParseException{
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,str-gt,abc]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,str-gt,\"abc\"]"));
        	}
           	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,eq,abc],[level,str-lt,abcd]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,eq,\"abc\"]"));
	            assertTrue(comsub.toString().contains("[level,str-lt,\"abcd\"]"));
        	}
           	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,str-le,abc],[level,str-ge,abcd]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,str-le,\"abc\"]"));
	            assertTrue(comsub.toString().contains("[level,str-ge,\"abcd\"]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,str-gt,abc],[level,isPresent,abc]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[shipID,str-gt,\"abc\"]"));
	            assertTrue(comsub.toString().contains("[level,isPresent,\"abc\"]"));
        	}
        }
        
        public void testCompositeSelectMixedTypes() throws ParseException{
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,audit],[firm,isPresent,abc],[trust,>=,0.0]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[firm,isPresent,\"abc\"]"));
	            assertTrue(comsub.toString().contains("[trust,>=,0.0]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,audit]}&{[class,eq,manifest],[shipID,=,12345],[content,eq,radioactivestuff],[firm,>,12345.67]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,manifest]"));
	             assertTrue(comsub.toString().contains("[shipID,=,12345]"));
	            assertTrue(comsub.toString().contains("[content,eq,\"radioactivestuff\"]"));
	            assertTrue(comsub.toString().contains("[firm,>,12345.67]"));
        	}
        }

        public void testCompositeSelectVariablesEqualling() throws ParseException{
        	{
        		String stringRep = new String("{{[class,eq,audit],[reading,=,$I$X0]}&{[class,eq,reading],[reading,=,$I$X0]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[reading,=,\"$I$X0\"]"));
        	}
    	  	
        	{
        		String stringRep = new String("{{[class,eq,audit],[reading,=,$F$X0]}&{[class,eq,reading],[reading,=,$F$X0]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[reading,=,\"$F$X0\"]"));
        	}
           
        	{
        		String stringRep = new String("{{[class,eq,audit],[reading,eq,$S$X0]}&{[class,eq,reading],[reading,eq,$S$X0]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[reading,eq,\"$S$X0\"]"));
        	}
    	
        	{
        		String stringRep = new String("{{{[class,eq,audit]}&{[class,eq,audit]}}&{[class,eq,audit]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().substring(10).contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().substring(25).contains("[class,eq,audit]"));
        	}
        	
        	{
        		String stringRep = new String("{{{[class,eq,audit],[reading,=,$I$X2]}&{[class,eq,audit],[reading,=,$I$X2],[value,=,$F$X3]}}&{[class,eq,audit],[value,=,$F$X3]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[reading,=,\"$I$X2\"]"));
	            assertTrue(comsub.toString().contains("[value,=,\"$F$X3\"]"));
        	}
        }
        
        
        public void testCompositeSelectBracesChecking() throws ParseException{
        	{
        		String stringRep = new String("{{{{[class,eq,fa]}&{{[class,eq,do]}||{[class,eq,re]}}}&{[class,eq,so]}}||{[class,eq,do]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,fa]"));
	            assertTrue(comsub.toString().contains("[class,eq,do]"));
	            assertTrue(comsub.toString().contains("[class,eq,re]"));
	            assertTrue(comsub.toString().contains("[class,eq,so]"));
	            assertTrue(comsub.toString().contains("[class,eq,do]"));
        	}
        	
        	{
        		String stringRep = new String("{{{{[class,eq,do]}&{{{[class,eq,re]}||{[class,eq,mi]}}&{[class,eq,fa]}}}||{[class,eq,so]}}&{{{[class,eq,la]}||{[class,eq,ti]}}&{[class,eq,do]}}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,do]"));
	            assertTrue(comsub.toString().contains("[class,eq,re]"));
	            assertTrue(comsub.toString().contains("[class,eq,mi]"));
	            assertTrue(comsub.toString().contains("[class,eq,fa]"));
	            assertTrue(comsub.toString().contains("[class,eq,so]"));
	            assertTrue(comsub.toString().contains("[class,eq,la]"));
	            assertTrue(comsub.toString().contains("[class,eq,ti]"));
	            assertTrue(comsub.toString().contains("[class,eq,do]"));
        	}
        }	
        
        public void test1CompositeSelect() throws ParseException{
        	{
        		String stringRep = new String("{{[class,eq,reading],[level,>,3]}&{[class,eq,audit],[trust,>,7]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[level,>,3]"));
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[trust,>,7]"));
        	}
        	
        	{
        		String stringRep = new String("{{[class,eq,reading],[level,>,3]}&{[class,eq,audit],[trust,>,7]}}");
	        	System.out.println("Input: " + stringRep);
	        	CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
	            assertTrue(comsub.toString().contains("[class,eq,reading]"));
	            assertTrue(comsub.toString().contains("[level,>,3]"));
	            assertTrue(comsub.toString().contains("[class,eq,audit]"));
	            assertTrue(comsub.toString().contains("[trust,>,7]"));
        	}
        }    


        public static TestSuite suite(){
            TestSuite suite = new TestSuite();
            suite.addTest(new TestMessageParser("testInsert"));
            suite.addTest(new TestMessageParser("testInsertMultipleAttributes"));
            suite.addTest(new TestMessageParser("testInsertMixedTypes"));
            
            suite.addTest(new TestMessageParser("testCreateTable"));
            suite.addTest(new TestMessageParser("testCreateTableAsteriskTypes"));
            suite.addTest(new TestMessageParser("testCreateTableMultipleAttributes"));
            suite.addTest(new TestMessageParser("testCreateTableIntWithDifferentOperators"));
            suite.addTest(new TestMessageParser("testCreateTableDoubleWithDifferentOperators"));
            suite.addTest(new TestMessageParser("testCreateTableStringWithDifferentOperators"));
            suite.addTest(new TestMessageParser("testCreateTableMixedTypes"));
            
            suite.addTest(new TestMessageParser("testSelect"));
            suite.addTest(new TestMessageParser("testSelectAsteriskTypes"));
            suite.addTest(new TestMessageParser("testSelectMultipleAttributes"));
            suite.addTest(new TestMessageParser("testSelectIntWithDifferentOperators"));
            suite.addTest(new TestMessageParser("testSelectDoubleWithDifferentOperators"));
            suite.addTest(new TestMessageParser("testSelectStringWithDifferentOperators"));
            suite.addTest(new TestMessageParser("testSelectMixedTypes"));


            suite.addTest(new TestMessageParser("testCompositeSelect"));
            suite.addTest(new TestMessageParser("testCompositeSelectAsteriskTypes"));
            suite.addTest(new TestMessageParser("testCompositeSelectMultipleAttributes"));
            suite.addTest(new TestMessageParser("testCompositeSelectIntWithDifferentOperators"));
            suite.addTest(new TestMessageParser("testCompositeSelectDoubleWithDifferentOperators"));
            suite.addTest(new TestMessageParser("testCompositeSelectStringWithDifferentOperators"));
            suite.addTest(new TestMessageParser("testCompositeSelectMixedTypes"));
      
            suite.addTest(new TestMessageParser("testCompositeSelectVariablesEqualling"));
            suite.addTest(new TestMessageParser("testCompositeSelectBracesChecking"));
            
            suite.addTest(new TestMessageParser("test1CompositeSelect"));
            
            return suite;
      }
        
    }