/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.csp.sentinel.datasource.etcd;

import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;

/**
 * This class provide a builder to build etcd connection config.
 *
 * @author tiger
 */
public class EtcdConnectionConfig {

    private String endpoints;
    private boolean authEnable;
    private String authority;
    private String charset;
    private String user;
    private String password;

    /**
     * Default empty constructor.
     */
    public EtcdConnectionConfig() {
    }

    /**
     * Constructor with endpoints.
     *
     * @param endpoints  the endpoints
     */
    public EtcdConnectionConfig(String endpoints) {

        AssertUtil.notEmpty(endpoints, "Endpoints must not be empty");

        setEndPoints(endpoints);
    }

    /**
     * Returns a new {@link EtcdConnectionConfig.Builder} to construct a {@link EtcdConnectionConfig}.
     *
     * @return a new {@link EtcdConnectionConfig.Builder} to construct a {@link EtcdConnectionConfig}.
     */
    public static EtcdConnectionConfig.Builder builder() {
        return new EtcdConnectionConfig.Builder();
    }

    public String getEndPoints() {
        return endpoints;
    }

    public void setEndPoints(String endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Returns the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password. Use empty string to skip authentication.
     *
     * @param password the password, must not be {@literal null}.
     */
    public void setPassword(String password) {

        AssertUtil.notNull(password, "Password must not be null");
        this.password = password;
    }

    /**
     * Returns the username.
     *
     * @return
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the username to be applied on Etcd connections.
     *
     * @param user the username.
     */
    public void setUser(String user) {
        this.user = user;
    }



    /**
     * Builder for Etcd EtcdConnectionConfig.
     */
    public static class Builder {

        private String endpoints;
        private String user;
        private String password;
        private boolean authEnable;
        private String authority;
        private String charset;

        private Builder() {
        }

        /**
         * Set Etcd endpoints. Creates a new builder.
         *
         * @param endpoints the endpoints
         * @return New builder with Etcd endpoints.
         */
        public static EtcdConnectionConfig.Builder etcd(String endpoints) {

            AssertUtil.notEmpty(endpoints, "Host must not be empty");

            Builder builder = EtcdConnectionConfig.builder();
            return builder.withEndpoints(endpoints);
        }

        /**
         * Set Etcd host and port. Creates a new builder
         *
         * @param host the host name
         * @param port the port
         * @return New builder with Etcd host/port.
         */
        public static EtcdConnectionConfig.Builder etcd(String host, int port) {

            AssertUtil.notEmpty(host, "Host must not be empty");
            AssertUtil.isTrue(isValidPort(port), String.format("Port out of range: %s", port));

            Builder builder = EtcdConnectionConfig.builder();
            String endpoint = "http://".concat(host).concat(":").concat(String.valueOf(port));
            return builder.withEndpoints(endpoint);
        }

        /**
         * Configures Etcd endpoints.
         *
         * @param endpoints endpoints must not be empty or {@literal null}
         * @return the builder
         */
        public EtcdConnectionConfig.Builder withEndpoints(String endpoints) {

            AssertUtil.notEmpty(endpoints, "Etcd endpoints must not empty");

            this.endpoints = endpoints;
            return this;
        }

        /**
         * Configures a username.
         *
         * @param user the username
         * @return the builder
         */
        public EtcdConnectionConfig.Builder withUser(String user) {

            AssertUtil.notNull(user, "User name must not be null");

            this.user = user;
            return this;
        }

        /**
         * Configures password.
         *
         * @param password the password
         * @return the builder
         */
        public EtcdConnectionConfig.Builder withPassword(String password) {

            AssertUtil.notNull(password, "Password must not be null");

            this.password = password;
            return this;
        }

        /**
         * Sets the authEnable.
         *
         * @param authEnable authEnable
         * @return the value of Builder
         */
        public EtcdConnectionConfig.Builder withAuthEnable(boolean authEnable) {
            this.authEnable = authEnable;
            return this;
        }

        /**
         * Sets the authority.
         *
         * @param authority authority
         * @return the value of Builder
         */
        public EtcdConnectionConfig.Builder withAuthority(String authority) {

            AssertUtil.notEmpty(authority, "authority must not empty");

            this.authority = authority;
            return this;
        }

        /**
         * Sets the charset.
         *
         * @param charset charset
         * @return the value of Builder
         */
        public EtcdConnectionConfig.Builder withCharset(String charset) {
            this.charset = charset;
            return this;
        }

        /**
         * @return the EtcdConnectionConfig.
         */
        public EtcdConnectionConfig build() {

            if (StringUtil.isEmpty(endpoints)) {
                throw new IllegalStateException(
                    "Cannot build a EtcdConnectionConfig. One of the following must be provided endpoints");
            }

            EtcdConnectionConfig redisConnectionConfig = new EtcdConnectionConfig();

            redisConnectionConfig.setEndPoints(endpoints);
            if(authEnable) {
                redisConnectionConfig.setAuthEnable(true);
                redisConnectionConfig.setPassword(password);
                redisConnectionConfig.setUser(user);
                redisConnectionConfig.setAuthority(authority);
                redisConnectionConfig.setCharset(charset);
            }

            return redisConnectionConfig;
        }
    }

    /**
     * Return true for valid port numbers.
     */
    private static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535;
    }

    /**
     * Gets the value of authority.
     *
     * @return the value of authority
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * Sets the authority.
     * <p>
     * <p>You can use getAuthority() to get the value of authority</p>
     *
     * @param authority authority
     */
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    /**
     * Gets the value of charset.
     *
     * @return the value of charset
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Sets the charset.
     * <p>
     * <p>You can use getCharset() to get the value of charset</p>
     *
     * @param charset charset
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * Sets the authEnable.
     * <p>
     * <p>You can use isAuthEnable() to get the value of authEnable</p>
     *
     * @param authEnable authEnable
     */
    public void setAuthEnable(boolean authEnable) {
        this.authEnable = authEnable;
    }


    /**
     * Gets the value of authEnable.
     *
     * @return the value of authEnable
     */
    public boolean isAuthEnable() {
        return authEnable;
    }
}
