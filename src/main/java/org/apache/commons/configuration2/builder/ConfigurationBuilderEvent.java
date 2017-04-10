/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration2.builder;

import org.apache.commons.configuration2.event.Event;
import org.apache.commons.configuration2.event.EventType;

/**
 * <p>
 * A base event class for events generated by a {@link ConfigurationBuilder}.
 * </p>
 * <p>
 * Configuration builders can trigger a number of different events. All these
 * events are derived from this class. This event base class does not define any
 * additional properties. However, it defines that the event source must be a
 * {@code ConfigurationBuilder}.
 * </p>
 *
 * @version $Id$
 * @since 2.0
 */
public class ConfigurationBuilderEvent extends Event
{
    /** The common super type for all events related to configuration builders. */
    public static final EventType<ConfigurationBuilderEvent> ANY =
            new EventType<>(Event.ANY, "BUILDER");

    /**
     * The specific event type for builder reset events. Events of this type are
     * generated each time the builder's {@code resetResult()} method is called.
     */
    public static final EventType<ConfigurationBuilderEvent> RESET =
            new EventType<>(ANY, "RESET");

    /**
     * The specific event type for configuration request events. Events of this
     * type are generated each time the builder's {@code getConfiguration()}
     * method is called (before the managed configuration is actually accessed
     * and the lock is acquired). This gives listeners the opportunity to
     * perform some checks which may invalidate the configuration, e.g. trigger
     * a reload check. <strong>Note:</strong> A listener must not call the
     * builder's {@code getConfiguration()} method - this will cause an
     * infinite loop!
     *
     * @see ConfigurationBuilder#getConfiguration()
     */
    public static final EventType<ConfigurationBuilderEvent> CONFIGURATION_REQUEST =
            new EventType<>(ANY,
                    "CONFIGURATION_REQUEST");

    /**
     * Creates a new instance of {@code ConfigurationBuilderEvent} and sets
     * basic properties.
     *
     * @param source the {@code ConfigurationBuilder} object which triggered
     *        this event (must not be <b>null</b>)
     * @param evType the type of this event (must not be <b>null</b>)
     * @throws IllegalArgumentException if a required parameter is null
     */
    public ConfigurationBuilderEvent(ConfigurationBuilder<?> source,
            EventType<? extends ConfigurationBuilderEvent> evType)
    {
        super(source, evType);
    }

    /**
     * Returns the source of this event as a {@code ConfigurationBuilder}.
     *
     * @return the {@code ConfigurationBuilder} which generated this event
     */
    @Override
    public ConfigurationBuilder<?> getSource()
    {
        return (ConfigurationBuilder<?>) super.getSource();
    }
}
