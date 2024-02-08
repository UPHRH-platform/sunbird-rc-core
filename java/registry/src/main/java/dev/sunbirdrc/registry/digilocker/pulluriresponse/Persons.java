package dev.sunbirdrc.registry.digilocker.pulluriresponse;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
@XmlRootElement
public class Persons {
    @XmlElement(name = "Person")
    public List<Person> getPerson() {
        return person;
    }


    public void setPerson(List<Person> person) {
        this.person = person;
    }

    List<Person> person = new ArrayList<>();

}
