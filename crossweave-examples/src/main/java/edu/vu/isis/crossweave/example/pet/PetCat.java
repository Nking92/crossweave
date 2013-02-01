
package edu.vu.isis.crossweave.example.pet;

public class PetCat extends Pet implements Cat {

    public PetCat(String name, boolean isMale) {
        super(name, isMale);
    }

    public void meow() {
        System.out.println(mName + " says, \"Meow!\"");
    }

    public void purr() {
        System.out.println(mName + " purrs when you pet " + (mIsMale ? "him" : "her") + ".");
    }

}
