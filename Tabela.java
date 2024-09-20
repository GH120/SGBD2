import java.util.ArrayList;

interface Data{

    boolean temBloqueio(Operacao operacao);

    boolean propagarBloqueioIntecional(Bloqueio bloqueio);

}

public class Tabela implements Data{

    ArrayList<Pagina> tuplas;
    Bloqueio         bloqueio;

    public boolean temBloqueio(Operacao operacao){

        if(bloqueio == null) return false;

        return true;
    }

    public boolean propagarBloqueioIntecional(Bloqueio bloqueio) {
        
        if(this.bloqueio == null){

            this.bloqueio = bloqueio;

            return true;
        }

        //if bloqueio compatível usando tabela conflitos

        return false;
    }
}

class Pagina implements Data{

    Tabela              tabela;
    ArrayList<Registro> registros;
    Bloqueio            bloqueio;


    public boolean temBloqueio(Operacao operacao){

        if(bloqueio == null) return false;

        return true;
    }

    public boolean propagarBloqueioIntecional(Bloqueio bloqueio) {
        
        if(this.bloqueio == null){

            this.bloqueio = bloqueio;

            return true;
        }

        //if bloqueio compatível usando tabela conflitos

        return false;
    }
}


class Registro implements Data{
    String   Nome;
    Integer  valor;
    Pagina    tupla;
    Bloqueio bloqueio;

    public boolean temBloqueio(Operacao operacao){

        if(bloqueio == null) return false;

        return true;
    }

    public boolean propagarBloqueioIntecional(Bloqueio bloqueio) {
        
        if(this.bloqueio == null){

            this.bloqueio = bloqueio;

            return true;
        }

        //if bloqueio compatível usando tabela conflitos

        return false;
    }
}