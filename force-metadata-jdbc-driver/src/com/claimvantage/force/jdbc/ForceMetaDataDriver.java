package com.claimvantage.force.jdbc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A JDBC driver that wraps the Force.com enterprise web service API to obtain
 * enough information for SchemaSpy to be able to create its output.
 * So it is just a few methods of DatabaseMetaData that are implemented.
 */
public class ForceMetaDataDriver implements Driver {
    
    private static final String URL = "jdbc:claimvantage:force";

    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith(URL);
    }

    public Connection connect(String driverUrl, Properties info) throws SQLException {

        String[] parts = driverUrl.split(":");
        if (parts.length != 5) {
            throw new SQLException("url must be of form \"jdbc:claimvantage:force:<un>:<pw>\" where <un> is a"
                    + " Force.com User name and <pw> is the corresponding Force.com Password including the security"
                    + " token");
        }
        String un = parts[3];
        String pw = parts[4];
        
        // Optional - set this property to not use the default Force.com login URL
        String forceUrl = info.getProperty("url");

        ResultSetFactory factory;
        try {
            Service service = new Service(un, pw, forceUrl, new Filter(info));
            factory = service.createResultSetFactory();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            throw new SQLException(sw.toString());
        }
        return new ForceConnection(factory);
    }

    public int getMajorVersion() {
        return 1;
    }

    public int getMinorVersion() {
        return 4;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
            throws SQLException {
        return new DriverPropertyInfo[] {};
    }

    public boolean jdbcCompliant() {
        return false;
    }
}
