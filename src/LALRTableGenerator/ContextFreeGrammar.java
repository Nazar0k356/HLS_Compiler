package LALRTableGenerator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.FileReader;

//This class reads a context-free grammar from a file, and converts it in a list of lists of integers
public class ContextFreeGrammar {

    //Linked list, which defines the symbols's grammar production rules
    public ArrayList<LinkedList<Integer>> productions = new ArrayList<LinkedList<Integer>>();
    public ArrayList<Integer> priorities = new ArrayList<>();
    ContextFreeGrammar(String grammarFileName, String terminalsFileName, String nonTerminalsFileName){
        try{
            SymbolType grammarTag = new SymbolType(terminalsFileName, nonTerminalsFileName);
            FileReader reader = new FileReader(grammarFileName);
            String symbol = "";
            char nextCharacter = ' ';
            //The analysis continues till the end of the file
            while(nextCharacter != (char)-1){
                int priority = 0;
                symbol = "";
                //All whitespace symbols are skipped
                while(nextCharacter == ' '){
                    nextCharacter = (char) reader.read();
                }
                while(nextCharacter == '$'){
                    nextCharacter = (char) reader.read();
                    priority++;
                }
                while(nextCharacter == ' '){
                    nextCharacter = (char) reader.read();
                }
                //Reading the head of the production
                while(nextCharacter != ' '){
                    symbol += nextCharacter;
                    nextCharacter = (char) reader.read();
                }
                //Adding new list with head - the first symbol in line
                LinkedList<Integer> newProduction = new LinkedList<Integer>();
                if(grammarTag.nonTerminalTags.containsKey(symbol)){
                    newProduction.addLast(grammarTag.nonTerminalTags.get(symbol));
                } else {
                    System.out.println(symbol);
                    throw new Exception ("Grammar mistake: terminal can not be a head of production!");
                }
                //Checking if an arrow is present
                //Skipping all whitespace symbols
                while(nextCharacter == ' '){
                    nextCharacter = (char) reader.read();
                }
                //Checking, if an arrow is present
                if(nextCharacter == '-'){
                    nextCharacter = (char) reader.read();
                } else {
                    throw new Exception ("Arrow is not present!");
                }
                if(nextCharacter == '>'){
                    nextCharacter = (char) reader.read();
                } else {
                    throw new Exception ("Arrow is not present!");
                }
                //Adding symbols to the production till the line is over
                while(nextCharacter != '\r' && nextCharacter != (char)-1){
                    symbol = "";
                    //All whitespace symbols are skipped
                    while(nextCharacter == ' '){
                        nextCharacter = (char) reader.read();
                    }
                    while(nextCharacter != ' ' && nextCharacter != '\r' && nextCharacter != '\n'
                            && nextCharacter != (char)-1){
                        symbol += nextCharacter;
                        nextCharacter = (char) reader.read();
                    }
                    //Adding symbol to the production
                    if(grammarTag.terminalTags.containsKey(symbol)){
                        //The symbol is a terminal
                        newProduction.addLast(grammarTag.terminalTags.get(symbol));
                    } else if(grammarTag.nonTerminalTags.containsKey(symbol)){
                        //The symbol is a non-terminal
                        newProduction.addLast(grammarTag.nonTerminalTags.get(symbol));
                    } else if(symbol.equals("ε")){
                        //This is an empty line
                        newProduction.addLast(0);
                    } else {
                        System.out.println(productions.size() + "  >" +symbol +"<;");
                        System.out.println(grammarTag.terminalTags.containsKey(symbol));
                        System.out.println(grammarTag.terminalTags.containsKey("-"));
                        throw new Exception("Invalid grammar symbol!");
                    }
                }
                //The line is over, adding production to the grammar and getting to the next line
                productions.addLast(newProduction);
                priorities.addLast(priority);
                while(nextCharacter == ' ' || nextCharacter == '\r' || nextCharacter == '\n'){
                    nextCharacter = (char) reader.read();
                }
            }
        }catch(Exception e){
            System.out.println("Class ContextFreeGrammar");
            System.out.println(e);
        }
    }
}
