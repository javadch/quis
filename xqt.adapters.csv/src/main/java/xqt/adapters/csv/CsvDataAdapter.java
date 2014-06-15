/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package xqt.adapters.csv;

import com.vaiona.csv.reader.DataReader;
import com.vaiona.csv.reader.DataReaderBuilder;
import com.vaiona.csv.reader.HeaderBuilder;
import com.vaiona.data.FieldInfo;
import com.vaiona.data.TypeSystem;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import xqt.model.adapters.DataAdapter;
import xqt.model.data.Resultset;
import xqt.model.data.ResultsetType;
import xqt.model.data.SchemaItem;
import xqt.model.declarations.PerspectiveAttributeDescriptor;
import xqt.model.statements.query.SelectDescriptor;

/**
 *
 * @author standard
 */
public class CsvDataAdapter implements DataAdapter {

    private ExpressionToJavaSource convertor = null;
    public CsvDataAdapter(){
        convertor = new ExpressionToJavaSource();
    }

    @Override
    public Resultset run(SelectDescriptor select) {
        DataReaderBuilder builder = new DataReaderBuilder();
        try{
            builder
                .baseClassName("Test")
                .dateFormat("yyyy-MM-dd'T'HH:mm:ssX") //check the timezone formatting
                //.addProjection("MAX", "SN")// MIN, SUM, COUNT, AVG, 
            ;
            try {
                String columnDelimiter = select.getSourceClause().getBinding().getConnection().getParameters().get("delimiter").getValue();
                switch (columnDelimiter){
                    case "comma": 
                        builder.columnDelimiter(",");
                        break;
                    case "tab": 
                        builder.columnDelimiter("\t");
                        break;
                    case "blank":
                        builder.columnDelimiter(" ");
                        break;
                    case "semicolon":
                        builder.columnDelimiter(";");
                        break;
                    default:
                        builder.columnDelimiter(columnDelimiter);
                        break;
                }                                        
            } catch(Exception ex){
                builder.columnDelimiter(",");
            }
            
            Boolean firstRowIsHeader = prepareFields(builder, select);            
            prepareAttributes(builder, select);
            prepareWhere(builder, select);            
            prepareOrdering(builder, select);
            prepareLimit(builder, select);
            
            DataReader<Object> reader = builder.build();
            if(reader != null){
                // when the reader is built, it can be used nutiple time having different CSV settings
                // as long as the query has not changed. means the reader can read/ query different files the share the same column info
                // but maybe different delimiter, etc.
                List<Object> result = reader
                        //.columnDelimiter(",") // set during build
                        //.quoteDelimiter("\"")
                        //.unitDelimiter("::")
                        .source(getCompleteSourceName(select))
                        .bypassFirstRow(firstRowIsHeader)
                        .read();
                
                //System.out.println("The result set contains " + result.stream().count() + " records.");
                Resultset resultSet = new Resultset(ResultsetType.Tabular);
                resultSet.setData(result);
                resultSet.setSchema(prepareSchema(select));
                return resultSet;
            }
        } catch (IOException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | InvocationTargetException | ParseException ex1) {
            Logger.getLogger(CsvDataAdapter.class.getName()).log(Level.SEVERE, null, ex1);
            // throw a proper exception
        }
        return null;        
    }

    @Override
    public void setup(Map<String, Object> config) {
    }

    private String getCompleteSourceName(SelectDescriptor select){ //may need a container index too!
        String basePath = select.getSourceClause().getBinding().getConnection().getSourceUri();
        String container0 = select.getSourceClause().getContainer();
        String fileExtention = "csv";
        String fileName = "";
        try{
            fileExtention = select.getSourceClause().getBinding().getConnection().getParameters().get("fileExtension").getValue();
        } catch (Exception ex){}
        fileName = basePath.concat(container0).concat(".").concat(fileExtention);
        return fileName;
    }
  
    private Boolean prepareFields(DataReaderBuilder builder, SelectDescriptor select) throws IOException {
        String fileName = getCompleteSourceName(select);
        HeaderBuilder hb = new HeaderBuilder();
        LinkedHashMap<String, FieldInfo> fields = hb.buildFromDataFile(fileName, builder.getColumnDelimiter(), builder.getTypeDelimiter(), builder.getUnitDelimiter());
        builder.addFields(fields);
        Boolean firstRowIsHeader = true;
        try {
            firstRowIsHeader = Boolean.valueOf(select.getSourceClause().getBinding().getConnection().getParameters().get("firstRowIsHeader").getValue());
        } catch (Exception ex){}
        return firstRowIsHeader;
    }

    private void prepareAttributes(DataReaderBuilder builder, SelectDescriptor select) {
        for(PerspectiveAttributeDescriptor attribute: select.getProjectionClause().getPerspective().getAttributes().values()){
            convertor.reset();
            convertor.visit(attribute.getForwardExpression());
            String exp = convertor.getSource(); 
            List<String> members = convertor.getMemeberNames();
            String typeNameInAdapter = TypeSystem.getTypes().get(attribute.getDataType()).getName();
            builder.addAttribute(attribute.getId(), attribute.getDataType(), typeNameInAdapter, exp, members);                
        }        
    }

    private HashSet<SchemaItem> prepareSchema(SelectDescriptor select) {
        // pay attention to aggrgates!
        HashSet<SchemaItem> schema = new LinkedHashSet<>();
        // do not use the functional counterpart, as it uses the streaming method, which doe not guarantee to preserve the order
        for(PerspectiveAttributeDescriptor attribute: select.getProjectionClause().getPerspective().getAttributes().values()){
            SchemaItem sItem = new SchemaItem();
            sItem.setDataType(attribute.getDataType());
            sItem.setName(attribute.getId());
            sItem.setSystemType(TypeSystem.getTypes().get(attribute.getDataType()).getName());
            sItem.setIndex(schema.size());            
            schema.add(sItem); 
        }
        return schema;
    }

    private void prepareWhere(DataReaderBuilder builder, SelectDescriptor select) {
        if(select.getFilterClause().getPredicate() != null ){
            convertor.reset();
            convertor.visit(select.getFilterClause().getPredicate());
            String filterString = convertor.getSource();
            builder        
                .addWhere(filterString);            
        }
    }

    private void prepareOrdering(DataReaderBuilder builder, SelectDescriptor select) {
        select.getOrderClause().getOrderItems().entrySet().stream()
                .map((entry) -> entry.getValue())
                .forEach((orderItem) -> {
                        builder.addSort(orderItem.getSortKey(), orderItem.getSortOrder().toString());
        });
    }

    private void prepareLimit(DataReaderBuilder builder, SelectDescriptor select) {
        builder.skip(select.getLimitClause().getSkip())
               .take(select.getLimitClause().getTake());
    }

}