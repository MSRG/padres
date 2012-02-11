/**
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "Exolab" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Exoffice Technologies.  For written permission,
 *    please contact info@exolab.org.
 *
 * 4. Products derived from this Software may not be called "Exolab"
 *    nor may "Exolab" appear in their names without prior written
 *    permission of Exoffice Technologies. Exolab is a registered
 *    trademark of Exoffice Technologies.
 *
 * 5. Due credit should be given to the Exolab Project
 *    (http://www.exolab.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY EXOFFICE TECHNOLOGIES AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * EXOFFICE TECHNOLOGIES OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2001-2002 (C) Exoffice Technologies Inc. All Rights Reserved.
 *
 * $Id$
 *
 * Date         Author  Changes
 * 06/07/2001    jima    Created
 */
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
 * Created on Jul 10, 2003
 *
 * Copywriter (c) 2003 Cybermation & University of Toronto All rights reserved.
 * 
 */
package ca.utoronto.msrg.padres.common.comm;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.ShutdownMessage;

/**
 * 
 * This is synchronized store and forward queue.
 */
public class MessageQueue implements Serializable {

	private static final long serialVersionUID = 6078479409032444270L;

	/**
	 * The message queue is implemented as linked list
	 */
	private List<Message> list = null;
	
	private boolean running = false;
	
	/**
	 * Default constructor
	 */
	public MessageQueue() {
		list = new LinkedList<Message>();
		running = true;
	}

	/**
	 * Return all elements in the collection
	 * 
	 * @return Object[] array of elements to return
	 */
	public synchronized Message[] toArray() {
		return list.toArray(new Message[0]);
	}

	/**
	 * Return the number elements in the queue
	 * 
	 * @return int size of the queue
	 */
	public synchronized int size() {
		return list.size();
	}

	/**
	 * Add the element to the queue in the required order. It uses a binary search to locate the
	 * correct position. Notifies blocked threads.
	 * 
	 * @param obj
	 *            object need to be added
	 */
	public synchronized void add(Message obj) {
		list.add(obj);
		notify();
	}
	
	/**
	 * Add the element to the queue in the required order. It uses a binary search to locate the
	 * correct position. Notifies blocked threads.
	 * 
	 * @param obj
	 *            object need to be added
	 */
	public synchronized void addFirst(Message obj) {
		list.add(0, obj);
		notify();
	}

	/**
	 * Returns the first element in the queue.
	 * 
	 * @return the first element in the queue, or <code>null</code>, if the queue is empty
	 */
	public synchronized Message first() {
		return (list.size() > 0) ? list.get(0) : null;
	}

	/**
	 * Removes and returns the first element on the queue.
	 * 
	 * @return the first element in the queue, or <code>null</code>, if the queue is empty
	 */
	public synchronized Message removeFirst() {
		return (list.size() > 0) ? list.remove(0) : null;
	}

	/**
	 * Remove an element from the queue, blocking until an element is available.
	 * 
	 * @return The first element in the queue.
	 */
	public synchronized Message blockingRemove() {
		// retry until an element is obtained
		while (true) {
			if (list.size() > 0) {
				return list.remove(0);
			} else {
				// block until notified that an element is in the queue
				try {
					wait();
				} catch (InterruptedException ie) {
				}
			}
		}
	}
	
//	
//	public void start(){
//		running = true;
//	}
//	
//	public void stop(){
//		running = false;
//	}
}
