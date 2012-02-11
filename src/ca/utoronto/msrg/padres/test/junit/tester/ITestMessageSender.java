package ca.utoronto.msrg.padres.test.junit.tester;

/**
 * Auxiliary class used as part of test framework.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */

interface ITestMessageSender {
	public String getBrokerURI();
	public String getRemoteURI();
}