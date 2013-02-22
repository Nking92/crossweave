
package edu.vu.isis.crossweave.example;

import edu.vu.isis.crossweave.annotation.DesignPattern;
import edu.vu.isis.crossweave.example.adapter.CatToDogAdapter;
import edu.vu.isis.crossweave.example.adapter.DogToCatAdapter;
import edu.vu.isis.crossweave.example.pet.Cat;
import edu.vu.isis.crossweave.example.pet.Dog;
import edu.vu.isis.crossweave.example.pet.PetCat;
import edu.vu.isis.crossweave.example.pet.PetDog;

@DesignPattern.Specifications(specs =
{
    @DesignPattern.Specification(alias = "dog", instanceName = "snoopy", namespace = "gof", patternName = "adapter", impl="foo"),
    @DesignPattern.Specification(alias = "cat", instanceName = "tom", namespace = "gof", patternName = "adapter", impl="bar")
})
@DesignPattern.Roles(roles =
{
    @DesignPattern.Role(alias = "cat", role = "client"),
    @DesignPattern.Role(alias = "dog", role = "client")
})
public class Main {

    public static void main(String[] args) {
        // Demonstrate the adapter pattern
        Cat tom = new PetCat("Tom", true);

        Dog snoopy = new PetDog("Snoopy", true);

        Dog tomTheDog = new CatToDogAdapter(tom);
        Cat snoopyTheCat = new DogToCatAdapter(snoopy);

        tomTheDog.bark();
        tomTheDog.wagTail();

        snoopyTheCat.meow();
        snoopyTheCat.purr();
    }

}
