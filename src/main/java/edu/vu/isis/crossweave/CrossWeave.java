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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class CrossWeave extends AbstractMojo {

    // TODO: Use maven plugin loggers
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
     * The Java sources to parse.
     */
    @Parameter(property="source", defaultValue="${project.build.sourceDirectory}")
    private File mSrc;
    
    /**
     * The file containing the StringTemplate template
     */
    @Parameter(property="template", defaultValue="") 
    // TODO: We want to be able to supply a default template file,
    // but where should we put it, and how do we refer to it?
    private File mStrTemplate;
    
    /**
     * The directory for the output file
     */
    @Parameter(property="outputDir", defaultValue="${project.build.directory}")
    private File mOutputDir;
    
    /**
     * Name of file to write output to
     */
    @Parameter(property="outputFile", defaultValue="PatternStructure")
    private String mOutputFilename;
    
    /**
     * The file containing the design pattern definitions
     */
    @Parameter(property="patternDef", required=true)
    private File mPatternDef;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map<String, Pattern> patternMap = new HashMap<String, Pattern>();
        
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(mPatternDef, new PatternDefHandler(patternMap));
        } catch (Exception e) {
            throw new MojoExecutionException("Could not parse pattern definition file", e);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Keys in patternMap: {}", patternMap.keySet().toString());
        }

        JavaDocBuilder builder = new JavaDocBuilder();
        
        try {
            if (mSrc.getName().endsWith(".java")) {
                builder.addSource(mSrc);
            } else {
                builder.addSourceTree(mSrc);
            }
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Could not find file: " + mSrc, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not read sources", e);
        }

        JavaSource[] sources = builder.getSources();
        Map<String, PatternInstance> instanceMap = new HashMap<String, PatternInstance>();
        scanPatternSpecs(sources, patternMap, instanceMap);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Keys in instanceMap: {}", instanceMap.keySet().toString());
        }

        scanRoles(sources, instanceMap);

        
        if(!mOutputDir.exists()) {
            mOutputDir.mkdirs();
        }
        
        FileWriter fw = null;
        try {
            File outputFile = new File(mOutputDir, mOutputFilename);
            fw = new FileWriter(outputFile);
            
            reportPatternErrors(instanceMap, fw);
            printStats(instanceMap, fw);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write output", e);
        } finally {
            if(fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
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

    public static void reportPatternErrors(Map<String, PatternInstance> instanceMap, FileWriter out) throws IOException {
        boolean errorFound = false;
        for (String alias : instanceMap.keySet()) {
            PatternInstance pat = instanceMap.get(alias);
            if (pat.hasEmptyRoles()) {
                errorFound = true;
                String fullyQualifiedName = pat.getFullyQualifiedName();
                out.write("Pattern " + fullyQualifiedName + " has empty roles:");
                for (Role role : pat.getEmptyRoles()) {
                    out.write("\t" + role.getName());
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
            out.write("No pattern errors found");
        }

        out.write('\n');
    }


    /**
     * @param instanceMap
     * @param out
     * @throws IOException 
     */
    public static void printStats(Map<String, PatternInstance> instanceMap, FileWriter out/*, STGroup template*/) throws IOException {
        //template.
        out.write("Summary of pattern structure: ");
        for (String alias : instanceMap.keySet()) {
            PatternInstance pat = instanceMap.get(alias);
            out.write("Pattern: " + pat.getFullyQualifiedName() + " (alias " + alias + ")");
            Collection<Role> roles = pat.getRoles();
            for (Role role : roles) {
                out.write("\tRole: " + role.getName());
                if (role.hasNoImplementers()) {
                    out.write("\t\tNo implementers");
                } else {
                    for (String implementer : role.getImplementers()) {
                        out.write("\t\tImplementer: " + implementer);
                    }
                }
            }
            out.write('\n');
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
