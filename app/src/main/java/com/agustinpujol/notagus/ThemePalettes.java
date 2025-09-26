package com.agustinpujol.notagus;

/** Paletas por tema: colores para header, contenido y FAB. */
public final class ThemePalettes {

    /** Paquete de colores resueltos para el tema actual. */
    public static final class Colors {
        public final int header;   // Fondo del header
        public final int content;  // Fondo del área de contenido
        public final int fab;      // Fondo del FAB (lo dejamos igual al header)
        public final int fabIcon;  // Color del ícono del FAB (contraste)
        public final int textOnHeader;
        public final int textOnContent;

        public Colors(int header, int content, int fab, int fabIcon, int textOnHeader, int textOnContent ) {
            this.header  = header;
            this.content = content;
            this.fab     = fab;
            this.fabIcon = fabIcon;
            this.textOnHeader = textOnHeader;
            this.textOnContent = textOnContent;
        }
    }

    /** Devuelve la paleta según el ThemeMode guardado. */
    public static Colors forMode(SettingsManager.ThemeMode mode) {
        switch (mode) {
            case DARK: {
                // Card DARK: arriba = #202126 (header), abajo = #292C33 (content)
                int header  = 0xFF202126;
                int content = 0xFF292C33;
                int fab     = header;      // FAB = header
                int fabIcon = 0xFFFFFFFF;  // blanco (mejor contraste)
                int textOnHeader  = 0xFFFFFFFF; // blanco sobre header oscuro
                int textOnContent = 0xFFFFFFFF; // blanco sobre content oscuro
                return new Colors(header, content, fab, fabIcon, textOnHeader, textOnContent);
            }
            case LIGHT: {
                // Card PINK: arriba = #F3AACB (header), abajo = #FBD5E5 (content)
                int header  = 0xFFF9E6EE;
                int content = 0xFFFFF7FA;
                int fab     = header;      // FAB = header
                int fabIcon = 0xFF000000;  // negro (mejor contraste)
                int textOnHeader  = 0xFF000000; // negro sobre header rosa claro
                int textOnContent = 0xFF000000; // negro sobre content rosa claro
                return new Colors(header, content, fab, fabIcon, textOnHeader, textOnContent);
            }
            case SYSTEM:
            default: {
                // Card default/system: arriba = #F4A950 (header), abajo = #FAE9D2 (content)
                int header  = 0xFFF4A950;
                int content = 0xFFFAE9D2;
                int fab     = header;      // FAB = header
                int fabIcon = 0xFF000000;  // negro
                int textOnHeader  = 0xFFFFFFFF; // blanco sobre header naranja
                int textOnContent = 0xFF000000; // negro sobre content crema
                return new Colors(header, content, fab, fabIcon, textOnHeader, textOnContent);
            }
        }
    }

    private ThemePalettes() {}
}
