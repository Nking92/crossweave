package edu.vu.isis.crossweave.example.pet;

import edu.vu.isis.crossweave.annotation.DesignPattern;

@DesignPattern.Role(alias = "dog", role = "adaptee")
public interface Dog {
    
    public void bark();
    public void wagTail();

}
