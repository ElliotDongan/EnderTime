/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * An Implementation of Properties that is sorted when iterating.
 * Made because i got tired of seeing config files written in random orders.
 * This is implemented very basically, and thus is not a speedy system.
 * This is not recommended for used in high traffic areas, and is mainly intended for writing to disc.
 */
public class SortedProperties extends Properties {
    private static final long serialVersionUID = -8913480931455982442L;

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        Set<Map.Entry<Object, Object>> ret = new TreeSet<>(Comparator.comparing(entry -> entry.getKey().toString()));
        ret.addAll(super.entrySet());
        return ret;
    }

    @Override
    public Set<Object> keySet() {
        return new TreeSet<>(super.keySet());
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(new TreeSet<>(super.keySet()));
    }

    public static void store(Properties props, Writer stream, String comment) throws IOException {
        SortedProperties sorted = new SortedProperties();
        sorted.putAll(props);
        sorted.store(stream, comment);
    }
}
