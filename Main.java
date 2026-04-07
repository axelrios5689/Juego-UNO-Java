import java.util.*;

/**
 * Representación de una carta individual.
 */
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
    public abstract void aplicarEfecto(Juego contexto);

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
    @Override public boolean esCompatible(Carta otra, String colorActual) {
        return this.color.equals(colorActual) || this.valor.equals(otra.getValor());
    }
    @Override public void aplicarEfecto(Juego contexto) {}
}

class CartaAccion extends Carta {
    public CartaAccion(String color, String valor) { super(color, valor); }
    @Override public boolean esCompatible(Carta otra, String colorActual) {
        return this.color.equals(colorActual) || this.valor.equals(otra.getValor());
    }
    @Override public void aplicarEfecto(Juego contexto) {
        switch (valor) {
            case "Salto" -> contexto.saltarProximoTurno();
            case "Rev" -> contexto.reversaEfecto();
            case "+2" -> contexto.hacerRobar(2);
        }
    }
}

class CartaComodin extends Carta {
    public CartaComodin(String valor) { super("Negro", valor); }
    @Override public boolean esCompatible(Carta otra, String colorActual) { return true; }
    @Override public void aplicarEfecto(Juego contexto) {
        contexto.cambiarColorGlobal();
        if (valor.equals("+4")) contexto.hacerRobar(4);
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

    public void dejarEnMesa(Carta c) { descarte.push(c); }
    public Carta verCima() { return descarte.peek(); }
}

abstract class Jugador {
    protected String nombre;
    protected List<Carta> mano = new ArrayList<>();
    public Jugador(String nombre) { this.nombre = nombre; }
    public void recibir(Carta c) { if (c != null) mano.add(c); }
    public String getNombre() { return nombre; }
    public void mostrarMano() {
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
    public abstract int elegirAccion(Scanner sc);
    public abstract int elegirCarta(Scanner sc);
    public abstract String elegirColor(Scanner sc);
}

class Humano extends Jugador {
    public Humano(String n) { super(n); }
    @Override public int elegirAccion(Scanner sc) {
        System.out.print("1. Jugar | 2. Robar: ");
        try { return Integer.parseInt(sc.nextLine()); } catch(Exception e) { return 0; }
    }
    @Override public int elegirCarta(Scanner sc) {
        System.out.print("Selecciona el índice: ");
        try { return Integer.parseInt(sc.nextLine()); } catch(Exception e) { return -1; }
    }
    @Override public String elegirColor(Scanner sc) {
        System.out.println("Elige nuevo color: 1.Rojo 2.Verde 3.Azul 4.Amarillo");
        int op;
        try { op = Integer.parseInt(sc.nextLine()); } catch(Exception e) { op = 1; }
        String[] c = {"Rojo", "Verde", "Azul", "Amarillo"};
        return c[Math.max(0, Math.min(op-1, 3))];
    }
}

class CPU extends Jugador {
    public CPU(String n) { super(n); }
    @Override public int elegirAccion(Scanner sc) { return 1; }
    @Override public int elegirCarta(Scanner sc) { return -1; }
    @Override public String elegirColor(Scanner sc) {
        Map<String, Integer> conteo = new HashMap<>();
        String[] colores = {"Rojo", "Verde", "Azul", "Amarillo"};
        for(String col : colores) conteo.put(col, 0);
        int totalColores = 0;
        for (Carta c : mano) {
            if (!c.getColor().equals("Negro")) {
                conteo.put(c.getColor(), conteo.get(c.getColor()) + 1);
                totalColores++;
            }
        }
        String mejorColor = colores[0];
        int max = -1;
        if (totalColores == 0) mejorColor = colores[new Random().nextInt(colores.length)];
        else {
            for (String col : colores) {
                if (conteo.get(col) > max) { max = conteo.get(col); mejorColor = col; }
            }
        }
        System.out.println(this.nombre + " ha elegido el color: " + mejorColor);
        return mejorColor;
    }
}

class Juego {
    private List<Jugador> jugadores = new ArrayList<>();
    private Baraja baraja = new Baraja(); 
    private int turno = 0;
    private int direccion = 1; 
    private String colorActual;
    private Scanner sc = new Scanner(System.in);
    private boolean saltarProximo = false;

    public Juego(String nombreH, int cpus) {
        jugadores.add(new Humano(nombreH));
        String[] nombresCPUs = {"Pepe", "Toña", "Marix"};
        for (int i = 0; i < cpus && i < nombresCPUs.length; i++) jugadores.add(new CPU(nombresCPUs[i]));
        for (Jugador j : jugadores) for (int i = 0; i < 7; i++) j.recibir(baraja.robar());
        
        Carta inicio;
        do { inicio = baraja.robar(); } while (inicio.getValor().equals("+4"));
        baraja.dejarEnMesa(inicio);
        colorActual = inicio.getColor().equals("Negro") ? "Blanco" : inicio.getColor();
        
        System.out.println("\n--- ¡LA PRIMERA CARTA ES! ---");
        for (int i = 0; i < 7; i++) System.out.println(inicio.getLinea(i));
        
        if (!inicio.getValor().matches("\\d")) {
            System.out.println("¡Efecto inicial activado!");
            // LÓGICA DE INICIO CORREGIDA:
            if (inicio.getValor().equals("+2")) {
                Jugador victima = jugadores.get(0); // Tú eres el primero
                System.out.println("¡" + victima.getNombre() + " roba 2 cartas por el +2 inicial y pierde su turno!");
                for (int i = 0; i < 2; i++) victima.recibir(baraja.robar());
                saltarProximo = true; 
            } else if (inicio.getValor().equals("Salto")) {
                System.out.println("¡Se salta el turno inicial de " + jugadores.get(0).getNombre() + "!");
                saltarProximo = true;
            } else if (inicio.getValor().equals("Rev")) {
                if (jugadores.size() == 2) {
                    System.out.println("¡Se salta el turno inicial de " + jugadores.get(0).getNombre() + "!");
                    saltarProximo = true;
                } else {
                    direccion = -1;
                    System.out.println("¡Cambio de dirección! Inicia el último jugador.");
                    turno = (0 + direccion + jugadores.size()) % jugadores.size();
                }
            } else {
                inicio.aplicarEfecto(this); 
            }
        } else {
            colorActual = inicio.getColor();
        }
    }

    public void iniciar() {
        while (true) {
            if (saltarProximo) {
                System.out.println(">>> SE SALTA EL TURNO DE " + jugadores.get(turno).getNombre() + " <<<");
                saltarProximo = false;
                avanzarPuntero();
                continue;
            }

            Jugador actual = jugadores.get(turno);
            imprimirEstado(actual);

            if (actual instanceof Humano) turnoHumano((Humano) actual);
            else turnoCPU((CPU) actual);

            if (actual.mano.size() == 1) {
                if (actual instanceof Humano) {
                    System.out.print("\n¡ESCRIBE 'UNO'!: ");
                    if (!sc.nextLine().equalsIgnoreCase("UNO")) {
                        System.out.println("Penalización +2 cartas por no decir UNO.");
                        actual.recibir(baraja.robar()); actual.recibir(baraja.robar());
                    }
                } else System.out.println("\n¡" + actual.getNombre() + " grita UNO!");
            }

            if (actual.mano.isEmpty()) {
                System.out.println("\n**********************************");
                System.out.println("¡EL GANADOR ES: " + actual.getNombre() + "!");
                System.out.println("**********************************");
                break;
            }
            avanzarPuntero();
        }
        sc.close();
    }

    private void imprimirEstado(Jugador j) {
        System.out.println("\n------------------------------------------------");
        System.out.println("TURNO ACTUAL: " + j.getNombre());
        System.out.println("CARTA EN MESA:");
        for (int i = 0; i < 7; i++) System.out.println(baraja.verCima().getLinea(i));
        String valMesa = baraja.verCima().getValor().equals("Color") ? "comodin" : baraja.verCima().getValor();
        System.out.println("ESTADO -> Color: " + colorActual + " | Valor: " + valMesa);
        if (j instanceof Humano) j.mostrarMano();
    }

    private void turnoHumano(Humano h) {
        int op = h.elegirAccion(sc);
        if (op == 2) {
            Carta r = baraja.robar();
            System.out.println("Robaste:");
            for (int i = 0; i < 7; i++) System.out.println(r.getLinea(i));
            System.out.print("¿Jugarla? (s/n): ");
            if (sc.nextLine().equalsIgnoreCase("s") && validar(h, r)) ejecutarAccionCarta(h, r);
            else h.recibir(r);
        } else {
            int idx = h.elegirCarta(sc);
            if (idx >= 0 && idx < h.mano.size()) {
                Carta e = h.mano.get(idx);
                if (validar(h, e)) { h.mano.remove(idx); ejecutarAccionCarta(h, e); }
                else { System.out.println("Incompatible."); turnoHumano(h); }
            } else { System.out.println("Índice inválido."); turnoHumano(h); }
        }
    }

    private void turnoCPU(CPU c) {
        Carta pJ = null;
        for (Carta ca : c.mano) if (validar(c, ca)) { pJ = ca; break; }
        if (pJ != null) {
            c.mano.remove(pJ);
            String v = pJ.getValor().equals("Color") ? "comodin" : pJ.getValor();
            System.out.println(c.getNombre() + " jugó: " + v + " " + pJ.getColor());
            ejecutarAccionCarta(c, pJ);
        } else {
            System.out.println(c.getNombre() + " roba y pasa.");
            c.recibir(baraja.robar());
        }
    }

    private boolean validar(Jugador j, Carta c) {
        if (c.getValor().equals("+4")) {
            for (Carta m : j.mano) if (m.getColor().equals(colorActual)) return false;
        }
        return c.esCompatible(baraja.verCima(), colorActual) || c.getColor().equals("Negro");
    }

    private void ejecutarAccionCarta(Jugador j, Carta c) {
        baraja.dejarEnMesa(c);
        if (!c.getColor().equals("Negro")) colorActual = c.getColor();
        c.aplicarEfecto(this);
    }

    public void saltarProximoTurno() { saltarProximo = true; }
    public void reversaEfecto() {
        if (jugadores.size() == 2) saltarProximo = true;
        else { direccion *= -1; System.out.println("¡Dirección cambiada!"); }
    }
    public void hacerRobar(int n) {
        int indiceVictima = (turno + (direccion + jugadores.size())) % jugadores.size();
        Jugador victima = jugadores.get(indiceVictima);
        for (int i = 0; i < n; i++) victima.recibir(baraja.robar());
        System.out.println("¡" + victima.getNombre() + " roba " + n + " cartas y pierde su turno!");
        saltarProximo = true; 
    }
    public void cambiarColorGlobal() { colorActual = jugadores.get(turno).elegirColor(sc); }
    private void avanzarPuntero() { turno = (turno + (direccion + jugadores.size())) % jugadores.size(); }
}

public class Main {
    public static void main(String[] args) {
        Scanner s_init = new Scanner(System.in);
        System.out.println("====================================");
        System.out.println("      BIENVENIDO AL JUEGO UNO       ");
        System.out.println("====================================");
        System.out.print("Nombre: "); String n = s_init.nextLine();
        System.out.print("CPUs (1-3): ");
        int c; try { c = Integer.parseInt(s_init.nextLine()); } catch(Exception e) { c = 1; }
        new Juego(n, Math.min(3, c)).iniciar();
    }
}