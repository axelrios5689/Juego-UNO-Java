import java.util.*;

/**
 * Simulador del juego UNO.
 * @version 3.0 (Refactorizacion a responsabilidades divididas)
 */

// ==========================================
// 1. GESTION DE INTERFAZ DE USUARIO (CONSOLA)
// ==========================================

/**
 * Proposito: Centraliza todas las interacciones de entrada y salida del sistema.
 * Aisla a la logica de negocio de la consola.
 */
class Consola {
    private Scanner sc = new Scanner(System.in);

    public void mostrarMensaje(String mensaje) {
        System.out.println(mensaje);
    }

    public void mostrarMesa(Carta cima, String colorActual) {
        System.out.println("\n------------------------------------------------");
        System.out.println("CARTA EN MESA:");
        for (int i = 0; i < 7; i++) System.out.println(cima.getLinea(i));
        
        String valMesa = cima.getValor().equals("Color") ? "comodin" : cima.getValor();
        System.out.println("ESTADO -> Color: " + colorActual + " | Valor: " + valMesa);
    }

    public void mostrarMano(String nombre, List<Carta> mano) {
        System.out.println("\nMano de " + nombre + " (" + mano.size() + " cartas):");
        for (int i = 0; i < mano.size(); i += 7) {
            int fin = Math.min(i + 7, mano.size());
            for (int j = i; j < fin; j++) System.out.printf("     [%d]      ", j);
            System.out.println();
            for (int l = 0; l < 7; l++) {
                for (int j = i; j < fin; j++) System.out.print(mano.get(j).getLinea(l) + "  ");
                System.out.println();
            }
        }
    }

    public int pedirEntero(String prompt, int valorPorDefecto) {
        System.out.print(prompt);
        try { 
            return Integer.parseInt(sc.nextLine()); 
        } catch(Exception e) { 
            return valorPorDefecto; 
        }
    }

    public String pedirCadena(String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }

    public void cerrar() {
        sc.close();
    }
}

// ==========================================
// 2. COMPONENTES DEL JUEGO (CARTAS Y BARAJA)
// ==========================================

abstract class Carta {
    protected String color; 
    protected String valor; 
    
    public static final String RESET = "\u001B[0m";
    public static final String ROJO = "\u001B[31m";
    public static final String VERDE = "\u001B[32m";
    public static final String AZUL = "\u001B[34m";
    public static final String AMARILLO = "\u001B[33m";
    public static final String BLANCO = "\u001B[37m";

    public Carta(String color, String valor) {
        this.color = color;
        this.valor = valor;
    }

    public String getColor() { return color; }
    public String getValor() { return valor; }

    public abstract boolean esCompatible(Carta otra, String colorActual);
    
    /**
     * @param turnos Componente que maneja el flujo de turnos
     * @param reglas Componente que impone penalizaciones y efectos
     */
    public abstract void aplicarEfecto(ManejadorTurnos turnos, GestorReglas reglas);

    protected String getANSI() {
        return switch (color) {
            case "Rojo" -> ROJO;
            case "Verde" -> VERDE;
            case "Azul" -> AZUL;
            case "Amarillo" -> AMARILLO;
            default -> BLANCO;
        };
    }

    public String getLinea(int linea) {
        String c = getANSI();
        String vDibujo = valor.equals("Color") ? "comodin" : valor;
        String[] diseño = {
            c + "+----------+" + RESET,
            c + "| " + String.format("%-8s", vDibujo) + " |" + RESET,
            c + "|          |" + RESET,
            c + "| " + String.format("%-8s", color) + " |" + RESET,
            c + "|          |" + RESET,
            c + "| " + String.format("%8s", vDibujo) + " |" + RESET,
            c + "+----------+" + RESET
        };
        return diseño[linea];
    }
}

class CartaNumerica extends Carta {
    public CartaNumerica(String color, String valor) { super(color, valor); }
    
    @Override 
    public boolean esCompatible(Carta otra, String colorActual) {
        return this.color.equals(colorActual) || this.valor.equals(otra.getValor());
    }
    
    @Override 
    public void aplicarEfecto(ManejadorTurnos turnos, GestorReglas reglas) {
        // Las cartas numéricas no alteran el flujo del juego
    }
}

class CartaAccion extends Carta {
    public CartaAccion(String color, String valor) { super(color, valor); }
    
    @Override 
    public boolean esCompatible(Carta otra, String colorActual) {
        return this.color.equals(colorActual) || this.valor.equals(otra.getValor());
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
    public CartaComodin(String valor) { super("Negro", valor); }
    
    @Override 
    public boolean esCompatible(Carta otra, String colorActual) { 
        return true; 
    }
    
    @Override 
    public void aplicarEfecto(ManejadorTurnos turnos, GestorReglas reglas) {
        reglas.solicitarCambioColor();
        if (valor.equals("+4")) reglas.penalizarSiguiente(4);
    }
}

class Baraja {
    private LinkedList<Carta> mazo = new LinkedList<>();
    private Stack<Carta> descarte = new Stack<>();

    public Baraja() {
        String[] colores = {"Rojo", "Verde", "Azul", "Amarillo"};
        for (String c : colores) {
            mazo.add(new CartaNumerica(c, "0"));
            for (int i = 1; i <= 9; i++) {
                mazo.add(new CartaNumerica(c, String.valueOf(i)));
                mazo.add(new CartaNumerica(c, String.valueOf(i)));
            }
            for (int i = 0; i < 2; i++) {
                mazo.add(new CartaAccion(c, "Salto"));
                mazo.add(new CartaAccion(c, "Rev"));
                mazo.add(new CartaAccion(c, "+2"));
            }
        }
        for (int i = 0; i < 4; i++) {
            mazo.add(new CartaComodin("Color"));
            mazo.add(new CartaComodin("+4"));
        }
        Collections.shuffle(mazo);
    }

    public Carta robar() {
        if (mazo.isEmpty()) {
            if (descarte.size() <= 1) return null;
            Carta cima = descarte.pop();
            mazo.addAll(descarte);
            descarte.clear();
            descarte.push(cima);
            Collections.shuffle(mazo);
        }
        return mazo.pollFirst();
    }

    public void devolverAlFondo(Carta c) {
        mazo.addLast(c);
    }

    public void dejarEnMesa(Carta c) { descarte.push(c); }
    public Carta verCima() { return descarte.peek(); }
}

// ==========================================
// 3. JUGADORES
// ==========================================

abstract class Jugador {
    protected String nombre;
    protected List<Carta> mano = new ArrayList<>();
    
    public Jugador(String nombre) { this.nombre = nombre; }
    public void recibir(Carta c) { if (c != null) mano.add(c); }
    public String getNombre() { return nombre; }
    public List<Carta> getMano() { return mano; }
    public boolean noTieneCartas() { return mano.isEmpty(); }
    
    public abstract void ejecutarTurno(Consola ui, GestorReglas reglas);
    public abstract String elegirColor(Consola ui);
}

class Humano extends Jugador {
    public Humano(String n) { super(n); }
    
    @Override 
    public void ejecutarTurno(Consola ui, GestorReglas reglas) {
        ui.mostrarMano(nombre, mano);
        int op = ui.pedirEntero("1. Jugar carta | 2. Robar: ", 0);
        
        if (op == 2) {
            Carta robada = reglas.procesarRobo(this);
            ui.mostrarMensaje("Robaste:");
            for (int i = 0; i < 7; i++) ui.mostrarMensaje(robada.getLinea(i));
            
            String jugar = ui.pedirCadena("¿Jugarla? (s/n): ");
            if (jugar.equalsIgnoreCase("s") && reglas.esJugadaValida(this, robada)) {
                reglas.jugarCarta(this, robada);
            } else {
                this.recibir(robada);
            }
        } else {
            int idx = ui.pedirEntero("Selecciona el índice de la carta: ", -1);
            if (idx >= 0 && idx < mano.size()) {
                Carta seleccionada = mano.get(idx);
                if (reglas.esJugadaValida(this, seleccionada)) {
                    mano.remove(idx);
                    reglas.jugarCarta(this, seleccionada);
                } else {
                    ui.mostrarMensaje("Carta incompatible.");
                    ejecutarTurno(ui, reglas); // Recursividad para forzar acción válida
                }
            } else { 
                ui.mostrarMensaje("Índice inválido."); 
                ejecutarTurno(ui, reglas); 
            }
        }
    }

    @Override 
    public String elegirColor(Consola ui) {
        int op = ui.pedirEntero("Elige nuevo color: 1.Rojo 2.Verde 3.Azul 4.Amarillo: ", 1);
        String[] c = {"Rojo", "Verde", "Azul", "Amarillo"};
        return c[Math.max(0, Math.min(op - 1, 3))];
    }
}

class CPU extends Jugador {
    public CPU(String n) { super(n); }
    
    @Override 
    public void ejecutarTurno(Consola ui, GestorReglas reglas) {
        Carta cartaAJugar = null;
        for (Carta c : mano) {
            if (reglas.esJugadaValida(this, c)) {
                cartaAJugar = c; 
                break;
            }
        }
        
        if (cartaAJugar != null) {
            mano.remove(cartaAJugar);
            String v = cartaAJugar.getValor().equals("Color") ? "comodin" : cartaAJugar.getValor();
            ui.mostrarMensaje(nombre + " jugó: " + v + " " + cartaAJugar.getColor());
            reglas.jugarCarta(this, cartaAJugar);
        } else {
            ui.mostrarMensaje(nombre + " no tiene cartas válidas. Roba y pasa.");
            this.recibir(reglas.procesarRobo(this));
        }
    }

    @Override 
    public String elegirColor(Consola ui) {
        Map<String, Integer> conteo = new HashMap<>();
        String[] colores = {"Rojo", "Verde", "Azul", "Amarillo"};
        for(String col : colores) conteo.put(col, 0);
        
        for (Carta c : mano) {
            if (!c.getColor().equals("Negro")) {
                conteo.put(c.getColor(), conteo.get(c.getColor()) + 1);
            }
        }
        
        String mejorColor = colores[0];
        int max = -1;
        for (String col : colores) {
            if (conteo.get(col) > max) { 
                max = conteo.get(col); 
                mejorColor = col; 
            }
        }
        
        ui.mostrarMensaje(nombre + " ha elegido el color: " + mejorColor);
        return mejorColor;
    }
}

// ==========================================
// 4. MANEJO DE TURNOS Y REGLAS
// ==========================================

/**
 * Propósito: Administra el índice del jugador activo y la dirección de la mesa.
 */
class ManejadorTurnos {
    private int turnoActual = 0; // Índice en la lista de jugadores
    private int direccion = 1;   // 1 horario, -1 antihorario
    private boolean saltarProximo = false;
    private int cantidadJugadores;

    public ManejadorTurnos(int cantidadJugadores) {
        this.cantidadJugadores = cantidadJugadores;
    }

    public int getTurnoActual() { return turnoActual; }
    
    public int getSiguienteTurno() {
        return (turnoActual + (direccion + cantidadJugadores)) % cantidadJugadores;
    }

    public void avanzar() {
        turnoActual = getSiguienteTurno();
    }

    public void activarSalto() {
        saltarProximo = true;
    }

    public boolean debeSaltar() {
        if (saltarProximo) {
            saltarProximo = false;
            return true;
        }
        return false;
    }

    public void invertirDireccion() {
        if (cantidadJugadores == 2) {
            activarSalto(); // En 1v1, el reverso actúa como salto
        } else {
            direccion *= -1;
        }
    }
}

/**
 * Propósito: Valida movimientos y aplica penalizaciones manteniendo el estado global (ej. color actual).
 */
class GestorReglas {
    private Baraja baraja;
    private ManejadorTurnos turnos;
    private List<Jugador> jugadores;
    private Consola ui;
    private String colorActual;

    public GestorReglas(Baraja b, ManejadorTurnos t, List<Jugador> j, Consola ui) {
        this.baraja = b;
        this.turnos = t;
        this.jugadores = j;
        this.ui = ui;
    }

    public String getColorActual() { return colorActual; }
    public void setColorActual(String color) { this.colorActual = color; }

    public boolean esJugadaValida(Jugador j, Carta c) {
        if (c.getValor().equals("+4")) {
            // Regla oficial: Solo puedes tirar +4 si no tienes el color actual
            for (Carta m : j.getMano()) {
                if (m.getColor().equals(colorActual)) return false;
            }
        }
        return c.esCompatible(baraja.verCima(), colorActual) || c.getColor().equals("Negro");
    }

    public void jugarCarta(Jugador j, Carta c) {
        baraja.dejarEnMesa(c);
        if (!c.getColor().equals("Negro")) {
            colorActual = c.getColor();
        }
        c.aplicarEfecto(turnos, this);
    }

    public Carta procesarRobo(Jugador j) {
        return baraja.robar();
    }

    public void penalizarSiguiente(int cantidadCartas) {
        int indiceVictima = turnos.getSiguienteTurno();
        Jugador victima = jugadores.get(indiceVictima);
        
        for (int i = 0; i < cantidadCartas; i++) {
            victima.recibir(baraja.robar());
        }
        
        ui.mostrarMensaje("¡" + victima.getNombre() + " roba " + cantidadCartas + " cartas y pierde su turno!");
        turnos.activarSalto();
    }

    public void solicitarCambioColor() {
        Jugador activo = jugadores.get(turnos.getTurnoActual());
        colorActual = activo.elegirColor(ui);
    }
    
    public void penalizarPorNoDecirUno(Jugador j) {
        ui.mostrarMensaje("Penalización +2 cartas por no decir UNO.");
        j.recibir(baraja.robar()); 
        j.recibir(baraja.robar());
    }
}

// ==========================================
// 5. CLASE ORQUESTADORA (MAIN Y JUEGO)
// ==========================================

/**
 * Propósito: Configurar las piezas del sistema y mantener el bucle principal vivo.
 */
class Juego {
    private List<Jugador> jugadores = new ArrayList<>();
    private Baraja baraja = new Baraja(); 
    private Consola ui = new Consola();
    private ManejadorTurnos turnos;
    private GestorReglas reglas;

    public Juego(String nombreH, int cpus) {
        jugadores.add(new Humano(nombreH));
        String[] nombresCPUs = {"Pablo", "Tania", "Maria"};
        for (int i = 0; i < cpus && i < nombresCPUs.length; i++) {
            jugadores.add(new CPU(nombresCPUs[i]));
        }
        
        turnos = new ManejadorTurnos(jugadores.size());
        reglas = new GestorReglas(baraja, turnos, jugadores, ui);

        // Repartir cartas iniciales
        for (Jugador j : jugadores) {
            for (int i = 0; i < 7; i++) j.recibir(baraja.robar());
        }
        
        establecerCartaInicial();
    }
    
    private void establecerCartaInicial() {
        Carta inicio;
        // La carta inicial debe ser estrictamente numérica según el reglamento
        do {
            inicio = baraja.robar();
            if (!(inicio instanceof CartaNumerica)) {
                baraja.devolverAlFondo(inicio);
            }
        } while (!(inicio instanceof CartaNumerica));
        
        baraja.dejarEnMesa(inicio);
        reglas.setColorActual(inicio.getColor());
        
        ui.mostrarMensaje("\n--- ¡LA PRIMERA CARTA ES! ---");
        for (int i = 0; i < 7; i++) ui.mostrarMensaje(inicio.getLinea(i));
    }

    public void iniciar() {
        while (true) {
            if (turnos.debeSaltar()) {
                ui.mostrarMensaje(">>> SE SALTA EL TURNO DE " + jugadores.get(turnos.getTurnoActual()).getNombre() + " <<<");
                turnos.avanzar();
                continue;
            }

            Jugador actual = jugadores.get(turnos.getTurnoActual());
            ui.mostrarMensaje("\nTURNO ACTUAL: " + actual.getNombre());
            ui.mostrarMesa(baraja.verCima(), reglas.getColorActual());

            actual.ejecutarTurno(ui, reglas);

            // Verificar si debe cantar UNO
            if (actual.getMano().size() == 1) {
                if (actual instanceof Humano) {
                    String respuesta = ui.pedirCadena("\n¡ESCRIBE 'UNO'!: ");
                    if (!respuesta.equalsIgnoreCase("UNO")) {
                        reglas.penalizarPorNoDecirUno(actual);
                    }
                } else {
                    ui.mostrarMensaje("\n¡" + actual.getNombre() + " grita UNO!");
                }
            }

            // Condición de victoria
            if (actual.noTieneCartas()) {
                ui.mostrarMensaje("\n**********************************");
                ui.mostrarMensaje("¡EL GANADOR ES: " + actual.getNombre() + "!");
                ui.mostrarMensaje("**********************************");
                break;
            }
            
            turnos.avanzar();
        }
        ui.cerrar();
    }
}

public class Main {
    public static void main(String[] args) {
        Consola uiInicial = new Consola();
        uiInicial.mostrarMensaje("====================================");
        uiInicial.mostrarMensaje("       BIENVENIDO AL JUEGO UNO      ");
        uiInicial.mostrarMensaje("====================================");
        
        String n = uiInicial.pedirCadena("Nombre: ");
        int c = uiInicial.pedirEntero("CPUs (1-3): ", 1);
        
        new Juego(n, Math.max(1, Math.min(3, c))).iniciar();
    }
}