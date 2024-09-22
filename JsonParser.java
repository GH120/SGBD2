import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
    public Database jsonToDatabase(String path) {
        Gson gson = new Gson();
        Database database = null;

        try (FileReader reader = new FileReader(path)) {
            Type databaseType = new TypeToken<Database>() {}.getType();
            database = gson.fromJson(reader, databaseType);
            database.nodes = database.tabelas;

            for (Tabela tabela : database.tabelas) {
                print("Tabela:");
                tabela.setPai(database);
                tabela.nodes = tabela.paginas;
                // print("PAI TABELA: " + tabela.getPai().toString());

                for (Pagina pagina : tabela.paginas) {
                    print("    Pagina:");
                    pagina.setPai(tabela);
                    pagina.nodes = pagina.registros;
                    // print("PAI PAGINA: " + pagina.getPai().toString());

                    for (Registro registro : pagina.registros) {
                        print("        Registro: " + registro.nome + ", Valor: " + registro.valor);
                        registro.setPai(pagina);
                        registro.bloqueios = new ArrayList<>();
                        // print("PAI REGISTRO: " + registro.getPai().toString());
                        
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return database;
    }

    public LinkedList<Operacao> jsonToOperacoes(String path, Database database) {
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(path)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            LinkedList<Operacao> operacoes = new LinkedList<>();

            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                
                Integer transaction = jsonObject.get("transaction").getAsInt();
                String registroNome = jsonObject.get("registro").getAsString();
                Operacao.lock escopoLock = Operacao.lock.valueOf(jsonObject.get("escopo").getAsString());
                Operacao.type tipo = Operacao.type.valueOf(jsonObject.get("tipo").getAsString());

                System.out.println("Buscando " + registroNome);
                Registro registro = database.buscarRegistro(registroNome);
                System.out.println("Registro encontrado:" + registro);

                Operacao operacao = null;
                switch (tipo) {
                    case READ:
                        operacao = new Read(transaction, registro);
                        break;
                    case WRITE:
                        operacao = new Write(transaction, registro);
                        break;
                    case COMMIT:
                        operacao = new Commit(transaction);
                        break;
                    case ABORT:
                        operacao = new Abort(transaction);
                        break;
                }

                // Definir o lock/escopo da operação
                if (operacao != null) {
                    operacao.escopoLock = escopoLock;
                    operacoes.add(operacao);
                }
            }

            for (Operacao op : operacoes) {
                // print("Transação: " + op.transaction + ", Registro: " + op.registro.nome + ", Operação: " + op.tipoOperacao + ", Escopo: " + op.escopoLock);
                print("Operação: " + op.getNome());
            }
    
            return operacoes;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
