/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xqt.engine.builtin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import xqt.engine.QueryEngine;
import xqt.model.ProcessModel;
import xqt.model.data.Variable;
import xqt.model.execution.ExecutionInfo;
import xqt.model.statements.StatementDescriptor;
import xqt.model.statements.StatementVisitor;

/**
 *
 * @author Javad Chamanara
 */
public class DefaultQueryEngine  implements QueryEngine{

    /** "memory" for variable/value pairs go here */
    private final Map<String, Variable> memory;    
    private ProcessModel model = null;
    
    public DefaultQueryEngine(ProcessModel processModel){
        this.memory = new HashMap<>();
        model = processModel;
        // register connections, bindings and perspectives
        // create adapter objects, better to do it in the visit function of the executer class
        // set versioning schemes
        // try to see whether adapters are able to connect to data sources
        // pre-fetch adapters' capabilities
    }

    @Override
    public Object getVariableValue(String variableName){
        if(this.memory.containsKey(variableName)){
            return (memory.get(variableName));
        }
        return (null);
    }

    @Override
    public Set<String> getVariableNames(){
        return this.memory.keySet();
    }
    
    @Override
    public List<Variable> getVariables(){
        return this.memory.values().stream().collect(Collectors.toList());
    }

    @Override
    public void deleteVariable(String variableName){
        this.memory.remove(variableName);
    }
    
    @Override
    public Boolean isCapableOf(String capabilityName){
        return (true);
    }

    @Override
    public Boolean isValid(String capabilityName){
        return(true);
    }

    @Override
    public ProcessModel getProcessModel(){
        return model;
    }
    
   /*
    * Executes each statement in the model. Every statement should hold its result and
    * set a flag showing it is executed.
    * Assignments statements are put into the memory of the query engine
    * There will be a CLEAR and CLEAR ID statements in the grammar which clear the whole memory or a single entry
    * In a more general sense, sme of the statements are executed against the query engine itself not the data source. adapter
    * for example, a statement may try to use a variable already in the memory to run a function on it!
    * All these information should be passed to the execute method of the statement in question!
    * Model objects can serve as the CONTRACT between different layers i.e., Grammar, Engine and Adapter.
    */

    @Override
    public ExecutionInfo execute(Integer statementDescriptorId){
        // check all the dependencies first and execute them if required, 
        // also invalidate the followers
        try{
            StatementDescriptor sd;
            sd = model.getStatement(statementDescriptorId);
            StatementVisitor visitor = new StatementExecuter(this); // 
            ExecutionInfo exInfo = sd.accept(visitor);
            if(exInfo.isExecuted() && exInfo.getVariable() != null)
                this.memory.put(exInfo.getVariable().getName(), exInfo.getVariable());
            return exInfo; //sd.getResult();
        }
        catch(Exception ex){
            // record, report error
            // update the statement result
            return null; // replace with a proper exception
        }        
    }

    @Override
    public void execute() {
        try {
            // the statements should be executed in the process order,
            // but the map does not guarantee the order!
            // TAKE CARE and change the code // <<<<<<<<<<<<<<<<<<<<<<<!!!!!!!!!!!!!!!!!
            // take a look at linked map, SortedMap
            // as the statementIds are ascending, unique and key of the map, maybe sortedmap solves the issue
            // but LinkedHashMap guarantees the insertion order without relying on the meaningfulness of the Ids
            StatementVisitor visitor = new StatementExecuter(this); // 
            for(StatementDescriptor sm: model.getStatements().values()){
                // pass the required information without binding too much to the structure of the statement
                // or needing too much knowledge about the statement!
                ExecutionInfo exInfo = sm.accept(visitor);
                if(exInfo.isExecuted() && exInfo.getVariable() != null)
                    this.memory.put(exInfo.getVariable().getName(), exInfo.getVariable());
            }
            } catch (Exception e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
            }
    }    
}