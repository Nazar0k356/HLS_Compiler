package LexicalAnalyzer;

//Class which defines tokens
public class Token {
    Token(int tag, int line){
        this.tag = tag;
        this.line = line;
    }
    //Tag of a token
    private int tag;

    //Line, in which this symbol is met
    private int line;

    //Getters
    public int getTag(){
        return tag;
    }

    public String getValue(){
        return null;
    }

    public int getLine(){
        return line;
    }
}
