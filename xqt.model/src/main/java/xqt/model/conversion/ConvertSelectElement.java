/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package xqt.model.conversion;

import com.vaiona.commons.data.AttributeInfo;
import com.vaiona.commons.types.TypeSystem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import xqt.model.adapters.DataAdapter;
import xqt.model.containers.DataContainer;
import xqt.model.declarations.PerspectiveAttributeDescriptor;
import xqt.model.declarations.PerspectiveDescriptor;
import xqt.model.exceptions.LanguageExceptionBuilder;
import xqt.model.statements.query.SelectionFeature;
import xqt.model.statements.query.OrderFeature;
import xqt.model.statements.query.TargetClause;

/**
 *
 * @author standard
 */
public class ConvertSelectElement {
    
    // take care when calling from adapters other than the default and CSV, because of the aggregate call redirection!!! in the convertor.visit method
    public Map<String, AttributeInfo> prepareAttributes(PerspectiveDescriptor perspective, DataAdapter adapter, boolean useOriginalNames) {
        //ExpressionLocalizer converter = new ExpressionLocalizer(adapter);
        Map<String, AttributeInfo> attributes = new LinkedHashMap<>();
        for(PerspectiveAttributeDescriptor attribute: perspective.getAttributes().values()){
            if(!attributes.containsKey(attribute.getId())){
                String newId = attribute.getId();
                if(useOriginalNames & attribute.getReference()!= null)
                    newId = attribute.getReference().getId();
                AttributeInfo ad = convert(perspective, attribute, newId, adapter);
                ad.index = attributes.size();
                attributes.put(attribute.getId(), ad);
            }
        }        
        return attributes;
    }

    public String prepareWhere(SelectionFeature filter, DataAdapter adapter) {
        ExpressionLocalizer convertor = new ExpressionLocalizer(adapter);
        if(filter == null || filter.getPredicate() == null)
            return "";
        convertor.reset();
        convertor.visit(filter.getPredicate()); // visit returns empty predicate string on null expressions
        String filterString = convertor.getSource();
        return filterString;
    }

    public Map<String, String> prepareOrdering(OrderFeature order) {
        Map<String, String> ordering = new LinkedHashMap<>();
        try {
            order.getOrderItems().entrySet().stream()
                    .map((entry) -> entry.getValue())
                    .forEach((orderItem) -> {
                            ordering.put(orderItem.getSortKey().getId(), orderItem.getSortOrder().toString());
            });
        }
        catch (Exception ex){            
        }
        
        return ordering;
    }

    public boolean shouldResultBeWrittenIntoFile(TargetClause target) {
        return(
                (
                    target.getContainer().getDataContainerType() == DataContainer.DataContainerType.Single
                ||  target.getContainer().getDataContainerType() == DataContainer.DataContainerType.Joined
                )
        );
    }
        
    public String translateExpression(String expression, PerspectiveDescriptor perspective, String prefix) {
        String expressionTranslated = "";
        for (StringTokenizer stringTokenizer = new StringTokenizer(expression, " ");
                stringTokenizer.hasMoreTokens();) {
            String token = stringTokenizer.nextToken();
            // translate the wehre clause
            if(perspective.getAttributes().containsKey(token)){
                expressionTranslated = expressionTranslated + " " + prefix + "." + token;
            }
            else {
                expressionTranslated = expressionTranslated + " " + token;
            }                      
        }
        return expressionTranslated;
    }
    
    public AttributeInfo convert(PerspectiveDescriptor perspective, PerspectiveAttributeDescriptor attribute, String newId, DataAdapter adapter){
        ExpressionLocalizer convertor = new ExpressionLocalizer(adapter);
        convertor.reset();
        convertor.visit(attribute.getForwardExpression());
        String exp = convertor.getSource(); 
        List<String> members = convertor.getMemeberNames();
        String typeNameInAdapter = attribute.getDataType();
        String runtimeType = TypeSystem.getTypes().get(TypeSystem.TypeName.String).getRuntimeType();
        if(TypeSystem.getTypes().containsKey(attribute.getDataType())){           
            typeNameInAdapter = TypeSystem.getTypes().get(attribute.getDataType()).getName();
            runtimeType = TypeSystem.getTypes().get(attribute.getDataType()).getRuntimeType();
        } else {
            perspective.getLanguageExceptions().add(
                LanguageExceptionBuilder.builder()
                    .setMessageTemplate("Can not infer the data type of attribute '%s' in perspective '%s'! It has data type 'Unknown'.")
                    .setContextInfo1(attribute.getId())
                    .setContextInfo2(perspective.getId())
                    .setLineNumber(attribute.getParserContext().getStart().getLine())
                    .setColumnNumber(-1)
                    .build()
            );   
        }
        AttributeInfo ad = new AttributeInfo();
        ad.name = newId;
        ad.conceptualDataType = attribute.getDataType();
        ad.internalDataType = typeNameInAdapter;
        ad.unit = attribute.getSemanticKey();
        ad.forwardMap = exp;
        ad.fields = members;
        ad.runtimeType = runtimeType;//TypeSystem.getTypes().get(attribute.getDataType()).getRuntimeType();
        ad.reference = attribute; // keeping the reference for possible further processing.
        ad.joinSide = attribute.getExtra();
        return (ad);                   
    }

    // should move here from DataReaderBuilderBase
//    public String enhanceExpression(String expression, boolean isJoinMode, String nonJoinPrefix, String joinPrefix) throws Exception {
//        String translated = "";
//        for (StringTokenizer stringTokenizer = new StringTokenizer(expression, " ");
//                stringTokenizer.hasMoreTokens();) {
//            String token = stringTokenizer.nextToken();
//            if(hasAggregate()){
//                // non aggregate attributes apear in both row and result entities, so if an attribute apears in the result but not in the row 
//                // entity, it is an aggregate attribute.
//                // translate the expression
//                if(rowEntityAttributes.containsKey(token)){
//                    if(!isJoinMode)
//                        translated = translated + " " + nonJoinPrefix + "." + token;
//                    else
//                        translated = translated + " " + joinPrefix + "." + token;
//                }
//                else {
//                    translated = translated + " " + token;
//                }                                      
//            } else {
//                // translate the wehre clause
//                if(resultEntityAttributes.containsKey(token)){
//                    if(!isJoinMode)
//                        translated = translated + " " + nonJoinPrefix + "." + token;
//                    else
//                        translated = translated + " " + joinPrefix + "." + token;
//                }
//                else {
//                    translated = translated + " " + token;
//                }                      
//            }
//        }
//        return translated;
//    }
//    
    
}
