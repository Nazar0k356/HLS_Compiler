package LexicalAnalyzer;
import java.util.LinkedList;
import java.io.FileReader;
import java.io.FileWriter;
//The main class of this lexic analyzer. Creates a linked list with all tokens of the code,
//which then can be used by syntax analyzer
public class LexicAnalyzer {
    //Linked list of all tokens
    private LinkedList<Token> tokenBuffer = new LinkedList<Token>();
    public LexicAnalyzer(String codeFileName, String tokensFileName, String errorsFileName) {
        TokenType Tag = new TokenType(tokensFileName);
        try{
            FileReader reader = new FileReader(codeFileName);
            FileWriter errorOutput = new FileWriter(errorsFileName);
            String word;
            char nextSymbol = (char)reader.read();
            symbolNum++;
            while(nextSymbol != (char)-1){ //The loop works till the end of file
                word = "";
                //Skipping all whiteSpace, nextLine symbols and carriageReturn symbols
                while(nextSymbol == ' ' || nextSymbol == '\n' || nextSymbol == '\r'){
                    if(nextSymbol == '\n'){
                        nextSymbol = (char)reader.read();
                        symbolNum = 1;
                        lineNum++;
                    } else {
                        nextSymbol = (char)reader.read();
                        symbolNum++;
                    }
                }
                //If symbols were skipped till the end of the file, analysis must be done one more time
                if(nextSymbol == (char)-1){
                    continue;
                }
                //Starting the analysis of the word. If the first symbol is a number - it is a constant,
                //if a letter - a word, else - an operator
                if(nextSymbol >= '0' && nextSymbol <= '9'){
                    //This is a constant
                    while(nextSymbol >= '0' && nextSymbol <= '9'){
                        word += nextSymbol;
                        nextSymbol = (char)reader.read();
                        symbolNum++;
                    }
                    //It may be a number with a decimal point
                    if(nextSymbol == '.'){
                        word += nextSymbol;
                        nextSymbol = (char)reader.read();
                        symbolNum++;
                        while(nextSymbol >= '0' && nextSymbol <= '9'){
                            word += nextSymbol;
                            nextSymbol = (char)reader.read();
                            symbolNum++;
                        }
                    }
                    //Adding new constant to an input token list
                    tokenBuffer.addFirst(new Const(Tag.currentMaxTag+1, word, lineNum));
                } else if(nextSymbol >= 'a' && nextSymbol <= 'z' || nextSymbol >= 'A' && nextSymbol <= 'Z'){
                    //This is a word
                    while(nextSymbol >= '0' && nextSymbol <= '9' ||
                            nextSymbol >= 'a' && nextSymbol <= 'z' || nextSymbol >= 'A' && nextSymbol <= 'Z'){
                        word += nextSymbol;
                        nextSymbol = (char)reader.read();
                        symbolNum++;
                    }
                    //Checking, if the lexeme is a key word
                    if(Tag.tokenTags.containsKey(word)){
                        //The lexeme is a keyword. Adding to buffer
                        tokenBuffer.addFirst(new Token(Tag.tokenTags.get(word), lineNum));
                    } else {
                        //The lexeme is an identifier. Adding to buffer
                        tokenBuffer.addFirst(new Lexeme(Tag.currentMaxTag + 2, word, lineNum));
                    }
                    //If it starts neither with a number nor a letter, it is an operator
                } else if(nextSymbol == '(' || nextSymbol == ')' || nextSymbol == '{' || nextSymbol == '}'
                        || nextSymbol == '[' || nextSymbol == ']' || nextSymbol == ';' || nextSymbol == ','
                        || nextSymbol == '&' || nextSymbol == '|' || nextSymbol == '*' || nextSymbol == ':'){
                    //This is a one-symbol token
                    word += nextSymbol;
                    nextSymbol = (char)reader.read();
                    symbolNum++;
                    //Adding the symbol to the buffer
                    tokenBuffer.addFirst(new Token(Tag.tokenTags.get(word), lineNum));
                } else if(nextSymbol == '=' || nextSymbol == '+' || nextSymbol == '-' || nextSymbol == '>'
                        || nextSymbol == '<' || nextSymbol == '!' || nextSymbol == '/'){
                    //This may be a one-symbol or a two-symbol token. In any case, it is a part of the word
                    word += nextSymbol;
                    //Saving this symbol. We need to analyze two symbols at a time
                    char prevSymbol = nextSymbol;
                    nextSymbol = (char)reader.read();
                    symbolNum++;
                    switch(prevSymbol){
                        case '=', '>', '<', '!':
                            if(nextSymbol == '='){
                                word += nextSymbol;
                                nextSymbol = (char)reader.read();
                                symbolNum++;
                            }
                            break;
                        case '+':
                            if(nextSymbol == '+'){
                                word += nextSymbol;
                                nextSymbol = (char)reader.read();
                                symbolNum++;
                            }
                            break;
                        case '-':
                            if(nextSymbol == '-'){
                                word += nextSymbol;
                                nextSymbol = (char)reader.read();
                                symbolNum++;
                            }
                            break;
                        case '/':
                            if(nextSymbol == '/'){
                                while(nextSymbol != '\n'){
                                    nextSymbol = (char)reader.read();
                                }
                                nextSymbol = (char)reader.read();
                                symbolNum = 1;
                                lineNum++;
                                continue;
                            }
                            break;
                    }
                    //Adding the token to the buffer
                    tokenBuffer.addFirst(new Token(Tag.tokenTags.get(word), lineNum));
                } else {
                    //The token does not exist, printing an error
                    errorOutput.write("Lexic error in line "+lineNum+", symbol "
                            +symbolNum+": invalid token\n");
                    nextSymbol = (char)reader.read();
                    symbolNum++;
                }
            }
            errorOutput.close();
            reader.close();
            //Putting end symbol
            tokenBuffer.addFirst(new Token(-1, lineNum));
        } catch(Exception e) {
            System.out.println("Class LexicAnalyzer:");
            System.out.println(e);
        }
    }

    //nextToken() method, which returns next token from the buffer to syntax analyzer
    public Token nextToken(){
        return tokenBuffer.removeLast();
    }

    //Method which returns the size of the buffer
    public int getBufferSize(){
        return tokenBuffer.size();
    }

    //Number of line and character. Used to locate lexical mistakes
    private int lineNum = 1;
    private int symbolNum = 0;


}

