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

import java.util.ArrayList;
import java.util.Collection;

/**
 * An instance of a Pattern. Whereas a Pattern is a model for a particular
 * design pattern, a PatternInstance tracks the classes that are members of a
 * particular usage of a pattern. For example, foo could be an instance of the
 * Adapter pattern in which the classes FooAdapter, FooClient, and FooAdaptee
 * implement particular roles of the pattern.
 * 
 * @author nick
 */
public class PatternInstance {
    
    private Pattern mPattern;
    private String mInstanceName;
    private Collection<Role> mRoles;

    /**
     * @param pattern The Pattern that we are instantiating
     * @param instanceName The name of this PatternInstance
     * @param roles The roles that are part of this PatternInstance
     */
    public PatternInstance(Pattern pattern, String instanceName, Collection<Role> roles) {
        mPattern = pattern;
        mInstanceName = instanceName;
        mRoles = roles;
    }

    /**
     * @return The pattern that is instantiated
     */
    public Pattern getPattern() {
        return mPattern;
    }

    /**
     * @return The Roles in this PatternInstance
     */
    public Collection<Role> getRoles() {
        return new ArrayList<Role>(mRoles);
    }

    /**
     * Adds an implementer to a role in this pattern instance
     * 
     * @param implementer The name of the entity implementing the role
     * @param role The role to add an implementer to
     * @return true if the implementer was added, false if not
     */
    public boolean addImplementerToRole(String implementer, Role role) {
        return addImplementerToRole(implementer, role.getName());
    }

    /**
     * Adds an implementer to a role in this pattern instance
     * 
     * @param implementer The name of the entity implementing the role
     * @param roleName The name of the role
     * @return true if the implementer was added, false if not
     */
    public boolean addImplementerToRole(String implementer, String roleName) {
        for (Role role : mRoles) {
            if (role.getName().equals(roleName)) {
                role.addImplementer(implementer);
                return true;
            }
        }
        return false;
    }

    /**
     * @return The roles in this pattern that have no implementers
     */
    public Collection<Role> getEmptyRoles() {
        Collection<Role> emptyRoles = new ArrayList<Role>();
        for (Role role : mRoles) {
            if (role.hasNoImplementers())
                emptyRoles.add(role);
        }
        return emptyRoles;
    }

    /**
     * @return true if this PatternInstance has roles that have not been filled,
     *         false otherwise
     */
    public boolean isUnfilled() {
        for (Role role : mRoles) {
            if (role.hasNoImplementers())
                return true;
        }
        return false;
    }

    /**
     * @return the fully qualified name of this PatternInstance
     */
    public String getFullyQualifiedName() {
        return mPattern.getFullyQualifiedName() + "." + mInstanceName;
    }

}
