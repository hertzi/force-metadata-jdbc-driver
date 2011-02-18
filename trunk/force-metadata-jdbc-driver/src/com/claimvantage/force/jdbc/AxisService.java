package com.claimvantage.force.jdbc;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.rpc.ServiceException;

import com.sforce.soap.partner.ChildRelationship;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PicklistEntry;
import com.sforce.soap.partner.RecordTypeInfo;
import com.sforce.soap.partner.SessionHeader;
import com.sforce.soap.partner.SforceServiceLocator;
import com.sforce.soap.partner.SoapBindingStub;
import com.sforce.soap.partner.fault.InvalidIdFault;
import com.sforce.soap.partner.fault.InvalidSObjectFault;
import com.sforce.soap.partner.fault.LoginFault;
import com.sforce.soap.partner.fault.UnexpectedErrorFault;

/**
 * Wraps the Force.com describe calls web service outputting simple data objects.
 *
 * Uses Axis.
 */
@Deprecated
public class AxisService {
    
    private Filter filter;
    private SoapBindingStub binding;

    public AxisService(String un, String pw, String url, Filter filter) throws ServiceException, UnexpectedErrorFault,
            InvalidIdFault, LoginFault, RemoteException {
        
        this.filter = filter;

        SforceServiceLocator locator = new SforceServiceLocator();
        if (url != null) {
            locator.setSoapEndpointAddress(url);
        }
        log("Force.com connection url " + locator.getSoapAddress());
        log(filter.toString());
        
        binding = (SoapBindingStub) locator.getSoap();
        binding.setTimeout(60000);
        
        LoginResult loginResult = binding.login(un, pw);
        binding._setProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY, loginResult.getServerUrl());
        SessionHeader sh = new SessionHeader();
        sh.setSessionId(loginResult.getSessionId());
        binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(), "SessionHeader", sh);
    }
    
    private void log(String message) {
        System.out.println("ForceMetaDataDriver: " + message);
    }
    
    /**
     * Grab the describe data and return it wrapped in a factory.r
     */
    public ResultSetFactory createResultSetFactory() throws InvalidSObjectFault, UnexpectedErrorFault, RemoteException {

        ResultSetFactory factory = new ResultSetFactory();
        Map<String, String> childParentReferenceNames = new HashMap<String, String>();
        Map<String, Boolean> childCascadeDeletes = new HashMap<String, Boolean>();
        List<String> typesList = getSObjectTypes();
        Set<String> typesSet = new HashSet<String>(typesList);
        List<String[]> batchedTypes = batch(typesList);
        
        // Need all child references so run through the batches first
        for (String[] batch : batchedTypes) {
            DescribeSObjectResult[] sobs = binding.describeSObjects(batch);
            if (sobs != null) {
                for (DescribeSObjectResult sob : sobs) {
                    ChildRelationship[] crs = sob.getChildRelationships();
                    if (crs != null) {
                        for (ChildRelationship cr : crs) {
                            if (typesSet.contains(cr.getChildSObject())) {
                                String qualified = cr.getChildSObject() + '.' + cr.getField();
                                childParentReferenceNames.put(qualified, cr.getRelationshipName());
                                childCascadeDeletes.put(qualified, cr.isCascadeDelete());
                            }
                        }
                    }
                }
            }
        }

        // Run through the batches again now the child references are available
        for (String[] batch : batchedTypes) {
            DescribeSObjectResult[] sobs = binding.describeSObjects(batch);
            if (sobs != null) {
                for (DescribeSObjectResult sob : sobs) {
                    Field[] fields = sob.getFields();
                    List<Column> columns = new ArrayList<Column>(fields.length);
                    for (Field field : fields) {
                        if (keep(field)) {
                            Column column = new Column(field.getName(), getType(field));
                            columns.add(column);

                            column.setLength(getLength(field));
                            
                            if ("reference".equals(field.getType().toString())) {
                                // MasterDetail vs Reference apparently not in API; cascade delete is though
                                String qualified = sob.getName() + "." + field.getName();
                                String childParentReferenceName = childParentReferenceNames.get(qualified);
                                Boolean cascadeDelete = childCascadeDeletes.get(qualified);
                                if (childParentReferenceName != null && cascadeDelete != null) {
                                    column.setComments("Referenced: " + childParentReferenceName
                                            + (cascadeDelete ? " (cascade delete)" : ""));
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
                                    if (typesSet.contains(referenceTo)) {
                                        column.setReferencedTable(referenceTo);
                                        column.setReferencedColumn("Id");
                                    }
                                }
                            }
                        }
                    }
    
                    Table table = new Table(sob.getName(), getRecordTypes(sob.getRecordTypeInfos()), columns);
                    factory.addTable(table);
                }
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
    
    // Avoid EXCEEDED_MAX_TYPES_LIMIT on call by breaking into batches
    private List<String[]> batch(List<String> types) throws UnexpectedErrorFault, RemoteException {

        List<String[]> batchedTypes = new ArrayList<String[]>();
        
        final int batchSize = 100;
        for (int batch = 0; batch < (types.size() + batchSize - 1) / batchSize; batch++) {
            int from = batch * batchSize;
            int to = (batch + 1) * batchSize;
            if (to > types.size()) {
                to = types.size();
            }
            List<String> t = types.subList(from, to);
            String[] a = new String[t.size()];
            t.toArray(a);
            batchedTypes.add(a);
        }
        
        return batchedTypes;
    }
    
    private List<String> getSObjectTypes() throws UnexpectedErrorFault, RemoteException {
        
        DescribeGlobalSObjectResult[] sobs = binding.describeGlobal().getSobjects();
        
        List<String> list = new ArrayList<String>();
        for (DescribeGlobalSObjectResult sob : sobs) {
            if (keep(sob)) {
                list.add(sob.getName());
            }
        }
        return list;
    }
    
    private boolean keep(DescribeGlobalSObjectResult sob) {
        // Filter tables.
        // Normally want the User table filtered as all objects are associated with that
        // so the graphs become a mess and very slow to generate.
        return filter.accept(sob);
    }
    
    private boolean keep(Field field) {
        // Keeping all fields
        return true;
    }
}
