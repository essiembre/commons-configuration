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

package org.apache.commons.configuration2.plist;

import java.io.File;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.configuration2.AbstractHierarchicalConfiguration;
import org.apache.commons.configuration2.AbstractHierarchicalFileConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationException;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.tree.ConfigurationNode;
import org.apache.commons.configuration2.tree.DefaultConfigurationNode;
import org.apache.commons.lang.StringUtils;

/**
 * NeXT / OpenStep style configuration. This configuration can read and write
 * ASCII plist files. It supports the GNUStep extension to specify date objects.
 * <p>
 * References:
 * <ul>
 *   <li><a
 * href="http://developer.apple.com/documentation/Cocoa/Conceptual/PropertyLists/Articles/OldStylePListsConcept.html">
 * Apple Documentation - Old-Style ASCII Property Lists</a></li>
 *   <li><a
 * href="http://www.gnustep.org/resources/documentation/Developer/Base/Reference/NSPropertyList.html">
 * GNUStep Documentation</a></li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>
 * {
 *     foo = "bar";
 *
 *     array = ( value1, value2, value3 );
 *
 *     data = &lt;4f3e0145ab>;
 *
 *     date = &lt;*D2007-05-05 20:05:00 +0100>;
 *
 *     nested =
 *     {
 *         key1 = value1;
 *         key2 = value;
 *         nested =
 *         {
 *             foo = bar
 *         }
 *     }
 * }
 * </pre>
 *
 * @since 1.2
 *
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
public class PropertyListConfiguration extends AbstractHierarchicalFileConfiguration
{
    /** The default date format, its use must be synchronized */
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("<*'D'yyyy-MM-dd HH:mm:ss Z>");

    /** The serial version UID. */
    private static final long serialVersionUID = 3227248503779092127L;

    /** Size of the indentation for the generated file. */
    private static final int INDENT_SIZE = 4;

    /**
     * Instance specific format that can be used to format a date in differents time zones.
     * Its use must be synchronized
     */
    final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DEFAULT_DATE_FORMAT.toPattern());

    /**
     * Creates an empty PropertyListConfiguration object which can be
     * used to synthesize a new plist file by adding values and
     * then saving().
     */
    public PropertyListConfiguration()
    {
    }

    /**
     * Creates a new instance of <code>PropertyListConfiguration</code> and
     * copies the content of the specified configuration into this object.
     *
     * @param c the configuration to copy
     * @since 1.4
     */
    public PropertyListConfiguration(AbstractHierarchicalConfiguration<? extends ConfigurationNode> c)
    {
        super(c);
    }

    /**
     * Creates and loads the property list from the specified file.
     *
     * @param fileName The name of the plist file to load.
     * @throws ConfigurationException Error while loading the plist file
     */
    public PropertyListConfiguration(String fileName) throws ConfigurationException
    {
        super(fileName);
    }

    /**
     * Creates and loads the property list from the specified file.
     *
     * @param file The plist file to load.
     * @throws ConfigurationException Error while loading the plist file
     */
    public PropertyListConfiguration(File file) throws ConfigurationException
    {
        super(file);
    }

    /**
     * Creates and loads the property list from the specified URL.
     *
     * @param url The location of the plist file to load.
     * @throws ConfigurationException Error while loading the plist file
     */
    public PropertyListConfiguration(URL url) throws ConfigurationException
    {
        super(url);
    }

    public void setProperty(String key, Object value)
    {
        // special case for byte arrays, they must be stored as is in the configuration
        if (value instanceof byte[])
        {
            fireEvent(EVENT_SET_PROPERTY, key, value, true);
            setDetailEvents(false);
            try
            {
                clearProperty(key);
                addPropertyDirect(key, value);
            }
            finally
            {
                setDetailEvents(true);
            }
            fireEvent(EVENT_SET_PROPERTY, key, value, false);
        }
        else
        {
            super.setProperty(key, value);
        }
    }

    public void addProperty(String key, Object value)
    {
        if (value instanceof byte[])
        {
            fireEvent(EVENT_ADD_PROPERTY, key, value, true);
            addPropertyDirect(key, value);
            fireEvent(EVENT_ADD_PROPERTY, key, value, false);
        }
        else
        {
            super.addProperty(key, value);
        }
    }

    public void load(Reader in) throws ConfigurationException
    {
        PropertyListParser parser = new PropertyListParser(in);
        try
        {
            AbstractHierarchicalConfiguration<ConfigurationNode> config = parser.parse();
            setRootNode(config.getRootNode());
        }
        catch (ParseException e)
        {
            throw new ConfigurationException(e);
        }
    }

    public void save(Writer out) throws ConfigurationException
    {
        PrintWriter writer = new PrintWriter(out);
        printNode(writer, 0, getRootNode());
        writer.flush();
    }

    /**
     * Append a node to the writer, indented according to a specific level.
     */
    private void printNode(PrintWriter out, int indentLevel, ConfigurationNode node)
    {
        String padding = StringUtils.repeat(" ", indentLevel * INDENT_SIZE);

        if (node.getName() != null)
        {
            out.print(padding + quoteString(node.getName()) + " = ");
        }

        // get all non trivial nodes
        List<ConfigurationNode> children = new ArrayList<ConfigurationNode>(node.getChildren());
        Iterator<ConfigurationNode> it = children.iterator();
        while (it.hasNext())
        {
            ConfigurationNode child = it.next();
            if (child.getValue() == null && (child.getChildren() == null || child.getChildren().isEmpty()))
            {
                it.remove();
            }
        }

        if (!children.isEmpty())
        {
            // skip a line, except for the root dictionary
            if (indentLevel > 0)
            {
                out.println();
            }

            out.println(padding + "{");

            // display the children
            it = children.iterator();
            while (it.hasNext())
            {
                ConfigurationNode child = it.next();

                printNode(out, indentLevel + 1, child);

                // add a semi colon for elements that are not dictionaries
                Object value = child.getValue();
                if (value != null && !(value instanceof Map) && !(value instanceof Configuration))
                {
                    out.println(";");
                }

                // skip a line after arrays and dictionaries
                if (it.hasNext() && (value == null || value instanceof List))
                {
                    out.println();
                }
            }

            out.print(padding + "}");

            // line feed if the dictionary is not in an array
            if (node.getParentNode() != null)
            {
                out.println();
            }
        }
        else
        {
            // display the leaf value
            Object value = node.getValue();
            printValue(out, indentLevel, value);
        }
    }

    /**
     * Append a value to the writer, indented according to a specific level.
     */
    private void printValue(PrintWriter out, int indentLevel, Object value)
    {
        String padding = StringUtils.repeat(" ", indentLevel * INDENT_SIZE);

        if (value instanceof List)
        {
            out.print("( ");
            Iterator it = ((List) value).iterator();
            while (it.hasNext())
            {
                printValue(out, indentLevel + 1, it.next());
                if (it.hasNext())
                {
                    out.print(", ");
                }
            }
            out.print(" )");
        }
        else if (value instanceof HierarchicalConfiguration)
        {
            printNode(out, indentLevel, ((HierarchicalConfiguration) value).getRootNode());
        }
        else if (value instanceof Configuration)
        {
            // display a flat Configuration as a dictionary
            out.println();
            out.println(padding + "{");

            Configuration config = (Configuration) value;
            Iterator it = config.getKeys();
            while (it.hasNext())
            {
                String key = (String) it.next();
                ConfigurationNode node = new DefaultConfigurationNode(key);
                node.setValue(config.getProperty(key));

                printNode(out, indentLevel + 1, node);
                out.println(";");
            }
            out.println(padding + "}");
        }
        else if (value instanceof Map)
        {
            // display a Map as a dictionary
            Map map = (Map) value;
            printValue(out, indentLevel, new MapConfiguration(map));
        }
        else if (value instanceof byte[])
        {
            out.print("<" + new String(Hex.encodeHex((byte[]) value)) + ">");
        }
        else if (value instanceof Date)
        {
            synchronized (DATE_FORMAT)
            {
                out.print(DATE_FORMAT.format((Date) value));
            }
        }
        else if (value instanceof Calendar)
        {
            // change the time zone of the date format
            synchronized (DATE_FORMAT)
            {
                Calendar calendar = (Calendar) value;
                TimeZone previousZone = DATE_FORMAT.getTimeZone();
                DATE_FORMAT.setTimeZone(calendar.getTimeZone());

                out.print(DATE_FORMAT.format(calendar.getTime()));

                // restore the previous time zone of the date format
                DATE_FORMAT.setTimeZone(previousZone);
            }
        }
        else if (value != null)
        {
            out.print(quoteString(String.valueOf(value)));
        }
    }

    /**
     * Quote the specified string if necessary, that's if the string contains:
     * <ul>
     *   <li>a space character (' ', '\t', '\r', '\n')</li>
     *   <li>a quote '"'</li>
     *   <li>special characters in plist files ('(', ')', '{', '}', '=', ';', ',')</li>
     * </ul>
     * Quotes within the string are escaped.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>abcd -> abcd</li>
     *   <li>ab cd -> "ab cd"</li>
     *   <li>foo"bar -> "foo\"bar"</li>
     *   <li>foo;bar -> "foo;bar"</li>
     * </ul>
     */
    String quoteString(String s)
    {
        if (s == null)
        {
            return null;
        }

        if (s.indexOf(' ') != -1
                || s.indexOf('\t') != -1
                || s.indexOf('\r') != -1
                || s.indexOf('\n') != -1
                || s.indexOf('"') != -1
                || s.indexOf('(') != -1
                || s.indexOf(')') != -1
                || s.indexOf('{') != -1
                || s.indexOf('}') != -1
                || s.indexOf('=') != -1
                || s.indexOf(',') != -1
                || s.indexOf(';') != -1)
        {
            s = StringUtils.replace(s, "\"", "\\\"");
            s = "\"" + s + "\"";
        }

        return s;
    }

    /**
     * Parses a date in a format like
     * <code>&lt;*D2002-03-22 11:30:00 +0100&gt;</code>.
     *
     * @param s the string with the date to be parsed
     * @return the parsed date
     * @throws ParseException if an error occurred while parsing the string
     */
    static Date parseDate(String s) throws ParseException
    {
        try
        {
            synchronized (DEFAULT_DATE_FORMAT)
            {
                return DEFAULT_DATE_FORMAT.parse(s);
            }
        }
        catch (Exception e)
        {
            throw (ParseException) new ParseException("Unable to parse the date").initCause(e);
        }
    }
}
