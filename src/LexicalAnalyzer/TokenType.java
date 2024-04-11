package LexicalAnalyzer;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
//Class from which you can get tags of all of the defined tokens
class TokenType {
    //Hashmap which gets lexeme of the token, and returns tag of the token.
    // Tags of regexes (constants and identifiers) are processed in the "Analyzer" class
    HashMap<String, Integer> tokenTags = new HashMap<String, Integer>();
    TokenType(String tokensFile){
        try{
            BufferedReader tokens = new BufferedReader(new FileReader(tokensFile));
            String token = tokens.readLine();
            while(token != null){
                currentMaxTag++;
                tokenTags.put(token, currentMaxTag);
                token = tokens.readLine();
            }
            tokens.close();
        } catch(Exception e){
            System.out.println("Class TokenType:");
            System.out.println(e);
        }
    }
    //Just in case, minimum available value of a tag is 256. UTF-8 uses values
    int currentMaxTag = 0;
}
