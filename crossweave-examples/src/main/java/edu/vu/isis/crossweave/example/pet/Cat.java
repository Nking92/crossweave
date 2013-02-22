package edu.vu.isis.crossweave.example.pet;

import edu.vu.isis.crossweave.annotation.DesignPattern;

@DesignPattern.Role(alias = "cat", role = "adaptee")
public interface Cat {

    public void meow();
    public void purr();
    
}
