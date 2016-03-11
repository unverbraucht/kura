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
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("multiton")
public interface GwtMultitonsService extends RemoteService {
	
	public List<String> getAvailableFactories(GwtXSRFToken xsrfToken) throws GwtKuraException;
	public List<String> getRegisteredInstances(GwtXSRFToken xsrfToken) throws GwtKuraException;
	
	public List<String> getRegisteredEmitters() throws GwtKuraException;
	public List<String> getRegisteredReceivers() throws GwtKuraException;
	
	public void newInstance(GwtXSRFToken xsrfToken, String factoryPid, String instanceName) throws GwtKuraException;
	public void deleteInstance(GwtXSRFToken xsrfToken, String servicePid) throws GwtKuraException;
}
