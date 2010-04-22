package com.claimvantage.force.jdbc;

import java.util.Properties;

import junit.framework.TestCase;

import com.sforce.soap.enterprise.DescribeGlobalSObjectResult;

public class FilterTest extends TestCase {

    public void testDefault() {
        
        Filter f = new Filter(null);
        
        assertEquals(false, f.accept(createStandard("Abc")));
        assertEquals(true, f.accept(createCustom("Abc")));
        assertEquals(true, f.accept(createCustom("USER")));
        assertEquals(true, f.accept(createCustom("event")));
    }
    
    public void testStandard() {
        
        Properties p = new Properties();
        p.put("exclude", "User,Event");
        p.put("standard", "true");
        p.put("custom", "false");
        Filter f = new Filter(p);
        
        assertEquals(true, f.accept(createStandard("Abc")));
        assertEquals(false, f.accept(createCustom("Abc")));
        assertEquals(false, f.accept(createStandard("USER")));
        assertEquals(false, f.accept(createStandard("event")));
    }
    
    public void testCustom() {
        
        Properties p = new Properties();
        p.put("exclude", " User, Event ");
        p.put("standard", " fALsE");
        p.put("custom", " tRuE ");
        Filter f = new Filter(p);

        assertEquals(false, f.accept(createStandard("Abc")));
        assertEquals(true, f.accept(createCustom("Abc")));
        assertEquals(false, f.accept(createStandard("USER")));
        assertEquals(false, f.accept(createStandard("event")));
    }

    private DescribeGlobalSObjectResult createStandard(String name) {
        DescribeGlobalSObjectResult d = new DescribeGlobalSObjectResult();
        d.setName(name);
        d.setCustom(false);
        return d;
    }
    
    private DescribeGlobalSObjectResult createCustom(String name) {
        DescribeGlobalSObjectResult d = new DescribeGlobalSObjectResult();
        d.setName(name);
        d.setCustom(true);
        return d;
    }
}
