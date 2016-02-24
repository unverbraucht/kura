/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.devices;

import java.util.EventObject;

public class ChangeEvent extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = -9217160170028216335L;

	public ChangeEvent(Object source) {
		super(source);
	}

}
