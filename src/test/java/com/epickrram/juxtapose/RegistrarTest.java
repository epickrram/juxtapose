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

import org.junit.Test;

import javax.management.ObjectName;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public final class RegistrarTest
{
    private static final String STRING_VALUE = "string-value";

    @Test
    public void shouldRegisterMBeanWithClassName() throws Exception
    {
        final ObjectName objectName = new ObjectName("com.epickrram.juxtapose:type=Foo");
        final String attribute = "bar";

        final Foo foo = new Foo();
        Registrar.registerBean(foo);

        assertThat((Integer) getPlatformMBeanServer().getAttribute(objectName, attribute), is(foo.getBar()));

        foo.increment();

        assertThat((Integer) getPlatformMBeanServer().getAttribute(objectName, attribute), is(foo.getBar()));

        Registrar.deregisterBean(foo);
    }

    @Test
    public void shouldExportMethods() throws Exception
    {
        final ObjectName objectName = new ObjectName("my.path:type=Method");
        final InvokedMethodObject invokedMethodObject = new InvokedMethodObject();
        Registrar.registerBean(invokedMethodObject);

        final int methodArgument = 17;

        getPlatformMBeanServer().invoke(objectName, "methodOne",
                new Object[] {methodArgument},
                new String[] {"int"});

        assertThat(invokedMethodObject.getValueOne(), is(methodArgument));

        final String stringArgument = "stringArgument";
        final String returnValue = (String) getPlatformMBeanServer().invoke(objectName, "methodTwo",
                new Object[]{stringArgument},
                new String[]{"java.lang.String"});

        assertThat(returnValue, is("RETURN:" + stringArgument));
    }

    @Test
    public void shouldRegisterMBeanWithSpecifiedPath() throws Exception
    {
        final SpecifiedPath bean = new SpecifiedPath(STRING_VALUE);
        Registrar.registerBean(bean);

        assertThat((String) getPlatformMBeanServer().getAttribute(new ObjectName("my.path:type=Custom"), "fieldValue"), is(STRING_VALUE));

        Registrar.deregisterBean(bean);
    }

    @Test
    public void shouldSetDescriptions() throws Exception
    {
        final SpecifiedPath bean = new SpecifiedPath(STRING_VALUE);
        Registrar.registerBean(bean);

        assertThat(getPlatformMBeanServer().getMBeanInfo(new ObjectName("my.path:type=Custom")).getDescription(), is("BeanDescription"));
        assertThat(getPlatformMBeanServer().getMBeanInfo(new ObjectName("my.path:type=Custom")).getAttributes()[0].getDescription(), is("AttributeDescription"));
        
        Registrar.deregisterBean(bean);
    }

    @ExportBean(path = "my.path:type=Custom", description = "BeanDescription")
    private static final class SpecifiedPath
    {
        private final String value;

        public SpecifiedPath(final String value)
        {
            this.value = value;
        }

        @Export(description = "AttributeDescription")
        public String getFieldValue()
        {
            return value;
        }
    }

    @ExportBean(path = "my.path:type=Method")
    private static final class InvokedMethodObject
    {
        private int valueOne = 0;

        @Export
        public void methodOne(final int valueOne)
        {
            this.valueOne = valueOne;
        }

        @Export
        public String methodTwo(final String value)
        {
            return "RETURN:" + value;
        }

        public int getValueOne()
        {
            return valueOne;
        }
    }
}