package ca.utoronto.msrg.padres.tools.panda;

/*
 * Created on May 7, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author alex
 *
 * Checks loaded input file for:
 * - duplicate entity ids
 * - duplicate brokers using same IP + port
 * - publication exists
 * 
 * This doesn't check for removal commands because no errors occur if you
 * are trying to kill a non-existing process.
 * 
 * It is also not smart in that you can use trick the verifier with different
 * addresses that actually refer to the same machine, such as "grey.msrg" and
 * grey's actual IP address.
 * 
 * It is also possible to check if client and broker connections are valid,
 * but since synchronization is very loose in a real world deployment, this
 * feature may be an overkill.  Plus, users may want to do stupid things on
 * purpose, and displaying a warning message for each one of those cases 
 * may render this verifier more bothersome than needed. 
 * 
 */

import java.util.HashSet;
import java.util.Set;

import ca.utoronto.msrg.padres.tools.panda.input.InputCommand;
import ca.utoronto.msrg.padres.tools.panda.input.ProcessRemoveCommand;

public class TopologyValidator {

	// private final Map brokerIdToAddrMap;
	private final Set<String> allIdSet;

	// private final Map brokerAddrToBrokerIdMap;

	private final int DEFAULT_MAP_SIZE_BIG = 1000;

	// private final int DEFAULT_MAP_SIZE_SMALL = 1000;

	public TopologyValidator(Panda panda) {
		// brokerIdToAddrMap = new HashMap(DEFAULT_MAP_SIZE_SMALL);
		allIdSet = new HashSet<String>(DEFAULT_MAP_SIZE_BIG);
		// brokerAddrToBrokerIdMap = new HashMap(DEFAULT_MAP_SIZE_SMALL);
	}

	public void clear() {
		// brokerIdToAddrMap.clear();
		allIdSet.clear();
		// brokerAddrToBrokerIdMap.clear();
	}

	public void verify(InputCommand inputCmd) {
		checkForDuplicateId(inputCmd);
	}

	private void checkForDuplicateId(InputCommand inputCmd) {
		String id = inputCmd.getId();
		
		if (inputCmd.getClass() != ProcessRemoveCommand.class && allIdSet.contains(id)) {
			System.out.println("WARNING: (TopologyValidator) Duplicate id found '" + id + "'\n");
		}

		// now add new id into set
		allIdSet.add(id);
	}

}
