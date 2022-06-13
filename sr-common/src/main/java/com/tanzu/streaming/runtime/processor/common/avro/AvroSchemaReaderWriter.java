/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tanzu.streaming.runtime.processor.common.avro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * Utility that help to read and write Avro GenericRecords with provided Avro Schema.
 */
public class AvroSchemaReaderWriter {

    private static final Log logger = LogFactory.getLog(AvroSchemaReaderWriter.class);

    private final Schema schema;

    private final GenericDatumReader<GenericRecord> datumReader;

    private final GenericDatumWriter<GenericRecord> datumWriter;

    private AvroSchemaReaderWriter(Schema schema) {
        this.schema = schema;
        this.datumReader = new GenericDatumReader<GenericRecord>(schema);
        this.datumWriter = new GenericDatumWriter<GenericRecord>(schema);
    }

    public static AvroSchemaReaderWriter from(Schema schema) {
        return new AvroSchemaReaderWriter(schema);
    }

    public static AvroSchemaReaderWriter from(String jsonAvroSchema) {
        return new AvroSchemaReaderWriter(new org.apache.avro.Schema.Parser().parse(jsonAvroSchema));
    }

    public static AvroSchemaReaderWriter from(Resource jsonAvroSchemaUri) {

        try {
            String jsonAvroSchema = StreamUtils.copyToString(jsonAvroSchemaUri.getInputStream(),
                    Charset.forName("UTF-8"));
            return from(jsonAvroSchema);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] writeRecord(GenericRecord record) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        try {
            this.datumWriter.write(record, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to write GenericRecord!", e);
        }
    }

    public void writeRecords(List<GenericRecord> records, OutputStream outputStream) {
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);

        for (GenericRecord record : records) {
            try {
                this.datumWriter.write(record, encoder);
            }
            catch (IOException e) {
                logger.warn("Failed to write GenericRecord! Skip it and continue with the rest", e);
            }
        }
        try {
            encoder.flush();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to write GenericRecord!", e);
        }
    }

    public GenericRecord readRecord(byte[] data) {
        
        if (data != null && data.length > 5 && data[0] == 0) {            
            // Remove Confluent Schema Registry Metadata
			data = Arrays.copyOfRange(data, 5, data.length);
        }

        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(new SeekableByteArrayInput(data), null);
        try {
            return this.datumReader.read(null, decoder);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to read byte data into GenericRecord!", e);
        }
    }

    public Schema getSchema() {
        return schema;
    }

    public static Resource toResource(String uri) {
        return new DefaultResourceLoader().getResource(uri);
    }
}
