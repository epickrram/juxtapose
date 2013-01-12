/*
Copyright 2011 Mark Price

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.epickrram.juxtapose;

import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

public final class Registrar
{
    private Registrar() {}

    public static <T> void registerBean(final T implementation)
    {
        final DynamicMBean dynamicMBean = createDynamicMBean(implementation);
        final ObjectName objectName = generateObjectName(implementation);
        registerObject(dynamicMBean, objectName);
    }

    public static <T> void deregisterBean(final T implementation)
    {
        deregisterObject(generateObjectName(implementation));
    }

    private static void registerObject(final DynamicMBean dynamicMBean, final ObjectName objectName)
    {
        try
        {
            getPlatformMBeanServer().registerMBean(dynamicMBean, objectName);
        }
        catch (InstanceAlreadyExistsException e)
        {
            throw new JmxException(e);
        }
        catch (MBeanRegistrationException e)
        {
            throw new JmxException(e);
        }
        catch (NotCompliantMBeanException e)
        {
            throw new JmxException(e);
        }
    }

    private static void deregisterObject(final ObjectName objectName)
    {
        try
        {
            getPlatformMBeanServer().unregisterMBean(objectName);
        }
        catch (MBeanRegistrationException e)
        {
            throw new JmxException(e);
        }
        catch (InstanceNotFoundException e)
        {
            throw new JmxException(e);
        }
    }

    private static <T> ObjectName generateObjectName(final T implementation)
    {
        try
        {
            return new ObjectName(generateObjectNameString(implementation));
        }
        catch (MalformedObjectNameException e)
        {
            throw new JmxException(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static <T> DynamicMBean createDynamicMBean(final T implementation)
    {
        final Class<T> cls = (Class<T>) implementation.getClass();
        final Method[] methods = cls.getDeclaredMethods();
        final Map<String, Method> attributeMethodMap = new HashMap<String, Method>();
        final List<Method> exportedMethods = new ArrayList<Method>();
        for (final Method method : methods)
        {
            final Export export = method.getAnnotation(Export.class);
            if(export != null)
            {
                if(method.getReturnType() != Void.class && method.getParameterTypes().length == 0)
                {
                    attributeMethodMap.put(getAttributeName(method), method);
                }
                else
                {
                    exportedMethods.add(method);
                }
            }
        }
        return new MethodInvokingDynamicMBean(implementation, attributeMethodMap, exportedMethods);
    }

    private static String getAttributeName(final Method method)
    {
        if(method.getName().startsWith("get"))
        {
            final String propertyName = method.getName().substring(3);
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return method.getName();
    }

    private static <T> String generateObjectNameString(final T implementation)
    {
        final ExportBean exportBean = implementation.getClass().getAnnotation(ExportBean.class);
        if(exportBean != null && exportBean.path().length() != 0)
        {
            return exportBean.path();
        }
        return implementation.getClass().getPackage().getName() + ":type=" + implementation.getClass().getSimpleName();
    }
}