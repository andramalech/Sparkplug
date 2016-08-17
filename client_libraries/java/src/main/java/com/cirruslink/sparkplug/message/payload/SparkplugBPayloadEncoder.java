/*
 * Licensed Materials - Property of Cirrus Link Solutions
 * Copyright (c) 2016 Cirrus Link Solutions LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.cirruslink.sparkplug.message.payload;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.cirruslink.sparkplug.message.model.DataSet;
import com.cirruslink.sparkplug.message.model.File;
import com.cirruslink.sparkplug.message.model.MetaData;
import com.cirruslink.sparkplug.message.model.Metric;
import com.cirruslink.sparkplug.message.model.Row;
import com.cirruslink.sparkplug.message.model.Value;
import com.cirruslink.sparkplug.message.model.ValueDataType;
import com.cirruslink.sparkplug.message.protobuf.SparkplugBProto;
import com.google.protobuf.ByteString;

public class SparkplugBPayloadEncoder implements PayloadEncoder <SparkplugBPayload> {
	
	private static Logger logger = LogManager.getLogger(SparkplugBPayloadEncoder.class.getName());
	
	public SparkplugBPayloadEncoder() {
		super();
	}
	
	public byte[] getBytes(SparkplugBPayload payload) throws IOException {
		
		SparkplugBProto.Payload.Builder protoMsg = SparkplugBProto.Payload.newBuilder();
		
		// Set the timestamp
		if (payload.getTimestamp() != null) {
			logger.debug("Setting time " + payload.getTimestamp());
			protoMsg.setTimestamp(payload.getTimestamp().getTime());
		}
		
		// Set the sequence number
		logger.debug("Setting sequence number " + payload.getSeq());
		protoMsg.setSeq(payload.getSeq());
		
		// Set the UUID if defined
		if (payload.getUuid() != null) {
			logger.debug("Setting the UUID " + payload.getUuid());
			protoMsg.setUuid(payload.getUuid());
		}
		
		// Set the metrics
		for (Metric metric : payload.getMetrics()) {
			
			// build a metric
			SparkplugBProto.Payload.Metric.Builder metricBuilder = SparkplugBProto.Payload.Metric.newBuilder();
			
			try {
				// set the basic parameters
				logger.debug("Adding metric: " + metric.getName());
				metricBuilder.setName(metric.getName());
				metricBuilder.setAlias(metric.getAlias());
				metricBuilder.setDatatype(metric.getDataType());
				if(metric.getTimestamp() != null) {
					metricBuilder.setTimestamp(metric.getTimestamp().getTime());
				}
				
				// Set the value and metadata
				metricBuilder = setMetricValue(metricBuilder, metric);
				if(metric.getMetaData() != null) {
					logger.debug("Metadata is not null");
					metricBuilder = setMetaData(metricBuilder, metric);
				}
				
				protoMsg.addMetric(metricBuilder);
			} catch(Exception e) {
				logger.error("Failed to add metric: " + metric.getName());
				throw new RuntimeException(e);
			}
		}
		

		// Set the body
		if (payload.getBody() != null) {
			logger.debug("Setting the body " + new String(payload.getBody()));
			protoMsg.setBody(ByteString.copyFrom(payload.getBody()));
		}

		return protoMsg.build().toByteArray();
	}
	
	private SparkplugBProto.Payload.Metric.Builder setMetricValue(SparkplugBProto.Payload.Metric.Builder metricBuilder,
			Metric metric) throws Exception {
		
		// Set the datatype
		metricBuilder.setDatatype(metric.getDataType());
		
		// Set the value
		if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Unknown)) {
			logger.error("Unknown DataType: " + metric.getDataType());
			throw new Exception("Failed to encode");
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Int1)) {
			metricBuilder.setIntValue((Integer) metric.getValue());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Int2)) {
			metricBuilder.setIntValue((Integer) metric.getValue());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Int4)) {
			metricBuilder.setIntValue((Integer) metric.getValue());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Int8)) {
			metricBuilder.setLongValue((Long) metric.getValue());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Float4)) {
			metricBuilder.setFloatValue((Float) metric.getValue());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Float8)) {
			metricBuilder.setDoubleValue((Double) metric.getValue());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Boolean)) {
			metricBuilder.setBooleanValue((Boolean) metric.getValue());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.String)) {
			metricBuilder.setStringValue((String) metric.getValue());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.DateTime)) {
			metricBuilder.setLongValue(((Date)metric.getValue()).getTime());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Dataset)) {
			DataSet dataSet = (DataSet) metric.getValue();
			SparkplugBProto.Payload.Metric.DataSet.Builder protoDataSetBuilder = SparkplugBProto.Payload.Metric.DataSet.newBuilder();
			
			protoDataSetBuilder.setNumOfColumns(dataSet.getNumOfColumns());
			
			List<Value<?>> columns = dataSet.getColumns();
			if(columns != null && !columns.isEmpty()) {
				for(Value<?> column : columns) {
					// Add the converted column
					protoDataSetBuilder.addColumns(convertValue(column));
				}
			} else {
				throw new Exception("Invalid DataSet");
			}
			
			List<Row> rows = dataSet.getRows();
			if(rows != null && !rows.isEmpty()) {
				for(Row row : rows) {
					SparkplugBProto.Payload.Metric.DataSet.Row.Builder protoRowBuilder = SparkplugBProto.Payload.Metric.DataSet.Row.newBuilder();
					List<Value<?>> values = row.getValues();
					if(values != null && !values.isEmpty()) {
						for(Value<?> value : values) {
							// Add the converted element
							protoRowBuilder.addElement(convertValue(value));
						}
						
						logger.debug("Adding row");
						protoDataSetBuilder.addRows(protoRowBuilder);
					} else {
						throw new Exception("Invalid DataSet");
					}
				}
			}
			
			// Finally add the dataset
			logger.debug("Adding the dataset");
			metricBuilder.setDatasetValue(protoDataSetBuilder);
			
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Text)) {
			metricBuilder.setStringValue((String) metric.getValue());
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.Bytes)) {
			metricBuilder.setBytesValue(ByteString.copyFrom((byte[]) metric.getValue()));
		} else if(metric.getDataType().equals(SparkplugBProto.Payload.Metric.DataType.File)) {
			metricBuilder.setBytesValue(ByteString.copyFrom(((File) metric.getValue()).getBytes()));
			SparkplugBProto.Payload.Metric.MetaData.Builder metaDataBuilder = SparkplugBProto.Payload.Metric.MetaData.newBuilder();
			metaDataBuilder.setFileName(((File) metric.getValue()).getFileName());
			metricBuilder.setMetadata(metaDataBuilder);
		} else {
			logger.error("Unknown DataType: " + metric.getDataType());
			throw new Exception("Failed to encode");
		}
		
		return metricBuilder;
	}
	
	private SparkplugBProto.Payload.Metric.Builder setMetaData(SparkplugBProto.Payload.Metric.Builder metricBuilder,
			Metric metric) throws Exception {
		
		// If the builder has been built already - use it
		SparkplugBProto.Payload.Metric.MetaData.Builder metaDataBuilder;
		if(metricBuilder.getMetadataBuilder() != null) {
			metaDataBuilder = metricBuilder.getMetadataBuilder();
		} else {
			metaDataBuilder = SparkplugBProto.Payload.Metric.MetaData.newBuilder();
		}
		
		MetaData metaData = metric.getMetaData();
		if(metaData.getUnits() != null) {
			metaDataBuilder.setUnits(metaData.getUnits());
		}
		if(metaData.getContentType() != null) {
			metaDataBuilder.setContentType(metaData.getContentType());
		}
		metaDataBuilder.setSize(metaData.getSize());
		if(metaData.getAlgorithm() != null) {
			metaDataBuilder.setAlgorithm(metaData.getAlgorithm());
		}
		if(metaData.getFormat() != null) {
			metaDataBuilder.setFormat(metaData.getFormat());
		}
		metaDataBuilder.setSeq(metaData.getSeq());
		if(metaData.getFileName() != null) {
			metaDataBuilder.setFileName(metaData.getFileName());
		}
		if(metaData.getFileType() != null) {
			metaDataBuilder.setFileType(metaData.getFileType());
		}
		if(metaData.getMd5() != null) {
			metaDataBuilder.setMd5(metaData.getMd5());
		}
		if(metaData.getDescription() != null) {
			metaDataBuilder.setDescription(metaData.getDescription());
		}
		metricBuilder.setMetadata(metaDataBuilder);
		
		return metricBuilder;
	}
	
	private SparkplugBProto.Payload.Metric.DataSet.Value.Builder convertValue(Value<?> value) throws Exception {
		SparkplugBProto.Payload.Metric.DataSet.Value.Builder protoValueBuilder = SparkplugBProto.Payload.Metric.DataSet.Value.newBuilder();
		
		if(value.getType() == ValueDataType.Unknown) {
			logger.error("Unknown DataType: " + value.getType());
			throw new Exception("Failed to convert value " + value.getType());
		} else if(value.getType() == ValueDataType.Int1) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.Int1);
			protoValueBuilder.setIntValue((Integer) value.getValue());
		} else if(value.getType() == ValueDataType.Int2) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.Int2);
			protoValueBuilder.setIntValue((Integer) value.getValue());
		} else if(value.getType() == ValueDataType.Int4) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.Int4);
			protoValueBuilder.setIntValue((Integer) value.getValue());
		} else if(value.getType() == ValueDataType.Int8) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.Int8);
			protoValueBuilder.setLongValue((Long) value.getValue());
		} else if(value.getType() == ValueDataType.Float4) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.Float4);
			protoValueBuilder.setFloatValue((Float) value.getValue());
		} else if(value.getType() == ValueDataType.Float8) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.Float8);
			protoValueBuilder.setDoubleValue((Double) value.getValue());
		} else if(value.getType() == ValueDataType.Boolean) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.Boolean);
			protoValueBuilder.setBooleanValue((Boolean) value.getValue());
		} else if(value.getType() == ValueDataType.String) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.String);
			protoValueBuilder.setStringValue((String) value.getValue());
		} else if(value.getType() == ValueDataType.DateTime) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.DateTime);
			protoValueBuilder.setLongValue(((Date) value.getValue()).getTime());
		} else if(value.getType() == ValueDataType.Text) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.String);
			protoValueBuilder.setStringValue((String) value.getValue());
		} else if(value.getType() == ValueDataType.Null) {
			protoValueBuilder.setType(SparkplugBProto.Payload.Metric.DataSet.Value.DataType.Null);
		} else {
			logger.error("Unknown DataType: " + value.getType());
			throw new Exception("Failed to convert value " + value.getType());
		}
		
		return protoValueBuilder;
	}
}