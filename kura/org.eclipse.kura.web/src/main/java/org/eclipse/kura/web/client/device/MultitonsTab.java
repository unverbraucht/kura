/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.client.device;

import java.util.List;

import org.eclipse.kura.web.client.configuration.ServiceTree;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtMultitonsService;
import org.eclipse.kura.web.shared.service.GwtMultitonsServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.BaseLoader;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FormEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Encoding;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Method;
import com.extjs.gxt.ui.client.widget.form.HiddenField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

public class MultitonsTab extends LayoutContainer {

	private static final Messages MSGS = GWT.create(Messages.class);

	private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/multiton/servlet";

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtMultitonsServiceAsync gwtMultitonService = GWT.create(GwtMultitonsService.class);

	@SuppressWarnings("unused")
	private GwtSession m_currentSession;
	private ServiceTree m_serviceTree;
	private LayoutContainer m_commandInput;
	private FormPanel m_formPanel;
	private Button m_executeButton;
	private Button m_newInstance;
	private ListBox m_factories;
	private ListBox m_instances;
	private ListBox m_emitters;
	private ListBox m_receivers;
	private Button m_deleteInstance;
	
	private TextField<String> m_commandField;
	private HiddenField<String> xsrfTokenField;

	private ButtonBar m_buttonBar;

	public MultitonsTab(GwtSession currentSession, ServiceTree serviceTree) {
		m_currentSession = currentSession;
		m_serviceTree = serviceTree;
	}

	protected void onRender(Element parent, int index) {
		super.onRender(parent, index);
		setLayout(new FitLayout());
		setId("device-multitons");

		FormData formData = new FormData("100%");
		//
		// Command Form
		//
		m_formPanel = new FormPanel();
		m_formPanel.setFrame(true);
		m_formPanel.setHeaderVisible(false);
		m_formPanel.setBorders(false);
		m_formPanel.setBodyBorder(false);
		m_formPanel.setAction(SERVLET_URL);
		m_formPanel.setEncoding(Encoding.MULTIPART);
		m_formPanel.setMethod(Method.POST);
		// m_formPanel.setHeight("100.0%");

		m_formPanel.setButtonAlign(HorizontalAlignment.RIGHT);
		m_buttonBar = m_formPanel.getButtonBar();
		initButtonBar();

		m_formPanel.addListener(Events.Submit, new Listener<FormEvent>() {
			public void handleEvent(FormEvent be) {
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {
					}
				});
			}
		});

		//
		// Input and Upload
		//
		m_commandField = new TextField<String>();
		m_commandField.setName("InstanceName");
		m_commandField.setAllowBlank(false);
		m_commandField.setFieldLabel("Name");
//		m_formPanel.add(m_commandField, formData);


		m_factories = new ListBox();
		m_factories.setName("Availeble Factories");
		m_factories.addItem("Empty");
		
		m_newInstance = new Button("New instance");
		m_newInstance.addSelectionListener(new SelectionListener<ButtonEvent>() {

			@Override
			public void componentSelected(ButtonEvent ce) {
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {
						String selectedPid = m_factories.getValue(m_factories.getSelectedIndex());
						String name = m_commandField.getValue();
						gwtMultitonService.newInstance(token, selectedPid, name, new AsyncCallback<Void>() {

							@Override
							public void onFailure(Throwable caught) {
								FailureHandler.handle(caught);
							}

							@Override
							public void onSuccess(Void result) {
								m_serviceTree.refreshServicePanel();
							}
						});
					}
				});
			}
		});
		
		HorizontalPanel hp = new HorizontalPanel();
		//hp.getElement().getStyle().setMarginLeft(80, Unit.PX);
		//hp.getElement().getStyle().setMarginBottom(7, Unit.PX);
		hp.add(m_commandField);
		hp.add(m_factories);
		hp.add(m_newInstance);
		m_formPanel.add(hp, new FormData("75%"));
		
		m_emitters = new ListBox();
		m_emitters.setName("Available Emitters");
		m_emitters.addItem("Empty");
		
		m_receivers = new ListBox();
		m_receivers.setName("Available Receivers");
		m_receivers.addItem("Empty");
		
		HorizontalPanel hp2 = new HorizontalPanel();
		hp2.add(m_emitters);
		hp2.add(m_receivers);
		m_formPanel.add(hp2, new FormData("100%"));
		

		//Factories proxy loader
		RpcProxy<List<String>> factories_proxy = new RpcProxy<List<String>>() {
			@Override
			protected void load(Object loadConfig, final AsyncCallback<List<String>> callback) {
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {
						gwtMultitonService.getAvailableFactories(token, new AsyncCallback<List<String>>() {

							@Override
							public void onFailure(Throwable caught) {
								FailureHandler.handle(caught);
							}

							@Override
							public void onSuccess(List<String> result) {
								callback.onSuccess(result);

							}
						});

					}
				});
			}
		};

		//Emitters proxy loader
		RpcProxy<List<String>> emitters_proxy = new RpcProxy<List<String>>() {
			@Override
			protected void load(Object loadConfig, final AsyncCallback<List<String>> callback) {
				gwtMultitonService.getRegisteredEmitters(new AsyncCallback<List<String>>() {

					@Override
					public void onFailure(Throwable caught) {
						FailureHandler.handle(caught);
					}

					@Override
					public void onSuccess(List<String> result) {
						callback.onSuccess(result);
					}
				});
			}
		};

		//Receivers proxy loader
		RpcProxy<List<String>> receivers_proxy = new RpcProxy<List<String>>() {
			@Override
			protected void load(Object loadConfig, final AsyncCallback<List<String>> callback) {
				gwtMultitonService.getRegisteredReceivers(new AsyncCallback<List<String>>() {

					@Override
					public void onFailure(Throwable caught) {
						FailureHandler.handle(caught);
					}

					@Override
					public void onSuccess(List<String> result) {
						callback.onSuccess(result);
					}
				});
			}
		};

		BaseLoader<List<String>> factories_loader = new BaseLoader<List<String>>(factories_proxy);
		factories_loader.addListener(BaseLoader.Load, new Listener<LoadEvent>() {

			@Override
			public void handleEvent(LoadEvent be) {
				if (be.getData() instanceof List<?>) {
					List<String> data = (List<String>) be.getData();
					m_factories.clear();
					for (String s : data) {
						m_factories.addItem(s);
					}
				}
			}
		});
		factories_loader.load();

		BaseLoader<List<String>> emitters_loader = new BaseLoader<List<String>>(factories_proxy);
		emitters_loader.addListener(BaseLoader.Load, new Listener<LoadEvent>() {

			@Override
			public void handleEvent(LoadEvent be) {
				if (be.getData() instanceof List<?>) {
					List<String> data = (List<String>) be.getData();
					m_emitters.clear();
					for(String s : data){
						m_emitters.addItem(s);
					}
				}
			}
		});
		emitters_loader.load();

		BaseLoader<List<String>> receivers_loader = new BaseLoader<List<String>>(factories_proxy);
		receivers_loader.addListener(BaseLoader.Load, new Listener<LoadEvent>() {

			@Override
			public void handleEvent(LoadEvent be) {
				if (be.getData() instanceof List<?>) {
					List<String> data = (List<String>) be.getData();
					m_emitters.clear();
					for(String s : data){
						m_emitters.addItem(s);
					}
				}
			}
		});
		receivers_loader.load();

		m_commandInput = m_formPanel;

		// Main Panel
		ContentPanel deviceCommandPanel = new ContentPanel();
		deviceCommandPanel.setBorders(false);
		deviceCommandPanel.setBodyBorder(false);
		deviceCommandPanel.setHeaderVisible(false);
		deviceCommandPanel.setScrollMode(Scroll.AUTO);
		deviceCommandPanel.setLayout(new FitLayout());
		deviceCommandPanel.add(m_commandInput);

		add(deviceCommandPanel);

	}

	private void initButtonBar() {
		m_executeButton = new Button(MSGS.deviceCommandExecute());
		m_executeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				if (m_formPanel.isValid()) {
					m_formPanel.submit();
				}
			}
		});

		m_buttonBar.add(m_executeButton);
	}

	public void refresh() {
		// do nothing
	}

}
