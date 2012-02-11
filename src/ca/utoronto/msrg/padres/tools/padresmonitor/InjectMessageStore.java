
package ca.utoronto.msrg.padres.tools.padresmonitor;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.InjectMessageDialog;

/**
 * @author Gerald Chan
 *
 * The data structure that cointain
 * 
 *  1. The text of the injected message
 *  2. Its type
 *  3. Its injection id
 * 
 * This class is used by the ClientInjectionManager and UnjectionMassageDialog
 * Its purpose is to tide the message and its type and injection id together, so when uninject
 * the backend will know what action to take, since adv msg and sub msg format is the same
 */
public class InjectMessageStore {

	private String m_Msg;
	private int m_Type;
	private String m_InjectID;
	
	public InjectMessageStore(String msg, int type, String injectID) {
		m_Msg = msg;
		m_Type = type;
		m_InjectID = injectID; 
	}
	
	public void setMsg(String msg) {
		m_Msg = msg;
	}
	
	public void setType(int type) {
		m_Type = type;
	}
	
	public void setInjectID(String id) {
		m_InjectID = id;
	}
	
	public String getMsg() {
		return m_Msg;
	}
	
	public int getType() {
		return m_Type;
	}
	
	public String getInjectID() {
		return m_InjectID;
	}
	
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj == null || !(obj instanceof InjectMessageStore)) {
			return result;
		}
		
		// Pointer is not null and is InjectMessageStore class
		InjectMessageStore other = ((InjectMessageStore)obj);
		if (other.getInjectID().equals(m_InjectID) &&
			other.getType() == m_Type &&
			other.getMsg().equals(m_Msg)) {
				result = true;
		}
		return result;
	}
	
	public String toString() {
		String result = "";
		switch(m_Type) {
			case InjectMessageDialog.INJ_TYPE_ADV:
				result = "ADV: "+m_Msg;
				break;
			case InjectMessageDialog.INJ_TYPE_PUB:
				result = "PUB: "+m_Msg;
				break;
			case InjectMessageDialog.INJ_TYPE_SUB:
				result = "SUB: "+m_Msg;
				break;
		}
	 
		return result;
	}
}
