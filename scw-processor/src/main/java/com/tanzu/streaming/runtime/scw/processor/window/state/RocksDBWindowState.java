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
package com.tanzu.streaming.runtime.scw.processor.window.state;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.array.BytePrimitiveArraySerializer;
import org.apache.flink.core.memory.DataOutputSerializer;
import org.apache.flink.runtime.state.ListDelimitedSerializer;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.StringAppendOperator;

import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

public class RocksDBWindowState implements State, AutoCloseable {

    public static final byte[] TIMESTAMP_PREFIX = "TS:".getBytes();
    public static final byte[] HEADERS_PREFIX = "HD:".getBytes();

    private static final String DEFAULT_DB_PATH = "/tmp/rocksdb-data/";
    private static final String cfdbPath = "./rocksdb-data-cf/";

    private final TypeSerializer<byte[]> typeSerializer;
    private final ListDelimitedSerializer listDelimitedSerializer;
    private final ThreadLocal<DataOutputSerializer> dataOutputViewThreadLocal;
    private final ConcurrentHashMap<Long, Object> keys;
    private final String databasePath;

    private Options options;
    private RocksDB db;

    public RocksDBWindowState() {
        this(DEFAULT_DB_PATH);
    }

    public RocksDBWindowState(String databasePath) {

        this.databasePath = (StringUtils.hasText(databasePath)) ? databasePath : DEFAULT_DB_PATH;

        this.typeSerializer = BytePrimitiveArraySerializer.INSTANCE;
        this.listDelimitedSerializer = new ListDelimitedSerializer();
        this.dataOutputViewThreadLocal = ThreadLocal.withInitial(() -> new DataOutputSerializer(128));
        this.keys = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void initialize() {
        RocksDB.loadLibrary();
        this.options = new Options()
                .setCreateIfMissing(true)
                .useFixedLengthPrefixExtractor(0)
                // .setDbLogDir(dbpath + "/logs/")
                .setMergeOperator(new StringAppendOperator());
        this.options.getEnv().setBackgroundThreads(2);
        try {
            this.db = RocksDB.open(this.options, this.databasePath);
            this.retrieveStoredState();
        }
        catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void preDestroy() throws IOException {
        this.close();
    }

    public void retrieveStoredState() {
        // retrieve
        RocksIterator iterator = this.db.newIterator();

        iterator.seekToFirst();
        iterator.seek(TIMESTAMP_PREFIX);
        while (iterator.isValid()) {
            byte[] key = iterator.key();
            byte[] value = iterator.value();

            // if (key.length == 8) {
            this.keys.putIfAbsent(bytesToLong(key), Void.TYPE);
            // }
            iterator.next();
            System.out.println(">>> Key len:" + key.length);
        }
        System.out.println("Keys Size: " + keys.size());
    }

    @Override
    public Set<Long> keys() {
        return this.keys.keySet();
    }

    @Override
    public void put(long windowStartTimeNs, Map<String, Object> headers, byte[] payload) {
        try {
            this.typeSerializer.serialize(payload, this.dataOutputViewThreadLocal.get());
            this.db.merge(withTimestampPrefix(longToBytes(windowStartTimeNs)),
                    this.dataOutputViewThreadLocal.get().getCopyOfBuffer());
            this.dataOutputViewThreadLocal.get().clear();

            this.keys.putIfAbsent(windowStartTimeNs, Void.TYPE);

            if (this.db.get(withHeadersPrefix(longToBytes(windowStartTimeNs))) == null) {
                Map<Object, Object> filteredHeaders = headers.entrySet().stream()
                        .filter(es -> es.getValue().getClass().isPrimitive() || es.getValue().getClass().isEnum()
                                || (es.getValue() instanceof String)
                                || (es.getValue() instanceof Number))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                // filteredHeaders.entrySet().stream()
                // .forEach(es -> System.out.println(es.getKey() + ":" + es.getKey().getClass().getSimpleName()
                // + " - " + es.getValue() + " : " + es.getValue().getClass().getSimpleName()));
                byte[] headerBytes = SerializationUtils.serialize(filteredHeaders);
                this.db.put(withHeadersPrefix(longToBytes(windowStartTimeNs)), headerBytes);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public StateEntry get(long timestamp) {
        return this.internalGet(timestamp);
    }

    @Override
    public StateEntry delete(long timestamp) {
        try {
            StateEntry stateEntry = this.internalGet(timestamp);
            this.db.delete(withTimestampPrefix(longToBytes(timestamp)));
            this.db.delete(withHeadersPrefix(longToBytes(timestamp)));
            this.keys.remove(timestamp);
            return stateEntry;
        }
        catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    private StateEntry internalGet(long timestamp) {
        try {
            byte[] encodedPayloads = this.db.get(withTimestampPrefix(longToBytes(timestamp)));
            if (encodedPayloads != null) {
                List<byte[]> payloads = this.listDelimitedSerializer.deserializeList(encodedPayloads,
                        this.typeSerializer);

                byte[] headerBytes = this.db.get(withHeadersPrefix(longToBytes(timestamp)));
                Map<String, Object> headers = (Map<String, Object>) SerializationUtils.deserialize(headerBytes);

                return new StateEntry(timestamp, headers, payloads);
            }
        }
        catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() {
        if (this.db != null) {
            this.db.close();
        }
        if (this.options != null) {
            this.options.close();
        }
        this.keys.clear();
    }

    public byte[] longToBytes(long longValue) {
        byte[] result = new byte[Long.BYTES];
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            result[i] = (byte) (longValue & 0xFF);
            longValue >>= Byte.SIZE;
        }
        return result;
    }

    private long bytesToLong(final byte[] bytes) {
        long result = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            result <<= Byte.SIZE;
            result |= (bytes[i] & 0xFF);
        }
        return result;
    }

    private byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private byte[] withTimestampPrefix(byte[] data) {
        return concat(TIMESTAMP_PREFIX, data);
    }

    private byte[] withHeadersPrefix(byte[] data) {
        return concat(HEADERS_PREFIX, data);
    }
}
