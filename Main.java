import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Simulador del juego UNO.
 * Arquitectura robusta con desacoplamiento de UI mediante interfaces (Observer/Listener).
 */

// ==========================================
// 1. ENUMS E INTERFACES DE DESACOPLAMIENTO
// ==========================================

enum ColorCarta { ROJO, VERDE, AZUL, AMARILLO, NEGRO }

/**
 * Permite que el motor de reglas emita alertas sin conocer la consola.
 */
interface NotificadorEventos {
    void informar(String mensaje);
}

/**
 * Define las operaciones visuales que necesita un Jugador, aislando la implementación real.
 */
interface InterfazUsuario extends NotificadorEventos {
    void mostrarMesa(Carta cima, ColorCarta colorActual);
    void mostrarMano(String nombre, List<Carta> mano);
    int pedirEntero(String prompt, int valorPorDefecto);
    String pedirCadena(String prompt);
}

// ==========================================
// 2. GESTION DE INTERFAZ DE USUARIO (CONSOLA)
// ==========================================

class Consola implements InterfazUsuario {
    private Scanner sc = new Scanner(System.in); // lector de entradas del sistema

    @Override
    public void informar(String mensaje) {
        System.out.println(mensaje);
    }

    @Override
    public void mostrarMesa(Carta cima, ColorCarta colorActual) {
        System.out.println("\n------------------------------------------------");
        System.out.println("CARTA EN MESA:");
        IntStream.range(0, 7).forEach(i -> System.out.println(cima.getLinea(i)));
        
        String valMesa = cima.getValor().equals("Color") ? "comodin" : cima.getValor();
        System.out.println("ESTADO -> Color: " + colorActual + " | Valor: " + valMesa);
    }

    @Override
    public void mostrarMano(String nombre, List<Carta> mano) {
        System.out.println("\nMano de " + nombre + " (" + mano.size() + " cartas):");
        
        // itera sobre la mano en bloques para mantener el formato ASCII legible en consola
        for (int i = 0; i < mano.size(); i += 7) {
            int fin = Math.min(i + 7, mano.size());
            for (int j = i; j < fin; j++) System.out.printf("     [%d]      ", j);
            System.out.println();
            for (int l = 0; l < 7; l++) {
                for (int j = i; j < fin; j++) System.out.print(mano.get(j).getLinea(l) + "  ");
                System.out.println();
            }
        } // fin del ciclo for de dibujado
    }

    @Override
    public int pedirEntero(String prompt, int valorPorDefecto) {
        System.out.print(prompt);
        try { return Integer.parseInt(sc.nextLine()); } 
        catch(Exception e) { return valorPorDefecto; }
    }

    @Override
    public String pedirCadena(String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }

    public void cerrar() { sc.close(); }
}

// ==========================================
// 3. COMPONENTES DEL JUEGO (CARTAS Y BARAJA)
// ==========================================

abstract class Carta {
    protected ColorCarta color; // Categoría de color de la carta
    protected String valor;     // Símbolo o número que representa
    
    public static final String RESET = "\u001B[0m";
    public static final String ROJO = "\u001B[31m";
    public static final String VERDE = "\u001B[32m";
    public static final String AZUL = "\u001B[34m";
    public static final String AMARILLO = "\u001B[33m";
    public static final String BLANCO = "\u001B[37m";

    public Carta(ColorCarta color, String valor) {
        this.color = color;
        this.valor = valor;
    }

    public ColorCarta getColor() { return color; }
    public String getValor() { return valor; }

    public abstract boolean esCompatible(Carta otra, ColorCarta colorActual);
    public abstract void aplicarEfecto(ManejadorTurnos turnos, GestorReglas reglas);

    protected String getANSI() {
        return switch (color) {
            case ROJO -> ROJO;
            case VERDE -> VERDE;
            case AZUL -> AZUL;
            case AMARILLO -> AMARILLO;
            case NEGRO -> BLANCO;
        };
    }

    public String getLinea(int linea) {
        String c = getANSI();
        String vDibujo = valor.equals("Color") ? "comodin" : valor;
        String[] diseño = {
            c + "+----------+" + RESET,
            c + "| " + String.format("%-8s", vDibujo) + " |" + RESET,
            c + "|          |" + RESET,
            c + "| " + String.format("%-8s", color.name()) + " |" + RESET,
            c + "|          |" + RESET,
            c + "| " + String.format("%8s", vDibujo) + " |" + RESET,
            c + "+----------+" + RESET
        };
        return diseño[linea];
    }
}

class CartaNumerica extends Carta {
    public CartaNumerica(ColorCarta color, String valor) { super(color, valor); }
    
    @Override 
    public boolean esCompatible(Carta otra, ColorCarta colorActual) {
        return this.color == colorActual || this.valor.equals(otra.getValor());
    }
    
    @Override 
    public void aplicarEfecto(ManejadorTurnos turnos, GestorReglas reglas) {}
}

class CartaAccion extends Carta {
    public CartaAccion(ColorCarta color, String valor) { super(color, valor); }
    
    @Override 
    public boolean esCompatible(Carta otra, ColorCarta colorActual) {
        return this.color == colorActual || this.valor.equals(otra.getValor());
    }
    
    @Override 
    public void aplicarEfecto(ManejadorTurnos turnos, GestorReglas reglas) {
        switch (valor) {
            case "Salto" -> turnos.activarSalto();
            case "Rev" -> turnos.invertirDireccion();
            case "+2" -> reglas.penalizarSiguiente(2);
        }
    }
}

class CartaComodin extends Carta {
    public CartaComodin(String valor) { super(ColorCarta.NEGRO, valor); }
    
    @Override 
    public boolean esCompatible(Carta otra, ColorCarta colorActual) { return true; }
    
    @Override 
    public void aplicarEfecto(ManejadorTurnos turnos, GestorReglas reglas) {
        // La asignación de nuevo color ahora recae en el orquestador
        if (valor.equals("+4")) reglas.penalizarSiguiente(4);
    }
}

class PilaDescarte {
    private Stack<Carta> pila = new Stack<>(); // pila física en la mesa

    public void dejarEnMesa(Carta c) { pila.push(c); }
    public Carta verCima() { return pila.peek(); }
    public int size() { return pila.size(); }
    public Carta removerCima() { return pila.pop(); }
    public void vaciar() { pila.clear(); }
    public Collection<Carta> obtenerTodas() { return pila; }
}

class Baraja {
    private LinkedList<Carta> mazo = new LinkedList<>(); // reserva para robar
    private PilaDescarte descarte;

    public Baraja(PilaDescarte descarte) {
        this.descarte = descarte;
        ColorCarta[] coloresBasicos = {ColorCarta.ROJO, ColorCarta.VERDE, ColorCarta.AZUL, ColorCarta.AMARILLO};
        
        Arrays.stream(coloresBasicos).forEach(c -> {
            mazo.add(new CartaNumerica(c, "0"));
            IntStream.rangeClosed(1, 9).forEach(i -> {
                mazo.add(new CartaNumerica(c, String.valueOf(i)));
                mazo.add(new CartaNumerica(c, String.valueOf(i)));
            });
            IntStream.range(0, 2).forEach(i -> {
                mazo.add(new CartaAccion(c, "Salto"));
                mazo.add(new CartaAccion(c, "Rev"));
                mazo.add(new CartaAccion(c, "+2"));
            });
        });
        
        IntStream.range(0, 4).forEach(i -> {
            mazo.add(new CartaComodin("Color"));
            mazo.add(new CartaComodin("+4"));
        });
        
        Collections.shuffle(mazo);
    }

    public Carta robar() {
        if (mazo.isEmpty()) {
            if (descarte.size() <= 1) return null;
            Carta cima = descarte.removerCima();
            mazo.addAll(descarte.obtenerTodas());
            descarte.vaciar();
            descarte.dejarEnMesa(cima);
            Collections.shuffle(mazo);
        }
        return mazo.pollFirst();
    }

    public void devolverAlFondo(Carta c) { mazo.addLast(c); }
}

// ==========================================
// 4. JUGADORES
// ==========================================

abstract class Jugador {
    protected String nombre;
    protected List<Carta> mano = new ArrayList<>(); // inventario en turno
    
    public Jugador(String nombre) { this.nombre = nombre; }
    public void recibir(Carta c) { if (c != null) mano.add(c); }
    public String getNombre() { return nombre; }
    public List<Carta> getMano() { return mano; }
    public boolean noTieneCartas() { return mano.isEmpty(); }
    
    /**
     * @param ui Interfaz aislada para pedir u ofrecer datos
     * @param reglas Validador para confirmar si una acción mental es legal
     * @return La carta seleccionada para jugar, o null si decidió robar
     */
    public abstract Carta ejecutarTurno(InterfazUsuario ui, GestorReglas reglas);
    public abstract ColorCarta elegirColor(InterfazUsuario ui);
}

class Humano extends Jugador {
    public Humano(String n) { super(n); }
    
    @Override 
    public Carta ejecutarTurno(InterfazUsuario ui, GestorReglas reglas) {
        ui.mostrarMano(nombre, mano);
        int op = ui.pedirEntero("1. Jugar carta | 2. Robar: ", 0);
        
        if (op == 2) {
            Carta robada = reglas.procesarRobo();
            ui.informar("Robaste:");
            IntStream.range(0, 7).forEach(i -> ui.informar(robada.getLinea(i)));
            
            String jugar = ui.pedirCadena("¿Jugarla? (s/n): ");
            if (jugar.equalsIgnoreCase("s") && reglas.esJugadaValida(this, robada)) {
                return robada;
            }
            this.recibir(robada);
            return null;
        } else {
            int idx = ui.pedirEntero("Selecciona el índice de la carta: ", -1);
            if (idx >= 0 && idx < mano.size()) {
                Carta seleccionada = mano.get(idx);
                if (reglas.esJugadaValida(this, seleccionada)) {
                    mano.remove(idx);
                    return seleccionada;
                }
                ui.informar("Carta incompatible.");
            } else { 
                ui.informar("Índice inválido."); 
            }
            return ejecutarTurno(ui, reglas); // recursividad si hay error
        }
    }

    @Override 
    public ColorCarta elegirColor(InterfazUsuario ui) {
        int op = ui.pedirEntero("Elige nuevo color: 1.Rojo 2.Verde 3.Azul 4.Amarillo: ", 1);
        ColorCarta[] c = {ColorCarta.ROJO, ColorCarta.VERDE, ColorCarta.AZUL, ColorCarta.AMARILLO};
        return c[Math.max(0, Math.min(op - 1, 3))];
    }
}

class CPU extends Jugador {
    public CPU(String n) { super(n); }
    
    @Override 
    public Carta ejecutarTurno(InterfazUsuario ui, GestorReglas reglas) {
        Carta cartaAJugar = mano.stream()
                .filter(c -> reglas.esJugadaValida(this, c))
                .findFirst()
                .orElse(null);
        
        if (cartaAJugar != null) {
            mano.remove(cartaAJugar);
            return cartaAJugar;
        }
        
        ui.informar(nombre + " no tiene cartas válidas. Roba y pasa.");
        this.recibir(reglas.procesarRobo());
        return null;
    }

    @Override 
    public ColorCarta elegirColor(InterfazUsuario ui) {
        ColorCarta mejorColor = mano.stream()
                .filter(c -> c.getColor() != ColorCarta.NEGRO)
                .collect(Collectors.groupingBy(Carta::getColor, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ColorCarta.ROJO);
                
        ui.informar(nombre + " ha elegido el color: " + mejorColor.name());
        return mejorColor;
    }
}

// ==========================================
// 5. MANEJO DE TURNOS Y REGLAS
// ==========================================

class ManejadorTurnos {
    private int turnoActual = 0; 
    private int direccion = 1;   
    private boolean saltarProximo = false; 
    private int cantidadJugadores;

    public ManejadorTurnos(int cantidadJugadores) { this.cantidadJugadores = cantidadJugadores; }
    public int getTurnoActual() { return turnoActual; }
    public int getSiguienteTurno() { return (turnoActual + (direccion + cantidadJugadores)) % cantidadJugadores; }
    public void avanzar() { turnoActual = getSiguienteTurno(); }
    public void activarSalto() { saltarProximo = true; }

    public boolean debeSaltar() {
        if (saltarProximo) {
            saltarProximo = false;
            return true;
        }
        return false;
    }

    public void invertirDireccion() {
        if (cantidadJugadores == 2) activarSalto(); 
        else direccion *= -1;
    }
}

class GestorReglas {
    private Baraja baraja;
    private PilaDescarte descarte;
    private ManejadorTurnos turnos;
    private List<Jugador> jugadores;
    private NotificadorEventos notificador; // Única vía de comunicación externa, sin depender de UI concreta
    private ColorCarta colorActual;

    public GestorReglas(Baraja b, PilaDescarte pd, ManejadorTurnos t, List<Jugador> j, NotificadorEventos notificador) {
        this.baraja = b;
        this.descarte = pd;
        this.turnos = t;
        this.jugadores = j;
        this.notificador = notificador;
    }

    public ColorCarta getColorActual() { return colorActual; }
    public void setColorActual(ColorCarta color) { this.colorActual = color; }

    public boolean esJugadaValida(Jugador j, Carta c) {
        if (c.getValor().equals("+4")) {
            return j.getMano().stream().noneMatch(m -> m.getColor() == colorActual);
        }
        return c.esCompatible(descarte.verCima(), colorActual) || c.getColor() == ColorCarta.NEGRO;
    }

    public void aplicarJugada(Carta c) {
        descarte.dejarEnMesa(c);
        if (c.getColor() != ColorCarta.NEGRO) {
            colorActual = c.getColor();
        }
        c.aplicarEfecto(turnos, this);
    }

    public Carta procesarRobo() { return baraja.robar(); }

    public void penalizarSiguiente(int cantidadCartas) {
        int indiceVictima = turnos.getSiguienteTurno();
        Jugador victima = jugadores.get(indiceVictima);
        
        IntStream.range(0, cantidadCartas).forEach(i -> victima.recibir(baraja.robar()));
        
        notificador.informar("¡" + victima.getNombre() + " roba " + cantidadCartas + " cartas y pierde su turno!");
        turnos.activarSalto();
    }

    /**
     * Centraliza la evaluación del grito de UNO para liberar al orquestador de conocer reglas del juego.
     * @param j El jugador actual en evaluación
     * @param grito Expresión afirmativa enviada desde el orquestador
     */
    public void validarCantoUno(Jugador j, boolean grito) {
        if (j.getMano().size() == 1) {
            if (!grito) {
                notificador.informar("Penalización +2 cartas por no decir UNO.");
                j.recibir(baraja.robar()); 
                j.recibir(baraja.robar());
            } else {
                notificador.informar("\n¡" + j.getNombre() + " grita UNO!");
            }
        }
    }
}

// ==========================================
// 6. CLASE ORQUESTADORA (MAIN Y JUEGO)
// ==========================================

class Juego {
    private List<Jugador> jugadores = new ArrayList<>();
    private PilaDescarte descarte = new PilaDescarte();
    private Baraja baraja = new Baraja(descarte); 
    private Consola consola = new Consola();
    private ManejadorTurnos turnos;
    private GestorReglas reglas;

    public Juego(String nombreH, int cpus) {
        jugadores.add(new Humano(nombreH));
        String[] nombresCPUs = {"Pablo", "Tania", "Maria"};
        IntStream.range(0, Math.min(cpus, nombresCPUs.length)).forEach(i -> jugadores.add(new CPU(nombresCPUs[i])));
        
        turnos = new ManejadorTurnos(jugadores.size());
        // Se inyecta la consola asumiendo solo su rol de NotificadorEventos
        reglas = new GestorReglas(baraja, descarte, turnos, jugadores, consola);

        jugadores.forEach(j -> IntStream.range(0, 7).forEach(i -> j.recibir(baraja.robar())));
        establecerCartaInicial();
    }
    
 private void establecerCartaInicial() {
    Carta inicio;
    
    // Buscamos la primera carta numérica
    do {
        inicio = baraja.robar();
        if (!(inicio instanceof CartaNumerica)) {
            baraja.devolverAlFondo(inicio);
        }
    } while (!(inicio instanceof CartaNumerica));

    // Aquí ya tenemos la carta definitiva
    descarte.dejarEnMesa(inicio);
    reglas.setColorActual(inicio.getColor());

    consola.informar("\n--- ¡LA PRIMERA CARTA ES! ---");
    
    // Usamos una variable FINAL para la lambda
    final Carta cartaInicial = inicio;
    
    IntStream.range(0, 7).forEach(i -> consola.informar(cartaInicial.getLinea(i)));
}

    public void iniciar() {
        while (true) {
            if (turnos.debeSaltar()) {
                consola.informar(">>> SE SALTA EL TURNO DE " + jugadores.get(turnos.getTurnoActual()).getNombre() + " <<<");
                turnos.avanzar();
                continue;
            }

            Jugador actual = jugadores.get(turnos.getTurnoActual());
            consola.informar("\nTURNO ACTUAL: " + actual.getNombre());
            consola.mostrarMesa(descarte.verCima(), reglas.getColorActual());

            // 1. Fase de Acción
            Carta cartaJugada = actual.ejecutarTurno(consola, reglas);

            // 2. Fase de Resolución y Estado en mesa
            if (cartaJugada != null) {
                if (cartaJugada.getColor() == ColorCarta.NEGRO) {
                    ColorCarta nuevoColor = actual.elegirColor(consola);
                    reglas.setColorActual(nuevoColor);
                }
                
                String v = cartaJugada.getValor().equals("Color") ? "comodin" : cartaJugada.getValor();
                if(actual instanceof Humano) consola.informar("Jugaste: " + v + " " + cartaJugada.getColor().name());
                
                reglas.aplicarJugada(cartaJugada);
            }

            // 3. Fase de Verificación de Reglas (UNO)
            if (actual.getMano().size() == 1) {
                boolean grito = true;
                if (actual instanceof Humano) {
                    grito = consola.pedirCadena("\n¡ESCRIBE 'UNO'!: ").equalsIgnoreCase("UNO");
                }
                reglas.validarCantoUno(actual, grito);
            } // fin de la verificación de UNO

            // 4. Condición de victoria
            if (actual.noTieneCartas()) {
                consola.informar("\n**********************************");
                consola.informar("¡EL GANADOR ES: " + actual.getNombre() + "!");
                consola.informar("**********************************");
                break;
            }
            
            turnos.avanzar();
        } // fin del ciclo principal
        consola.cerrar();
    }
}

public class Main {
    public static void main(String[] args) {
        Consola uiInicial = new Consola();
        uiInicial.informar("====================================");
        uiInicial.informar("       BIENVENIDO AL JUEGO UNO      ");
        uiInicial.informar("====================================");
        
        String n = uiInicial.pedirCadena("Nombre: ");
        int c = uiInicial.pedirEntero("CPUs (1-3): ", 1);
        
        new Juego(n, Math.max(1, Math.min(3, c))).iniciar();
    }
}