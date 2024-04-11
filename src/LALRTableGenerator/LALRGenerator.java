package LALRTableGenerator;

import java.util.LinkedList;

//Class which generates a LALR table for syntax analyzer
public class LALRGenerator {
    public SymbolType grammarTags;
    public ContextFreeGrammar grammar;
    public LALRGenerator(String grammarFileName, String terminalsFileName, String nonTerminalsFileName){
        try{
            this.grammarTags = new SymbolType(terminalsFileName, nonTerminalsFileName);
            this.grammar = new ContextFreeGrammar(grammarFileName, terminalsFileName, nonTerminalsFileName);
        } catch(Exception e) {
            System.out.println("Class LALRGenerator:");
            System.out.println(e);
        }
    }

    //Returns the tag of the start non-terminal
    public int getStartSymbol(){
        return grammarTags.terminalTags.size() + 1;
    }

    //Returns tags of id and const
    public int getId(){
        return grammarTags.terminalTags.get("id");
    }

    public int getConst(){
        return grammarTags.terminalTags.get("const");
    }

    //List of groups of items (states) of a LALR analyzer
    public LinkedList<LALRGroupOfItems> LALRStates = new LinkedList<>();

    //This method builds a list of LALR states, filling action, jump and lookaheads tables on the way
    public void buildLALRStates(){
        LinkedList<LALRGroupOfItems> addedStates = new LinkedList<>();
        //Setting start state
        LALRGroupOfItems startState = new LALRGroupOfItems();
        startState.items.add(new int[]{0, 1});

        LinkedList<Integer> startLookahead = new LinkedList<>();
        startLookahead.add(-1);
        startState.lookaheads.add(startLookahead);

        startState = LALRClosure(startState);
        LALRStates.add(startState);

        //Scanning all the states, while it is possible
        boolean statesAdded;
        do{
            statesAdded = false;
            //Reviewing all jump(states, symbols)
            for(LALRGroupOfItems state : LALRStates){
                state.GOTO = new String[grammarTags.getMaxTag()];
                for(int symbol = 1; symbol <= grammarTags.getMaxTag(); symbol++){

                    LALRGroupOfItems jumpState = new LALRGroupOfItems();
                    jumpState = LALRJump(state, symbol);
                    jumpState.LALRsort();

                    //There is no sense in reviewing empty states
                    if(jumpState.items.isEmpty()){
                        //Adding an error to a table
                        state.GOTO[symbol - 1] = "e";
                        continue;
                    }

                    int indexInAddedStates = indexOf(addedStates, jumpState);
                    int indexInLALRStates = indexOf(LALRStates, jumpState);
                    //If the state is new, it must be added
                    if(indexInAddedStates == -1 && indexInLALRStates == -1){
                        addedStates.add(jumpState);
                        statesAdded = true;
                        int index = indexOf(addedStates, jumpState);
                        state.GOTO[symbol - 1] = ""+index;
                    } else {
                        if(indexInLALRStates != -1){
                            if(LALRStates.get(indexInLALRStates).addAllLookaheads(jumpState)){
                                statesAdded = true;
                            }
                            int index = indexOf(LALRStates, jumpState);
                            state.GOTO[symbol - 1] = ""+index;
                        }

                        if(indexInAddedStates != -1){
                            if(addedStates.get(indexInAddedStates).addAllLookaheads(jumpState)){
                                statesAdded = true;
                            }
                            int index = indexOf(addedStates, jumpState);
                            state.GOTO[symbol - 1] = ""+index;
                        }
                    }
                }
            }
            //Refreshing list of states
            LALRStates.addAll(addedStates);
            addedStates.clear();
        } while(statesAdded);
    }

    //This method returns a number of a state in group of states
    private int indexOf(LinkedList<LALRGroupOfItems> states, LALRGroupOfItems state){
        for(int index = 0; index < states.size(); index++){
            if(states.get(index).equal(state)){
                return index;
            }
        }
        return -1;
    }

    //This method returns a list of items (a state), which LALR analyzer enters, when it meets a symbol
    private LALRGroupOfItems LALRJump(LALRGroupOfItems start, int symbol){
        LALRGroupOfItems result = new LALRGroupOfItems();
        for(int itemNum = 0; itemNum < start.items.size(); itemNum++){
            int[] item = start.items.get(itemNum);
            LinkedList<Integer> production = grammar.productions.get(item[0]);
            //If the symbol is located in the end of the production, jump() will do nothing
            if(production.size() <= item[1]){
                continue;
            }
            int followingSymbol = production.get(item[1]);
            //The next symbol is the same as the entry symbol, the point moves forward
            if(followingSymbol == symbol){
                if(!result.containsItem(new int[]{item[0], item[1] + 1})){
                    result.items.add(new int[]{item[0], item[1] + 1});
                    result.lookaheads.add(start.lookaheads.get(itemNum));
                }
            }
        }
        result = LALRClosure(result);
        return result;
    }
    //This method returns closure of the group of items, generates lookaheads fow new items and updates old
    private LALRGroupOfItems LALRClosure(LALRGroupOfItems state){
        LALRGroupOfItems result = new LALRGroupOfItems();
        //Adding all items to the result
        result.items.addAll(state.items);
        result.lookaheads.addAll(state.lookaheads);
        boolean addedItems;
        do{
            addedItems = false;
            LALRGroupOfItems newItems = new LALRGroupOfItems();
            for(int i = 0; i < result.items.size(); i++){
                int[] item = result.items.get(i);
                LinkedList<Integer> production = grammar.productions.get(item[0]);
                //If this is the last symbol of the line, it can not produce anything
                if(item[1] >= production.size()){
                    continue;
                }
                int nextSymbol = production.get(result.items.get(i)[1]);
                //If the next symbol is a non-terminal, it has it's own productions
                if(grammarTags.nonTerminalTags.containsValue(nextSymbol)){
                    //Lookaheads of this production
                    LinkedList<Integer> lookaheadSymbols =
                            firstOfGroup(new int[]{result.items.get(i)[0], result.items.get(i)[1] + 1},
                                    result.lookaheads.get(i));
                    //Trying to find productions of this symbol in grammar
                    for(int j = 0; j < grammar.productions.size(); j++){
                        if(grammar.productions.get(j).get(0) == nextSymbol){
                            int[] newItem = {j, 1};
                            int indexInResult = result.indexOf(newItem);
                            int indexInNewItems = newItems.indexOf(newItem);
                            //Updating lookaheads
                            if(indexInResult != -1){
                                for(int symbol : lookaheadSymbols){
                                    if(!result.lookaheads.get(indexInResult).contains(symbol)){
                                        result.lookaheads.get(indexInResult).add(symbol);
                                    }
                                }
                            }
                            if(indexInNewItems != -1){
                                for(int symbol : lookaheadSymbols){
                                    if(!newItems.lookaheads.get(indexInNewItems).contains(symbol)){
                                        newItems.lookaheads.get(indexInNewItems).add(symbol);
                                    }
                                }
                            }
                            //If the item is new, adding it
                            if(indexInNewItems == -1 && indexInResult == -1){
                                newItems.items.add(newItem);
                                newItems.lookaheads.add(lookaheadSymbols);
                                addedItems = true;
                            }
                        }
                    }
                }
            }
            for(int i = 0; i < newItems.items.size(); i++){
                if(!result.containsItem(newItems.items.get(i))){
                    result.items.add(newItems.items.get(i));
                    result.lookaheads.add(newItems.lookaheads.get(i));
                }
            }
        } while(addedItems);
        result.LALRsort();
        return result;
    }

    //This method returns a list of terminals, which can be first in the sentential form of the symbol
    private LinkedList<Integer> first(int symbol){
        LinkedList<Integer> firstSymbols = new LinkedList<Integer>();
        if(grammarTags.terminalTags.containsValue(symbol)){
            //Terminal symbols can produce only themselves
            firstSymbols.add(symbol);
            return firstSymbols;
        }
        //This is a non-terminal, starting to analyze the grammar
        for(LinkedList<Integer> production: grammar.productions){
            if(production.get(0) == symbol){
                //A production of this symbol has been found
                if(production.get(1) == 0){
                   //This symbol obviously produces an empty line
                   firstSymbols.add(0);
                   continue;
                }
                //This symbol does not produce an empty line obviously
                //Starting to analyze the body of the production
                boolean producesEmpty = false;
                for(int i = 1; i < production.size(); i++){
                    if(production.get(i) == symbol){
                        //Preventing an infinite recursion. Cases of analyzed objects being in he body of a production
                        //will be reviewed later
                        break;
                    }
                    producesEmpty = false;
                    if(first(production.get(i)).contains(0)){
                        //If the symbol produces an empty line, all of it's first() symbols must be added to the first()
                        //symbols of the head, and the next symbol must be analyzed
                        producesEmpty = true;
                        for(int firstSymbol : first(production.get(i))){
                            if(!firstSymbols.contains(firstSymbol) && firstSymbol != 0){
                                firstSymbols.add(firstSymbol);
                            }
                        }
                    } else {
                        //The symbol does not produce an empty line. All of it's first symbols are added to the first()
                        //symbols of the head
                        for(int firstSymbol : first(production.get(i))){
                            if(!firstSymbols.contains(firstSymbol)){
                                firstSymbols.add(firstSymbol);
                            }
                        }
                    }
                    if(producesEmpty && i == production.size() - 1){
                        //If it is the last symbol of the production, and it produces an empty line, the head produces
                        //an empty line
                        firstSymbols.add(0);
                    }
                    if(!producesEmpty){
                        break;
                    }
                }
            }
        }
        //All the obvious productions have been reviewed. Reviewing recursive productions
        if(!firstSymbols.contains(0)){
            return firstSymbols;
        }

        for(LinkedList<Integer> production: grammar.productions){
            if(production.get(0) == symbol){
                //A production of this symbol has been found
                if(production.get(1) == 0){
                    continue;
                }
                //Starting to analyze the body of the production
                boolean producesEmpty = false;
                for(int i = 1; i < production.size(); i++){
                    if(production.get(i) == symbol){
                        //To continue the analysis of the production, the symbol must produce an empty line
                        continue;
                    }
                    producesEmpty = false;
                    if(first(production.get(i)).contains(0)){
                        //If the symbol produces an empty line, all of it's first() symbols must be added to the first()
                        //symbols of the head, and the next symbol must be analyzed
                        producesEmpty = true;
                        for(int firstSymbol : first(production.get(i))){
                            if(!firstSymbols.contains(firstSymbol) && firstSymbol != 0){
                                firstSymbols.add(firstSymbol);
                            }
                        }
                    } else {
                        //The symbol does not produce an empty line. All of it's first symbols are added to the first()
                        //symbols of the head
                        for(int firstSymbol : first(production.get(i))){
                            if(!firstSymbols.contains(firstSymbol)){
                                firstSymbols.add(firstSymbol);
                            }
                        }
                    }
                    if(!producesEmpty){
                        break;
                    }
                }
            }
        }
        return firstSymbols;
    }

    //This method is used to generate lookaheads. Returns first of group of symbols and a lookahead
    private LinkedList<Integer> firstOfGroup(int[] item, LinkedList<Integer> lookaheads){
        LinkedList<Integer> result = new LinkedList<>();
        LinkedList<Integer> production = grammar.productions.get(item[0]);
        //Adding all first() of the group of symbols. If they end, adding lookaheads
        for(int i = item[1]; i < production.size(); i++){
            LinkedList<Integer> firstOfNext = first(production.get(i));
            for(int symbol : firstOfNext){
                if(!result.contains(symbol) && symbol != 0){
                    result.add(symbol);
                }
            }
            //
            if(!firstOfNext.contains(0)){
                //If the symbol doesn't produce an empty line, symbols after it can't
                //become lookaheads for the needed symbol
                return result;
            }
        }
        //The end of the production is reached, adding production's lookaheads
        for(int symbol : lookaheads){
            if(!result.contains(symbol) && symbol != 0){
                result.add(symbol);
            }
        }
        return result;
    }

    //This method returns a list of terminals, which can be immediately after this symbol in a line
    private LinkedList<Integer> follow(int symbol){
        LinkedList<Integer> followSymbols = new LinkedList<Integer>();
        //The only symbol that can follow the start non-terminal is the end of a line symbol
        if(grammar.productions.get(0).get(0) == symbol){
            followSymbols.add(-1);
            return followSymbols;
        }
        //Starting to look for this symbol in bodies of the grammar
        for(LinkedList<Integer> production : grammar.productions){
            for(int i = 1; i < production.size(); i++){
                boolean producesEmpty = false;
                if(production.get(i) == symbol){
                    //The symbol has been found
                    for(int j = i; j < production.size() - 1; j++){
                        producesEmpty = false;
                        LinkedList<Integer> firsts = first(production.get(j + 1));
                        //Adding all first() from the next symbol to the follow()
                        if(firsts.contains(0)){
                            producesEmpty = true;
                        }
                        for(int firstSymbol: firsts){
                            if(!followSymbols.contains(firstSymbol) && firstSymbol != 0){
                                followSymbols.add(firstSymbol);
                            }
                        }
                        if(!producesEmpty){
                            break;
                        }
                    }
                }
                //If an empty line is possible after the symbol, follow() of the head belongs to it
                if((producesEmpty && i == production.size() - 1 || production.getLast() == symbol)
                        && production.getFirst() != symbol){
                    for(int followSymbol : follow(production.get(0))){
                        if(!followSymbols.contains(followSymbol)){
                            followSymbols.add(followSymbol);
                        }
                    }
                }
            }
        }
        return followSymbols;
    }
}
