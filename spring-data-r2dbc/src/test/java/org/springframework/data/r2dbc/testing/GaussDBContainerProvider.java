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
package org.springframework.data.r2dbc.testing;

/**
 * Factory for GaussDB containers.
 */
@SuppressWarnings("rawtypes")
public class GaussDBContainerProvider extends org.testcontainers.containers.JdbcDatabaseContainerProvider {

    public static final String USER_PARAM = "user";

    public static final String PASSWORD_PARAM = "password";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(GaussDBContainer.NAME);
    }

    @Override
    public org.testcontainers.containers.JdbcDatabaseContainer newInstance() {
        return newInstance(GaussDBContainer.DEFAULT_TAG);
    }

    @Override
    public org.testcontainers.containers.JdbcDatabaseContainer newInstance(String tag) {
        return new GaussDBContainer(org.testcontainers.utility.DockerImageName.parse(GaussDBContainer.IMAGE).withTag(tag));
    }

    @Override
    public org.testcontainers.containers.JdbcDatabaseContainer newInstance(org.testcontainers.jdbc.ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }
}
