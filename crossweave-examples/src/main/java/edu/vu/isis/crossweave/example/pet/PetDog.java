
package edu.vu.isis.crossweave.example.pet;

public class PetDog extends Pet implements Dog {

    public PetDog(String name, boolean isMale) {
        super(name, isMale);
    }

    public void bark() {
        System.out.println(mName + " says, \"Woof!\"");
    }

    public void wagTail() {
        System.out.println(mName + " wags " + (mIsMale ? "his" : "her") + " tail happily.");
    }

}
