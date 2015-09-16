# Introduction #

[SchemaSpy](http://schemaspy.sourceforge.net/) is a Java program and so can be run in a variety of ways. Details are given below for using it from Ant. SchemaSpy itself delegates to the (non-Java) dot executable from [Graphviz](http://www.graphviz.org/) so that needs to be installed on the machine you are running SchemaSpy on and added to the path. (Check that this is working by running "dot -V" from the command-line which should just output some version information.)

Force.com objects are treated as tables and Force.com fields as columns. Note that formula fields return a size of 1300, the maximum size of the formula string. Picklist values, record type names and the "to many" relationship name are displayed in the SchemaSpy comments column.

These instructions have been updated to reflect **version 1.3 of the driver that uses the Partner web services API**. Thanks to asgavrikov for pointing out (see comments) that the Partner API is the appropriate one to use. So the jar combinations are:

  * force-metadata-jdbc-driver-1.3.jar and later require generated-sforce-partner-NN.jar
  * force-metadata-jdbc-driver-1.2.jar and earlier require a generated Axis Enterprise web services API jar (no longer provided here)

The jars contained in depends.zip including the Axis jars are required in both cases; see the instructions in the "Running" section.

# Options #

## Objects Output ##
The first version of the driver only output data for custom objects for simplicity and version 1.1 of the driver has that behavior by default. But it can now also output data for all objects in an org.

To supplement SchemaSpy's -i option (which is an inclusion mask), the driver supports these optional properties:

| **Name** | **Values** | **Default** | **Description** |
|:---------|:-----------|:------------|:----------------|
| custom   | true or false | "true"      | consider custom objects |
| standard | true or false | "false"     | consider standard objects |
| excludes | comma separated list of object names | "User"      | custom or standard object names that are an exact match are excluded (takes priority over includes) |
| includes | comma separated list of object names | Empty       | custom or standard object names that are an exact match are included (excludes takes priority over this) irrespective of the custom and standard flag settings |

You can confirm the settings by watching for this standard out output:
```
ForceMetaDataDriver: Filter [exclusionNames=[user], inclusionNames=[], keepCustom=true, keepStandard=false]
```

Note the default exclusion of the **User** object. As all other objects reference this object, the diagrams become dominated by these relationships if the User object is included obscuring the more significant relationships. Generation time also goes up dramatically: the standard objects take a few minutes to generate on my laptop with User excluded but an hour to generate with User included.

Here are some examples. Note the required backslash and obviously avoid whitespace:

| **SchemaSpy Argument** | **Description** |
|:-----------------------|:----------------|
| None                   | All custom objects |
| -connprops standard\=true;custom\=false | All standard objects except the User object |
| -connprops excludes\=;includes\=User | All custom objects and the User object |
| -connprops standard\=false;custom\=false;excludes\=;includes\=User,Task,Event | The User, Task and Event standard objects |

## Org URL ##

The default URL used to connect to the Partner web service that provides the Force.com metadata is compiled into the code generated from the WSDL. (Here that code is in a jar called generated-sforce-partner-18.jar.) Note that the URL must return data that matches the schema elements defined in the WSDL - some care is needed over versioning and so the URL includes a version number part e.g. `https://www.salesforce.com/services/Soap/u/18.0`.

However, a different URL is needed to connect to a sandbox URL. To allow alternate URLs to be used version 1.2 of the driver supports this new property:

| **Name** | **Values** | **Default** | **Description** |
|:---------|:-----------|:------------|:----------------|
| url      | e.g. `https://test.salesforce.com/services/Soap/u/18.0` | taken from the generated web service client jar | the URL (but note that the property name is in lower case) to use to get the Force.com metadata via the Partner web service API |

See the previous section for how to include this in the -connprops to SchemaSpy.

Note that the property name is **url** (lowercase) not URL (uppercase) and you should see the supplied value echoed to standard output like this if the value is correctly specified:
```
ForceMetaDataDriver: Force.com connection url https://test.salesforce.com/services/Soap/u/21.0
```

# Running #

The example here is For Ant; just put the parameters on the command-line and specify all the jars in the -cp argument to run from the command-line. (See the comments below.)

This assumes that all the Jar files (and force.properties) are in the folder "schemaspy". The arguments set to "fake" are ones that SchemaSpy insists be present but are not used. The -db and -desc arguments produce text in the output files and so can be changed fairly arbitrarily. The -un and -pw arguments are passed through to the JDBC driver and define the Force.com instance that is connected to. Note that the -pw argument must be made up of both the password and the security token.

```
<target name="document">
    <echo message="Generating SchemaSpy documentation (requires Graphviz to be installed to produce diagrams)"/>
    <delete dir="doc" failonerror="false"/>
    <java classname="net.sourceforge.schemaspy.Main" fork="true" failonerror="true">
        <arg line="-t schemaspy/force"/>
        <arg line="-db Claims"/>
        <arg line="-un ${sf.username}"/>
        <arg line="-pw ${sf.password}"/>
        <arg line="-o doc"/>
        <arg line="-font Arial"/>
        <arg line="-fontsize 8"/>
        <arg line="-hq"/>
        <arg line="-norows"/>
        <arg line='-desc "Extracted from ClaimVantage Claims r${env.SVN_REVISION} on Force.com"'/>
        <arg line="-u fake"/>
        <arg line="-p fake"/>
        <arg line="-host fake"/>
        <classpath>
             <fileset dir="schemaspy" includes="*.jar"/>
        </classpath>
    </java>
</target>
```

The files required for the schemaspy folder are:

  * [force-metadata-jdbc-driver-1.4.jar](http://force-metadata-jdbc-driver.googlecode.com/files/force-metadata-jdbc-driver-1.4.jar) from [Downloads](http://code.google.com/p/force-metadata-jdbc-driver/downloads/list)
  * [generated-sforce-partner-18.jar](http://force-metadata-jdbc-driver.googlecode.com/files/generated-sforce-partner-18.jar) from [Downloads](http://code.google.com/p/force-metadata-jdbc-driver/downloads/list)
  * [force.properties](http://force-metadata-jdbc-driver.googlecode.com/files/force.properties) from [Downloads](http://code.google.com/p/force-metadata-jdbc-driver/downloads/list)
  * [depends.zip](http://force-metadata-jdbc-driver.googlecode.com/files/depends.zip) from [Downloads](http://code.google.com/p/force-metadata-jdbc-driver/downloads/list) (extract the jars)
  * schemaSpy\_4.1.1.jar from [SchemaSpy](http://schemaspy.sourceforge.net/)

SchemaSpy calls the JDBC driver which calls the generated partner web services classes which use the Axis libraries to connect to Force.com via the internet. If your internet access passes through a **proxy** server then you may need to configure the Axis properties described in the "Network configuration" section of the [Client-Side Axis](http://ws.apache.org/axis/java/client-side-axis.html) documentation. These can be set using jvmarg elements inside the java element in Ant or using -D arguments on the Java command line.