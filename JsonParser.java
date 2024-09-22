import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParserOperacoes {

    public static void main(String[] args) {
        // String de operações de exemplo
        String operacoesStr = "w1(registro1 with tablelock) r1(registro1) u1(registro2) c2 a3";

        // Processa a string de operações e gera o JSON
        JsonArray result = processarOperacoes(operacoesStr);

        // Escreve o resultado em um arquivo JSON
        try {
            escreverParaArquivo(result, "operacoes.json");
            System.out.println("Arquivo 'operacoes.json' criado com sucesso!");
        } catch (IOException e) {
            System.err.println("Erro ao escrever o arquivo: " + e.getMessage());
        }
    }

    // Método para processar a string e retornar um JsonArray com as operações
    public static JsonArray processarOperacoes(String operacoesStr) {
        // Lista de operações a serem retornadas
        JsonArray jsonArray = new JsonArray();
        
        // Expressão regular para capturar cada operação, transação, registro e escopo (se especificado)
        String regex = "(w|r|u)(\\d+)\\(([^\\s]*)(?: with (tablelock|pagelock|rowlock))?\\)|(?:c|a)(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(operacoesStr);

        int timestamp = 1; // Timestamp fictício para o exemplo

        // Processa cada operação
        while (matcher.find()) {
            String tipoOperacao;
            int transactionId;
            String registro = null;
            String escopo = "rowlock"; // Escopo padrão

            // Verifica se é uma operação de `write`, `read` ou `update`
            if (matcher.group(1) != null) {
                String operacao = matcher.group(1); // Obtém a operação (w, r, u)
                transactionId = Integer.parseInt(matcher.group(2)); // ID da transação
                registro = matcher.group(3); // Registro associado à operação
                if (matcher.group(4) != null) {
                    escopo = matcher.group(4); // Escopo, se especificado
                }

                // Converte a operação para o formato completo
                switch (operacao.toLowerCase()) {
                    case "w":
                        tipoOperacao = "WRITE";
                        break;
                    case "r":
                        tipoOperacao = "READ";
                        break;
                    case "u":
                        tipoOperacao = "UPDATE";
                        break;
                    default:
                        tipoOperacao = "UNKNOWN"; // fallback
                }
            } else {
                // Caso seja `commit` (c) ou `abort` (a)
                tipoOperacao = operacoesStr.charAt(matcher.start()) == 'c' ? "COMMIT" : "ABORT";
                transactionId = Integer.parseInt(matcher.group(5));
            }

            // Cria um objeto JSON para cada operação
            JsonObject jsonOperacao = new JsonObject();
            jsonOperacao.addProperty("transaction", transactionId);
            jsonOperacao.addProperty("timestamp", timestamp);
            jsonOperacao.addProperty("tipo", tipoOperacao);

            // Adiciona o campo `registro` e `escopo` somente se a operação não for `commit` ou `abort`
            if (registro != null) {
                jsonOperacao.addProperty("registro", registro);
                jsonOperacao.addProperty("escopo", escopo);
            }

            // Adiciona ao array
            jsonArray.add(jsonOperacao);
        }

        return jsonArray;
    }

    // Método para escrever o JsonArray em um arquivo JSON
    public static void escreverParaArquivo(JsonArray jsonArray, String nomeArquivo) throws IOException {
        // Usar FileWriter e BufferedWriter para escrever o JSON no arquivo
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivo))) {
            writer.write(jsonArray.toString()); // 2 é para indentar o JSON
        }
    }
}

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
            database.bloqueios = new ArrayList<>();

            for (Tabela tabela : database.tabelas) {
                print("Tabela:");
                tabela.setPai(database);
                tabela.nodes = tabela.paginas;
                tabela.bloqueios = new ArrayList<>();
                // print("PAI TABELA: " + tabela.getPai().toString());

                for (Pagina pagina : tabela.paginas) {
                    print("    Pagina:");
                    pagina.setPai(tabela);
                    pagina.nodes = pagina.registros;
                    pagina.bloqueios = new ArrayList<>();
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

                String registroNome = "";
                Operacao.lock escopoLock = Operacao.lock.rowlock;

                //Commits ou aborts podem não ter registro
                try{
                    registroNome = jsonObject.get("registro").getAsString();
                    escopoLock   = Operacao.lock.valueOf(jsonObject.get("escopo").getAsString());
                }
                catch(Exception e){}


                Operacao.type tipo = Operacao.type.valueOf(jsonObject.get("tipo").getAsString());

                // System.out.println("Buscando " + registroNome);
                Registro registro = database.buscarRegistro(registroNome);
                // System.out.println("Registro encontrado:" + registro);

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
