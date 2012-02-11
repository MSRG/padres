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
 * Created on 21-Jul-2003
 *
 */
package ca.utoronto.msrg.padres.broker.controller;

import java.util.Map;
import java.io.Serializable;

/**
 * A generic Manager for PADRES commands in the Controller. Each Manager handles
 * a specific type of BROKER_CONTROL messages.
 * 
 * @author eli
 */
interface Manager {
	/**
	 * Handle a command sent to the broker Controller.
	 * 
	 * @param command
	 *            The command to handle.
	 */
	public void handleCommand(Map<String, Serializable> command, Serializable payload);
	
	public void shutdown();
}
