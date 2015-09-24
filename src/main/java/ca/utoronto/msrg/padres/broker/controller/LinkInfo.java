/**
 * 
 */
package ca.utoronto.msrg.padres.broker.controller;

/**
 * @author shou
 *
 */
public class LinkInfo {
	
	private boolean status;
	private int msgRate;
	
	public LinkInfo(){
		status = true;
	}
	
	public synchronized boolean getStatus(){
		return status;
	}
	
	public synchronized void setStatus(){
		status = false;
	}
	
	public synchronized void resetStatus(){
		status = true;
	}
	
	public synchronized int getMsgRate(){
		return msgRate;
	}
	public synchronized void setMsgRate(int rate){
			msgRate = rate;
	}
}
