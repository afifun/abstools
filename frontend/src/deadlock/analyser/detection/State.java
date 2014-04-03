package deadlock.analyser.detection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import deadlock.analyser.factory.GroupName;

//Class State implement the structure that contains a "list of couple (a,b)", this list is implement like an HashMap, the key is a TermVariable and the value is a 
//list of other TermVariable which appear in the right side of a couple dependency.



public class State {

    //hashmaps storing dependency couples
    HashMap<GroupName, HashSet<GroupName>> depCouple;
    HashMap<GroupName, HashSet<GroupName>> depCoupleAwait;
    
    public State(){
        this.depCouple = new HashMap<GroupName, HashSet<GroupName>>();
        this.depCoupleAwait = new HashMap<GroupName, HashSet<GroupName>>(); 
    }

    //Method for add (a,b) to this State
    public void addCouple(GroupName a, GroupName b){
        if(!depCouple.containsKey(a))
            depCouple.put(a, new HashSet<GroupName>());
        
        HashSet<GroupName> aCouples = depCouple.get(a);            
        
        if(!aCouples.contains(b))
            aCouples.add(b);
        
        HashSet<GroupName> aCouplesAwait;
        if((aCouplesAwait = depCoupleAwait.get(a)) != null && aCouplesAwait.contains(b))
            aCouplesAwait.remove(b);
      
    }

    //Method for add (a,b)@ to this State
    
    public void addCoupleAwait(GroupName a, GroupName b){ 	
        if(!containsCouple(a, b))
        {
            if(!depCoupleAwait.containsKey(a))
                depCoupleAwait.put(a, new HashSet<GroupName>());
            
            HashSet<GroupName> aCouplesAwait = depCoupleAwait.get(a);            
            
            if(!aCouplesAwait.contains(b))
                aCouplesAwait.add(b);
        }
   
    }

    //Method for add entire State s to this State, useful for parallel operation
    public void addState(State s){
        
        for(GroupName a : s.depCouple.keySet())
            for(GroupName b: s.depCouple.get(a))
                addCouple(a, b);
        
        for(GroupName a : s.depCoupleAwait.keySet())
            for(GroupName b: s.depCoupleAwait.get(a))
                addCoupleAwait(a, b);
   
    }

    //Method to know if a state is contained in this state
    public Boolean containState(State s){
        
        for(GroupName a : s.depCouple.keySet())
            for(GroupName b: s.depCouple.get(a))
                if(!containsCoupleGet(a, b))
                    return false;
        
        for(GroupName a : s.depCoupleAwait.keySet())
            for(GroupName b: s.depCoupleAwait.get(a))
                if(!containsCoupleAwait(a, b))
                    return false;
        
        return true;
    }

    //Method to know if a couple get is contained in this state
        private boolean containsCoupleGet(GroupName a, GroupName b) {
        HashSet<GroupName> aGroup;
        return ((aGroup = depCouple.get(a)) != null && aGroup.contains(b)) ;
    }
    
    //Method to know if a couple await is contained in this state
    private boolean containsCoupleAwait(GroupName a, GroupName b) {
        HashSet<GroupName> aGroup;
        return ((aGroup = depCoupleAwait.get(a)) != null && aGroup.contains(b)) ;
    }

    //Method to know if a couple is contained in this state
    public Boolean containsCouple(GroupName a, GroupName b){
        HashSet<GroupName> aGroup, aGroupAwait;
        return ((aGroup = depCouple.get(a)) != null && aGroup.contains(b)) || ((aGroupAwait = depCoupleAwait.get(a)) != null && aGroupAwait.contains(b));
    }

    //VarSubstitution (renaming) application
    public void apply(VarSubstitution s){
        
        depCouple = refreshHashMap(s, depCouple);
        depCoupleAwait = refreshHashMap(s, depCoupleAwait);
        
    }

    //performs a variable name substitution in dependency couples hashmap, returns new instance with new names
    private static HashMap<GroupName, HashSet<GroupName>> refreshHashMap(VarSubstitution s, HashMap<GroupName, HashSet<GroupName>> toRefresh) {
        
        HashMap<GroupName, HashSet<GroupName>> temp = new HashMap<GroupName, HashSet<GroupName>>(toRefresh.size());
        
        for(GroupName a: toRefresh.keySet())
        {
            GroupName key = s.apply(a);
            if(!temp.containsKey(key)){
              temp.put(key, new HashSet<GroupName>());
            }
            
            for (GroupName b : toRefresh.get(a)){
                temp.get(key).add(s.apply(b));
            }
            
        }
        
        return temp;
    }

    //Calculates de number of dependencies
    public Integer numberOfDep(){
        Integer i = 0;
        for(GroupName v : depCouple.keySet())
           i+= depCouple.get(v).size();
        for(GroupName v : depCoupleAwait.keySet())
            i+= depCoupleAwait.get(v).size();
        return i;
    }

    //calculates all the variables of the state
    //TODO ABEL: check correctness
    public Set<GroupName> fv(){
        Set<GroupName> fv = new TreeSet<GroupName>();
        for(GroupName a : depCouple.keySet()){
            fv.add(a);
            for(GroupName b : depCouple.get(a))
                fv.add(b);
        }
        for(GroupName a : depCoupleAwait.keySet()){
            fv.add(a);
            for(GroupName b : depCoupleAwait.get(a))
                fv.add(b);
        }
        return fv;
    }

    //Determines if there is a pure get dependencies cycle (Deadlock)
    public boolean hasCycleGet()
    {
        return hasCycle(depCouple);
    }
    
    //Determines if there is a pure await dependencies cycle (Livelock)
    public boolean hasCycleAwait()
    {
        return hasCycle(depCoupleAwait);
    }
    
    //Determines if there is a cycle combining both kind of dependencies (Lock)
    public boolean hasCycle()
    {
        //this doesn't work because there can be a deadlock combining dependencies from both kinds
        //return hasCycle = HasCycleGet() || HasCycleAwait();
        
        //this is the correct approach, to create the Union Graph and then check for cycles
        HashMap<GroupName, HashSet<GroupName>> allTogether = new  HashMap<GroupName, HashSet<GroupName>>();
        
        for(GroupName a : depCouple.keySet())
            for(GroupName b: depCouple.get(a)){
            if(!allTogether.containsKey(a))
                allTogether.put(a, new HashSet<GroupName>());
            
            allTogether.get(a).add(b);
            }
        for(GroupName a : depCoupleAwait.keySet())
            for(GroupName b: depCoupleAwait.get(a)){
            if(!allTogether.containsKey(a))
                allTogether.put(a, new HashSet<GroupName>());
            
            allTogether.get(a).add(b);
            }
        
        return hasCycle(allTogether);
    }
    
    //Checks for a cycle in a dependency couples hashmap
    private static boolean hasCycle(HashMap<GroupName, HashSet<GroupName>> graph)
    {
        HashSet<GroupName> visited = new HashSet<GroupName>();
        HashSet<GroupName> recorded = new HashSet<GroupName>();
        
        for(GroupName a : graph.keySet())
        {
            if(hasCycleUtil(graph, recorded, visited, a))
                return true;
        }
            
        
        return false;
    }
    
    //recursive method for cycling detection
    private static boolean hasCycleUtil(HashMap<GroupName, HashSet<GroupName>> graph, HashSet<GroupName> recorded, HashSet<GroupName> visited, GroupName current)
    {
       //this method performs a classic Breath First Search to check for cycles in an undirected graph and
       //keeps tracks of ancestors to determine if the closing edge is in deed a back edge
       if(!visited.contains(current))
       {
           visited.add(current);
           recorded.add(current);
           
           if(graph.containsKey(current)){
               for(GroupName b : graph.get(current))
               {
                   if(!visited.contains(b) && hasCycleUtil(graph, recorded, visited, b))
                       return true;
                   if(recorded.contains(b))
                       return true;
               }
           }
       }
       recorded.remove(current);
       return false;
        
    }
    
    //toString method
    public String toString(){
        String res = "";
        for(GroupName v : depCouple.keySet()){
            res += v.toString() + ": ";
            for(GroupName c : depCouple.get(v)){
                res += c.toString() + " ";
            }
            res += "\n";
        }
        
        for(GroupName v : depCoupleAwait.keySet()){
            res += v.toString() + ": ";
            for(GroupName c : depCoupleAwait.get(v)){
                res += c.toString() + " ";
            }
            res += "\n";
        }
        return res;
    }
}
