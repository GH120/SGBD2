import java.util.ArrayList;
import java.util.function.Predicate;

interface Data {
    boolean  temBloqueio(Operacao operacao);
    boolean  propagarBloqueioIntencional(Bloqueio bloqueio);
    Data     buscar(Predicate<Data> filtro);
    Registro buscarRegistro(String nome);
    Data     clonar(); 
}

//Composite é uma classe do padrão composite, útil para modelar a estrutura de árvore do BD
//Definida lá no final junto com os registros

class Database extends Composite {
    ArrayList<Data> tabelas;

    public Database(ArrayList<Tabela> tabelas) {
        super(tabelas);
        this.tabelas = this.nodes;
    }

}

class Tabela extends Composite {

    ArrayList<Data> pagina;
    Database        database;

    public Tabela(ArrayList<Pagina> pagina) {
        super(pagina);
        this.pagina = this.nodes;
    }

}

class Pagina extends Composite {

    ArrayList<Data> registros;
    Tabela          tabela;

    public Pagina(ArrayList<Registro> registros) {
        super(registros);
        this.registros = this.nodes;
    }

}

class Registro implements Data {
    String   nome;
    Integer  valor;
    Pagina   pagina;
    Bloqueio bloqueio;

    public Registro(String nome, Integer valor) {
        this.nome = nome;
        this.valor = valor;
    }

    public boolean temBloqueio(Operacao operacao) {
        return bloqueio != null;
    }

    public boolean propagarBloqueioIntencional(Bloqueio bloqueio) {
        if (this.bloqueio == null) {
            this.bloqueio = bloqueio;
            return true;
        }
        return false; // Logic for compatibility can be added
    }

    public Data buscar(Predicate<Data> condition) {
        if (condition.test(this)) return this;
        else                      return null;
    }

    public Registro buscarRegistro(String nome){
        if(this.nome == nome) return this;
        else                  return null;
    }

    public Data clonar() {
        return new Registro(this.nome, this.valor); // Creates a new Registro with the same values
    }
}

//Implementação do padrão composite, útil para modelar a árvore onde os nós folhas são os registros
class Composite implements Data{

    ArrayList<Data> nodes;
    Bloqueio bloqueio;

    Composite(ArrayList<? extends Data> nodes){

        this.nodes = new ArrayList<Data>(nodes);
    }

    public boolean temBloqueio(Operacao operacao) {
        return false;
    }

    public boolean propagarBloqueioIntencional(Bloqueio bloqueio) {
        return false;
    }

    public Data buscar(Predicate<Data> condition) {

        if (condition.test(this)) return this;

        else return  nodes.stream()
                          .filter(condition)
                          .findFirst()
                          .orElse(null);
    }

    public Registro buscarRegistro(String nome){

        return (Registro) buscar(x -> x instanceof Registro && ((Registro) x).nome.equals(nome));
    }

    public Data clonar() {
        Composite database = new Composite(nodes);

        database.nodes.clear();

        for (Data node : nodes) {
            database.nodes.add(node);
        }
        return database;
    }
}