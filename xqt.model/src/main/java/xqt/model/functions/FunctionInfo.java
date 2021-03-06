/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package xqt.model.functions;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author standard
 */

@XmlRootElement(name="Function")
public class FunctionInfo {
    
    private String name;
    private String appliesTo;
    private String returnType;
    private String packageName;
    private List<FunctionParameterInfo> parameters = new ArrayList<>();
    private List<FunctionImplementation> implementations;
    
    public FunctionInfo(){        
    }
    
    public FunctionInfo(String name, String appleisTo, String returnType, String packageName, List<FunctionParameterInfo> parameters, List<FunctionImplementation> implementations){
        this.name = name;
        this.appliesTo = appleisTo;
        this.returnType = returnType;
        this.packageName = packageName;
        this.parameters = parameters;
        this.implementations = implementations;
    }
    
    @XmlAttribute(name="name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute(name="appliesTo")
    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public Boolean isAggregate(){
        return (getAppliesTo().equalsIgnoreCase("column"));
    }
    
    @XmlAttribute(name="returnType")
    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    // does not need to be serialized, its poplulated by the file name.
    @XmlTransient
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    @XmlElementWrapper(name="Parameters")
    @XmlElement(name="Parameter")
    public List<FunctionParameterInfo> getParameters() {
        return parameters;
    }

    public void setParameters(List<FunctionParameterInfo> parameters) {
        this.parameters = parameters;
    }

    @XmlElementWrapper(name="Implementations")
    @XmlElement(name="Implementation")
    public List<FunctionImplementation> getImplementations() {
        return implementations;
    }

    public void setImplementations(List<FunctionImplementation> implementations) {
        this.implementations = implementations;
        //this.getImplementations().stream().filter(p->p.getDialect().equalsIgnoreCase("dialect")).findFirst().get().getNamespace();
    }
}
