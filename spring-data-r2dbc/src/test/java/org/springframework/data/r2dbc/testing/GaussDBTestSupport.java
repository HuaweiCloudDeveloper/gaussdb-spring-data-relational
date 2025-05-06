/*
 * Copyright 2021-2025 the original author or authors.
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
package org.springframework.data.r2dbc.testing;

import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.data.r2dbc.testing.ExternalDatabase.ProvidedDatabase;

import com.huawei.gaussdb.jdbc.ds.PGSimpleDataSource;

import io.r2dbc.spi.ConnectionFactory;
/**
 * Utility class for testing against GaussDB.
 *
 * Notes: this file is token from PostgresTestSupport and add specific changes for GaussDB
 *
 * @author liubao
 */
public class GaussDBTestSupport {

	private static ExternalDatabase testContainerDatabase;

	public static String CREATE_TABLE_LEGOSET = "CREATE TABLE legoset (\n" //
			+ "    id          integer CONSTRAINT id1 PRIMARY KEY,\n" //
			+ "    version     integer NULL,\n" //
			+ "    name        varchar(255) NOT NULL,\n" //
			+ "    manual      integer NULL,\n" //
			+ "    flag        boolean NULL,\n" //
			+ "    cert        bytea NULL\n" //
			+ ");";

	public static String CREATE_TABLE_LEGOSET_WITH_ID_GENERATION = "CREATE TABLE legoset (\n" //
			+ "    id          serial CONSTRAINT id1 PRIMARY KEY,\n" //
			+ "    version     integer NULL,\n" //
			+ "    name        varchar(255) NOT NULL,\n" //
			+ "    extra       varchar(255),\n" //
			+ "    manual      integer NULL\n" //
			+ ");";

	public static final String CREATE_TABLE_LEGOSET_WITH_MIXED_CASE_NAMES = "CREATE TABLE \"LegoSet\" (\n" //
			+ "    \"Id\"          serial CONSTRAINT id2 PRIMARY KEY,\n" //
			+ "    \"Name\"        varchar(255) NOT NULL,\n" //
			+ "    \"Manual\"      integer NULL\n" //
			+ ");";

	public static final String DROP_TABLE_LEGOSET_WITH_MIXED_CASE_NAMES = "DROP TABLE \"LegoSet\"";

	/**
	 * Returns a database either hosted locally at {@code jdbc:postgres//localhost:5432/postgres} or running inside
	 * Docker.
	 *
	 * @return information about the database. Guaranteed to be not {@literal null}.
	 */
	public static ExternalDatabase database() {

		if (Boolean.getBoolean("spring.data.r2dbc.test.preferLocalDatabase")) {

			return getFirstWorkingDatabase( //
					GaussDBTestSupport::local, //
					GaussDBTestSupport::testContainer //
			);
		} else {

			return getFirstWorkingDatabase( //
					GaussDBTestSupport::testContainer, //
					GaussDBTestSupport::local //
			);
		}
	}

	@SafeVarargs
	private static ExternalDatabase getFirstWorkingDatabase(Supplier<ExternalDatabase>... suppliers) {

		return Stream.of(suppliers).map(Supplier::get) //
				.filter(ExternalDatabase::checkValidity) //
				.findFirst() //
				.orElse(ExternalDatabase.unavailable());
	}

	/**
	 * Returns a locally provided database at {@code postgres:@localhost:5432/postgres}.
	 */
	private static ExternalDatabase local() {

		return ProvidedDatabase.builder() //
				.hostname("localhost") //
				.port(8000) //
				.database("postgres") //
				.username(GaussDBContainer.DEFAULT_USER_NAME) //
				.password(GaussDBContainer.DEFAULT_PASSWORD) //
				.jdbcUrl("jdbc:gaussdb://localhost:8000/postgres") //
				.build();
	}

	/**
	 * Returns a database provided via Testcontainers.
	 */
	private static ExternalDatabase testContainer() {

		if (testContainerDatabase == null) {

			try {
				GaussDBContainer<?> container = new GaussDBContainer<>();
				container.start();

				testContainerDatabase = ProvidedDatabase.builder(container).database(container.getDatabaseName()).build();

			} catch (IllegalStateException ise) {
				// docker not available.
				testContainerDatabase = ExternalDatabase.unavailable();
			}

		}

		return testContainerDatabase;
	}

	/**
	 * Creates a new {@link ConnectionFactory} configured from the {@link ExternalDatabase}..
	 */
	public static ConnectionFactory createConnectionFactory(ExternalDatabase database) {
		return ConnectionUtils.getConnectionFactory("gaussdb", database);
	}

	/**
	 * Creates a new {@link DataSource} configured from the {@link ExternalDatabase}.
	 */
	public static DataSource createDataSource(ExternalDatabase database) {

		try {
			PGSimpleDataSource dataSource = new PGSimpleDataSource();

			dataSource.setUser(database.getUsername());
			dataSource.setPassword(database.getPassword());
			dataSource.setURL(database.getJdbcUrl());

			return dataSource;
		} catch (com.huawei.gaussdb.jdbc.util.PSQLException e) {
			throw new RuntimeException(e);
		}
	}
}
