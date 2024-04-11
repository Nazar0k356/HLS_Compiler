package SyntaxDirectedTranslator;

import java.util.ArrayList;
import java.util.LinkedList;

//Class which define symbols in stack during syntax-directed translation
class Symbol {
    Symbol(int tag){
        this.tag = tag;
    }
    Symbol(int tag, int lineMin, int lineMax){
        this.tag = tag;
        this.lineMin = lineMin;
        this.lineMax = lineMax;
    }
    //Attributes of the symbol
    //Tag of the symbol
    int tag;
    //Lines, in which this symbol is met
    int lineMin;
    int lineMax;

    //Name for symbol in RAM
    String cellName;

    //Code attribute. Every symbol must have it
    ArrayList<String[]> code = new ArrayList<>();

    //Value attribute
    String value;

    //Type of the symbol
    Type type;

    //Identifies the symbol as an identifier of a function
    boolean isFunction = false;

    //List of arguments of the function (if symbol defines a list of parameters)
    LinkedList<Symbol> arguments = new LinkedList<>();

    //Beginning of the code of the function
    String codeBeginning;
    public class Type{
        Type(String name, int byteSize, Type childType){
            this.name = name;
            this.byteSize = byteSize;
            this.childType = childType;
        }
        public String name;
        int byteSize;
        Type childType;

        Type createArray(int size){
            int resultSize;
            if(childType == null){
                resultSize = byteSize * size + 4;
            } else {
                resultSize = 8 * size + 4;
            }
            String resultName = name + "[]";
            return new Type( resultName, resultSize, this);
        }
    }
}
