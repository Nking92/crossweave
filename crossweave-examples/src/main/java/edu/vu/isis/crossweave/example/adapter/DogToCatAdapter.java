package edu.vu.isis.crossweave.example.adapter;

import edu.vu.isis.crossweave.annotation.DesignPattern;
import edu.vu.isis.crossweave.example.pet.Cat;
import edu.vu.isis.crossweave.example.pet.Dog;

@DesignPattern.Role(alias = "dog", role = "adapter")
public class DogToCatAdapter implements Cat {

    private Dog mDog;
    
    public DogToCatAdapter(Dog dog) {
        mDog = dog;
    }
    
    public void meow() {
        mDog.bark();
    }

    public void purr() {
        mDog.wagTail();
    }

}
