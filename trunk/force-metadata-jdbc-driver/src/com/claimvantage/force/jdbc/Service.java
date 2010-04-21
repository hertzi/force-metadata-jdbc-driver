package com.claimvantage.force.jdbc;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import com.sforce.soap.enterprise.ChildRelationship;
import com.sforce.soap.enterprise.DescribeGlobalSObjectResult;
import com.sforce.soap.enterprise.DescribeSObjectResult;
import com.sforce.soap.enterprise.Field;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.PicklistEntry;
import com.sforce.soap.enterprise.RecordTypeInfo;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.InvalidSObjectFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;

/**
 * Wraps the Force.com describe calls web service outputting simple data objects.
 */
public class Service {
    
    private SoapBindingStub binding;

    public Service(String un, String pw) throws ServiceException, UnexpectedErrorFault,
            InvalidIdFault, LoginFault, RemoteException {

        binding = (SoapBindingStub) new SforceServiceLocator().getSoap();
        binding.setTimeout(60000);
        
        LoginResult loginResult = binding.login(un, pw);
        binding._setProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY, loginResult.getServerUrl());
        SessionHeader sh = new SessionHeader();
        sh.setSessionId(loginResult.getSessionId());
        binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(), "SessionHeader", sh);

    }
    
    /**
     * Grab the describe data and return it wrapped in a factory.
     */
    public ResultSetFactory createResultSetFactory() throws InvalidSObjectFault, UnexpectedErrorFault, RemoteException {
        
        ResultSetFactory factory = new ResultSetFactory();

        DescribeSObjectResult[] sobs = binding.describeSObjects(getSObjectTypes());
        if (sobs != null) {
            
            Map<String, String> childReferences = new HashMap<String, String>();
            for (DescribeSObjectResult sob : sobs) {
                ChildRelationship[] crs = sob.getChildRelationships();
                for (ChildRelationship cr : crs) {
                    childReferences.put(cr.getChildSObject() + '.' + cr.getField(), cr.getRelationshipName());
                }
            }
            
            for (DescribeSObjectResult sob : sobs) {

                Field[] fields = sob.getFields();
                List<Column> columns = new ArrayList<Column>(fields.length);
                for (Field field : fields) {
                    if (keep(field)) {
                        Column column = new Column(field.getName(), getType(field));
                        columns.add(column);

                        column.setLength(getLength(field));
                        
                        if ("reference".equals(field.getType().toString())) {
                            String childReference = childReferences.get(sob.getName() + "." + field.getName());
                            if (childReference != null) {
                                column.setComments("Referenced: " + childReference);
                            }
                        } else {
                            column.setComments(getPicklistValues(field.getPicklistValues()));
                        }
                        
                        // Booleans have this as false so not too helpful; leave off
                        column.setNillable(false);
                        
                        // NB Not implemented; see comment in ResultSetFactory class
                        column.setCalculated(field.isCalculated() || field.isAutoNumber());
                        
                        String[] referenceTos = field.getReferenceTo();
                        if (referenceTos != null) {
                            for (String referenceTo : referenceTos) {
                                column.setReferencedTable(referenceTo);
                                column.setReferencedColumn("Id");
                            }
                        }
                    }
                }

                Table table = new Table(sob.getName(), getRecordTypes(sob.getRecordTypeInfos()), columns);
                factory.addTable(table);
            }
        }
        
        return factory;
    }
    
    private String getType(Field field) {
        String s = field.getType().toString();
        return s.equalsIgnoreCase("double") ? "decimal" : s;
    }
    
    private int getLength(Field field) {
        if (field.getLength() != 0) {
            return field.getLength();
        } else if (field.getPrecision() != 0) {
            return field.getPrecision();
        } else if (field.getDigits() != 0) {
            return field.getDigits();
        } else if (field.getByteLength() != 0) {
            return field.getByteLength();
        } else {
            // SchemaSpy expects a value
            return 0;
        }
    }
    
    private String getPicklistValues(PicklistEntry[] entries) {
        if (entries != null && entries.length > 0) {
            StringBuilder sb = new StringBuilder(256);
            for (PicklistEntry entry : entries) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(entry.getValue());
            }
            return "Picklist: " + sb.toString();
        }
        return null;
    }
    
    private String getRecordTypes(RecordTypeInfo[] rts) {
        if (rts != null && rts.length > 0) {
            StringBuilder sb = new StringBuilder(256);
            for (RecordTypeInfo rt : rts) {
                // Master always present
                if (!rt.getName().equalsIgnoreCase("Master")) {
                    if (sb.length() > 0) {
                        sb.append(" | ");
                    }
                    sb.append(rt.getName());
                    if (rt.isDefaultRecordTypeMapping()) {
                        sb.append(" (default)");
                    }
                }
            }
            if (sb.length() > 0) {
                return "Record Types: " + sb.toString();
            }
        }
        return null;
    }
    
    private String[] getSObjectTypes() throws UnexpectedErrorFault, RemoteException {
        
        DescribeGlobalSObjectResult[] sobs = binding.describeGlobal().getSobjects();
        
        List<String> list = new ArrayList<String>();
        for (DescribeGlobalSObjectResult sob : sobs) {
            if (keep(sob)) {
                list.add(sob.getName());
            }
        }
        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }
    
    private boolean keep(DescribeGlobalSObjectResult sob) {
        return sob.isCustom();
    }
    
    private boolean keep(Field field) {
        // Leaving out the less interesting standard fields
        return field.isCustom()
                || field.getName().equalsIgnoreCase("Id")
                || field.getName().equalsIgnoreCase("Name");
    }
}
