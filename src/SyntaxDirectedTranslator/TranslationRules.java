package SyntaxDirectedTranslator;

import LALRTableGenerator.LALRGenerator;

import java.util.ArrayList;
import java.util.LinkedList;

public class TranslationRules {
    TranslationRules(LALRGenerator syntaxAnalyser, SymbolTable table){
        this.syntaxAnalyser = syntaxAnalyser;
        this.table = table;
    }
    private final LALRGenerator syntaxAnalyser;
    private SymbolTable table;

    //This method is used to apply a translation rule to a stack.
    void applyRule(LinkedList<Symbol> symbolStack, LinkedList<Integer> stateStack, int productionNum) throws Exception{
        LinkedList<Integer> production = syntaxAnalyser.grammar.productions.get(productionNum);
        Symbol head = new Symbol(production.get(0)); //Initializing the head of the production
        int last = symbolStack.size() - 1;
        switch (productionNum){
            case 0:{ //start -> expr
                head.code = symbolStack.get(last).code;
                break;
            }
            case 1:{ //expr -> expr expr
                head.code.addAll(symbolStack.get(last-1).code);
                head.code.addAll(symbolStack.get(last).code);
                break;
            }
            case 2, 22:{ //expr -> S1 { expr }
                         //funExpr -> S1 { funExpr }
                //The brackets are over, deleting all vars, assigned in brackets
                head.code = symbolStack.get(last-1).code;
                table.close();
                table = table.fatherTable;
                break;
            }
            case 3:{ //expr -> if ( stm ) S1 { expr } S3
                String end = newLabel();
                Symbol stm = symbolStack.get(last-6);
                Symbol expr = symbolStack.get(last-2);
                //Code
                head.code.addAll(stm.code);
                head.code.add(new String[]{"goIfFalse", stm.cellName, null, end});
                head.code.addAll(expr.code);
                head.code.add(new  String[]{"label", end, null, null});
                break;
            }
            case 4, 16:{ //expr -> if ( stm ) S1 { expr } S3 else S1 { expr } S3
                         //$ funExpr -> if ( stm ) S1 { funExpr } S3 else S1 { funExpr } S3
                String end = newLabel();
                String ifFalse = newLabel();
                Symbol stm = symbolStack.get(last-12);
                Symbol expr1 = symbolStack.get(last-8);
                Symbol expr2 = symbolStack.get(last-2);
                //Code
                head.code.addAll(stm.code);
                head.code.add(new String[]{"goIfFalse", stm.cellName, null, ifFalse});
                head.code.addAll(expr1.code);
                head.code.add(new String[]{"goto", null, null, end});
                head.code.add(new String[]{"label", ifFalse, null, null});
                head.code.addAll(expr2.code);
                head.code.add(new String[]{"label", end, null, null});
                break;
            }
            case 5:{ //expr -> while ( stm ) S1 { expr } S3
                String start = newLabel();
                String end = newLabel();
                Symbol stm = symbolStack.get(last-6);
                Symbol expr = symbolStack.get(last-2);
                //Code
                head.code.add(new String[]{"label", start, null, null});
                head.code.addAll(stm.code);
                head.code.add(new String[]{"goIfFalse", stm.cellName, null, end});
                head.code.addAll(expr.code);
                head.code.add(new String[]{"goto", null, null, start});
                head.code.add(new String[]{"label", end, null, null});
                break;
            }
            case 6, 25, 28:{ //expr -> ε
                             //param -> ε
                             //arg -> ε
                break;
            }
            case 7:{ //expr -> type id ;
                String lexeme = symbolStack.get(last-1).value;
                Symbol.Type type = symbolStack.get(last-2).type;
                table.addVarID(lexeme, type);
                table.defined.put(lexeme, false);
                break;
            }
            case 8:{ //expr -> type id = stm ;
                Symbol id = symbolStack.get(last-3);
                Symbol stm = symbolStack.get(last-1);

                String lexeme = id.value;
                Symbol idType = symbolStack.get(last-4);

                String value = stm.value;
                Symbol.Type stmType = stm.type;

                //Checking for an error
                if(notCompatible(idType.type, stm.type)){
                    throw new Exception("Impossible to assign "+stmType.name+" value to "+idType.type.name+" variable!");
                }
                //Initializing a variable
                table.addVarID(lexeme, idType.type);
                table.defined.put(lexeme, true);

                if(value != null){
                    table.symbols.get(lexeme).value = value;
                }
                ArrayList<String[]> alignTypesCode = alignTypes(idType.type, stm);
                //Adding code
                head.code.addAll(stm.code);
                head.code.addAll(alignTypesCode);
                head.code.add(new String[]{"=", stm.cellName, null, table.symbols.get(lexeme).cellName});
                break;
            }
            case 9:{ //expr -> id = stm ;
                Symbol id = symbolStack.get(last-3);
                String lexeme = id.value;

                Symbol stm = symbolStack.get(last-1);
                String value = stm.value;

                //Checking for an error
                if(!table.symbols.containsKey(lexeme)){
                    throw new Exception("Identifier "+lexeme+" is not defined!");
                }
                Symbol.Type idType = table.symbols.get(lexeme).type;
                if(notCompatible(idType, stm.type)){
                    throw new Exception("Impossible to assign "+stm.type.name+" value to "+idType.name+" variable!");
                }
                if(table.symbols.get(lexeme).isFunction){
                    throw new Exception("Function identifier "+lexeme+" can not be used as a variable!");
                }
                table.defined.put(lexeme, true);
                if(value != null){
                    table.symbols.get(lexeme).value = value;
                }
                ArrayList<String[]> alignTypesCode = alignTypes(table.symbols.get(lexeme).type, stm);
                //Adding code
                head.code.addAll(stm.code);
                head.code.addAll(alignTypesCode);
                head.code.add(new String[]{"=", stm.cellName, null, table.symbols.get(lexeme).cellName});
                table.clearLastExpressionsOf(table.symbols.get(lexeme).cellName);
                break;
            }
            case 10:{ //type -> int
                head.type = head.new Type("int", 4, null);
                break;
            }
            case 11:{ //type -> long
                head.type = head.new Type("long", 8, null);
                break;
            }
            case 12:{ //type -> double
                head.type = head.new Type("double", 8, null);
                break;
            }
            case 13:{ //type -> bool
                head.type = head.new Type("bool", 1, null);
                break;
            }
            case 14:{ //expr -> type id ( param ) S2 { funExpr }
                Symbol param = symbolStack.get(last-5);
                Symbol funExpr = symbolStack.get(last-1);
                String lexeme = symbolStack.get(last-7).value;
                Symbol.Type type = symbolStack.get(last-8).type;

                int procedureSize = table.procedureSize;
                int oldProcSize = table.fatherTable.procedureSize;
                table.close();
                table = table.fatherTable;
                table.procedureSize = oldProcSize;
                table.addFunID(lexeme, type);
                String codeBeginning = newLabel();
                table.symbols.get(lexeme).codeBeginning = codeBeginning;
                table.symbols.get(lexeme).arguments = new LinkedList<>();
                table.symbols.get(lexeme).arguments.addAll(param.arguments);
                int paramSize = 0;
                for(Symbol parameter : param.arguments){
                    paramSize += parameter.type.byteSize;
                }
                String end = newLabel();
                //Code
                head.code.add(new String[]{"goto", null, null, end});
                head.code.add(new String[]{"label", codeBeginning, null, null});
                head.code.add(new String[]{"+", "@RBP", "@"+paramSize, "@RBP"});
                head.code.add(new String[]{"pop", null, null, "@RAX"});
                head.code.add(new String[]{"=", "@RAX", null, "@[RSP+"+(248+paramSize)+"]"}); //save=240, rbp=8, paramSize is defined
                head.code.add(new String[]{"-", "@RSP", "@"+(procedureSize-paramSize), "@RSP"});
                head.code.addAll(funExpr.code);
                head.code.add(new String[]{"label", end, null, null});
                break;
            }
            case 15:{ //funExpr -> expr return stm ;
                Symbol expr = symbolStack.get(last-3);
                Symbol stm = symbolStack.get(last-1);
                //Checking for an error
                if(!stm.type.name.equals(table.returnType.name)){
                    throw new Exception("Mismatch of return type: "+stm.type.name+", expected: "+table.returnType.name+"!");
                }
                //Code
                head.code.addAll(expr.code);
                head.code.addAll(stm.code);
                head.code.add(new String[]{"=", stm.cellName, null, "@[basePointer+256]"}); //save=240, rbp=8, ret = 8
                head.code.add(new String[]{"=", "@RBP", null, "@RSP"});
                head.code.add(new String[]{"=", "@[RSP]", null, "@RBP"});
                head.code.add(new String[]{"+", "@RSP", "@8", "@RSP"});
                head.code.add(new String[]{"restore", null, null, null});
                head.code.add(new String[]{"return", null, null, null});
                break;
            }
            case 17, 18, 19:{ //funExpr -> if ( stm ) S1 { funExpr } S3 else S1 { expr } S3 funExpr
                              //funExpr -> if ( stm ) S1 { expr } S3 else S1 { funExpr } S3 funExpr
                              //funExpr -> if ( stm ) S1 { expr } S3 else S1 { expr } S3 funExpr
                String end = newLabel();
                String ifFalse = newLabel();
                Symbol stm = symbolStack.get(last-13);
                Symbol expr1 = symbolStack.get(last-9);
                Symbol expr2 = symbolStack.get(last-3);
                Symbol expr3 = symbolStack.get(last);
                //Code
                head.code.addAll(stm.code);
                head.code.add(new String[]{"goIfFalse", stm.cellName, null, ifFalse});
                head.code.addAll(expr1.code);
                head.code.add(new String[]{"goto", null, null, end});
                head.code.add(new String[]{"label", ifFalse, null, null});
                head.code.addAll(expr2.code);
                head.code.add(new String[]{"label", end, null, null});
                head.code.addAll(expr3.code);
                break;
            }
            case 20:{ //funExpr -> if ( stm ) S1 { funExpr } S3 funExpr
                String end = newLabel();
                Symbol stm = symbolStack.get(last-7);
                Symbol expr1 = symbolStack.get(last-3);
                Symbol expr2 = symbolStack.get(last);
                //Code
                head.code.addAll(stm.code);
                head.code.add(new String[]{"goIfFalse", stm.cellName, null, end});
                head.code.addAll(expr1.code);
                head.code.add(new  String[]{"label", end, null, null});
                head.code.addAll(expr2.code);
                break;
            }
            case 21:{ //funExpr -> while ( stm ) S1 { funExpr } S3 funExpr
                String start = newLabel();
                String end = newLabel();
                Symbol stm = symbolStack.get(last-7);
                Symbol expr1 = symbolStack.get(last-3);
                Symbol expr2 = symbolStack.get(last);
                //Code
                head.code.add(new String[]{"label", start, null, null});
                head.code.addAll(stm.code);
                head.code.add(new String[]{"goIfFalse", stm.cellName, null, end});
                head.code.addAll(expr1.code);
                head.code.add(new String[]{"goto", null, null, start});
                head.code.add(new String[]{"label", end, null, null});
                head.code.addAll(expr2.code);
                break;
            }
            case 23, 26:{ //param -> param , param
                          //arg -> arg , arg
                Symbol param1 = symbolStack.get(last-2);
                Symbol param2 = symbolStack.get(last);
                head.arguments.addAll(param1.arguments);
                head.arguments.addAll(param2.arguments);
                break;
            }
            case 24:{ //param -> type id
                Symbol.Type type = symbolStack.get(last-1).type;
                Symbol id = symbolStack.get(last);
                head.arguments = new LinkedList<>();
                head.arguments.add(new Symbol(syntaxAnalyser.getId()));
                head.arguments.getLast().value = id.value;
                head.arguments.getLast().type = type;
                break;
            }
            case 27:{ //arg ->  stm
                Symbol stm = symbolStack.get(last);
                head.arguments.add(stm);
                break;
            }
            case 29, 30, 31, 32:{ //stm -> stm + stm
                                  //stm -> stm - stm
                                  //stm -> stm * stm
                                  //stm -> stm / stm
                Symbol stm1 = symbolStack.get(last-2);
                Symbol stm2 = symbolStack.get(last);
                //Checking for an error
                String op;
                op = switch (productionNum) {
                    case 29 -> "+";
                    case 30 -> "-";
                    case 31 -> "*";
                    case 32 -> "/";
                    default -> null;
                };
                if(notCompatibleStm(stm1, stm2) || stm1.type.name.equals("bool") || stm2.type.name.equals("bool")){
                    throw new Exception("Impossible to apply "+op+" to values of "+stm1.type.name+" and "+stm2.type.name+" types!");
                }
                ArrayList<String[]> alignTypesCode = alignTypesStm(stm1, stm2);
                if(stm1.value != null && stm2.value != null){
                    String result = "";
                    switch(stm1.type.name){
                        case "int":{
                            int value1 = Integer.parseInt(stm1.value);
                            int value2 = Integer.parseInt(stm2.value);
                            switch(productionNum){
                                case 29:
                                    result += (value1 + value2);
                                    break;
                                case 30:
                                    result += (value1 - value2);
                                    break;
                                case 31:
                                    result += (value1 * value2);
                                    break;
                                case 32:
                                    result += (value1 / value2);
                                    break;
                            }
                            break;
                        }
                        case "long":{
                            long value1 = Long.parseLong(stm1.value);
                            long value2 = Long.parseLong(stm2.value);
                            switch(productionNum){
                                case 29:
                                    result += (value1 + value2);
                                    break;
                                case 30:
                                    result += (value1 - value2);
                                    break;
                                case 31:
                                    result += (value1 * value2);
                                    break;
                                case 32:
                                    result += (value1 / value2);
                                    break;
                            }
                            break;
                        }
                        case "double":{
                            double value1 = Double.parseDouble(stm1.value);
                            double value2 = Double.parseDouble(stm2.value);
                            switch(productionNum){
                                case 29:
                                    result += (value1 + value2);
                                    break;
                                case 30:
                                    result += (value1 - value2);
                                    break;
                                case 31:
                                    result += (value1 * value2);
                                    break;
                                case 32:
                                    result += (value1 / value2);
                                    break;
                            }
                            break;
                        }
                    }
                    head.value = result;
                }
                head.type = stm1.type;
                if(table.getLastOccurrence(new String[]{op, stm1.cellName, stm2.cellName}) != null){
                    head.cellName = table.getLastOccurrence(new String[]{op, stm1.cellName, stm2.cellName});
                    break;
                }
                head.cellName = table.giveMemory(head.type);
                //Code
                head.code.addAll(stm1.code);
                head.code.addAll(stm2.code);
                head.code.addAll(alignTypesCode);
                head.code.add(new String[]{op, stm1.cellName, stm2.cellName, head.cellName});
                if(op.equals("-") || op.equals("/")){
                    table.lastExpressions.put(new String[]{op, stm1.cellName, stm2.cellName}, head.cellName);
                } else {
                    table.lastExpressions.put(new String[]{op, stm1.cellName, stm2.cellName}, head.cellName);
                    table.lastExpressions.put(new String[]{op, stm2.cellName, stm1.cellName}, head.cellName);
                }
                break;
            }
            case 33, 34:{ //stm -> stm & stm
                          //stm -> stm | stm
                Symbol stm1 = symbolStack.get(last-2);
                Symbol stm2 = symbolStack.get(last);
                //Checking for an error
                String op;
                if(productionNum == 33){
                    op = "AND";
                } else {
                    op = "OR";
                }
                if(!stm1.type.name.equals("bool") || !stm2.type.name.equals("bool")){
                    throw new Exception("Impossible to apply "+op+" to values of "+stm1.type.name+" and "+stm2.type.name+" types!");
                }
                if(stm1.value != null && stm2.value != null){
                    String result = "";
                    boolean value1 = Boolean.parseBoolean(stm1.value);
                    boolean value2 = Boolean.parseBoolean(stm2.value);
                    switch(productionNum){
                        case 33:
                            result += (value1 && value2);
                            break;
                        case 34:
                            result += (value1 || value2);
                            break;
                    }
                    head.value = result;
                }
                head.type = head.new Type("bool", 1, null);
                if(table.getLastOccurrence(new String[]{op, stm1.cellName, stm2.cellName}) != null){
                    head.cellName = table.getLastOccurrence(new String[]{op, stm1.cellName, stm2.cellName});
                    break;
                }
                head.cellName = table.giveMemory(head.type);
                //Code
                head.code.addAll(stm1.code);
                head.code.addAll(stm2.code);
                head.code.add(new String[]{op, stm1.cellName, stm2.cellName, head.cellName});
                table.lastExpressions.put(new String[]{op, stm1.cellName, stm2.cellName}, head.cellName);
                table.lastExpressions.put(new String[]{op, stm2.cellName, stm1.cellName}, head.cellName);
                break;
            }
            case 35, 36:{ //stm -> stm ++
                          //stm -> stm --
                Symbol stm = symbolStack.get(last-1);
                String op;
                if(productionNum == 35){
                    op = "++";
                } else {
                    op = "--";
                }
                if(!stm.type.name.equals("int") && !stm.type.name.equals("long") && !stm.type.name.equals("double")){
                    throw new Exception("Impossible to apply "+op+" to a value of "+stm.type.name+" type!");
                }
                if(stm.value != null){
                    String result = "";
                    switch(stm.type.name){
                        case "int":{
                            if(productionNum ==35){
                                result += (Integer.parseInt(stm.value)+1);
                            } else {
                                result += (Integer.parseInt(stm.value)-1);
                            }
                            break;
                        }
                        case "long":{
                            if(productionNum == 35){
                                result += (Long.parseLong(stm.value)+1);
                            } else {
                                result += (Long.parseLong(stm.value)-1);
                            }
                            break;
                        }
                        case "double":{
                            if(productionNum == 35){
                                result += (Double.parseDouble(stm.value)+1);
                            } else {
                                result += (Double.parseDouble(stm.value)-1);
                            }
                            break;
                        }
                    }
                    head.value = result;
                }
                head.type = stm.type;
                if(table.getLastOccurrence(new String[]{op, stm.cellName, null}) != null){
                    head.cellName = table.getLastOccurrence(new String[]{op, stm.cellName, null});
                    break;
                }
                head.cellName = table.giveMemory(head.type);
                //Code
                head.code.addAll(stm.code);
                head.code.add(new String[]{op, stm.cellName, null, head.cellName});
                table.lastExpressions.put(new String[]{op, stm.cellName, null}, head.cellName);
                break;
            }
            case 37:{ //stm -> - stm
                Symbol stm = symbolStack.get(last);
                if(!stm.type.name.equals("int") && !stm.type.name.equals("long") && !stm.type.name.equals("double")){
                    throw new Exception("Impossible to apply unary minus to a value of "+stm.type.name+" type!");
                }
                if(stm.value != null){
                    String result = "";
                    switch(stm.type.name){
                        case "int":{
                            result += (Integer.parseInt(stm.value) * -1);
                            break;
                        }
                        case "long":{
                            result += (Long.parseLong(stm.value) * -1);
                            break;
                        }
                        case "double":{
                            result += (Double.parseDouble(stm.value) * -1);
                            break;
                        }
                    }
                    head.value = result;
                }
                head.type = stm.type;
                if(table.getLastOccurrence(new String[]{"uMinus", stm.cellName, null}) != null){
                    head.cellName = table.getLastOccurrence(new String[]{"uMinus", stm.cellName, null});
                    break;
                }
                head.cellName = table.giveMemory(head.type);
                //Code
                head.code.addAll(stm.code);
                head.code.add(new String[]{"uMinus", stm.cellName, null, head.cellName});
                table.lastExpressions.put(new String[]{"uMinus", stm.cellName, null}, head.cellName);
                break;
            }
            case 38:{ //stm -> ! stm
                Symbol stm = symbolStack.get(last);
                if(!stm.type.name.equals("bool")){
                    throw new Exception("Impossible to apply NOT to a value of "+stm.type.name+" type!");
                }
                if(stm.value != null){
                    String result = "";
                    result += !Boolean.parseBoolean(stm.value);
                    head.value = result;
                }
                head.type = stm.type;
                if(table.getLastOccurrence(new String[]{"NOT", stm.cellName, null}) != null){
                    head.cellName = table.getLastOccurrence(new String[]{"NOT", stm.cellName, null});
                    break;
                }
                head.cellName = table.giveMemory(head.type);
                //Code
                head.code.addAll(stm.code);
                head.code.add(new String[]{"NOT", stm.cellName, null, head.cellName});
                table.lastExpressions.put(new String[]{"NOT", stm.cellName, null}, head.cellName);
                break;
            }
            case 39, 40:{ //stm -> stm != stm
                          //stm -> stm == stm
                Symbol stm1 = symbolStack.get(last-2);
                Symbol stm2 = symbolStack.get(last);
                //Checking for an error
                String op;
                if(productionNum == 39){
                    op = "!=";
                } else {
                    op = "==";
                }
                if(notCompatibleStm(stm1, stm2) || stm1.type.name.equals("bool") || stm2.type.name.equals("bool")){
                    throw new Exception("Impossible to apply "+op+" to values of "+stm1.type.name+" and "+stm2.type.name+" types!");
                }
                ArrayList<String[]> alignTypesCode = alignTypesStm(stm1, stm2);
                if(stm1.value != null && stm2.value != null){
                    String result = "";
                    if(productionNum == 39){
                        if(!stm1.value.equals(stm2.value)){
                            result += "true";
                        } else {
                            result += "false";
                        }
                    } else {
                        if(stm1.value.equals(stm2.value)){
                            result += "true";
                        } else {
                            result += "false";
                        }
                    }
                    head.value = result;
                }
                head.type = head.new Type("bool", 1, null);
                if(table.getLastOccurrence(new String[]{op, stm1.cellName, stm2.cellName}) != null){
                    head.cellName = table.getLastOccurrence(new String[]{op, stm1.cellName, stm2.cellName});
                    break;
                }
                head.cellName = table.giveMemory(head.type);
                //Code
                head.code.addAll(stm1.code);
                head.code.addAll(stm2.code);
                head.code.addAll(alignTypesCode);
                head.code.add(new String[]{op, stm1.cellName, stm2.cellName, head.cellName});
                table.lastExpressions.put(new String[]{op, stm1.cellName, stm2.cellName}, head.cellName);
                table.lastExpressions.put(new String[]{op, stm2.cellName, stm1.cellName}, head.cellName);
                break;
            }
            case 41, 42, 43, 44:{ //stm -> stm < stm
                                  //stm -> stm > stm
                                  //stm -> stm <= stm
                                  //stm -> stm >= stm
                Symbol stm1 = symbolStack.get(last-2);
                Symbol stm2 = symbolStack.get(last);
                //Checking for an error
                String op;
                op = switch (productionNum) {
                    case 41 -> "<";
                    case 42 -> ">";
                    case 43 -> "<=";
                    case 44 -> ">=";
                    default -> null;
                };
                if(notCompatibleStm(stm1, stm2) || stm1.type.name.equals("bool") || stm2.type.name.equals("bool")){
                    throw new Exception("Impossible to apply "+op+" to values of "+stm1.type.name+" and "+stm2.type.name+" types!");
                }
                ArrayList<String[]> alignTypesCode = alignTypesStm(stm1, stm2);
                if(stm1.value != null && stm2.value != null){
                    String result = "";
                    switch(stm1.type.name){
                        case "int":{
                            int value1 = Integer.parseInt(stm1.value);
                            int value2 = Integer.parseInt(stm2.value);
                            switch(productionNum){
                                case 41:
                                    result += (value1 < value2);
                                    break;
                                case 42:
                                    result += (value1 > value2);
                                    break;
                                case 43:
                                    result += (value1 <= value2);
                                    break;
                                case 44:
                                    result += (value1 >= value2);
                                    break;
                            }
                            break;
                        }
                        case "long":{
                            long value1 = Long.parseLong(stm1.value);
                            long value2 = Long.parseLong(stm2.value);
                            switch(productionNum){
                                case 41:
                                    result += (value1 < value2);
                                    break;
                                case 42:
                                    result += (value1 > value2);
                                    break;
                                case 43:
                                    result += (value1 <= value2);
                                    break;
                                case 44:
                                    result += (value1 >= value2);
                                    break;
                            }
                            break;
                        }
                        case "double":{
                            double value1 = Double.parseDouble(stm1.value);
                            double value2 = Double.parseDouble(stm2.value);
                            switch(productionNum){
                                case 41:
                                    result += (value1 < value2);
                                    break;
                                case 42:
                                    result += (value1 > value2);
                                    break;
                                case 43:
                                    result += (value1 <= value2);
                                    break;
                                case 44:
                                    result += (value1 >= value2);
                                    break;
                            }
                            break;
                        }
                    }
                    head.value = result;
                }
                head.type = head.new Type("bool", 1, null);
                if(table.getLastOccurrence(new String[]{op, stm1.cellName, stm2.cellName}) != null){
                    head.cellName = table.getLastOccurrence(new String[]{op, stm1.cellName, stm2.cellName});
                    break;
                }
                head.cellName = table.giveMemory(head.type);
                //Code
                head.code.addAll(stm1.code);
                head.code.addAll(stm2.code);
                head.code.addAll(alignTypesCode);
                head.code.add(new String[]{op, stm1.cellName, stm2.cellName, head.cellName});
                table.lastExpressions.put(new String[]{op, stm1.cellName, stm2.cellName}, head.cellName);
                break;
            }
            case 45:{ //stm -> id
                Symbol id = symbolStack.get(last);
                String lexeme = id.value;
                //Checking for errors
                if(!table.symbols.containsKey(lexeme)){
                    throw new Exception("Identifier "+lexeme+" is not defined!");
                }
                if(table.symbols.get(lexeme).isFunction){
                    throw new Exception("Function identifier "+lexeme+" can not be used as a variable!");
                }
                if(!table.defined.get(lexeme)){
                    throw new Exception("Identifier "+lexeme+" is not initialized!");
                }
                head.cellName = table.symbols.get(lexeme).cellName;
                head.type = table.symbols.get(lexeme).type;
                if(table.symbols.get(lexeme).value != null){
                    head.value = table.symbols.get(lexeme).value;
                }
                break;
            }
            case 46:{ //stm -> const
                Symbol constant = symbolStack.get(last);
                try {
                    Integer.parseInt(constant.value);
                    head.type = head.new Type("int", 4, null);
                    head.value = constant.value;
                    if(table.getLastOccurrence(new String[]{"=", "@"+constant.value, null}) != null){
                        head.cellName = table.getLastOccurrence(new String[]{"=", "@"+constant.value, null});
                        break;
                    }
                    head.cellName = table.giveMemory(head.type);
                } catch (Exception e1) {
                    try{
                        Long.parseLong(constant.value);
                        head.type = head.new Type("long", 8, null);
                        head.value = constant.value;
                        if(table.getLastOccurrence(new String[]{"=", "@"+constant.value, null}) != null){
                            head.cellName = table.getLastOccurrence(new String[]{"=", "@"+constant.value, null});
                            break;
                        }
                        head.cellName = table.giveMemory(head.type);
                    } catch (Exception e2){
                        try{
                            Double.parseDouble(constant.value);
                            head.type = head.new Type("double", 8, null);
                            head.value = constant.value;
                            if(table.getLastOccurrence(new String[]{"=", "@"+constant.value, null}) != null){
                                head.cellName = table.getLastOccurrence(new String[]{"=", "@"+constant.value, null});
                                break;
                            }
                            head.cellName = table.giveMemory(head.type);
                        } catch (Exception e3){
                            throw new Exception("Illegal constant format!");
                        }
                    }
                }
                //Code
                head.code.add(new String[]{"=", "@"+constant.value, null, head.cellName});
                table.lastExpressions.put(new String[]{"=", "@"+constant.value, null}, head.cellName);
                break;
            }
            case 47:{ //stm -> true
                head.type = head.new Type("bool", 1, null);
                head.value = "true";
                if(table.getLastOccurrence(new String[]{"=", "@1", null}) != null){
                    head.cellName = table.getLastOccurrence(new String[]{"=", "@1", null});
                    break;
                }
                head.cellName = table.giveMemory(head.type);
                //Code
                head.code.add(new String[]{"=", "@1", null, head.cellName});
                table.lastExpressions.put(new String[]{"=", "@1", null}, head.cellName);
                break;
            }
            case 48:{ //stm -> false
                head.type = head.new Type("bool", 1, null);
                head.value = "false";
                if(table.getLastOccurrence(new String[]{"=", "@0", null}) != null){
                    head.cellName = table.getLastOccurrence(new String[]{"=", "@0", null});
                    break;
                }
                head.cellName = table.giveMemory(head.type);
                //Code
                head.code.add(new String[]{"=", "@0", null, head.cellName});
                table.lastExpressions.put(new String[]{"=", "@0", null}, head.cellName);
                break;
            }
            case 49:{ //stm -> id ( arg )
                Symbol idName = symbolStack.get(last-3);
                String lexeme = idName.value;
                Symbol arg = symbolStack.get(last-1);

                //Checking for an error
                if(!table.symbols.containsKey(lexeme)){
                    throw new Exception("Identifier "+lexeme+" is not defined!");
                }
                Symbol id = table.symbols.get(lexeme);
                if(!id.isFunction){
                    throw new Exception("Variable identifier "+lexeme+" can not be used as a function!");
                }
                if(arg.arguments.size() != id.arguments.size()){
                    throw new Exception("Mismatch of number of arguments: "+arg.arguments.size()+", expected: "+id.arguments.size()+"!");
                }
                for(int i = 0; i < arg.arguments.size(); i++){
                    Symbol.Type argumentType = arg.arguments.get(i).type;
                    Symbol.Type paramType = id.arguments.get(i).type;
                    if(!paramType.name.equals(argumentType.name)){
                        throw new Exception("Mismatch of argument type: "+argumentType+", expected: "+paramType+"!");
                    }
                }
                String resultCell = table.giveMemory(id.type);
                head.cellName = resultCell;
                head.type = id.type;
                //Code
                head.code.add(new String[]{"-", "@RSP", "@"+head.type.byteSize, "@RSP"});
                head.code.add(new String[]{"-", "@RSP", "@8", "@RSP"});
                head.code.add(new String[]{"save", null, null, null});
                head.code.add(new String[]{"push", "@RBP", null, null});
                for(Symbol argument : arg.arguments){
                    head.code.addAll(argument.code);
                    head.code.add(new String[]{"param", argument.cellName, null, null});
                }
                head.code.add(new String[]{"=", "@RSP", null, "@RBP"});
                head.code.add(new String[]{"call", null, null, id.codeBeginning});
                head.code.add(new String[]{"pop", null, null, resultCell});
                break;
            }
            case 50:{ //stm -> ( stm )
                head = symbolStack.get(last-1);
                break;
            }
            case 51:{ //S1 -> ε
                //Creating a new table before brackets
                table = new SymbolTable(table, table.idTag);
                break;
            }
            case 52:{ //S2 -> ε
                Symbol param  = symbolStack.get(last-1);
                Symbol.Type type  = symbolStack.get(last-4).type;

                //Creating table of symbols of a new function
                SymbolTable funTable = new SymbolTable(this.table, syntaxAnalyser.getId());
                funTable.symbols.clear();
                funTable.sizeOfMemory.clear();
                funTable.addressOfMemory.clear();
                funTable.defined.clear();
                funTable.maxAddress = 0;
                funTable.needToTransfer.clear();
                funTable.lastExpressions.clear();
                funTable.returnType = type;
                funTable.procedureSize = 0;

                //Adding start symbol table of the function
                for(Symbol parameter : param.arguments){
                    funTable.addVarID(parameter.value, parameter.type);
                    funTable.defined.put(parameter.value, true);
                }
                this.table = funTable;
                break;
            }
            case 53:{ //S3 -> ε
                table.close();
                table = table.fatherTable;
                break;
            }
        }
        //Removing symbols and states from stacks, transferring information about lines, which the symbol occupies
        if(production.get(1) != 0){
            //If the production is empty, no symbols need to be removed
            for(int i = 0; i < production.size() - 1; i++){
                Symbol temp = symbolStack.removeLast();
                stateStack.removeLast();
                if(i == production.size() - 2){
                    head.lineMin = temp.lineMin;
                }
                if(i == 0){
                    head.lineMax = temp.lineMax;
                }
            }
        }
        symbolStack.addLast(head);
        int nextState = Integer.parseInt(syntaxAnalyser.LALRStates.get(stateStack.getLast()).GOTO[head.tag - 1]);
        stateStack.addLast(nextState);
    }

    //Method which checks if value of given type can be given to a variable of given type
    private boolean notCompatible(Symbol.Type idType, Symbol.Type stmType){
        if(!idType.name.equals(stmType.name)){
            return (!idType.name.equals("long") && !idType.name.equals("double")) || !stmType.name.equals("int");
        }
        return false;
    }

    //Method which checks if two values of different types are compatible in arithmetic operations
    private boolean notCompatibleStm(Symbol stm1, Symbol stm2){
        return notCompatible(stm1.type, stm2.type) && notCompatible(stm2.type, stm1.type);
    }

    //Generates code which makes stm the same type of id
    private ArrayList<String[]> alignTypes(Symbol.Type idType, Symbol stm){
        ArrayList<String[]> result = new ArrayList<>();
        System.out.println("Aligning "+stm.type.name+" to "+idType.name);
        if(idType.name.equals(stm.type.name)){
            return result;
        }
        String oldCellName = stm.cellName;
        if(idType.name.equals("long") && stm.type.name.equals("int")){
            stm.type = stm.new Type("long", 8, null);
            String newCellName = table.giveMemory(stm.type);
            stm.cellName = newCellName;
            result.add(new String[]{"intToLong", oldCellName, null, newCellName});
        }
        if(idType.name.equals("double") && stm.type.name.equals("int")){
            stm.type = stm.new Type("double", 8, null);
            String newCellName = table.giveMemory(stm.type);
            stm.cellName = newCellName;
            result.add(new String[]{"intToDouble", oldCellName, null, newCellName});
        }
        return  result;
    }

    //Generates code which makes two stm be the same type
    private ArrayList<String[]> alignTypesStm(Symbol stm1, Symbol stm2){
        if(!notCompatible(stm1.type, stm2.type)){
            return alignTypes(stm1.type, stm2);
        } else {
            return alignTypes(stm2.type, stm1);
        }
    }

    private int labelNum = 0;

    //Creating a new code label
    public String newLabel(){
        return "L"+labelNum++;
    }
}
