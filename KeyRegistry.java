import java.util.HashMap;

public class KeyRegistry{


    public HashMap<String, String> hashMap;


    KeyRegistry(){
        hashMap = new HashMap<>();
    }

    public void add (Class c, String key){
        hashMap.put(c.getName(), key);
    }

    public String get(Class c){
        return hashMap.get(c.getName());
    }
}