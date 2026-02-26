import java.util.*;

class Carta {
    String color;
    String valor;

    public Carta(String color, String valor) {
        this.color = color;
        this.valor = valor;
    }

    @Override
    public String toString() {
        return  "\n+----------+" +
                "\n| " + String.format("%-2s", valor) + "        |" +
                "\n|          |" +
                "\n|" + String.format("%-10s", color) + "|" +
                "\n|          |" +
                "\n|       " + String.format("%2s", valor) + " |" +
                "\n+----------+";
    }
}

class Baraja {
    ArrayList<Carta> cartas = new ArrayList<>();

    public Baraja() {
        crearMazoOficial();
        barajear();
    }

    public void crearMazoOficial() {
        String[] colores = {"Rojo", "Amarillo", "Verde", "Azul"};

        for (String color : colores) {
            cartas.add(new Carta(color, "0"));
            for (int i = 1; i <= 9; i++) {
                cartas.add(new Carta(color, String.valueOf(i)));
                cartas.add(new Carta(color, String.valueOf(i)));
            }
        }
    }

    public void barajear() {
        Collections.shuffle(cartas);
    }

    public Carta repartir() {
        if (cartas.size() > 0) {
            return cartas.remove(cartas.size() - 1);
        }
        return null;
    }
}

class Jugador {
    String nombre;
    ArrayList<Carta> mano = new ArrayList<>();

    public Jugador(String nombre) {
        this.nombre = nombre;
    }

    public void recibirCarta(Carta carta) {
        if (carta != null) {
            mano.add(carta);
        }
    }

    public void verMano() {
        System.out.println("Mano de " + nombre + ":");
        for (int i = 0; i < mano.size(); i++) {
            System.out.println("[" + i + "] " + mano.get(i));
        }
    }
}

class Juego {
    ArrayList<Jugador> listaJugadores = new ArrayList<>();
    Baraja mazo = new Baraja();
    Carta descarte;
    int turnoActual = 0;
    ArrayList<Carta> pilaReciclaje = new ArrayList<>();

    public void registrarJugadores(String[] nombres) {
        for (String nombre : nombres) {
            listaJugadores.add(new Jugador(nombre));
        }
    }

    public void repartirCartasIniciales() {
        for (Jugador jugador : listaJugadores) {
            for (int i = 0; i < 7; i++) {
                jugador.recibirCarta(mazo.repartir());
            }
        }
    }

    public boolean intentarJugarCarta(Jugador jugador, Carta cartaJugada) {
        if (descarte == null ||
            cartaJugada.color.equals(descarte.color) ||
            cartaJugada.valor.equals(descarte.valor)) {

            if (descarte != null) {
                pilaReciclaje.add(descarte);
            }

            descarte = cartaJugada;
            jugador.mano.remove(cartaJugada);
            System.out.println("Jugada exitosa");
            return true;

        } else {
            return false;
        }
    }
}

public class Main {

    public static void limpiarPantalla() {
        for (int i = 0; i < 40; i++) {
            System.out.println();
        }
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Introduce tu nombre: ");
        String nombreUsuario = sc.nextLine();

        Juego miJuego = new Juego();
        miJuego.registrarJugadores(new String[]{nombreUsuario, "CPU"});
        miJuego.repartirCartasIniciales();
        miJuego.descarte = miJuego.mazo.repartir();

        boolean juegoEnProgreso = true;
        Random random = new Random();

        while (juegoEnProgreso) {

            limpiarPantalla();

            if (miJuego.mazo.cartas.size() < 2) {
                miJuego.mazo.cartas.addAll(miJuego.pilaReciclaje);
                miJuego.pilaReciclaje.clear();
                miJuego.mazo.barajear();
                System.out.println("¡El mazo se había agotado! Reciclando cartas...");
            }

            System.out.println("\n--- Carta en la mesa: " + miJuego.descarte);
            Jugador jugadorActual = miJuego.listaJugadores.get(miJuego.turnoActual);

            // ===== TURNO DEL JUGADOR =====
            if (jugadorActual.nombre.equals(nombreUsuario)) {

                jugadorActual.verMano();
                boolean decidido = false;

                while (!decidido) {

                    System.out.println("\n1. Poner una carta");
                    System.out.println("2. Robar una carta");
                    System.out.print("Selecciona opción: ");
                    String opcion = sc.nextLine();

                    if (opcion.equals("1")) {

                        try {
                            System.out.print("Elige índice: ");
                            int indice = Integer.parseInt(sc.nextLine());
                            Carta cartaSeleccionada = jugadorActual.mano.get(indice);

                            if (miJuego.intentarJugarCarta(jugadorActual, cartaSeleccionada)) {

                                if (jugadorActual.mano.size() == 1) {
                                    System.out.print("¡Te queda una carta! ¿Gritar UNO? (s): ");
                                    String grito = sc.nextLine().toLowerCase();

                                    if (!grito.equals("s")) {
                                        System.out.println("¡No gritaste UNO! +2 cartas.");
                                        jugadorActual.recibirCarta(miJuego.mazo.repartir());
                                        jugadorActual.recibirCarta(miJuego.mazo.repartir());
                                    } else {
                                        System.out.println("¡Gritaste UNO!");
                                    }
                                }

                                decidido = true;
                            } else {
                                System.out.println("Carta no válida.");
                            }

                        } catch (Exception e) {
                            System.out.println("Índice inválido.");
                        }

                    } else if (opcion.equals("2")) {

                        Carta cartaNueva = miJuego.mazo.repartir();
                        System.out.println("Robaste: " + cartaNueva.color + " " + cartaNueva.valor);

                        if (!miJuego.intentarJugarCarta(jugadorActual, cartaNueva)) {
                            jugadorActual.recibirCarta(cartaNueva);
                        }

                        decidido = true;
                    }
                }

            }
            // ===== TURNO DE LA CPU =====
            else {

                System.out.println("\nTurno de la CPU");
                boolean jugadaRealizada = false;

                for (Carta carta : jugadorActual.mano) {

                    if (miJuego.intentarJugarCarta(jugadorActual, carta)) {

                        System.out.println("CPU jugó: " + carta.color + " " + carta.valor);

                        if (jugadorActual.mano.size() == 1) {

                            if (random.nextDouble() < 0.5) {
                                System.out.println("¡CPU grita: UNO!");
                            } else {
                                System.out.println("¡CPU olvidó gritar UNO! Penalización +2 cartas.");
                                jugadorActual.recibirCarta(miJuego.mazo.repartir());
                                jugadorActual.recibirCarta(miJuego.mazo.repartir());
                            }
                        }

                        jugadaRealizada = true;
                        break;
                    }
                }

                if (!jugadaRealizada) {

                    Carta cartaNueva = miJuego.mazo.repartir();

                    if (miJuego.intentarJugarCarta(jugadorActual, cartaNueva)) {

                        System.out.println("CPU robó y jugó: " + cartaNueva.color + " " + cartaNueva.valor);

                        if (jugadorActual.mano.size() == 1) {

                            if (random.nextDouble() < 0.7) {
                                System.out.println("¡CPU grita: UNO!");
                            } else {
                                System.out.println("¡CPU olvidó gritar UNO! Penalización +2 cartas.");
                                jugadorActual.recibirCarta(miJuego.mazo.repartir());
                                jugadorActual.recibirCarta(miJuego.mazo.repartir());
                            }
                        }

                    } else {
                        jugadorActual.recibirCarta(cartaNueva);
                        System.out.println("CPU robó pero no pudo jugar.");
                    }
                }
            }

            System.out.println("\nPresiona Enter para continuar...");
            sc.nextLine();

            if (jugadorActual.mano.size() == 0) {
                juegoEnProgreso = false;
                System.out.println("¡El jugador " + jugadorActual.nombre + " ha ganado!");
            } else {
                miJuego.turnoActual =
                        (miJuego.turnoActual + 1) % miJuego.listaJugadores.size();
            }
        }

        sc.close();
    }
}