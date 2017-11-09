package com.compare.xsd.models.xsd.impl;

import com.compare.xsd.models.xsd.XsdNode;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSComplexTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSModelGroupImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSParticleDecl;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSParticle;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import javafx.scene.image.Image;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.java.Log;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Log
@EqualsAndHashCode
@Getter
public class XsdElement implements XsdNode {
    private static final String SCHEMA_DEFINITION = "http://www.w3.org/2001/XMLSchema";

    private final XSElementDecl element;
    private final List<XsdElement> childElements = new ArrayList<>();
    private final List<XsdAttribute> attributes = new ArrayList<>();

    private String name;
    private String type;
    private Integer minOccurrence;
    private Integer maxOccurrence;

    //region Constructors

    /**
     * Initialize a new {@link XsdElement}.
     *
     * @param element Set the element to process.
     */
    public XsdElement(XSElementDecl element) {
        Assert.notNull(element, "element cannot be null");
        this.element = element;

        init();
    }

    //endregion

    //region Getters & Setters

    @Override
    public String getCardinality() {
        return minOccurrence + ".." + (maxOccurrence != null ? maxOccurrence : "*");
    }

    @Override
    public Image getIcon() {
        return new Image(getClass().getResourceAsStream("/icons/element.png"));
    }

    //endregion

    //region Functions

    private void init() {
        XSTypeDefinition typeDefinition = element.getTypeDefinition();

        this.name = element.getName();
        log.fine("Processing element " + this.name);

        if (typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
            loadComplexType((XSComplexTypeDecl) typeDefinition);
        } else if (typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            loadSimpleType((XSSimpleTypeDecl) typeDefinition);
        } else {
            log.warning("Unknown element type " + typeDefinition.getTypeCategory());
        }
    }

    private void loadSimpleType(XSSimpleTypeDecl simpleType) {
        loadType(simpleType.getBaseType());
    }

    private void loadComplexType(XSComplexTypeDecl complexType) {
        XSParticleDecl particle = (XSParticleDecl) complexType.getParticle();

        if (particle != null) {
            XSModelGroupImpl group = (XSModelGroupImpl) particle.getTerm();
            XSObjectList children = group.getParticles();

            this.minOccurrence = particle.getMinOccurs();
            this.maxOccurrence = particle.getMaxOccursUnbounded() ? null : particle.getMaxOccurs();

            for (Object childItem : children) {
                if (childItem instanceof XSParticle) {
                    XSParticleDecl child = (XSParticleDecl) childItem;

                    this.childElements.add(new XsdElement((XSElementDecl) child.getTerm()));
                }
            }
        } else {
            loadType(complexType.getBaseType());
        }
    }

    private void loadType(XSTypeDefinition typeDefinition) {
        while (typeDefinition.getBaseType() != null && !typeDefinition.getNamespace().equals(SCHEMA_DEFINITION)) {
            typeDefinition = typeDefinition.getBaseType();
        }

        this.type = typeDefinition.getName();
    }

    //endregion
}
