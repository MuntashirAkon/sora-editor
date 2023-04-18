/**
 * Copyright (c) 2022 Sebastian Thomschke and others.
 * <p>
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.tm4e.core.internal.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public final class ObjectCloner {

    private static final LoadingCache<Class<?>, Optional<Method>> CLONE_METHODS = CacheBuilder.newBuilder().weakKeys()
            .build(new CacheLoader<Class<?>, Optional<Method>>() {
                @Override
                public Optional<Method> load(final Class<?> cls) {
                    try {
                        return Optional.of(cls.getMethod("clone"));
                    } catch (final Exception ex) {
                        return Optional.empty();
                    }
                }
            });

    public static <@NonNull T> T deepClone(final T obj) {
        return deepClone(obj, new IdentityHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static <@NonNull T> T deepClone(final T obj, final Map<Object, @Nullable Object> clones) {
        final Object clone = clones.get(obj);

        if (clone != null)
            return (T) clone;

        if (obj instanceof List<?>) {
            List<T> list = (List<T>) obj;
            final List<T> listClone = shallowClone(list, () -> new ArrayList<>(list));
            clones.put(list, listClone);
            for (int i = 0; i < listClone.size(); ++i) {
                listClone.set(i, deepCloneNullable(listClone.get(i), clones));
            }
            return (T) listClone;
        }

        if (obj instanceof Set<?>) {
            Set<T> set = (Set<T>) obj;
            final Set<@Nullable Object> setClone = (Set<@Nullable Object>) shallowClone(set, HashSet::new);
            clones.put(set, setClone);
            setClone.clear();
            for (final T e : set) {
                setClone.add(deepCloneNullable(e, clones));
            }
            return (T) setClone;
        }

        if (obj instanceof Map<?, ?>) {
            Map<?, T> map = (Map<?, T>) obj;
            final HashMap<?, T> mapClone = (HashMap<?, T>) shallowClone(map, () -> new HashMap<Object, T>(map));
            clones.put(map, mapClone);
            // Replacement for mapClone.replaceAll((k, v) -> deepCloneNullable(v, clones));
            for (Map.Entry<?, T> entry : mapClone.entrySet()) {
                entry.setValue(deepCloneNullable(entry.getValue(), clones));
            }
            return (T) mapClone;
        }

        if (obj.getClass().isArray()) {
            final int len = Array.getLength(obj);
            final Class<?> arrayType = obj.getClass().getComponentType();
            final Object arrayClone = Array.newInstance(arrayType, len);
            clones.put(obj, arrayClone);
            for (int i = 0; i < len; i++) {
                Array.set(arrayClone, i, deepCloneNullable(Array.get(obj, i), clones));
            }
            return (T) arrayClone;
        }

        final T shallowClone = shallowClone(obj, () -> obj);
        clones.put(obj, shallowClone);
        return obj;
    }

    @Nullable
    private static <@Nullable T> T deepCloneNullable(final T obj, final Map<Object, @Nullable Object> clones) {
        if (obj == null) {
            return null;
        }
        return deepClone(obj, clones);
    }

    @SuppressWarnings("unchecked")
    private static <@NonNull T> T shallowClone(final T obj, final Supplier<T> fallback) {
        final Class<?> objClass = obj.getClass();
        if (obj instanceof Cloneable) {
            try {
                final Optional<Method> cloneMethod = CLONE_METHODS.get(objClass);
                if (cloneMethod.isPresent()) {
                    return (T) cloneMethod.get().invoke(obj);
                }
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        }
        return fallback.get();
    }

    private ObjectCloner() {
    }
}
