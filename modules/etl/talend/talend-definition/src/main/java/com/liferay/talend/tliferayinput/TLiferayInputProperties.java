/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.talend.tliferayinput;

import com.liferay.talend.LiferayBaseComponentDefinition;
import com.liferay.talend.connection.LiferayConnectionProperties;
import com.liferay.talend.connection.LiferayConnectionResourceBaseProperties;
import com.liferay.talend.exception.ExceptionUtils;
import com.liferay.talend.runtime.LiferaySourceOrSinkRuntime;

import java.io.IOException;

import java.util.Collections;
import java.util.Set;

import org.apache.avro.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.talend.components.api.component.PropertyPathConnector;
import org.talend.daikon.SimpleNamedThing;
import org.talend.daikon.properties.PresentationItem;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.ValidationResult.Result;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;
import org.talend.daikon.properties.property.Property;
import org.talend.daikon.properties.property.PropertyFactory;
import org.talend.daikon.sandbox.SandboxedInstance;

/**
 * @author Zoltán Takács
 */
public class TLiferayInputProperties
	extends LiferayConnectionResourceBaseProperties {

	public TLiferayInputProperties(String name) {
		super(name);
	}

	/**
	 * Refreshes form after "Guess Schema" button was processed
	 */
	public void afterGuessSchema() {
		if (_log.isDebugEnabled()) {
			_log.debug(
				"Selected resource URL: " + resource.resourceURL.getValue());
			_log.debug("Query string: " + queryString.getValue());
		}

		refreshLayout(getForm(Form.MAIN));
	}

	@Override
	public void refreshLayout(Form form) {
		super.refreshLayout(form);

		boolean hideDevWidgets = true;

		String formName = form.getName();

		if (formName.equals(Form.MAIN) ||
			formName.equals(LiferayConnectionProperties.FORM_WIZARD)) {

			_setHidden(form, guessSchema, hideDevWidgets);
			_setHidden(form, queryString, hideDevWidgets);
		}
	}

	@Override
	public void setupLayout() {
		super.setupLayout();

		Form mainForm = getForm(Form.MAIN);

		mainForm.addRow(queryString);

		Widget guessButtonWidget = Widget.widget(guessSchema);

		guessButtonWidget.setLongRunning(true);
		guessButtonWidget.setWidgetType(Widget.BUTTON_WIDGET_TYPE);

		mainForm.addRow(guessButtonWidget);
	}

	@Override
	public void setupProperties() {
		super.setupProperties();

		queryString.setValue("");
	}

	public ValidationResult validateGuessSchema() {
		try (SandboxedInstance sandboxedInstance =
				LiferayBaseComponentDefinition.getSandboxedInstance(
					LiferayBaseComponentDefinition.
						RUNTIME_SOURCE_OR_SINK_CLASS_NAME)) {

			LiferaySourceOrSinkRuntime liferaySourceOrSinkRuntime =
				(LiferaySourceOrSinkRuntime)sandboxedInstance.getInstance();

			ValidationResult validationResult =
				liferaySourceOrSinkRuntime.initialize(
					null, _getEffectiveConnectionProperties());

			if (validationResult.getStatus() == Result.ERROR) {
				return validationResult;
			}

			validationResult = liferaySourceOrSinkRuntime.validate(null);

			if (validationResult.getStatus() == ValidationResult.Result.OK) {
				try {
					Schema runtimeSchema =
						liferaySourceOrSinkRuntime.
							getInputResourceCollectionSchema(
								resource.resourceURL.getValue());

					resource.main.schema.setValue(runtimeSchema);
				}
				catch (IOException ioe) {
					ExceptionUtils.exceptionToValidationResult(ioe);
				}
			}

			return validationResult;
		}
	}

	public transient PresentationItem guessSchema = new PresentationItem(
		"guessSchema", "Guess Schema");
	public Property<String> queryString = PropertyFactory.newProperty(
		"queryString");

	@Override
	protected Set<PropertyPathConnector> getAllSchemaPropertiesConnectors(
		boolean outputConnectors) {

		if (outputConnectors) {
			return Collections.singleton(mainConnector);
		}

		return Collections.<PropertyPathConnector>emptySet();
	}

	private LiferayConnectionProperties _getEffectiveConnectionProperties() {
		LiferayConnectionProperties liferayConnectionProperties =
			getLiferayConnectionProperties();

		if (liferayConnectionProperties == null) {
			_log.error("LiferayConnectionProperties is null");
		}

		LiferayConnectionProperties referencedLiferayConnectionProperties =
			liferayConnectionProperties.getReferencedConnectionProperties();

		if (referencedLiferayConnectionProperties != null) {
			if (_log.isDebugEnabled()) {
				_log.debug("Using a reference connection properties.");
				_log.debug(
					"User ID: " +
						referencedLiferayConnectionProperties.userId.
							getValue());
				_log.debug(
					"Endpoint: " +
						referencedLiferayConnectionProperties.endpoint.
							getValue());
			}

			return referencedLiferayConnectionProperties;
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"User ID: " + liferayConnectionProperties.userId.getValue());
			_log.debug(
				"Endpoint: " + liferayConnectionProperties.endpoint.getValue());
		}

		return liferayConnectionProperties;
	}

	private void _setHidden(
		Form form, SimpleNamedThing simpleNamedThing, boolean hidden) {

		Widget widget = form.getWidget(simpleNamedThing.getName());

		widget.setHidden(hidden);
	}

	private static final Logger _log = LoggerFactory.getLogger(
		TLiferayInputProperties.class);

	private static final long serialVersionUID = 8010931662185868407L;

}