/*Copyright (C) 2010-2013 Institute for Software Integrated Systems (ISIS)
This software was developed by the Institute for Software Integrated
Systems (ISIS) at Vanderbilt University, Tennessee, USA for the 
Transformative Apps program under DARPA, Contract # HR011-10-C-0175.
The United States Government has unlimited rights to this software. 
The US government has the right to use, modify, reproduce, release, 
perform, display, or disclose computer software or computer software 
documentation in whole or in part, in any manner and for any 
purpose whatsoever, and to have or authorize others to do so.
 */
package edu.vu.isis.crossweave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses an xml file with pattern definitions and creates corresponding Pattern
 * objects
 * 
 * @author nick
 */
public class PatternDefHandler extends DefaultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatternDefHandler.class);

    private Map<String, Pattern> mPatternMap;
    // private Pattern mCurPattern = null;
    private boolean mIsPatternChild = false;
    private String mCurPatternName;
    private String mCurPatternNamespace;
    private List<String> mCurPatternImpls = new ArrayList<String>();
    private List<Role> mCurPatternRoles = new ArrayList<Role>();

    /**
     * @param patternMap The Map that will be filled with Strings of pattern
     *            names mapped to Patterns
     */
    public PatternDefHandler(Map<String, Pattern> patternMap) {
        mPatternMap = patternMap;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equals("pattern")) {
            if (mIsPatternChild)
                throw new SAXException("pattern element in bad location");
            mIsPatternChild = true;
            mCurPatternNamespace = attributes.getValue("namespace");
            mCurPatternName = attributes.getValue("name");
            LOGGER.debug("Pattern element: namespace={} name={}", mCurPatternNamespace,
                    mCurPatternName);
        } else if (qName.equals("role")) {
            if (!mIsPatternChild)
                throw new SAXException("role element must be a child of pattern element");
            String name = attributes.getValue("name");
            Role role = new Role(name);
            mCurPatternRoles.add(role);
            LOGGER.debug("Role element: name={}", name);
        } else if (qName.equals("impl")) {
            if (!mIsPatternChild)
                throw new SAXException("impl element must be a child of pattern element");
            String name = attributes.getValue("name");
            mCurPatternImpls.add(name);
            LOGGER.debug("Pattern impl element: name={}", name);
        } else {
            throw new SAXException("Unknown element: " + qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("pattern")) {
            mIsPatternChild = false;

            if (mCurPatternRoles.isEmpty()) {
                LOGGER.warn("Pattern def for pattern name={} namespace={} " +
                        "has no roles specified. Pattern will be created with no roles.",
                        mCurPatternName,
                        mCurPatternNamespace);
            }

            // Instantiate patterns with impls specified
            if (mCurPatternImpls.isEmpty()) {
                LOGGER.info("Pattern def for pattern name={} namespace={} " +
                        "has no impls specified",
                        mCurPatternName, mCurPatternNamespace);
                addPatternToMap(mCurPatternNamespace, mCurPatternName, "");
            } else {
                for (String impl : mCurPatternImpls) {
                    addPatternToMap(mCurPatternNamespace, mCurPatternName, impl);
                }
            }

            // Clean up lists
            mCurPatternRoles.clear();
            mCurPatternImpls.clear();
            LOGGER.debug("End of pattern element reached");
        }
    }

    private void addPatternToMap(String namespace, String name, String impl) {
        Pattern pattern = new Pattern(namespace, name, impl);
        for (Role role : mCurPatternRoles) {
            pattern.addRole(role);
        }
        mPatternMap.put(pattern.getFullyQualifiedName(), pattern);
    }
}
