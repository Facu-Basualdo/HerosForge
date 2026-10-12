package com.herosforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Las ramas del árbol: agrupan las estadísticas en el menú, la pantalla y {@code /estadisticas ver}. No bloquean
 * nada: cualquier estadística se puede comprar desde el principio.
 */
public enum Rama {
	GUERRERO("guerrero", "Guerrero", Items.NETHERITE_SWORD, "Daño cuerpo a cuerpo y a distancia"),
	GUARDIAN("guardian", "Guardián", Items.TOTEM_OF_UNDYING, "Vida, armadura y aguantar golpes"),
	SUPERVIVIENTE("superviviente", "Superviviente", Items.GOLDEN_CARROT, "Curarse, el hambre y los efectos"),
	EXPLORADOR("explorador", "Explorador", Items.COMPASS, "Moverse por tierra, aire y agua"),
	ARTESANO("artesano", "Artesano", Items.DIAMOND_PICKAXE, "Minar, juntar y comerciar");

	private final String id;
	private final String nombre;
	private final Item icono;
	private final String resumen;

	Rama(String id, String nombre, Item icono, String resumen) {
		this.id = id;
		this.nombre = nombre;
		this.icono = icono;
		this.resumen = resumen;
	}

	public String id() {
		return id;
	}

	public String nombre() {
		return nombre;
	}

	public Item icono() {
		return icono;
	}

	/** Una línea para el ícono de la rama en el cofre. */
	public String resumen() {
		return resumen;
	}

	/** Las de esta rama, en el orden del enum. */
	public List<Estadistica> estadisticas() {
		List<Estadistica> deLaRama = new ArrayList<>();
		for (Estadistica estadistica : Estadistica.values()) {
			if (estadistica.rama() == this) {
				deLaRama.add(estadistica);
			}
		}
		return deLaRama;
	}

	public static Optional<Rama> porId(String id) {
		for (Rama rama : values()) {
			if (rama.id.equals(id)) {
				return Optional.of(rama);
			}
		}
		return Optional.empty();
	}
}
