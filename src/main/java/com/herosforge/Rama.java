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
	GUERRERO("guerrero", Items.NETHERITE_SWORD),
	GUARDIAN("guardian", Items.TOTEM_OF_UNDYING),
	SUPERVIVIENTE("superviviente", Items.GOLDEN_CARROT),
	EXPLORADOR("explorador", Items.COMPASS),
	ARTESANO("artesano", Items.DIAMOND_PICKAXE);

	private final String id;
	private final Item icono;

	Rama(String id, Item icono) {
		this.id = id;
		this.icono = icono;
	}

	public String id() {
		return id;
	}

	public String nombre(String idioma) {
		return Idioma.plantilla(idioma, "herosforge.rama." + id);
	}

	public Item icono() {
		return icono;
	}

	/** Una línea para el ícono de la rama en el cofre. */
	public String resumen(String idioma) {
		return Idioma.plantilla(idioma, "herosforge.rama." + id + ".resumen");
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
