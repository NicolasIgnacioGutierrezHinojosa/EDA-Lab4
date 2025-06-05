import java.io.*;
import java.util.*;

public class Main {
    public class Paciente{
        private String nombre;
        private String apellido;
        private String id;
        private int categoria;
        private Long tiempoLlegada;
        private String estado;
        private String area;
        private Stack<String> historialCambios = new Stack<>();

        Paciente(String nombre, String apellido, String id, int categoria, Long tiempoLlegada){
            this.nombre = nombre;
            this.apellido = apellido;
            this.id = id;
            this.categoria = categoria;
            this.tiempoLlegada = tiempoLlegada;
            this.estado = "en_espera";

            this.historialCambios = null;
        }

    }
    public static void main(String[] args){

    }
}
