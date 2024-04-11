package SyntaxDirectedTranslator;

import LALRTableGenerator.LALRGroupOfItems;
import LexicalAnalyzer.Token;
import LexicalAnalyzer.LexicAnalyzer;
import LALRTableGenerator.LALRGenerator;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;

public class Translator {
    public Translator(LexicAnalyzer lexer, LALRGenerator syntaxAnalyzer, String resultFileName, String syntaxErrorsFileName){
        this.lexer = lexer;
        this.syntaxAnalyzer = syntaxAnalyzer;
        this.resultFileName = resultFileName;
        this.syntaxErrorsFileName = syntaxErrorsFileName;
    }
    //All of the files needed for lexical analyzer, syntax analyzer and translator
    private String resultFileName, syntaxErrorsFileName;

    //Lexical analyzer, used during translation
    LexicAnalyzer lexer;

    //Syntax analyzer, used during translation
    LALRGenerator syntaxAnalyzer;

    //Table of symbols, used during translation
    public SymbolTable table;

    //Translation rules, used during translation
    public TranslationRules translation;

    public ArrayList<String[]> translate(){
        boolean errorsPresent = false;
        try{
            FileWriter errorOutput = new FileWriter(syntaxErrorsFileName);
            FileWriter resultOutput = new FileWriter(resultFileName);

            syntaxAnalyzer.buildLALRStates();

            //A table of symbols
            table = new SymbolTable(null, syntaxAnalyzer.getId());
            //A stack with symbols
            LinkedList<Symbol> symbolStack = new LinkedList<>();
            //A stack with states
            LinkedList<Integer> stateStack = new LinkedList<>();
            stateStack.addLast(0);
            //Creating a list of translation rules, which are used while parsing
            translation = new TranslationRules(syntaxAnalyzer, table);
            boolean needToFinish = false;
            boolean needToReadNextSymbol = true;
            Token nextToken;
            Symbol nextSymbol = null;
            do{
                boolean actionDone = false;
                //Reading next symbol, if it is necessary
                if(needToReadNextSymbol){
                    nextToken = lexer.nextToken();
                    nextSymbol = new Symbol(nextToken.getTag(), nextToken.getLine(), nextToken.getLine());
                    if(nextToken.getValue() != null){
                        nextSymbol.value = nextToken.getValue();
                    }
                }
                needToReadNextSymbol = true;
                LALRGroupOfItems topState = syntaxAnalyzer.LALRStates.get(stateStack.getLast());
                int maxPriority = 0;
                for(int[] item : topState.items){
                    int priority = syntaxAnalyzer.grammar.priorities.get(item[0]);
                    if(priority > maxPriority){
                        maxPriority = priority;
                    }
                }
                //Going through items, depending ob their priority
                for(int priority = maxPriority; priority >= 0; priority--){
                    LinkedList<Integer> numsOfItems = new LinkedList<>();
                    //Adding all items of this priority to the list
                    for(int i = 0; i < topState.items.size(); i++){
                        int thisPriority = syntaxAnalyzer.grammar.priorities.get(topState.items.get(i)[0]);
                        if(thisPriority == priority){
                            numsOfItems.add(i);
                        }
                    }
                    for(int numOfItem : numsOfItems){
                        int[] item = topState.items.get(numOfItem);
                        LinkedList<Integer> production = syntaxAnalyzer.grammar.productions.get(item[0]);
                        int positionOfDot = item[1];
                        LinkedList<Integer> lookaheads = topState.lookaheads.get(numOfItem);
                        if((production.size() == positionOfDot || production.get(1) == 0)){
                            if(!lookaheads.contains(nextSymbol.tag)){
                                continue;
                            }
                            //Checking for the start production
                            if(item[0] == 0){
                                System.out.println("Згорка по фінішній продукції");
                                symbolStack.getLast().code.addFirst(new String[]{"-", "@RSP", "@"+table.procedureSize, "@RSP"});
                                needToFinish = true;
                                actionDone = true;
                                break;
                            }
                            System.out.println("Згорка по продукції "+item[0]);
                            //Folding symbols in the stack, using this production
                            try{
                                translation.applyRule(symbolStack, stateStack, item[0]);
                            } catch (Exception e){
                                errorOutput.write("Syntax error in lines "+nextSymbol.lineMin + " - "+nextSymbol.lineMax+":\r\n");
                                errorOutput.write(e.getMessage());
                                errorOutput.flush();
                                errorsPresent = true;
                                //panicMode();
                                return null;
                                //break;
                            }
                            //Accounting for translation errors
                            actionDone = true;
                            needToReadNextSymbol = false;
                            break;
                        } else if(production.get(positionOfDot) == nextSymbol.tag && !topState.GOTO[nextSymbol.tag-1].equals("e")){
                            //Putting symbol in the stack
                            symbolStack.addLast(nextSymbol);
                            stateStack.addLast(Integer.valueOf(topState.GOTO[nextSymbol.tag-1]));
                            actionDone = true;
                            break;
                        }
                    }
                    if(actionDone){
                        break;
                    }
                }
                if(!actionDone){
                    errorOutput.write("Syntax error in lines "+nextSymbol.lineMin + " - "+nextSymbol.lineMax+"\r\n");
                    errorOutput.flush();
                    errorsPresent = true;
                    //panicMode();
                    return null;
                }
            } while(!needToFinish);
            if(errorsPresent){
                return null;
            }
            //ВИДАЛИТИ!!! ТЕСТ!!!
            System.out.println("=======================================");
            System.out.println("Код: ");
            for(String[] line : symbolStack.getLast().code){
                if(line[0] != null){
                    System.out.print(line[0]+" ");
                }
                if(line[1] != null){
                    System.out.print(line[1]+" ");
                }
                if(line[2] != null){
                    System.out.print(line[2]+" ");
                }
                if(line[3] != null){
                    System.out.print(line[3]+" ");
                }
                System.out.println();
                resultOutput.write(line[0]+" "+line[1]+" "+line[2]+" "+line[3]+"\r\n");
                resultOutput.flush();
            }
            System.out.println("=======================================");
            System.out.println("Розмір: ");
            for(String varName : table.generalSizes.keySet()){
                System.out.println(varName+": "+table.generalSizes.get(varName)+" bytes");
            }
            System.out.println("=======================================");
            System.out.println("Адреси: ");
            for(String varName : table.generalAddress.keySet()){
                System.out.println(varName+": "+"BP-"+table.generalAddress.get(varName));
            }
            System.out.println("=======================================");
            System.out.println("Типи: ");
            for(String varName : table.generalTypes.keySet()){
                System.out.println(varName+": "+table.generalTypes.get(varName).name);
            }
            System.out.println("=======================================");
            System.out.println("Розмір стартової процедури: ");
            System.out.println(table.procedureSize+" bytes");
            System.out.println("=======================================");

            errorOutput.close();
            resultOutput.close();
            return symbolStack.getLast().code;
        } catch (Exception e){
            System.out.println("Class Translator:");
            System.out.println(e);
            return null;
        }
    }

    private void panicMode(){

    }
}
