package LALRTableGenerator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

//Class from which you can get tags of all the defined terminals and non-terminals. Used to analyze a grammar
class SymbolType {
    //Hashmaps, which help us to identify, whether the word is a terminal ot a non-terminal
    HashMap<String, Integer> terminalTags = new HashMap<String, Integer>();
    HashMap<String, Integer> nonTerminalTags = new HashMap<String, Integer>();
    SymbolType(String terminalsFileName, String nonTerminalsFileName ) {
        try{
            //Reading and memorising all terminals
            BufferedReader terminals = new BufferedReader(new FileReader(terminalsFileName));
            String word = terminals.readLine();
            while(word != null){
                currentMaxTag++;
                terminalTags.put(word, currentMaxTag);
                word = terminals.readLine();
            }
            terminals.close();

            //Reading and memorising all nonTerminals
            BufferedReader nonTerminals = new BufferedReader(new FileReader(nonTerminalsFileName));
            word = nonTerminals.readLine();
            while(word != null){
                currentMaxTag++;
                nonTerminalTags.put(word, currentMaxTag);
                word = nonTerminals.readLine();
            }
            nonTerminals.close();
        } catch(Exception e) {
            System.out.println("Class SymbolType:");
            System.out.println(e);
        }
    }
    private int currentMaxTag = 0;

    public int getMaxTag(){
        return currentMaxTag;
    }
}
