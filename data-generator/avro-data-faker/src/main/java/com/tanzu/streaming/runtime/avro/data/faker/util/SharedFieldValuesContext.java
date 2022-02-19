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

package com.tanzu.streaming.runtime.avro.data.faker.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.util.Assert;

/**
 * Allow field values generated from one data-faker schema to be used as random values for fields in for another.
 *
 *  - The field names in both schemas (the one from which the values are generated and
 *  the other where they are randomly used).
 *
 *  - The field values in the correlation context can be generated from multiple schemas.
 *  - The field values in the correlation context can be used in multiple target schemas.
 */
public class SharedFieldValuesContext {

	/**
	 * Holds the current state in form of fieldName -> ListOf Values (note that the value can be an array).
	 */
	private final ConcurrentHashMap<String, List<Object[]>> state = new ConcurrentHashMap<>();

	private final Lock readLock;
	private final Lock writeLock;
	private final Random random;

	public SharedFieldValuesContext() {
		this(new Random());
	}

	public SharedFieldValuesContext(Random random) {
		this.random = random;

		ReadWriteLock lock = new ReentrantReadWriteLock();
		this.writeLock = lock.writeLock();
		this.readLock = lock.readLock();
	}

	/**
	 * Add new value for the field.
	 * @param fieldName Filed name.
	 * @param objects Field value to add to the shared state. (Note that the value can be an array).
	 */
	public void addValue(String fieldName, Object... objects) {
		try {
			this.writeLock.lock();
			this.state.putIfAbsent(fieldName, new ArrayList<>());
			this.state.get(fieldName).add(objects);
		}
		finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Retrieve a random value from those collected in the context for the given field name..
	 * @param fieldName field name to retrieve value from the context's state.
	 * @return Return a random field value form the shared state.
	 */
	public Object field(String fieldName) {
		try {
			this.readLock.lock();

			List<Object[]> fieldValues = this.state.get(fieldName);

			Assert.notNull(fieldValues, "Could not find field values for field: " + fieldName);

			Object[] values = fieldValues.get(this.random.nextInt(fieldValues.size()));

			return (values.length == 1) ? values[0] : values;
		}
		finally {
			this.readLock.unlock();
		}
	}
}
