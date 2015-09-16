This JDBC driver implements just enough of the java.sql.DatabaseMetaData interface to allow the wonderful [SchemaSpy](http://schemaspy.sourceforge.net/) to output a set of ERDs for a [Force.com](http://www.salesforce.com/platform/) data model. In our environment, the model is generated on every [Jenkins](http://jenkins-ci.org/) build.

If you just want to interactively browse the ERD of an org take a look at the [Schema Builder in Winter ’12](http://blogs.developerforce.com/developer-relations/2011/10/an-erd-is-worth-a-thousand-words-schema-builder-in-winter-12.html) before reading further.

See [Usage for V2](http://code.google.com/p/force-metadata-jdbc-driver/wiki/UsageForV2) for how to run SchemaSpy with this driver. (For earlier versions see [Usage](http://code.google.com/p/force-metadata-jdbc-driver/wiki/Useage).)

Here is the change history:

  * Version 2.2 of the driver supports proxy servers by transferring proxy server system properties to the underlying [WSC](http://code.google.com/p/sfdc-wsc/) configuration
  * Version 2.1 of the driver adds the label, default value, formula and help to the field comments and the singular and plural names to the object comments. It also gets rid of a bogus leading underscore in the type names that was introduced in version 2.0
  * Version 2.0 of the driver includes all the required classes in a single jar and is easier to use
  * Version 1.5 of the driver includes [Chris Wolf's](http://chriswolf.heroku.com/) additions implementing more of the JDBC specification allowing the driver to also be used with [Open ModelSphere](http://www.modelsphere.org/) and improving the chances that it will work with other tools
  * Version 1.4 of the driver displays "cascade delete" in the comments which appears to be the only information related to master-detail vs lookup references available in the APIs
  * Version 1.3 of the driver runs against the Partner web service API rather than the Enterprise one
  * Version 1.2 of the driver adds the option of specifying the Enterprise web service API URL to allow connection to sandbox orgs
  * Version 1.1 of the driver adds options to allow standard as well as custom objects to be considered

It is possible that this JDBC driver could be used with other tools that use JDBC to access the database metadata. But it is very likely that code additions and changes would be needed as only the parts of the API that SchemaSpy required were implemented.

Here is a sample output page for a Force.com project:

![http://force-metadata-jdbc-driver.googlecode.com/files/sample-schemaspy-page.png](http://force-metadata-jdbc-driver.googlecode.com/files/sample-schemaspy-page.png)