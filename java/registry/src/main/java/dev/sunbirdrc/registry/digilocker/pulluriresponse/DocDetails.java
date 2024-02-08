package dev.sunbirdrc.registry.digilocker.pulluriresponse;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement
@XmlType(propOrder = {"issuedTo", "uri", "docContent", "dataContent"})
public class DocDetails {
	private IssuedTo issuedTo;
	private String uri;
	private Object docContent;
	private Object dataContent;
	@XmlElement(name = "IssuedTo")
	public IssuedTo getIssuedTo() {
		return issuedTo;
	}

	public void setIssuedTo(IssuedTo issuedTo) {
		this.issuedTo = issuedTo;
	}

	public String getUri() {
		return uri;
	}
	@XmlElement(name = "URI")
	public void setUri(String uri) {
		this.uri = uri;
	}
	@XmlElement(name = "DocContent")
	public Object getDocContent() {
		return docContent;
	}

	public void setDocContent(Object docContent) {
		this.docContent = docContent;
	}
	@XmlElement(name = "DataContent")
	public Object getDataContent() {
		return dataContent;
	}

	public void setDataContent(Object dataContent) {
		this.dataContent = dataContent;
	}

}

