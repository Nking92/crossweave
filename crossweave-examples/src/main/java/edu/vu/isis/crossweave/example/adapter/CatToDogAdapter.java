package edu.vu.isis.crossweave.example.adapter;

import edu.vu.isis.crossweave.example.pet.Cat;
import edu.vu.isis.crossweave.example.pet.Dog;

public class CatToDogAdapter implements Dog {

    private Cat mCat;
    
    public CatToDogAdapter(Cat cat) {
        mCat = cat;
    }

    public void bark() {
        mCat.meow();
    }

    public void wagTail() {
        mCat.purr();
    }

}
