import com.google.gson.Gson;

public class Main{
    public static void main(String[] args) {
        Database database = Controle.createDatabase("resources/dbs/database1.json");
        System.out.println(database);
        Controle.createOpsList("resources/ops/database1.json", database);
    }
}
