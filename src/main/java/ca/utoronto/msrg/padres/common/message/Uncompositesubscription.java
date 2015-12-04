package ca.utoronto.msrg.padres.common.message;

import java.io.Serializable;

/**
 * @author shuang
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

public class Uncompositesubscription implements Serializable {

	public static final long serialVersionUID = 1;

	private String csubID;

	public Uncompositesubscription() {
		csubID = "";
	}

	public Uncompositesubscription(String stringRep) {
		csubID = stringRep;
	}

	/**
	 * @return
	 */
	public String getSubID() {
		return csubID;
	}

	/**
	 * @param csubID
	 */
	public void setSubID(String csubID) {
		this.csubID = csubID;
	}

	public Uncompositesubscription duplicate() {
		Uncompositesubscription newUnCS = new Uncompositesubscription();
		newUnCS.csubID = this.csubID;
		return newUnCS;
	}

	public String toString() {
		return csubID;
	}
}
