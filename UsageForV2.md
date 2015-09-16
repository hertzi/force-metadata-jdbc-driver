# Introduction #

This version of the driver doesn't offer new functionality but is packaged so that it is easier to use. A single driver jar file is all that is required; this contains the driver classes and also all the classes needed to access the Partner WSDL API to get the metadata information from an org.

(For information on how to use earlier versions see [Usage](http://code.google.com/p/force-metadata-jdbc-driver/wiki/Useage).)

# Running #

Here is how to generate the [SchemaSpy](http://schemaspy.sourceforge.net/) output for your Force.com org:

  * Download the [SchemaSpy](http://schemaspy.sourceforge.net/) jar
  * Download and install [Graphviz](http://www.graphviz.org/) that is used by SchemaSpy to create the automatically laid out diagrams; multiple platforms including Windows and Mac are supported
  * Download the latest (2.x) version of the [Force Metadata JDBC driver](http://code.google.com/p/force-metadata-jdbc-driver/downloads/list) jar
  * In the folder that contains the jars just enter this (replacing the arguments that start with "My" with your own values and entering it all on one line):

```
java -cp schemaSpy_5.0.0.jar;force-metadata-jdbc-driver-2.0.jar net.sourceforge.schemaspy.Main
    -t force
    -u MyUserName -p MyPasswordAndSecurityToken
    -font Arial -fontsize 8 -hq -norows -o doc
    -db MyDbName -desc "Extracted from MyDbName on Force.com"
```

The arguments are documented in the [SchemaSpy](http://schemaspy.sourceforge.net/) web site. The only change needed for Mac/Unix is the -cp argument separator changing from ";" to ":".

By default all custom objects are output. The set of objects that are output can be customized by adding a -connprops argument. Here is an example that outputs five standard objects in addition to all the custom objects:
```
    -connprops excludes\=;includes\=Account,Contact,User,Task,Event
```
On Mac/Unix bash this would need to be:
```
    -connprops excludes\\=\;includes\\=Account,Contact,User,Task,Event
```
More details of how the -connprops can be used including how to change the URL to for example connect to a sandbox org are in the [original usage page](http://code.google.com/p/force-metadata-jdbc-driver/wiki/Useage). (Note that the property to achieve this is **url** which is in lower case.)

# Proxy Servers #

Since version 2.2, the following system properties (that can be set using e.g. Java executable -D arguments) are used if present to configure the corresponding values in the underlying [WSC](http://code.google.com/p/sfdc-wsc/) configuration:

  * http.auth.ntlm.domain
  * http.proxyHost and http.proxyPort (both must be set)
  * http.proxyUser
  * http.proxyPassword

# Other #

Thanks to [Chris Wolf](http://chriswolf.heroku.com/) for the discussion that led to this approach. He also contributed the changes required to make the driver work with [Open ModelSphere](http://www.modelsphere.org/) and potentially other tools that require more of the JDBC spec to be implemented than SchemaSpy does. And his approach to the user name and password handling eliminated the non-SchemaSpy standard -un and -pw arguments.

Note that the code is built to work with Java 1.5 so will also work with Java 1.6.

This new version of the driver has been tested with:

  * SchemaSpy 5.0.0 and Graphviz 2.26.3
  * ModelSphere v3.1-912

Instead of the general purpose [Axis](http://axis.apache.org/axis/) web services stack the code is now using the [Force.com Web Service Connector (WSC)](http://code.google.com/p/sfdc-wsc/) web service client stack compiled for Java 1.5. Version 21 of the Partner WSDL API is used.

The force.properties file that was separate is now included in the driver jar and accessed in place from there.

As this is all just standard Java code it is simple to also run it from Ant; for example our [continuous integration build](http://force201.wordpress.com/2010/03/11/continuous-integration/) generates the SchemaSpy model using Ant each time code is checked in.