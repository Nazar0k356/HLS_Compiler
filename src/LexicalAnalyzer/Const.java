package LexicalAnalyzer;

//Class which defines constants
public class Const extends Token{
    Const(int tag, String value, int line){
        super(tag, line);
        this.value = value;
    }
    //Value of a constant
    private String value;

    //Getters
    public String getValue(){
        return value;
    }
}
