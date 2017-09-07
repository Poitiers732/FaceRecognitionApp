package sample;

/**
 * Created by Wojtek on 15.08.2017.
 * klasa person przechowuje dane o imieniu oraz procencie podopbienstwa z osobą z nazwą wpisaną w te
 */
public class Person {

    String personName;
    int personPercent;

    public Person(String personName, int personPercent) {
        this.personName = personName;
        this.personPercent = personPercent;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public int getPersonPercent() {
        return personPercent;
    }

    public void setPersonPercent(int personPercent) {
        this.personPercent = personPercent;
    }
}
