package com.claimvantage.force.jdbc;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.sforce.soap.enterprise.DescribeGlobalSObjectResult;

public class Filter {
    
    private boolean keepStandard = false;
    private boolean keepCustom = true;
    private Set<String> exclusionNames = new HashSet<String>();
    
    /**
     * Supports for example "exclude=User,Event;standard=true;custom=false".
     */
    public Filter(Properties info) {
        if (info != null) {
            for (Object o : info.keySet()) {
                String key = ((String) o).trim();
                String value = info.getProperty(key).trim();
                if (key.equals("exclude")) {
                    String[] es = value.split(",");
                    for (String e : es) {
                        if (e.trim().length() != 0) {
                            exclusionNames.add(e.trim().toLowerCase());
                        }
                    }
                } else if (key.equals("standard")) {
                    keepStandard = Boolean.parseBoolean(value);
                } else if (key.equals("custom")) {
                    keepCustom = Boolean.parseBoolean(value);
                }
            }
        }
    }
    
    public boolean accept(DescribeGlobalSObjectResult sob) {
        if (sob.isCustom() && !keepCustom) {
            return false;
        }
        if (!sob.isCustom() && !keepStandard) {
            return false;
        }
        return !exclusionNames.contains(sob.getName().toLowerCase());
    }

    @Override
    public String toString() {
        return "Filter [exclusionNames=" + exclusionNames
                + ", keepCustom=" + keepCustom
                + ", keepStandard=" + keepStandard
                + "]";
    }
}
