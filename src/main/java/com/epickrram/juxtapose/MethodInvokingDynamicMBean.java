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

import javax.management.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MethodInvokingDynamicMBean implements DynamicMBean
{
    private final Map<String, Method> attributeMethodByNameMap;
    private final Object implementation;
    private final Map<String, Method> exportedMethodByNameMap;
    private final MBeanAttributeInfo[] mBeanAttributeInfo;
    private final MBeanOperationInfo[] mBeanOperationInfo;
    private final MBeanInfo mBeanInfo;

    MethodInvokingDynamicMBean(final Object implementation, final Map<String, Method> attributeMethodByNameMap,
                               final List<Method> exportedMethods)
    {
        this.attributeMethodByNameMap = attributeMethodByNameMap;
        this.implementation = implementation;
        this.mBeanAttributeInfo = createAttributeInfo(attributeMethodByNameMap);
        this.mBeanOperationInfo = createOperationInfo(exportedMethods);
        mBeanInfo = createMBeanInfo();
        exportedMethodByNameMap = new HashMap<String, Method>(exportedMethods.size());
        for (Method method : exportedMethods)
        {
            exportedMethodByNameMap.put(method.getName(), method);
        }
    }

    public Object getAttribute(final String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        if(attributeMethodByNameMap.containsKey(attribute))
        {
            try
            {
                return attributeMethodByNameMap.get(attribute).invoke(implementation);
            }
            catch (IllegalAccessException e)
            {
                throw new ReflectionException(e);
            }
            catch (InvocationTargetException e)
            {
                throw new ReflectionException(e);
            }
        }
        throw new AttributeNotFoundException("Cannot find attribute " + attribute + ", is it annotated with @Export?");
    }

    public Object invoke(final String actionName, final Object[] params, final String[] signature) throws MBeanException, ReflectionException
    {
        if(exportedMethodByNameMap.containsKey(actionName))
        {
            try
            {
                return exportedMethodByNameMap.get(actionName).invoke(implementation, params);
            }
            catch (IllegalAccessException e)
            {
                throw new ReflectionException(e);
            }
            catch (InvocationTargetException e)
            {
                throw new ReflectionException(e);
            }
        }
        throw new MBeanException(new JmxException("Cannot find method " + actionName + ", is it annotated with @Export?"));
    }

    public MBeanInfo getMBeanInfo()
    {
        return mBeanInfo;
    }

    public void setAttribute(final Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
    {
        throw new UnsupportedOperationException();
    }

    public AttributeList getAttributes(final String[] attributes)
    {
        throw new UnsupportedOperationException();
    }

    public AttributeList setAttributes(final AttributeList attributes)
    {
        throw new UnsupportedOperationException();
    }

    private MBeanInfo createMBeanInfo()
    {
        return new MBeanInfo(this.implementation.getClass().getName(),
                getMBeanDescription(), mBeanAttributeInfo, null, mBeanOperationInfo, null);
    }

    private String getMBeanDescription()
    {
        final ExportBean exportBean = implementation.getClass().getAnnotation(ExportBean.class);
        return (exportBean != null && exportBean.description().length() != 0) ?
                exportBean.description() :
                implementation.getClass().getSimpleName();
    }

    private MBeanOperationInfo[] createOperationInfo(final List<Method> exportedMethods)
    {
        final MBeanOperationInfo[] mBeanOperationInfo = new MBeanOperationInfo[exportedMethods.size()];
        int counter = 0;
        for (Method method : exportedMethods)
        {
            final String description = getMethodDescription(method);
            mBeanOperationInfo[counter++] = new MBeanOperationInfo(description, method);
        }
        return mBeanOperationInfo;
    }

    private MBeanAttributeInfo[] createAttributeInfo(final Map<String, Method> attributeMethodMap)
    {
        final MBeanAttributeInfo[] mBeanAttributeInfo;
        try
        {
            mBeanAttributeInfo = new MBeanAttributeInfo[attributeMethodMap.size()];

            int counter = 0;
            for (Map.Entry<String, Method> entry : attributeMethodMap.entrySet())
            {
                final Method method = entry.getValue();
                final String description = getMethodDescription(method);
                mBeanAttributeInfo[counter++] = new MBeanAttributeInfo(entry.getKey(), description, method, null);
            }
        }
        catch (IntrospectionException e)
        {
            throw new JmxException(e);
        }
        return mBeanAttributeInfo;
    }

    private static String getMethodDescription(final Method method)
    {
        final Export export = getExportAnnotation(method);
        return (export != null && export.description().length() != 0) ? export.description() : method.getName();
    }

    private static Export getExportAnnotation(final Method method)
    {
        return method.getAnnotation(Export.class);
    }
}