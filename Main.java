import com.google.gson.Gson;
import java.util.*;

public class Main{
    public static void main(String[] args) {
        Database database = Controle.createDatabase("resources/dbs/database1.json");
        // System.out.println(database);
        LinkedList<Operacao> ops = Controle.createOpsList("resources/ops/database1.json", database);
        Controle.runEscalonamento(ops, database);
    }
}
