import java.io.*;
import java.util.*;

// Clase Paciente
class Paciente {
    String nombre, apellido, id;
    int categoria;
    long tiempoLlegada;
    String estado, area;
    Stack<String> historialCambios;
    long tiempoAtencion;

    public Paciente(String nombre, String apellido, String id, int categoria, long tiempoLlegada) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.id = id;
        this.categoria = categoria;
        this.tiempoLlegada = tiempoLlegada;
        this.estado = "en_espera";
        this.area = asignarAreaAtencion();
        this.historialCambios = new Stack<>();
        this.tiempoAtencion = -1;
    }

    public long tiempoEsperaActual(long tiempoActual) {
        return (tiempoActual - tiempoLlegada) / 60000;
    }

    public void registrarCambio(String descripcion) {
        historialCambios.push(descripcion);
    }

    public String obtenerUltimoCambio() {
        return historialCambios.isEmpty() ? null : historialCambios.pop();
    }

    private String asignarAreaAtencion() {
        return switch (categoria) {
            case 1, 2 -> "urgencia_adulto";
            case 3, 4 -> "SAPU";
            case 5 -> "urgencia_infantil";
            default -> "SAPU";
        };
    }

    @Override
    public String toString() {
        return String.format("%s %s;%s;C%d;%s;%s;%d", nombre, apellido, id, categoria, estado, area, tiempoLlegada);
    }
}

class AreaAtencion {
    String nombre;
    PriorityQueue<Paciente> pacientesHeap;
    int capacidadMaxima;

    public AreaAtencion(String nombre, int capacidadMaxima) {
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
        this.pacientesHeap = new PriorityQueue<>((p1, p2) -> {
            if (p1.categoria != p2.categoria)
                return Integer.compare(p1.categoria, p2.categoria);
            return Long.compare(p1.tiempoLlegada, p2.tiempoLlegada);
        });
    }

    public void ingresarPaciente(Paciente p) {
        if (!estaSaturada()) pacientesHeap.offer(p);
    }

    public Paciente atenderPaciente() {
        return pacientesHeap.poll();
    }

    public boolean estaSaturada() {
        return pacientesHeap.size() >= capacidadMaxima;
    }

    public List<Paciente> obtenerPacientesPorHeapSort() {
        List<Paciente> lista = new ArrayList<>(pacientesHeap);
        lista.sort((p1, p2) -> {
            if (p1.categoria != p2.categoria)
                return Integer.compare(p1.categoria, p2.categoria);
            return Long.compare(p1.tiempoLlegada, p2.tiempoLlegada);
        });
        return lista;
    }
}

class Hospital {
    Map<String, Paciente> pacientesTotales = new HashMap<>();
    PriorityQueue<Paciente> colaAtencion;
    Map<String, AreaAtencion> areasAtencion = new HashMap<>();
    List<Paciente> pacientesAtendidos = new ArrayList<>();
    Map<Integer, Integer> pacientesAtendidosPorCategoria = new HashMap<>();
    Map<Integer, Long> tiempoEsperaTotalPorCategoria = new HashMap<>();
    Map<Integer, Integer> cantidadPacientesPorCategoria = new HashMap<>();
    List<Paciente> pacientesFueraDeTiempo = new ArrayList<>();

    public Hospital() {
        colaAtencion = new PriorityQueue<>((p1, p2) -> {
            long ahora = System.currentTimeMillis();
            long espera1 = (ahora - p1.tiempoLlegada) / 60000;
            long espera2 = (ahora - p2.tiempoLlegada) / 60000;

            int prioridad1 = p1.categoria - (espera1 > 180 ? 1 : 0);
            int prioridad2 = p2.categoria - (espera2 > 180 ? 1 : 0);

            if (prioridad1 != prioridad2)
                return Integer.compare(prioridad1, prioridad2);
            return Long.compare(p1.tiempoLlegada, p2.tiempoLlegada);
        });
    }

    public void agregarArea(AreaAtencion area) {
        areasAtencion.put(area.nombre, area);
    }

    public void registrarPaciente(Paciente p) {
        pacientesTotales.put(p.id, p);
        colaAtencion.offer(p);
        cantidadPacientesPorCategoria.put(p.categoria,
                cantidadPacientesPorCategoria.getOrDefault(p.categoria, 0) + 1);
    }

    public void reasignarCategoria(String id, int nuevaCategoria) {
        Paciente p = pacientesTotales.get(id);
        if (p != null) {
            p.registrarCambio("Categoría cambiada de C" + p.categoria + " a C" + nuevaCategoria);
            colaAtencion.remove(p);
            p.categoria = nuevaCategoria;
            colaAtencion.offer(p);
        }
    }

    public Paciente atenderSiguiente(long tiempoSimulado) {
        if (colaAtencion.isEmpty()) return null;
        Paciente paciente = null;
        for (Paciente p : colaAtencion) {
            if (p.tiempoEsperaActual(tiempoSimulado) > tiempoMaxPermitido(p.categoria)) {
                paciente = p;
                pacientesFueraDeTiempo.add(p);
                break;
            }
        }
        if (paciente == null) paciente = colaAtencion.poll();
        else colaAtencion.remove(paciente);

        if (paciente != null) {
            paciente.estado = "atendido";
            paciente.tiempoAtencion = tiempoSimulado;
            AreaAtencion area = areasAtencion.get(paciente.area);
            if (area != null && !area.estaSaturada()) {
                area.ingresarPaciente(paciente);
                pacientesAtendidos.add(paciente);
                pacientesAtendidosPorCategoria.put(paciente.categoria,
                        pacientesAtendidosPorCategoria.getOrDefault(paciente.categoria, 0) + 1);
                long tiempoEspera = paciente.tiempoEsperaActual(tiempoSimulado);
                tiempoEsperaTotalPorCategoria.put(paciente.categoria,
                        tiempoEsperaTotalPorCategoria.getOrDefault(paciente.categoria, 0L) + tiempoEspera);
            }
        }
        return paciente;
    }

    private long tiempoMaxPermitido(int categoria) {
        return switch (categoria) {
            case 1 -> 0;
            case 2 -> 30;
            case 3 -> 90;
            case 4 -> 180;
            case 5 -> Long.MAX_VALUE;
            default -> Long.MAX_VALUE;
        };
    }

    public void mostrarEstadisticas() {
        System.out.println("\n--- ESTADÍSTICAS ---");
        System.out.println("Total pacientes atendidos: " + pacientesAtendidos.size());
        for (int c = 1; c <= 5; c++) {
            int atendidos = pacientesAtendidosPorCategoria.getOrDefault(c, 0);
            long totalEspera = tiempoEsperaTotalPorCategoria.getOrDefault(c, 0L);
            double promedio = atendidos > 0 ? ((double) totalEspera / atendidos) : 0;
            int total = cantidadPacientesPorCategoria.getOrDefault(c, 0);
            System.out.printf("C%d - Atendidos: %d, Promedio espera: %.2f min, Total registrados: %d\n",
                    c, atendidos, promedio, total);
        }
        System.out.println("Pacientes que excedieron el tiempo máximo: " + pacientesFueraDeTiempo.size());
    }

    public void seguimientoPaciente(String id) {
        Paciente p = pacientesTotales.get(id);
        if (p != null && p.tiempoAtencion != -1) {
            long espera = (p.tiempoAtencion - p.tiempoLlegada) / 60000;
            System.out.println("Paciente " + p.nombre + " " + p.apellido + " (" + p.id + ") fue atendido en " + espera + " minutos.");
        }
    }
}
class GeneradorPacientes {
    static final String[] NOMBRES = {"Juan", "Maria", "Pedro", "Ana", "Luis", "Camila", "Jose"};
    static final String[] APELLIDOS = {"Gomez", "Lopez", "Martinez", "Fernandez", "Perez", "Diaz"};
    static int idCounter = 1;
    static Random random = new Random();

    public static List<Paciente> generarPacientes(int n, long inicio) {
        List<Paciente> lista = new ArrayList<>();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("Pacientes_24h.txt"))) {
            for (int i = 0; i < n; i++) {
                String nombre = NOMBRES[random.nextInt(NOMBRES.length)];
                String apellido = APELLIDOS[random.nextInt(APELLIDOS.length)];
                String id = "PAC" + (idCounter++);
                int categoria = generarCategoria();
                long llegada = inicio + i * 600000;
                Paciente p = new Paciente(nombre, apellido, id, categoria, llegada);
                lista.add(p);
                bw.write(p.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private static int generarCategoria() {
        int val = random.nextInt(100);
        if (val < 10) return 1;
        if (val < 25) return 2;
        if (val < 43) return 3;
        if (val < 70) return 4;
        return 5;
    }
}

class SimuladorUrgencia {
    private Hospital hospital;
    private List<Paciente> pacientes;
    private int indice;
    private long inicio;

    public SimuladorUrgencia(Hospital hospital) {
        this.hospital = hospital;
        this.indice = 0;
        this.inicio = System.currentTimeMillis();
    }

    public void simular(int cantidad) {
        pacientes = GeneradorPacientes.generarPacientes(cantidad, inicio);
        int acumulado = 0;

        for (int minuto = 0; minuto < 1440; minuto++) {
            long tiempoSim = inicio + minuto * 60000L;
            if (minuto % 10 == 0 && indice < pacientes.size()) {
                hospital.registrarPaciente(pacientes.get(indice++));
                acumulado++;
            }
            if (minuto % 15 == 0) {
                hospital.atenderSiguiente(tiempoSim);
                if (acumulado >= 3) {
                    hospital.atenderSiguiente(tiempoSim);
                    hospital.atenderSiguiente(tiempoSim);
                    acumulado = 0;
                }
            }
        }

        hospital.mostrarEstadisticas();

        
        for (Paciente p : pacientes) {
            if (p.categoria == 4) {
                hospital.seguimientoPaciente(p.id);
                break;
            }
        }
    }
}

class HospitalUrgencySimulation {
    public static void main(String[] args) {
        Hospital hospital = new Hospital();
        hospital.agregarArea(new AreaAtencion("SAPU", 100));
        hospital.agregarArea(new AreaAtencion("urgencia_adulto", 100));
        hospital.agregarArea(new AreaAtencion("urgencia_infantil", 100));

        SimuladorUrgencia simulador = new SimuladorUrgencia(hospital);
        simulador.simular(200);

        // Simula cambio de categoría
        hospital.reasignarCategoria("PAC5", 1);
    }
}

