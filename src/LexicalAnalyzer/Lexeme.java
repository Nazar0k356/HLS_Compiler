package LexicalAnalyzer;

//Class wich defines lexemes
public class Lexeme extends Token{
    Lexeme(int tag, String lexeme, int line){
        super(tag, line);
        this.lexeme = lexeme;
    }
    //String which defines a lexeme
    private String lexeme;

    //Getters
    public String getValue(){
        return lexeme;
    }
}
