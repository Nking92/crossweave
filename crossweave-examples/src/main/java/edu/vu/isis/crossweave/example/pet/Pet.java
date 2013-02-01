package edu.vu.isis.crossweave.example.pet;

public abstract class Pet {

    protected String mName;
    protected boolean mIsMale;
    
    protected Pet(String name, boolean isMale) {
        mName = name;
        mIsMale = isMale;
    }
    
}
