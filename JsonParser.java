import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class JsonParser {

    public boolean debug = false;

    public JsonParser(boolean debug) {
        this.debug = debug;
    }

    public void print(String x) {
        if (this.debug) {
            System.out.println(x);
        }
    }
    public void jsonToDatabase(String path) {
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(path)) {
            
            Type databaseType = new TypeToken<Database>() {}.getType();

            Database database = gson.fromJson(reader, databaseType);

            for (Tabela tabela : database.tabelas) {
                print("Tabela");
                for (Pagina pagina : tabela.paginas) {
                    print("    Pagina");
                    for (Registro registro : pagina.registros) {
                        print("        Registro: " + registro.nome + ", Valor: " + registro.valor);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
}
