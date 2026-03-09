/*
 * Copyright 2026 Axel Howind - axh@dua3.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cabe022;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JCL SPI implementation for SLB4J.
 */
public final class LogFactoryJcl extends LogFactory {

    private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Constructor.
     */
    public LogFactoryJcl() { /* nothing to do */ }

    @Override
    public @Nullable Object getAttribute(String name) {
        return null;
    }

    @Override
    public String[] getAttributeNames() {
        return null;
    }

    @Override
    public Log getInstance(Class<?> clazz) throws LogConfigurationException {
        return null;
    }

    @Override
    public Log getInstance(String name) throws LogConfigurationException {
        return null;
    }

    @Override
    public void release() { /* nothing to do */ }

    @Override
    public void removeAttribute(String name) { /* nothing to do */ }

    @Override
    public void setAttribute(String name, @Nullable Object value) { /* nothing to do */ }
}
