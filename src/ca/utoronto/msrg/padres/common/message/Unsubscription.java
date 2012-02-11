// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================
/*
 * Created on 23-Jul-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.common.message;

import java.io.Serializable;

/**
 * @author eli
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Unsubscription implements Serializable {

	public static final long serialVersionUID = 1;

	private String subID;

	public Unsubscription() {
		subID = "";
	}

	public Unsubscription(String subID) {
		this.subID = subID;
	}

	/**
	 * @return
	 */
	public String getSubID() {
		return subID;
	}

	/**
	 * @param subID
	 */
	public void setSubID(String subID) {
		this.subID = subID;
	}

	public Unsubscription duplicate() {
		Unsubscription newUnSub = new Unsubscription();
		newUnSub.subID = this.subID;
		return newUnSub;
	}

	public String toString() {
		return subID;
	}
}
