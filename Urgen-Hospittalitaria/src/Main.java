import java.util.*;

// Clase Paciente
class Paciente {
    String nombre;
    String apellido;
    String id;
    int categoria; // 1 a 5
    long tiempoLlegada; // Unix timestamp en milisegundos
    String estado; // en_espera, en_atencion, atendido
    String area; // SAPU, urgencia_adulto, urgencia_infantil
    Stack<String> historialCambios;

    public Paciente(String nombre, String apellido, String id, int categoria, long tiempoLlegada) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.id = id;
        this.categoria = categoria;
        this.tiempoLlegada = tiempoLlegada;
        this.estado = "en_espera";
        this.area = asignarAreaAtencion();
        this.historialCambios = new Stack<>();
    }

    // Devuelve el tiempo de espera actual en minutos basado en un tiempo simulado
    public long tiempoEsperaActual(long tiempoActual) {
        return (tiempoActual - tiempoLlegada) / 60000; // milisegundos a minutos
    }

    public void registrarCambio(String descripcion) {
        historialCambios.push(descripcion);
    }

    public String obtenerUltimoCambio() {
        return historialCambios.isEmpty() ? null : historialCambios.pop();
    }

    private String asignarAreaAtencion() {
        // Asignar área según categoría
        switch (categoria) {
            case 1:
            case 2:
                return "urgencia_adulto";
            case 3:
            case 4:
                return "SAPU";
            case 5:
                return "urgencia_infantil";
            default:
                return "SAPU";
        }
    }

    @Override
    public String toString() {
        return String.format("Paciente[%s %s, ID: %s, Cat: C%d, Estado: %s, Área: %s, Llegada: %d]",
                nombre, apellido, id, categoria, estado, area, tiempoLlegada);
    }
}

// Clase AreaAtencion
class AreaAtencion {
    String nombre;
    PriorityQueue<Paciente> pacientesHeap;
    int capacidadMaxima;

    public AreaAtencion(String nombre, int capacidadMaxima) {
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
        this.pacientesHeap = new PriorityQueue<>(new Comparator<Paciente>() {
            @Override
            public int compare(Paciente p1, Paciente p2) {
                if (p1.categoria != p2.categoria)
                    return Integer.compare(p1.categoria, p2.categoria);
                else
                    return Long.compare(p1.tiempoLlegada, p2.tiempoLlegada);
            }
        });
    }

    public void ingresarPaciente(Paciente p) {
        if (!estaSaturada()) {
            pacientesHeap.offer(p);
        }
    }

    public Paciente atenderPaciente() {
        return pacientesHeap.poll();
    }

    public boolean estaSaturada() {
        return pacientesHeap.size() >= capacidadMaxima;
    }

    // Retorna pacientes ordenados por prioridad usando heapSort (simulado)
    public List<Paciente> obtenerPacientesPorHeapSort() {
        List<Paciente> lista = new ArrayList<>(pacientesHeap);
        // Ordenar según prioridad
        lista.sort(new Comparator<Paciente>() {
            @Override
            public int compare(Paciente p1, Paciente p2) {
                if (p1.categoria != p2.categoria)
                    return Integer.compare(p1.categoria, p2.categoria);
                return Long.compare(p1.tiempoLlegada, p2.tiempoLlegada);
            }
        });
        return lista;
    }

    @Override
    public String toString() {
        return nombre + " - pacientes: " + pacientesHeap.size();
    }
}

// Clase Hospital
class Hospital {
    Map<String, Paciente> pacientesTotales;
    PriorityQueue<Paciente> colaAtencion;
    Map<String, AreaAtencion> areasAtencion;
    List<Paciente> pacientesAtendidos;

    // Para estadísticas
    Map<Integer, Integer> pacientesAtendidosPorCategoria;
    Map<Integer, Long> tiempoEsperaTotalPorCategoria;
    Map<Integer, Integer> cantidadPacientesPorCategoria;

    public Hospital() {
        pacientesTotales = new HashMap<>();

        colaAtencion = new PriorityQueue<>(new Comparator<Paciente>() {
            @Override
            public int compare(Paciente p1, Paciente p2) {
                if (p1.categoria != p2.categoria)
                    return Integer.compare(p1.categoria, p2.categoria);
                else
                    return Long.compare(p1.tiempoLlegada, p2.tiempoLlegada);
            }
        });

        areasAtencion = new HashMap<>();
        pacientesAtendidos = new ArrayList<>();

        pacientesAtendidosPorCategoria = new HashMap<>();
        tiempoEsperaTotalPorCategoria = new HashMap<>();
        cantidadPacientesPorCategoria = new HashMap<>();
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
            p.categoria = nuevaCategoria;
            // Reinsertar paciente en colaAtencion porque prioridad cambió, así que eliminar y añadir
            colaAtencion.remove(p);
            colaAtencion.offer(p);
        }
    }

    public Paciente atenderSiguiente(long tiempoSimulado) {
        if (colaAtencion.isEmpty())
            return null;

        Paciente pacientePrioritario = null;

        // Buscar paciente que excedió tiempo maximo para atención prioritaria
        for (Paciente p : colaAtencion) {
            long tiempoEspera = p.tiempoEsperaActual(tiempoSimulado);
            if (tiempoEspera > tiempoMaxPermitido(p.categoria)) {
                pacientePrioritario = p;
                break;
            }
        }
        if (pacientePrioritario == null)
            pacientePrioritario = colaAtencion.poll();
        else
            colaAtencion.remove(pacientePrioritario);

        if (pacientePrioritario != null) {
            pacientePrioritario.estado = "en_atencion";
            // Asignar a área y agregar en heap de área si no saturada
            AreaAtencion area = areasAtencion.get(pacientePrioritario.area);
            if (area != null && !area.estaSaturada()) {
                area.ingresarPaciente(pacientePrioritario);
                pacientePrioritario.estado = "atendido";
                pacientesAtendidos.add(pacientePrioritario);

                // Estadísticas
                pacientesAtendidosPorCategoria.put(
                        pacientePrioritario.categoria,
                        pacientesAtendidosPorCategoria.getOrDefault(pacientePrioritario.categoria, 0) + 1);

                long tiempoEspera = pacientePrioritario.tiempoEsperaActual(tiempoSimulado);
                tiempoEsperaTotalPorCategoria.put(
                        pacientePrioritario.categoria,
                        tiempoEsperaTotalPorCategoria.getOrDefault(pacientePrioritario.categoria, 0L) + tiempoEspera);
            }
        }
        return pacientePrioritario;
    }

    // Devuelve lista pacientes en espera de una categoría
    public List<Paciente> obtenerPacientesPorCategoria(int categoria) {
        List<Paciente> lista = new ArrayList<>();
        for (Paciente p : colaAtencion) {
            if (p.categoria == categoria) {
                lista.add(p);
            }
        }
        return lista;
    }

    public AreaAtencion obtenerArea(String nombre) {
        return areasAtencion.get(nombre);
    }

    // Tiempo máximo permitido por categoría en minutos
    private long tiempoMaxPermitido(int categoria) {
        switch (categoria) {
            case 1: return 0; // atención inmediata
            case 2: return 30;
            case 3: return 90;
            case 4: return 180;
            case 5: return Long.MAX_VALUE; // sin límite
            default: return Long.MAX_VALUE;
        }
    }

    // Estadísticas para reporte
    public void mostrarEstadisticas() {
        System.out.println("Estadísticas de atención:");
        int totalAtendidos = pacientesAtendidos.size();
        System.out.println("Total pacientes atendidos: " + totalAtendidos);
        for (int c = 1; c <= 5; c++) {
            int atendidos = pacientesAtendidosPorCategoria.getOrDefault(c, 0);
            long totalEspera = tiempoEsperaTotalPorCategoria.getOrDefault(c, 0L);
            int cantidad = cantidadPacientesPorCategoria.getOrDefault(c, 0);
            double promedio = atendidos > 0 ? ((double) totalEspera / atendidos) : 0;
            System.out.printf("Categoría C%d - Atendidos: %d, Espera promedio: %.2f minutos, Ingresados totales: %d\n",
                    c, atendidos, promedio, cantidad);
        }
    }
}

// Clase GeneradorPacientes
class GeneradorPacientes {
    private static final String[] NOMBRES = {"Juan", "Maria", "Pedro", "Ana", "Luis", "Camila", "Jose"};
    private static final String[] APELLIDOS = {"Gomez", "Lopez", "Martinez", "Fernandez", "Perez", "Diaz"};
    private static int idCounter = 1;
    private static Random random = new Random();

    public static List<Paciente> generarPacientes(int n, long timestampInicio) {
        List<Paciente> pacientes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String nombre = NOMBRES[random.nextInt(NOMBRES.length)];
            String apellido = APELLIDOS[random.nextInt(APELLIDOS.length)];
            String id = "PAC" + (idCounter++);
            int categoria = generarCategoria();
            long tiempoLlegada = timestampInicio + i * 600000; // cada 10 minutos en milisegundos
            pacientes.add(new Paciente(nombre, apellido, id, categoria, tiempoLlegada));
        }
        return pacientes;
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

// Clase SimuladorUrgencia
class SimuladorUrgencia {
    private Hospital hospital;
    private List<Paciente> pacientesGenerados;
    private int indicePacienteNuevo;
    private long tiempoInicioSimulacion;

    public SimuladorUrgencia(Hospital hospital) {
        this.hospital = hospital;
        this.indicePacienteNuevo = 0;
        this.tiempoInicioSimulacion = System.currentTimeMillis();
    }

    public void simular(int pacientesPorDia) {
        pacientesGenerados = GeneradorPacientes.generarPacientes(pacientesPorDia, tiempoInicioSimulacion);

        int nuevosIngresosAcumulados = 0;

        // Simulación minuto a minuto por 24 horas (1440 minutos)
        for (int minuto = 0; minuto < 1440; minuto++) {
            long tiempoSimulado = tiempoInicioSimulacion + minuto * 60000L; // minuto en ms

            // Cada 10 minutos llega un paciente
            if (minuto % 10 == 0 && indicePacienteNuevo < pacientesGenerados.size()) {
                Paciente nuevo = pacientesGenerados.get(indicePacienteNuevo++);
                hospital.registrarPaciente(nuevo);
                nuevosIngresosAcumulados++;
            }

            // Cada 15 minutos se atiende un paciente
            if (minuto % 15 == 0) {
                hospital.atenderSiguiente(tiempoSimulado);
                // Reset acumulado para el caso especial de 3 ingresos -> 2 atenciones
                if (nuevosIngresosAcumulados >= 3) {
                    // Se atienden 2 pacientes adicionales inmediatamente en teria aunque no se si esta bine segun el lab 
                    hospital.atenderSiguiente(tiempoSimulado);
                    hospital.atenderSiguiente(tiempoSimulado);
                    nuevosIngresosAcumulados = 0;
                }
            }

            
        }

        hospital.mostrarEstadisticas();
    }
}


    class HospitalUrgencySimulation {
    public static void main(String[] args) {
        Hospital hospital = new Hospital();
        hospital.agregarArea(new AreaAtencion("SAPU", 100));
        hospital.agregarArea(new AreaAtencion("urgencia_adulto", 100));
        hospital.agregarArea(new AreaAtencion("urgencia_infantil", 100));

        SimuladorUrgencia simulador = new SimuladorUrgencia(hospital);
        simulador.simular(200); // Simulación con 200 pacientes en 24h

        // Se pueden agregar pruebas específicas o no se en este punto estoy al borde de la locura sjsjsjsjjss
        // List<Paciente> c4Pacientes = hospital.obtenerPacientesPorCategoria(4); 
        // System.out.println("Pacientes C4 en espera: " + c4Pacientes.size());
    }
}

