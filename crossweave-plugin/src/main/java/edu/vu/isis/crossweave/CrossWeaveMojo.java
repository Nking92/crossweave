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
import org.apache.maven.plugin.logging.Log;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Analyzes project source code and writes a pattern description file for the
 * patterns present in the code.
 * 
 * @goal analyze
 * @author nick
 */
public class CrossWeaveMojo extends AbstractMojo {

    private static Log logger;

    /**
     * Fully qualified name of the design pattern specification annotation
     * (multiple version)
     */
    public static final String PATTERN_SPECS_ANN_FQN = "edu.vu.isis.crossweave.annotation.DesignPattern$Specifications";

    /**
     * Fully qualified name of the design pattern specification annotation
     * (single version)
     */
    public static final String PATTERN_SPEC_ANN_FQN = "edu.vu.isis.crossweave.annotation.DesignPattern$Specification";

    /**
     * Fully qualified name of the design pattern role annotation (multiple
     * version)
     */
    public static final String PATTERN_ROLES_ANN_FQN = "edu.vu.isis.crossweave.annotation.DesignPattern$Roles";

    /**
     * Fully qualified name of the design pattern role annotation (single
     * version)
     */
    public static final String PATTERN_ROLE_ANN_FQN = "edu.vu.isis.crossweave.annotation.DesignPattern$Role";

    /**
     * The Java sources to parse.
     * 
     * @parameter default-value="${projec.build.sourceDirectory}"
     */
    private File source;

    /**
     * The file containing the StringTemplate template
     * 
     * @required
     */
    private String template;

    /**
     * The directory for the output file
     * 
     * @parameter default-value="${project.build.directory}"
     */
    private File outputDir;

    /**
     * Name of file to write output to
     * 
     * @parameter default-value="PatternStructure"
     */
    private String outputFile;

    /**
     * The file containing the design pattern definitions
     * 
     * @required
     */
    private File patternDef;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // StaticLoggerBinder.getSingleton().setLog(getLog());
        // logger = LoggerFactory.getLogger(CrossWeaveMojo.class);
        logger = getLog();
        Map<String, Pattern> patternMap = new HashMap<String, Pattern>();

        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(patternDef, new PatternDefHandler(patternMap));
        } catch (Exception e) {
            throw new MojoExecutionException("Could not parse pattern definition file", e);
        }

        logger.info("Keys in patternMap: " + patternMap.keySet().toString());

        JavaDocBuilder builder = new JavaDocBuilder();

        try {
            if (source.getName().endsWith(".java")) {
                builder.addSource(source);
            } else {
                builder.addSourceTree(source);
            }
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Could not find file: " + source, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not read sources", e);
        }

        JavaSource[] sources = builder.getSources();
        Map<String, PatternInstance> instanceMap = new HashMap<String, PatternInstance>();
        scanPatternSpecs(sources, patternMap, instanceMap);

        logger.info("Keys in instanceMap: " + instanceMap.keySet().toString());

        scanRoles(sources, instanceMap);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        FileWriter fw = null;
        try {
            if (outputFile == null) {
                outputFile = "PatternStructure";
            }
            File file = new File(outputDir, outputFile);
            fw = new FileWriter(file);

            STGroup stg = new STGroupFile(template);
            reportPatternErrors(instanceMap, fw, stg);
            printStats(instanceMap, fw, stg);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write output", e);
        } finally {
            if (fw != null) {
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
                for (Annotation ann : clazz.getAnnotations()) {
                    if (ann.getType().getFullyQualifiedName().equals(PATTERN_SPECS_ANN_FQN)) {
                        @SuppressWarnings("unchecked")
                        List<Annotation> list = (List<Annotation>) ann.getNamedParameter("specs");
                        for (Annotation a : list) {
                            processSpecAnnotation(a, patternMap, instanceMap);
                        }
                    } else if (ann.getType().getFullyQualifiedName().equals(PATTERN_SPEC_ANN_FQN)) {
                        processSpecAnnotation(ann, patternMap, instanceMap);
                    }
                }
            }
        }
    }

    public static void scanRoles(JavaSource[] sources, Map<String, PatternInstance> instanceMap) {
        for (JavaSource src : sources) {
            for (JavaClass clazz : src.getClasses()) {
                for (Annotation ann : clazz.getAnnotations()) {
                    if (ann.getType().getFullyQualifiedName().equals(PATTERN_ROLES_ANN_FQN)) {
                        @SuppressWarnings("unchecked")
                        List<Annotation> list = (List<Annotation>) ann.getNamedParameter("roles");
                        for (Annotation a : list) {
                            processRoleAnnotation(a, instanceMap, clazz);
                        }
                    } else if (ann.getType().getFullyQualifiedName().equals(PATTERN_ROLE_ANN_FQN)) {
                        processRoleAnnotation(ann, instanceMap, clazz);
                    }
                }
            }
        }
    }

    public static void reportPatternErrors(Map<String, PatternInstance> instanceMap,
            FileWriter out, STGroup template)
            throws IOException {
        boolean errorFound = false;
        ST st = template.getInstanceOf("reportPatternErrors");
        logger.info("owidfj " + st.getName());
        st.add("instanceSet", instanceMap.values());
        out.write(st.render());
        /*
        for (String alias : instanceMap.keySet()) {
            PatternInstance pat = instanceMap.get(alias);
            if (pat.hasEmptyRoles()) {
                errorFound = true;
                ST st = template.getInstanceOf("error");
                st.add("fqn", pat.getFullyQualifiedName());
                for (Role role : pat.getEmptyRoles()) {
                    st.add("emptyRole", role);
                }
                out.write(st.render());
            } else {
                logger.info("PatternInstance " + pat.getFullyQualifiedName()
                        + " has all roles filled");
                logger.info("Implementers: ");
                for (Role role : pat.getRoles()) {
                    logger.info("\tRole " + role.getName() + ": "
                            + role.getImplementers().toString());
                }
            }
        } */

        if (!errorFound) {
//            ST st = template.getInstanceOf("noError");
//            out.write(st.render());
        }
        
        out.write("\n");

    }

    /**
     * @param instanceMap
     * @param out
     * @throws IOException
     */
    public static void printStats(Map<String, PatternInstance> instanceMap, FileWriter out,
            STGroup template) throws IOException {
        for (String alias : instanceMap.keySet()) {
            ST stPatternDesc = template.getInstanceOf("patDesc");
            PatternInstance pat = instanceMap.get(alias);
            stPatternDesc.add("fqn", pat.getFullyQualifiedName());
            stPatternDesc.add("patName", pat.getPattern().getName());
            stPatternDesc.add("alias", alias);
            out.write(stPatternDesc.render());
            Collection<Role> roles = pat.getRoles();
            for (Role role : roles) {
                ST stRoleDesc = template.getInstanceOf("roleDesc");
                stRoleDesc.add("roleName", role.getName());
                if (role.hasNoImplementers()) {
                } else {
                    for (String implementer : role.getImplementers()) {
                        stRoleDesc.add("implementer", implementer);
                    }
                }
                out.write(stRoleDesc.render());
            }
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

    private static final void processSpecAnnotation(
            Annotation ann,
            Map<String, Pattern> patternMap,
            Map<String, PatternInstance> instanceMap) {
        String alias = getParamAndTrimQuotes(ann, "alias");
        if (instanceMap.containsKey(alias)) {
            logger.warn("Found multiple specs for pattern with alias " + alias);
            return;
        }

        String namespace = getParamAndTrimQuotes(ann, "namespace");
        String patternName = getParamAndTrimQuotes(ann, "patternName");
        String impl = getParamAndTrimQuotes(ann, "impl");
        String instanceName = getParamAndTrimQuotes(ann, "instanceName");
        String fullyQualifiedName = namespace + "." + patternName + "." + impl;

        Pattern pattern = patternMap.get(fullyQualifiedName);
        if (pattern == null) {
            logger.error("Pattern with FQN " + fullyQualifiedName
                    + " not found in patternMap");
            return;
        }

        instanceMap.put(alias, pattern.instantiate(instanceName));
        logger.info("Pattern " + pattern.getFullyQualifiedName()
                + " instantiated to " + instanceName + " with alias " + alias);
    }

    private static final void processRoleAnnotation(
            Annotation ann,
            Map<String, PatternInstance> instanceMap,
            JavaClass clazz) {
        String alias = getParamAndTrimQuotes(ann, "alias");
        String role = getParamAndTrimQuotes(ann, "role");

        PatternInstance pat = instanceMap.get(alias);

        if (pat == null) {
            logger.error("No pattern to match alias " + alias + " on role " + role
                    + " in class " + clazz.getFullyQualifiedName());
            return;
        }

        if (pat.addImplementerToRole(clazz.getFullyQualifiedName(), role)) {
            logger.info("Implementer " + clazz.getFullyQualifiedName()
                    + " added to role " + role + " for pattern "
                    + pat.getPattern().getName());
        } else {
            logger.error("Pattern instance alias " + alias
                    + " does not have a role called " + role);
            logger.error("Roles in pattern instance: " + pat.getRoles().toString());
            logger.error("Could not add role " + role + " to pattern "
                    + pat.getPattern().getName());
        }
    }

}
