/*
 * Licensed Materials - Property of Cirrus Link Solutions
 * Copyright (c) 2016 Cirrus Link Solutions LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.cirruslink.sparkplug.message.model;

import java.util.Objects;

import com.cirruslink.sparkplug.SparkplugInvalidTypeException;

/**
 * The value of a property in a Property Set.
 */
public class PropertyValue {
	
	private PropertyDataType type;
	private Object value;
	
	/**
	 * A constructor.
	 * 
	 * @param type the property type
	 * @param value the property value
	 * @throws SparkplugInvalidTypeException 
	 */
	public PropertyValue(PropertyDataType type, Object value) throws SparkplugInvalidTypeException {
		this.type = type;
		this.value = value;
		type.checkType(value);
	}

	public PropertyDataType getType() {
		return type;
	}

	public void setType(PropertyDataType type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || this.getClass() != object.getClass()) {
			return false;
		}
		PropertyValue propValue = (PropertyValue) object;
		return Objects.equals(type, propValue.getType())
				&& Objects.equals(value, propValue.getValue());
	}
}