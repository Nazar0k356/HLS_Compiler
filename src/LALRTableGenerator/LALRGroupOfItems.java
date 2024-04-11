package LALRTableGenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java. util. Arrays;

//This class defines a group of items (a state), used while building LALR-table
public class LALRGroupOfItems{
    //A list of items in this group of items.
    //First number - number of production, second - position of point in the production
    public ArrayList<int[]> items = new ArrayList<>();

    //Symbols which can follow an item (lookaheads). This list and "items" are bonded
    public LinkedList<LinkedList<Integer>> lookaheads = new LinkedList<LinkedList<Integer>>();

    //This is a table of all analyzer state changes, depending on a next symbol
    public String[] GOTO;

    //Sorting the items
    private void LRsort(){
        //Simple bubble sort
        int size = items.size();
        for (int i = 0; i < size - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < size - i - 1; j++) {
                //Swapping elements, if number of the production of the first is greater than that of a second,
                //or numbers of their productions is equal, but the point of the first is further
                if (items.get(j)[0] > items.get(j + 1)[0] || (( items.get(j)[0] == items.get(j + 1)[0])
                        && items.get(j)[1] > items.get(j + 1)[1])){
                    //Swapping items
                    int[] temp = items.get(j);
                    items.set(j, items.get(j + 1));
                    items.set(j + 1, temp);
                }
            }
            if (!swapped) {
                break;
            }
        }
    }

    //Sorting the items and lookaheads
    void LALRsort(){
        //Simple bubble sort
        int size = items.size();
        for (int i = 0; i < size - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < size - i - 1; j++) {
                //Swapping elements, if number of the production of the first is greater than that of a second,
                //or numbers of their productions is equal, but the point of the first is further
                if (items.get(j)[0] > items.get(j + 1)[0] || ( items.get(j)[0] == items.get(j + 1)[0])
                        && items.get(j)[1] < items.get(j + 1)[1]){
                    //Swapping items
                    int[] tempItems = items.get(j);
                    items.set(j, items.get(j + 1));
                    items.set(j + 1, tempItems);
                    //Swapping lookaheads
                    LinkedList<Integer> tempLookaheads = lookaheads.get(j);
                    lookaheads.set(j, lookaheads.get(j + 1));
                    lookaheads.set(j + 1, tempLookaheads);
                }
            }
            if (!swapped) {
                break;
            }
        }
    }

    //Returns true, if two groups of items (sorted) are equal
    boolean equal(LALRGroupOfItems state) {
        if (state == null) {
            return false;
        }

        if (this.items.size() != state.items.size()) {
            return false;
        }

        for (int i = 0; i < this.items.size(); i++) {
            int[] item1 = this.items.get(i);
            int[] item2 = state.items.get(i);

            if (!Arrays.equals(item1, item2)) {
                return false;
            }
        }

        return true;
    }

    //Returns true, if group of items has an item
    boolean containsItem(int[] item){
        for(int[] groupItem : items){
            if(groupItem[0] == item[0] && groupItem[1] == item[1]){
                return true;
            }
        }
        return false;
    }

    //This method returns number of item in this group og items
    int indexOf(int[] item){
        for(int i = 0; i < items.size(); i++){
            if(items.get(i)[0] == item[0] && items.get(i)[1] == item[1]){
                return i;
            }
        }
        return -1;
    }

    //This method copies all non-present lookaheads from a state with identical items,
    //and returns false, if no symbols were added
    boolean addAllLookaheads(LALRGroupOfItems state){
        boolean result = false;
        for(int i = 0; i < state.lookaheads.size(); i++){
            //Checking all lists of lookaheads. If there exists a new lookahead,
            //adding it to a current list of lookaheads
            for(int lookaheadSymbol : state.lookaheads.get(i)){
                if(!this.lookaheads.get(i).contains(lookaheadSymbol)){
                    this.lookaheads.get(i).add(lookaheadSymbol);
                    result = true;
                }
            }
        }
        return result;
    }
}
