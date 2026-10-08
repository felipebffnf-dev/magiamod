package com.magia;

public enum Spell {
    // FOGO
    BOLA_DE_FOGO(Escola.FOGO, "Bola de Fogo", 20f, 15),
    NOVA_DE_FOGO(Escola.FOGO, "Nova de Fogo", 35f, 80),
    // GELO
    CONGELAR(Escola.GELO, "Congelar", 20f, 40),
    NEVASCA(Escola.GELO, "Nevasca", 35f, 100),
    // RAIO
    RAIO(Escola.RAIO, "Raio", 30f, 40),
    DESCARGA(Escola.RAIO, "Descarga", 30f, 70),
    // VENTO
    RAJADA(Escola.VENTO, "Rajada", 15f, 30),
    IMPULSO(Escola.VENTO, "Impulso", 20f, 60),
    // TERRA
    PELE_DE_PEDRA(Escola.TERRA, "Pele de Pedra", 30f, 200),
    TERREMOTO(Escola.TERRA, "Terremoto", 40f, 120),
    // LUZ
    CURA(Escola.LUZ, "Cura", 25f, 60),
    ESCUDO_DE_LUZ(Escola.LUZ, "Escudo de Luz", 30f, 300);

    public enum Escola {
        FOGO("Fogo", 0xFFFF7F3F),
        GELO("Gelo", 0xFF7FD9FF),
        RAIO("Raio", 0xFFFFF03F),
        VENTO("Vento", 0xFFBFFFD9),
        TERRA("Terra", 0xFFB58A5A),
        LUZ("Luz", 0xFFFFFFB0);

        public final String nome;
        public final int cor;

        Escola(String nome, int cor) {
            this.nome = nome;
            this.cor = cor;
        }
    }

    public final Escola escola;
    public final String nome;
    public final float custo;
    public final int cooldown; // em ticks (20 ticks = 1 segundo)

    Spell(Escola escola, String nome, float custo, int cooldown) {
        this.escola = escola;
        this.nome = nome;
        this.custo = custo;
        this.cooldown = cooldown;
    }

    /** Próxima magia dentro da mesma escola. */
    public Spell proximaNaEscola() {
        Spell[] todas = values();
        for (int i = 1; i <= todas.length; i++) {
            Spell s = todas[(ordinal() + i) % todas.length];
            if (s.escola == this.escola) {
                return s;
            }
        }
        return this;
    }

    /** Primeira magia da próxima escola. */
    public Spell proximaEscola() {
        Spell[] todas = values();
        for (int i = 1; i <= todas.length; i++) {
            Spell s = todas[(ordinal() + i) % todas.length];
            if (s.escola != this.escola) {
                return s;
            }
        }
        return this;
    }
}
