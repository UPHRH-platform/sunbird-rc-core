package dev.sunbirdrc.registry.digilocker.pulldoc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlRootElement(name = "PullDocResponse")
@XmlType(propOrder = {"responseStatus", "docDetails"})
public class PullDocResponse implements Serializable {

    private ResponseStatus responseStatus;
    private DocDetailsRs docDetails;

    @XmlElement(name = "ResponseStatus")
    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    @XmlElement(name = "DocDetails")
    public DocDetailsRs getDocDetails() {
        return docDetails;
    }

    public void setDocDetails(DocDetailsRs docDetails) {
        this.docDetails = docDetails;
    }

}








