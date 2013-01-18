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
 * Keeps track of what is implementing a particular role of a design pattern
 * 
 * @author nick
 */
public class Role {

    private String mName;
    private Collection<String> mImplementers = new ArrayList<String>();

    /**
     * @param name The name of the role
     */
    public Role(String name) {
        mName = name;
    }

    /**
     * @return The name of this role
     */
    public String getName() {
        return mName;
    }

    /**
     * Adds an implementer to this role. Multiple implementers of the same name
     * can be added.
     * 
     * @param implementer The name of the thing that is implementing this role
     */
    public void addImplementer(String implementer) {
        mImplementers.add(implementer);
    }

    /**
     * Determines whether an implementer of a given name implements this role
     * 
     * @param implementer The name of the implementer
     * @return true if the given implementer implements this role, false
     *         otherwise
     */
    public boolean isImplementer(String implementer) {
        return mImplementers.contains(implementer);
    }

    /**
     * Removes the implementer from this role
     * 
     * @param implementer The name of the implementer to remove
     */
    public void removeImplementer(String implementer) {
        mImplementers.remove(implementer);
    }

    /**
     * Determines whether this role has no implementers
     * 
     * @return true if nothing is implementing this role, false otherwise
     */
    public boolean hasNoImplementers() {
        return mImplementers.isEmpty();
    }

    /**
     * @return the names of the implementers of this role
     */
    public Collection<String> getImplementers() {
        return new ArrayList<String>(mImplementers);
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Role))
            return false;
        Role role = (Role) o;
        return role.mName == mName;
    }

    @Override
    public String toString() {
        return mName;
    }

}
