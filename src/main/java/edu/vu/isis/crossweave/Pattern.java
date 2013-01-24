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

import java.util.Collection;
import java.util.HashSet;

/**
 * Object representation of a design pattern specified by a DesignPattern
 * annotation. This class represents the properties of a pattern. An instance of
 * a particular pattern is represented by a PatternInstance.
 * 
 * @author nick
 */
public class Pattern {

    private String mNamespace;
    private String mName;
    private String mImplementation;
    private Collection<Role> mRoles = new HashSet<Role>();

    /**
     * Creates a new Pattern
     * 
     * @param namespace the namespace of the pattern (e.g. GoF, POSA)
     * @param patternName the name of this pattern (e.g. Flyweight, Adapter)
     * @param implementation the implementation of this pattern (optional)
     */
    public Pattern(String namespace, String patternName, String implementation) {
        mNamespace = namespace;
        mName = patternName;

        if (implementation == null) {
            mImplementation = "";
        } else {
            mImplementation = implementation;
        }
    }

    /**
     * @return the pattern namespace
     */
    public String getNamespace() {
        return mNamespace;
    }

    /**
     * @return the pattern name
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the pattern implementation
     */
    public String getImplementation() {
        return mImplementation;
    }

    /**
     * adds a role to this pattern
     * 
     * @param role the role to add
     */
    public void addRole(Role role) {
        mRoles.add(role);
    }

    /**
     * removes a role from this pattern
     * 
     * @param role the role to remove
     */
    public void removeRole(Role role) {
        mRoles.remove(role);
    }

    /**
     * Creates an instance of this pattern with the given instanceName. If you
     * call this method twice with the instanceName foo, it will return
     * different PatternInstance objects with the instanceName foo.
     * 
     * @param instanceName the name of the PatternInstance
     * @return
     */
    public PatternInstance instantiate(String instanceName) {
        // We need to do a deep copy of the all the roles so that they
        // are unique across pattern instances
        HashSet<Role> copySet = new HashSet<Role>(mRoles.size());
        for (Role role : mRoles) {
            Role copyRole = new Role(role.getName());
            copySet.add(copyRole);
        }
        return new PatternInstance(this, instanceName, copySet);
    }

    /**
     * @return the fully qualified name of this pattern
     */
    public String getFullyQualifiedName() {
        return mNamespace + "." + mName + "." + mImplementation;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 31 + mNamespace.hashCode();
        result = result * 31 + mName.hashCode();
        result = result * 31 + mImplementation.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pattern)) {
            return false;
        }
        Pattern pattern = (Pattern) o;
        return pattern.mNamespace == this.mNamespace
                && pattern.mName == this.mName
                && pattern.mImplementation == this.mImplementation;
    }

}
