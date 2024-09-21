import java.util.ArrayList;
import java.util.function.Predicate;

//TODO criar testes para verificar se clonagem está funcionando

abstract class Data {

    public Data parentNode = null;

    abstract boolean  temBloqueio(Operacao operacao);
    abstract boolean  propagarBloqueioIntencional(Bloqueio bloqueio);
    abstract Data     buscar(Predicate<Data> filtro);
    abstract Registro buscarRegistro(String nome);
    abstract Data     clonar(); 

    //Get parent node e set parent node?
}

//Implementação do padrão composite, útil para modelar a árvore onde os nós folhas são os registros
abstract class Composite extends Data{

    private ArrayList<? extends Data> nodes; //nodes é uma maneira generalizada de chamar os filhos, se for database então seus filhos são tabelas, de tabelas páginas e assim em diante.
    Bloqueio bloqueio; //Talvez transformar isso numa relação n para n

    Composite(ArrayList<? extends Data> nodes, Data parentNode){

        this.nodes      = nodes;
        this.parentNode = parentNode;
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
                          .map(node -> node.buscar(condition))
                          .filter(node -> node != null)
                          .findFirst()
                          .orElse(null);
    }

    public Registro buscarRegistro(String nome){

        Data registro = buscar(x -> x instanceof Registro && ((Registro) x).nome.equals(nome));

        return (Registro) registro;
    }
}

//Composite é uma classe do padrão composite, útil para modelar a estrutura de árvore do BD
//Definida lá no final junto com os registros

class Database extends Composite {
    ArrayList<Tabela> tabelas;

    public Database(ArrayList<Tabela> tabelas) {
        super(tabelas, null);
        this.tabelas = tabelas;
    }

    public Database clonar() {
        Database copia = new Database(new ArrayList<>());

        for (Tabela tabela : tabelas) {

            Tabela cloneTabela = tabela.clonar();

            copia.tabelas.add(cloneTabela);

            cloneTabela.parentNode = copia;
        }

        return copia;
    }
}

class Tabela extends Composite {

    ArrayList<Pagina> paginas;
    Database        database;

    public Tabela(ArrayList<Pagina> paginas, Database database) {
        super(paginas, database);
        this.paginas = paginas;
        this.database = database;
    }

    public Tabela clonar() {
        Tabela copia = new Tabela(new ArrayList<>(), database);

        for (Pagina pagina : paginas) {

            Pagina paginaClone = pagina.clonar();

            copia.paginas.add(paginaClone);

            paginaClone.parentNode = copia;
        }

        return copia;
    }

}

class Pagina extends Composite {

    ArrayList<Registro> registros;
    Tabela          tabela;

    public Pagina(ArrayList<Registro> registros, Tabela tabela) {
        super(registros, tabela);
        this.registros = registros;
        this.tabela    = tabela;
    }

    public Pagina clonar() {
        Pagina copia = new Pagina(new ArrayList<>(), tabela);

        for (Registro registro : registros) {

            Registro registroClone = registro.clonar();

            copia.registros.add(registroClone);

            registroClone.parentNode = copia;
        }

        return copia;
    }

}

class Registro extends Data {
    String   nome;
    Integer  valor;
    Pagina   pagina;
    Bloqueio bloqueio; //Talvez transformar isso numa relação n para n

    public Registro(String nome, Integer valor, Pagina pagina) {
        this.nome       = nome;
        this.valor      = valor;
        this.pagina     = pagina;
        this.parentNode = pagina;
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

    public Registro clonar() {
        return new Registro(this.nome, this.valor, this.pagina); // Creates a new Registro with the same values
    }
}