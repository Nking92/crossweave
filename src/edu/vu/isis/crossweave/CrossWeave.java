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

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.Annotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class CrossWeave {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrossWeave.class);

    /**
     * Fully qualified name of the design pattern specification annotation
     */
    public static final String PATTERN_SPEC_ANN_FQN = "edu.vu.isis.ammo.annotation.DesignPattern$Specification";

    /**
     * Fully qualified name of the design pattern role annotation
     */
    public static final String PATTERN_ROLE_ANN_FQN = "edu.vu.isis.ammo.annotation.DesignPattern$Role";

    /**
     * Print stream to send output to
     */
    private static final PrintStream OUTPUT_STREAM = System.out;

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        if (args.length != 2) {
            usage();
            return;
        }

        String xmlFilename = args[0];
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        Map<String, Pattern> patternMap = new HashMap<String, Pattern>();

        parser.parse(new File(xmlFilename), new PatternDefHandler(patternMap));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Keys in patternMap: {}", patternMap.keySet().toString());
        }

        String javaFilename = args[1];
        LOGGER.trace("Java file name: {}", javaFilename);
        JavaDocBuilder builder = new JavaDocBuilder();
        if (javaFilename.endsWith(".java")) {
            builder.addSource(new File(javaFilename));
        } else {
            builder.addSourceTree(new File(javaFilename));
        }

        JavaSource[] sources = builder.getSources();
        Map<String, PatternInstance> instanceMap = new HashMap<String, PatternInstance>();
        scanPatternSpecs(sources, patternMap, instanceMap);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Keys in instanceMap: {}", instanceMap.keySet().toString());
        }

        scanRoles(sources, instanceMap);

        reportPatternErrors(instanceMap, OUTPUT_STREAM);
        printStats(instanceMap, OUTPUT_STREAM);

        long end = System.nanoTime();
        LOGGER.trace("Execution time: " + (end - start) + "ns (" + (end - start) / 1000000 + "ms)");
    }

    public static void usage() {
        System.out.println("Usage:");
        System.out.println("CrossWeave [pattern definition file] " +
                "[java source file/directory]");
    }

    public static void scanPatternSpecs(JavaSource[] sources, Map<String, Pattern> patternMap,
            Map<String, PatternInstance> instanceMap) {
        for (JavaSource src : sources) {
            for (JavaClass clazz : src.getClasses()) {
                // LOGGER.trace("Class found: {}", clazz.getName());
                for (Annotation ann : clazz.getAnnotations()) {
                    // LOGGER.trace("\tAnnotation found: {}", ann.getType());
                    if (ann.getType().getFullyQualifiedName().equals(PATTERN_SPEC_ANN_FQN)) {
                        String alias = getParamAndTrimQuotes(ann, "alias");
                        if (instanceMap.containsKey(alias)) {
                            LOGGER.error("Found multiple specs for pattern with alias {}", alias);
                            LOGGER.error("Skipping spec in class {}", clazz.getName());
                            continue;
                        }

                        String namespace = getParamAndTrimQuotes(ann, "namespace");
                        String patternName = getParamAndTrimQuotes(ann, "patternName");
                        String impl = getParamAndTrimQuotes(ann, "impl");
                        String instanceName = getParamAndTrimQuotes(ann, "instanceName");
                        String fullyQualifiedName = namespace + "." + patternName + "." + impl;

                        Pattern pattern = patternMap.get(fullyQualifiedName);
                        if (pattern == null) {
                            LOGGER.error("Pattern with FQN {} not found in patternMap",
                                    fullyQualifiedName);
                            continue;
                        }

                        instanceMap.put(alias, pattern.instantiate(instanceName));
                        LOGGER.debug("Pattern {} instantiated to {} with alias {}",
                                pattern.getFullyQualifiedName(), instanceName, alias);
                    } else {
                        // LOGGER.trace("{} did not match {}",
                        // ann.getType().getFullyQualifiedName(),
                        // PATTERN_SPEC_ANN_FQN);
                    }
                }
            }
        }
    }

    public static void scanRoles(JavaSource[] sources, Map<String, PatternInstance> instanceMap) {
        for (JavaSource src : sources) {
            for (JavaClass clazz : src.getClasses()) {
                for (Annotation ann : clazz.getAnnotations()) {
                    if (ann.getType().getFullyQualifiedName().equals(PATTERN_ROLE_ANN_FQN)) {
                        String alias = getParamAndTrimQuotes(ann, "alias");
                        String role = getParamAndTrimQuotes(ann, "role");

                        PatternInstance pat = instanceMap.get(alias);
                        
                        if (pat == null) {
                            LOGGER.error("No pattern to match alias {} on role {} in class {}",
                                    alias, role, clazz.getFullyQualifiedName());
                            continue;
                        }
                        
                        if (pat.addImplementerToRole(clazz.getFullyQualifiedName(), role)) {
                            LOGGER.debug("Implementer {} added to role {} for pattern {}",
                                    clazz.getFullyQualifiedName(), role, pat.getPattern().getName());
                        } else {
                            LOGGER.warn("Pattern instance alias {} does not have a role called {}",
                                    alias, role);
                            LOGGER.warn("Roles in pattern instance: {}", pat.getRoles().toString());
                            LOGGER.error("Could not add role {} to pattern {}", role, pat
                                    .getPattern().getName());
                        }
                    }
                }
            }
        }
    }

    public static void reportPatternErrors(Map<String, PatternInstance> instanceMap, PrintStream out) {
        boolean errorFound = false;
        for (String alias : instanceMap.keySet()) {
            PatternInstance pat = instanceMap.get(alias);
            if (pat.hasEmptyRoles()) {
                errorFound = true;
                String fullyQualifiedName = pat.getFullyQualifiedName();
                out.println("Pattern " + fullyQualifiedName + " has empty roles:");
                for (Role role : pat.getEmptyRoles()) {
                    out.println("\t" + role.getName());
                }
            } else if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("PatternInstance {} has all roles filled", pat.getFullyQualifiedName());
                LOGGER.trace("Implementers: ");
                for (Role role : pat.getRoles()) {
                    LOGGER.trace("\tRole {}: {}", role.getName(), role.getImplementers().toString());
                }
            }
        }

        if (!errorFound) {
            out.println("No pattern errors found");
        }

        out.println();
    }


    /**
     * @param instanceMap
     * @param out
     */
    private static final String PRINT_STATS_TEMPLATE = "Summary of pattern structure: " +
            "<pat:{x | Pattern: <pat.fullyQualifiedName> (alias <alias>)";
    public static void printStats(Map<String, PatternInstance> instanceMap, PrintStream out) {
        out.println("Summary of pattern structure: ");
        for (String alias : instanceMap.keySet()) {
            PatternInstance pat = instanceMap.get(alias);
            out.println("Pattern: " + pat.getFullyQualifiedName() + " (alias " + alias + ")");
            Collection<Role> roles = pat.getRoles();
            for (Role role : roles) {
                out.println("\tRole: " + role.getName());
                if (role.hasNoImplementers()) {
                    out.println("\t\tNo implementers");
                } else {
                    for (String implementer : role.getImplementers()) {
                        out.println("\t\tImplementer: " + implementer);
                    }
                }
            }
            out.println();
        }
    }

    private static String trimQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String getParamAndTrimQuotes(Annotation ann, String key) {
        String param = (String) ann.getNamedParameter(key);
        if (param == null) {
            return "";
        } else {
            return trimQuotes(param);
        }
    }
}
