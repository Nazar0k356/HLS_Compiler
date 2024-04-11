package SyntaxDirectedTranslator;

import java.util.HashMap;
import java.util.LinkedList;

//This is the class which defines a table of symbols of the analyzer
public class SymbolTable {
    SymbolTable(SymbolTable fatherTable, int idTag){
        this.fatherTable = fatherTable;
        //Putting al symbols in the new table
        if(fatherTable != null){
            this.symbols.putAll(fatherTable.symbols);
            this.sizeOfMemory.putAll(fatherTable.sizeOfMemory);
            this.addressOfMemory.putAll(fatherTable.addressOfMemory);
            this.defined.putAll(fatherTable.defined);
            this.varNum = fatherTable.varNum;
            this.generalAddress.putAll(fatherTable.generalAddress);
            this.generalTypes.putAll(fatherTable.generalTypes);
            this.generalSizes.putAll(fatherTable.generalSizes);
            this.maxAddress = fatherTable.maxAddress;
            this.lastExpressions.putAll(fatherTable.lastExpressions);
            this.returnType = fatherTable.returnType;
            this.procedureSize = fatherTable.procedureSize;

        } else {
            procedureSize = 0;
        }
        for(String identifier : symbols.keySet()){
            needToTransfer.put(identifier, true);
        }
        this.idTag = idTag;
        //Putting all memory data in the new table
    }

    //This is a father table of this table of symbols
    SymbolTable fatherTable;

    //This is a hashmap, which returns a symbol by its name
    HashMap<String, Symbol> symbols = new HashMap<>();

    //Information about size and address of memory cell
    HashMap<String, Integer> sizeOfMemory = new HashMap<>();
    HashMap<String, Integer> addressOfMemory = new HashMap<>();

    //Current size of all variables in the table
    int maxAddress = 0;

    //All the sizes of memory cells
    HashMap<String, Integer> generalSizes = new HashMap<>();

    //All the addresses of memory cells
    public HashMap<String, Integer> generalAddress = new HashMap<>();

    //All the types of memory cells
    public HashMap<String, Symbol.Type> generalTypes = new HashMap<>();

    //The last value of every memory cell
    HashMap<String[], String> lastExpressions = new HashMap<>();

    //Symbols, needed to transfer to father table after this one is closed
    HashMap<String, Boolean> needToTransfer = new HashMap<>();

    //List of defined and undefined symbols
    HashMap<String, Boolean> defined = new HashMap<>();

    //If this is a table of symbols of a function, it must return a type
    Symbol.Type returnType;

    int procedureSize;

    //Tag of identifier in this grammar
    int idTag;

    //Deletes all assignments, values in which are changed
    void clearLastExpressionsOf(String cellName){
        LinkedList<String[]> needToDelete = new LinkedList<>();
        for(String[] assignment : lastExpressions.keySet()){
            if(lastExpressions.get(assignment).equals(cellName)){
                needToDelete.add(assignment);
            }
            for(String word : assignment){
                if(word != null && word.equals(cellName)){
                    needToDelete.add(assignment);
                }
            }
        }
        for(String[] assignment : needToDelete){
            lastExpressions.remove(assignment);
        }
    }

    //Returns last occurrence of this assignment
    String getLastOccurrence(String[] assignment){
        for(String[] tempAssignment : lastExpressions.keySet()){
            if(tempAssignment.length != assignment.length){
                continue;
            }
            boolean isEqual = true;
            for(int i = 0; i < assignment.length; i++){
                if(assignment[i] != null && tempAssignment[i] == null || assignment[i] == null && tempAssignment[i] != null){
                    isEqual = false;
                    break;
                }
                if(assignment[i] == null && tempAssignment[i] == null){
                    continue;
                }
                if(!assignment[i].equals(tempAssignment[i])){
                    isEqual = false;
                    break;
                }
            }
            if(isEqual){
                return lastExpressions.get(tempAssignment);
            }
        }
        return  null;
    }

    //Methods which transfers all changes to father table
    void close(){
        for(String identifier : symbols.keySet()){
            if(needToTransfer.get(identifier)){
                fatherTable.symbols.put(identifier, symbols.get(identifier));
            }
        }
        fatherTable.varNum = varNum;
        fatherTable.generalAddress = generalAddress;
        fatherTable.generalTypes = generalTypes;
        fatherTable.generalSizes = generalSizes;
        fatherTable.procedureSize = procedureSize;
    }

    void addVarID(String id, Symbol.Type type){
        Symbol newId = new Symbol(idTag);
        newId.type = type;
        symbols.put(id, newId);
        symbols.get(id).cellName = giveMemory(type);
        needToTransfer.put(id, false);
    }

    void addFunID(String id, Symbol.Type type){
        Symbol newId = new Symbol(idTag);
        newId.type = type;
        newId.isFunction = true;
        symbols.put(id, newId);
        needToTransfer.put(id, false);
    }

    //Allocating memory for a variable
    String giveMemory(Symbol.Type type){
        String newName = newCellName();
        sizeOfMemory.put(newName, type.byteSize);
        maxAddress += type.byteSize;
        procedureSize += type.byteSize;
        addressOfMemory.put(newName, maxAddress);
        generalAddress.put(newName, maxAddress);
        generalTypes.put(newName, type);
        generalSizes.put(newName, type.byteSize);
        return newName;
    }
    int varNum = 0;

    //Getting new temporary variable
    String newCellName(){
        return "V"+varNum++;
    }
}
