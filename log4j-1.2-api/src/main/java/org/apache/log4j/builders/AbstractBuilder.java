/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.log4j.builders;

import static org.apache.log4j.xml.XmlConfiguration.NAME_ATTR;
import static org.apache.log4j.xml.XmlConfiguration.VALUE_ATTR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

/**
 * Base class for Log4j 1 component builders.
 */
public abstract class AbstractBuilder {

    private static Logger LOGGER = StatusLogger.getLogger();
    protected static final String FILE_PARAM = "File";
    protected static final String APPEND_PARAM = "Append";
    protected static final String BUFFERED_IO_PARAM = "BufferedIO";
    protected static final String BUFFER_SIZE_PARAM = "BufferSize";
    protected static final String MAX_SIZE_PARAM = "MaxFileSize";
    protected static final String MAX_BACKUP_INDEX = "MaxBackupIndex";
    protected static final String RELATIVE = "RELATIVE";

    private final String prefix;
    private final Properties properties;

    public AbstractBuilder() {
        this.prefix = null;
        this.properties = new Properties();
    }

    public AbstractBuilder(String prefix, Properties props) {
        this.prefix = prefix + ".";
        this.properties = (Properties) props.clone();
        Map<String, String> map = new HashMap<>();
        System.getProperties().forEach((k, v) -> map.put(k.toString(), v.toString()));
        props.forEach((k, v) -> map.put(k.toString(), v.toString()));
        // normalize keys to lower case for case-insensitive access.
        props.forEach((k, v) -> map.put(toLowerCase(k.toString()), v.toString()));
        props.entrySet().forEach(e -> this.properties.put(toLowerCase(e.getKey().toString()), e.getValue()));
    }

    public String getProperty(String key) {
        return getProperty(key, null);
    }

    public String getProperty(String key, String defaultValue) {
        String fullKey = prefix + key;
        String value = properties.getProperty(fullKey);
        value = value != null ? value : properties.getProperty(toLowerCase(fullKey), defaultValue);
        value = value == null ? defaultValue : substVars(value);
        return value == null ? defaultValue : value;
    }

    protected String getNameAttribute(Element element) {
        return element.getAttribute(NAME_ATTR);
    }

    protected String getValueAttribute(Element element) {
        return substVars(element.getAttribute(VALUE_ATTR));
    }

    public boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(getProperty(key, Boolean.FALSE.toString()));
    }

    public int getIntegerProperty(String key, int defaultValue) {
        String value = null;
        try {
            value = getProperty(key);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (Exception ex) {
            LOGGER.warn("Error converting value {} of {} to an integer: {}", value, key, ex.getMessage());
        }
        return defaultValue;
    }

    public Properties getProperties() {
        return properties;
    }


    protected org.apache.logging.log4j.core.Filter buildFilters(String level, Filter filter) {
        if (level != null && filter != null) {
            List<org.apache.logging.log4j.core.Filter> filterList = new ArrayList<>();
            org.apache.logging.log4j.core.Filter thresholdFilter =
                    ThresholdFilter.createFilter(OptionConverter.convertLevel(level, Level.TRACE),
                            org.apache.logging.log4j.core.Filter.Result.NEUTRAL,
                            org.apache.logging.log4j.core.Filter.Result.DENY);
            filterList.add(thresholdFilter);
            Filter f = filter;
            while (f != null) {
                if (filter instanceof FilterWrapper) {
                    filterList.add(((FilterWrapper) f).getFilter());
                } else {
                    filterList.add(new FilterAdapter(f));
                }
                f = f.getNext();
            }
            return CompositeFilter.createFilters(filterList.toArray(org.apache.logging.log4j.core.Filter.EMPTY_ARRAY));
        } else if (level != null) {
            return ThresholdFilter.createFilter(OptionConverter.convertLevel(level, Level.TRACE),
                    org.apache.logging.log4j.core.Filter.Result.NEUTRAL,
                    org.apache.logging.log4j.core.Filter.Result.DENY);
        } else if (filter != null) {
            if (filter instanceof FilterWrapper) {
                return ((FilterWrapper) filter).getFilter();
            }
            return new FilterAdapter(filter);
        }
        return null;
    }

    protected String substVars(String value) {
        return OptionConverter.substVars(value, properties);
    }

    String toLowerCase(final String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

}
