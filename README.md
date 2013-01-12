juxtapose
=========

Annotation-driven MBean exporting for Java

Usage
=====

Annotate attributes for export:

    @Export(description = "myIntegerAttribute")
    public int getIntegerAttribute()
    {
        // ...
    }

Annotate methods for export:

    @Export
    public void updateCounter(final int value)
    {
        // ...
    }

Annotate your object to define a description and JMX `ObjectName`:

    @ExportBean(path = "my.bean.path:type=MyObject", description = "My Object")
    public final class MyObject
    {
        // ...
    }

All annotation arguments are optional, sensible defaults will be used if they are not present.

To register/deregister your object with the local MBean server, use the `Registrar` methods:

    final MyObject myObject = new MyObject();
    Registrar.registerBean(myObject);
    // ...
    Registrar.deregisterBean(myObject);
