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
public class Unadvertisement implements Serializable {

	public static final long serialVersionUID = 1;

	private String advID;

	public Unadvertisement() {
		advID = "";
	}

	public Unadvertisement(String stringRep) {
		advID = stringRep;
	}

	/**
	 * @return
	 */
	public String getAdvID() {
		return advID;
	}

	/**
	 * @param advID
	 */
	public void setAdvID(String advID) {
		this.advID = advID;
	}

	public Unadvertisement duplicate() {
		Unadvertisement newUnAdv = new Unadvertisement();
		newUnAdv.advID = this.advID;
		return newUnAdv;
	}

	public String toString() {
		return advID;
	}
}
