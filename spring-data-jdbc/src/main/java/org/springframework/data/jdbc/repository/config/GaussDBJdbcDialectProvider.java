/*
 * Copyright 2019-2025 the original author or authors.
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
package org.springframework.data.jdbc.repository.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.jdbc.core.dialect.JdbcGaussDBDialect;
import org.springframework.data.jdbc.repository.config.DialectResolver.JdbcDialectProvider;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.lang.Nullable;

public class GaussDBJdbcDialectProvider implements JdbcDialectProvider {
  private static final Log LOG = LogFactory.getLog(GaussDBJdbcDialectProvider.class);

  @Override
  public Optional<Dialect> getDialect(JdbcOperations operations) {
    return Optional.ofNullable(operations.execute((ConnectionCallback<Dialect>) GaussDBJdbcDialectProvider::getDialect));
  }

  @Nullable
  private static Dialect getDialect(Connection connection) throws SQLException {
    DatabaseMetaData metaData = connection.getMetaData();

    String name = metaData.getDatabaseProductName().toLowerCase(Locale.ENGLISH);

    if (name.contains("gaussdb")) {
      return JdbcGaussDBDialect.INSTANCE;
    }
    LOG.info(String.format("Couldn't determine Dialect for \"%s\"", name));
    return null;
  }
}
